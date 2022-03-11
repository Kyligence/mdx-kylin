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


package io.kylin.mdx.insight.mondrian.query;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.DimensionCol;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.DimensionTable;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.Hierarchy;
import io.kylin.mdx.insight.engine.service.parser.CalcMemberParserImpl;
import io.kylin.mdx.insight.engine.service.parser.NamedSetParserImpl;
import io.kylin.mdx.insight.engine.support.ExprParseException;
import mondrian.calc.impl.BetterExpCompiler;
import mondrian.mdx.MdxVisitorImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.Property;
import mondrian.olap.Validator;
import mondrian.rolap.RolapSchema;
import mondrian.xmla.XmlaRequestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

public class CalcMemberTest extends BaseEnvSetting {

    private static final String line = System.getProperty("line.separator");

    private static final String format_String = "FORMAT_STRING";
    private static final String member_scope = "$member_scope";
    private static final String member_ordinal = "MEMBER_ORDINAL";


    private static final String template = "WITH MEMBER [Measures].[xaa] as %s select from cube1";

    private static final String[] snippers = {
            "sum([DATES].[Cal-Hierarchy].AllMembers,[Measures].[QTY_INT])",
            "1+2+3+4-3",
            "sum([DATES].[Cal-Hierarchy].AllMembers,[Measures].[QTY_INT])",
            //字符串用三个单引号。当鉴别到formula有单引号时，多添加两个单引号
            "'''daf'''",
            "-2",
            "abs(-2.32)",
            "acos(465)",
            "aggregate([DATES].[Cal-Hierarchy].AllMembers,[Measures].[QTY_INT])",
            "count(Ascendants([DATES].[Cal-Hierarchy]))",
            "asin(0.5)",
            "Atanh(0.5)",
            "avg([DATES].[D_YEAR].MEMBERS,[Measures].[QTY_INT])",
            "Cdate(10)",
            "CoalesceEmpty([Measures].[QTY_INT])",
            "Correlation([DATES].[Cal-Hierarchy].Members,[Measures].[QTY_INT])",
            "cos(3)",
            "count([DATES].[Cal-Hierarchy].Level.Members)",
            "[DATES].[Cal-Hierarchy].members.Count",
            "cousin([DATES].[Cal-Hierarchy].[D_Month].[2],[DATES].[D_YEAR].[1993])",
            "covariance([DATES].[Cal-Hierarchy].Members,[Measures].[CMV_INT])",
            "sum(Crossjoin([DATES].[Cal-Hierarchy].Members,[CUSTOMER].[C_NAME].Members),[Measures].[QTY_INT])",
            "CurrentDATEString(\"yyyy-MM-dd\")",
            "[DATES].[D_YEAR].currentMember is empty",
            "[DATES].[D_YEAR].DataMember",
            "Date()",
            "dateadd(\"m\",1,now())",
            "datediff(\"y\",now(),now())",
            "datepart(\"y\",now())",
            "Day(Now())",
            "[DATES].[Cal-Hierarchy].DefaultMember.Name",
            "degrees(12)",
            "SUM(Distinct([DATES].[Cal-Hierarchy].Children),[Measures].[QTY_INT])",
            "SUM(DrilldownLevel([DATES].[Cal-Hierarchy].Children),[Measures].[QTY_INT] )",
            "sum(DrilldownLevelBottom([DATES].[Cal-Hierarchy].Children , [Measures].[QTY_INT] ),[Measures].[QTY_INT])",
            "sum(DRILLDOWNLEVELTOP( [DATES].[Cal-Hierarchy].Children , [Measures].[QTY_INT] ),[Measures].[QTY_INT] )",
            "sum(DrilldownMember ( [DATES].[Cal-Hierarchy].Children, {[DATES].[Cal-Hierarchy].[D_YEAR].[1993]} ),[Measures].[QTY_INT])",
            "count(Except([DATES].[D_YEAR].MEMBERS,{[DATES].[D_YEAR].[1993]}))",
            "count(EXISTS([DATES].[D_YEAR].MEMBERS,{[DATES].[D_YEAR].[1993]}))",
            "Exp(23)",
            "[DATES].[Cal-Hierarchy].firstMember.FirstChild",
            "[DATES].[Cal-Hierarchy].CurrentMember.FirstSibling.Name",
            "Fix(9999.9999)",
            "Format(Now(),'yyyy-mm-dd')",
            "FormatCurrency(99999.999)",
            "FormatPercent(0.23,8)",
            "[DATES].[Cal-Hierarchy].CurrentMember.Hierarchy",
            "Hour(now())",
            "iif(1=0 or 1=1 ,1,2)",
            "[DATES].[D_YEAR] is [CUSTOMER].[C_NAME]",
            "[DATES].[Cal-Hierarchy].CurrentMember IS EMPTY",
            "[DATES].[Cal-Hierarchy].CurrentMember IS NULL",
            "InStr( \"abcdefghijklmnñopqrstuvwxyz\", \"o\") ",
            "Int(10.999)",
            "IsDate('dafas')",
            "IsEmpty([Measures].[QTY_INT])",
            "LCase(\"ANCBHJ\")",
            "LTrim(\"kjljlksdjlfj\")",
            "[DATES].[Calendar].CurrentMember.Lag(-2) ",
            "[DATES].[Cal-Hierarchy].CurrentMember.LastSibling",
            "[DATES].[Cal-Hierarchy].CurrentMember.Lead(-1).Name",
            "Left(\"abcdefg\",2)",
            "len(\"sdkjflsjdaflkjsdflkj\")",
            "Log(100)",
            "Log10(100)",
            "MAX([DATES].[D_MONTHNUMINYEAR].members,[Measures].[QTY_INT])",
            "Median([DATES].[D_MONTHNUMINYEAR].members,[Measures].[QTY_INT])",
            "SUM(Order([DATES].[D_YEAR].members,[Measures].[QTY_INT],BDESC))",
            "sum(Members('[DATES].[Cal-Hierarchy].[D_YEAR].&[1993] '),[Measures].[QTY_INT])",
            "Mid(\"abcd\",2,2)",
            "Minute(now())",
            "MonthName(10,1>2)",
            "SUM(MTD([DATES].[Cal-Hierarchy].[member]),[Measures].[QTY_INT])", //need the default hierarchy in this cube
            "[DATES].[D_YEAR].name",
            "[DATES].[D_YEAR].nextmember.name",
            "oct(999)",
            "SUM(Order([DATES].[Cal-Hierarchy].[D_YEAR].members,[Measures].[QTY_INT],BDESC))",
            "[DATES].[Cal-Hierarchy].[D_YEAR].ordinal",
            "AGGREGATE(PARALLELPERIOD([DATES].[Cal-Hierarchy].[D_YEAR], 1), [Measures].[QTY_INT])",
            "Pi()",
            "[DATES].[Cal-Hierarchy].&[D_YEAR].Parent.Name",
            "SUM(QTD(),[Measures].[QTY_INT])",
            "AGGREGATE(YTD(PARALLELPERIOD([DATES].[Cal-Hierarchy].[D_YEAR], 1)) , [Measures].[SUM_PRICE])",
            "ClosingPeriod()",
            "openingPeriod()", //need the default hierarchy in this cube
            "right(\"likjdlksjalfkdjflkskjaf\",2)",
            "round(10.8434)",
            "second(now())",
            "SetToStr ([DATES].[D_YEAR].Children)",
            "sin(10)",
            "sqr(9)",
            "Stddev({[DATES].[D_YEAR].[1992],[DATES].[D_YEAR].[1993],[DATES].[D_YEAR].[1994]},[Measures].[QTY_INT])",
            "str(100)",
            "StrToMember ('[DATES].[D_YEAR].[1993]')",
            "SUM(Subset([DATES].[Cal-Hierarchy].[D_YEAR].members,1,2),[Measures].[QTY_INT])",
            "SUM(tail([DATES].[Cal-Hierarchy].[D_YEAR].members,2),[Measures].[QTY_INT])",
            "tan(10)",
            "ThirdQ({[DATES].[D_YEAR].[1992],[DATES].[D_YEAR].[1993],[DATES].[D_YEAR].[1994]},[Measures].[QTY_INT])",
            "Time()",
            "TimeSerial(10,10,10)",
            "TimeValue(Now())",
            "Timer()",
            "count(TopCount({[DATES].[Cal-Hierarchy].[D_YEAR].members},2,[Measures].[Reseller Sales Amount]))",
            "trim(' da  ') ",
            "TupleToStr(([DATES].[Cal-Hierarchy].&[1993],[CUSTOMER].[C_NATION].&[MOROCCO]))",
            "TypeName(10)",
            "Ucase(\"sdjlkfjlksdjflsajfldkjafl\")",
            "[DATES].[D_YEAR].UniqueName",
            "val('af23')",
            "Var([DATES].[Cal-Hierarchy].[D_YEAR].members,[Measures].[QTY_INT])",
            "WeekDay(now())",
            "SUM(WTD(),[Measures].[QTY_INT])",
            "1=0 XOR 1=1",
            "year(now())",
            "case [CUSTOMER].[C_REGION].currentmember.name when \"AFRICA\" then \"1\" else \"2\" end",
            "count([DATES].[Cal-Hierarchy].[被改了].children)"
    };

    private static final String[] badSnippets = {
            "1+2+3-",
            "(1>2) > 3",
            "abs(-2.323.23)",
            "abs(-2.323.23)",
            "Ascendants(count(Ascendants([Date].[fda])),[DATES].[Cal-Hierarchy])",
            "LastPeriods(2,[DATES].[Cal-Hierarchy].[D_YEAR].[1993])", //is set
            "[DATES].[Cal-Hierarchy].CurrentMember.Level", //is set
            "[DATES].[Cal-Hierarchy].Levels(1)", //is set
            "[DATES].[Dim_year].[1993].siblings"
    };


    private static final String[] named_set_snippets = {
            "TopCount(tail([DATES].[Cal-Hierarchy].members, 2), 5, [Measures].[GVM_CNT])",
            "TopCount([DATES].[Cal-Hierarchy].members, 5, [Measures].[GVM_CNT_CM])",
            "filter([top_seller], [Measures].[GVM_CNT] > 100)"
    };

    private static final String[] named_set_snippets_expression = {
            "filter([top_seller], [Measures].[GVM_CNT] > 100)",
    };


    private static final String[] named_set_bad_snippets = {
            "Count([KYLIN_SALES].[year].[year].members)",
            "cos(1)",
            "[DATES].[Cal-Hierarchy].[被改了].members"
    };

    private SimpleSchema simpleSchema;

    @Before
    public void init() {
        simpleSchema = createSchema();
    }


    @Test
    public void testCM() throws ExprParseException {
        CalcMemberParserImpl calcMemberParserImpl = new CalcMemberParserImpl();
        for (int i = 0; i < snippers.length; i++) {
            calcMemberParserImpl.parse(snippers[i], simpleSchema);
        }

        for (int i = 0; i < badSnippets.length; i++) {
            try {
                calcMemberParserImpl.parse(badSnippets[i], simpleSchema);
                throw new RuntimeException("calcMember validating bad snippet can't go here");
            } catch (Exception e) {
                if (!(e instanceof ExprParseException) && !(e instanceof MondrianException)) {
                    throw e;
                }
            }
        }
        XmlaRequestContext context = new XmlaRequestContext();
        calcMemberParserImpl.checkConnect("test", "top_user", "admin");
        calcMemberParserImpl.clearMapNameSetForLocation();
    }

    @Test
    public void testNamedSet() throws ExprParseException {
        NamedSetParserImpl namedSetParserImpl = new NamedSetParserImpl();
        for (int i = 0; i < named_set_snippets.length; i++) {
            namedSetParserImpl.parse(named_set_snippets[i], simpleSchema);
        }

        for (int i = 0; i < named_set_bad_snippets.length; i++) {
            try {
                namedSetParserImpl.parse(named_set_bad_snippets[i], simpleSchema);
                throw new RuntimeException("calcMember validating bad snippet can't go here");
            } catch (Exception e) {
                if (!(e instanceof ExprParseException) && !(e instanceof MondrianException)) {
                    throw e;
                }
            }
        }
    }
    @Test
    public void testNamedSetExpression() throws ExprParseException {
        SimpleSchema schema = createSchema();
        NamedSetParserImpl namedSetParserImpl = new NamedSetParserImpl();
        SimpleSchema.NamedSet ns = new SimpleSchema.NamedSet();
        ns.setName("top_seller");
        ns.setExpression("TopCount(tail([DATES].[Cal-Hierarchy].members, 2), 5, [Measures].[GVM_CNT])");
        Set<SimpleSchema.NamedSet> namedsets = Sets.newHashSet(ns);
        schema.setNamedSets(namedsets);
        for (int i = 0; i < named_set_snippets_expression.length; i++) {
            namedSetParserImpl.parse(named_set_snippets_expression[i], schema);
        }
    }
    @Test
    public void testInnerClass() {
        CalcMemberParserImpl.MockLevel mockLevel = new CalcMemberParserImpl.MockLevel("1");
        Assert.assertTrue(mockLevel.getDepth() == 0);
        Assert.assertNull(mockLevel.getDescription());
        Assert.assertNull(mockLevel.lookupChild(null, null, null));
        Assert.assertNull(mockLevel.getQualifiedName());
        Assert.assertNull(mockLevel.getCaption());
        Assert.assertNull(mockLevel.getLocalized(null, null));
        Assert.assertFalse(mockLevel.isVisible());
        Assert.assertNull(mockLevel.getChildLevel());
        Assert.assertNull(mockLevel.getParentLevel());
        Assert.assertFalse(mockLevel.isAll());
        Assert.assertFalse(mockLevel.areMembersUnique());
        Assert.assertNull(mockLevel.getLevelType());
        Assert.assertNotNull(mockLevel.getProperties());
        Assert.assertNull(mockLevel.getMemberFormatter());
        Assert.assertTrue(mockLevel.getApproxRowCount() == 0);
        Assert.assertNull(mockLevel.getAnnotationMap());
        Assert.assertNull(mockLevel.getName());
        CalcMemberParserImpl.MockMember mockMemberT =  new CalcMemberParserImpl.MockMember("1.2");
        CalcMemberParserImpl.MockMember mockMember =  new CalcMemberParserImpl.MockMember("1.2.3");

        Assert.assertNull(mockMember.getParentMember());
        Assert.assertNull(mockMember.getDescription());
        Assert.assertNull(mockMember.lookupChild(null, null, null));
        Assert.assertNull(mockMember.getQualifiedName());
        Assert.assertNull(mockMember.getCaption());
        Assert.assertNull(mockMember.getLocalized(null, null));
        Assert.assertFalse(mockMember.isVisible());
        Assert.assertNull(mockMember.getParentUniqueName());
        Assert.assertNull(mockMember.getValue());
        Assert.assertNotNull(mockMember.getMemberType());
        Assert.assertFalse(mockMember.isParentChildLeaf());
        mockMember.setName("1");
        Assert.assertFalse(mockMember.isAll());
        Assert.assertFalse(mockMember.isMeasure());
        Assert.assertFalse(mockMember.isNull());
        Assert.assertFalse(mockMember.isChildOrEqualTo(null));
        Assert.assertFalse(mockMember.isCalculated());
        Assert.assertFalse(mockMember.isEvaluated());
        Assert.assertTrue(mockMember.getSolveOrder() == 0);
        Assert.assertNull(mockMember.getExpression());
        Assert.assertNull(mockMember.getAncestorMembers());
        Assert.assertFalse(mockMember.isCalculatedInQuery());
        Assert.assertNull(mockMember.getPropertyValue(new Property("1", null, 1, true, true, true, null)));
        Assert.assertNull(mockMember.getPropertyValue("1"));
        Assert.assertNull(mockMember.getPropertyValue("1", true));
        Assert.assertNull(mockMember.getPropertyFormattedValue(new Property("1", null, 1, true, true, true, null)));
        Assert.assertNull(mockMember.getPropertyFormattedValue("1"));
        mockMember.setProperty(new Property("1", null, 1, true, true, true, null), null);
        mockMember.setProperty("1", null);
        Assert.assertNotNull(mockMember.getProperties());
        Assert.assertTrue(mockMember.getOrdinal() == 0);
        Assert.assertNull(mockMember.getOrderKey());
        Assert.assertFalse(mockMember.isHidden());
        Assert.assertTrue(mockMember.getDepth() == 0);
        Assert.assertNull(mockMember.getDataMember());
        Assert.assertTrue(mockMember.compareTo(null) == 0);
        Assert.assertNull(mockMember.getAnnotationMap());
        Assert.assertNull(mockMember.getName());

        CalcMemberParserImpl.MockHierarchy mockHierarchyT = new CalcMemberParserImpl.MockHierarchy("1.2.3");
        CalcMemberParserImpl.MockHierarchy mockHierarchy = new CalcMemberParserImpl.MockHierarchy("1");
        Assert.assertNotNull(mockHierarchy.getDescription());
        Assert.assertNull(mockHierarchy.lookupChild(null, null, null));
        Assert.assertNull(mockHierarchy.getQualifiedName());
        Assert.assertNull(mockHierarchy.getCaption());
        Assert.assertNull(mockHierarchy.getLocalized(null, null));
        Assert.assertNull(mockHierarchy.getHierarchy());
        Assert.assertFalse(mockHierarchy.isVisible());
        Assert.assertNull(mockHierarchy.getLevelList());
        Assert.assertNotNull(mockHierarchy.getLevels());
        Assert.assertNull(mockHierarchy.getDefaultMember());
        Assert.assertNull(mockHierarchy.getAllMember());
        Assert.assertNull(mockHierarchy.getNullMember());
        Assert.assertFalse(mockHierarchy.hasAll());
        Assert.assertNull(mockHierarchy.createMember(null, null, "1", null));
        Assert.assertNull(mockHierarchy.getUniqueNameSsas());
        Assert.assertNull(mockHierarchy.getAnnotationMap());
        Assert.assertNull(mockHierarchy.getName());
        mockHierarchy.toString();
        Assert.assertFalse(mockHierarchy.equals(mockHierarchyT));
        mockHierarchy.hashCode();

        CalcMemberParserImpl.ExprAdaptor exprAdaptor = new CalcMemberParserImpl.ExprAdaptor();
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.clone());
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.getCategory());
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.getType());
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.unparse(null));
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.accept(new CalcMemberParserImpl.MondrianValidatorAdaptor(new RolapSchema.RolapSchemaFunctionTable(Collections.emptyList()))));
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.accept(new BetterExpCompiler(null, null)));
        Assert.assertThrows(UnsupportedOperationException.class, () ->exprAdaptor.accept(new MdxVisitorImpl()));

        CalcMemberParserImpl.MockDimensionExpr mockDimensionExpr = new CalcMemberParserImpl.MockDimensionExpr();
        mockDimensionExpr.getCategory();
        mockDimensionExpr.getType();
        mockDimensionExpr.accept(new CalcMemberParserImpl.MondrianValidatorAdaptor(new RolapSchema.RolapSchemaFunctionTable(Collections.emptyList())));

        CalcMemberParserImpl.MockDimension mockDimension = new CalcMemberParserImpl.MockDimension();
        mockDimension.getUniqueName();
        mockDimension.getDescription();
        mockDimension.lookupChild(null, null, null);
        mockDimension.getQualifiedName();
        mockDimension.getCaption();
        mockDimension.getLocalized(null, null);
        mockDimension.getHierarchy();
        mockDimension.getDimension();
        mockDimension.isVisible();
        mockDimension.getHierarchies();
        mockDimension.isMeasures();
        mockDimension.getDimensionType();
        mockDimension.getSchema();
        mockDimension.getAnnotationMap();
        mockDimension.getName();
    }

    private String createMDX() {


        /*something wrong*/
        String snipper41 = "count(EXTRACT([DATES].[D_YEAR].Members, [DATES].[Cal-Hierarchy]))";
        String snipper65 = "LinRegIntercept(LastPeriods(10),[Measures].[COUNT_ORDER_INT],[Measures].[QTY_INT])";
        String snipper68 = "MIRR([DATES].[D_MONTHNUMINYEAR].members,[Measures].[QTY_INT])"; //not array at args[0]
        String snipper117 = "'''dfa''' || '''dfafdaf'''";

        return String.format(template, "da");
    }


//    public void testCM() {
//        StringBuilder sb = new StringBuilder();
//
//        String fqName = "[Measures].[Measures].[name]";
//        String expr = "aggregate(ytd(),[Measure].[price])";
//        String formatStr = "#,###.00";
//        sb.append("WITH")
//                .append(line)
//                .append("MEMBER ")
//                .append(fqName)
//                .append(line)
//                .append(" AS ")
//                .append("'").append(expr).append("'")
//                .append(",").append(line)
//                .append(format_String).append(" = ")
//                .append("\"").append(formatStr).append("\"")
//                .append(", ").append(line)
//                .append(member_scope).append("= 'CUBE'")
//                .append(",").append(line)
//                .append(member_ordinal).append(" = 1").append(line);
//
//        sb.append("SELECT FROM ").append("cube1");
//
//    }

    private SimpleSchema createSchema() {
        SimpleSchema schema = new SimpleSchema();
        Set<String> measures = Sets.newHashSet("Reseller Sales Amount", "SUM_PRICE", "GVM_CNT");
        Set<String> cm = Sets.newHashSet("QTY_INT", "CMV_INT", "GVM_CNT_CM");
        SimpleSchema.NamedSet ns = new SimpleSchema.NamedSet();
        ns.setName("top_seller");
        SimpleSchema.NamedSet nst = new SimpleSchema.NamedSet();
        nst.setName("top2_seller");
        Set<SimpleSchema.NamedSet> namedsets = Sets.newHashSet(ns,nst);


        schema.setMeasureAliases(measures);
        schema.setCalcMeasureNames(cm);
        schema.setNamedSets(namedsets);

        Set<DimensionTable> dimensionTables = Sets.newHashSet();

        DimensionTable dimensionTable = new DimensionTable();
        dimensionTable.setAlias("DATES");
        dimensionTable.setTableColAliases(
                Sets.newHashSet(
                        new DimensionCol("D_YEAR", 0),
                        new DimensionCol("D_Month", 0),
                        new DimensionCol("Calendar", 0),
                        new DimensionCol("D_MONTHNUMINYEAR", 0)
                ));
        dimensionTable.setHierarchies(Sets.newHashSet(
                new Hierarchy("Cal-Hierarchy", Sets.newHashSet("D_YEAR", "D_Month"))
        ));

        DimensionTable dimensionTable2 = new DimensionTable();
        dimensionTable2.setAlias("CUSTOMER");
        dimensionTable2.setTableColAliases(
                Sets.newHashSet(
                        new DimensionCol("C_NAME", 0),
                        new DimensionCol("C_NATION", 0),
                        new DimensionCol("C_REGION", 0)
                ));


        dimensionTables.add(dimensionTable);
        dimensionTables.add(dimensionTable2);
        schema.setDimensionTables(dimensionTables);
        return schema;
    }
}
