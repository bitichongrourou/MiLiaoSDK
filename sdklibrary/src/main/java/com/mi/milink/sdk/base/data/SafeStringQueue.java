
package com.mi.milink.sdk.base.data;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 字符串安全队列 <br>
 * <br>
 * <br>
 * 使用{@code ConcurrentLinkedQueue}实现的安全队列<br>
 * 利用字符缓存写的方法减轻Writer在I/O操作时对内存的压力<br>
 * <br>
 * p.s 本类与{@code BufferedWriter}的缓冲机制是不同的: <br>
 * <p>
 * {@code BufferedWriter} 是使用内存缓冲降低I/O操作的频繁度<br>
 * 本类是利用字符缓冲降低写入字符串时对内存的压力
 * </p>
 * 
 * @author MK
 */
public class SafeStringQueue implements Iterable<String> {
    private static final String TAG = SafeStringQueue.class.getSimpleName();

    private ConcurrentLinkedQueue<String> bufferQueue = null; // 安全队列

    private AtomicInteger bufferSize = null; // 缓冲队列的字符数记录，用于提升计算缓冲区字符串总字符数的速度

    public SafeStringQueue() {
        bufferQueue = new ConcurrentLinkedQueue<String>();
        bufferSize = new AtomicInteger(0);
    }

    /**
     * 添加字符串到缓冲区
     * 
     * @param str 字符串
     * @return 添加后缓冲字符串的总长度，单位: 字符
     */
    public int addToBuffer(String str) {
        int dataLen = str.length();

        bufferQueue.add(str);

        return bufferSize.addAndGet(dataLen);
    }

    /**
     * 将全部字符串缓冲写入Writer，需要提供一个字符缓冲区
     * 
     * @param writer 写对象
     * @param buffer 字符写缓冲，用于降低I/O操作和内存占用<br>
     *            <i>建议持有本实例的对象准备一个char[]作为buffer长期使用</i>
     * @throws IOException Writer对象可能抛出的I/O异常
     */
    public void writeAndFlush(Writer writer, char[] buffer) throws IOException {
        // 参数检查
        if (writer == null || buffer == null || buffer.length == 0) {
            return;
        }

        int strRestLen = 0; // 字符串未写入的长度
        int strLen = 0; // 字符串总长度
        int strPos = 0; // 字符串当前写入位置

        int writeLen = 0; // 即将写入的长度

        int bufferLen = buffer.length; // 缓冲区的长度
        int bufferRestLen = bufferLen; // 缓冲区空闲的长度
        int bufferPos = 0; // 缓冲区当前的空闲位置

        /* 为Writer的CharsetEncode增加TC保护，防止IllegalStateException */
        try {
            for (String str : this) {
                // 初始化字符串的写入位置信息
                strPos = 0;
                strLen = str.length();
                strRestLen = strLen;

                // 如果该字符串还有没写完
                while (strRestLen > 0) {
                    // 计算该写入多长：取 缓冲区空闲大小 和 字符串剩余大小 的最小值
                    writeLen = (bufferRestLen > strRestLen) ? strRestLen : bufferRestLen;
                    // 从字符串中读取writeLen这么长的字符，添入缓冲区
                    str.getChars(strPos, strPos + writeLen, buffer, bufferPos);
                    // 标记缓冲区的写入情况
                    bufferRestLen -= writeLen;
                    bufferPos += writeLen;
                    // 标记字符串的写入情况
                    strRestLen -= writeLen;
                    strPos += writeLen;
                    // 如果缓冲区已经写满，则调用写对象写入
                    if (bufferRestLen == 0) {
                        writer.write(buffer, 0, bufferLen);
                        // 写完后，重置缓冲区
                        bufferPos = 0;
                        bufferRestLen = bufferLen;
                    }
                }
                // 字符串全部写完，继续写下一个字符串
            }

            // 全部字符串都写完，将缓冲区剩余的全部写入写对象
            if (bufferPos > 0) {
                writer.write(buffer, 0, bufferPos);
            }

            // 要求写对象冲刷自缓存，保证输出
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得缓冲的字符串的字符总长度
     * 
     * @return 单位: 字符
     */
    public int getBufferSize() {
        return bufferSize.get();
    }

    /**
     * 清空缓冲的全部字符串
     */
    public void clear() {
        bufferQueue.clear();
        // FIXME 没有同步就设置bufferSize，会不会引起addToBuffer时不准确的情况？
        bufferSize.set(0);
    }

    /**
     * 迭代器支持
     */
    @Override
    public Iterator<String> iterator() {
        return bufferQueue.iterator();
    }
}
