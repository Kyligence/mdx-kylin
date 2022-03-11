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



package io.kylin.mdx.web.rewriter.utils;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.ErrorCode;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;

/**
 * @author hui.wang
 */
public class ExpUtils {

    public static boolean isMeasures(Id id) {
        Id.Segment segment = id.getElement(0);
        if (segment instanceof Id.NameSegment) {
            return Dimension.MEASURES_NAME.equalsIgnoreCase(((Id.NameSegment) segment).getName());
        }
        return false;
    }

    public static String getDimensionName(Id id) {
        return id.getElement(0).toString();
    }

    public static String getHierarchyName(Id id) {
        return getHierarchyName(id, true);
    }

    public static String getHierarchyName(Id id, boolean allowMeasures) {
        if (isMeasures(id)) {
            if (allowMeasures) {
                return Dimension.MEASURES_UNIQUE_NAME;
            } else {
                return null;
            }
        } else {
            if (id.getSegments().size() < 2) {
                return null;
            } else {
                return id.getElement(0) + "." + id.getElement(1);
            }
        }
    }

    public static String getLevelName(Id id) {
        if (id.getSegments().size() < 3) {
            return null;
        }
        if (id.getElement(2) instanceof Id.KeySegment) {
            return null;
        }
        return id.getElement(0) + "." + id.getElement(1) + "." + id.getElement(2);
    }

    public static String getSimpleMeasureName(Id id) {
        if (id.getSegments().size() != 2) {
            return null;
        }
        Id.Segment segment = id.getElement(1);
        if (segment instanceof Id.NameSegment) {
            return ((Id.NameSegment) segment).getName();
        } else {
            return null;
        }
    }

    public static String getSimpleHierarchyName(Id id) {
        if (id.getSegments().size() >= 2) {
            if (id.getSegments().get(1) instanceof Id.NameSegment) {
                return ((Id.NameSegment) id.getSegments().get(1)).getName();
            }
        }
        return null;
    }

    /**
     * 判断当前查询 axes 或者子查询的 axes 部分长度为 0
     */
    public static boolean isNonAxis(Query query) {
        if (query instanceof SubQuery) {
            if (!isNonAxis(((SubQuery) query).getQuery())) {
                return false;
            }
        }
        return query.getAxes().length == 0 && query.getSlicerAxis() == null;
    }

    // Exp 快速访问工具类

    public static Exp getArg(Exp exp, int index) {
        if (exp instanceof UnresolvedFunCall) {
            return ((UnresolvedFunCall) exp).getArg(index);
        }
        throw new SemanticException("Expression [" + Util.unparse(exp) + "] must be function!",
                ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
    }

    public static Id getArg2Id(Exp exp, int index) {
        Exp arg = getArg(exp, index);
        if (arg instanceof Id) {
            return (Id) arg;
        }
        throw new SemanticException("Expression [" + Util.unparse(arg) + "] must be id!",
                ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
    }

    public static UnresolvedFunCall getArg2Fun(Exp exp, int index) {
        if (exp instanceof UnresolvedFunCall) {
            Exp arg = ((UnresolvedFunCall) exp).getArg(index);
            if (arg instanceof UnresolvedFunCall) {
                return (UnresolvedFunCall) arg;
            }
            exp = arg;
        }
        throw new SemanticException("Expression [" + Util.unparse(exp) + "] must be function!",
                ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
    }

    public static UnresolvedFunCall getArg2FunWithName(Exp exp, int index, String funName) {
        if (exp instanceof UnresolvedFunCall) {
            Exp arg = ((UnresolvedFunCall) exp).getArg(index);
            if (arg instanceof UnresolvedFunCall && ((UnresolvedFunCall)arg).getFunName().equalsIgnoreCase(funName)) {
                return (UnresolvedFunCall) arg;
            }
            exp = arg;
        }
        throw new PatternUnmatchedException(
                "Expression [" + Util.unparse(exp) + "] must be a \"" + funName + "\" function expression.",
                ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
    }
}
