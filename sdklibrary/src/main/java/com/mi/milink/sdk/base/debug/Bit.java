
package com.mi.milink.sdk.base.debug;

/**
 * 位运算
 * 
 * @author MK
 */
public final class Bit {
    public static final int add(int source, int sub) {
        return source | sub;
    }

    public static final boolean has(int source, int sub) {
        return sub == (source & sub);
    }

    public static final int remove(int source, int sub) {
        return source ^ (source & sub);
    }

    public static final int log2(int source) {
        return (int) (Math.log(source) / Math.log(2));
    }
}
