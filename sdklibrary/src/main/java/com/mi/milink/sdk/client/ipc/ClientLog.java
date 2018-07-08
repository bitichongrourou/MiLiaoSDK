
package com.mi.milink.sdk.client.ipc;

import java.io.File;
import java.io.IOException;

import android.os.AsyncTask;

import com.mi.milink.sdk.base.debug.FileTracer;
import com.mi.milink.sdk.base.debug.FileTracerReader;
import com.mi.milink.sdk.base.debug.LogcatTracer;
import com.mi.milink.sdk.base.debug.TraceLevel;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.data.Option;
import com.mi.milink.sdk.debug.MiLinkTracer;
import com.mi.milink.sdk.util.CommonUtils;
import com.mi.milink.sdk.util.FileUtils;

/**
 * 客户端日志接口<br>
 * 该类提供了与{@link android.util.Log}类似的功能，按等级打印日志 可以打包某一天的NS和客户端日志，用于上传日志
 *
 * @author MK
 */
public class ClientLog extends MiLinkTracer {

    private static ClientLog sInstance = null;

    /**
     * 获取一个MnsClientLog实例
     *
     * @return MnsClientLog实例
     */
    public static ClientLog getInstance() {
        if (sInstance == null) {
            synchronized (ClientLog.class) {
                if (sInstance == null) {
                    sInstance = new ClientLog();
                }
            }
        }

        return sInstance;
    }

    /**
     * 设置最大的单日日志文件大小<br>
     *
     * @param maxSize 单位：字节
     */
    public static void setMaxFolderSize(long maxSize) {
        int blockCount = (int) (maxSize / Const.Debug.DefFileBlockSize);

        if (!Const.Debug.InfiniteTraceFile) {
            if (blockCount < 1) {
                blockCount = Const.Debug.DefFileBlockCount;
            }
        } else {
            blockCount = 1024;
        }

        Option.putInt(Const.Debug.ClientFileBlockCount, blockCount).commit();
    }

    /**
     * 设置日志文件最长保管周期<br>
     * 如果设置的日期小于1天，则设置为默认周期，
     *
     * @param maxPeriod 单位: 毫秒
     */
    public static void setMaxKeepPeriod(long maxPeriod) {
        if (maxPeriod < 24 * 60 * 60 * 1000L) {
            maxPeriod = Const.Debug.DefFileKeepPeriod;
        }

        Option.putLong(Const.Debug.ClientFileKeepPeriod, maxPeriod).commit();
    }

    /**
     * 设置文件日志追踪级别<br>
     * {@code level}应该是{@link TraceLevel} 中的某个常量或者其中若干个的按位或操作的结果<br>
     * 如果{@code level} >{@link TraceLevel#ALL} 或者{@code level} <0, 那么级别设置为
     *
     * @param level 参考级别常量
     */
    public static void setFileTraceLevel(int level) {
        int traceLevel = level;

        if (level > ALL || level < 0) {
            traceLevel = Const.Debug.DefFileTraceLevel;
        }

        Option.putInt(Const.Debug.ClientFileTraceLevel, traceLevel).commit();
    }

    /**
     * 设置logcat日志追踪级别<br>
     * {@code level}应该是{@link TraceLevel} 中的某个常量或者其中若干个的按位或操作的结果<br>
     * 如果{@code level} >{@link TraceLevel#ALL} 或者{@code level} <0, 那么级别设置为
     *
     * @param level 参考级别常量
     */
    public static void setLogcatTraceLevel(int level) {
        int traceLevel = level;

        if (level > ALL || level < 0) {
            traceLevel = Const.Debug.DefLogcatTraceLevel;
        }

        Option.putInt(Const.Debug.ClientLogcatTraceLevel, traceLevel).commit();
    }

    // ------------------------------------------------------------------------------
    // 日志打印方法
    // ------------------------------------------------------------------------------
    public static final void v(String tag, String msg) {
        getInstance().trace(TraceLevel.VERBOSE, tag, msg, null);
    }

    public static final void v(String tag, String msg, Throwable tr) {
        getInstance().trace(TraceLevel.VERBOSE, tag, msg, tr);
    }

    public static final void d(String tag, String msg) {
        getInstance().trace(TraceLevel.DEBUG, tag, msg, null);
    }

    public static final void d(String tag, String msg, Throwable tr) {
        getInstance().trace(TraceLevel.DEBUG, tag, msg, tr);
    }

    public static final void i(String tag, String msg) {
        getInstance().trace(TraceLevel.INFO, tag, msg, null);
    }

    public static final void i(String tag, String msg, Throwable tr) {
        getInstance().trace(TraceLevel.INFO, tag, msg, tr);
    }

    public static final void w(String tag, String msg) {
        getInstance().trace(TraceLevel.WARN, tag, msg, null);
    }

    public static final void w(String tag, String msg, Throwable tr) {
        getInstance().trace(TraceLevel.WARN, tag, msg, tr);
    }

    public static final void e(String tag, String msg) {
        getInstance().trace(TraceLevel.ERROR, tag, msg, null);
    }

    public static final void e(String tag, String msg, Throwable tr) {
        getInstance().trace(TraceLevel.ERROR, tag, msg, tr);
    }

    // ------------------------------------------------------------------------------
    // 日志辅助方法
    // ------------------------------------------------------------------------------
    /**
     * 打包某一天的MNS和客户端日志，用于上传日志
     *
     * @param time 时间，单位ms， 哪一天的日志
     * @param tempFolder 临时文件夹
     * @param destFile 目标文件(ZIP)
     * @return 是否打包成功
     */
    public static boolean packLogs(long time, File tempFolder, File destFile) {
        return getInstance().pack(time, tempFolder, destFile);
    }

    /**
     * 确保日志全部写入文件
     */
    public static void ensureLogsToFile() {
        getInstance().flush();
    }

    protected ClientLog() {
        super();
        fileTracer = new FileTracer(CLIENT_CONFIG);
        logcatTracer = new LogcatTracer();
        MiLinkTracer.setInstance(this);
        onSharedPreferenceChanged(null, Const.Debug.ClientFileTraceLevel);
        onSharedPreferenceChanged(null, Const.Debug.ClientLogcatTraceLevel);
    }

    /**
     * 打包某一天的MNS和客户端日志，用于上传日志
     *
     * @param time 时间，单位ms， 哪一天的日志
     * @param tempFolder 临时文件夹
     * @param destFile 目标文件(ZIP)
     * @return 是否打包成功
     */
    public boolean pack(long time, File tempFolder, File destFile) {
        // 打包MNS的日志文件
        File mnsLogFile = new FileTracerReader(SERVICE_CONFIG).pack(time, tempFolder, false);

        // 打包CLIENT的日志文件
        File clientLogFile = new FileTracerReader(CLIENT_CONFIG).pack(time, tempFolder, false);

        // 打包Channel的日志文件
        File channelLogFile = new FileTracerReader(CHANNEL_CONFIG).pack(time, tempFolder, false);
        
        // 压缩这两个文件
        return FileUtils.zip(new File[] {
                mnsLogFile, clientLogFile,channelLogFile
        }, destFile);
    }

    // 找到问题原因了,稳定后可以去掉
    private static AsyncTask<Void, Void, Void> generateSystemLogThread = null;

    public synchronized static void generateSystemLog() {
        if (generateSystemLogThread != null) {
            return;
        }
        generateSystemLogThread = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPostExecute(Void result) {
                generateSystemLogThread = null;
            }

            @Override
            protected Void doInBackground(Void... params) {
                String fileFolderPath = getLogFilePath().getAbsolutePath()+File.separator
                        + CommonUtils.createDataFormat("yyyy-MM-dd").format(
                                System.currentTimeMillis()) + File.separator;
                String fileName = "1.system.log";
                String filePath = fileFolderPath + fileName;
                String cmd = String.format("logcat -v time -f %s -t %d", filePath, 8000);
                v("Command", cmd);
                try {
                    Runtime.getRuntime().exec(cmd);
                    Runtime.getRuntime().exec("logcat -c");
                } catch (IOException e) {
                }
                return null;
            }
        };
        generateSystemLogThread.execute();
    }
}
