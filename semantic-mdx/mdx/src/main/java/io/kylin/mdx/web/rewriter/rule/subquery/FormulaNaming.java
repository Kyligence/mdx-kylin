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



package io.kylin.mdx.web.rewriter.rule.subquery;

import io.kylin.mdx.web.rewriter.SimpleValidator;
import mondrian.olap.AxisOrdinal;
import mondrian.olap.Id;
import mondrian.olap.QueryAxis;

/**
 * 用于给 Formula 语句中 Set 和 Member 命名
 */
public class FormulaNaming {

    private static final String SET_ROW_PREFIX = "XL_Row_Dim_";

    private static final String SET_COL_PREFIX = "XL_Col_Dim_";

    private int colCnt = 0;

    private int rowCnt = 0;

    private int slicer = 0;

    public Id newSetName(QueryAxis axis) {
        String dimName;
        if (axis.getAxisOrdinal() == AxisOrdinal.StandardAxisOrdinal.COLUMNS) {
            dimName = SET_COL_PREFIX + (colCnt++);
        } else {
            dimName = SET_ROW_PREFIX + (rowCnt++);
        }
        return new Id(new Id.NameSegment(dimName, Id.Quoting.QUOTED));
    }

    public Id newMemberName(SimpleValidator validator, String hierarchy) {
        String dimName = hierarchy + ".[Slicer_" + (slicer++) + "]";
        return (Id) validator.parseExpression(dimName);
    }

}
