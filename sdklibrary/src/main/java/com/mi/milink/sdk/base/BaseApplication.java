package com.mi.milink.sdk.base;

import android.app.Application;

import com.mi.milink.sdk.data.ClientAppInfo;

/**
 * 基础的application，也可以不继承这个，只要在app的application的oncreate中加Global.init
 *
 * @author MK
 *
 */
public abstract class BaseApplication extends Application {

	public abstract ClientAppInfo getClientAppInfo();

	@Override
	public void onCreate() {
		super.onCreate();
		Global.init(this, getClientAppInfo());
	}

}
