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


package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.service.RepairFileService;
import io.kylin.mdx.insight.core.support.ExcelFileDataSourceInfo;
import io.kylin.mdx.insight.core.support.IOExceptionConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("api")
public class RepairFileController {
    private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8";
    private static final String CONTENT_DISPOSITION_TEMPLATE = "attachment; filename=\"%s\"";

    private final Optional<RepairFileService> repairFileService;

    @Autowired
    public RepairFileController(Optional<RepairFileService> repairFileService) {
        this.repairFileService = repairFileService;
    }

    @PostMapping("/helper/schema-source/excel")
    public ResponseEntity<ExcelFileDataSourceInfo> checkExcelFileDataSources(@RequestParam("file") MultipartFile file) throws IOException {
        if (!repairFileService.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_IMPLEMENTED)
                    .build();
        }

        ExcelFileDataSourceInfo result = repairFileService.get().checkDataSources(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/helper/schema-repair/excel", produces = XLSX_MIME_TYPE)
    public ResponseEntity<StreamingResponseBody> repairExcelFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (!repairFileService.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_IMPLEMENTED)
                    .build();
        }

        IOExceptionConsumer<OutputStream> result = repairFileService.get().repairFile(file);
        String contentDisposition = String.format(CONTENT_DISPOSITION_TEMPLATE,
                URLEncoder.encode("repaired_" + file.getOriginalFilename(), "UTF-8"))
                .replace("+", "%20");

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, XLSX_MIME_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(result::accept);
    }

    @GetMapping("/helper/configuration")
    @ResponseBody
    public Map<String, Long> fileSizeProperty() {
        DataSize uploadFileMaxSize = DataSize.parse(SemanticConfig.getInstance().getUploadFileMaxSize());
        return Collections.singletonMap("repair-excel-limit", uploadFileMaxSize.toBytes());
    }
}
