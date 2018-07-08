
package com.mi.milink.sdk.base.debug;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mi.milink.sdk.util.CommonUtils;
import com.mi.milink.sdk.util.FileUtils;

/**
 * 日志文件读取类
 * 
 * @author MK
 */
public class FileTracerReader {

    /**
     * 默认缓冲区长度
     */
    private static final int DEF_BUFFER_SIZE = 8 * 1024;

    private static final String TAG = FileTracerReader.class.getSimpleName();

    private FileTracerConfig mConfig;

    /**
     * 创建一个日志文件读取器，依赖于指定的文件日志追踪器配置
     * 
     * @param config 文件日志追踪器配置
     */
    public FileTracerReader(FileTracerConfig config) {
        setConfig(config);
    }

    /**
     * 创建一个日志文件读取器，依赖于指定的文件日志追踪器的配置
     * 
     * @param fileTracer 文件日志追踪器
     */
    public FileTracerReader(FileTracer fileTracer) {
        this(fileTracer.getConfig());
    }

    /**
     * 打包某一天的日志到指定文件夹，使用ZIP压缩
     * 
     * @param time 日志的时间
     * @param tempFolder 存放文件夹
     * @return 成功则返回打包文件的路径 <br>
     *         打包或压缩失败则返回null
     */
    public File pack(long time, File tempFolder) {
        // 默认使用ZIP压缩
        return pack(time, tempFolder, true);
    }

    /**
     * 打包某一天的日志到指定文件夹，可以指定ZIP压缩
     * 
     * @param time 日志的时间
     * @param tempFolder 存放文件夹
     * @param needZip 是否需要ZIP压缩<br>
     * <br>
     *            <i>如果使用ZIP压缩，则扩展名为".zip"，否则为依赖的文件日志追踪器配置对象的日志文件扩展名
     *            {@code getConfig().getFileExt()} </i>
     * @return 成功则返回打包文件的路径 <br>
     *         打包或压缩失败则返回null
     */
    public File pack(long time, File tempFolder, boolean needZip) {
        // 打包生成源文件
        File resu = doPack(time, tempFolder);

        // 生成失败，则返回null
        if (resu == null) {
            return null;
        }

        if (needZip) {
            // 需要ZIP压缩
            File dest = new File(resu.getAbsolutePath() + FileUtils.ZIP_FILE_EXT);

            boolean isZipped = FileUtils.zip(resu, dest);
            // 压缩成功，返回压缩后路径；压缩失败，返回null
            return isZipped ? dest : null;
        } else {
            return resu;
        }
    }

    /**
     * 打包某一天的日志到指定文件夹
     * 
     * @param time 日志的时间
     * @param tempFolder 存放文件夹
     * @return 成功则返回打包文件的路径 <br>
     *         失败则返回null
     */
    private File doPack(long time, File tempFolder) {
        File resu = null;
        // 获得该时间所属的日志文件夹
        File workFolder = getConfig().getWorkFolder(time);
        // 获得全部碎片
        File[] blockFiles = getConfig().getAllBlocksInFolder(workFolder);

        // 创建打包文件，如果已经存在，则删除准备覆盖
        File tempFile = new File(tempFolder, workFolder.getName() + getConfig().getFileExt());

        if (tempFile.exists()) {
            tempFile.delete();
        }

        // 没有碎片文件，那么直接返回一个空文件
        if (blockFiles == null) {
            try {
                tempFile.createNewFile();
            } catch (IOException e) {

            }

            return tempFile;
        }

        // 碎片按照索引排序
        getConfig().sortBlocksByIndex(blockFiles);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        int readLen = 0;
        // 创建缓冲区
        byte[] buffer = new byte[DEF_BUFFER_SIZE];

        try {
            bos = new BufferedOutputStream(new FileOutputStream(tempFile, true));

            for (File file : blockFiles) {
                // 关闭上一个碎片的流
                CommonUtils.closeDataObject(bis);
                // 开始新碎片
                bis = new BufferedInputStream(new FileInputStream(file));

                while (0 < (readLen = bis.read(buffer, 0, buffer.length))) {
                    // 写入打包文件
                    bos.write(buffer, 0, readLen);
                }
            }

            bos.flush();

            resu = tempFile;
        } catch (IOException e) {
            // 不能用MilinkLog.e防止死循环
            e.printStackTrace();
            resu = null;
        } finally {
            // 关闭用过的流对象
            CommonUtils.closeDataObject(bos);
            CommonUtils.closeDataObject(bis);
        }

        return resu;
    }

    /**
     * 读取某一天的日志<br>
     * * <i>已经废弃。不建议直接操作日志碎片，若要导出日志，直接使用{@code FileTracerReader.pack()}方法</i>
     * 
     * @param time 日志时间
     * @param buffer 缓冲区。如不指定，则创建一个默认大小的缓冲区
     * @param startFileIndex 开始的碎片索引
     * @param startDataIndex 开始的数据偏移，从startFileIndex开始计算
     * @param eachTimeReadLen 每次读取的长度。若指定范围超过缓冲区长度，则使用缓冲区长度
     * @param callback 每次读取后的回调
     * @return
     */
    @Deprecated
    public boolean read(long time, byte[] buffer, int startFileIndex, int startDataIndex,
            int eachTimeReadLen, ReaderCallback callback) {
        // 没有回调，操作无意义
        if (callback == null) {
            return false;
        }

        // 没有缓冲区，则创建一个默认的缓冲区
        if (buffer == null) {
            buffer = new byte[DEF_BUFFER_SIZE];
        }

        // 每次读取的长度超过了缓冲区，则校正
        if (eachTimeReadLen > buffer.length) {
            eachTimeReadLen = buffer.length;
        }

        boolean resu = false;

        // 获得该时间所属的日志文件夹
        File workFolder = getConfig().getWorkFolder(time);
        // 获得全部碎片
        File[] blockFiles = getConfig().getAllBlocksInFolder(workFolder);

        if (blockFiles == null) {
            return false;
        }
        // 碎片按照索引排序
        getConfig().sortBlocksByIndex(blockFiles);

        BufferedInputStream bis = null;

        int readLen = 0;
        // 要跳过的数据长度
        int readSkip = startDataIndex;

        try {
            for (int i = startFileIndex; i < blockFiles.length; i++) {
                File file = blockFiles[i];

                // 需要跳过的数据已经跳过了当前碎片
                if (readSkip > file.length()) {
                    readSkip -= (int) file.length();
                    // 跳过该碎片
                    continue;
                }

                CommonUtils.closeDataObject(bis);

                bis = new BufferedInputStream(new FileInputStream(file));

                // 需要跳过数据
                if (readSkip > 0) {
                    bis.skip(readSkip);
                    readSkip = 0;
                }
                // 开始读取
                while (0 < (readLen = bis.read(buffer, 0, buffer.length))) {
                    // 通知调用者
                    callback.onTraceRead(this, buffer, readLen);

                }

                // 成功读取
                resu = true;
            }
        } catch (IOException e) {
            // 不能用MilinkLog.e防止死循环
            e.printStackTrace();
            resu = false;
        } finally {
            // 关闭用过的流对象
            CommonUtils.closeDataObject(bis);
        }

        return resu;
    }

    /**
     * 获得所依赖的文件日志追踪器配置
     * 
     * @return 文件日志追踪器配置
     */
    public FileTracerConfig getConfig() {
        return mConfig;
    }

    /**
     * 设置文件日志追踪器配置
     * 
     * @param config 文件日志追踪器配置
     */
    public void setConfig(FileTracerConfig config) {
        this.mConfig = config;
    }

    /**
     * 日志文件读取回调 <br>
     * <br>
     * <i>已经废弃。不建议直接操作日志碎片，若要导出日志，直接使用{@code FileTracerReader.pack()}方法</i>
     * 
     * @author lewistian
     */
    @Deprecated
    public interface ReaderCallback {
        /**
         * 当读取到日志文件的数据时，会回调该接口
         * 
         * @see FileTracerReader.read()
         * @param sender 回调该接口的FileTracerReader对象
         * @param buffer 数据的缓冲区
         * @param readLen 有效数据长度
         */
        public void onTraceRead(FileTracerReader sender, byte[] buffer, int readLen);
    }
}
