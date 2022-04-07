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


package io.kylin.mdx.insight.common.async;

/**
 * @author wanghui
 * Created 2016-01-11 下午5:33
 */
public abstract class ParamsTask implements Runnable {

    public static final int FIRST = 0;
    public static final int SECOND = 1;
    public static final int THREE = 2;
    public static final int FOUR = 3;
    public static final int FIVE = 4;
    public static final int SIX = 5;
    public static final int SEVEN = 6;
    public static final int EIGHT = 7;
    public static final int NINE = 8;
    public static final int TEN = 9;

    private final Object[] paramsList;

    protected ParamsTask(Object... params) {
        this.paramsList = new Object[params.length];
        System.arraycopy(params, 0, this.paramsList, 0, params.length);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int id) {
        return (T) paramsList[id];
    }

    public <T> T get(int id, Class<T> clazz) {
        return clazz.cast(paramsList[id]);
    }

}
