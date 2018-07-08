
package com.mi.milink.sdk.session.common;

import com.mi.milink.sdk.debug.MiLinkLog;

public class BufferUtil {

    private static String TAG = "BufferUtil";

    public static boolean isHttpHead(byte[] head) {
        String temp = new String(head).substring(0, 4);
        String UpperCase = temp.toUpperCase();

        if (null != head && UpperCase.charAt(0) == 'H' && UpperCase.charAt(1) == 'T'
                && UpperCase.charAt(2) == 'T' && UpperCase.charAt(3) == 'P')
            return true;
        else
            return false;
    }

    // 判断是否为维纳斯头
    public static boolean isMNSHead(byte[] head) {
        if (null != head && head.length >= StreamUtil.MNS.length)
            return head[0] == StreamUtil.MNS[0] && head[1] == StreamUtil.MNS[1]
                    && head[2] == StreamUtil.MNS[2] && head[3] == StreamUtil.MNS[3];
        else
            return false;
    }

    public static boolean isEqualByte(byte a, byte b) {
        return (a == b)
                || (Character.isLetter((char) a) && Character.isLetter((char) a) && (Math
                        .abs(a - b) == ('a' - 'A')));
    }

    /**
     * 字节拷贝
     */
    public static void copy(byte[] dest, int destOffset, byte[] src, int srcOffset, int thelength) {
        for (int i = 0; i < thelength; i++) {
            dest[destOffset++] = src[srcOffset + i];
        }
    }

    // 查找子串
    public static int findFormByte(byte[] bOrg, byte[] bSerach) {
        MiLinkLog.d(TAG, "findFormByte recvData");

        for (int i = bSerach.length - 1; i < bOrg.length; i++) {
            boolean result = true;
            int bSerachSize = bSerach.length;
            for (int j = 0; j < bSerachSize; j++) {
                if (!BufferUtil.isEqualByte(bOrg[i - bSerachSize + j + 1], bSerach[j])) {
                    result = false;
                }
            }
            if (result == true) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 使用KMP算法实现与findFormByte()同样的功能
     *
     * @param bOrg
     * @param bSearch
     * @return
     */
    public static int findFromByteKMP(byte[] bOrg, byte[] bSearch) {
        int i = getIndexKMP(bOrg, bSearch);
        if (i != -1) {
            i += bSearch.length;
        }
        return i;
    }

    /**
     * KMP算法实现查找字节数组
     *
     * @param s 目标串
     * @param b 要查找的串
     * @return 返回s在b中的开始下标，如果不存在返回-1
     * @author chadguo
     */
    public static int getIndexKMP(byte[] s, byte[] b) {
        int lenS = s.length;
        int lenB = b.length;
        if (lenS < lenB) {
            return -1;
        }

        int[] next = getKMPNext(b);
        if (next == null) {
            return -1;
        }

        int i = 0, j = 0;
        while (i < lenS && j < lenB) {
            if (BufferUtil.isEqualByte(s[i], b[j])) {
                i++;
                j++;
            } else if (j == 0) {
                i++;
            } else {
                j = next[j - 1] + 1;
            }
        }

        if (j == lenB) {
            return i - j;
        } else {
            return -1;
        }

    }

    private static int[] getKMPNext(byte[] b) {
        int len = b.length;
        if (len == 0) {
            return null;
        }
        int[] next = new int[len];
        next[0] = -1;
        int index;
        for (int i = 1; i < len; i++) {
            index = next[i - 1];
            while (index >= 0 && !BufferUtil.isEqualByte(b[i], b[index + 1])) {
                index = next[index];
            }
            if (BufferUtil.isEqualByte(b[i], b[index + 1])) {
                next[i] = index + 1;
            } else {
                next[i] = -1;
            }
        }
        return next;
    }

    public static int findContentLengthFromByte(byte[] bOrg) {
        byte[] content = new byte["Content-Length:".length()];
        BufferUtil.copy(content, 0, "Content-Length:".getBytes(), 0, "Content-Length:".length());
        return findFormByte(bOrg, content);
    }

    public static int findMNSHeaderFromByte(byte[] bOrg) {
        byte[] bSerach = new byte[4];
        bSerach[0] = StreamUtil.MNS[0];
        bSerach[1] = StreamUtil.MNS[1];
        bSerach[2] = StreamUtil.MNS[2];
        bSerach[3] = StreamUtil.MNS[3];
        return findFormByte(bOrg, bSerach);
    }

    public static int findHttpHeaderEndFromByte(byte[] bOrg) {
        byte[] bSerach = new byte[4];
        bSerach[0] = '\r';
        bSerach[1] = '\n';
        bSerach[2] = '\r';
        bSerach[3] = '\n';
        return findFormByte(bOrg, bSerach);
    }

}
