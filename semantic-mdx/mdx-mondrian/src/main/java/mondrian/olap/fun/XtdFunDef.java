/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.MemberCalc;
import mondrian.calc.TupleList;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.Type;
import mondrian.resource.MondrianResource;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;
import mondrian.xmla.XmlaRequestContext;
import org.olap4j.metadata.Dimension;

import java.util.Objects;

/**
 * Definition of <code>Ytd</code>, <code>Qtd</code>, <code>Mtd</code>,
 * and <code>Wtd</code> MDX builtin functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class XtdFunDef extends FunDefBase {
    private final org.olap4j.metadata.Level.Type levelType;

    static final ResolverImpl MtdResolver =
        new ResolverImpl(
            "Mtd",
            "Mtd([<Member>])",
            "A shortcut function for the PeriodsToDate function that specifies the level to be Month.",
            new String[]{"fx", "fxm"},
            org.olap4j.metadata.Level.Type.TIME_MONTHS);

    static final ResolverImpl QtdResolver =
        new ResolverImpl(
            "Qtd",
            "Qtd([<Member>])",
            "A shortcut function for the PeriodsToDate function that specifies the level to be Quarter.",
            new String[]{"fx", "fxm"},
            org.olap4j.metadata.Level.Type.TIME_QUARTERS);

    static final ResolverImpl WtdResolver =
        new ResolverImpl(
            "Wtd",
            "Wtd([<Member>])",
            "A shortcut function for the PeriodsToDate function that specifies the level to be Week.",
            new String[]{"fx", "fxm"},
            org.olap4j.metadata.Level.Type.TIME_WEEKS);

    static final ResolverImpl YtdResolver =
        new ResolverImpl(
            "Ytd",
            "Ytd([<Member>])",
            "A shortcut function for the PeriodsToDate function that specifies the level to be Year.",
            new String[]{"fx", "fxm"},
            org.olap4j.metadata.Level.Type.TIME_YEARS);

    public XtdFunDef(
        FunDef dummyFunDef,
        org.olap4j.metadata.Level.Type levelType)
    {
        super(dummyFunDef);
        assert levelType.isTime();
        this.levelType = levelType;
    }

    @Override
    public Type getResultType(Validator validator, Exp[] args) {
        if (args.length == 0) {
            if (Objects.nonNull(XmlaRequestContext.getContext()) && XmlaRequestContext.getContext().doValidateCM) {
                return new SetType(MemberType.Unknown);
            }
            // With no args, the default implementation cannot
            // guess the hierarchy.
            RolapHierarchy defaultTimeHierarchy =
                    ((RolapCube) validator.getQuery().getCube()).getTimeHierarchy(
                            getName());
            return new SetType(MemberType.forHierarchy(defaultTimeHierarchy));
        }
        final Type type = args[0].getType();
        if ((Objects.isNull(XmlaRequestContext.getContext()) || !XmlaRequestContext.getContext().doValidateCM)) {
            if (type.getDimension().getDimensionType() != Dimension.Type.TIME) {
                throw validator.getPrintStackEx(MondrianResource.instance().TimeArgNeeded.ex(getName()));
            }
        }
        return super.getResultType(validator, args);
    }

    private Level getLevel(Evaluator evaluator) {
        return evaluator.getCube().getTimeLevel(levelType);
    }

    private Level getLevelInXtdHierarchy(Evaluator evaluator,Hierarchy hierarchy) {
        return ((CubeBase)evaluator.getCube()).getTimeLevelInHierarchy(levelType,hierarchy);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        Level xtdLevel=null;
        if(call.getArgs()!=null && call.getArgs().length!=0) {
            Object returnTypeObj = ((ResolvedFunCall) call.getArg(0)).getReturnType();
            if (returnTypeObj instanceof MemberType) {
                xtdLevel = getLevelInXtdHierarchy(compiler.getEvaluator(),
                        ((MemberType) returnTypeObj).getHierarchy());
            }
        }

        if(xtdLevel==null){
            xtdLevel = getLevel(compiler.getEvaluator());
        }

        final Level level=xtdLevel;
        switch (call.getArgCount()) {
        case 0:
            return new AbstractListCalc(call, new Calc[0]) {
                public TupleList evaluateList(Evaluator evaluator) {
                    return new UnaryTupleList(
                        periodsToDate(evaluator, level, null));
                }

                public boolean dependsOn(Hierarchy hierarchy) {
                    return hierarchy.getDimension().getDimensionType()
                        == Dimension.Type.TIME;
                }
            };
        default:
            final MemberCalc memberCalc =
                compiler.compileMember(call.getArg(0));
            return new AbstractListCalc(call, new Calc[] {memberCalc}) {
                public TupleList evaluateList(Evaluator evaluator) {
                    return new UnaryTupleList(
                        periodsToDate(
                            evaluator,
                            level,
                            memberCalc.evaluateMember(evaluator)));
                }
            };
        }
    }

    private static class ResolverImpl extends MultiResolver {
        private final org.olap4j.metadata.Level.Type levelType;

        public ResolverImpl(
            String name,
            String signature,
            String description,
            String[] signatures,
            org.olap4j.metadata.Level.Type levelType)
        {
            super(name, signature, description, signatures);
            this.levelType = levelType;
        }

        protected FunDef createFunDef(Exp[] args, FunDef dummyFunDef) {
            return new XtdFunDef(dummyFunDef, levelType);
        }
    }
}

// End XtdFunDef.java
