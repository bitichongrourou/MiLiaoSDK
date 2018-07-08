
package com.mi.milink.sdk.mipush;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.data.Option;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.xiaomi.channel.commonutils.logger.LoggerInterface;
import com.xiaomi.mipush.sdk.Logger;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushMessage;

public class MiPushManager {
    public static final String PREF_KEY_REGID = "MIPUSH_REG_ID2";

    public static final String PREF_KEY_ALIAS = "MIPUSH_ALIAS";
    
    private static final String TAG = "MiPushManager";

    private MiPushMessageListener l = null;

    private static MiPushManager INSTANCE;

    private WakeLock mWakeLock;

    private Handler mHandler;

    private MiPushManager() {
        try {
            mHandler = new Handler(Global.getMainLooper());
        } catch (Exception e) {

        }
        mRegId = Option.getString(PREF_KEY_REGID, "");
        mAlias = Option.getString(PREF_KEY_ALIAS, "");
        MiLinkLog.w(TAG, "MiPushManager() mRegId="+mRegId);
        
        LoggerInterface newLogger = new LoggerInterface() {
            @Override
            public void setTag(String tag) {
                // ignore
            }

            @Override
            public void log(String content, Throwable t) {
            	MiLinkLog.w(TAG, content, t);
            }

            @Override
            public void log(String content) {
            	MiLinkLog.d(TAG, content);
            }
        };
        Logger.setLogger(Global.getContext(), newLogger);
    }

    public static MiPushManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MiPushManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiPushManager();
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void logoff() {
        MiLinkLog.w(TAG, "mipush logoff mAlias:"+mAlias);
        if (!TextUtils.isEmpty(mAlias)) {
        	MiPushClient.unsetAlias(Global.getContext(), mAlias, null);
        }
        mAlias = "";
        Option.putString(PREF_KEY_ALIAS, mAlias).commit();
        mRegId = "";
        Option.putString(PREF_KEY_REGID, mRegId).commit();
    }

    private boolean registing = false;

    public synchronized void setRegisting(boolean ing) {
        registing = ing;
    }

    private String mRegId;

    private String mAlias;

    public interface MiPushRegisterListener {
        void onSetMiPushRegId(String regId);
    }

    private MiPushRegisterListener mMiPushRegisterListener;

    public void setMiPushRegisterListener(MiPushRegisterListener l) {
        this.mMiPushRegisterListener = l;
    }

    public void setMiPushRegId(String regId) {
        MiLinkLog.w(TAG, "setMiPushRegId regId="+regId);
        if (!TextUtils.isEmpty(regId) && !regId.equals(mRegId)) {
            mRegId = regId;
            Option.putString(PREF_KEY_REGID, mRegId).commit();
            // 通知milink进程
            if (mMiPushRegisterListener != null) {
                mMiPushRegisterListener.onSetMiPushRegId(regId);
            }
        }
    }

    public void setAlias(String alias) {
        MiLinkLog.w(TAG, "setMiPushRegId alias="+alias);
        if (!TextUtils.isEmpty(alias) && !alias.equals(mAlias)) {
            mAlias = alias;
            Option.putString(PREF_KEY_ALIAS, mAlias).commit();
        }
    }

    public void clearAlias() {
        MiLinkLog.w(TAG, "clearAlias ");
        mAlias = "";
        Option.putString(PREF_KEY_ALIAS, mAlias).commit();
    }
    
    private String userId;

    public synchronized void registerMiPush(String userId, MiPushRegisterListener l) {
        this.userId = userId;
        this.mMiPushRegisterListener = l;
        registerMiPush(false);
    }

    
    public synchronized void registerMiPush(boolean clearRegid){
    	  if(clearRegid){
    		  MiLinkLog.w(TAG, "clearRegid==true");
    		  mRegId = "";
    	  }
    	  // 判断是否不用注册
    	  if (!TextUtils.isEmpty(mRegId)){
    		  // 已经注册了regid
    		  if(TextUtils.isEmpty(userId)){
        		  //匿名 无需注册
    			  MiLinkLog.w(TAG, " userId==null & mRegId!=null,register cancel");
    			  return;
        	  }else{
        		  //实名
        		  if (!TextUtils.isEmpty(mAlias) && mAlias.equals(userId) ) {
                      // regid和alias都不为空。那就不用注册啊
                      MiLinkLog.w(TAG, "mRegId and mAlias not null,register cancel");
                      return;
                  }
        	  }
          }
    	  
          MiLinkLog.w(TAG, "request registerMiPush registing=" + registing);
          if (registing) {
              MiLinkLog.w(TAG, "mipush is already registing now ,cancel;");
              return;
          }
          registing = true;
          mHandler.postDelayed(new Runnable() {

              @Override
              public void run() {
                  registing = false;
              }
          }, 20 * 1000);
         
          if (TextUtils.isEmpty(mRegId)) {
              // 如果regid没注册，则注册
              String appId = Global.getClientAppInfo().getMiPushAppId();
              String appKey = Global.getClientAppInfo().getMiPushAppKey();
              if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                  MiLinkLog.w(TAG, "register mipush appid=" + appId + ",appkey=" + appKey);
                  // 保证一个机器只注册一个,防止多个用户登录一台机器，某个用户退出时，仍会收到push
                  // 只有在有别的用户登录时才反注册上一个用户的regid
//                  MiPushClient.unregisterPush(Global.getContext());
                  MiPushClient.registerPush(Global.getContext(), appId, appKey);
              }
              // 如果没有id，则不需要设置别名，注册结束
              if (TextUtils.isEmpty(userId)) {
                  registing = false;
              }
              return;
          }
          // 如果 mRegid不为空
          if (!TextUtils.isEmpty(userId)) {
              // 如果已经有别名，且一致。
              if (userId.equals(mAlias)) {
                  MiLinkLog.w(TAG, "mMiPush_RegAlias == mUserId,no need register");
              } else {
                  // 注册一个别名
                  bindAliasByUserId();
              }
          }
          registing = false;
    }
    
    public synchronized void bindAliasByUserId() {
        if (!TextUtils.isEmpty(this.userId)) {
            MiLinkLog.w(TAG, "set alias userId=" + userId);
            MiPushClient.setAlias(Global.getContext(), userId, null);
        }
        registing = false;
    }

    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        if (l != null) {
            tryWakeLock(500);
            l.onReceivePassThroughMessage(context, message);
        }
    }

    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        if (l != null) {
            tryWakeLock(500);
            l.onNotificationMessageClicked(context, message);
        }
    }

    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        if (l != null) {
            tryWakeLock(500);
            l.onNotificationMessageArrived(context, message);
        }
    }

    public void setMessageListener(MiPushMessageListener listener) {
        this.l = listener;
    }

    private Runnable mReleaseWakeLockRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                if (mWakeLock != null) {
                    MiLinkLog.w(TAG, "Wakelock RELEASED By MiPushManger");
                    mWakeLock.release();
                    mWakeLock = null;
                }
            } catch (Exception e) {
                mWakeLock = null;
            }
        }
    };

    private Runnable mAcquireWakeLockRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                Context context = Global.getApplicationContext();
                if (context != null && mWakeLock == null) {
                    MiLinkLog.w(TAG, "Wakelock ACQUIRED By MiPushManger");
                    PowerManager pm = (PowerManager) context.getApplicationContext()
                            .getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "formipush");
                    mWakeLock.acquire();
                }
            } catch (Exception e) {
                MiLinkLog.e(TAG, "acquireWakeLock exception", e);
            }
        }
    };

    /**
     * 在后台模式下，获取wakelock，保障请求能够发送出去
     */
    private void tryWakeLock(int wakeTime) {
        if (mHandler != null) {
            mHandler.removeCallbacks(mReleaseWakeLockRunnable);
            mHandler.post(mAcquireWakeLockRunnable);
            mHandler.postDelayed(mReleaseWakeLockRunnable, wakeTime);
        }
    }

    /**
     * 清除mipush通知栏消息，当消息为-1时为清除所有消息。
     * 
     * @param notifyId
     */
    public void clearNotification(int notifyId) {
        if (notifyId < 0) {
            MiPushClient.clearNotification(Global.getContext());
        } else {
            MiPushClient.clearNotification(Global.getContext(), notifyId);
        }
    }
}
