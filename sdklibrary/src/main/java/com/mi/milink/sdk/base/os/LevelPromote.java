
package com.mi.milink.sdk.base.os;

import java.lang.reflect.Method;

public class LevelPromote {

    public static void promoteApplicationLevelInMIUI() {
        try {
            Class<?> onwClass = Class.forName("com.miui.whetstone.WhetstoneActivityManager");
            Method m = onwClass.getDeclaredMethod("promoteApplicationLevel", int.class);
            m.invoke(null, 2);
        } catch (Exception e) {
        }
    }
}
