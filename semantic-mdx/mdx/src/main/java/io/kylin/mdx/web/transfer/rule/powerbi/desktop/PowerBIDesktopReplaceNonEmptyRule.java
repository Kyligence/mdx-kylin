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


package io.kylin.mdx.web.transfer.rule.powerbi.desktop;

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;

import java.util.ArrayDeque;
import java.util.Deque;

public class PowerBIDesktopReplaceNonEmptyRule extends MdxTransferRule {

    /**
     * Replace NonEmpty function with "subst NonEmpty(%1,%2)=Filter(%1,not isEmpty(%2.Item(0)))"
     * <b>(Only deal with upper class "NONEMPTY")</b>
     * @param mdx The input MDX statement to process.
     * @return The MDX statement with NONEMPTY replaced if the input is a valid MDX statement, or the
     * original statement if it is not.
     */
    @Override
    public String apply(String mdx) {
        int functionStart = mdx.indexOf("NONEMPTY(");
        while (functionStart > 0) {
            String prefix = mdx.substring(0, functionStart);
            String functionBody = mdx.substring(functionStart + 9);

            //Position of the comma which splits the two parameters
            int parameterSplit = -1;
            //Position of the right parenthesis which indicates the function end
            int functionEnd = -1;

            Deque<Character> stack = new ArrayDeque<>();
            stack.push('(');
            char[] functionBodyChars = functionBody.toCharArray();

                /*Find the position of NONEMPTY's parameters %1 and %2.
                A parameter may contain nested functions or lists, which have extra commas,
                so we should check brackets to find the correct position of parameters.*/
            for (int i = 0; i < functionBodyChars.length && functionEnd < 0; i++) {
                char currentChar = functionBodyChars[i];
                switch (currentChar) {
                    case '(':
                    case '{':
                        stack.push(currentChar);
                        break;
                    case ')':
                        if (stack.size() == 0 || stack.pop() != '(') {
                            return mdx;
                        } else if (stack.size() == 0 && parameterSplit > 0) {
                            functionEnd = i;
                        }
                        break;
                    case '}':
                        if (stack.size() == 0 || stack.pop() != '{') {
                            return mdx;
                        }
                        break;
                    case ',':
                        if (stack.size() == 1 && parameterSplit < 0) {
                            parameterSplit = i;
                        }
                        break;
                }
            }

            if (parameterSplit > 0 && functionEnd > parameterSplit + 1) {
                String parameter1 = functionBody.substring(0, parameterSplit);
                String parameter2 = functionBody.substring(parameterSplit + 1, functionEnd);
                String suffix = functionBody.substring(functionEnd + 1);

                //No need to use StringBuilder with JDK >=8
                StringBuilder replacedStatement = new StringBuilder();
                replacedStatement.append(prefix);
                replacedStatement.append("FILTER(");
                replacedStatement.append(parameter1);
                replacedStatement.append(",NOT ISEMPTY(");
                replacedStatement.append(parameter2);
                replacedStatement.append(".ITEM(0))");
                replacedStatement.append(')');
                replacedStatement.append(suffix);
                mdx = replacedStatement.toString();
            }

            functionStart = mdx.indexOf("NONEMPTY(");
        }

        return mdx;
    }
}
