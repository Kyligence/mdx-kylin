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


package io.kylin.mdx.core.mondrian;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class MdnForeignKey {

    @JacksonXmlProperty(localName = "Column")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<MdnColumn> columns;

    public void addMdnColumns(List<String> fks) {
        if (columns == null) {
            columns = new LinkedList<>();
        }

        for (String fk : fks) {
            columns.add(new MdnColumn(fk));
        }

    }

    public void addMdnColumn(String fk) {
        if (columns == null) {
            columns = new LinkedList<>();
        }

        columns.add(new MdnColumn(fk));

    }
}
