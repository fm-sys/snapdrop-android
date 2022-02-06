package com.fmsys.snapdrop.utils;

import android.content.SharedPreferences;
import android.util.Log;

import com.fmsys.snapdrop.R;
import com.fmsys.snapdrop.SnapdropApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtils {
    private static String logcatLogs;

    private LogUtils() {
        // utility class
    }

    public static String getLogs(final SharedPreferences prefs, final boolean refresh) {
        if (refresh) {
            logcatLogs = prefs.getString(SnapdropApplication.getInstance().getApplicationContext().getString(R.string.pref_last_crash), "") + requestLogcatLogs();
        }
        return logcatLogs;
    }

    private static String requestLogcatLogs() {
        String logs = "Unable to read logs";
        try {
            // Only filter log messages which are important for us...
            final Process process = Runtime.getRuntime().exec("logcat *:I eglCodecCommon:S -d");
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            final StringBuilder logsBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logsBuilder.append(line).append("\n");
            }
            logs = logsBuilder.toString();
            bufferedReader.close();
        } catch (IOException e) {
            Log.e("LogUtils", "Exception while reading logs", e);
        }
        return logs;
    }

    public static String getStacktrace(final Throwable ex) {
        final StringBuilder builder = new StringBuilder(getStacktraceSegment(ex));
        Throwable cause = ex.getCause();
        while (cause != null) {
            builder.append("caused by: ").append(getStacktraceSegment(cause));
            cause = cause.getCause();
        }
        return builder.toString();
    }

    private static String getStacktraceSegment(final Throwable ex) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw + "\n";
    }
}
