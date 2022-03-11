package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.common.SemanticConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class MdxQueryControllerTest extends BaseControllerTest {

    private static final String CONTROLLER_NAME = "mdxquerycontroller";

    private static final String[] MDX_QUERY_CONTROLLER_METHODS = {
            "selectHistoryLogList"
    };

    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Test
    public void selectHistoryLogListTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/query-history?pageNum=1&pageSize=10&projectName=le&orderBy=id")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(MDX_QUERY_CONTROLLER_METHODS[0])))
                .andExpect(status().isOk());
    }

    @Test
    public void selectQueryInfoTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/query-history/805ad9c1-3a7d-4b3f-8b15-98cc38cc594a?pageNum=1&pageSize=10&status=true")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
        )
                .andExpect(status().isOk());
    }

    @Test
    public void testGetClusterInfo() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/query-history/cluster")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
        )
                .andExpect(status().isOk());
    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME;
    }

}
