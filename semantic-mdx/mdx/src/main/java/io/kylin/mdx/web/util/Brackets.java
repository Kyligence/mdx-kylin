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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Brackets {
    private final Brackets parent;
    private final BracketType type;
    private final int startIndex;
    private int endIndex;
    private final List<Brackets> children;
    private final List<Integer> commas;

    public Brackets(Brackets parent, BracketType type, int startIndex) {
        this.parent = parent;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = -1;
        this.children = new ArrayList<>();
        this.commas = new ArrayList<>();
    }

    public void flattenParentheses(String originalString, StringBuilder sb) {
        if (type != BracketType.Parentheses) {
            sb.append(type.getLeftChar());
        }

        if (children.isEmpty()) {
            sb.append(originalString, startIndex + 1, endIndex);
        } else {
            sb.append(originalString, startIndex + 1, children.get(0).startIndex);
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                children.get(i).flattenParentheses(originalString, sb);
            }
            sb.append(originalString, children.get(children.size() - 1).getEndIndex() + 1, endIndex);
        }

        if (type != BracketType.Parentheses) {
            sb.append(type.getRightChar());
        }
    }
}
