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


package io.kylin.mdx;

import com.google.common.collect.Lists;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.data.ToolLauncher;
import io.kylin.mdx.insight.data.command.CommandDirector;
import io.kylin.mdx.insight.data.command.CommandLineExecutor;
import io.kylin.mdx.insight.data.command.DecryptionCommand;
import io.kylin.mdx.insight.data.command.EncryptionCommand;
import io.kylin.mdx.insight.data.deleter.QueryLogDeleterCommand;
import io.kylin.mdx.insight.data.upgrade.ConfAndMetadataUpgradeCommand;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ApplicationRunnerTest {

    static {
        System.setProperty("MDX_HOME", "src/test/resources/");
        System.setProperty("MDX_CONF", "src/test/resources/conf");
    }

    @Test
    public void testToolLaucher() {
        String args[] = {"no-exec"};
        try {
            ToolLauncher.main(args);
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void testCommander() {

        CommandLineExecutor upgrade = CommandDirector.direct("upgrade");
        Assert.assertTrue(upgrade instanceof ConfAndMetadataUpgradeCommand);

        CommandLineExecutor deleteQueryLog = CommandDirector.direct("deleteQueryLog");
        Assert.assertTrue(deleteQueryLog instanceof QueryLogDeleterCommand);

        CommandLineExecutor encrypt = CommandDirector.direct("encrypt");
        Assert.assertTrue(encrypt instanceof EncryptionCommand);

        CommandLineExecutor decrypt = CommandDirector.direct("decrypt");
        Assert.assertTrue(decrypt instanceof DecryptionCommand);

        DecryptionCommand decryptionCommandSpy = spy(DecryptionCommand.class);
        decryptionCommandSpy.execute(new String[]{"decrypt", "2303a047dba718ea3c259a59835f95c4"});
        verify(decryptionCommandSpy, times(1)).execute(any());

        EncryptionCommand encryptionCommandSpy = spy(EncryptionCommand.class);
        encryptionCommandSpy.execute(new String[]{"encrypt", "root"});
        verify(encryptionCommandSpy, times(1)).execute(any());

    }

    @Test
    public void testRunner() throws SemanticException {

        CommandLineExecutor executor = CommandDirector.direct("upgrade");

        if (executor instanceof ConfAndMetadataUpgradeCommand) {
            ConfAndMetadataUpgradeCommand upgrade = (ConfAndMetadataUpgradeCommand) executor;
            upgrade.run(new Arguments());
        } else {
            throw new RuntimeException("command class cast error");
        }
    }

    public static class Arguments implements ApplicationArguments {

        @Override
        public String[] getSourceArgs() {
            return new String[0];
        }

        @Override
        public Set<String> getOptionNames() {
            return null;
        }

        @Override
        public boolean containsOption(String name) {
            return false;
        }

        @Override
        public List<String> getOptionValues(String name) {
            if ("cur_version".equals(name)) {
                return Lists.newArrayList("0.25.0");
            }

            if ("new_version".equals(name)) {
                return Lists.newArrayList("0.25.0");
            }

            return Lists.newArrayList("03a.27.0");
        }

        @Override
        public List<String> getNonOptionArgs() {
            return null;
        }
    }
}
