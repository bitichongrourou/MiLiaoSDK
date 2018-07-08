package com.mi.milink.sdk.util.compress;


public class CompressionFactory {
    
    public static final int METHOD_NONE = 0;
    public static final int METHOD_ZLIB = 1;

    private static ZLibCompression sZLibCompression = new ZLibCompression();

    private static NoCompression sNoCompression = new NoCompression();

    public static ICompression createCompression(int method) {
        switch (method) {
            case METHOD_ZLIB:
                return sZLibCompression;
            case METHOD_NONE:
                return sNoCompression;
            default:
                return sNoCompression;
        }
    }

}
