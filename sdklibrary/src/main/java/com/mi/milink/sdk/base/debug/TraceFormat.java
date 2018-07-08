
package com.mi.milink.sdk.base.debug;

import android.text.format.Time;
import android.util.Log;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 日志格式化类<br>
 * <br>
 * <p>
 * 将日志信息格式化为字符串的格式类，本类的实例已经实现了默认格式<br>
 * 若要自定义一种日志输出的格式，请继承并重载该类的方法，生成子类实例用于构造要使用的{@link CustomLogcat}对象<br>
 * <br>
 * 若要使用默认格式，请引用 {@code TraceFormat.DEFAULT} 对象<br>
 * <br>
 * {@code getLevelPrefix()} :<br>
 * 对于指定级别的前缀描述字符，默认为 <i>"V D I W E A -"</i>，分别对应详尽、调试、信息、警告、错误、断言和未知的级别<br>
 * <br>
 * {@code formatTrace()} :<br>
 * 格式化日志，默认为 "级别字母 [时间][调用者线程名][标签] 内容\n *** EXCEPTION : \n 异常调用堆栈" <br>
 * <br>
 * {@code formatThrowable()} :<br>
 * 格式化异常信息，默认为 Trowable.printStackTrace()的样式，逻辑上略有简化 <br>
 * </p>
 *
 * @author MK
 */
public class TraceFormat {
    public static final String STR_VERBOSE = "V";

    public static final String STR_DEBUG = "D";

    public static final String STR_INFO = "I";

    public static final String STR_WARN = "W";

    public static final String STR_ERROR = "E";

    public static final String STR_ASSERT = "A";

    public static final String STR_UNKNOWN = "-";

    public static final String TRACE_TIME_FORMAT = "%Y-%m-%d %H:%M:%S";

    public static final TraceFormat DEFAULT = new TraceFormat();

    /**
     * 获得日志级别前缀
     *
     * @param level 日志级别 参考 {@link TraceLevel} <br>
     * <br>
     *            默认为 <i>"V D I W E A -"</i>，分别对应详尽、调试、信息、警告、错误、断言和未知的级别<br>
     * <br>
     * @return 日志级别前缀字符串
     */
    public static final String getLevelPrefix(int level) {
        switch (level) {
            case TraceLevel.DEBUG:
                return STR_DEBUG;
            case TraceLevel.INFO:
                return STR_INFO;
            case TraceLevel.WARN:
                return STR_WARN;
            case TraceLevel.ERROR:
                return STR_ERROR;
            case TraceLevel.VERBOSE:
                return STR_VERBOSE;
            case TraceLevel.ASSERT:
                return STR_ASSERT;
            default:
                return STR_UNKNOWN;
        }
    }

    /**
     * 格式化日志信息
     *
     * @param level 日志等级
     * @param thread 调用者线程 <br>
     *            <i>(p.s. 如果调用者线程为空，则线程名称显示为 "N/A" )</i><br>
     * @param time 日志时间
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 相关异常
     * @return 格式化的日志信息
     */
    public String formatTrace(int level, Thread thread, long time, String tag, String msg,
            Throwable tr) {
        // 因为String.format() 也是用StringBuilder实现的，但是格式化比较慢，就换StringBuilder自己造了
        // 因为获取毫秒数在某些华为机型上有问题，所以也自己获取了
        // 因为Android提供了更方便的格式化Throwable的方法，就不用自己的了
        // 稍微修改了一下格式，和Android Logcat接近了
        // 按照Android Documents的说法，Time对象比Calender更好一点

        try {
            long ms = time % 1000;

            Time timeObj = new Time();

            timeObj.set(time);

            StringBuilder builder = new StringBuilder();

            builder.append(getLevelPrefix(level)).append('/')
                    .append(timeObj.format(TRACE_TIME_FORMAT)).append('.');

            if (ms < 10) {
                builder.append("00");
            } else if (ms < 100) {
                builder.append('0');
            }

            builder.append(ms).append(' ').append('[').append(Global.getPid()).append(']')
                    .append('[');

            if (thread == null) {
                builder.append(CommonUtils.NOT_AVALIBLE);
            } else {
                builder.append(thread.getName());
            }

            builder.append(']').append('[').append(tag).append(']').append(' ').append(msg)
                    .append('\n');

            if (tr != null) {
                builder.append("*** Exception : \n").append(Log.getStackTraceString(tr))
                        .append('\n');
            }

            return builder.toString();
        } catch (OutOfMemoryError e) {
            return "";
        }
    }
}
