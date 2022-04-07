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

import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.DimensionCol;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.DimensionTable;
import io.kylin.mdx.insight.engine.bean.SimpleSchema.Hierarchy;
import io.kylin.mdx.insight.engine.support.ExprParseException;
import lombok.Data;
import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.mdx.*;
import mondrian.olap.*;
import mondrian.olap.type.*;
import mondrian.parser.JavaccParserValidatorImpl;
import mondrian.parser.QueryPartFactoryImpl;
import mondrian.resource.MondrianResource;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapSchema;
import mondrian.server.MondrianServerRegistry;
import mondrian.server.StatementImpl;
import mondrian.spi.MemberFormatter;
import mondrian.xmla.XmlaRequestContext;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.*;

@Service
public class CalcMemberParserImpl implements ICalcMemberParser {

//    private static final String line = System.getProperty("line.separator");
//
//    private static final String format_String = "FORMAT_STRING";
//    private static final String member_scope = "$member_scope";
//    private static final String member_ordinal = "MEMBER_ORDINAL";

    private static final Exp UNKNOWN_EXP = new ExprAdaptor();

    private static final String SIMPLE_MDX_TEMPLATE = "WITH MEMBER [Measures].[1] as %s select from cube1";

    private final RolapSchema.RolapSchemaFunctionTable funTable = new RolapSchema.RolapSchemaFunctionTable(Collections.emptyList());

    private final Map<String, List<String>> mapNameSetForLocation = new HashMap<>();

    private String currentNameSet;

    public CalcMemberParserImpl() {
        funTable.init();
    }

    public void clearMapNameSetForLocation() {
        mapNameSetForLocation.clear();
    }
    @Override
    public void parse(String calcMemberSnippet, SimpleSchema simpleSchema) throws ExprParseException {
        MockInternalStatement mockInternalStmt = new MockInternalStatement();
        JavaccParserValidatorImpl javaccParser = new JavaccParserValidatorImpl(new QueryPartFactoryImpl(true));

        XmlaRequestContext context = new XmlaRequestContext();
        try {
            context.doValidateCM = true;

            QueryPart queryPart;
            try {
                queryPart = javaccParser.parseInternal(
                        mockInternalStmt,
                        new SimpleMDXBuilder(calcMemberSnippet).build(),
                        false,
                        funTable,
                        false
                );
            } catch (Exception e) {
                throw new ExprParseException("Parse " + calcMemberSnippet + " error.", e);
            }

            if (!(queryPart instanceof Query)) {
                throw new ExprParseException("Mdx parse error: QueryPart can't cast to Query class");
            }

            Query query = (Query) queryPart;

            MondrianValidatorAdaptor mondrianValidator = new MondrianValidatorAdaptor(funTable);
            mondrianValidator.setQuery(query);

            List<String> dimensionsForLocation = new ArrayList<>();

            for (Formula formula : query.getFormulas()) {
                CalcMemberExpressionValidator calcMemberExpressionValidator = createExprValidator(mondrianValidator, funTable, simpleSchema);
                Exp validatedExp = calcMemberExpressionValidator.validate(formula.getExpression(), "");

                checkResultExpType(validatedExp, calcMemberSnippet);

                checkTuple(formula);

                if (this instanceof NamedSetParserImpl) {
                    dimensionsForLocation.addAll(calcMemberExpressionValidator.getDimensionsForLocation());
                    String prevNameSet = currentNameSet;
                    for (String nameset : calcMemberExpressionValidator.getNamesetForLocation()) {
                        for (SimpleSchema.NamedSet namedSet : simpleSchema.getNamedSets()) {
                            if (namedSet.getName().equals(nameset)) {
                                if (!mapNameSetForLocation.containsKey(nameset)) {
                                    currentNameSet = nameset;
                                    if (namedSet.getExpression() != null && !"".equals(namedSet.getExpression())) {
                                        parse(namedSet.getExpression(), simpleSchema);
                                    }
                                    if (mapNameSetForLocation.containsKey(currentNameSet) && !mapNameSetForLocation.get(currentNameSet).isEmpty()) {
                                        dimensionsForLocation.addAll(mapNameSetForLocation.get(currentNameSet));
                                    }
                                } else {
                                    dimensionsForLocation.addAll(mapNameSetForLocation.get(nameset));
                                }
                                break;
                            }
                        }
                    }
                    currentNameSet = prevNameSet;
                    if (currentNameSet != null) {
                        if (!mapNameSetForLocation.containsKey(currentNameSet)) {
                            mapNameSetForLocation.put(currentNameSet, dimensionsForLocation);
                        }
                    }
                }
            }

            if (this instanceof NamedSetParserImpl) {
                ((NamedSetParserImpl) this).addDimensionForLocation(dimensionsForLocation);
            }
        } finally {
            context.clear();
        }
    }

    private void checkTuple(Formula formula) throws ExprParseException {
        Exp exp = formula.getExpression();
        if (!(exp instanceof UnresolvedFunCall)) {
            return;
        }
        Exp[] exps = ((UnresolvedFunCall) exp).getArgs();
        for (Exp exp1 : exps) {
            if ((exp1 instanceof UnresolvedFunCall && ((UnresolvedFunCall) exp1).getFunName() != null && "()".equals(((UnresolvedFunCall) exp1).getFunName()))) {
                Exp[] exp2s = ((UnresolvedFunCall) exp1).getArgs();
                Set<String> hierarchies = new HashSet<>();
                for (Exp exp3 : exp2s) {
                    String exp3String = exp3.toString();
                    int lastIndex = exp3String.lastIndexOf('.');
                    if (lastIndex < 1) {
                        continue;
                    }
                    exp3String = exp3String.substring(0, lastIndex);
                    if (hierarchies.contains(exp3String)) {
                        throw new ExprParseException("There are duplicate dimensions in tuple.");
                    }
                    hierarchies.add(exp3String);
                }
            }
        }
    }
    @Override
    public void checkConnect(String project, String dataset, String user) throws ExprParseException {
        try {
            String repositoryId = user.toUpperCase() + "_" + project + "_" + "datasources";
            MondrianServerRegistry.INSTANCE.checkDataset(repositoryId, dataset);
        } catch (Exception e) {
            throw new ExprParseException(e);
        }
    }

    private CalcMemberExpressionValidator createExprValidator(Validator validator,
                                                              RolapSchema.RolapSchemaFunctionTable funTable,
                                                              SimpleSchema simpleSchema) {
        return new CalcMemberExpressionValidator(validator, funTable, simpleSchema);
    }

    @Override
    public void checkResultExpType(Exp validatedExp, String snippet) {
        if (!TypeUtil.canEvaluate(validatedExp.getType())) {
            throw MondrianResource.instance().MdxMemberExpIsSet.ex(snippet);
        }
    }

    static class CalcMemberExpressionValidator {

        private Validator validator;

        private FunTable funTable;

        private OlapElementFinder olapElementFinder;

        private List<String> dimensionsForLocation = new ArrayList<>();

        private HashSet<String> namesetForLocation = new HashSet<>();

        CalcMemberExpressionValidator(Validator validator, FunTable funTable, SimpleSchema simpleSchema) {
            this.validator = validator;
            this.funTable = funTable;
            this.olapElementFinder = new OlapElementFinder(simpleSchema);
        }

        List<String> getDimensionsForLocation() {
            return dimensionsForLocation;
        }

        public HashSet<String> getNamesetForLocation() {
            return namesetForLocation;
        }

        Exp validate(Exp exp, String funName) throws ExprParseException {
            if (exp instanceof UnresolvedFunCall) {
                UnresolvedFunCall ufc = (UnresolvedFunCall) exp;
                Exp[] args = ufc.getArgs();
                Exp[] newArgs = new Exp[args.length];
                FunDef funDef = resolveFunctionArgs(args, newArgs, ufc.getFunName(), ufc.getSyntax());

                return funDef.createCall(validator, newArgs);
            } else if (exp instanceof Id) {
                Id id = (Id) exp;
                List<Id.Segment> segments = id.getSegments();
                if (segments.size() == 1) {
                    final Id.Segment s = segments.get(0);
                    if (s.quoting == Id.Quoting.UNQUOTED) {
                        Id.NameSegment nameSegment = (Id.NameSegment) s;
                        if (funTable.isReserved(nameSegment.getName())) {
                            return Literal.createSymbol(
                                    nameSegment.getName().toUpperCase());
                        }
                    }
                }

                try {
                    Exp expElement;
                    expElement = lookup(segments, funName, id.toString());
                    if (expElement == UNKNOWN_EXP) {
                        throw new ExprParseException("no suitable type find for " + Util.quoteMdxIdentifier(segments));
                    }

                    return expElement;
                } catch (ExprParseException e) {
                    throw new ExprParseException("Parse " + Util.quoteMdxIdentifier(segments) + " error.", e);
                }

            } else if (exp instanceof Literal) {
                return exp;
            } else {
                throw new RuntimeException("unknow to parse string");
            }
        }

        /**
         * support segment format like
         * [dimension]
         * [dimension].[hierarchy]
         * [dimension].[hierarchy].[level]
         * [dimension].[hierarchy].[member]
         * [dimension].[hierarchy].[member][.[member]...]
         * [dimension].[hierarchy].[level].[member]
         * [dimension].[hierarchy].[level][.[level]...].[member]
         * [dimension].[hierarchy].[level].[member][.[member]...]
         * [dimension].[hierarchy].&[member]
         * [dimension].[hierarchy].&[member][.&[member]...]
         * [dimension].[hierarchy].[level].&[member]
         * [dimension].[hierarchy].[level].&[member][.&[member]...]
         */
        private Exp lookup(List<Id.Segment> segments, String funName, String idString) throws ExprParseException {

            int prevOlapType = OlapElementType.UNKNOWN;
            Iterator<Id.Segment> segIter = segments.iterator();
            OlapElementContext context = new OlapElementContext();
            boolean keyMemberSuffix = false;
            while (segIter.hasNext()) {
                Id.Segment segment = segIter.next();

                if (segment instanceof Id.NameSegment) {
                    if (keyMemberSuffix) {
                        throw new ExprParseException("Don't allow format: XX.&[member].[member]");
                    }
                    String olapElementName = ((Id.NameSegment) segment).getName();
                    prevOlapType = lookupOlapElement(olapElementName, prevOlapType, context);
                }

                if (segment instanceof Id.KeySegment) {
                    keyMemberSuffix = true;
                    prevOlapType = OlapElementType.MEMBER;
                }

            }

            return convertOlapExp(prevOlapType, funName, idString);
        }

        private int lookupOlapElement(String olapElementName, int prevOlapType, OlapElementContext context) throws ExprParseException {

            switch (prevOlapType) {
                case OlapElementType.UNKNOWN:
                    if (olapElementFinder.findMeasureDimension(olapElementName)) {
                        dimensionsForLocation.add(olapElementName);
                        return OlapElementType.MEASURE_DIMENSION;
                    } else if (olapElementFinder.findRegularDimension(olapElementName, context)) {
                        String model = context.getDimensionTable().getModel();
                        if (model != null && !"".equals(model)) {
                            dimensionsForLocation.add(String.join(".", model, olapElementName));
                        } else {
                            dimensionsForLocation.add(olapElementName);
                        }

                        return OlapElementType.DIMENSION;
                    } else if (olapElementFinder.findNamedSet(olapElementName)) {
                        namesetForLocation.add(olapElementName);
                        return OlapElementType.NAMED_SET;
                    } else {
                        throw new ExprParseException("The argument [" + olapElementName + "] can't resolve the dimension or named set.");
                    }
                case OlapElementType.MEASURE_DIMENSION:
                    if (olapElementFinder.findMeasureDimension(olapElementName)) {
                        return OlapElementType.MEASURE_DIMENSION;
                    } else if (olapElementFinder.findMeasureName(olapElementName)) {
                        context.setMeasureName(olapElementName);
                        return OlapElementType.MEASURE_MEMBER;
                    } else {
                        throw new ExprParseException("The measure name [" + olapElementName + "] can't resolve.");
                    }
                case OlapElementType.DIMENSION:
                    if (olapElementFinder.findHierarchy(olapElementName, context)) {
                        return OlapElementType.HIERARCHY;
                    } else {
                        throw new ExprParseException("The hierarchy name [" + olapElementName + "] can't resolve.");
                    }
                case OlapElementType.HIERARCHY:
                case OlapElementType.LEVEL:
                    if (olapElementFinder.findLevel(olapElementName, context)) {
                        return OlapElementType.LEVEL;
                    } else {
                        return OlapElementType.MEMBER;
                    }
                case OlapElementType.MEMBER:
                    return OlapElementType.MEMBER;
                case OlapElementType.MEASURE_MEMBER:
                    throw new ExprParseException("Don't allow [Measures].[name].[name].");
                case OlapElementType.NAMED_SET:
                    throw new ExprParseException("Only allow single named set format");
                default:
                    throw new ExprParseException("unknown error!");
            }
        }

        private Exp convertOlapExp(int category, String funName, String idString) {
            switch (category) {
                case OlapElementType.DIMENSION:
                    switch (funName.toLowerCase()) {
                        case FunctionName.EXTRACT:
                            MockDimension mockDimension = new MockDimension(idString);
                            DimensionExpr dimensionExpr = new DimensionExpr(mockDimension);
                            return dimensionExpr;
                        default:
                            return new MockDimensionExpr();
                    }
                case OlapElementType.HIERARCHY:
                    switch (funName.toLowerCase()) {
                        case FunctionName.STRTOTUPLE:
                        case FunctionName.STRTOSET:
                        case FunctionName.EXTRACT:
                        case FunctionName.MEMBERS:
                            MockHierarchy mockHierarchy = new MockHierarchy(idString);
                            return new HierarchyExpr(mockHierarchy);
                        default:
                            return new MockHierarchyExpr();
                    }
                case OlapElementType.LEVEL:
                    switch (funName.toLowerCase()) {
                        case FunctionName.MEMBERS:
                            MockDimension mockDimension = new MockDimension(idString);
                            String[] tmp = idString.split("\\.");
                            MockHierarchy mockHierarchy;
                            if (tmp.length > 2) {
                                mockHierarchy = new MockHierarchy(tmp[0] + "." + tmp[1], mockDimension);
                            } else {
                                mockHierarchy = new MockHierarchy(idString, mockDimension);
                            }
                            MockLevel mockLevel = new MockLevel(mockHierarchy);
                            return new LevelExpr(mockLevel);
                        default:
                            return new MockLevelExpr();
                    }
                case OlapElementType.MEMBER:
                    switch (funName.toLowerCase()) {
                        case FunctionName.BRACKET:
                            return new MemberExpr(new MockMember(idString));
                        case FunctionName.MEMBERS:
                            return UNKNOWN_EXP;
                        default:
                            return new MockMemberExpr();
                    }
                case OlapElementType.MEASURE_MEMBER:
                    return new MockMemberExpr();
                case OlapElementType.NAMED_SET:
                    return new MockSetExpr();
                default:
                    return UNKNOWN_EXP;
            }
        }

        private FunDef resolveFunctionArgs(Exp[] args, Exp[] newArgs, String funName, Syntax syntax) throws ExprParseException {
            for (int i = 0; i < args.length; i++) {
                newArgs[i] = validate(args[i], funName);
            }

            return validator.getDef(newArgs, funName, syntax);
        }
    }


    @Data
    static class OlapElementContext {

        private DimensionTable dimensionTable;

        private Object hierarchy;

        private String measureName;

    }

    static class OlapElementFinder {

        private static final String MEASURE_DIMENSION = "Measures";

        private SimpleSchema simpleSchema;

        OlapElementFinder(SimpleSchema simpleSchema) {
            this.simpleSchema = simpleSchema;
        }

        boolean findMeasureDimension(String lookingUpName) {
            return MEASURE_DIMENSION.equals(lookingUpName);
        }

        boolean findMeasureName(String lookingupName) {
            return !Utils.isCollectionEmpty(simpleSchema.getMeasureAliases()) && simpleSchema.getMeasureAliases().contains(lookingupName)
                    || !Utils.isCollectionEmpty(simpleSchema.getCalcMeasureNames()) && simpleSchema.getCalcMeasureNames().contains(lookingupName);

        }

        boolean findRegularDimension(String lookingUpName, OlapElementContext context) {
            if (!Utils.isCollectionEmpty(simpleSchema.getDimensionTables())
                    && simpleSchema.getDimensionTables().contains(new DimensionTable(lookingUpName))) {

                Set<DimensionTable> dimensionTables = simpleSchema.getDimensionTables();
                for (DimensionTable dimensionTable : dimensionTables) {
                    if (dimensionTable.getAlias().equals(lookingUpName)) {
                        context.setDimensionTable(dimensionTable);
                        break;
                    }
                }

                return true;
            }

            return false;
        }

        boolean findHierarchy(String lookingUpName, OlapElementContext context) {
            DimensionTable dimensionTable = context.getDimensionTable();
            assert dimensionTable != null;

            if (!Utils.isCollectionEmpty(dimensionTable.getHierarchies())
                    && dimensionTable.getHierarchies().contains(new Hierarchy(lookingUpName))) {

                Set<Hierarchy> hierarchies = dimensionTable.getHierarchies();
                for (Hierarchy hierarchy : hierarchies) {
                    if (hierarchy.getName().equals(lookingUpName)) {
                        context.setHierarchy(hierarchy);
                        break;
                    }
                }

                return true;
            }

            // Support the dimension table column as hierarchy called hierarchy attribute
            if (!Utils.isCollectionEmpty(dimensionTable.getTableColAliases())
                    && dimensionTable.getTableColAliases().contains(new DimensionCol(lookingUpName))) {

                Set<DimensionCol> tableColAliases = dimensionTable.getTableColAliases();
                for (DimensionCol dimensionCol : tableColAliases) {
                    if (dimensionCol.getTableColAlias().equals(lookingUpName)) {
                        context.setHierarchy(lookingUpName);
                        break;
                    }
                }
                return true;
            }

            return false;
        }

        boolean findLevel(String lookingUpName, OlapElementContext context) {

            Object hierarchyObj = context.getHierarchy();

            if (hierarchyObj instanceof Hierarchy) {
                Hierarchy hierarchy = (Hierarchy) hierarchyObj;

                if (!Utils.isCollectionEmpty(hierarchy.getTableColAliases())
                        && hierarchy.getTableColAliases().contains(lookingUpName)) {

                    return true;
                }
            }

            // Support hierarchy name same as level name. Because the dimension table column default as one hierarchy
            // whose level is also default the table column name
            if (hierarchyObj instanceof String && hierarchyObj.equals(lookingUpName)) {
                return true;
            }

            return false;
        }

        public boolean findNamedSet(String olapElementName) {
            if (Utils.isCollectionEmpty(simpleSchema.getNamedSets())) {
                return false;
            }

            for (SimpleSchema.NamedSet namedSet : simpleSchema.getNamedSets()) {
                if (namedSet.getName().equals(olapElementName)) {
                    return true;
                }
            }

            return false;
        }
    }


    interface OlapElementType {
        int UNKNOWN = -1;
        int DIMENSION = 1;
        int HIERARCHY = 2;
        int LEVEL = 3;
        int MEMBER = 4;
        int MEASURE_MEMBER = 5;
        int MEASURE_DIMENSION = 6;
        int NAMED_SET = 7;
    }

    interface FunctionName {
        String STRTOSET = "strtoset";
        String STRTOTUPLE = "strtotuple";
        String EXTRACT = "extract";
        String MEMBERS = "members";
        String BRACKET = "{}";
    }

    static class SimpleMDXBuilder {

        private String calcMemberSnippet;

        public SimpleMDXBuilder(String calcMemberSnippet) {
            this.calcMemberSnippet = calcMemberSnippet;
        }

        public String build() {
            return String.format(SIMPLE_MDX_TEMPLATE, calcMemberSnippet);
        }
    }

    public static class ExprAdaptor extends QueryPart implements Exp {

        @Override
        public Exp clone() {
            throw new UnsupportedOperationException("The clone isn't allowed");
        }

        @Override
        public int getCategory() {
            throw new UnsupportedOperationException("The getCategory() isn't allowed");
        }

        @Override
        public Type getType() {
            throw new UnsupportedOperationException("The getType() isn't allowed");
        }

        @Override
        public void unparse(PrintWriter pw) {
            throw new UnsupportedOperationException("The unparse(PrintWriter) isn't allowed");
        }

        @Override
        public Exp accept(Validator validator) {
            throw new UnsupportedOperationException("The accept(Validator) isn't allowed");
        }

        @Override
        public Calc accept(ExpCompiler compiler) {
            throw new UnsupportedOperationException("The accept(ExpCompiler) isn't allowed");
        }

        @Override
        public Object accept(MdxVisitor visitor) {
            throw new UnsupportedOperationException("The accept(MdxVisitor) isn't allowed");
        }
    }


    public static class MockDimensionExpr extends ExprAdaptor {
        @Override
        public Type getType() {
            return DimensionType.Unknown;
        }

        @Override
        public int getCategory() {
            return Category.Dimension;
        }

        @Override
        public Exp accept(Validator validator) {
            return this;
        }
    }

    static class MockHierarchyExpr extends ExprAdaptor {
        @Override
        public Type getType() {
            return HierarchyType.Unknown;
        }

        @Override
        public int getCategory() {
            return Category.Hierarchy;
        }

        @Override
        public Exp accept(Validator validator) {
            return this;
        }
    }

    static class MockLevelExpr extends ExprAdaptor {
        @Override
        public Type getType() {
            return LevelType.Unknown;
        }

        @Override
        public int getCategory() {
            return Category.Level;
        }

        @Override
        public Exp accept(Validator validator) {
            return this;
        }
    }

    static class MockMemberExpr extends ExprAdaptor {
        @Override
        public Type getType() {
            return MemberType.Unknown;
        }

        @Override
        public int getCategory() {
            return Category.Member;
        }

        @Override
        public Exp accept(Validator validator) {
            return this;
        }
    }

    static class MockSetExpr extends ExprAdaptor {
        @Override
        public Type getType() {
            return new SetType(MemberType.Unknown);
        }

        @Override
        public int getCategory() {
            return Category.Set;
        }

        @Override
        public Exp accept(Validator validator) {
            return this;
        }
    }

    static class MockInternalStatement extends StatementImpl {
        private boolean closed = false;

        @Override
        public void close() {
        }

        @Override
        public RolapConnection getMondrianConnection() {
            return null;
        }
    }


    public static class MondrianValidatorAdaptor extends ValidatorImpl {

        private Query query;

        public MondrianValidatorAdaptor(FunTable funTable) {
            super(funTable);
        }

        @Override
        protected void defineParameter(Parameter param) {
            final String name = param.getName();
//            query.parameters.add(param);
//            query.parametersByName.put(name, param);
        }

        public void setQuery(Query query) {
            this.query = query;
        }

        @Override
        public Query getQuery() {
            return query;
        }

        @Override
        public SchemaReader getSchemaReader() {
            return null;
        }
    }

    public static class MockHierarchy implements mondrian.olap.Hierarchy {

        private String idString;

        private Dimension dimension;

        public MockHierarchy(String idString) {
            this.idString = idString;
            String[] tmp = idString.split("\\.");
            if (tmp.length > 2) {
                this.idString = tmp[0] + "." + tmp[1];
            } else {
                this.idString = idString;
            }
        }

        public MockHierarchy(String idString, Dimension dimension) {
            this.idString = idString;
            this.dimension = dimension;
        }

        @Override
        public String getUniqueName() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public OlapElement lookupChild(SchemaReader schemaReader, Id.Segment s, MatchType matchType) {
            return null;
        }

        @Override
        public String getQualifiedName() {
            return null;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public String getLocalized(LocalizedProperty prop, Locale locale) {
            return null;
        }

        @Override
        public mondrian.olap.Hierarchy getHierarchy() {
            return null;
        }

        @Override
        public Dimension getDimension() {
            return this.dimension;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public Level[] getLevels() {
            return new Level[0];
        }

        @Override
        public List<? extends Level> getLevelList() {
            return null;
        }

        @Override
        public Member getDefaultMember() {
            return null;
        }

        @Override
        public Member getAllMember() {
            return null;
        }

        @Override
        public Member getNullMember() {
            return null;
        }

        @Override
        public boolean hasAll() {
            return false;
        }

        @Override
        public Member createMember(Member parent, Level level, String name, Formula formula) {
            return null;
        }

        @Override
        public String getUniqueNameSsas() {
            return null;
        }

        @Override
        public Map<String, Annotation> getAnnotationMap() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String toString() {
            return this.idString;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && this.toString().equals(obj.toString()));
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public static class MockDimension implements mondrian.olap.Dimension {

        private String idString;

        private Dimension dimension;

        public MockDimension() {
        }

        public MockDimension(String idString) {
            this.idString = idString;
            this.dimension = new MockDimension();
        }

        @Override
        public String getUniqueName() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public OlapElement lookupChild(SchemaReader schemaReader, Id.Segment s, MatchType matchType) {
            return null;
        }

        @Override
        public String getQualifiedName() {
            return "";
        }

        @Override
        public String getCaption() {
            return "";
        }

        @Override
        public String getLocalized(LocalizedProperty prop, Locale locale) {
            return "";
        }

        @Override
        public mondrian.olap.Hierarchy getHierarchy() {
            return null;
        }

        @Override
        public Dimension getDimension() {
            return this.dimension;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public mondrian.olap.Hierarchy[] getHierarchies() {
            return new mondrian.olap.Hierarchy[0];
        }

        @Override
        public List<? extends mondrian.olap.Hierarchy> getHierarchyList() {
            return null;
        }

        @Override
        public boolean isMeasures() {
            return false;
        }

        @Override
        public org.olap4j.metadata.Dimension.Type getDimensionType() {
            return null;
        }

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public Map<String, Annotation> getAnnotationMap() {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }
    }

    public static class MockMember implements mondrian.olap.Member {

        private String idString;

        private Level level;

        private Dimension dimension;

        private mondrian.olap.Hierarchy hierarchy;

        public MockMember(String idString) {
            this.idString = idString;
            this.dimension = new MockDimension(idString);
            String[] tmp = idString.split("\\.");
            if (tmp.length > 2) {
                this.hierarchy = new MockHierarchy(tmp[0] + "." + tmp[1], dimension);
            } else {
                this.hierarchy = new MockHierarchy(idString, dimension);
            }
            this.level = new MockLevel(hierarchy);
        }

        @Override
        public Member getParentMember() {
            return null;
        }

        @Override
        public Level getLevel() {
            return this.level;
        }

        @Override
        public String getUniqueName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public OlapElement lookupChild(SchemaReader schemaReader, Id.Segment s, MatchType matchType) {
            return null;
        }

        @Override
        public String getQualifiedName() {
            return null;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public String getLocalized(LocalizedProperty prop, Locale locale) {
            return null;
        }

        @Override
        public mondrian.olap.Hierarchy getHierarchy() {
            return this.hierarchy;
        }

        @Override
        public Dimension getDimension() {
            return this.dimension;
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public String getParentUniqueName() {
            return null;
        }

        @Override
        public Comparable getValue() {
            return null;
        }

        @Override
        public MemberType getMemberType() {
            return MemberType.UNKNOWN;
        }

        @Override
        public boolean isParentChildLeaf() {
            return false;
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public boolean isAll() {
            return false;
        }

        @Override
        public boolean isMeasure() {
            return false;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isChildOrEqualTo(Member member) {
            return false;
        }

        @Override
        public boolean isCalculated() {
            return false;
        }

        @Override
        public boolean isEvaluated() {
            return false;
        }

        @Override
        public int getSolveOrder() {
            return 0;
        }

        @Override
        public Exp getExpression() {
            return null;
        }

        @Override
        public List<Member> getAncestorMembers() {
            return null;
        }

        @Override
        public boolean isCalculatedInQuery() {
            return false;
        }

        @Override
        public Object getPropertyValue(Property property) {
            return null;
        }

        @Override
        public Object getPropertyValue(String propertyName) {
            return null;
        }

        @Override
        public Object getPropertyValue(String propertyName, boolean matchCase) {
            return null;
        }

        @Override
        public String getPropertyFormattedValue(Property property) {
            return null;
        }

        @Override
        public String getPropertyFormattedValue(String propertyName) {
            return null;
        }

        @Override
        public void setProperty(Property property, Object value) {

        }

        @Override
        public void setProperty(String propertyName, Object value) {

        }

        @Override
        public Property[] getProperties() {
            return new Property[0];
        }

        @Override
        public int getOrdinal() {
            return 0;
        }

        @Override
        public Comparable getOrderKey() {
            return null;
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public Member getDataMember() {
            return null;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }

        @Override
        public Map<String, Annotation> getAnnotationMap() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }
    }

    public static class MockLevel implements mondrian.olap.Level {

        private String idString;

        private mondrian.olap.Hierarchy hierarchy;

        public MockLevel(String idString) {
            this.idString = idString;
        }

        public MockLevel(mondrian.olap.Hierarchy hierarchy) {
            this.hierarchy = hierarchy;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public String getUniqueName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public OlapElement lookupChild(SchemaReader schemaReader, Id.Segment s, MatchType matchType) {
            return null;
        }

        @Override
        public String getQualifiedName() {
            return null;
        }

        @Override
        public String getCaption() {
            return null;
        }

        @Override
        public String getLocalized(LocalizedProperty prop, Locale locale) {
            return null;
        }

        @Override
        public mondrian.olap.Hierarchy getHierarchy() {
            return this.hierarchy;
        }

        @Override
        public Dimension getDimension() {
            return this.hierarchy.getDimension();
        }

        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public Level getChildLevel() {
            return null;
        }

        @Override
        public Level getParentLevel() {
            return null;
        }

        @Override
        public boolean isAll() {
            return false;
        }

        @Override
        public boolean areMembersUnique() {
            return false;
        }

        @Override
        public org.olap4j.metadata.Level.Type getLevelType() {
            return null;
        }

        @Override
        public Property[] getProperties() {
            return new Property[0];
        }

        @Override
        public Property[] getInheritedProperties() {
            return new Property[0];
        }

        @Override
        public MemberFormatter getMemberFormatter() {
            return null;
        }

        @Override
        public int getApproxRowCount() {
            return 0;
        }

        @Override
        public Map<String, Annotation> getAnnotationMap() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }
    }

//    public static void main(String[] args) {
//        MockInternalStatement mockInternalStmt = new MockInternalStatement();
//        JavaccParserValidatorImpl javaccParser = new JavaccParserValidatorImpl(new MockQueryPartFactoryImpl());
//
//        String queryString = "Except (\n" +
//                "\tAddCalculatedMembers (\n" +
//                "\t\tExcept (\n" +
//                "\t\t\t[ BUYER_COUNTRY ].[ NAME ].Members,\n" +
//                "\t\t\t{[ BUYER_COUNTRY ].[ NAME ].Levels (0 ).Members }\n" +
//                "\t\t)\n" +
//                "\t) ,{[ BUYER_COUNTRY ].[ NAME ].[ Japan ]:[ BUYER_COUNTRY ].[ NAME ].[ United States ]}\n" +
//                ")";
//
//        RolapSchema.RolapSchemaFunctionTable funTable = new RolapSchema.RolapSchemaFunctionTable(Collections.emptyList());
//        funTable.init();
//        Exp exp = javaccParser.parseExpression(mockInternalStmt, queryString, false, funTable);
//
//        System.out.println(exp);
//    }
}
