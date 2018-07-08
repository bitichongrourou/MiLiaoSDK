
package com.mi.milink.sdk.base.debug;

/**
 * 日志追踪器基础类
 *
 * @author MK
 */
public abstract class Tracer {
    private volatile int mTraceLevel = TraceLevel.ALL;

    private volatile boolean mEnabled = true;

    private TraceFormat mTraceFormat = TraceFormat.DEFAULT;

    /**
     * 构造方法：创建一个日志追踪器，关心级别为{@code TraceLevel.ALL}，初始时启用，并使用默认的日志格式
     */
    public Tracer() {
        this(TraceLevel.ALL, true, TraceFormat.DEFAULT);
    }

    /**
     * @param level 关心的日志级别 参考{@link TraceLevel}
     * @param enable 初始时启用/禁用日志追踪
     * @param format 日志格式 参考{@link TraceFormat}
     */
    public Tracer(int level, boolean enable, TraceFormat format) {
        setTraceLevel(level);
        setEnabled(enable);
        setTraceFormat(format);
    }

    /**
     * 追踪日志<br>
     * <br>
     * 当追踪器被禁用，或该日志的级别不是追踪器关心的级别时，则不会发生任何事情。
     *
     * @param level 日志级别<br>
     *            {@link TraceFormat.VERBOSE} 详细<br>
     *            {@link TraceFormat.DEBUG} 调试<br>
     *            {@link TraceFormat.INFO} 信息<br>
     *            {@link TraceFormat.WARN} 警告<br>
     *            {@link TraceFormat.ERROR} 错误<br>
     *            或者他们的组合 —— 参考{@link TraceLevel}
     * @param thread 调用者线程对象
     * @param time 日志时间，单位ms
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 日志附带的异常对象
     */
    public void trace(int level, Thread thread, long time, String tag, String msg, Throwable tr) {
        // 检查日志追踪器是否有效
        if (isEnabled()) {
            // 检查是否是关心的级别
            if (Bit.has(mTraceLevel, level)) {
                // 调用实现方法打印日志
                try {
                    doTrace(level, thread, time, tag, msg, tr);
                } catch (OutOfMemoryError error) {
                    // MnsLog.e("Tracer", "out of memory ", error);
                }
            }
        }
    }

    /**
     * 追踪日志
     *
     * @param level 日志级别<br>
     *            {@link TraceFormat.VERBOSE} 详细<br>
     *            {@link TraceFormat.DEBUG} 调试<br>
     *            {@link TraceFormat.INFO} 信息<br>
     *            {@link TraceFormat.WARN} 警告<br>
     *            {@link TraceFormat.ERROR} 错误 —— 或者他们的组合}
     * @param formattedTrace 格式化后的日志字符串
     **/
    public void trace(int level, String formattedTrace) {
        if (isEnabled()) {
            if (Bit.has(mTraceLevel, level)) {
                doTrace(formattedTrace);
            }
        }
    }

    /**
     * 收到日志追踪请求，子类实现
     *
     * @param level 日志级别<br>
     *            {@link TraceFormat.VERBOSE} 详细<br>
     *            {@link TraceFormat.DEBUG} 调试<br>
     *            {@link TraceFormat.INFO} 信息<br>
     *            {@link TraceFormat.WARN} 警告<br>
     *            {@link TraceFormat.ERROR} 错误 —— 或者他们的组合}
     * @param thread 调用者线程对象
     * @param time 日志时间，单位ms
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 日志附带的异常对象
     */
    protected abstract void doTrace(int level, Thread thread, long time, String tag, String msg,
            Throwable tr);

    /**
     * 收到日志追踪请求，子类实现
     *
     * @param formattedTrace 格式化后的日志字符串
     **/
    protected abstract void doTrace(String formattedTrace);

    /**
     * 获得追踪器需要追踪的日志级别 <br>
     *
     * @return 日志级别
     */
    public int getTraceLevel() {
        return mTraceLevel;
    }

    /**
     * 设置日志追踪器的日志级别
     *
     * @param traceLevel 日志级别<br>
     *            参考{@link TraceLevel}中的常量
     */
    public void setTraceLevel(int traceLevel) {
        this.mTraceLevel = traceLevel;
    }

    /**
     * 检查当前追踪器是否启用
     *
     * @return true 表示追踪器已经启用<br>
     *         false 表示追踪器已被禁用
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * 启用/禁用当前追踪器
     *
     * @param enabled true 表示启用追踪器<br>
     *            false 表示禁用追踪器
     */
    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    /**
     * 获得当前追踪器的日志格式
     *
     * @return 当前追踪器的日志格式 <br>
     *         参考 {@link TraceFormat}
     */
    public TraceFormat getTraceFormat() {
        return mTraceFormat;
    }

    /**
     * 设置当前追踪器的日志格式
     *
     * @param traceFormat 日志格式 <br>
     *            参考 {@link TraceFormat}
     */
    public void setTraceFormat(TraceFormat traceFormat) {
        this.mTraceFormat = traceFormat;
    }
}
