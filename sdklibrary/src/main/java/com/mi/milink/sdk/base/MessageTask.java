package com.mi.milink.sdk.base;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.Build;
import android.os.Looper;

import com.mi.milink.sdk.aidl.PacketData;


public abstract class MessageTask extends FutureTask<PacketData>  {

    public MessageTask() {
        super(new Callable<PacketData>() {
            @Override
            public PacketData call() throws Exception {
                throw new IllegalStateException("this should never be called");
            }
        });
    }

    public final MessageTask start() {
        doSendWork();
        return this;
    }

    public abstract void doSendWork();

    private void ensureNotOnMainThread() {
        final Looper looper = Looper.myLooper();
        if (looper != null && looper == Global.getMainLooper()) {
            final IllegalStateException exception = new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
            if (Global.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.FROYO) {
                throw exception;
            }
        }
    }

	private PacketData getTaskResult(Long i, TimeUnit unit)
			throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
		try {
			if (i == null) {
				return get();
			} else {
				return get(i, unit);
			}
		} catch (CancellationException e) {
			throw e;
		} catch (TimeoutException e) {
			throw e;
		} catch (InterruptedException e) {
			throw e;
		} catch (ExecutionException e) {
			throw e;
		} finally {
			cancel(true /* interrupt if running */);
		}
	}
    
    private PacketData internalGetResult(Long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, CancellationException,
            TimeoutException {
        if (!isDone()) {
            ensureNotOnMainThread();
        }
       return getTaskResult(timeout,unit);
    }

    public PacketData getResult(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, CancellationException, TimeoutException {
        PacketData packet = internalGetResult(timeout, unit);
        return packet;
	}

	public PacketData getChannelResult(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
		PacketData packet = getTaskResult(timeout, unit);
		return packet;
	}

}



