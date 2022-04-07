package io.kylin.mdx.insight.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author qi.wu
 */

@Slf4j
public class ShellUtils {

    public static Process executeShell(String shellCommand) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS ");
        log.info("shell scripts ready at {}", dateFormat.format(new Date()));
        log.info("shell command: {}", shellCommand);
        Process pid = null;
        pid = Runtime.getRuntime().exec(shellCommand);
        if (pid != null) {
            log.info("PID {} is created", pid.toString());
        } else {
            log.error("No pid");
        }
        return pid;
    }

    public static int getPidStatus(Process pid) throws InterruptedException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS ");
        int status = -1;
        /* TODO: add timeout */
        status = pid.waitFor();
        if (status != 0) {
            log.error("shell script error");
        } else {
            log.info("shell script exec success at {}", dateFormat.format(new Date()));
        }
        return status;
    }

    public static String getPidName(Process pid) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pid.getInputStream()));
        String line = null;
        String pre = null;
        while (bufferedReader != null && (line = bufferedReader.readLine()) != null) {
            pre = line;
        }
        return pre;
    }
}

