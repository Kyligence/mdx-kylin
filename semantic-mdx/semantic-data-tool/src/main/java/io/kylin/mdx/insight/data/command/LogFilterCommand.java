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


package io.kylin.mdx.insight.data.command;

import io.kylin.mdx.insight.common.util.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFilterCommand implements CommandLineExecutor {

    static final String CONDITION_FORMAT_STRING = "yyyy-MM-dd_HH:mm:ss";
    static final String LINE_DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss";

    @Override
    public void execute(String[] args) {
        if (args == null || args.length != 4) {
            System.out.println("Only allow 3 args. Please check args size\n");
            return;
        }

        String filePath = args[1];
        String startAt = args[2];
        String endAt = args[3];
        SimpleDateFormat sdf = new SimpleDateFormat(CONDITION_FORMAT_STRING);
        startAt = sdf.format(new Date(Long.parseLong(startAt)));
        endAt = sdf.format(new Date(Long.parseLong(endAt)));
        System.out.println("File path is : " + filePath);
        System.out.println("Start time is : " + startAt);
        System.out.println("End time is : " + endAt);

        Pair<Long, Long> startAndEnd;
        Date startDate;
        Date endDate;
        try {
            SimpleDateFormat conditionDateFormat = new SimpleDateFormat(CONDITION_FORMAT_STRING);
            startDate = conditionDateFormat.parse(startAt);
            endDate = conditionDateFormat.parse(endAt);
            startAndEnd = Pair.of(startDate.getTime(), endDate.getTime());
        } catch (ParseException e) {
            System.out.println("Bad format of date: " + e.getMessage());
            return;
        }

        File logFile = new File(filePath);
        if (!logFile.exists() || !logFile.isFile() || !Utils.isTargetFile(startDate, logFile)) {
            System.out.println(" No data in this time range\n\n");
            return;
        }

        try {
            extractFromCurrentFile(startAndEnd, logFile);
        } catch (IOException | ParseException e) {
            System.out.println("Extracting failure due to : " + e.getMessage());
        }


    }

    private void extractFromCurrentFile(Pair<Long, Long> startAndEnd, File logFile) throws IOException, ParseException {
        String line = "";
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat lineStartDateFormat = new SimpleDateFormat(LINE_DATE_FORMAT_STRING);
        Long start = startAndEnd.getLeft();
        Long end = startAndEnd.getRight();
        System.out.println("Current file is target file");
        boolean alreadyStartCopy = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            while ((line = br.readLine()) != null) {
                if (!logFile.getName().contains("heapdump.hprof") && !logFile.getName().contains("semantic.out")) {
                    ///extract data in specified time range
                    String lineStart = "";
                    // the format of date time in gc.log：2020-11-25T14:48:01.484+0800: 0.679
                    if (logFile.getName().startsWith("gc") && logFile.getName().endsWith(".log")) {
                        String[] linePartArray = line.split(":");
                        if (linePartArray.length >= 3) {
                            lineStart = linePartArray[0].replace("T", " ") + ":" + linePartArray[1] + ":" + "00";
                        }
                    } else {
                        lineStart = line.split(",")[0];
                    }
                    if (!Utils.isDateString(lineStart, LINE_DATE_FORMAT_STRING)) {
                        if (alreadyStartCopy) {
                            sb.append(line).append("\n");
                        }
                        continue;
                    }
                    Date dateInLine = lineStartDateFormat.parse(lineStart);
                    Long inLineTime = dateInLine.getTime();
                    int toStart = inLineTime.compareTo(start);
                    int toEnd = inLineTime.compareTo(end);
                    if (toEnd > 0) {
                        break;
                    }
                    if (toStart < 0) {
                        continue;
                    }
                    alreadyStartCopy = true;
                }
                sb.append(line).append("\n");
            }
            if (sb.toString().isEmpty()) {
                //There is no data in specified time range from this file
                sb.append(" No data in this time range\n\n");
            }
        }

        System.out.println(sb.toString());
    }
}
