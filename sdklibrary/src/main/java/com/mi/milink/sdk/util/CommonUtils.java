
package com.mi.milink.sdk.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;

import android.os.Parcel;
import android.text.format.Time;

public abstract class CommonUtils {

    /**
     * 空字符串常量 <br>
     * <br>
     * <i>佛曰：四大皆空</i>
     */
    public static final String EMPTY = "";

    /**
     * "不可用"字符串常量
     */
    public static final String NOT_AVALIBLE = "N/A";

    /**
     * 判断字符串是否为空内容/空指针
     * 
     * @param str 字符串
     * @return 是空内容/空指针，返回true，否则返回false
     */
    public static boolean isTextEmpty(String str) {
        return (str == null) || (str.length() < 1);
    }

    /**
     * 创建指定格式的时间格式化对象
     * 
     * @param pattern 时间格式，形如"yyyy-MM-dd HH-mm-ss.SSS"
     * @return Format 时间格式化对象
     */
    public static SimpleDateFormat createDataFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public static String join(Collection<?> collection, String separator) {
        if (collection == null) {
            return null;
        }
        Iterator<?> iterator = collection.iterator();
        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return first.toString();
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16,
                                                    // probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            buf.append(separator);
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    /**
     * 得到毫秒数对应的时间串, 格式 "2013-07-04 12:44:53.098" <br>
     * <br>
     * 这个方法使用Android的Time类实现，用以替代java.util.Calender<br>
     * 使用同余计算毫秒数，在某些ROM上，JDK被替换的算法不能计算毫秒数 (e.g. 华为荣耀)
     * 
     * @param time 毫秒数
     * @return 时间字符串
     */
    public static String getTimeStr(long time) {
        long ms = time % 1000;

        Time timeObj = new Time();

        timeObj.set(time);

        StringBuilder builder = new StringBuilder();

        builder.append(timeObj.format("%Y-%m-%d %H:%M:%S")).append('.');

        if (ms < 10) {
            builder.append("00");
        } else if (ms < 100) {
            builder.append('0');
        }

        builder.append(ms);

        return builder.toString();
    }

    /**
     * MD5加密
     */
    public static byte[] strToMd5(String str) {
        byte[] resu = null;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            md5.update(str.getBytes());

            resu = md5.digest();
        } catch (NoSuchAlgorithmException e) {
            resu = str.getBytes();
        }

        return resu;
    }

    /**
     * copy com.xiaomi.accountsdk.utils.CloudCoder 的hashDeviceInfo函数
     * 与miui.cloud.CloudManager.getHashedDeviceId使用相同的SHA1算法，确保与方流统计组使用同样算法。
     * 增强了异常情况的处理。 输入null时，返回""。 保证不会出现闪退。
     */
    public static String miuiSHA1(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] d = md.digest(plain.getBytes());
            return android.util.Base64.encodeToString(d, android.util.Base64.URL_SAFE).substring(0,
                    16);
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 向Parcel写入数组
     */
    public static void writeParcelBytes(Parcel parcel, byte[] data) {
        if (data == null) {
            parcel.writeInt(-1);
        } else {
            parcel.writeInt(data.length);
            parcel.writeByteArray(data);
        }
    }

    /**
     * 从Parcel中读取数组
     */
    public static byte[] readParcelBytes(Parcel parcel) {
        int len = parcel.readInt();

        if (len > -1) {
            byte[] resu = new byte[len];

            parcel.readByteArray(resu);

            return resu;
        } else {
            return null;
        }
    }

    /**
     * 便利方法：清空缓冲区
     * 
     * @param buffer
     */
    public static void zeroMemory(byte[] buffer) {
        if (buffer == null) {
            return;
        }

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0;
        }
    }

    /**
     * 便利方法：关闭文件或流数据对象
     * 
     * @param object 文件或流数据对象
     * @return 是否成功调用关闭方法
     */
    public static boolean closeDataObject(Object object) {
        if (object == null) {
            return false;
        }

        try {
            if (object instanceof InputStream) {
                ((InputStream) object).close();
            } else if (object instanceof OutputStream) {
                ((OutputStream) object).close();
            } else if (object instanceof Reader) {
                ((Reader) object).close();
            } else if (object instanceof Writer) {
                ((Writer) object).close();
            } else if (object instanceof RandomAccessFile) {
                ((RandomAccessFile) object).close();
            } else {
                return false;
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static final boolean isLegalIp(String ip) {
        if (ip == null) {
            return false;
        }
        return ip
                .matches("((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))");
    }

    public static final boolean isLegalPort(int port) {
        return port >= 0 && port < 65536;
    }
}
