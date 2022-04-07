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


package io.kylin.mdx.insight.engine.service.parser;

import mondrian.olap.Exp;
import mondrian.olap.type.TypeUtil;
import mondrian.resource.MondrianResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NamedSetParserImpl extends CalcMemberParserImpl {

    private List<String> dimensionForLocation = new ArrayList<>();

    public NamedSetParserImpl() {
        super();
    }

    @Override
    public void checkResultExpType(Exp validatedExp, String snippet) {
        if (!TypeUtil.isSet(validatedExp.getType())) {
            throw MondrianResource.instance().MdxSetExpNotSet.ex(snippet);
        }
    }

    public List<String> getDimensionForLocation() {
        return dimensionForLocation;
    }

    public void addDimensionForLocation(List<String> dimensionForLocation) {
        this.dimensionForLocation.addAll(dimensionForLocation);
    }

}
