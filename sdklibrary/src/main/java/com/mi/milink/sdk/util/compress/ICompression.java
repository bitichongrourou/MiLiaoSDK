package com.mi.milink.sdk.util.compress;

/**
 * 
 * @author MK
 *
 */
public interface ICompression {
    byte[] compress(byte[] data);

    byte[] decompress(byte[] data);
}
