/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.core.support;

import io.micrometer.core.instrument.*;


/**
 * MDX Queries monitoring indicators
 */
public class MdxQueryMetrics extends Metrics{
    public static Timer mdxQueryRequestTimer() {
        return Timer.builder("mdx_query_time")
                .publishPercentiles(0.90, 0.95, 0.99, 0.999)
                .publishPercentileHistogram()
                .tag("method", "request")
                .register(globalRegistry);
    }

    public static Counter mdxQueryRequestTotal(){
        return Counter
                .builder("mdx_query_count")
                // optional
                .description("Total number of MDX queries")
                .tag("method", "request")
                .register(globalRegistry);
    }

    public static Counter mdxQueryRequestFail(){
        return Counter
                .builder("mdx_query_fail")
                .description("Number of MDX query failures") // optional
                .tag("method", "request")
                .register(globalRegistry);
    }

    public static DistributionSummary mdxQueryRequestSummary(){
        return DistributionSummary.builder("mdx_query_summary")
                .publishPercentiles(0.90, 0.95, 0.99, 0.999)
                .description("MDX query summary") // optional
                .baseUnit("ms") // optional
                .tag("method", "request")
                .register(globalRegistry);
    }

    public static Timer sqlExecutedTimer() {
        return Timer.builder("sql_executed_time")
                .publishPercentiles(0.90, 0.95, 0.99, 0.999)
                .publishPercentileHistogram()
                .tag("method", "query")
                .register(globalRegistry);
    }

    public static Counter sqlExecutedTotal(){
        return Counter
                .builder("sql_executed_count")
                .description("Total number of sql queries") // optional
                .tag("method", "query")
                .register(globalRegistry);
    }

    public static Counter sqlExecutedFail(){
        return Counter
                .builder("sql_executed_fail")
                .description("Number of sql query failures") // optional
                .tag("method", "query")
                .register(globalRegistry);
    }

    public static DistributionSummary sqlExecutedSummary(){
        return DistributionSummary.builder("sql_executed_summary")
                .publishPercentiles(0.90, 0.95, 0.99, 0.999)
                .description("sql query summary") // optional
                .baseUnit("ms") // optional
                .tag("method", "query")
                .register(globalRegistry);
    }
}
