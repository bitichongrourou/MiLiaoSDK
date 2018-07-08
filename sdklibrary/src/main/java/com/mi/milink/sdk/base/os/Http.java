package com.mi.milink.sdk.base.os;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 简单的HTTP协议封装
 *
 */
public class Http {
	public static final int HTTP_SUCCESS = 200;

	public static final int HTTP_REDIRECT = 300;

	public static final int HTTP_CLIENT_ERROR = 400;

	public static final int HTTP_SERVER_ERROR = 500;

	public static final int HTTP_CONNECT_ERROR = 1024;

	public static final int HTTP_URL_NOT_AVALIBLE = 2048;

	public static final int HTTP_CODE_ERROR = 4096;

	public static final int DEFAULT_READ_TIMEOUT = 60 * 1000;

	public static final int DEFAULT_CONNECT_TIMEOUT = 60 * 1000;

	public static final String GET = "GET";

	public static final String POST = "POST";

	public static final String GZIP = "gzip";

	public static final String PROTOCOL_PREFIX = "http://";

	public static final int PROTOCOL_PREFIX_LENGTH = PROTOCOL_PREFIX.length();

	public static final char PROTOCOL_PORT_SPLITTER = ':';

	public static final char PROTOCOL_HOST_SPLITTER = '/';

	public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

	public static final String HEADER_X_ONLINE_HOST = "X-Online-Host";

	public static final String HEADER_HOST = "Host";

	/**
	 * HTTP代理模式
	 *
	 * @author lewistian
	 */
	public static enum HttpProxyMode {
		NeverTry, Direct, ViaProxy
	}

	/**
	 * HTTP代理
	 *
	 * @author lewistian
	 */
	public static abstract class HttpProxy {
		public abstract String getHost();

		public abstract int getPort();

		@Override
		public String toString() {
			return getHost() + PROTOCOL_PORT_SPLITTER + getPort();
		}

		public static HttpProxy Default = new HttpProxy() {

			@SuppressWarnings("deprecation")
			@Override
			public int getPort() {
				return android.net.Proxy.getDefaultPort();
			}

			@SuppressWarnings("deprecation")
			@Override
			public String getHost() {
				return android.net.Proxy.getDefaultHost();
			}
		};
	}

	public static boolean isSuccess(int responseCode) {
		return (responseCode >= HTTP_SUCCESS)
				&& (responseCode < HTTP_SUCCESS + 99);
	}

	/**
	 * 分离URL中的主机和路径
	 *
	 * @param origUrl
	 *            　原URL
	 * @return String[2] : <br>
	 *         [0]=>主机名<br>
	 *         [1]=>路径
	 */
	public static String[] splitUrl(String origUrl) {
		String[] result = new String[2];

		if (origUrl == null || origUrl.length() < PROTOCOL_PREFIX_LENGTH) {
			return result;
		}

		origUrl = origUrl.toLowerCase().startsWith(PROTOCOL_PREFIX) ? origUrl
				: PROTOCOL_PREFIX.concat(origUrl);

		int index = origUrl.indexOf(PROTOCOL_HOST_SPLITTER,
				PROTOCOL_PREFIX_LENGTH);

		if (index <= PROTOCOL_PREFIX_LENGTH) {
			index = origUrl.length();
		}

		// orig => "http://www.qq.com///.cgi"
		// orig => "www.qq.com///.cgi"
		// orig => "HTTP://www.qq.com///.cgi"
		// ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
		// host => www.qq.com
		result[0] = origUrl.substring(PROTOCOL_PREFIX_LENGTH, index);

		// file => ///.cgi
		if (index < origUrl.length()) {
			result[1] = origUrl.substring(index, origUrl.length());
		} else {
			result[1] = "";
		}

		return result;
	}

	public static int doRequest(String url, String method, String params,
			boolean gzip, HttpProxy proxy) {
		return doRequest(url, method, params, gzip, proxy,
				DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}

	public static int doRequest(String url, String method, byte[] data,
			boolean gzip, HttpProxy proxy) {
		return doRequest(url, method, data, gzip, proxy,
				DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
	}

	public static int doRequest(String url, String method, String params,
			boolean gzip, HttpProxy proxy, int connTimeout, int readTimeout) {
		byte[] data = (params == null) ? null : params.getBytes();

		return doRequest(url, method, data, gzip, proxy, connTimeout,
				readTimeout);
	}

	public static int doRequest(String url, String method, byte[] data,
			boolean gzip, HttpProxy proxy, int connTimeout, int readTimeout) {
		return doRequest(url, method, data, gzip, proxy, connTimeout,
				readTimeout, null);
	}

	public static int doRequest(String url, String method, String params,
			boolean gzip, HttpProxy proxy, int connTimeout, int readTimeout,
			String redirectHost) {
		byte[] data = (params == null) ? null : params.getBytes();

		return doRequest(url, method, data, gzip, proxy, connTimeout,
				readTimeout, redirectHost);
	}

	/**
	 * 发送Http请求
	 *
	 * @param url
	 *            URL
	 * @param method
	 *            谓词，GET/POST
	 * @param data
	 *            数据
	 * @param gzip
	 *            是否压缩
	 * @param proxy
	 *            代理
	 * @param connTimeout
	 *            连接超时
	 * @param readTimeout
	 *            读取超时
	 * @param redirectHost
	 *            需要HOST转发
	 * @return HTTP错误码
	 */
	public static int doRequest(String url, String method, byte[] data,
			boolean gzip, HttpProxy proxy, int connTimeout, int readTimeout,
			String redirectHost) {

		HttpURLConnection connection = null;
		URL urlObj = null;

		int responseCode = HTTP_SUCCESS;

		try {
			String finalUrl = url;
			String[] urlParts = null;

			// 是否使用代理<1>: 分离出主机名称和路径，组装新的URL
			if (proxy != null) {
				urlParts = splitUrl(url);

				finalUrl = proxy.toString() + urlParts[1];
			}

			finalUrl = finalUrl.toLowerCase().startsWith(PROTOCOL_PREFIX) ? finalUrl
					: PROTOCOL_PREFIX.concat(finalUrl);

			urlObj = new URL(finalUrl);

			connection = (HttpURLConnection) urlObj.openConnection();

			connection.setReadTimeout(readTimeout);
			connection.setConnectTimeout(connTimeout);
			// 拒绝使用缓存，始终从服务器获取数据。
			// 这通常会对拥有缓存的applet等JAVA程序带来获取图片等数据造成影响，造成跳过网络请求。
			// 为了避免潜在的风险，加上这个设定。
			connection.setUseCaches(false);

			// 发送HTTP请求
			{
				connection.setRequestMethod(method);
				connection.setDoInput(true);

				// 需要Host转发
				if (redirectHost != null && redirectHost.length() > 0) {
					connection.setRequestProperty(HEADER_HOST, redirectHost);
				}

				// 是否进行GZip压缩<1>: 填写REQUEST HEADER
				if (gzip) {
					connection
							.setRequestProperty(HEADER_CONTENT_ENCODING, GZIP);
				}

				// 是否使用代理<2>: 填写REQUEST HEADER
				if (proxy != null) {
					connection.setRequestProperty(HEADER_X_ONLINE_HOST,
							urlParts[0]);
				}

				if (data != null) {
					connection.setDoOutput(true);
					OutputStream paramsStream = connection.getOutputStream();
					{
						// 是否进行GZip压缩<1>: 压缩输出流
						if (gzip) {
							GZIPOutputStream gzipStream = new GZIPOutputStream(
									paramsStream);

							gzipStream.write(data);
							gzipStream.flush();
							gzipStream.close();
						} else {
							paramsStream.write(data);
							paramsStream.flush();
							paramsStream.close();
						}
					}
				} else {
					connection.setDoOutput(false);
				}
			}

			responseCode = connection.getResponseCode();

			// InputStream responseStream = connection.getInputStream();
			//
			// if (responseStream != null)
			// {
			// byte[] data2 = new byte[10 * 1024];
			//
			// int len = responseStream.read(data2, 0, 10 * 1024);
			//
			// System.out.println(new String(data2));
			// }
		} catch (MalformedURLException e) {
			responseCode = HTTP_URL_NOT_AVALIBLE;
		} catch (IOException e) {
			responseCode = HTTP_CONNECT_ERROR;
		} catch (Exception e) {
			responseCode = HTTP_CODE_ERROR;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return responseCode;
	}

	public static int download(final String url, final File destFile) {
		int responseCode = HTTP_CLIENT_ERROR;

		HttpURLConnection connection = null;
		URL urlObj = null;

		BufferedOutputStream bos = null;
		InputStream is = null;

		HttpProxy proxy = NetworkDash.isWap() ? HttpProxy.Default : null;

		try {
			String finalUrl = url;
			String[] urlParts = null;

			// 是否使用代理<1>: 分离出主机名称和路径，组装新的URL
			if (proxy != null) {
				urlParts = splitUrl(url);

				finalUrl = proxy.toString() + urlParts[1];
			}

			finalUrl = finalUrl.toLowerCase().startsWith(PROTOCOL_PREFIX) ? finalUrl
					: PROTOCOL_PREFIX.concat(finalUrl);

			urlObj = new URL(finalUrl);

			connection = (HttpURLConnection) urlObj.openConnection();

			connection.setReadTimeout(30 * 1000);
			connection.setConnectTimeout(15 * 1000);

			connection.setRequestMethod(GET);
			connection.setDoInput(true);

			// 是否使用代理<2>: 填写REQUEST HEADER
			if (proxy != null) {
				connection
						.setRequestProperty(HEADER_X_ONLINE_HOST, urlParts[0]);
			}

			responseCode = connection.getResponseCode();

			if (isSuccess(responseCode)) {
				bos = new BufferedOutputStream(new FileOutputStream(destFile,
						true));

				is = connection.getInputStream();

				int readLen = 0;

				byte[] buffer = new byte[8 * 1024];

				while (-1 != (readLen = is.read(buffer, 0, buffer.length))) {
					bos.write(buffer, 0, readLen);
				}

				bos.flush();
				bos.close();
			}
		} catch (IOException e) {
//			MiLinkTracer.autoTrace(TraceLevel.ERROR, "Http", "Download Failed", e);

			responseCode = HTTP_CONNECT_ERROR;
		} catch (Exception e) {
//			MiLinkTracer.autoTrace(TraceLevel.ERROR, "Http", "Download Failed", e);

			responseCode = HTTP_CODE_ERROR;
		} finally {
			CommonUtils.closeDataObject(bos);

			if (connection != null) {
				connection.disconnect();
			}
		}

//		MiLinkTracer.autoTrace(TraceLevel.INFO, "Http", "Download Result = "
//				+ responseCode, null);

		return responseCode;
	}
}
