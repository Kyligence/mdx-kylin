package io.kylin.mdx.insight.core.service;

import com.google.common.collect.Lists;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.engine.service.parser.CalcMemberParserImpl;
import io.kylin.mdx.insight.server.SemanticLauncher;
import io.kylin.mdx.insight.server.bean.dto.CalculationMeasureDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportDetailsDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportRequestDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DimensionColDTO;
import io.kylin.mdx.insight.server.bean.dto.DimensionTableDTO;
import io.kylin.mdx.insight.server.bean.dto.MeasureDTO;
import io.kylin.mdx.insight.server.bean.dto.NamedSetDTO;
import io.kylin.mdx.insight.server.bean.dto.SemanticModelDTO;
import io.kylin.mdx.insight.server.service.BatchDatasetService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.web.client.RestTemplate;


import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;


@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class BatchDatasetServiceTest extends BaseEnvSetting {

    @InjectMocks
    private BatchDatasetService batchDatasetService;

    @Mock
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void testCheckConnectDataset() {
        String project = "sales";
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(HttpStatus.MULTI_STATUS);
        String url = SemanticConfig.getInstance().getDiscoverCatalogUrl(project);
        String basicAuth = "admin";
        HttpEntity<String> requestEntity = batchDatasetService.getCheckDatasetHttpEntity(basicAuth, false);
        when(restTemplate.postForEntity(url, requestEntity, String.class)).thenReturn(responseEntity);
        boolean isConvertorMock = SemanticConfig.getInstance().isConvertorMock();
        System.setProperty("converter.mock", "false");
        Assert.assertThrows(SemanticException.class, () -> batchDatasetService.checkConnectDataset(1, null, null, "sales", "sales", null, basicAuth));
        System.setProperty("converter.mock", String.valueOf(isConvertorMock));
    }

    @Test
    public void testCheckConnectDatasetHttpOK() throws PwdDecryptException {
        String project = "sales";
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(HttpStatus.OK);
        String url = SemanticConfig.getInstance().getDiscoverCatalogUrl(project);
        String basicAuth = "admin";
        HttpEntity<String> requestEntity = batchDatasetService.getCheckDatasetHttpEntity(basicAuth, false);
        when(restTemplate.postForEntity(url, requestEntity, String.class)).thenReturn(responseEntity);
        DatasetDTO datasetDTO = new DatasetDTO();
        datasetDTO.setProject(project);
        boolean isConvertorMock = SemanticConfig.getInstance().isConvertorMock();
        System.setProperty("converter.mock", "false");
        batchDatasetService.checkConnectDataset(1, datasetDTO, null, "sales", "sales", null, basicAuth);
        System.setProperty("converter.mock", String.valueOf(isConvertorMock));
    }
    @Test
    public void testIsEmptyNameSet() {
        Assert.assertFalse(batchDatasetService.isEmptyNameSet("{\"sales\"}"));
    }
    @Test
    public void testImportDatasetToInternal() {
        DatasetImportRequestDTO datasetImportRequestDTO = new DatasetImportRequestDTO();
        List<DatasetImportDetailsDTO> datasets = new ArrayList<>();
        DatasetImportDetailsDTO datasetImportDetailsDTO = new DatasetImportDetailsDTO();
        datasetImportDetailsDTO.setType("ADD_NEW");
        datasetImportDetailsDTO.setAcl(false);
        datasetImportDetailsDTO.setId("1");
        datasets.add(datasetImportDetailsDTO);
        datasetImportRequestDTO.setDatasets(datasets);


        DatasetDTO datasetDTO = new DatasetDTO();
        datasetDTO.setDatasetName("sales");
        List<SemanticModelDTO> semanticModelDTOS = new ArrayList<>();
        datasetDTO.setModels(semanticModelDTOS);
        SemanticModelDTO semanticModelDTO = new SemanticModelDTO();
        semanticModelDTOS.add(semanticModelDTO);
        MeasureDTO measureDTO = new MeasureDTO();
        semanticModelDTO.setMeasures(Lists.newArrayList(measureDTO));

        DimensionTableDTO dimensionTableDTO = new DimensionTableDTO();
        semanticModelDTO.setDimensionTables(Lists.newArrayList(dimensionTableDTO));
        DimensionColDTO dimensionColDTO = new DimensionColDTO();
        dimensionTableDTO.setDimCols(Lists.newArrayList(dimensionColDTO));

        datasetDTO.setCalculateMeasures(Lists.newArrayList(new CalculationMeasureDTO()));
        datasetDTO.setNamedSets(Lists.newArrayList(new NamedSetDTO()));

        semanticModelDTO.setDimensionTables(Lists.newArrayList(dimensionTableDTO));
        Pair<DatasetDTO, DatasetDTO> pair = Pair.of(null, datasetDTO);
        BatchDatasetService.importDatasetMap.put("1", pair);

        batchDatasetService.importDatasetToInternal(new DatasetImportResponseDTO(),datasetImportRequestDTO, "auth");
    }
}
