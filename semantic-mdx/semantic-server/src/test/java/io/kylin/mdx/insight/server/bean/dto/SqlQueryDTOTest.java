package io.kylin.mdx.insight.server.bean.dto;

import io.kylin.mdx.insight.core.entity.SqlQuery;
import org.junit.Assert;
import org.junit.Test;

public class SqlQueryDTOTest {
    @Test
    public void dataSetItemEquals() {
        SqlQuery sqlQuery = new SqlQuery() ;
        sqlQuery.setKeQueryId("1");
        SqlQueryDTO sqlQueryDTO = new SqlQueryDTO(sqlQuery);
        Assert.assertEquals(sqlQueryDTO.getKeQueryId(), sqlQuery.getKeQueryId());
    }
}
