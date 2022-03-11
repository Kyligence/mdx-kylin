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

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;

public class DecryptionCommand implements CommandLineExecutor {
    private static final int ARGS_LENGTH = 2;

    @Override
    public void execute(String[] args) {
        if (args == null || args.length != ARGS_LENGTH) {
            System.out.println("Only allow two args. Please check args size\n");
            return;
        }

        try {
            String cipherTxt = args[1];
            String clearTxt = AESWithECBEncryptor.decrypt(cipherTxt);
            System.out.println("\nThe decryption string: " + clearTxt);
        } catch (PwdDecryptException e) {
            e.printStackTrace();
        }

    }
}
