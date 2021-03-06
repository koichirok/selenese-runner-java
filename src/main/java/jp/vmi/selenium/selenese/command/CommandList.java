package jp.vmi.selenium.selenese.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.SeleneseCommandErrorException;
import jp.vmi.selenium.selenese.inject.DoCommand;
import jp.vmi.selenium.selenese.result.CommandResult;
import jp.vmi.selenium.selenese.result.CommandResultList;
import jp.vmi.selenium.selenese.result.Error;
import jp.vmi.selenium.selenese.result.Result;

/**
 * Command list.
 */
public class CommandList implements Iterable<ICommand> {

    private final Map<Object, Integer> indexCache = new HashMap<>();
    private final List<ICommand> commandList = new ArrayList<>();
    private static final Scanner systemInReader = new Scanner(System.in);

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements.
     */
    public boolean isEmpty() {
        return commandList.isEmpty();
    }

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of elements in this list.
     */
    public int size() {
        return commandList.size();
    }

    /**
     * Add command.
     *
     * @param command command.
     * @return true if this collection changed as a result of the call. (for keeping compatibility)
     */
    public boolean add(ICommand command) {
        if (command instanceof ILabel)
            indexCache.put(((ILabel) command).getLabel(), commandList.size());
        return commandList.add(command);
    }

    /**
     * Get index by label or command.
     *
     * @param key label string or ICommand object.
     * @return index or -1.
     */
    public int indexOf(Object key) {
        Integer index = indexCache.get(key);
        if (index == null) {
            index = commandList.indexOf(key);
            indexCache.put(key, index);
        }
        return index;
    }

    /**
     * Original list iterator.
     *
     * see {@link ArrayList#listIterator(int)}
     *
     * @param index start index.
     * @return ListIterator.
     */
    protected ListIterator<ICommand> originalListIterator(int index) {
        return commandList.listIterator(index);
    }

    @Override
    public CommandListIterator iterator() {
        return iterator(null);
    }

    /**
     * Create the iterator of this command list.
     *
     * @param parentIterator parent iterator.
     *
     * @return iterator.
     */
    public CommandListIterator iterator(CommandListIterator parentIterator) {
        return new CommandListIterator(this, parentIterator);
    }

    @DoCommand
    protected Result doCommand(Context context, ICommand command, String... curArgs) {
        try {
            return command.execute(context, curArgs);
        } catch (SeleneseCommandErrorException e) {
            return e.getError();
        } catch (Exception e) {
            return new Error(e);
        }
    }

    private static final Pattern JS_BLOCK_RE = Pattern.compile("javascript\\{(.*)\\}", Pattern.DOTALL);

    protected void evalCurArgs(Context context, String[] curArgs) {
        for (int i = 0; i < curArgs.length; i++) {
            Matcher matcher = JS_BLOCK_RE.matcher(curArgs[i]);
            if (matcher.matches()) {
                Object value = context.getEval().eval(context, matcher.group(1));
                if (value == null)
                    value = "";
                curArgs[i] = value.toString();
            }
        }
    }

    /**
     * Execute command list.
     *
     * @param context Selenese Runner context.
     * @param cresultList command result list for keeping all command results.
     * @return result of command list execution.
     */
    public Result execute(Context context, CommandResultList cresultList) {
        CommandListIterator parentIterator = context.getCommandListIterator();
        CommandListIterator commandListIterator = iterator(parentIterator);
        context.pushCommandListIterator(commandListIterator);
        CommandSequence sequence = commandListIterator.getCommandSequence();
        boolean isContinued = true;
        try {
            while (isContinued && commandListIterator.hasNext()) {
                ICommand command = commandListIterator.next();
                sequence.increment(command);
                List<Screenshot> ss = command.getScreenshots();
                int prevSSIndex = (ss == null) ? 0 : ss.size();
                String[] curArgs = command.getVariableResolvedArguments(context.getCurrentTestCase().getSourceType(), context.getVarsMap());
                evalCurArgs(context, curArgs);
                Result result = null;
                context.resetRetries();
                while (true) {
                    if(command.getName().equals("comment")) {
                        if (command.getArguments().length > 0) {
                            if (command.getArguments()[0].equals("breakpoint")) {
                                ((Runner)context).setInteractive(true);
                            }
                        }
                    }
                    if (context.isInteractive()) {
                        while (true) {
                            System.out.println(">>>>>Interactive mode<<<<<");
                            System.out.println("Current command: " + command.getName() + " " + Arrays.toString(command.getArguments()));
                            System.out.println("Input <space> or <return> to run. Input c to exit interactive mode. Input < to previous command. Input > to next command.");
                            String userInputKey = systemInReader.nextLine();

                            if (userInputKey.equals(" ") || userInputKey.equals("")) {
                                break;
                            }
                            if (userInputKey.equals("c")) {
                                ((Runner)context).setInteractive(false);
                                break;
                            }   
                            if (userInputKey.equals("<")) {
                                commandListIterator.jumpTo(command);
                                if (commandListIterator.hasPrevious()) {
                                    command = commandListIterator.previous();
                                    commandListIterator.next();
                                    curArgs = command.getVariableResolvedArguments(context.getCurrentTestCase().getSourceType(), context.getVarsMap());
                                }
                                continue;
                            } 
                            if (userInputKey.equals(">")) {
                                if (commandListIterator.hasNext()) {
                                    command = commandListIterator.next();
                                    curArgs = command.getVariableResolvedArguments(context.getCurrentTestCase().getSourceType(), context.getVarsMap());
                                }
                                continue;
                            }                              
                        }
                    }                    
                    result = doCommand(context, command, curArgs);
                    if (result.isSuccess() || context.hasReachedMaxRetries())
                        break;
                    context.incrementRetries();
                    context.waitSpeed();
                }
                if (result.isAborted())
                    isContinued = false;
                else
                    context.waitSpeed();
                ss = command.getScreenshots();
                List<Screenshot> newSS;
                if (ss == null || prevSSIndex == ss.size())
                    newSS = null;
                else
                    newSS = new ArrayList<>(ss.subList(prevSSIndex, ss.size()));
                CommandResult cresult = new CommandResult(sequence.toString(), command, newSS, result, cresultList.getEndTime(), System.currentTimeMillis());
                cresultList.add(cresult);

            }
        } finally {
            context.popCommandListIterator();
        }
        return cresultList.getResult();
    }

    @Override
    public String toString() {
        return commandList.toString();
    }
}
