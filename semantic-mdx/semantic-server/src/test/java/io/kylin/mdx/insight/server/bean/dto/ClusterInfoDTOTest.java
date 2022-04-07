package io.kylin.mdx.insight.server.bean.dto;

import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ClusterInfoDTOTest {

    @Test
    public void testClusterDTO() throws IOException {
        String content = "{\n" +
                "    \"start_at\": \"1\",\n" +
                "    \"end_at\": \"2\",\n" +
                "    \"log_type\": 0,\n" +
                "    \"clusters\": [{\n" +
                "        \"host\": \"localhost\",\n" +
                "        \"port\": \"7080\",\n" +
                "        \"status\": \"active\"\n" +
                "    }]\n" +
                "}";
        ClusterInfoDTO.ClusterDTO clusterDTO = new ClusterInfoDTO.ClusterDTO(
                "localhost", "7080");
        clusterDTO.setStatus("active");
        ClusterInfoDTO clusterInfoDTO = JacksonSerDeUtils.getJsonMapper().
                readValue(content, ClusterInfoDTO.class);
        Assert.assertEquals(clusterInfoDTO.getClusterNodes().get(0), clusterDTO);
    }

}