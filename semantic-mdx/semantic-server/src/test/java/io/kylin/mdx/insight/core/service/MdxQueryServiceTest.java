package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.entity.BasicStatistics;
import io.kylin.mdx.insight.core.entity.QueryCostStatistics;
import io.kylin.mdx.insight.core.entity.StatisticsTrend;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class MdxQueryServiceTest extends BaseEnvSetting {

    @Mock
    private MdxQueryService MdxQueryService;

    private static final Long startTime = 1608026117653L;

    private final Long endTime = 1608112517653L;

    private final String projectName = "learn_kylin";

    private final List<String> datasetNames = Collections.singletonList("1");

    private final List<Long> axis = Arrays.asList(1608026117653L, 1608112517653L);

    @Test
    public void getBasicStatisticsTest() {
        BasicStatistics basicStatistics = MdxQueryService.getBasicStatistics(startTime, endTime, projectName);
        Assert.assertNull(basicStatistics);
    }

    @Test
    public void getStatisticsTrendTest() {
        StatisticsTrend statisticsTrend = MdxQueryService.getStatisticsTrend(axis, projectName, datasetNames);
        Assert.assertNull(statisticsTrend);
    }

    @Test
    public void getQueryCostStatisticsTest() {
        QueryCostStatistics queryCostStatistics = MdxQueryService.getQueryCostStatistics(axis, projectName);
        Assert.assertNull(queryCostStatistics);
    }

    @Test
    public void getRankingStatisticsTest() {
        Long count = 10L;
        String direction = "ASC";
        List<Map<String, String>> rankingStatistics = MdxQueryService.getRankingStatistics(startTime, endTime, count, direction, projectName);
        Assert.assertNotNull(rankingStatistics);
    }

}
