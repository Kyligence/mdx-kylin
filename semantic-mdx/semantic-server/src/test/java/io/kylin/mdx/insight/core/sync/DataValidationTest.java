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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class DataValidationTest extends BaseEnvSetting {

    private final static String project = "EasyJet";

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private ModelService modelService;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/datasetvalidation/kylin");
    }

    @Test
    @Transactional
    public void testNormal() throws SemanticException {

        List<KylinGenericModel> nocacheGenericModels = SemanticAdapter.INSTANCE.getNocacheGenericModels(project);

        DatasetEntity datasetEntity = datasetService.getDatasetEntity("EasyJet", "POC_Test_2");

        DatasetValidator datasetValidator = new DatasetValidator(datasetEntity, datasetService, nocacheGenericModels, modelService);

        DatasetValidateResult validate = datasetValidator.validate();

        Assert.assertEquals(DatasetValidateResult.DatasetValidateType.NORMAL, validate.getDatasetValidateType());
    }

    @Test
    @Transactional
    public void testBrokenForInvalidMeasureReferenceInCalculatedMeasure() throws SemanticException {
        List<KylinGenericModel> nocacheGenericModels = SemanticAdapter.INSTANCE.getNocacheGenericModels(project);

        DatasetEntity datasetEntity = datasetService.getDatasetEntity("EasyJet", "POC_Test_2_cm_invalid");

        DatasetValidator datasetValidator = new DatasetValidator(datasetEntity, datasetService, nocacheGenericModels, modelService);

        DatasetValidateResult validate = datasetValidator.validate();

        Assert.assertEquals(DatasetValidateResult.DatasetValidateType.BROKEN, validate.getDatasetValidateType());
    }

    @Test
    @Transactional
    public void testBrokenForMissingDimensionAndWeightColumn() throws SemanticException {
        List<KylinGenericModel> nocacheGenericModels = SemanticAdapter.INSTANCE.getNocacheGenericModels(project);

        DatasetEntity datasetEntity = datasetService.getDatasetEntity("EasyJet", "POC_Test_2_weight_missing");

        DatasetValidator datasetValidator = new DatasetValidator(datasetEntity, datasetService, nocacheGenericModels, modelService);

        DatasetValidateResult validate = datasetValidator.validate();

        Assert.assertEquals(DatasetValidateResult.DatasetValidateType.BROKEN, validate.getDatasetValidateType());
        Assert.assertEquals(DatasetBrokenType.HIERARCHY_WEIGHT_COL_DELETED, validate.getBrokenInfo().getHierarchies().get(0).getDatasetBrokenType());
        Assert.assertEquals(DatasetBrokenType.HIERARCHY_DIM_WEIGHT_COL_DELETED, validate.getBrokenInfo().getHierarchies().get(1).getDatasetBrokenType());
    }
}
