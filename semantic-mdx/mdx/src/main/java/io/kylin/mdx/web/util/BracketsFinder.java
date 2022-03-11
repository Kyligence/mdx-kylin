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


package io.kylin.mdx.web.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class BracketsFinder {
    public static Brackets find(String mdx, int offset) {
        int pIndex = mdx.indexOf('(', offset);
        int bIndex = mdx.indexOf('{', offset);
        int startIndex = Math.min(
                pIndex < 0 ? Integer.MAX_VALUE : pIndex,
                bIndex < 0 ? Integer.MAX_VALUE : bIndex);
        char startChar = mdx.charAt(startIndex);

        Brackets root = new Brackets(null, BracketType.fromChar(startChar), startIndex);

        Deque<Brackets> stack = new ArrayDeque<>();
        stack.push(root);

        for (int i = startIndex + 1; i < mdx.length() && !stack.isEmpty(); i++) {
            char currentChar = mdx.charAt(i);
            Brackets brackets;
            switch (currentChar) {
                case '(':
                case '{':
                    brackets = new Brackets(stack.peek(), BracketType.fromChar(currentChar), i);
                    assert stack.peek() != null;
                    stack.peek().getChildren().add(brackets);
                    stack.push(brackets);
                    break;
                case ')':
                    brackets = stack.pop();
                    if (brackets.getType() != BracketType.Parentheses) {
                        throw new IllegalArgumentException("Parentheses matching failed.");
                    }
                    brackets.setEndIndex(i);
                    break;
                case '}':
                    brackets = stack.pop();
                    if (brackets.getType() != BracketType.Braces) {
                        throw new IllegalArgumentException("Braces matching failed.");
                    }
                    brackets.setEndIndex(i);
                    break;
                case ',':
                    assert stack.peek() != null;
                    stack.peek().getCommas().add(i);
                    break;
                default:
                    break;
            }
        }

        return root;
    }
}
