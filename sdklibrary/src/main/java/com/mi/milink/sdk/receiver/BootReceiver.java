
package com.mi.milink.sdk.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!MiAccountManager.getInstance().appHasLogined()) {
            return;
        }
        MiLinkLog.v(TAG, "boot broadcast, NetworkReceiver start milink service");
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra(Const.Extra.OnStartCommandReturn, 1);
        serviceIntent.setComponent(new ComponentName(context, Const.IPC.ServiceName));
        context.startService(serviceIntent);
    }

}
