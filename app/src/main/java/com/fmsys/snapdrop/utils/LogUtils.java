package com.fmsys.snapdrop.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogUtils {
    public static String logcatLogs;

    private LogUtils() {
        // utility class
    }

    public static String requestLogcatLogs() {
        try {
            // Only filter log messages which are important for us...
            final Process process = Runtime.getRuntime().exec("logcat *:I eglCodecCommon:S -d");
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            final StringBuilder logs = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logs.append(line).append("\n");
            }
            logcatLogs = logs.toString();
            bufferedReader.close();
        } catch (IOException e) {
            Log.e("LogUtils", "Exception while reading logs", e);
        }
        return logcatLogs;
    }
}
