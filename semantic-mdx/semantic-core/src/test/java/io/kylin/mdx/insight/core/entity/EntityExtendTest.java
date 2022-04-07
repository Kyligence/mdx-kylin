package io.kylin.mdx.insight.core.entity;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityExtendTest {

    @Test
    public void test() {
        EntityExtend entityExtend = new EntityExtend();
        entityExtend.addVisibleUser("User1");
        entityExtend.addVisibleUser("User2");
        entityExtend.addInvisibleUser("User3");
        entityExtend.addInvisibleUser("User4");
        entityExtend.removeInvisibleUser("User4");
        entityExtend.removeInvisibleUser("User5");
        assertEquals(entityExtend.getVisible().size(), 2);
        assertEquals(entityExtend.getInvisible().size(), 1);
    }

    @Test
    public void SqlQueryTest() {
        Map<String, Object> sqlQueryMap = new HashMap<>();
        sqlQueryMap.put("id", 1);
        SqlQuery sqlQuery = new SqlQuery(sqlQueryMap);
        Assert.assertEquals(sqlQuery.getId().intValue(), 1);
    }

    @Test
    public void mdxQueryTest() {
        Map<String, Object> mdxQueryMap = new HashMap<>();
        mdxQueryMap.put("id", 1);
        MdxQuery sqlQuery = new MdxQuery(mdxQueryMap);
        Assert.assertEquals(sqlQuery.getId().intValue(), 1);
    }

    @Test
    public void propertyAttrTest() {
        PropertyAttr propertyAttr = new PropertyAttr("test","test","test");
        PropertyAttr propertyAttr1 = new PropertyAttr("test","test","test");
        propertyAttr1.equals(propertyAttr);
        Assert.assertEquals(propertyAttr.hashCode(), propertyAttr1.hashCode());
    }


}