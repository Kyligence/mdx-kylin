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


package io.kylin.mdx.web.transfer;

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;
import io.kylin.mdx.web.transfer.rule.excel.*;
import io.kylin.mdx.web.transfer.rule.generic.*;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.*;
import io.kylin.mdx.web.transfer.rule.excel.ExcelDrillThroughWhereRule;
import io.kylin.mdx.web.transfer.rule.excel.ExcelIfKeepOneRule;
import io.kylin.mdx.web.transfer.rule.excel.ExcelIfSearchRule;
import io.kylin.mdx.web.transfer.rule.excel.ExcelIfSlicerRule;
import io.kylin.mdx.web.transfer.rule.excel.ExcelQueryPartOptimizeRule;
import io.kylin.mdx.web.transfer.rule.generic.ClearLineSeparatorRule;
import io.kylin.mdx.web.transfer.rule.generic.IfEndsWithFromRule;
import io.kylin.mdx.web.transfer.rule.generic.IfSubtractSetRule;
import io.kylin.mdx.web.transfer.rule.generic.ReplaceFunctionNameRule;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.PowerBIConnectChangeCrossValueFormatRule;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.PowerBIConnectChangeValueFormatRule;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.PowerBIConnectIfChangeFilterFormatRule;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.PowerBIConnectIfChangeHierarchyFormatRule;
import io.kylin.mdx.web.transfer.rule.powerbi.connect.PowerBIConnectRemoveDistinctRule;
import io.kylin.mdx.web.transfer.rule.powerbi.desktop.PowerBIDesktopReplaceMemberCaptionRule;
import io.kylin.mdx.web.transfer.rule.powerbi.desktop.PowerBIDesktopReplaceNonEmptyRule;
import io.kylin.mdx.web.transfer.rule.powerbi.desktop.PowerBIDesktopReplaceSubSelectRule;
import io.kylin.mdx.web.transfer.rule.smartbi.SmartBIGenerateFlatTableMDXRule;
import io.kylin.mdx.web.transfer.rule.smartbi.SmartBIPushdownSubSetRule;
import io.kylin.mdx.web.transfer.rule.smartbi.SmartBIReadonlyResolveParentUniqueNameRule;
import io.kylin.mdx.web.transfer.rule.smartbi.SmartBIRemoveEmptySetRule;
import io.kylin.mdx.web.transfer.rule.tableau.TableauIfKeepOneRule;
import io.kylin.mdx.web.transfer.rule.tableau.TableauIfSearchRule;
import io.kylin.mdx.web.transfer.rule.tableau.TableauIfSlicerRule;
import io.kylin.mdx.web.transfer.rule.tableau.TableauQueryPartOptimizeRule;
import mondrian.xmla.XmlaRequestContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransferRuleManager {

    public static String applyAllRules(String clientType, String mdx, boolean other) {
        if (clientType == null || mdx == null) {
            return mdx;
        }
        ClientRules rules = getSpecificTransferRules(clientType, mdx, other);
        if (rules == null) {
            return mdx;
        }
        for (MdxTransferRule rule : rules.newRules()) {
            rule.setMdx(mdx);
            rule.apply();
            mdx = rule.getMdx();
            if (rule.isCompleted()) {
                break;
            }
        }
        return mdx;
    }

    public static ClientRules getSpecificTransferRules(String clientType, String mdx, boolean other) {
        switch (clientType) {
            case XmlaRequestContext.ClientType.SMARTBI:
                return other ? ClientRules.SmartBI_Other : ClientRules.SmartBI;
            case XmlaRequestContext.ClientType.MSOLAP:
                if (mdx.toUpperCase().startsWith("DRILLTHROUGH")) {
                    return ClientRules.Excel_DrillThrough;
                }
                if (XmlaRequestContext.getContext().tableauFlag
                        || mdx.contains("[MEMBER_UNIQUE_NAME],[MEMBER_ORDINAL],[MEMBER_CAPTION]")
                        || mdx.contains("CELL PROPERTIES FORMAT_STRING")) {
                    // 考虑到语句被改写，所以一旦初次认为是 tableau，后续均认为是 tableau
                    XmlaRequestContext.getContext().tableauFlag = true;
                    return other ? ClientRules.Tableau_Other : ClientRules.Tableau;
                } else {
                    return other ? ClientRules.Excel_Other : ClientRules.Excel;
                }
            case XmlaRequestContext.ClientType.POWERBI:
                return other ? ClientRules.PowerBI_Other : ClientRules.PowerBI;
            case XmlaRequestContext.ClientType.POWERBI_DESKTOP:
                return other ? ClientRules.PowerBiDesktop_Other : ClientRules.PowerBiDesktop;
            default:
                return null;
        }
    }

    public enum ClientRules {

        Excel() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new ReplaceFunctionNameRule(),
                        new ExcelIfSearchRule(),
                        new ExcelIfSlicerRule(),
                        new ExcelIfKeepOneRule(),
                        new IfEndsWithFromRule(),
                        new IfSubtractSetRule(),
                        new ClearLineSeparatorRule(),
                        new ExcelQueryPartOptimizeRule()
                );
            }
        },

        Excel_Other() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new ReplaceFunctionNameRule(),
                        new ExcelIfSearchRule(),
                        new ExcelIfSlicerRule(),
                        new ExcelIfKeepOneRule(),
                        new IfEndsWithFromRule(),
                        new IfSubtractSetRule()
                );
            }
        },

        Excel_DrillThrough() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Collections.singletonList(ExcelDrillThroughWhereRule.getInstance());
            }
        },

        Tableau() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new ReplaceFunctionNameRule(),
                        new TableauIfSearchRule(),
                        new TableauIfSlicerRule(),
                        new TableauIfKeepOneRule(),
                        new ClearLineSeparatorRule(),
                        new TableauQueryPartOptimizeRule()
                );
            }
        },

        Tableau_Other() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new ReplaceFunctionNameRule(),
                        new TableauIfSearchRule(),
                        new TableauIfSlicerRule(),
                        new TableauIfKeepOneRule(),
                        new ClearLineSeparatorRule()
                );
            }
        },

        PowerBI() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new PowerBIConnectRemoveDistinctRule(),
                        new PowerBIConnectIfChangeHierarchyFormatRule(),
                        new PowerBIConnectIfChangeFilterFormatRule(),
                        new PowerBIConnectChangeValueFormatRule(),
                        new PowerBIConnectChangeCrossValueFormatRule()
                );
            }
        },

        PowerBI_Other() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new PowerBIConnectRemoveDistinctRule(),
                        new PowerBIConnectIfChangeHierarchyFormatRule(),
                        new PowerBIConnectIfChangeFilterFormatRule(),
                        new PowerBIConnectChangeValueFormatRule(),
                        new PowerBIConnectChangeCrossValueFormatRule()
                );
            }
        },

        PowerBiDesktop() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new PowerBIDesktopReplaceNonEmptyRule(),
                        new PowerBIDesktopReplaceSubSelectRule(),
                        new PowerBIDesktopReplaceMemberCaptionRule()
                );
            }
        },

        PowerBiDesktop_Other() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new PowerBIDesktopReplaceNonEmptyRule(),
                        new PowerBIDesktopReplaceSubSelectRule(),
                        new PowerBIDesktopReplaceMemberCaptionRule()
                );
            }
        },

        SmartBI() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new SmartBIRemoveEmptySetRule(),
                        new SmartBIGenerateFlatTableMDXRule(),
                        new SmartBIPushdownSubSetRule(),
                        new SmartBIReadonlyResolveParentUniqueNameRule()
                );
            }
        },

        SmartBI_Other() {
            @Override
            public List<MdxTransferRule> newRules() {
                return Arrays.asList(
                        new SmartBIRemoveEmptySetRule(),
                        new SmartBIGenerateFlatTableMDXRule(),
                        new SmartBIPushdownSubSetRule(),
                        new SmartBIReadonlyResolveParentUniqueNameRule()
                );
            }
        };

        public abstract List<MdxTransferRule> newRules();

    }

}
