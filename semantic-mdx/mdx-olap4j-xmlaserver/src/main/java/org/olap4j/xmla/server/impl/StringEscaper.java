/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.olap4j.xmla.server.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility for replacing special characters
 * with escape sequences in strings.
 *
 * <p>To create a StringEscaper, create a builder.
 * Initially it is the identity transform (it leaves every character unchanged).
 * Call {@link Builder#defineEscape} as many
 * times as necessary to set up mappings, and then call {@link Builder#build}
 * to create a StringEscaper.</p>
 *
 * <p>StringEscaper is immutable, but you can call {@link #toBuilder()} to
 * get a builder back.</p>
 *
 * <p>Several escapers are pre-defined:</p>
 *
 * <dl>
 *     <dt>{@link #HTML_ESCAPER}</dt>
 *     <dd>HTML (using &amp;amp;, &amp;&lt;, etc.)</dd>
 *     <dt>{@link #XML_ESCAPER}</dt>
 *     <dd>XML (same as HTML escaper)</dd>
 *     <dt>{@link #XML_NUMERIC_ESCAPER}</dt>
 *     <dd>Uses numeric codes, e.g. &amp;#38; for &amp;.</dd>
 *     <dt>{@link #URL_ARG_ESCAPER}</dt>
 *     <dd>Converts '?' and '&amp;' in URL arguments into URL format</dd>
 *     <dt>{@link #URL_ESCAPER}</dt>
 *     <dd>Converts to URL format</dd>
 * </dl>
 */
public class StringEscaper {
    private final String[] translationTable;

    public static final StringEscaper PLAIN_TEXT_ESCAPER =
            new Builder().build();

    public static final StringEscaper HTML_ESCAPER =
            new Builder()
                    .defineEscape('&', "&amp;")
                    .defineEscape('"', "&quot;")// not "&apos;"
                    .defineEscape('\'', "&#39;")
                    .defineEscape('<', "&lt;")
                    .defineEscape('>', "&gt;")
                    .build();

    public static final StringEscaper XML_ESCAPER = HTML_ESCAPER;

    public static final StringEscaper XML_NUMERIC_ESCAPER =
            new Builder()
                    .defineEscape('&', "&#38;")
                    .defineEscape('"', "&#34;")
                    .defineEscape('\'', "&#39;")
                    .defineEscape('<', "&#60;")
                    .defineEscape('>', "&#62;")
                    .defineEscape('\t', "&#9;")
                    .defineEscape('\n', "&#10;")
                    .defineEscape('\r', "&#13;")
                    .build();

    public static final StringEscaper URL_ARG_ESCAPER =
            new Builder()
                    .defineEscape('?', "%3f")
                    .defineEscape('&', "%26")
                    .build();

    public static final StringEscaper URL_ESCAPER =
            URL_ARG_ESCAPER.toBuilder()
                    .defineEscape('%', "%%")
                    .defineEscape('"', "%22")
                    .defineEscape('\r', "+")
                    .defineEscape('\n', "+")
                    .defineEscape(' ', "+")
                    .defineEscape('#', "%23")
                    .build();

    /**
     * Creates a StringEscaper. Only called from Builder.
     */
    private StringEscaper(String[] translationTable) {
        this.translationTable = translationTable;
    }

    /**
     * Apply an immutable transformation to the given string.
     */
    public String escapeString(String s) {
        StringBuilder sb = null;
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            String escape;
            // codes >= 128 (e.g. Euro sign) are always escaped
            if (c > 127) {
                escape = "&#" + Integer.toString(c) + ";";
            } else if (c >= translationTable.length) {
                escape = null;
            } else {
                escape = translationTable[c];
            }
            if (escape == null) {
                if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(n * 2);
                    sb.append(s, 0, i);
                }
                sb.append(escape);
            }
        }

        if (sb == null) {
            return s;
        } else {
            return sb.toString();
        }
    }


    public void appendEscapedString(String s, Appendable sb) throws IOException {
        appendEscapedString(s, sb, false, false);
    }


    /**
     * Applies an immutable transformation to the given string, writing the
     * results to an {@link Appendable} (such as a {@link StringBuilder}).
     */
    public void appendEscapedString(String s, Appendable sb, boolean nowrap, boolean pure) throws IOException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            // 用于替代使用正则匹配 \r\n
            if (nowrap) {
                if (c == '\r') {
                    // \r
                    c = ' ';
                    if (i + 1 < n && s.charAt(i + 1) == '\n') {
                        // \r\n
                        i++;
                    }
                } else if (c == '\n') {
                    // \n
                    c = ' ';
                }
            }

            if (c >= 0xd800 && c <= 0xdbff && i + 1 < n) {
                c = (char) (((c - 0xd7c0) << 10) | (s.charAt(++i) & 0x3ff));
            }
            if (c < 0x80) {
                if (c < 0x20 && (c != '\t' && c != '\r' && c != '\n')) {
                    sb.append("&#xfffd;");
                } else {
                    // if data from pure character, do not escape
                    if (pure) {
                        sb.append(c);
                    } else {
                        switch (c) {
                            case '&':
                                sb.append("&#38;");
                                break;
                            case '>':
                                sb.append("&#62;");
                                break;
                            case '<':
                                sb.append("&#60;");
                                break;
                            case '\'':
                                sb.append("&#39;");
                                break;
                            case '\"':
                                sb.append("&#34;");
                                break;
                            default:
                                sb.append(c);
                        }
                    }
                }
            } else if ((c >= 0xd800 && c <= 0xdfff) || c == 0xfffe || c == 0xffff) {
                sb.append("&#xfffd;");
            } else {
                sb.append(c);
            }
        }
    }


    /**
     * Creates a builder from an existing escaper.
     */
    public Builder toBuilder() {
        return new Builder(
                new ArrayList<String>(Arrays.asList(translationTable)));
    }

    /**
     * Builder for {@link StringEscaper} instances.
     */
    public static class Builder {
        private final List<String> translationVector;

        public Builder() {
            this(new ArrayList<String>());
        }

        private Builder(List<String> translationVector) {
            this.translationVector = translationVector;
        }

        /**
         * Creates an escaper with the current state of the translation
         * table.
         *
         * @return A string escaper
         */
        public StringEscaper build() {
            return new StringEscaper(
                    translationVector.toArray(new String[0]));
        }

        /**
         * Map character "from" to escape sequence "to"
         */
        public Builder defineEscape(char from, String to) {
            int i = (int) from;
            if (i >= translationVector.size()) {
                // Extend list by adding the requisite number of nulls.
                translationVector.addAll(
                        Collections.<String>nCopies(
                                i + 1 - translationVector.size(), null));
            }
            translationVector.set(i, to);
            return this;
        }
    }
}

// End StringEscaper.java
