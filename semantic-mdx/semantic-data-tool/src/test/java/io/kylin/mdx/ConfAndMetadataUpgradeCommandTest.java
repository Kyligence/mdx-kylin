package io.kylin.mdx;

import io.kylin.mdx.insight.core.dao.MdxInfoMapper;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.MdxInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.data.upgrade.ConfAndMetadataUpgradeCommand;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.RowBounds;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.ApplicationArguments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class ConfAndMetadataUpgradeCommandTest {

    private static final String MDX_DIR = "src/test/resources/mdxDir/";

    private static final String OLD_MDX_DIR = "src/test/resources/mdxDir/oldMdx/";

    private static final String NEW_MDX_DIR = "src/test/resources/mdxDir/newMdx/";

    private static final String TMP_MDX_DIR = "src/test/resources/tmp";

    @Test
    public void testProcessTrim() throws Exception {
        if (!new File(TMP_MDX_DIR).exists()) {
            new File(TMP_MDX_DIR).mkdir();
        }
        FileUtils.copyDirectory(new File(MDX_DIR), new File(TMP_MDX_DIR));
        UserTest userTest = new UserTest();
        MdxInfoTest mdxInfoTest = new MdxInfoTest();
        ConfAndMetadataUpgradeCommand confAndMetadataUpgradeCommand = new ConfAndMetadataUpgradeCommand(mdxInfoTest, userTest);
        Arguments arguments = new Arguments();
        confAndMetadataUpgradeCommand.run(arguments);
        FileUtils.deleteDirectory(new File(MDX_DIR));
        if (!new File(MDX_DIR).exists()) {
            new File(MDX_DIR).mkdir();
        }
        FileUtils.copyDirectory(new File(TMP_MDX_DIR), new File(MDX_DIR));
        FileUtils.deleteDirectory(new File(TMP_MDX_DIR));
    }

    @Test
    public void testVersionCompare() {
        String version1 = "1.2.3";
        String version2 = "1.2.4.1031";
        String version3 = "0.1.2";
        Assert.assertEquals(ConfAndMetadataUpgradeCommand.versionCompare(version1, version2), -1);
        Assert.assertEquals(ConfAndMetadataUpgradeCommand.versionCompare(version1, version3), 1);
        Assert.assertEquals(ConfAndMetadataUpgradeCommand.versionCompare(version1, version1), -1);
    }

    @Test
    public void testLessVersion() {
        String version1 = "1.2.3";
        String version2 = "1.2.4.1031";
        String version3 = "0.1.2";
        Assert.assertFalse(ConfAndMetadataUpgradeCommand.lessVersion(version1, version2));
        Assert.assertTrue(ConfAndMetadataUpgradeCommand.lessVersion(version1, version3));
        Assert.assertFalse(ConfAndMetadataUpgradeCommand.lessVersion(version1, version1));
        Assert.assertFalse(ConfAndMetadataUpgradeCommand.lessVersion("1.2.18.1059-GA", "1.3.0.1061-GA"));
        Assert.assertTrue(ConfAndMetadataUpgradeCommand.lessVersion("1.3.1.1062-GA", "1.3.0.1061-GA"));
    }

    @Test
    public void updateProperties() throws IOException {
        {
            String inputProperties = "## Properties\n\n" +
                    "## Whether to enable support for HIERARCHY_VISIBILITY and MEASURE_VISIBILITY restrictions.\n" +
                    "#insight.mdx.xmla.support-rowset-visibilities=true\n";
            String content = new ConfAndMetadataUpgradeCommand().generateProperties(new HashMap<>(),
                    new BufferedReader(new StringReader(inputProperties)), "1.2.18.1058-GA");
            Assert.assertTrue(content.contains("insight.mdx.xmla.support-rowset-visibilities=false"));
        }
        {
            String inputProperties = "## Properties\n\n" +
                    "## Whether to enable support for HIERARCHY_VISIBILITY and MEASURE_VISIBILITY restrictions.\n" +
                    "#insight.mdx.xmla.support-rowset-visibilities=true\n";
            Map<String, String> conf = new HashMap<>();
            conf.put("insight.mdx.xmla.support-rowset-visibilities", "false");
            String content = new ConfAndMetadataUpgradeCommand().generateProperties(conf,
                    new BufferedReader(new StringReader(inputProperties)), "1.2.18.1058-GA");
            Assert.assertTrue(content.contains("insight.mdx.xmla.support-rowset-visibilities=false"));

            conf.put("insight.mdx.xmla.support-rowset-visibilities", "true");
            content = new ConfAndMetadataUpgradeCommand().generateProperties(conf,
                    new BufferedReader(new StringReader(inputProperties)), "1.2.18.1058-GA");
            Assert.assertTrue(content.contains("insight.mdx.xmla.support-rowset-visibilities=true"));
        }
    }

    public static class Arguments implements ApplicationArguments {

        public String[] sourceArgs;

        public Arguments() {
            this.sourceArgs = new String[]{"upgrade", OLD_MDX_DIR, NEW_MDX_DIR, "copyLog", "mysql"};
        }

        @Override
        public String[] getSourceArgs() {
            return sourceArgs;
        }

        @Override
        public Set<String> getOptionNames() {
            return null;
        }

        @Override
        public boolean containsOption(String name) {
            return false;
        }

        @Override
        public List<String> getOptionValues(String name) {
            return null;
        }

        @Override
        public List<String> getNonOptionArgs() {
            return null;
        }
    }

    public static class UserTest implements UserInfoMapper {

        @Override
        public int insertSelective(UserInfo record) {
            return 0;
        }

        @Override
        public int selectLicenseNumWithLock() {
            return 0;
        }

        @Override
        public int selectLicenseNum() {
            return 0;
        }

        @Override
        public List<UserInfo> selectAll() {
            List<UserInfo> userInfos = new ArrayList<>();
            UserInfo userInfo1 = new UserInfo("ADMIN");
            UserInfo userInfo2 = new UserInfo("ADMIN");
            userInfos.add(userInfo1);
            userInfos.add(userInfo2);
            return userInfos;
        }

        @Override
        public UserInfo selectByUserName(String username) {
            return null;
        }

        @Override
        public UserInfo selectByUserAndPwd(String username, String password) {
            return null;
        }

        @Override
        public int updateByPrimaryKeySelective(UserInfo record) {
            return 0;
        }

        @Override
        public int updateLicenseAuthByUsername(String username, Integer licenseAuth) {
            return 0;
        }

        @Override
        public List<UserInfo> selectAllUsersByPage(RowBounds rowBounds) {
            return null;
        }

        @Override
        public List<String> selectAllUsersName() {
            return null;
        }

        @Override
        public int insertUsers(List<UserInfo> users) {
            return 0;
        }

        @Override
        public int deleteUsersByNames(List<String> userNames) {
            return 0;
        }

        @Override
        public UserInfo selectConfUsr() {
            return null;
        }

        @Override
        public int updateConfUsr(UserInfo userInfo) {
            return 0;
        }

        @Override
        public int deleteUsersById(List<Integer> ids) {
            return 0;
        }
    }

    static class MdxInfoTest implements MdxInfoMapper {

        @Override
        public String getDatabaseVersion() {
            return null;
        }

        @Override
        public int deleteByPrimaryKey(Object o) {
            return 0;
        }

        @Override
        public int delete(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public int insert(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public int insertSelective(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public boolean existsWithPrimaryKey(Object o) {
            return false;
        }

        @Override
        public List<MdxInfo> selectAll() {
            return null;
        }

        @Override
        public MdxInfo selectByPrimaryKey(Object o) {
            return new MdxInfo(1, "1.2.0-1031", 0L, 0L);
        }

        @Override
        public int selectCount(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public List<MdxInfo> select(MdxInfo mdxInfo) {
            return null;
        }

        @Override
        public MdxInfo selectOne(MdxInfo mdxInfo) {
            return null;
        }

        @Override
        public int updateByPrimaryKey(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public int updateByPrimaryKeySelective(MdxInfo mdxInfo) {
            return 0;
        }

        @Override
        public int deleteByExample(Object o) {
            return 0;
        }

        @Override
        public List<MdxInfo> selectByExample(Object o) {
            return null;
        }

        @Override
        public int selectCountByExample(Object o) {
            return 0;
        }

        @Override
        public MdxInfo selectOneByExample(Object o) {
            return null;
        }

        @Override
        public int updateByExample(MdxInfo mdxInfo, Object o) {
            return 0;
        }

        @Override
        public int updateByExampleSelective(MdxInfo mdxInfo, Object o) {
            return 0;
        }

        @Override
        public List<MdxInfo> selectByExampleAndRowBounds(Object o, RowBounds rowBounds) {
            return null;
        }

        @Override
        public List<MdxInfo> selectByRowBounds(MdxInfo mdxInfo, RowBounds rowBounds) {
            return null;
        }
    }

}
