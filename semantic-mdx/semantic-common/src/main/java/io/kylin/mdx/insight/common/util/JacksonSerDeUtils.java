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


package io.kylin.mdx.insight.common.util;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Slf4j
public class JacksonSerDeUtils {

    private static final ObjectMapper defaultJsonMapper;

    private static final XmlMapper defaultXmlMapper;

    static {
        // init json mapper
        defaultJsonMapper = new ObjectMapper();
        defaultJsonMapper.enable(SerializationFeature.INDENT_OUTPUT);
        defaultJsonMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        defaultJsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        defaultJsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        defaultJsonMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        defaultJsonMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        defaultJsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        defaultJsonMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // init xml mapper
        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        XMLOutputFactory output = new WstxOutputFactory();
        output.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        defaultXmlMapper = new XmlMapper(input, output);
        defaultXmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
        defaultXmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        defaultXmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        defaultXmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        defaultXmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    public static ObjectMapper getJsonMapper() {
        return defaultJsonMapper;
    }

    public static XmlMapper getXmlMapper() {
        return defaultXmlMapper;
    }

    public static <T> T readJson(URL url, Class<T> clazz) {
        try {
            return getJsonMapper().readValue(url, clazz);
        } catch (IOException e) {
            log.error("error when read json from URL {}", url, e);
        }
        return null;
    }

    public static <T> T readInputStream(InputStream inputStream, Class<T> clazz) {
        try {
            return getJsonMapper().readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("error when read json from InputStream {}", inputStream, e);
        }
        return null;
    }

    public static <T> T readString(String str, Class<T> clazz) {
        try {
            return getJsonMapper().readValue(str, clazz);
        } catch (IOException e) {
            log.error("error when read json from str {}", str, e);
        }
        return null;
    }

    public static <T> T readXmlToClazz(String xml, Class<T> clazz) {
        try {
            return getXmlMapper().readValue(xml, clazz);
        } catch (IOException e) {
            log.error("error when read json from json {}", xml, e);
        }
        return null;
    }

    public static String writeXmlAsString(Object obj) {
        try {
            return getXmlMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("error when write xml", e);
        }
        return null;
    }

    public static byte[] writeJsonAsByte(Object obj) {
        try {
            return getJsonMapper().writeValueAsBytes(obj);
        } catch (JsonProcessingException e){
            log.error("error when write json", e);
        }
        return null;
    }


    public static <T> T readXml(URL url, Class<T> clazz) {
        try {
            return getXmlMapper().readValue(url, clazz);
        } catch (Exception e) {
            log.error("error when read xml from URL {}", url, e);
        }
        return null;
    }

    public static <T> T readXml(File file, Class<T> clazz) {
        try {
            return getXmlMapper().readValue(file, clazz);
        } catch (Exception e) {
            log.error("error when read xml from file {}", file, e);
        }
        return null;
    }
}
