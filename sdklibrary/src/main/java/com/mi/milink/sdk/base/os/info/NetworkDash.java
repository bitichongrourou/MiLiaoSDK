
package com.mi.milink.sdk.base.os.info;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.debug.CustomLogcat;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 设备网络信息收集类 <br>
 * <br>
 * 为了保证入口的简洁和代码的可读性，请使用{@link com.mi.milink.sdk.base.os.Device.Network}
 *
 * @author MK
 */
public class NetworkDash {
    /**
     * 判断当前网络是否可用，这将刷新当前网络信息
     *
     * @return 网络是否已经连接上
     */
    public static boolean isAvailable() {
        updateNetworkState();

        NetworkState state = getCurrState();

        if (state != null) {
            return getCurrState().isAvailable();
        } else {
            return false;
        }
    }

    /**
     * 获得当前移动网络的接入点<br>
     * <br>
     * 如果无网络/并非移动网络，得到{@link AccessPoint.NONE}<br>
     * 如果并非{@link AccessPoint}枚举中的范围，得到{@link AccessPoint.NEVER_HEARD}
     *
     * @return 接入点枚举
     * @see AccessPoint
     */
    public static AccessPoint getAccessPoint() {
        NetworkState state = getCurrState();

        if (state != null) {
            return state.getAccessPoint();
        } else {
            return AccessPoint.NONE;
        }
    }

    /**
     * 获得当前的网络类型
     *
     * @return 网络类型枚举
     * @see NetworkType
     */
    public static NetworkType getType() {
        NetworkState state = getCurrState();

        if (state != null) {
            return state.getType();
        } else {
            return NetworkType.NONE;
        }
    }

    /**
     * 获得当前的接入点名称 <br>
     * <br>
     * 建议只在需要未知接入点或者具体接入点名称时使用，若要对接入点进行逻辑分支，请使用{@link getAccessPoint()}方法
     *
     * @return 接入点名称
     * @see AccessPoint
     */
    public static String getApnName() {
        NetworkState state = getCurrState();

        if (state != null) {
            return state.getApnName();
        } else {
            return CommonUtils.EMPTY;
        }
    }

    /**
     * 获得当前的接入点类型 <br>
     * <br>
     * 建议只在需要未知接入点或者具体接入点名称时使用，若要对接入点进行逻辑分支，请使用{@link getAccessPoint()}方法
     *
     * @return 接入点名称
     * @see AccessPoint
     */
    public static int getApnType() {
        NetworkState state = getCurrState();

        if (state != null) {
            return state.getApnType();
        }
        return -1;
    }

    /**
     * 获得接入点名字或者"wifi"字样
     *
     * @return
     */
    public static String getApnNameOrWifi() {
        if (!isAvailable()) {
            return CommonUtils.EMPTY;
        } else {
            if (isWifi()) {
                return "wifi";
            } else {
                return getApnName();
            }
        }
    }

    /**
     * 获得接入点名字或者"wifi"字样或者"ethernet"字样
     *
     * @return
     */
    public static String getApnNameOrWifiOrEthernet() {
        if (!isAvailable()) {
            return CommonUtils.EMPTY;
        } else {
            if (isWifi()) {
                return "wifi";
            } else if (isEthernet()) {
                return "ethernet";
            } else {
                return getApnName();
            }
        }
    }

    /**
     * 获得当前的运营商，使用APN
     *
     * @return 运营商
     * @see ServiceProvider
     */
    public static ServiceProvider getProvider() {
        NetworkState state = getCurrState();

        if (state != null) {
            return state.getAccessPoint().getProvider();
        } else {
            return ServiceProvider.NONE;
        }
    }

    /**
     * 获得当前的运营商
     *
     * @param useIMSIFirst 优先使用IMSI判断
     * @return 运营商
     * @see ServiceProvider
     */
    public static ServiceProvider getProvider(boolean useIMSIFirst) {
        ServiceProvider provider = ServiceProvider.NONE;

        if (useIMSIFirst) {
            provider = getIMSIProvider();

            if (!ServiceProvider.NONE.equals(provider)) {
                return provider;
            }
        }

        provider = getProvider();

        return provider;
    }

    /**
     * 获得当前的运营商，根据IMSI中的MNC和MCC记录，即可刷新
     *
     * @return
     */
    public static ServiceProvider updateIMSIProvider() {
        // 防止权限问题和IPC通信问题
        try {
            synchronized (NetworkDash.class) {
                String IMSI = getIMSI();

                sImsiProvider = ServiceProvider.fromIMSI(IMSI);

                CustomLogcat.w("NetworkObserver", IMSI + " => " + sImsiProvider);

                return sImsiProvider;
            }
        } catch (Exception e) {
            return ServiceProvider.NONE;
        }
    }

    public static String getIMSI() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) Global
                    .getSystemService(Context.TELEPHONY_SERVICE);

            String IMSI1 = telephonyManager.getSimOperator();
            if (TextUtils.isEmpty(IMSI1)) // 处理双卡手机
            {
                IMSI1 = getDeviceIdBySlot(Global.getContext(), 0);
            }
            if (TextUtils.isEmpty(IMSI1)) {
                IMSI1 = getDeviceIdBySlot(Global.getContext(), 1);
            }

            CustomLogcat.i("NetworkDash", "imsi: " + IMSI1);

            return IMSI1;
        } catch (Exception e) {
            return null;
        }
    }

    // 获取双卡双待IMEI
    public static String getDeviceIdBySlot(Context context, int slotID) {
        CustomLogcat.v("NetworkDash", "isDeviceIdBySlot:" + slotID);
        if (context == null)
            return null;
        if (slotID < 0 || slotID > 1)
            return null;

        String imei = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> mLoadClass = Class.forName("android.telephony.TelephonyManager");

            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimStateGemini = mLoadClass.getMethod("getSimOperatorGemini", parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimStateGemini.invoke(telephonyManager, obParameter);

            if (ob_phone != null) {
                imei = ob_phone.toString();
            }
        } catch (Exception e) {

        }
        return imei;
    }

    /**
     * 获得当前的运营商，根据IMSI中的MNC和MCC记录
     *
     * @return the imsiProvider
     */
    public static ServiceProvider getIMSIProvider() {
        if (sImsiProvider == null) {
            updateIMSIProvider();
        }

        return sImsiProvider;
    }

    /**
     * 判断当前是否是WAP网络
     *
     * @return 若是移动网络且接入点是WAP接入点，返回true，否则返回false，包括非wap网络、非移动网络和无网络
     */
    public static boolean isWap() {
        return getAccessPoint().isWap();
    }

    /**
     * 判断当前是否是移动网络
     *
     * @return 若是移动网络，返回true，否则返回false，包括非移动网络和无网络
     */
    public static boolean isMobile() {
        NetworkType type = getType();

        return NetworkType.MOBILE_3G.equals(type) || NetworkType.MOBILE_2G.equals(type);
    }

    public static boolean is2G() {
        NetworkType type = getType();

        return NetworkType.MOBILE_2G.equals(type);
    }

    public static boolean is3G() {
        NetworkType type = getType();

        return NetworkType.MOBILE_3G.equals(type);
    }

    /**
     * 判断当前是否是WIFI网络
     *
     * @return 若是WIFI网络，返回true，否则返回false，包括非WIFI网络和无网络
     */
    public static boolean isWifi() {
        return NetworkType.WIFI.equals(getType());
    }

    /**
     * 判断当时是否是有线网络
     *
     * @return 若是有线网络，返回true，否则返回flase
     */
    public static boolean isEthernet() {
        return NetworkType.ETHERNET.equals(getType());
    }

    /**
     * 获得手机信号格数，和状态栏的提示理论上保持一致 <br>
     * <br>
     * <b>支持GSM/CDMA/EVDO网络，会自动处理<br>
     * <b>至少需要Android 2.1及以上的API Level (7)</b>
     *
     * @return 从弱到强 0..4<br>
     *         如果不支持或者尚未获得，返回 -1
     */
    public static int getCellLevel() {
        return OBSERVER.getCellLevel();
    }

    /**
     * 添加网络状态变化监听器
     *
     * @param listener 监听器
     */
    public static void addListener(NetworkStateListener listener) {
        synchronized (OBSERVER_LIST) {
            OBSERVER_LIST.add(new WeakReference<NetworkStateListener>(listener));
        }
    }

    /**
     * 移除网络状态变化监听器
     *
     * @param listener 监听器
     */
    public static void removeListener(NetworkStateListener listener) {
        synchronized (OBSERVER_LIST) {
            WeakReference<NetworkStateListener> reference = null;

            for (WeakReference<NetworkStateListener> weakReference : OBSERVER_LIST) {
                NetworkStateListener realListener = weakReference.get();

                if (realListener != null) {
                    if (realListener.equals(listener)) {
                        reference = weakReference;
                        break;
                    }
                }
            }

            OBSERVER_LIST.remove(reference);
        }
    }

    private static NetworkState sCurrState;

    private static NetworkState sLastState;

    private static Handler sMainHandler;

    /**
     * IMSI的运营商，为了减少IPC通信，只在网络变更的时候更新它
     */
    private static ServiceProvider sImsiProvider = null;

    private static final NetworkObserver OBSERVER = new NetworkObserver() {
        @Override
        public void onNetworkChanged() {
            updateNetworkState();
        }

    };

    private static final List<WeakReference<NetworkStateListener>> OBSERVER_LIST = new ArrayList<WeakReference<NetworkStateListener>>();

    static {
        // 初始化当前网络状态
        updateNetworkState();
        // 开始监听网络变化
        OBSERVER.startListen();
    }

    private static void notifyNetworkStateChange() {
        if (OBSERVER_LIST == null) {
            return;
        }

        synchronized (OBSERVER_LIST) {
            for (WeakReference<NetworkStateListener> listener : OBSERVER_LIST) {
                NetworkStateListener realListener = listener.get();

                if (realListener != null) {
                    realListener.onNetworkStateChanged(getLastState(), getCurrState());
                }
            }
        }
    }

    /**
     * 刷新网络信息 <br>
     *
     * @return 网络信息是否变化
     */
    public static boolean updateNetworkState() {
        synchronized (NetworkDash.class) {

            NetworkInfo info = null;
            try {
                ConnectivityManager manager = (ConnectivityManager) Global
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                if (manager == null) {
                    return false;
                }
                info = manager.getActiveNetworkInfo();

            } catch (Error e1) {
                info = null;
            } catch (Exception e) {
                info = null;
            }
            // 一步一个坑的系统API 别问我为什么加TC ...
            // 好吧 直接改成exception, 因为还发现有nullpoint的出现在这里
            boolean changed = setCurrState(NetworkState.fromNetworkInfo(info));

            if (changed) {
                // 网络变动时，更新IMSI信息
                updateIMSIProvider();

                if (sMainHandler == null) {
                    sMainHandler = new Handler(Global.getMainLooper());
                }

                sMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyNetworkStateChange();
                    }
                });
            }

            return changed;
        }
    }

    /**
     * 获得当前的网络状态信息
     *
     * @return 网络状态信息
     * @see NetworkState
     */
    public static NetworkState getCurrState() {
        return sCurrState;
    }

    protected static NetworkState getLastState() {
        return sLastState;
    }

    /**
     * 更新当前的状态
     *
     * @param newState
     * @return 若状态变化，则返回true; 否则返回false
     */
    protected static boolean setCurrState(NetworkState newState) {
        synchronized (NetworkDash.class) {
            boolean changed = false;

            if (sCurrState == null) {
                NetworkDash.sLastState = sCurrState;
                NetworkDash.sCurrState = newState;

                changed = true;
            }

            if (!sCurrState.equals(newState)) {
                NetworkDash.sLastState = NetworkDash.sCurrState;
                NetworkDash.sCurrState = newState;

                changed = true;
            }

            if (changed) {
                CustomLogcat.w("NetworkObserver", "LAST -> " + sLastState);
                CustomLogcat.w("NetworkObserver", "CURR -> " + sCurrState);
            }

            return changed;
        }
    }
}
