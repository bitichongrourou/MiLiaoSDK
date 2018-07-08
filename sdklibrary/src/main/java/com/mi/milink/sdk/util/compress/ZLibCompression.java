package com.mi.milink.sdk.util.compress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.mi.milink.sdk.debug.MiLinkLog;


public class ZLibCompression implements ICompression {
    private final static String TAG = ZLibCompression.class.getName();

    @Override
    public byte[] compress(byte[] data) {
        if (null == data)
            return null;

        byte[] output = new byte[0];

        Deflater compresser = new Deflater();

        compresser.reset();
        compresser.setInput(data);
        compresser.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!compresser.finished()) {
                int i = compresser.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = null;
            MiLinkLog.e(TAG, "compress fail", e);
        } catch (OutOfMemoryError e) {
            output = null;
            MiLinkLog.e(TAG, "compress out of memory", e);
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                MiLinkLog.e(TAG, "close fail", e);
            }
        }
        compresser.end();
        return output;
    }

    @Override
    public byte[] decompress(byte[] data) {
        if (null == data)
            return null;

        byte[] output = new byte[0];

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = null;
            MiLinkLog.e(TAG, "decompress fail", e);
        } catch (OutOfMemoryError e) {
            output = null;
            MiLinkLog.e(TAG, "decompress out of memory", e);
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                MiLinkLog.e(TAG, "close fail", e);
            }
        }

        decompresser.end();
        return output;
    }
}
