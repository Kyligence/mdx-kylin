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


package io.kylin.mdx.insight.server.bean;

import lombok.Data;

import java.util.List;

@Data
public class Page<T> {

    private int pageNum;

    private int pageSize;

    private List<T> list;

    private long total;

    public Page(List<T> list) {
        this.list = list;

        if (list instanceof com.github.pagehelper.Page) {
            com.github.pagehelper.Page page = (com.github.pagehelper.Page) list;
            this.pageNum = page.getPageNum() - 1;
            this.pageSize = page.getPageSize();

            this.total = page.getTotal();
        }
    }

    public void setPageInfo(List list) {
        if (list instanceof com.github.pagehelper.Page) {
            com.github.pagehelper.Page page = (com.github.pagehelper.Page) list;
            this.pageNum = page.getPageNum() - 1;
            this.pageSize = page.getPageSize();

            this.total = page.getTotal();
        }
    }
}
