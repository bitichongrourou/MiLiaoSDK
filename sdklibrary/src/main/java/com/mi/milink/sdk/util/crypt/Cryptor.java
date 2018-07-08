package com.mi.milink.sdk.util.crypt;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

import com.mi.milink.sdk.debug.MiLinkLog;

public abstract class Cryptor {

    private static final byte IV[] = {
            0x64, 0x17, 0x54, 0x72, 0x48, 0x00, 0x4, 0x61, 0x49, 0x61, 0x2, 0x34, 0x54, 0x66, 0x12,
            0x20
    };

    /**
     * aes文件解密
     *
     * @param src
     * @return
     */
    public static byte[] decrypt(byte[] src, byte[] key) {
//        MnsLog.v("crypt", "key is :" + bytesToHex(key));
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] decodedBytes = cipher.update(src, 0, src.length);
            byte[] lastBytes = cipher.doFinal();
            int len = (decodedBytes == null ? 0 : decodedBytes.length)
                    + (lastBytes == null ? 0 : lastBytes.length);
            if (len <= 0)
                return null;
            byte[] resultBytes = new byte[len];
            int filled = 0;
            if (decodedBytes != null) {
                System.arraycopy(decodedBytes, 0, resultBytes, 0, decodedBytes.length);
                filled = decodedBytes.length;
            }
            if (lastBytes != null) {
                System.arraycopy(lastBytes, 0, resultBytes, filled, lastBytes.length);
            }
            return resultBytes;
        } catch (Exception ex) {
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] mChars = "0123456789ABCDEF".toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(mChars[(bytes[i] & 0xFF) >> 4]);
            sb.append(mChars[bytes[i] & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * aes文件加密
     *
     * @param src
     * @return
     */
    public static byte[] encrypt(byte[] src, byte[] key) {
//        MnsLog.v("crypt", "key is :" + bytesToHex(key));
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "算法/模式/补码方式"
            IvParameterSpec iv = new IvParameterSpec(IV);// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(src);
            return encrypted;
        } catch (Exception ex) {
        }
        return null;
    }
    
    private static PublicKey getPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.decode(keyStr, Base64.NO_WRAP);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    private static final int RSA_1024_ENCYPT_LEN = 117;// 1024位rsa可加密的最大字节长度。
    
    public static byte[] encryptRSA(byte[] src, String publicKeyString) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");// "算法/模式/补码方式"
            // 这里一定是1024位的rsa公钥，服务端保证的。
            PublicKey skeySpec = getPublicKey(publicKeyString);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            int srcLen = src.length;
            int srcOffset = 0;
            int encyptLen = RSA_1024_ENCYPT_LEN;
            int outputSize = 0;
            byte[] buffer = new byte[2 * 1024];
            do {
                // 如果末尾不足RSA_1024_ENCYPT_LEN。
                if (srcOffset + RSA_1024_ENCYPT_LEN > srcLen) {
                    encyptLen = srcLen - srcOffset;
                }
                byte[] temp = cipher.doFinal(src, srcOffset, encyptLen);// 单次加密117字节，输出128字节。
                int tempLen = temp.length;
                // 如果buffer不够大，将容量翻倍。
                if (outputSize + tempLen > buffer.length) {
                    byte[] newBuffer = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, newBuffer, 0, outputSize);
                    buffer = newBuffer;
                }
                System.arraycopy(temp, 0, buffer, outputSize, tempLen);
                srcOffset += encyptLen;
                outputSize += tempLen;
            } while (srcOffset < srcLen);
            byte[] enBytes = new byte[outputSize];
            System.arraycopy(buffer, 0, enBytes, 0, outputSize);
            return enBytes;
        } catch (Exception ex) {
            MiLinkLog.e("crypt", ex);
        }
        return null;
    }
}
