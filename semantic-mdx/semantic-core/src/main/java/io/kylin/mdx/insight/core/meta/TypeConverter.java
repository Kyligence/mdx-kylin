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


package io.kylin.mdx.insight.core.meta;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TypeConverter {


    private static final Map<Integer, String> TYPE_VALUES_MAP;

    static {
        Class clazz = java.sql.Types.class;
        Field[] fields = java.sql.Types.class.getDeclaredFields();
        TYPE_VALUES_MAP = new HashMap<>(fields.length);

        try {
            // load java.sql.types fields
            for (Field field : fields) {
                TYPE_VALUES_MAP.put(field.getInt(clazz), field.getName().toUpperCase());
            }

        } catch (IllegalAccessException e) {
            log.error("can not init tableau mappings", e);
        }
    }

    public static String getLiteralSqlType(Integer dataType) {
        return TYPE_VALUES_MAP.get(dataType);
    }


}
