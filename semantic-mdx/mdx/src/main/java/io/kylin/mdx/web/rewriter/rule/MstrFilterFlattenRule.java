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



package io.kylin.mdx.web.rewriter.rule;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.web.rewriter.BaseRewriteRule;
import io.kylin.mdx.web.rewriter.utils.ExpVisitor;
import io.kylin.mdx.web.rewriter.utils.PatternUnmatchedException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.Exp;
import mondrian.olap.Formula;
import mondrian.olap.Id;
import mondrian.olap.Literal;
import mondrian.olap.NamedSet;
import mondrian.olap.Query;
import mondrian.olap.Syntax;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.olap4j.xmla.server.impl.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MstrFilterFlattenRule extends BaseRewriteRule {
    private static final String DIMENSION_ORDINAL = "DimensionOrdinal";
    private static final String SET_ORDINAL = "SetOrdinal";
    private static final Pattern SET_NAME_PATTERN = Pattern.compile(String.format(
            "^dim(?<%s>\\d+)_filter_(?:comp(?<%s>\\d+)|elements|ext_elements|final|ext_result)$",
            DIMENSION_ORDINAL, SET_ORDINAL));

    @Override
    public Pair<Boolean, Query> rewrite(Query query) {
        Formula[] formulas = query.getFormulas();

        Map<Integer, HierarchyFilter> filters;
        try {
            filters = parseFilterSets(formulas);
        } catch (PatternUnmatchedException e) {
            log.info("MSTR filter flattening rule matching failed, will not do any modification.", e);
            return new Pair<>(false, query);
        }
        if (MapUtils.isEmpty(filters) || filters.entrySet().stream().allMatch(entry -> entry.getValue().isEmpty())) {
            return new Pair<>(false, query);
        }

        Formula[] newFormulas = createNewFormulas(formulas, filters);
        query.setFormulas(newFormulas);
        return new Pair<>(true, query);
    }

    private static Map<Integer, HierarchyFilter> parseFilterSets(Formula[] formulas) {
        Map<Integer, HierarchyFilter> filters = new LinkedHashMap<>();

        for (int i = 0; i < formulas.length; i++) {
            Formula formula = formulas[i];
            if (formula.isMember()) {
                continue;
            }

            NamedSet namedSet = formula.getNamedSet();
            Exp exp = namedSet.getExp();
            if (!(exp instanceof UnresolvedFunCall)) {
                continue;
            }

            Matcher nameMatcher = SET_NAME_PATTERN.matcher(namedSet.getName());
            if (!nameMatcher.matches()) {
                continue;
            }
            int dimensionOrdinal = Integer.parseInt(nameMatcher.group(DIMENSION_ORDINAL));
            final int currentSetIndex = i;
            HierarchyFilter currentFilter = filters.computeIfAbsent(
                    dimensionOrdinal,
                    ignored -> new HierarchyFilter(currentSetIndex));

            String setOrdinalString = nameMatcher.group(SET_ORDINAL);
            if (setOrdinalString == null) {
                if (currentFilter.getEndIndex() == i - 1) {
                    currentFilter.setFinalSetName(namedSet.getName());
                    currentFilter.setEndIndex(i);
                }
                continue;
            }
            currentFilter.setEndIndex(i);
            int setOrdinal = Integer.parseInt(setOrdinalString);

            UnresolvedFunCall unresolvedFunCall = (UnresolvedFunCall)exp;
            String functionName = unresolvedFunCall.getFunName();
            if (!"UNION".equalsIgnoreCase(functionName) && !"EXCEPT".equalsIgnoreCase(functionName)) {
                continue;
            }

            if ("UNION".equalsIgnoreCase(unresolvedFunCall.getFunName())) {
                Id unionMember = extractUnionMemberId(unresolvedFunCall);
                currentFilter.addUnionMember(setOrdinal, unionMember);
            } else if ("EXCEPT".equalsIgnoreCase(unresolvedFunCall.getFunName())) {
                String exceptSetName = extractExceptSetName(unresolvedFunCall);
                Matcher exceptSetNameMatcher = SET_NAME_PATTERN.matcher(exceptSetName);
                if (!exceptSetNameMatcher.find()) {
                    continue;
                }

                int exceptDimensionOrdinal = Integer.parseInt(exceptSetNameMatcher.group(DIMENSION_ORDINAL));
                assert dimensionOrdinal == exceptDimensionOrdinal;

                int exceptSetOrdinal = Integer.parseInt(exceptSetNameMatcher.group(SET_ORDINAL));
                currentFilter.addExceptSet(exceptSetOrdinal);
            }
        }

        return filters;
    }

    private static Id extractUnionMemberId(UnresolvedFunCall unionExp) {
        Id memberId = (Id)new ExpVisitor(unionExp)
                .argFunWithName(0, "{}")
                .argId(0)
                .execute();

        assert unionExp.getArg(1) instanceof UnresolvedFunCall;
        ExpVisitor unionExpVisitor = new ExpVisitor(unionExp);
        if ("{}".equals(((UnresolvedFunCall)unionExp.getArg(1)).getFunName())) {
            unionExpVisitor = unionExpVisitor
                    .argFunWithName(1, "{}")
                    .argFunWithName(0, "AddCalculatedMembers");
        } else {
            unionExpVisitor = unionExpVisitor
                    .argFunWithName(1, "AddCalculatedMembers");
        }
        Id repeatedMemberId = (Id)unionExpVisitor
                .argFunWithName(0, "Distinct")
                .argFunWithName(0, "Descendants")
                .argFunWithName(0, "{}")
                .argId(0)
                .execute();

        if (!Objects.equals(memberId, repeatedMemberId)) {
            throw new SemanticException(
                    String.format("Different member %s and %s found in MicroStrategy Union NamedSet.",
                            memberId, repeatedMemberId),
                    ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
        }
        return memberId;
    }

    private static String extractExceptSetName(UnresolvedFunCall exceptExp) {
        Id setId = (Id)new ExpVisitor(exceptExp)
                .argId(1)
                .execute();
        List<Id.Segment> segments = setId.getSegments();
        if (segments.size() != 1 || !(segments.get(0) instanceof Id.NameSegment)) {
            throw new SemanticException(
                    String.format("Unsupported argument of Except function %s found in MicroStrategy Except NamedSet.",
                            setId.toString()),
                    ErrorCode.UNSUPPORTED_PATTERN_IN_MDX);
        }
        return ((Id.NameSegment)segments.get(0)).getName();
    }

    private static Formula[] createNewFormulas(Formula[] formulas, Map<Integer, HierarchyFilter> filters) {
        int newFormulasSize = formulas.length;
        for (Map.Entry<Integer, HierarchyFilter> entry : filters.entrySet()) {
            HierarchyFilter currentFilter = entry.getValue();
            newFormulasSize -= (currentFilter.getEndIndex() - currentFilter.getStartIndex());
        }

        Formula[] newFormulas = new Formula[newFormulasSize];
        int targetStartIndex = 0, sourceStartIndex = 0;
        for (Map.Entry<Integer, HierarchyFilter> entry : filters.entrySet()) {
            HierarchyFilter currentFilter = entry.getValue();

            int copyLength = currentFilter.getStartIndex() - sourceStartIndex;
            System.arraycopy(formulas, sourceStartIndex, newFormulas, targetStartIndex, copyLength);

            targetStartIndex += copyLength;
            newFormulas[targetStartIndex] = new Formula(
                    new Id(new Id.NameSegment(currentFilter.getFinalSetName(), Id.Quoting.QUOTED)),
                    currentFilter.createFilterExp());

            targetStartIndex++;
            sourceStartIndex = currentFilter.getEndIndex() + 1;
        }
        System.arraycopy(formulas, sourceStartIndex, newFormulas, targetStartIndex, formulas.length - sourceStartIndex);

        return newFormulas;
    }

    @Data
    @RequiredArgsConstructor
    private static class HierarchyFilter {
        private Map<Integer, Id> unionMap;
        private Set<Integer> exceptSet;
        private final int startIndex;
        private int endIndex;
        private String finalSetName;
        private List<Id> addedMembers;
        private List<Id> exceptedMembers;

        private void addUnionMember(int setOrdinal, Id unionMember) {
            if (unionMap == null) {
                unionMap = new LinkedHashMap<>();
            }
            unionMap.put(setOrdinal, unionMember);
        }

        private void addExceptSet(int exceptSetOrdinal) {
            if (exceptSet == null) {
                exceptSet = new LinkedHashSet<>();
            }
            exceptSet.add(exceptSetOrdinal);
        }

        private boolean isEmpty() {
            return MapUtils.isEmpty(unionMap) && CollectionUtils.isEmpty(exceptSet);
        }

        private void extractMembers() {
            if (exceptSet != null) {
                for (int exceptSetOrdinal : exceptSet) {
                    Id exceptedMemberId = unionMap.remove(exceptSetOrdinal);
                    if (exceptedMembers == null) {
                        exceptedMembers = new ArrayList<>();
                    }
                    exceptedMembers.add(exceptedMemberId);
                }
            }

            if (unionMap != null) {
                for (Map.Entry<Integer, Id> entry : unionMap.entrySet()) {
                    if (addedMembers == null) {
                        addedMembers = new ArrayList<>();
                    }
                    addedMembers.add(entry.getValue());
                }
            }
        }

        private UnresolvedFunCall createFilterExp() {
            if (Utils.isCollectionEmpty(addedMembers)) {
                extractMembers();
            }
            UnresolvedFunCall idsFunCall = distinctDescendants(addedMembers);

            if (Utils.isCollectionEmpty(exceptedMembers)) {
                return idsFunCall;
            }

            UnresolvedFunCall exceptIdsFunCall = distinctDescendants(exceptedMembers);
            return new UnresolvedFunCall("Except", new Exp[] {idsFunCall, exceptIdsFunCall});
        }

        private static UnresolvedFunCall distinctDescendants(List<Id> ids) {
            UnresolvedFunCall memberIdsSet = new UnresolvedFunCall("{}", Syntax.Braces, ids.toArray(new Exp[0]));
            Literal descendantsDistance = Literal.create(BigDecimal.ZERO);
            Id descendantsFlag = new Id(new Id.NameSegment("self_and_after", Id.Quoting.UNQUOTED));

            Exp[] descendantsArgs = new Exp[] {memberIdsSet, descendantsDistance, descendantsFlag};
            UnresolvedFunCall descendantsFunCall = new UnresolvedFunCall("Descendants", descendantsArgs);

            return new UnresolvedFunCall("AddCalculatedMembers", new Exp[] {
                    new UnresolvedFunCall("Distinct", new Exp[] {descendantsFunCall})});
        }
    }
}
