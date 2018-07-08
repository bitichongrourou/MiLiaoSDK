
package com.mi.milink.sdk.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.debug.FileTracer;
import com.mi.milink.sdk.base.debug.FileTracerConfig;
import com.mi.milink.sdk.base.debug.LogcatTracer;
import com.mi.milink.sdk.base.debug.TraceLevel;
import com.mi.milink.sdk.base.os.Device;
import com.mi.milink.sdk.base.os.info.StorageDash;
import com.mi.milink.sdk.base.os.info.StorageInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.data.Option;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * MiLink日志追踪器<br>
 * <p>
 * 有三个开关(默认为全部打开)：<br>
 * {@link #setEnabled(boolean)} ：是否打印日志；<br>
 * {@link #setFileTracerEnabled(boolean)} :是否打印日志到文件；<br>
 * {@link #setLogcatTracerEnabled(boolean)} :是否打印日志到logcat;<br>
 * </p>
 * 
 * @author MK
 */
public class MiLinkTracer implements TraceLevel, SharedPreferences.OnSharedPreferenceChangeListener {
    private static MiLinkTracer sInstance = null;

    protected static final FileTracerConfig CLIENT_CONFIG;

    protected static final FileTracerConfig SERVICE_CONFIG;
    
    protected static final FileTracerConfig CHANNEL_CONFIG;

    static {
        File rootPath = getLogFilePath();
        Global.getClientAppInfo().setLogPath(rootPath.getAbsolutePath());
        int clientBlockCount = Global.isDebug() ? Integer.MAX_VALUE : Option.getInt(
                Const.Debug.ClientFileBlockCount, Const.Debug.DefFileBlockCount);
        long clientKeepPeriod = Option.getLong(Const.Debug.ClientFileKeepPeriod,
                Const.Debug.DefFileKeepPeriod);

        CLIENT_CONFIG = new FileTracerConfig(rootPath, clientBlockCount,
                Const.Debug.DefFileBlockSize, Const.Debug.DefDataThreshold,
                Const.Debug.ClientFileTracerName, Const.Debug.DefTimeThreshold,
                FileTracerConfig.PRIORITY_BACKGROUND, Const.Debug.ClientFileExt, clientKeepPeriod);

        int blockCount = Global.isDebug() ? Integer.MAX_VALUE : Option.getInt(
                Const.Debug.FileBlockCount, Const.Debug.DefFileBlockCount);
        long keepPeriod = Option.getLong(Const.Debug.FileKeepPeriod, Const.Debug.DefFileKeepPeriod);

        SERVICE_CONFIG = new FileTracerConfig(rootPath, blockCount, Const.Debug.DefFileBlockSize,
                Const.Debug.DefDataThreshold, Const.Debug.FileTracerName,
                Const.Debug.DefTimeThreshold, FileTracerConfig.PRIORITY_BACKGROUND,
                Const.Debug.FileExt, keepPeriod);
        
        CHANNEL_CONFIG =  new FileTracerConfig(rootPath, blockCount, Const.Debug.DefFileBlockSize,
                Const.Debug.DefDataThreshold, Const.Debug.FileTracerName,
                Const.Debug.DefTimeThreshold, FileTracerConfig.PRIORITY_BACKGROUND,
                Const.Debug.ChannelFileExt, keepPeriod);
        
    }

    protected FileTracer fileTracer;

    protected LogcatTracer logcatTracer;

    private volatile boolean enabled = Const.Debug.Enabled;

    private volatile boolean fileTracerEnabled = Const.Debug.FileTracerEnabled;

    private volatile boolean logcatTracerEnabled = Const.Debug.LogcatTracerEnabled;

    protected MiLinkTracer() {
        // 开始监控配置项变化
        try {
        	Option.startListen(this);
		} catch (Exception e) {

		}
    }

    public static void setInstance(MiLinkTracer inst) {
        MiLinkTracer.sInstance = inst;
    }

    /**
     * 对于不知道自己的进程，或者干脆两个进程都会用到的对象，可以使用这个方法打印LOG
     *
     * @param level 日志级别
     * @param tag 标签
     * @param msg 内容
     * @param tr 可抛对象
     */
    public static void autoTrace(int level, String tag, String msg, Throwable tr) {
        if (sInstance != null) {
            sInstance.trace(level, tag, msg, tr);
        }
    }

    /**
     * 获得日志文件路径<br>
     * 文件路径分两个部分：1，存储在手机或者外部存储器；2，子目录<br>
     * 1，存储在手机或者外部存储器：<br>
     * 则使用外部存储，否则使用手机存储<br>
     * 2，子目录:<br>
     *
     * @return 文件对象
     */
    public static File getLogFilePath() {
        boolean useExternal = false;

        String path = Const.Debug.FileRoot + File.separator
                + Global.getClientAppInfo().getAppName() + File.separator + "logs" + File.separator
                + Global.getPackageName();

        StorageInfo info = StorageDash.getExternalInfo();

        if (info != null) {
            if (info.getAvailableSize() > Const.Debug.MinSpaceRequired) {
                useExternal = true;
            }
        }

        if (useExternal) {
            if (!TextUtils.isEmpty(Global.getClientAppInfo().getLogPath())) {
                return new File(Environment.getExternalStorageDirectory(), Global
                        .getClientAppInfo().getLogPath());
            } else {
                return new File(Environment.getExternalStorageDirectory(), path);
            }
        } else {
            return getLogFileInternalPath();
        }
    }

    public static File getLogFileInternalPath() {
        String path = "logs" + File.separator;
        if (!TextUtils.isEmpty(Global.getClientAppInfo().getLogPath())) {
            return new File(Global.getFilesDir(), Global.getClientAppInfo().getLogPath());
        } else {
            return new File(Global.getFilesDir(), path);
        }
    }

    public void stop() {
        if (fileTracer != null) {
            fileTracer.flush();
            fileTracer.quit();
        }
    }

    public void flush() {
        if (fileTracer != null) {
            fileTracer.flush();
        }
    }

    /**
     * 写日志
     *
     * @param level 级别， {@link TraceLevel}
     * @param tag 标签
     * @param msg 描述
     * @param tr 异常信息
     */
    public void trace(int level, String tag, String msg, Throwable tr) {
        // 检测是否允许Debug
        if (isEnabled()) {
            try {
                // 检测是否允许文件日志追踪
                if (isFileTracerEnabled()) {
                    if (fileTracer != null) {
                        fileTracer.trace(level, Thread.currentThread(), System.currentTimeMillis(),
                                tag, msg, tr);
                    }
                }

                // 检测是否允许Logcat追踪
                if (isLogcatTracerEnabled()) {
                    tag = tag + "(MiLinkSDK)(" + Global.getClientAppInfo().getAppName() + ")";
                    if (logcatTracer != null) {
                        logcatTracer.trace(level, Thread.currentThread(),
                                System.currentTimeMillis(), tag, msg, tr);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 设置是否允许写日志
     *
     * @param enabled true：写日志； false:不写
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置日志级别
     *
     * @param traceLevel 日志级别
     * @see TraceLevel
     */
    public final void setFileTracerLevel(int traceLevel) {
        fileTracer.setTraceLevel(traceLevel);
    }

    /**
     * 设置是否把日志写入文件
     *
     * @param fileTracerEnabled true:写入； false:不写入
     */
    public final void setFileTracerEnabled(boolean fileTracerEnabled) {
        this.fileTracer.flush();

        this.fileTracerEnabled = fileTracerEnabled;
    }

    public final boolean isFileTracerEnabled() {
        return fileTracerEnabled;
    }

    /**
     * 设置是否输出日志到logcat
     *
     * @param logcatTracerEnabled true:输出； false:不输出；
     */
    public final void setLogcatTracerEnabled(boolean logcatTracerEnabled) {
        this.logcatTracerEnabled = logcatTracerEnabled;
    }

    public final boolean isLogcatTracerEnabled() {
        return logcatTracerEnabled;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        if (Const.Debug.FileTraceLevel.equals(key)) {
            int traceLevel = Option.getInt(Const.Debug.FileTraceLevel,
                    Const.Debug.DefFileTraceLevel);
            trace(ERROR, "MnsTracer", "File Trace Level Changed = " + traceLevel, null);

            fileTracer.setTraceLevel(traceLevel);
        } else if (Const.Debug.ClientFileTraceLevel.equals(key)) {
            int traceLevel = Option.getInt(Const.Debug.ClientFileTraceLevel,
                    Const.Debug.DefFileTraceLevel);
            trace(ERROR, "MnsTracer", "Client File Trace Level Changed = " + traceLevel, null);

            fileTracer.setTraceLevel(traceLevel);
        } else if (Const.Debug.LogcatTraceLevel.equals(key)) {
            int traceLevel = Option.getInt(Const.Debug.LogcatTraceLevel,
                    Const.Debug.DefLogcatTraceLevel);
            trace(ERROR, "MnsTracer", "Logcat Trace Level Changed = " + traceLevel, null);

            logcatTracer.setTraceLevel(traceLevel);
        } else if (Const.Debug.ClientLogcatTraceLevel.equals(key)) {
            int traceLevel = Option.getInt(Const.Debug.ClientLogcatTraceLevel,
                    Const.Debug.DefLogcatTraceLevel);
            trace(ERROR, "MnsTracer", "Client Logcat Trace Level Changed = " + traceLevel, null);

            logcatTracer.setTraceLevel(traceLevel);
        }
    }

    /**
     * 提供给ShowLog使用的接口
     *
     * @param pageIndex 对应的Log分片文件的索引，按照更新时间，pageIndex = 0 时取最新的分片
     * @return
     */
    public static BufferedReader getChannelLogReader(int pageIndex) {
        File folder = CHANNEL_CONFIG.getWorkFolder(System.currentTimeMillis());

        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        File[] files = CHANNEL_CONFIG.getAllBlocksInFolder(folder);
        files = CHANNEL_CONFIG.sortBlocksByIndex(files);

        BufferedReader br = null;
        if (pageIndex >= 0 && pageIndex < files.length) {
            int realIndex = files.length - pageIndex - 1;
            File f = files[realIndex];
            FileReader fr = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
            } catch (FileNotFoundException e) {
            }
        }
        return br;
    }

    
    
    /**
     * 提供给ShowLog使用的接口
     *
     * @param pageIndex 对应的Log分片文件的索引，按照更新时间，pageIndex = 0 时取最新的分片
     * @return
     */
    public static BufferedReader getClientLogReader(int pageIndex) {
        File folder = CLIENT_CONFIG.getWorkFolder(System.currentTimeMillis());

        if (folder == null || !folder.isDirectory()) {
            return null;
        }

        File[] files = CLIENT_CONFIG.getAllBlocksInFolder(folder);
        files = CLIENT_CONFIG.sortBlocksByIndex(files);

        BufferedReader br = null;
        if (pageIndex >= 0 && pageIndex < files.length) {
            int realIndex = files.length - pageIndex - 1;
            File f = files[realIndex];
            FileReader fr = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
            } catch (FileNotFoundException e) {
            }
        }
        return br;
    }

    /**
     * 提供给ShowLog使用的接口
     *
     * @param pageIndex 对应的Log分片文件的索引，按照更新时间，pageIndex = 0 时取最新的分片
     * @return
     */
    public static BufferedReader getMnsLogReader(int pageIndex) {
        File folder = SERVICE_CONFIG.getWorkFolder(System.currentTimeMillis());
        File[] files = SERVICE_CONFIG.getAllBlocksInFolder(folder);

        if (files == null) {
            return null;
        }
        files = SERVICE_CONFIG.sortBlocksByIndex(files);

        BufferedReader br = null;
        if (pageIndex >= 0 && pageIndex < files.length) {
            int realIndex = files.length - pageIndex - 1;
            File f = files[realIndex];
            FileReader fr = null;
            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);
            } catch (FileNotFoundException e) {
            }
        }
        return br;
    }

    
    /**
     * 清除当天所有日志文件
     */
    public static void cleanChannelLog() {
        File folder = CHANNEL_CONFIG.getWorkFolder(System.currentTimeMillis());
        File[] files = CHANNEL_CONFIG.getAllBlocksInFolder(folder);

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }
    }
    
    /**
     * 清除当天所有日志文件
     */
    public static void cleanMnsLog() {
        File folder = SERVICE_CONFIG.getWorkFolder(System.currentTimeMillis());
        File[] files = SERVICE_CONFIG.getAllBlocksInFolder(folder);

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }
    }

    /**
     * 清除当天所有日志文件
     */
    public static void cleanClientLog() {
        // setEnabled(false);

        File folder = CLIENT_CONFIG.getWorkFolder(System.currentTimeMillis());
        File[] files = CLIENT_CONFIG.getAllBlocksInFolder(folder);

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }

        // fileTracer.setCurrTraceFile(null); //
        // 这句是为了防止只有一个分片被清空时，旧的trace文件和新的一样，导致无法写入

        // setEnabled(true);
    }

    /**
     * 准备Log上报的文件：将mns log和client log做文件合并
     *
     * @return
     */
    public static File prepareReportLogFile(long time) {
        if (time < 1) {
            time = System.currentTimeMillis();
        }

        FileTracerConfig client_CONFIG = CLIENT_CONFIG;
        FileTracerConfig service_CONFIG = SERVICE_CONFIG;
        FileTracerConfig channel_CONFIG = CHANNEL_CONFIG;
        final int MAX_PAGE_FILE_COUNT_TOTAL = 24;
        File rootPath = getLogFilePath();
        String reportPath = "report.log";

        // ensure report file
        File reportFile = new File(rootPath, reportPath);
        if (reportFile.exists()) {
            reportFile.delete();
        } else {
            try {
                reportFile.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }

        // merge files
        File root = client_CONFIG.getWorkFolder(time);
        File[] totalClientFiles = client_CONFIG.getAllBlocksInFolder(root);

        if (totalClientFiles != null) {
            totalClientFiles = client_CONFIG.sortBlocksByIndex(totalClientFiles);
        }
        File[] totalServiceFiles = service_CONFIG.getAllBlocksInFolder(root);
        if (totalServiceFiles != null) {
            totalServiceFiles = client_CONFIG.sortBlocksByIndex(totalServiceFiles);
        }
        
        File[] totalChannelFiles = channel_CONFIG.getAllBlocksInFolder(root);
        if (totalChannelFiles != null) {
        	totalChannelFiles = client_CONFIG.sortBlocksByIndex(totalChannelFiles);
        }

        float totalClientCount = totalClientFiles != null ? totalClientFiles.length : 0;
        float totalServiceCount = totalServiceFiles != null ? totalServiceFiles.length : 0;
        float totalChannelCount = totalChannelFiles != null ? totalChannelFiles.length : 0;
        if (totalClientCount + totalServiceCount + totalChannelCount <= 0)
            return reportFile;

        float total = totalClientCount + totalServiceCount + totalChannelCount;
        int selectedClientCount = Math
                .round((totalClientCount / total) * MAX_PAGE_FILE_COUNT_TOTAL);
        int selectedServiceCount = Math.round((totalServiceCount / total)
                * MAX_PAGE_FILE_COUNT_TOTAL);
        int selectedChannelCount = Math
                .round((totalChannelCount / total) * MAX_PAGE_FILE_COUNT_TOTAL);
		// 增加保护：至少写入一个分片
		if (selectedClientCount == 0 && (totalClientFiles != null && totalClientFiles.length > 0)) {
			selectedClientCount = 1;
			selectedServiceCount--;
			if (selectedChannelCount == 0) {
				if (selectedServiceCount > 1) {
					selectedServiceCount--;
				}
				selectedChannelCount = 1;
			}

		} else if (selectedServiceCount == 0 && (totalServiceFiles != null && totalServiceFiles.length > 0)) {
			selectedServiceCount = 1;
			selectedClientCount--;

			if (selectedChannelCount == 0) {
				if (selectedClientCount > 1) {
					selectedClientCount--;
				}
				selectedChannelCount = 1;
			}
		}
        
        
        List<File> srcClientFiles = new ArrayList<File>();
        List<File> srcServiceFiles = new ArrayList<File>();
        List<File> srcChannelFiles = new ArrayList<File>();
        if (totalClientFiles != null) {
            while (selectedClientCount > 0) {
                selectedClientCount--;
                if (srcClientFiles.size() < totalClientFiles.length) {
                    File srcClient = totalClientFiles[totalClientFiles.length
                            - srcClientFiles.size() - 1];
                    srcClientFiles.add(0, srcClient);
                }
            }
        }
        if (totalServiceFiles != null) {
            while (selectedServiceCount > 0) {
                selectedServiceCount--;
                if (srcServiceFiles.size() < totalServiceFiles.length) {
                    File srcService = totalServiceFiles[totalServiceFiles.length
                            - srcServiceFiles.size() - 1];
                    srcServiceFiles.add(0, srcService);
                }
            }
        }
        if (totalChannelFiles != null) {
            while (selectedChannelCount > 0) {
                selectedChannelCount--;
                if (srcChannelFiles.size() < totalChannelFiles.length) {
                    File srcChannel = totalChannelFiles[totalChannelFiles.length
                            - srcChannelFiles.size() - 1];
                    srcChannelFiles.add(0, srcChannel);
                }
            }
        }
        
        

        // write file
        mergeFiles(srcClientFiles, reportFile,
                "------client log block count:" + srcClientFiles.size() + "------\n");
        mergeFiles(srcServiceFiles, reportFile,
                "\n------mns log block count:" + srcServiceFiles.size() + "------\n");
        mergeFiles(srcChannelFiles, reportFile,
                "------client log block count:" + srcChannelFiles.size() + "------\n");
        
        return reportFile;
    }

    /**
     * 准备Log上报的文件：将mns log和client log做文件合并<br>
     * 首先获取time当天的，如果大小小于size,则以天为单位向前继续取日志，直到不小于size
     *
     * @param time
     * @param size
     * @return
     */
    public static File prepareReportLogFile(long time, int size) {
        if (size < 0) {
            return prepareReportLogFile(time);
        }

        if (time < 1) {
            time = System.currentTimeMillis();
        }

        FileTracerConfig client_CONFIG = CLIENT_CONFIG;
        FileTracerConfig service_CONFIG = SERVICE_CONFIG;
        FileTracerConfig channel_CONFIG = CHANNEL_CONFIG;
        final int MAX_PAGE_FILE_COUNT_TOTAL = 24;
        File rootPath = getLogFilePath();
        String reportPath = "report.log";

        // ensure report file
        File reportFile = new File(rootPath, reportPath);
        if (reportFile.exists()) {
            reportFile.delete();
        } else {
            try {
                reportFile.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }

        int haved = 0;
        List<File> srcClientFiles = new ArrayList<File>();
        List<File> srcServiceFiles = new ArrayList<File>();
        List<File> srcChannelFiles = new ArrayList<File>();

        List<File> tempClientFiles = new ArrayList<File>();
        List<File> tempServiceFiles = new ArrayList<File>();
        List<File> tempChannelFiles = new ArrayList<File>();
        int counter = 0;// protect
        while (haved < size && counter++ < 7) {
            // 退出条件
            if (!client_CONFIG.isWorkFolderExists(time) && !service_CONFIG.isWorkFolderExists(time)) {
                break;
            }
            tempClientFiles.clear();
            tempServiceFiles.clear();
            tempChannelFiles.clear();
            // merge files
            File root = client_CONFIG.getWorkFolder(time);
            File[] totalClientFiles = client_CONFIG.getAllBlocksInFolder(root);

            if (totalClientFiles != null) {
                totalClientFiles = client_CONFIG.sortBlocksByIndex(totalClientFiles);
            }
            File[] totalServiceFiles = service_CONFIG.getAllBlocksInFolder(root);
            if (totalServiceFiles != null) {
                totalServiceFiles = service_CONFIG.sortBlocksByIndex(totalServiceFiles);
            }
            
            File[] totalChannelFiles = channel_CONFIG.getAllBlocksInFolder(root);
            if (totalChannelFiles != null) {
            	totalChannelFiles = channel_CONFIG.sortBlocksByIndex(totalServiceFiles);
            }
            

            float totalClientCount = totalClientFiles != null ? totalClientFiles.length : 0;
            float totalServiceCount = totalServiceFiles != null ? totalServiceFiles.length : 0;
            float totalChannelCount = totalChannelFiles != null ? totalChannelFiles.length : 0;
            if (totalClientCount + totalServiceCount + totalChannelCount <= 0)
                continue;

            float total = totalClientCount + totalServiceCount + totalChannelCount;
            int selectedClientCount = Math.round((totalClientCount / total)
                    * MAX_PAGE_FILE_COUNT_TOTAL);
            int selectedServiceCount = Math.round((totalServiceCount / total)
                    * MAX_PAGE_FILE_COUNT_TOTAL);
            int selectedChannelCount = Math.round((totalChannelCount / total)
                    * MAX_PAGE_FILE_COUNT_TOTAL);
			// 增加保护：至少写入一个分片
			if (selectedClientCount == 0 && (totalClientFiles != null && totalClientFiles.length > 0)) {
				selectedClientCount = 1;
				selectedServiceCount--;
				if (selectedChannelCount == 0) {
					if (selectedServiceCount > 1) {
						selectedServiceCount--;
					}
					selectedChannelCount = 1;
				}

			} else if (selectedServiceCount == 0 && (totalServiceFiles != null && totalServiceFiles.length > 0)) {
				selectedServiceCount = 1;
				selectedClientCount--;

				if (selectedChannelCount == 0) {
					if (selectedClientCount > 1) {
						selectedClientCount--;
					}
					selectedChannelCount = 1;
				}
			}
            
            if (totalClientFiles != null) {
                while (selectedClientCount > 0) {
                    selectedClientCount--;
                    if (tempClientFiles.size() < totalClientFiles.length) {
                        File srcClient = totalClientFiles[totalClientFiles.length
                                - tempClientFiles.size() - 1];
                        tempClientFiles.add(0, srcClient);
                        haved += srcClient.length();
                    }
                }
            }
            if (totalServiceFiles != null) {
                while (selectedServiceCount > 0) {
                    selectedServiceCount--;
                    if (tempServiceFiles.size() < totalServiceFiles.length) {
                        File srcService = totalServiceFiles[totalServiceFiles.length
                                - tempServiceFiles.size() - 1];
                        tempServiceFiles.add(0, srcService);
                        haved += srcService.length();
                    }
                }
            }
            if (totalChannelFiles != null) {
                while (selectedChannelCount > 0) {
                    selectedChannelCount--;
                    if (tempChannelFiles.size() < totalChannelFiles.length) {
                        File srcChannel = totalChannelFiles[totalChannelFiles.length
                                - tempChannelFiles.size() - 1];
                        tempChannelFiles.add(0, srcChannel);
                        haved += srcChannel.length();
                    }
                }
            }

            time -= 24 * 60 * 60 * 1000L;
            srcClientFiles.addAll(tempClientFiles);
            srcServiceFiles.addAll(tempServiceFiles);
            srcChannelFiles.addAll(tempChannelFiles);
        }

        if (srcClientFiles.size() == 0 && srcServiceFiles.size() == 0  && srcChannelFiles.size() == 0) {
            return null;
        }

        // write file
        mergeFiles(srcClientFiles, reportFile,
                "------client log block count:" + srcClientFiles.size() + "------\n");
        mergeFiles(srcServiceFiles, reportFile,
                "\n------mns log block count:" + srcServiceFiles.size() + "------\n");
        mergeFiles(srcChannelFiles, reportFile,
                "\n------mns log block count:" + srcChannelFiles.size() + "------\n");

        return reportFile;
    }

    /**
     * 文件合并
     *
     * @param srcFiles 需要合并的源文件
     * @param destFile 写入的目标文件
     * @param tag 用于分割的字符串，会写在最前面
     * @return
     */
    private static boolean mergeFiles(List<File> srcFiles, File destFile, String tag) {
        if (srcFiles == null || srcFiles.size() < 1 || destFile == null)
            return false;

        FileOutputStream fout = null;
        boolean result = true;
        try {
            fout = new FileOutputStream(destFile, true);
            if (!TextUtils.isEmpty(tag))
                fout.write(tag.getBytes("UTF-8"));

            byte[] buffer = new byte[4096];
            for (int i = 0; i < srcFiles.size(); i++) {
                FileInputStream fin = new FileInputStream(srcFiles.get(i));
                int count = 0;
                while ((count = fin.read(buffer, 0, buffer.length)) > 0) {
                    fout.write(buffer, 0, count);
                }
                fin.close();
            }
        } catch (FileNotFoundException e) {
            result = false;
        } catch (UnsupportedEncodingException e) {
            result = false;
        } catch (IOException e) {
            result = false;
        } finally {
            result = CommonUtils.closeDataObject(fout);
        }
        return result;
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else {
            for (File f : file.listFiles()) {
                deleteFile(f);
            }
        }
    }
}
