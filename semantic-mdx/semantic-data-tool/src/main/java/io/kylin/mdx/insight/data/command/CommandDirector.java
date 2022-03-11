/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.data.command;

import io.kylin.mdx.insight.data.deleter.QueryLogDeleterCommand;
import io.kylin.mdx.insight.data.upgrade.ConfAndMetadataUpgradeCommand;

public enum CommandDirector {

    /**
     * metadata and config upgrade
     */
    DataUpgrade("upgrade", new ConfAndMetadataUpgradeCommand()),

    /**
     * encryption tool
     */
    Encryption("encrypt", new EncryptionCommand()),

    /**
     * decryption tool
     */
    Decryption("decrypt", new DecryptionCommand()),

    /**
     * datasets export tool
     */
    Log("log", new LogFilterCommand()),

    /**
     * periodically delete database log data tool
     */
    Deleter("deleteQueryLog", new QueryLogDeleterCommand());

    private String command;

    private CommandLineExecutor commandLineExecutor;

    CommandDirector(String command, CommandLineExecutor commandLineExec) {
        this.command = command;
        this.commandLineExecutor = commandLineExec;
    }

    public static CommandLineExecutor direct(String command) {

        if (DataUpgrade.command.equalsIgnoreCase(command)) {
            return DataUpgrade.commandLineExecutor;
        } else if (Encryption.command.equalsIgnoreCase(command)) {
            return Encryption.commandLineExecutor;
        } else if (Decryption.command.equalsIgnoreCase(command)) {
            return Decryption.commandLineExecutor;
        } else if (Log.command.equalsIgnoreCase(command)) {
            return Log.commandLineExecutor;
        } else if (Deleter.command.equalsIgnoreCase(command)){
            return Deleter.commandLineExecutor;
        }else {
            return CommandLineExecutor.NEVER_EXEC;
        }
    }
}
