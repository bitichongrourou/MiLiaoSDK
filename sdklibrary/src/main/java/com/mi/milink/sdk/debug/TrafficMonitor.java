
package com.mi.milink.sdk.debug;

import java.io.Serializable;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.text.TextUtils;

import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 流量统计
 * 
 * @author MK
 */
public class TrafficMonitor {

    private static final String TAG = "TrafficMonitor";

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static TrafficMonitor sInstance = new TrafficMonitor();

    private final ConcurrentHashMap<String, TrafficMonitorItem> mMonitorItemMap = new ConcurrentHashMap<String, TrafficMonitorItem>(
            32);

    private volatile long mStartTime;

    private volatile boolean mIsStarted;

    private TrafficMonitor() {
        mIsStarted = false;
    }

    public static TrafficMonitor getInstance() {
        return sInstance;
    }

    /**
     * 恢复流量统计
     */
    public void resume() {
        mIsStarted = true;
    }

    /**
     * 暂停流量统计
     */
    public void pause() {
        mIsStarted = false;
    }

    /**
     * 开始统计，从调用此方法这刻开始算起，之前的统计会被清空。
     */
    public void start() {
        mStartTime = System.currentTimeMillis();
        mMonitorItemMap.clear();
        mIsStarted = true;
    }

    public void traffic(String cmd, int size) {
        if (mIsStarted) {
            if (!TextUtils.isEmpty(cmd)) {
                TrafficMonitorItem item = mMonitorItemMap.get(cmd);
                if (item != null) {
                    item.count++;
                    item.totalSize += size;
                } else {
                    item = new TrafficMonitorItem();
                    mMonitorItemMap.put(cmd, item);
                    item.count++;
                    item.totalSize += size;
                }
            }
        }
    }

    /**
     * 输出时，按照内部命令字和业务命令字分类统计
     */
    public void print() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("startTime:").append(
                CommonUtils.createDataFormat(TIME_FORMAT).format(new Date(mStartTime)));
        ConcurrentHashMap<String, TrafficMonitorItem> map = new ConcurrentHashMap<String, TrafficMonitorItem>(
                32);
        map.putAll(mMonitorItemMap);
        int milinkCount = 0, mlinkTotalSize = 0;
        int otherCount = 0, otherTotalSize = 0;
        int httpCount = 0, httpTotalSize = 0;
        for (Entry<String, TrafficMonitorItem> entry : map.entrySet()) {
            if (Const.isMnsCmd(entry.getKey())) {
                milinkCount += entry.getValue().count;
                mlinkTotalSize += entry.getValue().totalSize;
            } else if (entry.getKey().contains(".do")) {
                httpCount += entry.getValue().count;
                httpTotalSize += entry.getValue().totalSize;
            } else {
                otherCount += entry.getValue().count;
                otherTotalSize += entry.getValue().totalSize;
            }
        }
        sb.append(" ");
        sb.append("{mlinkCount:").append(milinkCount).append(",");
        sb.append("mlinkTotalSize:").append(mlinkTotalSize).append("} ");
        sb.append("{otherCount:").append(otherCount).append(",");
        sb.append("otherTotalSize:").append(otherTotalSize).append("} ");
        sb.append("{httpCount:").append(httpCount).append(",");
        sb.append("httpTotalSize:").append(httpTotalSize).append("}");
        sb.append("]");
        MiLinkLog.i(TAG, "traffic statistic: " + sb.toString());
    }

    /**
     * 输出时，按照每个命令字分别统计
     */
    public void printDetail() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("startTime:").append(
                CommonUtils.createDataFormat(TIME_FORMAT).format(new Date(mStartTime)));
        ConcurrentHashMap<String, TrafficMonitorItem> map = new ConcurrentHashMap<String, TrafficMonitorItem>(
                32);
        map.putAll(mMonitorItemMap);
        for (Entry<String, TrafficMonitorItem> entry : map.entrySet()) {
            sb.append(" {cmd:").append(entry.getKey()).append(", count:")
                    .append(entry.getValue().count).append(", totalSize:")
                    .append(entry.getValue().totalSize).append("}");
        }
        sb.append("]");
        MiLinkLog.i(TAG, "traffic statistic detail: " + sb.toString());
    }

    static class TrafficMonitorItem implements Serializable {

        private static final long serialVersionUID = -1887022887439235063L;

        public int count; // 个数

        public int totalSize; // 总大小，单位是B

    }

}
