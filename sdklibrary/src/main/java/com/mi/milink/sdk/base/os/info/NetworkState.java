
package com.mi.milink.sdk.base.os.info;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mi.milink.sdk.util.CommonUtils;

/**
 * 网络状态
 *
 * @autho MK
 */
public class NetworkState {
    private static final NetworkState NONE = new NetworkState(false, null, AccessPoint.NONE,
            NetworkType.NONE);

    /**
     * 从{@link android.net.NetworkInfo}构造网络状态信息
     *
     * @param info NetworkInfo对象
     * @return 网络状态
     */
    public static NetworkState fromNetworkInfo(NetworkInfo info) {
        // 得不到信息，返回NONE对象
        if (info == null) {
            return NetworkState.NONE;
        }

        NetworkState state = new NetworkState();
        state.setAvailable(info.isAvailable());
        state.setConnected(info.isConnected());
        state.setApnName(info.getExtraInfo());
        state.setAccessPoint(AccessPoint.forName(state.getApnName()));

        switch (info.getType()) {
        // WIFI网络
            case ConnectivityManager.TYPE_WIFI: {
                state.setType(NetworkType.WIFI);
                state.setApnType(NETWORK_TYPE_WIFI);
                break;
            }
            // 有线网络
            case ConnectivityManager.TYPE_ETHERNET: {
                state.setType(NetworkType.ETHERNET);
                state.setApnType(NETWORK_TYPE_WIFI);
                break;
            }
            // 移动网络
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI: {
                // 根据速度判定是否是3G网络
                state.setType(is3GMobileType(info.getSubtype()) ? NetworkType.MOBILE_3G
                        : NetworkType.MOBILE_2G);
                state.setApnType(info.getSubtype());
                break;
            }
            // 其他网络
            default:
                state.setType(NetworkType.OTHERS);
                state.setApnType(NETWORK_TYPE_WIFI);
                break;
        }

        // 保存额外的信息对象
        state.setMoreInfo(info);

        return state;
    }

    /*
     * 移动网络子类型常量 ******* 从 Android SDK 14 中复制的常量，防止目标SDK没有对应常量 ********
     */
    private static final int NETWORK_TYPE_WIFI = -1;

    private static final int NETWORK_TYPE_UNKNOWN = 0;

    private static final int NETWORK_TYPE_GPRS = 1;

    private static final int NETWORK_TYPE_EDGE = 2;

    private static final int NETWORK_TYPE_UMTS = 3;

    private static final int NETWORK_TYPE_CDMA = 4;

    private static final int NETWORK_TYPE_EVDO_0 = 5;

    private static final int NETWORK_TYPE_EVDO_A = 6;

    private static final int NETWORK_TYPE_1xRTT = 7;

    private static final int NETWORK_TYPE_HSDPA = 8;

    private static final int NETWORK_TYPE_HSUPA = 9;

    private static final int NETWORK_TYPE_HSPA = 10;

    private static final int NETWORK_TYPE_IDEN = 11;

    private static final int NETWORK_TYPE_EVDO_B = 12;

    private static final int NETWORK_TYPE_LTE = 13;

    private static final int NETWORK_TYPE_EHRPD = 14;

    private static final int NETWORK_TYPE_HSPAP = 15;

    /** Unknown network class. {@hide} */
    private static final int NETWORK_CLASS_UNKNOWN = 0;

    /** Class of broadly defined "2G" networks. {@hide} */
    private static final int NETWORK_CLASS_2_G = 1;

    /** Class of broadly defined "3G" networks. {@hide} */
    private static final int NETWORK_CLASS_3_G = 2;

    /** Class of broadly defined "4G" networks. {@hide} */
    private static final int NETWORK_CLASS_4_G = 3;

    private boolean connected = false;

    private boolean available = false;

    private String apnName = null;

    private int apnType = NETWORK_TYPE_WIFI;

    private NetworkType type = NetworkType.NONE;

    private AccessPoint accessPoint = AccessPoint.NONE;

    private NetworkInfo moreInfo;

    private NetworkState(boolean conn, String apn, AccessPoint ap, NetworkType tp) {
        setConnected(conn);
        setApnName(apn);
        setAccessPoint(ap);
        setType(tp);
    }

    private NetworkState() {

    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof NetworkState) {
            return (((NetworkState) o).isConnected() == this.isConnected()) //
                    && (((NetworkState) o).getType().equals(this.getType())) //
                    && (((NetworkState) o).getApnName().equals(this.getApnName()));
        } else {
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getApnName() {
        if (apnName == null) {
            return CommonUtils.EMPTY;
        } else {
            return apnName;
        }
    }

    public void setApnName(String apnName) {
        this.apnName = apnName;
    }

    public int getApnType() {
        return this.apnType;
    }

    public void setApnType(int type) {
        this.apnType = type;
    }

    public NetworkType getType() {
        return type;
    }

    public void setType(NetworkType type) {
        this.type = type;
    }

    public AccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(AccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    /**
     * 判断子类型是否是快速网络，姑且认为快速网络即为3G网络（包括4G）
     *
     * @param type 网络子类型
     * @return 3G网络，返回true; 2G网络或者未知网络，返回false
     */
    private static boolean is3GMobileType(int type) {
        switch (type) {
            case NETWORK_TYPE_1xRTT:// ~ 50-100 kbps
            case NETWORK_TYPE_CDMA:// ~ 14-64 kbps
            case NETWORK_TYPE_GPRS:// ~ 100 kbps
            case NETWORK_TYPE_EDGE:// ~ 50-100 kbps
            case NETWORK_TYPE_IDEN:// ~ 25 kbps
            case NETWORK_TYPE_UNKNOWN:
                return false;

            case NETWORK_TYPE_EVDO_0:// ~ 400-1000 kbps
            case NETWORK_TYPE_EVDO_A:// ~ 600-1400 kbps
            case NETWORK_TYPE_HSDPA:// ~ 2-14 Mbps
            case NETWORK_TYPE_HSPA:// ~ 700-1700 kbps
            case NETWORK_TYPE_HSUPA:// ~ 1-23 Mbps
            case NETWORK_TYPE_UMTS: // ~ 400-7000 kbps
            case NETWORK_TYPE_EHRPD:// ~ 1-2 Mbps
            case NETWORK_TYPE_EVDO_B:// ~ 5 Mbps
            case NETWORK_TYPE_HSPAP:// ~ 10-20 Mbps
            case NETWORK_TYPE_LTE:// ~ 10+ Mbps
                return true;

            default:
                return false;
        }
    }

    private static boolean is4GMobileType(int type) {
        return getNetworkClass(type) == NETWORK_CLASS_4_G;
    }

    /**
     * Return general class of network type, such as "3G" or "4G". In cases
     * where classification is contentious, this method is conservative.
     */
    private static int getNetworkClass(int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_GPRS:
            case NETWORK_TYPE_EDGE:
            case NETWORK_TYPE_CDMA:
            case NETWORK_TYPE_1xRTT:
            case NETWORK_TYPE_IDEN:
                return NETWORK_CLASS_2_G;
            case NETWORK_TYPE_UMTS:
            case NETWORK_TYPE_EVDO_0:
            case NETWORK_TYPE_EVDO_A:
            case NETWORK_TYPE_HSDPA:
            case NETWORK_TYPE_HSUPA:
            case NETWORK_TYPE_HSPA:
            case NETWORK_TYPE_EVDO_B:
            case NETWORK_TYPE_EHRPD:
            case NETWORK_TYPE_HSPAP:
                return NETWORK_CLASS_3_G;
            case NETWORK_TYPE_LTE:
                return NETWORK_CLASS_4_G;
            default:
                return NETWORK_CLASS_UNKNOWN;
        }
    }

    public NetworkInfo getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(NetworkInfo moreInfo) {
        this.moreInfo = moreInfo;
    }

    @Override
    public String toString() {
        return "NetworkState [connected=" + connected + ", apnName=" + apnName + ", type=" + type
                + ", accessPoint=" + accessPoint + "]";
    }
}
