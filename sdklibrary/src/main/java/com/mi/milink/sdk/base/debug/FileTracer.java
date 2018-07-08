
package com.mi.milink.sdk.base.debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.mi.milink.sdk.base.data.SafeStringQueue;

/**
 * 文件日志追踪器 <br>
 * <br>
 * 用于实现异步的、后台的文件日志追踪，使用日期 + 序号为量度，提供读取和写入的方法<br>
 * <br>
 * <p>
 * 通过提供{@link FileTracerConfig}对象，可以为{@link FileTracer}实例设置如下选项：<br>
 * <br>
 * 
 * <pre>
 * 工作路径: 设置日志保存的路径，日志文件根据日期保存在该路径的/yyyy-MM-dd/文件夹下，以"1.xxx", "2.xxx"命名<br>
 * 日志文件扩展名: 用于方便将不同文件日志追踪器实例生成个的日志文件保存在同一个目录下，格式为".xxx"<br>
 * 线程名称: I/O操作的线程名称<br>
 * 线程优先级: I/O操作线程的优先级<br>
 * I/O操作时间阈值: 每隔多长时间进行一次I/O操作<br>
 * I/O操作数据量阈值: 每当数据量超过多少时，进行一次I/O操作<br>
 * 日志文件分片数量: 每天的日志文件最多有多少个分片，超过该值将自动删除最旧的分片<br>
 * 日志文件分片大小: 每个日志分片最大的尺寸，超过该值后将创建并使用新的分片<br>
 * 日志保存期限: 最长保存日志的时间，若超过该期限，则在{@code FileTracerConfig.cleanWorkRoot()}删除它们<br>
 * </pre>
 * 
 * </p>
 * 
 * @author MK
 */
public class FileTracer extends Tracer implements Handler.Callback {
	private static final int MSG_FLUSH = 1024;

	private static final String TAG = FileTracer.class.getSimpleName();

	private FileTracerConfig mConfig;

	private OutputStreamWriter mFileWriter; // 文件的写对象。使用FileWriter而非BufferedWriter的原因是本类已经是二级缓冲I/O操作了

	private FileChannel mFc;// 与fileWriter对应

	private File mCurrTraceFile;

	private char[] mCharBuffer; // 文件的写缓冲

	private volatile SafeStringQueue mBufferA; // 缓冲A

	private volatile SafeStringQueue mBufferB; // 缓冲B

	private volatile SafeStringQueue mWriteBuffer; // 写缓冲，用于保存当前其他调用者追踪的日志

	private volatile SafeStringQueue mReadBuffer; // 读缓冲，用于交付I/O操作线程写入文件

	private volatile boolean mIsFlushing = false;

	private HandlerThread mHandlerThread;

	private Handler mHandler;

	/**
	 * 创建一个文件日志追踪器实例，关心级别为{@code TraceLevel.ALL}，初始时启用，并使用默认的日志格式
	 * 
	 * @param config
	 *            文件日志追踪器的参数配置 {@link FileTracerConfig}
	 */
	public FileTracer(FileTracerConfig config) {
		this(TraceLevel.ALL, true, TraceFormat.DEFAULT, config);
	}

	/**
	 * 创建一个文件日志追踪器实例
	 * 
	 * @param level
	 *            关心的日志级别 参考{@link TraceLevel}
	 * @param enable
	 *            初始时启用/禁用日志追踪
	 * @param format
	 *            日志格式 参考{@link TraceFormat}
	 * @param config
	 *            文件日志追踪器的参数配置 {@link FileTracerConfig}
	 */
	public FileTracer(int level, boolean enable, TraceFormat format, FileTracerConfig config) {
		super(level, enable, format);
		try {
			// 应用提供的文件日志追踪器配置
			setConfig(config);
			// 创建双缓冲安全队列
			mBufferA = new SafeStringQueue();
			mBufferB = new SafeStringQueue();
			// 初始化写缓冲和读缓冲
			mWriteBuffer = mBufferA;
			mReadBuffer = mBufferB;
			// 初始化I/O操作时的字符缓冲
			mCharBuffer = new char[config.getMaxBufferSize()];

			// 初始化文件操作对象
			obtainFileWriter();
			// 创建I/O操作线程
			mHandlerThread = new HandlerThread(config.getThreadName(), config.getThreadPriority());
			// 启动I/O操作线程
			if (mHandlerThread != null) {
				mHandlerThread.start();
			}
			// 创建线程消息句柄
			if (mHandlerThread.isAlive()) {
				mHandler = new Handler(mHandlerThread.getLooper(), this);
			}
			// 开始时间阈值监控
			prepareNextFlush();

			// 延迟执行删除过期日志
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					// 清理过期的日志文件
					getConfig().cleanWorkFolders();
				}

			}, 15 * 1000L);
		} catch (Exception e) {
		}
	}

	/**
	 * 立刻进行一次I/O操作，将缓冲的日志写入文件中
	 */
	public void flush() {
		// 如果已经请求写入文件，则移除之前的请求，防止频繁写
		if (mHandler.hasMessages(MSG_FLUSH)) {
			mHandler.removeMessages(MSG_FLUSH);
		}

		mHandler.sendEmptyMessage(MSG_FLUSH);
	}

	/**
	 * 退出文件日志追踪器<br>
	 * <br>
	 * p.s.会尽可能的将日志全部写入文件，但调用quit()时其他线程正在写的日志可能会丢失
	 */
	public void quit() {
		// 线程退出后，FileTrace不可用
		setEnabled(false);
		// 关闭文件写对象
		closeFileWriter();
		// 退出I/O操作线程
		mHandlerThread.quit();
	}

	@Override
	protected void doTrace(int level, Thread thread, long time, String tag, String msg, Throwable tr) {
		String trace = getTraceFormat().formatTrace(level, thread, time, tag, msg, tr);
		doTrace(trace);
	}

	@Override
	protected void doTrace(String formattedTrace) {
		mWriteBuffer.addToBuffer(formattedTrace);

		// 检测数据量阈值
		if (mWriteBuffer.getBufferSize() >= getConfig().getMaxBufferSize()) {
			flush();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_FLUSH: {
			flushBuffer();
			prepareNextFlush();
			break;
		}
		default:
			break;
		}
		return true;
	}

	/**
	 * 准备下一次I/O操作
	 */
	private void prepareNextFlush() {
		mHandler.sendEmptyMessageDelayed(MSG_FLUSH, getConfig().getFlushInterval());
	}

	/**
	 * 将日志全部写入文件
	 */
	private void flushBuffer() {
		// 不允许在非文件追踪器线程调用
		if (Thread.currentThread() != mHandlerThread) {
			return;
		}
		// 已经正在I/O操作时，不要重入
		if (mIsFlushing) {
			return;
		}
		// 标记正在进行I/O操作
		mIsFlushing = true;
		{
			Writer fos;
			// OutputStreamWriter writer;
			FileLock fileLock = null;

			// 将读写缓冲交换，保证I/O操作时能持续接受新日志
			swapBuffers();
			// 将读缓冲写入文件
			try {
				fos = obtainFileWriter();

				if (fos != null) {
					// 获得文件锁
					fileLock = mFc != null ? mFc.lock() : null;
					// 冲入日志
					mReadBuffer.writeAndFlush(fos, mCharBuffer);
				}
			} catch (Exception e) {

			} finally {
				// 释放文件锁
				if (fileLock != null) {
					try {
						fileLock.release();
					} catch (Exception e2) {

					}
				}

				// 读缓冲中的日志都不再可用了（即便失败），立刻清除
				mReadBuffer.clear();
			}
		}
		// 标记I/O操作完成
		mIsFlushing = false;
	}

	/**
	 * 获取文件的写对象
	 * 
	 * @return Writer实例
	 */
	private Writer obtainFileWriter() {
		boolean forceChanged = false;

		// 从配置中获得现在应该写入的文件
		File newFile = getConfig().getCurrFile();

		// 文件不存在了就强制更换
		if (mCurrTraceFile != null) {
			if (!mCurrTraceFile.exists() || !mCurrTraceFile.canWrite()) {
				forceChanged = true;
			}
		}

		// 如果是不同于之前的文件，则关闭当前文件的写对象，重新创建
		if (forceChanged || ((newFile != null) && (!newFile.equals(mCurrTraceFile)))) {
			mCurrTraceFile = newFile;

			closeFileWriter();

			try {
				FileOutputStream fos = new FileOutputStream(mCurrTraceFile, true);
				mFc = fos.getChannel();
				mFileWriter = new OutputStreamWriter(fos);
			} catch (IOException e) {
				return null;
			}
		}

		return mFileWriter;
	}

	/**
	 * 关闭文件写对象
	 */
	private void closeFileWriter() {
		try {
			if (mFileWriter != null) {
				mFc = null;
				mFileWriter.flush();
				mFileWriter.close();
			}
		}
		// FileWriter因为使用了CharsetEncoder，
		// 所以会抛出IllegalStateException，
		// 在使用中要保持镇定，多抓异常=_=
		catch (Exception e) {
			// 不能用MilinkLog.e防止死循环
			e.printStackTrace();
		}
	}

	/**
	 * 交换读写缓存
	 */
	private void swapBuffers() {
		synchronized (this) {
			if (mWriteBuffer == mBufferA) {
				mWriteBuffer = mBufferB;
				mReadBuffer = mBufferA;
			} else {
				mWriteBuffer = mBufferA;
				mReadBuffer = mBufferB;
			}
		}
	}

	/**
	 * 获得当前文件日志追踪器的配置
	 * 
	 * @return 文件日志追踪器的配置
	 */
	public FileTracerConfig getConfig() {
		return mConfig;
	}

	/**
	 * 设置当前文件日志追踪器的配置
	 * 
	 * @param config
	 *            文件日志追踪器的配置
	 */
	public void setConfig(FileTracerConfig config) {
		this.mConfig = config;
	}
}
