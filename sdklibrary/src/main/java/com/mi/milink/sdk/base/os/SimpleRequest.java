
package com.mi.milink.sdk.base.os;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;

import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.util.compress.CompressionFactory;

public final class SimpleRequest {

    private static final String TAG = "SimpleRequest";

    private static final String ENC = "utf-8";

    private static final int TIMEOUT = 20 * 1000; // 20s

    protected static String appendUrl(String origin, List<NameValuePair> nameValuePairs) {
        if (origin == null) {
            throw new NullPointerException("origin is not allowed null");
        }
        StringBuilder urlBuilder = new StringBuilder(origin);
        if (nameValuePairs != null) {
            final String paramPart = URLEncodedUtils.format(nameValuePairs, ENC);
            if (paramPart != null && paramPart.length() > 0) {
                if (origin.contains("?")) {
                    urlBuilder.append("&");
                } else {
                    urlBuilder.append("?");
                }
                urlBuilder.append(paramPart);
            }
        }
        return urlBuilder.toString();
    }

    public static List<NameValuePair> mapToPairs(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            BasicNameValuePair pair = new BasicNameValuePair(key, value != null ? value : "");
            pairs.add(pair);
        }
        return pairs;
    }

    public static Map<String, String> listToMap(Map<String, List<String>> listMap) {
        Map<String, String> map = new HashMap<String, String>();
        if (listMap != null) {
            Set<Map.Entry<String, List<String>>> entries = listMap.entrySet();
            for (Map.Entry<String, List<String>> entry : entries) {
                final String key = entry.getKey();
                final List<String> valueList = entry.getValue();
                if (key != null && valueList != null && valueList.size() > 0) {
                    map.put(key, valueList.get(0));
                }
            }
        }
        return map;
    }
    
    public static StringContent postAsString(String url, Map<String, String> params,
            Map<String, String> cookies, boolean readBody, LengthPair lengthPair) throws IOException {
        return postAsString(url, params, cookies, readBody, null, lengthPair);
    }

    public static StringContent postAsStringByHttps(String url, Map<String, String> params,
            Map<String, String> cookies, boolean readBody, String host,
            LengthPair lengthPair) throws IOException {
    	
        HttpURLConnection conn = makeConn(url, cookies);
        if (conn == null) {
            return null;
        }
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            if(!TextUtils.isEmpty(host)) {
                conn.addRequestProperty("Host", host);
            }
            conn.addRequestProperty("Content-Encoding", "gzip");
            conn.connect();

            List<NameValuePair> nameValuePairs = mapToPairs(params);
            if (nameValuePairs != null) {
                String content = URLEncodedUtils.format(nameValuePairs, ENC);
                OutputStream os = conn.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                byte[] originByteArray = content.getBytes(ENC);
                MiLinkLog.v(TAG, "originByteArray.length=" + originByteArray.length);
                byte[] compressByteArray = CompressionFactory.createCompression(
                        CompressionFactory.METHOD_ZLIB).compress(originByteArray);
                MiLinkLog.v(TAG, "compressByteArray.length=" + compressByteArray.length);
                try {
                    bos.write(compressByteArray);
                    if(lengthPair != null) {
                        lengthPair.originLength = originByteArray.length;
                        lengthPair.compressLength = compressByteArray.length;
                    }
                } catch (Exception e) {
                    MiLinkLog.e(TAG, e);
                } finally {
                    try {
                        bos.flush();
                        bos.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            int code = conn.getResponseCode();
            MiLinkLog.v(TAG, "getResponseCode=" + code);
            if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                final Map<String, List<String>> headerFields = conn.getHeaderFields();
                final CookieManager cm = new CookieManager();
                // final URI reqUri = URI.create(url);
                URL reqUrl = new URL(url);
                final URI reqUri = new URI(reqUrl.getProtocol(), reqUrl.getHost(),
                        reqUrl.getPath(), reqUrl.getQuery(), null);
                cm.put(reqUri, headerFields);
                StringBuilder sb = new StringBuilder();
                if (readBody) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()), 1024);
                    try {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    } catch (IOException e) {
                        MiLinkLog.e(TAG, e);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
                final StringContent stringContent = new StringContent(sb.toString());
                Map<String, String> cookieMap = parseCookies(cm.getCookieStore().get(reqUri));
                stringContent.putCookies(cookieMap);
                cookieMap.putAll(listToMap(headerFields));
                stringContent.putHeaders(cookieMap);
                return stringContent;
            } else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
            } else {
            }
        } catch (ProtocolException e) {
            throw new IOException("protocol error");
        } catch (URISyntaxException e) {

        } finally {
            conn.disconnect();
        }
        return null;
    }
    
    public static StringContent postAsString(String url, Map<String, String> params,
            Map<String, String> cookies, boolean readBody, String host,
            LengthPair lengthPair) throws IOException {
        HttpURLConnection conn = makeConn(url, cookies);
        if (conn == null) {
            return null;
        }
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            if(!TextUtils.isEmpty(host)) {
                conn.addRequestProperty("Host", host);
            }
            conn.addRequestProperty("Content-Encoding", "gzip");
            conn.connect();

            List<NameValuePair> nameValuePairs = mapToPairs(params);
            if (nameValuePairs != null) {
                String content = URLEncodedUtils.format(nameValuePairs, ENC);
                OutputStream os = conn.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                byte[] originByteArray = content.getBytes(ENC);
                MiLinkLog.v(TAG, "originByteArray.length=" + originByteArray.length);
                byte[] compressByteArray = CompressionFactory.createCompression(
                        CompressionFactory.METHOD_ZLIB).compress(originByteArray);
                MiLinkLog.v(TAG, "compressByteArray.length=" + compressByteArray.length);
                try {
                    bos.write(compressByteArray);
                    if(lengthPair != null) {
                        lengthPair.originLength = originByteArray.length;
                        lengthPair.compressLength = compressByteArray.length;
                    }
                } catch (Exception e) {
                    MiLinkLog.e(TAG, e);
                } finally {
                    try {
                        bos.flush();
                        bos.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            int code = conn.getResponseCode();
            MiLinkLog.v(TAG, "getResponseCode=" + code);
            if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                final Map<String, List<String>> headerFields = conn.getHeaderFields();
                final CookieManager cm = new CookieManager();
                // final URI reqUri = URI.create(url);
                URL reqUrl = new URL(url);
                final URI reqUri = new URI(reqUrl.getProtocol(), reqUrl.getHost(),
                        reqUrl.getPath(), reqUrl.getQuery(), null);
                cm.put(reqUri, headerFields);
                StringBuilder sb = new StringBuilder();
                if (readBody) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream()), 1024);
                    try {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    } catch (IOException e) {
                        MiLinkLog.e(TAG, e);
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
                final StringContent stringContent = new StringContent(sb.toString());
                Map<String, String> cookieMap = parseCookies(cm.getCookieStore().get(reqUri));
                stringContent.putCookies(cookieMap);
                cookieMap.putAll(listToMap(headerFields));
                stringContent.putHeaders(cookieMap);
                return stringContent;
            } else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
            } else {
            }
        } catch (ProtocolException e) {
            throw new IOException("protocol error");
        } catch (URISyntaxException e) {

        } finally {
            conn.disconnect();
        }
        return null;
    }

    protected static HttpURLConnection makeConn(String url, Map<String, String> cookies) {
        URL req = null;
        try {
            req = new URL(url);
        } catch (MalformedURLException e) {
            MiLinkLog.e(TAG, e);
        }
        if (req == null) {
            return null;
        }
        try {
        	HttpURLConnection conn;
//        	if(url.startsWith("https")){
//        		 // 创建SSLContext对象，并使用我们指定的信任管理器初始化
//                TrustManager[] tm = { new MyX509TrustManager() };
//                SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
//                sslContext.init(null, tm, new java.security.SecureRandom());
//                // 从上述SSLContext对象中得到SSLSocketFactory对象
//                SSLSocketFactory ssf = sslContext.getSocketFactory();
//                // 创建URL对象
//                // 创建HttpsURLConnection对象，并设置其SSLSocketFactory对象
//                conn = (HttpsURLConnection) req.openConnection();
//                ((HttpsURLConnection)conn).setSSLSocketFactory(ssf);
//        	}else{
        		conn = (HttpURLConnection) req.openConnection();	
//        	}
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(TIMEOUT);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (cookies != null) {
                conn.setRequestProperty("Cookie", joinMap(cookies, "; "));
            }
            return conn;
        } catch (Exception e) {
            MiLinkLog.e(TAG, e);
        }
        return null;
    }

    public static String joinMap(Map<String, String> map, String sp) {
        if (map == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<String, String>> entries = map.entrySet();
        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            if (i > 0) {
                sb.append(sp);
            }
            final String key = entry.getKey();
            final String value = entry.getValue();
            sb.append(key);
            sb.append("=");
            sb.append(value);
            i++;
        }
        return sb.toString();
    }

    protected static Map<String, String> parseCookies(List<HttpCookie> cookies) {
        Map<String, String> cookieMap = new HashMap<String, String>();
        for (HttpCookie cookie : cookies) {
            if (!cookie.hasExpired()) {
                final String name = cookie.getName();
                final String value = cookie.getValue();
                if (name != null) {
                    cookieMap.put(name, value);
                }
            } else {
            }
        }
        return cookieMap;
    }
    
    public static class LengthPair{
        public int originLength;
        public int compressLength;
    }

    public static class HeaderContent {

        private final Map<String, String> headers = new HashMap<String, String>();

        private final Map<String, String> cookies = new HashMap<String, String>();

        public void putHeader(String key, String value) {
            headers.put(key, value);
        }

        public String getHeader(String key) {
            return headers.get(key);
        }

        public void putCookie(String key, String value) {
            cookies.put(key, value);
        }

        public String getCookie(String key) {
            return cookies.get(key);
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void putHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public void putCookies(Map<String, String> cookies) {
            this.cookies.putAll(cookies);
        }

        @Override
        public String toString() {
            return "HeaderContent{" + "headers=" + headers + '}';
        }
    }

    public static class StringContent extends HeaderContent {

        private String body;

        public StringContent(String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "StringContent{" + "body='" + body + '\'' + '}';
        }
    }

    public static class MapContent extends HeaderContent {

        private Map<String, Object> bodies;

        public MapContent(Map<String, Object> bodies) {
            this.bodies = bodies;
        }

        public Object getFromBody(String key) {
            return bodies.get(key);
        }

        @Override
        public String toString() {
            return "MapContent{" + "bodies=" + bodies + '}';
        }
    }

    public static class StreamContent extends HeaderContent {

        private InputStream stream;

        public StreamContent(InputStream stream) {
            this.stream = stream;
        }

        public InputStream getStream() {
            return stream;
        }

    }

}
