
package com.mi.milink.sdk.base.debug;

import android.util.Log;

import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * Android Logcat日志追踪器<br>
 * <br>
 * 一个方便的{@code android.util.Log}的包装类，用于将日志信息输出到Logcat<br>
 * <br>
 * 若仅仅需要Logcat的功能，建议使用 {@code LogcatTracer.getInstance()}而非创建一个新的实例
 * 
 * @author MK
 */
public final class LogcatTracer extends Tracer {

    public LogcatTracer() {
        super();
        setTraceLevel(Const.Debug.DefLogcatTraceLevel);
    }

    @Override
    protected void doTrace(int level, Thread thread, long time, String tag, String msg, Throwable tr) {
        msg = thread.getName() + "=>" + msg;
        switch (level) {
            case TraceLevel.VERBOSE: {
                Log.v(tag, msg, tr);
            }
                break;
            case TraceLevel.DEBUG: {
                Log.d(tag, msg, tr);
            }
                break;
            case TraceLevel.INFO: {
                Log.i(tag, msg, tr);
            }
                break;
            case TraceLevel.WARN: {
                Log.w(tag, msg, tr);
            }
                break;
            case TraceLevel.ERROR: {
                Log.e(tag, msg, tr);
            }
                break;
            case TraceLevel.ASSERT: {
                Log.e(tag, msg, tr);
            }
                break;
            default:
                break;
        }
    }

    @Override
    protected void doTrace(String formattedTrace) {
        Log.v(CommonUtils.EMPTY, formattedTrace);
    }
}
