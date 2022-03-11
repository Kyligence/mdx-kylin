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

public class ExceptionUtils {

    public static String getRootCause(Throwable t) {
        Throwable origin = t;
        while (t != null) {
            if (t.getCause() == null) {
                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.toString();
                }
                return msg;
            }
            t = t.getCause();
        }
        if (origin == null) {
            return "";
        }
        return origin.getMessage();
    }

    public static String getFormattedErrorMsg(Throwable t, ErrorCode errorCode) {
        String rootCause = getRootCause(t);
        return getFormattedErrorMsg(rootCause, errorCode);
    }

    public static String getFormattedErrorMsg(String msg, ErrorCode errorCode) {
        if (msg.startsWith("[MDX-")) {
            return msg;
        }
        if (errorCode == null) {
            return "[MDX-00000001] Internal Error, " + msg;
        }
        return "[" + errorCode.getCode() + "] " + msg;
    }

}
