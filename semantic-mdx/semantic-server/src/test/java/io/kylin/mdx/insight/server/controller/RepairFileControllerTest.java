package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.core.service.RepairFileService;
import io.kylin.mdx.insight.core.support.ExcelFileDataSourceInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepairFileControllerTest extends BaseControllerTest {

    private static final String CONTROLLER_NAME = "repairFileController";

    @Mock
    private RepairFileService repairFileService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }

    @Test
    public void repairExcelFile() throws IOException {
        {
            RepairFileController controller = new RepairFileController(Optional.empty());

            ResponseEntity<StreamingResponseBody> responseEntity1 = controller.repairExcelFile(multipartFile);
            Assert.assertEquals(responseEntity1.getStatusCode(), HttpStatus.NOT_IMPLEMENTED);

            ResponseEntity<ExcelFileDataSourceInfo> responseEntity2 = controller.checkExcelFileDataSources(multipartFile);
            Assert.assertEquals(responseEntity2.getStatusCode(), HttpStatus.NOT_IMPLEMENTED);
        }
        {
            when(repairFileService.repairFile(multipartFile)).thenReturn(o -> o.write("success".getBytes()));
            ExcelFileDataSourceInfo excelFileDataSourceInfo = new ExcelFileDataSourceInfo(
                    new ExcelFileDataSourceInfo.DataSourceCounts(),
                    new ExcelFileDataSourceInfo.DataSourceCounts());
            when(repairFileService.checkDataSources(multipartFile)).thenReturn(excelFileDataSourceInfo);
            RepairFileController controller = new RepairFileController(Optional.of(repairFileService));

            ResponseEntity<StreamingResponseBody> responseEntity1 = controller.repairExcelFile(multipartFile);
            Assert.assertEquals(responseEntity1.getStatusCode(), HttpStatus.OK);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Objects.requireNonNull(responseEntity1.getBody()).writeTo(os);
            Assert.assertEquals(os.toString(), "success");

            ResponseEntity<ExcelFileDataSourceInfo> responseEntity2 = controller.checkExcelFileDataSources(multipartFile);
            Assert.assertEquals(responseEntity2.getStatusCode(), HttpStatus.OK);
            Assert.assertEquals(excelFileDataSourceInfo, responseEntity2.getBody());
        }
    }

    @Test
    public void fileSizePropertyTest() {
        RepairFileController controller = new RepairFileController(Optional.empty());
        Map<String, Long> result  = controller.fileSizeProperty();
        Assert.assertEquals(result.size(), 1L);
    }
}
