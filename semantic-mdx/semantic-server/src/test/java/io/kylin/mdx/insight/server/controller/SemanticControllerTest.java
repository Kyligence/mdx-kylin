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


package io.kylin.mdx.insight.server.controller;

import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.engine.service.parser.CalcMemberParserImpl;
import io.kylin.mdx.insight.engine.service.parser.DefaultMemberValidatorImpl;
import io.kylin.mdx.insight.engine.service.parser.NamedSetParserImpl;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.engine.support.ExprParseException;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.DefaultMemberValidateDTO;
import io.kylin.mdx.insight.server.bean.dto.MdxExprValidationDTO;
import io.kylin.mdx.insight.server.service.BatchDatasetService;
import mondrian.olap.MondrianException;
import mondrian.parser.TokenMgrError;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SemanticControllerTest extends BaseEnvSetting {

    @Mock
    private DatasetService datasetService;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private BrokenService brokenService;

    @Mock
    private CalcMemberParserImpl calcMemberParserImpl;

    @Mock
    private MdxExprValidationDTO MDXExprValidationDTO;

    @Mock
    private NamedSetParserImpl namedSetParserImpl;

    @Mock
    private AuthService authService;

    @Mock
    private DefaultMemberValidatorImpl defaultMemberValidatorImpl;

    @Mock
    private DefaultMemberValidateDTO defaultMemberValidateDTO;

    @Mock
    private BatchDatasetService batchDatasetService;


    @Test
    public void testValidateCM() throws ExprParseException {

        DatasetController datasetController = new DatasetController(datasetService, projectManager, brokenService, calcMemberParserImpl, namedSetParserImpl, authService, defaultMemberValidatorImpl);

        when(MDXExprValidationDTO.getCalcMemberSnippets()).thenReturn(Collections.emptyList());

        Response<List<String>> response = datasetController.validateCM(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response.getStatus());


        when(MDXExprValidationDTO.getCalcMemberSnippets()).thenReturn(Arrays.asList("cos(1)", "1+1"));
        doNothing().when(calcMemberParserImpl).parse(any(), any());
        Response<List<String>> response2 = datasetController.validateCM(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response2.getStatus());

        when(MDXExprValidationDTO.getCalcMemberSnippets()).thenReturn(Arrays.asList("cos(1)aa", "1+aaa1"));
        Response<List<String>> response3 = datasetController.validateCM(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response3.getStatus());

    }

    @Test
    public void testValidateNameSet() {
        DatasetController datasetController = new DatasetController(datasetService, projectManager, brokenService, calcMemberParserImpl, namedSetParserImpl, authService, defaultMemberValidatorImpl);

        when(MDXExprValidationDTO.getNamedSetSnippets()).thenReturn(Collections.emptyList());

        Response<List<JSONObject>> response = datasetController.validateNamedSets(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response.getStatus());

        Response<List<JSONObject>> response2 = datasetController.validateNamedSets(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response2.getStatus());

        when(MDXExprValidationDTO.getNamedSetSnippets()).thenReturn(Arrays.asList(new String[]{"","test"}));
        Response<List<JSONObject>> response3 = datasetController.validateNamedSets(MDXExprValidationDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response3.getStatus());
    }

    @Test
    public void testValidateDefaultMembers() {


        DatasetController datasetController = new DatasetController(datasetService, projectManager, brokenService, calcMemberParserImpl, namedSetParserImpl, authService, defaultMemberValidatorImpl);

        when(defaultMemberValidateDTO.getDefaultMemberList()).thenReturn(Arrays.asList("[KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019]"));
        Response<List<String>> response1 = datasetController.validateDefaultMembers(defaultMemberValidateDTO);
        Assert.assertEquals(Response.Status.FAIL.ordinal(), (int) response1.getStatus());

        when(defaultMemberValidateDTO.getDefaultMemberList()).thenReturn(Arrays.asList("[KYLIN_CAL_DT].[YEAR_BEG_DT].&[2019]"));
        when(defaultMemberValidateDTO.getDefaultMemberPathList()).thenReturn(Arrays.asList("[KYLIN_CAL_DT].[YEAR_BEG_DT].[2019]"));
        Response<List<String>> response2 = datasetController.validateDefaultMembers(defaultMemberValidateDTO);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response2.getStatus());

    }

    private static final String CM_PARSER_SYNTAX_ERROR1 = "While parsing WITH MEMBER";
    private static final String CM_PARSER_SYNTAX_ERROR2 = "Syntax error\n";

    @Test
    public void testCollectCMParserErrorMesg() {
        MondrianException syntaxErrorExcep = new MondrianException(CM_PARSER_SYNTAX_ERROR2);
        MondrianException parseError = new MondrianException(CM_PARSER_SYNTAX_ERROR1, syntaxErrorExcep);
        Assert.assertEquals(CM_PARSER_SYNTAX_ERROR2, batchDatasetService.collectCmParserErrorMessage(parseError));

        TokenMgrError tokenMgrError = new TokenMgrError();
        Assert.assertEquals(CM_PARSER_SYNTAX_ERROR2, batchDatasetService.collectCmParserErrorMessage(tokenMgrError));

        ExprParseException exprParseException = new ExprParseException("calc exception");
        Assert.assertEquals("calc exception\n", batchDatasetService.collectCmParserErrorMessage(exprParseException));

    }
}
