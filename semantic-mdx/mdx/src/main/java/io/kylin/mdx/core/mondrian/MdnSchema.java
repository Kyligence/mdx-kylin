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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.kylin.mdx.core.mondrian.physicalschema.MdnPhysicalSchema;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "Schema")
public class MdnSchema {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "metamodelVersion", isAttribute = true)
    private String version = "4.0";

    @JacksonXmlProperty(localName = "PhysicalSchema")
    private MdnPhysicalSchema mdnPhysicalSchema;

    @JacksonXmlProperty(localName = "Cube")
    private MdnCube mdnCube;
}
