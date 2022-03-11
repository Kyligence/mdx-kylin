/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2006-2012 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.mdx.*;
import mondrian.olap.*;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.fun.ParenthesesFunDef;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.StringType;
import mondrian.rolap.aggmatcher.AggStar;
import mondrian.rolap.sql.CrossJoinArg;
import mondrian.rolap.sql.DescendantsCrossJoinArg;
import mondrian.rolap.sql.SqlQuery;
import mondrian.spi.Dialect;
import mondrian.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates SQL from parse tree nodes. Currently it creates the SQL that
 * accesses a measure for the ORDER BY that is generated for a TopCount.<p/>
 *
 * @author av
 * @since Nov 17, 2005
  */
public class RolapNativeSql {
    private static Set<String> COMPARATORS = new HashSet<String>() {
        {
            add("=");
            add("<>");
            add("<");
            add(">");
            add("<=");
            add(">=");
        }
    };

    private SqlQuery sqlQuery;
    private Dialect dialect;

    CompositeSqlCompiler numericCompiler;
    CompositeSqlCompiler booleanCompiler;

    RolapStoredMeasure storedMeasure;
    final AggStar aggStar;
    final Evaluator evaluator;
    final CrossJoinArg cjArg;

    /**
     * We remember one of the measures so we can generate
     * the constraints from RolapAggregationManager. Also
     * make sure all measures live in the same star.
     *
     * @see RolapAggregationManager#makeRequest(RolapEvaluator, boolean)
     */
    private boolean saveStoredMeasure(RolapStoredMeasure m) {
        if (storedMeasure != null) {
            RolapStar star1 = getStar(storedMeasure);
            RolapStar star2 = getStar(m);
            if (star1 != star2) {
                return false;
            }
        }
        this.storedMeasure = m;
        return true;
    }

    private RolapStar getStar(RolapStoredMeasure m) {
        return m.getStarMeasure().getStar();
    }

    /**
     * Translates an expression into SQL
     */
    interface SqlCompiler {
        /**
         * Returns SQL. If <code>exp</code> can not be compiled into SQL,
         * returns null.
         *
         * @param exp Expression
         * @return SQL, or null if cannot be converted into SQL
         */
        String compile(Exp exp);
    }

    /**
     * Implementation of {@link SqlCompiler} that uses chain of responsibility
     * to find a matching sql compiler.
     */
    static class CompositeSqlCompiler implements SqlCompiler {
        List<SqlCompiler> compilers = new ArrayList<SqlCompiler>();

        public void add(SqlCompiler compiler) {
            compilers.add(compiler);
        }

        public String compile(Exp exp) {
            for (SqlCompiler compiler : compilers) {
                String s = compiler.compile(exp);
                if (s != null) {
                    return s;
                }
            }
            return null;
        }

        public String toString() {
            return compilers.toString();
        }
    }

    /**
     * Compiles a numeric literal to SQL.
     */
    class NumberSqlCompiler implements SqlCompiler {
        public String compile(Exp exp) {
            if (!(exp instanceof Literal)) {
                return null;
            }
            if ((exp.getCategory() & Category.Numeric) == 0) {
                return null;
            }
            Literal literal = (Literal) exp;
            String expr = String.valueOf(literal.getValue());
            if (dialect.getDatabaseProduct().getFamily()
                == Dialect.DatabaseProduct.DB2)
            {
                expr = "FLOAT(" + expr + ")";
            }
            return expr;
        }

        public String toString() {
            return "NumberSqlCompiler";
        }
    }

    /**
     * Compiles a string literal to SQL.
     */
    class StringSqlCompiler implements SqlCompiler {
        public String compile(Exp exp) {
            if (!(exp instanceof Literal)) {
                return null;
            }
            if (exp.getCategory() != Category.String) {
                return null;
            }
            Literal literal = (Literal) exp;
            return '\'' + String.valueOf(literal.getValue()) + '\'';
        }

        public String toString() {
            return "StringSqlCompiler";
        }
    }

    /**
     * Base class to remove MemberScalarExp.
     */
    abstract class MemberSqlCompiler implements SqlCompiler {
        protected Exp unwind(Exp exp) {
            return exp;
        }
    }

    /**
     * Compiles a measure into SQL, the measure will be aggregated
     * like <code>sum(measure)</code>.
     */
    class StoredMeasureSqlCompiler extends MemberSqlCompiler {

        public String compile(Exp exp) {
            exp = unwind(exp);
            if (!(exp instanceof MemberExpr)) {
                return null;
            }
            final Member member = ((MemberExpr) exp).getMember();
            if (!(member instanceof RolapStoredMeasure)) {
                return null;
            }
            RolapStoredMeasure measure = (RolapStoredMeasure) member;
            if (measure.isCalculated()) {
                return null; // ??
            }
            if (!saveStoredMeasure(measure)) {
                return null;
            }

            String exprInner;
            // Use aggregate table to create condition if available
            if (aggStar != null && measure.getStarMeasure() != null)
            {
                RolapStar.Column column = measure.getStarMeasure();
                int bitPos = column.getBitPosition();
                AggStar.Table.Column aggColumn = aggStar.lookupColumn(bitPos);
                exprInner = aggColumn.generateExprString(sqlQuery);
            } else if (measure.getExpr() == null) {
                exprInner = "*";
            } else {
                exprInner = measure.getExpr().toSql();
            }

            String expr = measure.getAggregator().getExpression(exprInner);
            if (dialect.getDatabaseProduct().getFamily()
                == Dialect.DatabaseProduct.DB2)
            {
                expr = "FLOAT(" + expr + ")";
            }
            return expr;
        }

        public String toString() {
            return "StoredMeasureSqlCompiler";
        }
    }

    /**
     * Compiles an expression like (&lt;currentmember>.&lt;level>.&lt;name> = [String]
     *  and &lt;currentmember>.&lt;Member_Value> = [String])
     */
    class CurrentMemberLevelNameAndCaptionEqualsSqlCompiler implements SqlCompiler {
        @Override
        public String toString() {
            return "CurrentMemberLevelNameAndCaptionEqualsSqlCompiler";
        }

        @Override
        public String compile(Exp exp) {
            if (exp == null)
                return null;
            exp = removeComparingSingleArgParenthesises(exp);

            if (!(exp instanceof ResolvedFunCall)
                    || !((ResolvedFunCall) exp).getFunDef().getName().equalsIgnoreCase("AND"))
                return null;

            Exp levelNameEqualsExp = ((ResolvedFunCall) exp).getArg(0);
            Level level = getLevelByName(levelNameEqualsExp);
            if (level == null)
                return null;

            // If we uses Descendants(,,leaves) and the provided level is not the leaf level,
            // we should not return any result.
            if (cjArg instanceof DescendantsCrossJoinArg
                    && ((DescendantsCrossJoinArg) cjArg).isLeaves()
                    && (level instanceof FunUtil.NullLevel || level.getChildLevel() != null))
                return "false";

            if (!(level instanceof RolapCubeLevel))
                return null;
            RolapCubeLevel rolapCubeLevel = (RolapCubeLevel) level;
            RolapSchema.PhysColumn valueColumnExp = rolapCubeLevel.attribute.getValueExp();
            if (valueColumnExp == null)
                valueColumnExp = rolapCubeLevel.attribute.getNameExp();
            if (valueColumnExp == null && rolapCubeLevel.attribute.getKeyList() != null && !rolapCubeLevel.attribute.getKeyList().isEmpty())
                valueColumnExp = rolapCubeLevel.attribute.getKeyList().get(0);
            if (valueColumnExp == null)
                return null;

            Exp levelMemberValueExp = ((ResolvedFunCall) exp).getArg(1);
            Pair<FunDef, Object> memberValueComparator = getLevelMemberValueComparator(level, levelMemberValueExp);
            if (memberValueComparator == null)
                return null;

            StringBuilder comparingStringBuilder = new StringBuilder();
            comparingStringBuilder.append(' ').append(memberValueComparator.left.getName()).append(' ');
            if (valueColumnExp.getDatatype() != null && valueColumnExp.getDatatype().isNumeric() && memberValueComparator.right instanceof Number) {
                comparingStringBuilder.append(memberValueComparator.right);
            } else {
                comparingStringBuilder.append('\'').append(memberValueComparator.right).append('\'');
            }

            return valueColumnExp.toSql() + comparingStringBuilder.toString();
        }

        /**
         * Get the level from &lt;Hierarchy>.CurrentMember.Level.Name = [String]
         */
        private Level getLevelByName(Exp levelNameEqualsExp) {
            if (!(levelNameEqualsExp instanceof ResolvedFunCall)
                    || !((ResolvedFunCall) levelNameEqualsExp).getFunDef().getName().equals("="))
                return null;

            Exp levelNameExp = ((ResolvedFunCall) levelNameEqualsExp).getArg(0);
            Hierarchy hierarchy = getHierarchyFromFun(
                    levelNameExp, "Name", "Level", "CurrentMember");
            if (hierarchy == null)
                return null;

            Exp levelNameLiteral = ((ResolvedFunCall) levelNameEqualsExp).getArg(1);
            Object levelName = getLiteralContent(levelNameLiteral);
            if (levelName == null)
                return null;

            List<? extends Level> levelList = hierarchy.getLevelList();
            for (Level level : levelList) {
                if (level.getName().equals(levelName)) {
                    return level;
                }
            }
            return FunUtil.NullLevel;
        }

        /**
         * Get the comparator and literal value from &lt;Hierarchy>.CurrentMember.Member_Value &lt;Comparator> [String]
         */
        private Pair<FunDef, Object> getLevelMemberValueComparator(Level level, Exp levelMemberValueExp) {
            if (!(levelMemberValueExp instanceof ResolvedFunCall)
                    || !COMPARATORS.contains(((ResolvedFunCall) levelMemberValueExp).getFunDef().getName()))
                return null;
            ResolvedFunCall comparatorFunCall = (ResolvedFunCall) levelMemberValueExp;

            Exp valueExp = (comparatorFunCall.getArg(0));
            Hierarchy hierarchy = getHierarchyFromFun(
                    valueExp, "Member_Value", "CurrentMember");
            if (hierarchy == null || !level.getHierarchy().equals(hierarchy))
                return null;

            Exp valueLiteral = (comparatorFunCall.getArg(1));
            Object literal = getLiteralContent(valueLiteral);

            return new Pair<>(comparatorFunCall.getFunDef(), literal);
        }

        private Hierarchy getHierarchyFromFun(Exp exp, String... funNames) {
            for (String funName : funNames) {
                if (!((exp instanceof ResolvedFunCall)
                    && ((ResolvedFunCall) exp).getFunDef().getName().equals(funName)
                    && ((ResolvedFunCall) exp).getArgCount() == 1))
                return null;
                exp = ((ResolvedFunCall) exp).getArg(0);
            }
            if (!(exp instanceof HierarchyExpr))
                return null;
            return ((HierarchyExpr) exp).getHierarchy();
        }

        private Object getLiteralContent(Exp exp) {
            if (!(exp instanceof Literal))
                return null;
            return ((Literal) exp).getValue();
        }

        private Exp removeComparingSingleArgParenthesises(Exp exp) {
            if (exp instanceof ResolvedFunCall) {
                ResolvedFunCall resolvedFunCall = (ResolvedFunCall) exp;

                Exp[] newArgs = resolvedFunCall.getArgs().clone();
                for (int i = 0; i < newArgs.length; i++) {
                    newArgs[i] = removeComparingSingleArgParenthesises(newArgs[i]);
                }

                if (resolvedFunCall.getFunDef() instanceof ParenthesesFunDef
                        && resolvedFunCall.getArgs().length == 1
                        && resolvedFunCall.getArg(0) instanceof ResolvedFunCall) {
                    ResolvedFunCall parenthesisedResolvedFunCall = (ResolvedFunCall) resolvedFunCall.getArg(0);
                    if (COMPARATORS.contains(parenthesisedResolvedFunCall.getFunName()))
                        return resolvedFunCall.getArg(0);
                }
                if (!Arrays.equals(newArgs, resolvedFunCall.getArgs()))
                    return new ResolvedFunCall(resolvedFunCall.getFunDef(), newArgs, resolvedFunCall.getReturnType());
            }
            return exp;
        }
    }

    /**
     * Compiles a MATCHES MDX operator into SQL regular
     * expression match.
     */
    class MatchingSqlCompiler extends FunCallSqlCompilerBase {

        protected MatchingSqlCompiler()
        {
            super(Category.Logical, "MATCHES", 2);
        }

        public String compile(Exp exp) {
            if (!match(exp)) {
                return null;
            }
            if (!dialect.allowsRegularExpressionInWhereClause()
                || !(exp instanceof ResolvedFunCall)
                || evaluator == null)
            {
                return null;
            }

            final Exp arg0 = ((ResolvedFunCall)exp).getArg(0);
            final Exp arg1 = ((ResolvedFunCall)exp).getArg(1);

            // Must finish by ".Caption" or ".Name"
            if (!(arg0 instanceof ResolvedFunCall)
                || ((ResolvedFunCall)arg0).getArgCount() != 1
                || !(arg0.getType() instanceof StringType)
                || (!((ResolvedFunCall)arg0).getFunName().equals("Name")
                    && !((ResolvedFunCall)arg0)
                            .getFunName().equals("Caption")))
            {
                return null;
            }

            final boolean useCaption;
            if (((ResolvedFunCall)arg0).getFunName().equals("Name")) {
                useCaption = false;
            } else {
                useCaption = true;
            }

            // Must be ".CurrentMember"
            final Exp currMemberExpr = ((ResolvedFunCall)arg0).getArg(0);
            if (!(currMemberExpr instanceof ResolvedFunCall)
                || ((ResolvedFunCall)currMemberExpr).getArgCount() != 1
                || !(currMemberExpr.getType() instanceof MemberType)
                || !((ResolvedFunCall)currMemberExpr)
                        .getFunName().equals("CurrentMember"))
            {
                return null;
            }

            // Must be a dimension, a hierarchy or a level.
            final RolapCubeDimension dimension;
            final Exp dimExpr = ((ResolvedFunCall)currMemberExpr).getArg(0);
            if (dimExpr instanceof DimensionExpr) {
                dimension =
                    (RolapCubeDimension) evaluator.getCachedResult(
                        new ExpCacheDescriptor(dimExpr, evaluator));
            } else if (dimExpr instanceof HierarchyExpr) {
                final RolapCubeHierarchy hierarchy =
                    (RolapCubeHierarchy) evaluator.getCachedResult(
                        new ExpCacheDescriptor(dimExpr, evaluator));
                dimension = hierarchy.getDimension();
            } else if (dimExpr instanceof LevelExpr) {
                final RolapCubeLevel level =
                    (RolapCubeLevel) evaluator.getCachedResult(
                        new ExpCacheDescriptor(dimExpr, evaluator));
                dimension = level.getDimension();
            } else {
                return null;
            }

            RolapLevel rolapLevel = cjArg == null ? null : cjArg.getLevel();

            if (rolapLevel != null
                && dimension.equals(rolapLevel.getDimension()))
            {
                // We can't use the evaluator because the filter is filtering
                // a set which is uses same dimension as the predicate.
                // We must use, in order of priority,
                //  - caption requested: caption->name->key
                //  - name requested: name->key
                RolapSchema.PhysColumn expression =
                    useCaption
                        ? rolapLevel.attribute.getCaptionExp() == null
                            ? rolapLevel.attribute.getNameExp()
                            : rolapLevel.attribute.getCaptionExp()
                        : rolapLevel.attribute.getNameExp();
                /*
                 * If an aggregation table is used, it might be more efficient
                 * to use only the aggregate table and not the hierarchy table.
                 * Try to lookup the column bit key. If that fails, we will
                 * link the aggregate table to the hierarchy table. If no
                 * aggregate table is used, we can use the column expression
                 * directly.
                 */
                String sourceExp;
                if (aggStar != null
                    && rolapLevel instanceof RolapCubeLevel
                    && rolapLevel.attribute.getKeyList().size() == 1
                    && rolapLevel.attribute.getKeyList().get(0) == expression)
                {
                    // The following is disabled until we sort out AggStars.
                    // "col" will always come out null.
                    int bitPos = -100;
                    /*
                    final RolapCubeLevel cubeLevel =
                        (RolapCubeLevel) rolapLevel;
                    RolapStar.Column starColumn =
                        measureGroup.getRolapStarColumn(
                            cubeLevel.cubeDimension,
                            column,
                            false);
                    int bitPos = starColumn.getBitPosition();
                    AggStar.Table.Column aggColumn =
                        aggStar.lookupColumn(bitPos);
                    */

                    mondrian.rolap.aggmatcher.AggStar.Table.Column col =
                        aggStar.lookupColumn(bitPos);
                    if (col != null) {
                        sourceExp = col.generateExprString(sqlQuery);
                    } else {
                        // FIXME:
                        /*
                        // Make sure the level table is part of the query.
                        rolapLevel.getHierarchy().addToFrom(
                            sqlQuery,
                            expression);
                        */
                        sourceExp = expression.toSql();
                    }
                } else if (aggStar != null) {
                    // FIXME:
                    /*
                    // Make sure the level table is part of the query.
                    rolapLevel.getHierarchy().addToFrom(sqlQuery, expression);
                    */
                    sourceExp = expression.toSql();
                } else {
                    sourceExp = expression.toSql();
                }

                // The dialect might require the use of the alias rather
                // then the column exp.
                if (dialect.requiresHavingAlias()) {
                    sourceExp = sqlQuery.getAlias(sourceExp);
                }
                return
                    dialect.generateRegularExpression(
                        sourceExp,
                        String.valueOf(
                            evaluator.getCachedResult(
                                new ExpCacheDescriptor(arg1, evaluator))));
            } else {
                return null;
            }
        }
        public String toString() {
            return "MatchingSqlCompiler";
        }
    }

    /**
     * Compiles the underlying expression of a calculated member.
     */
    class CalculatedMemberSqlCompiler extends MemberSqlCompiler {
        SqlCompiler compiler;

        CalculatedMemberSqlCompiler(SqlCompiler argumentCompiler) {
            this.compiler = argumentCompiler;
        }

        public String compile(Exp exp) {
            exp = unwind(exp);
            if (!(exp instanceof MemberExpr)) {
                return null;
            }
            final Member member = ((MemberExpr) exp).getMember();
            if (!(member instanceof CalculatedMember)) {
                return null;
            }
            exp = member.getExpression();
            if (exp == null) {
                return null;
            }
            return compiler.compile(exp);
        }

        public String toString() {
            return "CalculatedMemberSqlCompiler";
        }
    }

    /**
     * Contains utility methods to compile FunCall expressions into SQL.
     */
    abstract class FunCallSqlCompilerBase implements SqlCompiler {
        int category;
        String mdx;
        int argCount;

        FunCallSqlCompilerBase(int category, String mdx, int argCount) {
            this.category = category;
            this.mdx = mdx;
            this.argCount = argCount;
        }

        /**
         * @return true if exp is a matching FunCall
         */
        protected boolean match(Exp exp) {
            if ((exp.getCategory() & category) == 0) {
                return false;
            }
            if (!(exp instanceof FunCall)) {
                return false;
            }
            FunCall fc = (FunCall) exp;
            if (!mdx.equalsIgnoreCase(fc.getFunName())) {
                return false;
            }
            Exp[] args = fc.getArgs();
            if (argCount >= 0 && args.length != argCount) {
                return false;
            }
            return true;
        }

        /**
         * compiles the arguments of a FunCall
         *
         * @return array of expressions or null if either exp does not match or
         * any argument could not be compiled.
         */
        protected String[] compileArgs(Exp exp, SqlCompiler compiler) {
            if (!match(exp)) {
                return null;
            }
            Exp[] args = ((FunCall) exp).getArgs();
            String[] sqls = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                sqls[i] = compiler.compile(args[i]);
                if (sqls[i] == null) {
                    return null;
                }
            }
            return sqls;
        }
    }

    /**
     * Compiles a funcall, e.g. foo(a, b, c).
     */
    class FunCallSqlCompiler extends FunCallSqlCompilerBase {
        SqlCompiler compiler;
        String sql;

        protected FunCallSqlCompiler(
            int category, String mdx, String sql,
            int argCount, SqlCompiler argumentCompiler)
        {
            super(category, mdx, argCount);
            this.sql = sql;
            this.compiler = argumentCompiler;
        }

        public String compile(Exp exp) {
            String[] args = compileArgs(exp, compiler);
            if (args == null) {
                return null;
            }
            StringBuilder buf = new StringBuilder();
            buf.append(sql);
            buf.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(args[i]);
            }
            buf.append(")");
            return buf.toString();
        }

        public String toString() {
            return "FunCallSqlCompiler[" + mdx + "]";
        }
    }

    /**
     * Shortcut for an unary operator like NOT(a).
     */
    class UnaryOpSqlCompiler extends FunCallSqlCompiler {
        protected UnaryOpSqlCompiler(
            int category,
            String mdx,
            String sql,
            SqlCompiler argumentCompiler)
        {
            super(category, mdx, sql, 1, argumentCompiler);
        }
    }

    /**
     * Shortcut for ().
     */
    class ParenthesisSqlCompiler extends FunCallSqlCompiler {
        protected ParenthesisSqlCompiler(
            int category,
            SqlCompiler argumentCompiler)
        {
            super(category, "()", "", 1, argumentCompiler);
        }

        public String toString() {
            return "ParenthesisSqlCompiler";
        }
    }

    /**
     * Compiles an infix operator like addition into SQL like <code>(a
     * + b)</code>.
     */
    class InfixOpSqlCompiler extends FunCallSqlCompilerBase {
        private final String sql;
        private final SqlCompiler compiler;

        protected InfixOpSqlCompiler(
            int category,
            String mdx,
            String sql,
            SqlCompiler argumentCompiler)
        {
            super(category, mdx, 2);
            this.sql = sql;
            this.compiler = argumentCompiler;
        }

        public String compile(Exp exp) {
            String[] args = compileArgs(exp, compiler);
            if (args == null) {
                return null;
            }
            return "(" + args[0] + " " + sql + " " + args[1] + ")";
        }

        public String toString() {
            return "InfixSqlCompiler[" + mdx + "]";
        }
    }

    /**
     * Compiles an <code>IsEmpty(measure)</code>
     * expression into SQL <code>measure is null</code>.
     */
    class IsEmptySqlCompiler extends FunCallSqlCompilerBase {
        private final SqlCompiler compiler;

        protected IsEmptySqlCompiler(
            int category, String mdx,
            SqlCompiler argumentCompiler)
        {
            super(category, mdx, 1);
            this.compiler = argumentCompiler;
        }

        public String compile(Exp exp) {
            String[] args = compileArgs(exp, compiler);
            if (args == null) {
                return null;
            }
            return "(" + args[0] + " is null" + ")";
        }

        public String toString() {
            return "IsEmptySqlCompiler[" + mdx + "]";
        }
    }

    /**
     * Compiles an <code>IIF(cond, val1, val2)</code> expression into SQL
     * <code>CASE WHEN cond THEN val1 ELSE val2 END</code>.
     */
    class IifSqlCompiler extends FunCallSqlCompilerBase {

        SqlCompiler valueCompiler;

        IifSqlCompiler(int category, SqlCompiler valueCompiler) {
            super(category, "iif", 3);
            this.valueCompiler = valueCompiler;
        }

        public String compile(Exp exp) {
            if (!match(exp)) {
                return null;
            }
            Exp[] args = ((FunCall) exp).getArgs();
            String cond = booleanCompiler.compile(args[0]);
            String val1 = valueCompiler.compile(args[1]);
            String val2 = valueCompiler.compile(args[2]);
            if (cond == null || val1 == null || val2 == null) {
                return null;
            }
            return sqlQuery.getDialect().caseWhenElse(cond, val1, val2);
        }
    }

    /**
     * Compiles an {@link mondrian.olap.fun.CaseTestFunDef} expression into SQL.
     */
    class CaseTestSqlCompiler extends FunCallSqlCompilerBase {
        CaseTestSqlCompiler(int category) {
            super(category, "_CaseTest", -1);
        }

        public String compile(Exp exp) {
            if (!match(exp)) {
                return null;
            }
            Exp[] args = ((FunCall) exp).getArgs();
            if (args.length < 2) {
                return null;
            }
            String[] expressions = new String[args.length];

            for (int i = 0; i < args.length / 2; i++) {
                expressions[i * 2] = booleanCompiler.compile(args[i * 2]);
                if (expressions[i * 2] == null) {
                    return null;
                }
                expressions[i * 2 + 1] = numericCompiler.compile(args[i * 2 + 1]);
                if (expressions[i * 2 + 1] == null) {
                    return null;
                }
            }

            if (args.length % 2 == 1) {
                expressions[args.length - 1] = numericCompiler.compile(args[args.length - 1]);
                if (expressions[args.length - 1] == null) {
                    return null;
                }
            }

            return sqlQuery.getDialect().caseWhenTestElse(expressions);
        }
    }

    /**
     * Compiles an {@link mondrian.olap.fun.CaseMatchFunDef} expression into SQL.
     */
    class CaseMatchSqlCompiler extends FunCallSqlCompilerBase {
        CaseMatchSqlCompiler(int category) {
            super(category, "_CaseMatch", -1);
        }

        public String compile(Exp exp) {
            if (!match(exp)) {
                return null;
            }
            Exp[] args = ((FunCall) exp).getArgs();
            if (args.length < 3) {
                return null;
            }
            String[] expressions = new String[args.length];

            expressions[0] = numericCompiler.compile(args[0]);
            if (expressions[0] == null) {
                return null;
            }
            for (int i = 1; i < (args.length + 1) / 2; i++) {
                expressions[i * 2 - 1] = numericCompiler.compile(args[i * 2 - 1]);
                if (expressions[i * 2 - 1] == null) {
                    return null;
                }
                expressions[i * 2] = numericCompiler.compile(args[i * 2]);
                if (expressions[i * 2] == null) {
                    return null;
                }
            }

            if (args.length % 2 == 0) {
                expressions[args.length - 1] = numericCompiler.compile(args[args.length - 1]);
                if (expressions[args.length - 1] == null) {
                    return null;
                }
            }

            return sqlQuery.getDialect().caseWhenMatchElse(expressions);
        }
    }

    /**
     * Creates a RolapNativeSql.
     *
     * @param sqlQuery the query which is needed for different SQL dialects -
     * it is not modified
     */
    public RolapNativeSql(
        SqlQuery sqlQuery,
        AggStar aggStar,
        Evaluator evaluator,
        CrossJoinArg cjArg)
    {
        this.sqlQuery = sqlQuery;
        this.cjArg = cjArg;
        this.evaluator = evaluator;
        this.dialect = sqlQuery.getDialect();
        this.aggStar = aggStar;

        numericCompiler = new CompositeSqlCompiler();
        booleanCompiler = new CompositeSqlCompiler();

        numericCompiler.add(new CaseTestSqlCompiler(Category.Value));
        numericCompiler.add(new CaseMatchSqlCompiler(Category.Value));
        numericCompiler.add(new StringSqlCompiler());
        numericCompiler.add(new NumberSqlCompiler());
        numericCompiler.add(new StoredMeasureSqlCompiler());
        numericCompiler.add(new CalculatedMemberSqlCompiler(numericCompiler));

        numericCompiler.add(
            new ParenthesisSqlCompiler(Category.Numeric, numericCompiler));
        numericCompiler.add(
            new InfixOpSqlCompiler(
                Category.Numeric, "+", "+", numericCompiler));
        numericCompiler.add(
            new InfixOpSqlCompiler(
                Category.Numeric, "-", "-", numericCompiler));
        numericCompiler.add(
            new InfixOpSqlCompiler(
                Category.Numeric, "/", "/", numericCompiler));
        numericCompiler.add(
            new InfixOpSqlCompiler(
                Category.Numeric, "*", "*", numericCompiler));
        numericCompiler.add(
            new IifSqlCompiler(Category.Numeric, numericCompiler));

        booleanCompiler.add(
                new CurrentMemberLevelNameAndCaptionEqualsSqlCompiler());
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "<", "<", numericCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "<=", "<=", numericCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, ">", ">", numericCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, ">=", ">=", numericCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "=", "=", numericCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "<>", "<>", numericCompiler));
        booleanCompiler.add(
            new IsEmptySqlCompiler(
                Category.Logical, "IsEmpty", numericCompiler));

        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "and", "AND", booleanCompiler));
        booleanCompiler.add(
            new InfixOpSqlCompiler(
                Category.Logical, "or", "OR", booleanCompiler));
        booleanCompiler.add(
            new UnaryOpSqlCompiler(
                Category.Logical, "not", "NOT", booleanCompiler));
        booleanCompiler.add(
            new MatchingSqlCompiler());
        booleanCompiler.add(
            new ParenthesisSqlCompiler(Category.Logical, booleanCompiler));
        booleanCompiler.add(
            new IifSqlCompiler(Category.Logical, booleanCompiler));
    }

    /**
     * Generates an aggregate of a measure, e.g. "sum(Store_Sales)" for
     * TopCount. The returned expr will be added to the select list and to the
     * order by clause.
     */
    public String generateTopCountOrderBy(Exp exp) {
        return numericCompiler.compile(exp);
    }

    public String generateFilterCondition(Exp exp) {
        return booleanCompiler.compile(exp);
    }

    public RolapStoredMeasure getStoredMeasure() {
        return storedMeasure;
    }

}

// End RolapNativeSql.java
