package com.mi.milink.sdk.base.os.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.SecureRandom;

import com.mi.milink.sdk.util.SecureRandomUtils;

/**
 * udp请求封装
 * 
 * @author ethamhuang
 */
final class UdpClient {

	private static final int EPHEMERAL_START = 1024;

	private static final int EPHEMERAL_STOP = 65535;

	private static final int EPHEMERAL_RANGE = EPHEMERAL_STOP - EPHEMERAL_START;

	private long timeout_value = 5 * 1000; // sage:一般3秒没有响应就不会再响应。这里设置为3秒

	private static final int MAX_SIZE = 512;

	private static SecureRandom prng = SecureRandomUtils.createSecureRandom();

	public byte[] sendrecv(String dnsAddress, byte[] data) throws IOException,
			SocketTimeoutException {

		SelectableChannel channel = null;
		SelectionKey key = null;
		try {
			channel = DatagramChannel.open();
			channel.configureBlocking(false);
			Selector selector = Selector.open();

			key = channel.register(selector, SelectionKey.OP_READ);
			DatagramChannel datagramchannel = (DatagramChannel) key.channel();

			int port = prng.nextInt(EPHEMERAL_RANGE) + 1024;
			InetSocketAddress temp = new InetSocketAddress(port);
			datagramchannel.socket().bind(temp);

			InetSocketAddress dnsServerSocketAdress = new InetSocketAddress(
					InetAddress.getByName(dnsAddress), DnsConstants.DNS_PORT);

			datagramchannel.connect(dnsServerSocketAdress);
			datagramchannel.write(ByteBuffer.wrap(data));

			byte[] tmp = new byte[MAX_SIZE];
			// key.interestOps(SelectionKey.OP_READ);
			long endTime = System.currentTimeMillis() + timeout_value;
			try {
				while (!key.isReadable())
					blockUntil(key, endTime);
			} finally {
				if (key.isValid())
					key.interestOps(0);
			}

			long ret = datagramchannel.read(ByteBuffer.wrap(tmp));
			if (ret > 0) {
				int len = (int) ret;
				data = new byte[len];
				System.arraycopy(tmp, 0, data, 0, len);

				return data;
			}

		} finally {
			if (key != null) {
				key.selector().close();
				key.channel().close();
			}
		}

		return null;

	}

	public void setTimeout(long timeout) {
		// LogUtility.w("dnstest", "###设置超时：" + timeout);
		if (timeout > 0) {
			timeout_value = timeout;
			// LogUtility.w("dnstest", "###当前超时时间：" + timeout_value);
		}
	}

	private static void blockUntil(SelectionKey key, long endTime)
			throws IOException, SocketTimeoutException {
		long timeout = endTime - System.currentTimeMillis();
		// LogUtility.w("dnstest", "timeout:" + timeout);
		int nkeys = 0;
		if (timeout > 0)
			nkeys = key.selector().select(timeout);
		else if (timeout == 0)
			nkeys = key.selector().selectNow();
		if (nkeys == 0)
			throw new SocketTimeoutException();
	}

}
