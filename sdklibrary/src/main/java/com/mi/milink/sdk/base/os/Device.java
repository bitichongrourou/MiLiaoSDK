
package com.mi.milink.sdk.base.os;

import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.base.os.info.DnsDash;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.StorageDash;
import com.mi.milink.sdk.base.os.info.WifiDash;

/**
 * 设备信息获取类
 * 
 * @author MK
 */
public class Device extends DeviceDash {
    /**
     * 获得设备信息字符串，形如"imei=xxxxxxx&model=xxxxx&……"<br>
     * <br>
     * 即原MNS的DeviceInfo，供业务层调取
     *
     * @return 设备信息
     */
    public static String getInfo() {
        return getInstance().getDeviceInfo();
    }

    /**
     * 存储器信息
     *
     * @author MK
     */
    public static class Storage extends StorageDash {

    }

    /**
     * 网络信息
     *
     * @author MK
     */
    public static class Network extends NetworkDash {
        /**
         * 系统代理信息<br>
         * <br>
         * 要获取系统默认代理，使用{@link Proxy#Default}
         *
         * @author MK
         */
        public static abstract class Proxy extends Http.HttpProxy {

        }

        /**
         * 本地DNS信息
         *
         * @author MK
         */
        public static class Dns extends DnsDash {

        }

        /**
         * WIFI网卡信息
         *
         * @author MK
         */
        public static class Wifi extends WifiDash {

        }
        

        public static class NetworkDetailInfo {
            public int apnType;

            public int cellLevel;

            public String apnName;

            public String wifiInfo;

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                if (this == o) {
                    return true;
                }
                NetworkDetailInfo that = (NetworkDetailInfo) o;
                if (apnType == that.apnType && cellLevel == that.cellLevel) {
                    if (apnName == null && that.apnName == null) {
                        if (wifiInfo == null && that.wifiInfo == null) {
                            return true;
                        }
                        if (wifiInfo.equals(that.wifiInfo)) {
                            return true;
                        }
                    }
                    if (apnName.equals(that.apnName)) {
                        if (wifiInfo == null && that.wifiInfo == null) {
                            return true;
                        }
                        if (wifiInfo.equals(that.wifiInfo)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("apnType=").append(apnType);
                sb.append(",").append("cellLevel=").append(cellLevel);
                sb.append(",").append("apnName=").append(apnName);
                sb.append(",").append("wifiInfo=").append(wifiInfo);
                sb.append("}");
                return sb.toString();
            }

            @Override
            public int hashCode() {
                int result = 17;
                if (apnName != null) {
                    result = 31 * result + apnName.hashCode();
                }
                if (wifiInfo != null) {
                    result = 31 * result + wifiInfo.hashCode();
                }
                result = 31 * result + apnType;
                result = 31 * result + cellLevel;
                return result;
            }

        }

        public static NetworkDetailInfo getCurrentNetworkDetailInfo() {
            NetworkDetailInfo info = new NetworkDetailInfo();
            info.apnType = NetworkDash.getApnType();
            info.apnName = NetworkDash.getApnNameOrWifiOrEthernet();
            info.wifiInfo = WifiDash.getWifiInfo();
            info.cellLevel = NetworkDash.getCellLevel();
            return info;
        }
    }

}
