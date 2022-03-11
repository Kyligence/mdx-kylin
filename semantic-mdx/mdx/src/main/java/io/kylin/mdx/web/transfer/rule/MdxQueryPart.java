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

package io.kylin.mdx.web.transfer.rule;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MdxQueryPart {

    String preClause;

    String columnsStr;

    String rowsStr;

    String postClause;

    @Override
    public String toString() {
        if (columnsStr == null && rowsStr == null) {
            return null;
        } else if (columnsStr == null) {
            return preClause + rowsStr + " " + postClause;
        } else if (rowsStr == null) {
            return preClause + columnsStr + " " + postClause;
        } else {
            return preClause + columnsStr + ", " + rowsStr + postClause;
        }
    }
}
