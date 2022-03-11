package io.kylin.mdx.insight.core.meta;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.meta.mock.KylinConvertorMock;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

// TODO: modify to for Kylin
public class KylinConvertorTest {

    private final ConnectionInfo connInfo = ConnectionInfo.builder().user("ADMIN").password("KYLIN").project("AdventureWorks").build();

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "src/test/resources/kylin/");
        System.setProperty("kylin.json.path", "/kylin");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/user_group/groupMembers/ROLE_ADMIN"), "/kylin/kylin_get_group_members.json");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/access/ProjectInstance/AdventureWorks"), "/kylin/kylin_access_project.json");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/user_group/usersWithGroup"), "/kylin/kylin_get_groups.json");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/tables"), "/kylin/kylin_get_tables.json");
    }

    @AfterClass
    public static void clear() {
        System.setProperty("MDX_HOME", "");
        MockTestLoader.clear();
    }

    @Test
    public void getGroups() {
        KylinConvertor kylinConvertor = new KylinConvertorMock();
        ConnectionInfo connectionInfo = new ConnectionInfo();
        List<String> groups = kylinConvertor.getGroups(connectionInfo);
        Assert.assertEquals(9, groups.size());
    }

    @Test
    public void getGroupsByProject() {
        KylinConvertor kylinConvertor = new KylinConvertorMock();
        List<String> expects = JSON.parseArray("[\"w_group\"]", String.class);
        List<String> groups = kylinConvertor.getGroupsByProject(connInfo);
        Assert.assertEquals(expects, groups);
    }

    @Test
    public void getUsersByProject() {
        KylinConvertor kylinConvertor = new KylinConvertorMock();
        List<String> expects = JSON.parseArray("[\"ADMIN\", \"WANGHUI\"]", String.class);
        List<String> users = kylinConvertor.getUsersByProject(connInfo);
        Assert.assertEquals(expects, users);
    }

    @Test
    public void getDimensionCardinality() {
        KylinConvertor kylinConvertor = new KylinConvertorMock();
        Assert.assertNotNull(kylinConvertor.getHighCardinalityDimension(connInfo));
    }

    @Test
    public void validateResponse() {
        JSONObject response = new JSONObject();
        response.put(SemanticConstants.STATUS_KEY, "999");
        response.put("stacktrace", "error");
        try {
            AbstractConvertor.validateRespSuccess(response);
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals("error", e.getMessage());
        }
    }

}
