package mondrian.xmla;

import org.junit.Assert;
import org.junit.Test;
import org.olap4j.xmla.server.impl.Util;

public class UtilTest {
    @Test
    public void extractCatalogFromMdxTest() {
        String mdx = "SELECT [from].[from from from].&[from from] on column from [from from from catalog] where ([from].[from from])";
        String expected = "from from from catalog";
        String actual = Util.extractCatalogFromMdx(mdx);
        System.out.println(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getMdxCacheUsedTest() {
        XmlaRequestContext context = new XmlaRequestContext();
        //query unsuccessful
        context.runningStatistics.success = false;
        Object sql1 = "sql1";
        Object sql2 = "sql2";
        try {
            context.runningStatistics.mapExtendExecuteSql.put(sql1, new XmlaRequestContext.ExecuteSql((String) sql1));
            context.runningStatistics.mapExtendExecuteSql.put(sql2, new XmlaRequestContext.ExecuteSql((String) sql2));
            context.runningStatistics.setSqlCacheUse(sql1, true);
            context.runningStatistics.setSqlCacheUse(sql2, true);
        } catch (Exception e) {
            //Just check cache result, no need to handle this.
        }
        Assert.assertFalse(context.runningStatistics.getMdxCacheUsed());

        //query successful, no sql
        context.runningStatistics.mapExtendExecuteSql.clear();
        context.runningStatistics.success = true;
        Assert.assertTrue(context.runningStatistics.getMdxCacheUsed());

        //query successful, part sql statements hit cache
        context.runningStatistics.mapExtendExecuteSql.clear();
        context.runningStatistics.success = false;
        try {
            context.runningStatistics.mapExtendExecuteSql.put(sql1, new XmlaRequestContext.ExecuteSql((String) sql1));
            context.runningStatistics.mapExtendExecuteSql.put(sql2, new XmlaRequestContext.ExecuteSql((String) sql2));
            context.runningStatistics.setSqlCacheUse(sql1, true);
            context.runningStatistics.setSqlCacheUse(sql2, false);
        } catch (Exception e) {
            //Just check cache result, no need to handle this.
        }
        Assert.assertFalse(context.runningStatistics.getMdxCacheUsed());

        //query successful. all sql statements hit cache
        context.runningStatistics.mapExtendExecuteSql.clear();
        context.runningStatistics.success = true;
        try {
            context.runningStatistics.mapExtendExecuteSql.put(sql1, new XmlaRequestContext.ExecuteSql((String) sql1));
            context.runningStatistics.mapExtendExecuteSql.put(sql2, new XmlaRequestContext.ExecuteSql((String) sql2));
            context.runningStatistics.setSqlCacheUse(sql1, true);
            context.runningStatistics.setSqlCacheUse(sql2, true);
        } catch (Exception e) {
            //Just check cache result, no need to handle this.
        }
        Assert.assertTrue(context.runningStatistics.getMdxCacheUsed());
    }

}
