
package com.mi.milink.sdk.base.debug;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;

import android.os.Process;

import com.mi.milink.sdk.util.CommonUtils;
import com.mi.milink.sdk.util.FileUtils;

/**
 * 文件日志追踪器配置类<br>
 * <p>
 * 通过提供{@link FileTracerConfig}对象，可以为{@link FileTracer}实例设置如下选项：<br>
 * <br>
 * 
 * <pre>
 * 工作路径: 设置日志保存的路径，日志文件根据日期保存在该路径的/yyyy-MM-dd/文件夹下，以"1.xxx", "2.xxx"命名<br>
 * 日志文件扩展名: 用于方便将不同文件日志追踪器实例生成的日志文件保存在同一个目录下，格式为".xxx"<br>
 * 线程名称: I/O操作的线程名称<br>
 * 线程优先级: I/O操作线程的优先级<br>
 * I/O操作时间阈值: 每隔多长时间进行一次I/O操作<br>
 * I/O操作数据量阈值: 每当数据量超过多少时，进行一次I/O操作<br>
 * 日志文件分片数量: 每天的日志文件最多有多少个分片，超过该值将自动删除最旧的分片<br>
 * 日志文件分片大小: 每个日志分片最大的尺寸，超过该值后将创建并使用新的分片<br>
 * 日志保存期限: 最长保存日志的时间，若超过该期限，则在{@code FileTracerConfig.cleanWorkRoot()}删除它们<br>
 * </pre>
 * 
 * </p>
 * 
 * @author MK
 */
public class FileTracerConfig {
    /**
     * 日志分片数量，分片大小：不限制数量
     * 
     * @see setMaxBlockSize(), setMaxBlockCount()
     */
    public static final int NO_LIMITED = Integer.MAX_VALUE;

    /**
     * 日志保存期限：永久 <br>
     * <br>
     * <i>永远有多远？大概 2^63 - 1 ms 那么远</i>
     */
    public static final long FOREVER = Long.MAX_VALUE;

    /**
     * I/O操作线程优先级：后台线程，即{@code android.os.Process.THREAD_PRIORITY_BACKGROUND}
     */
    public static final int PRIORITY_BACKGROUND = Process.THREAD_PRIORITY_BACKGROUND;

    /**
     * I/O操作线程优先级：默认，即{@code android.os.Process.THREAD_PRIORITY_DEFAULT}
     */
    public static final int PRIORITY_STANDARD = Process.THREAD_PRIORITY_DEFAULT;

    /**
     * 默认I/O操作字符缓冲大小
     */
    public static final int DEF_BUFFER_SIZE = 8 * 1024;

    /**
     * 默认日志文件扩展名
     */
    public static final String DEF_TRACE_FILEEXT = ".log";

    /**
     * 默认日志文件夹日期格式
     */
    public static final String DEF_FOLDER_FORMAT = "yyyy-MM-dd";

    /**
     * 默认I/O操作线程名称
     */
    public static final String DEF_THREAD_NAME = "Tracer.File";

    /**
     * 默认I/O操作时间阈值
     */
    public static final long DEF_FLUSH_INTERVAL = 10 * 1000L;

    private static final FileFilter DEF_TRACE_FOLDER_FILTER = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            if (!pathname.isDirectory()) {
                return false;
            }

            return getTimeFromFolder(pathname) > 0;
        }
    };

    public static long getTimeFromFolder(File folder) {
        try {
            SimpleDateFormat formatter = CommonUtils.createDataFormat(DEF_FOLDER_FORMAT);
            return formatter.parse(folder.getName()).getTime();
        } catch (Exception e) {
            return -1L;
        }
    }

    private String mThreadName = DEF_THREAD_NAME;

    private int mMaxBlockSize = NO_LIMITED;

    private int mMaxBlockCount = NO_LIMITED;

    private int mMaxBufferSize = DEF_BUFFER_SIZE;

    private long mFlushInterval = DEF_FLUSH_INTERVAL;

    private File mRootFolder;

    private int mThreadPriority = PRIORITY_BACKGROUND;

    private String mFileExt = DEF_TRACE_FILEEXT;

    private long mKeepPeriod = FOREVER;

    private FileFilter mLogFileFilter = new FileFilter() {// 扫描日志分片文件的过滤器

        @Override
        public boolean accept(File pathname) {
            String fileName = pathname.getName();

            // 检查扩展名是否符合配置
            boolean conditionA = fileName.endsWith(getFileExt());

            if (!conditionA) {
                return false;
            }

            // 检查文件名是否为序号
            boolean conditionB = getBlockCountFromFile(pathname) != -1;

            return conditionB;
        }
    };

    private Comparator<? super File> mBlockComparetor = new Comparator<File>() {// 分片文件排序器

        @Override
        public int compare(File lhs, File rhs) {
            return getBlockCountFromFile(lhs) - getBlockCountFromFile(rhs);
        }
    };

    /**
     * 创建一个默认的文件日志追踪器配置: 不进行分片，线程名称"Tracer.File"，后台级现成，时间阈值
     * 10,000ms，数据量阈值4096个字符，日志文件扩展名为".log"，日志文件永久保存
     * 
     * @param root 工作路径
     */
    public FileTracerConfig(File root) {
        this(root, NO_LIMITED, NO_LIMITED, DEF_BUFFER_SIZE, DEF_THREAD_NAME, DEF_FLUSH_INTERVAL,
                PRIORITY_BACKGROUND, DEF_TRACE_FILEEXT, FOREVER);
    }

    /**
     * 创建一个文件日志追踪器配置
     * 
     * @param root 工作路径
     * @param blockCount 分片最大数量，单位: 个
     * @param blockSize 分片最大尺寸，单位: byte
     * @param bufferSize I/O操作的数据量阈值，单位: 字符
     * @param threadName I/O操作线程名称
     * @param interval I/O操作的时间阈值
     * @param priority I/O操作线程优先级
     * @param fileExt 日志文件扩展名，形如".xxx"
     * @param keepPeriod 日志文件保存期限
     */
    public FileTracerConfig(File root, int blockCount, int blockSize, int bufferSize,
            String threadName, long interval, int priority, String fileExt, long keepPeriod) {
        setRootFolder(root);
        setMaxBlockCount(blockCount);
        setMaxBlockSize(blockSize);
        setMaxBufferSize(bufferSize);
        setThreadName(threadName);
        setFlushInterval(interval);
        setThreadPriority(priority);
        setFileExt(fileExt);
        setKeepPeriod(keepPeriod);
    }

    /**
     * 获得当前应该输出的日志文件
     * 
     * @return 日志文件路径
     */
    public File getCurrFile() {
        return getWorkFile(System.currentTimeMillis());
    }

    /**
     * 获得指定时间的最新日志分片，并检查分片状况
     * 
     * @param time 时间
     * @return 该时间的最新日志分片
     */
    private File getWorkFile(long time) {
        return ensureBlockCount(getWorkFolder(time));
    }

    /**
     * 获得指定时间的日志目录，如果不存在则创建该目录
     * 
     * @param time 时间
     * @return 指定时间的日志目录
     */
    public File getWorkFolder(long time) {
        File workFolder = getWorkFolderPath(time);
        if (!workFolder.exists()) {
            workFolder.mkdirs();
        }
        return workFolder;
    }

    public boolean isWorkFolderExists(long time) {
        return getWorkFolderPath(time).exists();
    }

    private File getWorkFolderPath(long time) {
        return new File(getRootFolder(), CommonUtils.createDataFormat(DEF_FOLDER_FORMAT).format(
                time));
    }

    /**
     * 得到当前应该使用的分片文件<br>
     * <br>
     * 这将检查分片数量和最近分片的分片大小。如果超过最大分片数，则删除旧分片直到满足要求；如果最新的分片超过分片大小限制，则创建新的分片
     * 
     * @param folder 分片存放的文件夹
     * @return 当前应该使用的分片文件
     */
    private File ensureBlockCount(File folder) {
        // 枚举所有分片
        File[] files = getAllBlocksInFolder(folder);

        // 如果当前没有分片，则返回新分片
        if (files == null || files.length == 0) {
            return new File(folder, "1" + getFileExt());
        }
        // 对所有旧分片按照索引排序
        sortBlocksByIndex(files);
        // 取得其中最新的分片
        File resu = files[files.length - 1];
        // 计算需要清理的旧分片数量
        int cleanCount = files.length - getMaxBlockCount();
        // 检查最新分片是否超过了分片大小限制
        if ((int) resu.length() > getMaxBlockSize()) {
            // 创建并使用新的分片
            int newIndex = getBlockCountFromFile(resu) + 1;
            resu = new File(folder, newIndex + getFileExt());
            // 因为创建了新的分片，所以需要删除的旧分片又多了一个
            cleanCount += 1;
        }

        // 删除所有旧分片
        for (int i = 0; i < cleanCount; i++) {
            files[i].delete();
        }

        // 返回最新的分片
        return resu;
    }

    /**
     * 枚举日志目录下的所有分片
     * 
     * @param folder 日志目录
     * @return 分片文件数组
     */
    public File[] getAllBlocksInFolder(File folder) {
        return folder.listFiles(mLogFileFilter);
    }

    /**
     * 清理过期的日志文件夹
     */
    public void cleanWorkFolders() {
        if (getRootFolder() == null) {
            return;
        }

        File[] folders = getRootFolder().listFiles(DEF_TRACE_FOLDER_FILTER);

        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            long time = getTimeFromFolder(folder);

            if (System.currentTimeMillis() - time > getKeepPeriod()) {
                FileUtils.deleteFile(folder);
            }
        }
    }

    /**
     * 获得所有分片的大小和，即某日的日志数据总量
     * 
     * @param folder 日志目录
     * @return 分片文件总大小，单位: byte
     */
    public long getSizeOfBlocks(File folder) {
        // 确保一次分片符合规则
        ensureBlockCount(folder);

        File[] blockFiles = getAllBlocksInFolder(folder);

        return getSizeOfBlocks(blockFiles);
    }

    /**
     * 获得所有分片的大小和，即某日的日志数据总量
     * 
     * @param blockFiles 分片文件数组
     * @return 分片文件总大小，单位: byte
     */
    public long getSizeOfBlocks(File[] blockFiles) {
        long size = 0;
        for (File file : blockFiles) {
            if (file.exists() && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    /**
     * 按照从旧到新的索引顺序排列分片
     * 
     * @param blockFiles 分片文件数组
     * @return 排序后的文件数组
     */
    public File[] sortBlocksByIndex(File[] blockFiles) {
        Arrays.sort(blockFiles, mBlockComparetor);
        return blockFiles;
    }

    /**
     * 获得文件的分片索引
     * 
     * @param file 文件
     * @return 分片索引，如果不是分片文件，则返回-1
     */
    private static int getBlockCountFromFile(File file) {
        try {
            String fileName = file.getName();
            int p = fileName.indexOf('.');
            fileName = fileName.substring(0, p);
            return Integer.parseInt(fileName);
        } catch (Exception e) {
            // 文件为空、文件名不合法
            return -1;
        }
    }

    /**
     * 获取I/O操作的线程名称
     * 
     * @return 线程名称
     */
    public String getThreadName() {
        return mThreadName;
    }

    /**
     * 设置I/O操作的线程名称
     * 
     * @param name 线程名称
     */
    public void setThreadName(String name) {
        this.mThreadName = name;
    }

    /**
     * 获取日志文件分片最大尺寸
     * 
     * @return 单位: byte
     */
    public int getMaxBlockSize() {
        return mMaxBlockSize;
    }

    /**
     * 设置日志文件分片最大尺寸
     * 
     * @param maxBlockSize 单位: byte
     */
    public void setMaxBlockSize(int maxBlockSize) {
        this.mMaxBlockSize = maxBlockSize;
    }

    /**
     * 获取每天日志文件分片的最大数量
     * 
     * @return 单位: 个
     */
    public int getMaxBlockCount() {
        return mMaxBlockCount;
    }

    /**
     * 设置每天日志文件分片的最大数量
     * 
     * @param maxBlockCount 单位: 个
     */
    public void setMaxBlockCount(int maxBlockCount) {
        this.mMaxBlockCount = maxBlockCount;
    }

    public int getMaxBufferSize() {
        return mMaxBufferSize;
    }

    /**
     * 设置I/O操作的数据量阈值
     * 
     * @param maxBufferSize 单位: 字符
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.mMaxBufferSize = maxBufferSize;
    }

    /**
     * I/O操作的数据量阈值
     * 
     * @return 单位: ms
     */
    public long getFlushInterval() {
        return mFlushInterval;
    }

    /**
     * 设置I/O操作的时间阈值
     * 
     * @param flushInterval 单位: ms
     */
    public void setFlushInterval(long flushInterval) {
        this.mFlushInterval = flushInterval;
    }

    /**
     * 获取工作目录
     * 
     * @return
     */
    public File getRootFolder() {
        return mRootFolder;
    }

    /**
     * 设置工作目录
     * 
     * @param rootFolder 工作目录
     */
    public void setRootFolder(File rootFolder) {
        this.mRootFolder = rootFolder;
    }

    /**
     * 获取I/O操作线程的优先级
     * 
     * @return
     */
    public int getThreadPriority() {
        return mThreadPriority;
    }

    /**
     * 设置I/O操作线程的优先级
     * 
     * @param priority 优先级
     */
    public void setThreadPriority(int priority) {
        this.mThreadPriority = priority;
    }

    /**
     * 获取日志文件的扩展名
     * 
     * @return
     */
    public String getFileExt() {
        return mFileExt;
    }

    /**
     * 设置日志文件的扩展名
     * 
     * @param fileExt 形如 ".xxx"
     */
    public void setFileExt(String fileExt) {
        this.mFileExt = fileExt;
    }

    /**
     * 获得日志文件保存期限
     * 
     * @return 保存期限
     */
    public long getKeepPeriod() {
        return mKeepPeriod;
    }

    /**
     * 设置日志文件保存期限
     * 
     * @param keepPeriod
     */
    public void setKeepPeriod(long keepPeriod) {
        this.mKeepPeriod = keepPeriod;
    }
}
