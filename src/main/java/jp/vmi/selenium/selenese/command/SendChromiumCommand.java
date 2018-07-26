package jp.vmi.selenium.selenese.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.result.Result;

import static jp.vmi.selenium.selenese.command.ArgumentType.VALUE;
import static jp.vmi.selenium.selenese.result.Success.SUCCESS;

public class SendChromiumCommand extends AbstractCommand {
    private static final Logger log = LoggerFactory.getLogger(Assertion.class);

    SendChromiumCommand(int index, String name, String... args) {
        super(index, name, args, VALUE, VALUE);
    }

    @Override
    public boolean mayUpdateScreen() {
        return false;
    }

    @Override
    protected Result executeImpl(Context context, String... curArgs) {
        WebDriver driver = context.getWrappedDriver();
        if (driver instanceof RemoteWebDriver) {
            RemoteWebDriver rwd = (RemoteWebDriver) driver;
            HttpCommandExecutor executor = (HttpCommandExecutor) rwd.getCommandExecutor();

            String cmdName = "chromium/send_command_and_get_result";
            try {
                // このブロックの処理は一度実行されるだけでよいので、 selenese-runner-java 本体にマージしてもらう場合は
                // 別の場所に移動させる
                // この処理の恩恵を受けるのは RemoteWebDriver (remote-browser=chrome) と ChromeDriver だけなので
                // ChromeDriverFactory にコードを仕込んで RemoteWebDriver が ChromeDriver 用のオプションを生成する
                // あたりでも実行するようにする
                Method defineCommand = HttpCommandExecutor.class.getDeclaredMethod("defineCommand", String.class, CommandInfo.class);
                defineCommand.setAccessible(true);
                CommandInfo cmd = new CommandInfo("/session/:sessionId/chromium/send_command_and_get_result", HttpMethod.POST);
                defineCommand.invoke(executor, cmdName, cmd);
            } catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            HashMap<String,Object> params = new HashMap<>();
            params.put("cmd", curArgs[0]);
            params.put("params",curArgs[1].isEmpty() ? new JsonObject() : new JsonParser().parse(curArgs[1]));

            try {
                Response res = executor.execute(new Command(rwd.getSessionId(), cmdName, params));
                log.info(res.toString()); // TODO: #toString() はあんまりなのでもうちょっとどうにかする
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return SUCCESS;
    }
}
