// This code is based on:
//
//   com.thoughtworks.selenium.webdriven.commands.AllowNativeXPath
//
// in Selenium WebDriver.
//
// The following copyright is copied from original.
// ---
// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package jp.vmi.selenium.selenese.command;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.result.Error;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Warning;

import static jp.vmi.selenium.selenese.command.ArgumentType.*;
import static jp.vmi.selenium.selenese.result.Success.*;

/**
 * Re-implementation of AllowNativeXPath.
 */
public class AllowNativeXpath extends AbstractCommand {

    private static final int ARG_FLAG = 0;

    AllowNativeXpath(int index, String name, String... args) {
        super(index, name, args, VALUE);
    }

    @Override
    public boolean mayUpdateScreen() {
        return false;
    }

    @Override
    protected Result executeImpl(Context context, String... curArgs) {
        String flag = curArgs[ARG_FLAG];
        if (flag.isEmpty())
            return new Error("You must set this value to 'true' or 'false'");
        else if (!"true".equalsIgnoreCase(flag))
            return new Warning("Supported XPath is native only");
        return SUCCESS;
    }
}
