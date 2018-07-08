
package com.mi.milink.sdk.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 客户端App信息对象
 *
 * @author MK
 */
public class ClientAppInfo implements Parcelable {

    public static final int VTALK_APP_ID = 10001;

    public static final int MILIAO_APP_ID_TEMP = 10002; // miliao 7.1

    public static final int MILIAO_APP_ID = 10004; // miliao

    public static final int ON_APP_ID = 10011;

    public static final int FORUM_APP_ID = 10005;
    
    public static final int SUPPORT_APP_ID = 10006;

    public static final int GAME_CENTER_APP_ID = 20001;

    public static final int XIAOMI_PUSH_APP_ID = 30001;
    
    public static final int LIVE_APP_ID = 10007;
    
    public static final int LIVE_SDK_APP_ID = 10008;
    
    public static final int MILIAO_2 = 10014;
    
    public static final int CARTOON_APP_ID = 10009;
    
    public static final int KNIGHTS_APP_ID = 10010;
    
    public static final int GAME_LOGIN_PAY_SDK = 20002;
    
    public static final int MI_SHOP_APP_ID = 20003;

    public static final int MI_NEW_GAME_CENTER_APP_ID = 20005;

    public static final int YI_MI_BUY = 10012;
    
    public static final int LIVE_PUSH_SDK_BOTTOM = 100001;
    
    public static final int LIVE_PUSH_SDK_UP = 200000;
    
    public static final int MODE_LONG_CONNECTION = 0;

    public static final int MODE_MEDIUM_CONNECTION = 1;

    private static final String DEFAULT_LANGUAGE_CODE = "zh_CN";

    // 必须，客户端AppId，由服务端分配
    private int appId = 0;

    // 必须，平台名称
    private String platformName = "and";
    // 必须，客户端名称
    private String appName = CommonUtils.NOT_AVALIBLE;

    // 必须，客户端版本名字
    private String versionName = CommonUtils.NOT_AVALIBLE;

    // 必须，客户端版本号
    private int versionCode = 0;

    // 必须，客户端渠道号，如果是TEST则使用测试环境IP。
    private String releaseChannel = CommonUtils.NOT_AVALIBLE;

    // 必须，包名
    private String packageName = CommonUtils.NOT_AVALIBLE;

    // 默认，连接模式，MODE_MEDIUM_CONNECTION、MODE_LONG_CONNECTION两个值可选;默认为长连接模式，中连接模式如果10分钟未发包会主动断开连接。
    private int mMode = MODE_LONG_CONNECTION;

    // 默认，语言
    private String languageCode = DEFAULT_LANGUAGE_CODE;

    // 默认，日志路径，不填的话日志会默认保存在SDCard/XiaoMi/appName/logs目录下。
    private String logPath = "";

    private String mipushAppId = "";

    private String mipushAppKey = "";

    private String gv = "";

    private String serviceProcessName = "";
    
    private boolean isIpModle;
    

    public ClientAppInfo(String savedInstance) {
        fromString(savedInstance);
    }

    private ClientAppInfo(Builder builder) {
        appId = builder.appId;
        platformName = builder.platformName;
        versionCode = builder.versionCode;
        versionName = builder.versionName;
        appName = builder.appName;

        releaseChannel = builder.releaseChannel;
        packageName = builder.packageName;
        serviceProcessName = builder.serviceProcessName;
        languageCode = builder.languageCode;

        logPath = builder.logPath;
        mipushAppId = builder.mipushAppId;
        mipushAppKey = builder.mipushAppKey;
        gv = builder.gv;
        mMode = builder.mMode;
        
        isIpModle = builder.isIpModle();

        if (appId == 0) {
            throw new IllegalArgumentException("appid can not be 0, error!");
        }
        if (versionCode == 0) {
            throw new IllegalArgumentException("versionCode can not be 0, error!");
        }
        if (CommonUtils.NOT_AVALIBLE.equals(versionName)) {
            throw new IllegalArgumentException("versionName is not correct, error!");
        }
        if (CommonUtils.NOT_AVALIBLE.equals(appName)) {
            throw new IllegalArgumentException("appName is not correct, error!");
        }
        if (CommonUtils.NOT_AVALIBLE.equals(releaseChannel)) {
            throw new IllegalArgumentException("releaseChannel is not correct, error!");
        }
        if (CommonUtils.NOT_AVALIBLE.equals(packageName)) {
            throw new IllegalArgumentException("packageName is not correct, error!");
        }
    }

    public static class Builder {
        // 客户端AppId
        private int appId = 0;

        // 平台名称
        private String platformName = "and";
        
        // 客户端版本号
        private int versionCode = 0;

        // 客户端名称
        private String appName = CommonUtils.NOT_AVALIBLE;

        // 客户端版本名字
        private String versionName = CommonUtils.NOT_AVALIBLE;

        // 客户端渠道号
        private String releaseChannel = CommonUtils.NOT_AVALIBLE;

        private String packageName = CommonUtils.NOT_AVALIBLE;

        private String serviceProcessName = "";

        private String languageCode = DEFAULT_LANGUAGE_CODE;

        private String logPath = "";

        private String mipushAppId = "";

        private String mipushAppKey = "";

        private String gv = "";

        private int mMode = MODE_LONG_CONNECTION;

        private boolean isIpModle;
        
        public Builder(int appId) {
            this.appId = appId;
        }

        public boolean isIpModle() {
			return isIpModle;
		}

		public void setIpModle(boolean isIpModle) {
			this.isIpModle = isIpModle;
		}

		public Builder setVersionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        public Builder setVersionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        public Builder setReleaseChannel(String releaseChannel) {
            this.releaseChannel = releaseChannel;
            return this;
        }

        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder setServiceProcessName(String serviceProcessName) {
            this.serviceProcessName = serviceProcessName;
            return this;
        }

        public Builder setLanguageCode(String languageCode) {
            this.languageCode = languageCode;
            return this;
        }

        public Builder setLogPath(String logPath) {
            this.logPath = logPath;
            return this;
        }

        public Builder setMipushAppKey(String mipushAppKey) {
            this.mipushAppKey = mipushAppKey;
            return this;
        }

        public Builder setMipushAppId(String mipushAppId) {
            this.mipushAppId = mipushAppId;
            return this;
        }

        public Builder setPlatformName(String name) {
            this.platformName = name;
            return this;
        }
        
        public Builder setGv(String gv) {
            this.gv = gv;
            return this;
        }

        public Builder setLinkMode(int linkMode) {
            this.mMode = linkMode;
            return this;
        }

        public ClientAppInfo build() {
            return new ClientAppInfo(this);
        }
    }

    public boolean isMediumConnection() {
        return this.mMode == MODE_MEDIUM_CONNECTION;
    }

    private void setLinkMode(int linkMode) {
        this.mMode = linkMode;
    }

    public int getLinkMode() {
        return mMode;
    }

    public String getGv() {
        return gv;
    }

    private void setGv(String gv) {
        this.gv = gv;
    }

	public String getPlatformName() {
		return platformName;
	}
	
    public int getAppId() {
        return appId;
    }

    public String getMiPushAppId() {
        return mipushAppId;
    }

    private void setMiPushAppId(String appId) {
        this.mipushAppId = appId;
    }

    private void setMiPushAppKey(String appKey) {
        this.mipushAppKey = appKey;
    }

    public String getMiPushAppKey() {
        return mipushAppKey;
    }

    private void setAppId(int appId) {
        this.appId = appId;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getAppName() {
        return appName;
    }

    private void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    private void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getReleaseChannel() {
        return releaseChannel;
    }

    private void setReleaseChannel(String releaseChannel) {
        this.releaseChannel = releaseChannel;
    }

    public boolean isIpModle() {
		return isIpModle;
	}

	public void setIpModle(boolean isIpModle) {
		this.isIpModle = isIpModle;
	}

	private void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    private void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String language) {
        if (!TextUtils.isEmpty(language)) {
            this.languageCode = language;
        } else {
            this.languageCode = DEFAULT_LANGUAGE_CODE;
        }
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    @Override
    public String toString() {
        return appId + ";" + versionCode + ";" + versionName + ";" + releaseChannel + ";" + appName
                + ";" + packageName + ";" + languageCode + ";" + logPath + ";"+platformName+";";
    }

    private void fromString(String savedInstance) {
        if (!TextUtils.isEmpty(savedInstance)) {
            String[] fields = savedInstance.split(";");
            setAppId(Convert.strToInt(fields[0], 0));
            setVersionCode(Convert.strToInt(fields[1], 1));
            setVersionName(fields[2]);
            setReleaseChannel(fields[3]);
            if (fields.length > 4) {
                setAppName(fields[4]);
            }
            if (fields.length > 5) {
                setPackageName(fields[5]);
            }
            if (fields.length > 6) {
                setLanguageCode(fields[6]);
            }
            if (fields.length > 7) {
                setLogPath(fields[7]);
            }
            if (fields.length > 8) {
                setServiceProcessName(fields[8]);
            }
            if (fields.length > 9) {
                setMiPushAppId(fields[9]);
            }
            if (fields.length > 10) {
                setMiPushAppKey(fields[10]);
            }
            if (fields.length > 11) {
                setGv(fields[11]);
            }
            if (fields.length > 12) {
                setLinkMode(Convert.strToInt(fields[12], 12));
            }
            if (fields.length > 13) {
                setPlatformName(fields[13]);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getAppId());
        dest.writeInt(getVersionCode());
        dest.writeString(getVersionName());
        dest.writeString(getReleaseChannel());
        dest.writeString(getAppName());
        dest.writeString(getPackageName());
        dest.writeString(getLanguageCode());
        dest.writeString(getLogPath());
        dest.writeString(getServiceProcessName());
        dest.writeString(getMiPushAppId());
        dest.writeString(getMiPushAppKey());
        dest.writeString(getGv());
        dest.writeInt(getLinkMode());
        dest.writeString(getPlatformName());
    }

    public void readFromParcel(Parcel source) {
        setAppId(source.readInt());
        setVersionCode(source.readInt());
        setVersionName(source.readString());
        setReleaseChannel(source.readString());
        setAppName(source.readString());
        setPackageName(source.readString());
        setLanguageCode(source.readString());
        setLogPath(source.readString());
        setServiceProcessName(source.readString());
        setMiPushAppId(source.readString());
        setMiPushAppKey(source.readString());
        setGv(source.readString());
        setLinkMode(source.readInt());
        setPlatformName(source.readString());
    }

    private void setPlatformName(String readString) {
		this.platformName = readString;
 	}

	public static final Parcelable.Creator<ClientAppInfo> CREATOR = new Parcelable.Creator<ClientAppInfo>() {

        @Override
        public ClientAppInfo createFromParcel(Parcel source) {
            ClientAppInfo obj = new ClientAppInfo("");
            obj.readFromParcel(source);
            return obj;
        }

        @Override
        public ClientAppInfo[] newArray(int size) {
            return new ClientAppInfo[size];
        }
    };

    public static boolean isTestChannel() {
        return Global.getClientAppInfo().getReleaseChannel().contains("test")
                || Global.getClientAppInfo().getReleaseChannel().contains("TEST");
    }

    public static boolean isMiliaoApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.MILIAO_APP_ID_TEMP
                || Global.getClientAppInfo().getAppId() == ClientAppInfo.MILIAO_APP_ID;
    }

    public static boolean isForumApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.FORUM_APP_ID;
    }

    public static boolean isVtalkApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.VTALK_APP_ID;
    }

    public static boolean isOnApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.ON_APP_ID;
    }

    public static boolean isGameCenterApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.GAME_CENTER_APP_ID;
    }

    public static boolean isXiaoMiPushApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.XIAOMI_PUSH_APP_ID;
    }
    
    public static boolean isSupportApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.SUPPORT_APP_ID;
    }
    
    public static boolean isLiveApp() {
        return Global.getClientAppInfo().getAppId() == ClientAppInfo.LIVE_APP_ID;
    }

    public static boolean isSupportMiPush() {
        String mipushAppid = Global.getClientAppInfo().getMiPushAppId();
        String mipushAppkey = Global.getClientAppInfo().getMiPushAppKey();
        return !TextUtils.isEmpty(mipushAppid) && !TextUtils.isEmpty(mipushAppkey);
    }

    /**
     * @param client1
     * @param client1
     * @return 1>2 返回1 1=2 返回0 1<2 返回－1
     */
    public static int isHigherVersion(ClientAppInfo client1, ClientAppInfo client2) {
        if (client1 == null || client2 == null) {
            return 0;
        }
        return client1.getVersionCode() - client2.getVersionCode();
    }

    public String getServiceProcessName() {
        return serviceProcessName;
    }

    private void setServiceProcessName(String serviceProcessName) {
        this.serviceProcessName = serviceProcessName;
    }



}
