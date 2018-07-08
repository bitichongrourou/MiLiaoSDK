
package com.mi.milink.sdk.base.debug;

/**
 * 可定制的日志追踪类，默认的实现是将日志信息输出到Logcat，类似android.util.Log的作用。 可以调用方法
 * {@code CustomLogcat.setCustomTracer()}设置指定的Tracer, 实现定制的日志追踪器
 * 
 * @author MK
 */
public class CustomLogcat implements TraceLevel {

    private static volatile Tracer sCustomTracer = new LogcatTracer();

    /**
     * 
     * @param tracer 当tracer为null, 相当与调用setCustomTracer(LogcatTracer.getInstance());
     */
    public static void setCustomTracer(Tracer tracer) {
        if (tracer == null) {
            sCustomTracer = new LogcatTracer();
        } else {
            sCustomTracer = tracer;
        }
    }

    public static Tracer getCustomTracer() {
        return sCustomTracer;
    }

    public static void v(String tag, String msg) {
        v(tag, msg, null);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (sCustomTracer != null) {
            sCustomTracer.trace(VERBOSE, Thread.currentThread(), System.currentTimeMillis(), tag,
                    msg, tr);
        }
    }

    public static void d(String tag, String msg) {
        d(tag, msg, null);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (sCustomTracer != null) {
            sCustomTracer.trace(DEBUG, Thread.currentThread(), System.currentTimeMillis(), tag, msg,
                    tr);
        }
    }

    public static void i(String tag, String msg) {
        i(tag, msg, null);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (sCustomTracer != null) {
            sCustomTracer.trace(INFO, Thread.currentThread(), System.currentTimeMillis(), tag, msg,
                    tr);
        }
    }

    public static void w(String tag, String msg) {
        w(tag, msg, null);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (sCustomTracer != null) {
            sCustomTracer.trace(WARN, Thread.currentThread(), System.currentTimeMillis(), tag, msg,
                    tr);
        }
    }

    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (sCustomTracer != null) {
            sCustomTracer.trace(ERROR, Thread.currentThread(), System.currentTimeMillis(), tag, msg,
                    tr);
        }
    }

}
