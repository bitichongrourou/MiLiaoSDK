
package com.mi.milink.sdk.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Build;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * 清除安卓预加载的资源，减少milink进程的内存消耗
 * 
 * @author MK
 */
public class PreloadClearUtil {

    public static void clearResources() {
        try {
            MiLinkLog.v("PreloadClearUtil", "clear resource");
            clearPreloadedDrawables();
            clearPreloadedColorDrawables();
            clearPreloadedColorStateLists();
        } catch (Exception e) {
            MiLinkLog.e("PreloadClearUtil", e);
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NewApi")
    private static void clearPreloadedDrawables() {
        try {
            Field mFieldPreloadedDrawables = getField(Resources.class, "sPreloadedDrawables");

            if (mFieldPreloadedDrawables != null) {
                mFieldPreloadedDrawables.setAccessible(true);

                if (Build.VERSION.SDK_INT <= 17) {
                    LongSparseArray<ConstantState> dArray = (LongSparseArray<ConstantState>) mFieldPreloadedDrawables
                            .get(null);

                    if (dArray != null) {
                        clearLongSparseArray(dArray);
                    }
                } else if (Build.VERSION.SDK_INT >= 18) {
                    LongSparseArray<ConstantState>[] dArray = (LongSparseArray<ConstantState>[]) mFieldPreloadedDrawables
                            .get(null);

                    if (dArray != null) {
                        for (int i = 0; i < dArray.length; i++) {
                            clearLongSparseArray(dArray[i]);
                        }
                    }
                }
            }

        } catch (Exception e) {
            MiLinkLog.e("PreloadClearUtil", e);
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NewApi")
    private static void clearPreloadedColorDrawables() {
        try {
            Field mFieldPreloadedColorDrawables = getField(Resources.class,
                    "sPreloadedColorDrawables");

            if (mFieldPreloadedColorDrawables != null) {
                mFieldPreloadedColorDrawables.setAccessible(true);
                LongSparseArray<ConstantState> sPreloadedColorDrawables = (LongSparseArray<ConstantState>) mFieldPreloadedColorDrawables
                        .get(null);

                if (sPreloadedColorDrawables != null) {
                    clearLongSparseArray(sPreloadedColorDrawables);
                }
            }
        } catch (Exception e) {
            MiLinkLog.e("PreloadClearUtil", e);
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("NewApi")
    private static void clearPreloadedColorStateLists() {
        try {
            Field mFieldPreloadedColorStateLists = getField(Resources.class,
                    "sPreloadedColorStateLists");

            if (mFieldPreloadedColorStateLists == null) {
                mFieldPreloadedColorStateLists = getField(Resources.class,
                        "mPreloadedColorStateLists");
            }

            if (mFieldPreloadedColorStateLists != null) {
                mFieldPreloadedColorStateLists.setAccessible(true);

                if (Build.VERSION.SDK_INT <= 15) {
                    SparseArray<ColorStateList> sPreloadedColorStateLists = (SparseArray<ColorStateList>) mFieldPreloadedColorStateLists
                            .get(null);
                    if (sPreloadedColorStateLists != null) {
                        clearSparseArray(sPreloadedColorStateLists);
                    }
                } else {
                    LongSparseArray<ColorStateList> sPreloadedColorStateLists = (LongSparseArray<ColorStateList>) mFieldPreloadedColorStateLists
                            .get(null);
                    if (sPreloadedColorStateLists != null) {
                        clearColorStateListArray(sPreloadedColorStateLists);
                    }
                }
            }
        } catch (Exception e) {
            MiLinkLog.e("PreloadClearUtil", e);
        }
    }

    private static Field getField(Class<?> thisClass, String fieldName) {
        Field field = null;

        if (!TextUtils.isEmpty(fieldName)) {
            try {
                field = thisClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                MiLinkLog.e("PreloadClearUtil", e);
            }
        }

        return field;
    }

    private static void clearColorStateListArray(LongSparseArray<ColorStateList> spArray) {
        if (spArray != null) {
            int size = spArray.size();
            for (int i = 0; i < size; i++) {
                spArray.setValueAt(i, null);
            }
        }
    }

    private static void clearLongSparseArray(LongSparseArray<ConstantState> spArray) {
        if (spArray != null) {
            int size = spArray.size();
            for (int i = 0; i < size; i++) {
                ConstantState state = spArray.valueAt(i);
                if (state != null) {
                    Drawable drawable = state.newDrawable();
                    if (drawable != null && drawable instanceof BitmapDrawable) {
                        BitmapDrawable bmDrawable = (BitmapDrawable) drawable;
                        Bitmap bitmap = bmDrawable.getBitmap();
                        if (bitmap != null) {
                            bitmap.recycle();
                            setBitmap(bmDrawable, null);
                        }
                    }
                }
            }
        }
    }

    private static void setBitmap(BitmapDrawable drawable, Bitmap bitmap) {
        try {
            Method method = getMethod(BitmapDrawable.class, "setBitmap", new Class[] {
                Bitmap.class
            });
            if (method != null) {
                method.setAccessible(true);
                method.invoke(drawable, bitmap);
            }
        } catch (Exception e) {
            MiLinkLog.e("PreloadClearUtil", e);
        }
    }

    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    private static Method getMethod(Class clazz, String methodName, final Class[] classes)
            throws Exception {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, classes);
        } catch (NoSuchMethodException e) {
            try {
                method = clazz.getMethod(methodName, classes);
            } catch (NoSuchMethodException ex) {
                if (clazz.getSuperclass() == null) {
                    return method;
                } else {
                    method = getMethod(clazz.getSuperclass(), methodName, classes);
                }
            }
        }
        return method;
    }

    private static void clearSparseArray(SparseArray<?> spArray) {
        if (spArray != null) {
            int size = spArray.size();
            for (int i = 0; i < size; i++) {
                spArray.setValueAt(i, null);
            }
        }
    }
}
