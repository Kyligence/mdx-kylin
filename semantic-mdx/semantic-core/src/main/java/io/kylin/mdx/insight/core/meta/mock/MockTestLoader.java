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


package io.kylin.mdx.insight.core.meta.mock;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mock KYLIN API 调用结果
 */
public class MockTestLoader {

    /**
     * uri -> path
     */
    private static final Map<Entry, Value> MATCHES = new HashMap<>();

    public static void put(Entry entry, Value value) {
        MATCHES.put(entry, value);
    }

    public static void put(Entry entry, String path) {
        MATCHES.put(entry, new Const(path));
    }

    public static void clear() {
        MATCHES.clear();
    }

    public static String load(String uri) {
        return getContentByInputStream(findPathByUri(uri));
    }

    private static String findPathByUri(String uri) {
        for (Map.Entry<Entry, Value> entry : MATCHES.entrySet()) {
            if (entry.getKey().match(uri)) {
                return entry.getValue().get(uri);
            }
        }
        throw new IllegalArgumentException("No matches uri : " + uri);
    }

    public static String getContentByInputStream(String path) {
        try (InputStream input = MockTestLoader.class.getResourceAsStream(path)) {
            return IOUtils.toString(input);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static abstract class Entry {

        public abstract boolean match(String uri);

    }

    public static class Full extends Entry {

        private final String matches;

        public Full(String matches) {
            this.matches = matches;
        }

        @Override
        public boolean match(String uri) {
            return matches.equals(uri);
        }

    }

    public static class Contains extends Entry {

        private final String matches;

        public Contains(String matches) {
            this.matches = matches;
        }

        @Override
        public boolean match(String uri) {
            return uri.contains(matches);
        }

    }

    public static class Prefix extends Entry {

        private final String matches;

        public Prefix(String matches) {
            this.matches = matches;
        }

        @Override
        public boolean match(String uri) {
            return uri.startsWith(matches);
        }

    }

    public static class Suffix extends Entry {

        private final String matches;

        public Suffix(String matches) {
            this.matches = matches;
        }

        @Override
        public boolean match(String uri) {
            return uri.endsWith(matches);
        }

    }

    public static class Regex extends Entry {

        private final Pattern pattern;

        public Regex(String matches) {
            this.pattern = Pattern.compile(matches);
        }

        @Override
        public boolean match(String uri) {
            return pattern.matcher(uri).find();
        }

    }

    public static class Predicate extends Entry {

        private final java.util.function.Predicate<String> predicate;

        public Predicate(java.util.function.Predicate<String> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean match(String uri) {
            return predicate.test(uri);
        }

    }

    public static abstract class Value {

        public abstract String get(String uri);

    }

    public static class Const extends Value {

        private final String value;

        public Const(String value) {
            this.value = value;
        }

        @Override
        public String get(String uri) {
            return value;
        }

    }

}
