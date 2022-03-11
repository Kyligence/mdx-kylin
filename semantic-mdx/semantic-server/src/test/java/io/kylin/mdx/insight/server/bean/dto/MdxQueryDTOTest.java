package io.kylin.mdx.insight.server.bean.dto;

import io.kylin.mdx.insight.core.entity.MdxQuery;
import org.junit.Assert;
import org.junit.Test;

public class MdxQueryDTOTest {
    @Test
    public void createMdxQuery() {
        MdxQuery mdxQuery = new MdxQuery();
        mdxQuery.setMdxQueryId("1");
        MdxQueryDTO mdxQueryDTO =new MdxQueryDTO(mdxQuery);
        Assert.assertEquals(mdxQueryDTO.getQueryId(), mdxQuery.getMdxQueryId());
    }
}
