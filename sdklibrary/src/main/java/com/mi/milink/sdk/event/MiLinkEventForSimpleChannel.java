
package com.mi.milink.sdk.event;

import com.mi.milink.sdk.session.simplechannel.SessionForSimpleChannel;

/**
 * @author chengsimin
 */
public class MiLinkEventForSimpleChannel {
	public static class SessionConnectEvent {
		public static enum EventType {
			SessionBuildSuccess, SessionBuildFailed, SessionRunError
		}

		public EventType mEventType;

		public SessionForSimpleChannel mSession;

		public int mRetCode;

		public SessionConnectEvent(EventType eventType, SessionForSimpleChannel session, int retCode) {
			this.mEventType = eventType;
			this.mSession = session;
			this.mRetCode = retCode;
		}
	}

	public static class SessionLoginEvent {
		public static enum EventType {
			LoginSuccess, LoginFailed, LogoffCmdReturn
		}

		public EventType mEventType;

		public SessionForSimpleChannel mSession;

		public int mRetCode;

		public SessionLoginEvent(EventType eventType, SessionForSimpleChannel session, int retCode) {
			this.mEventType = eventType;
			this.mSession = session;
			this.mRetCode = retCode;
		}
	}

	public static class SessionOtherEvent {
		public static enum EventType {
			RequestMapIsNotEmpty, RequestMapIsEmpty, RecvInvalidPacket, StatisticsTimeoutPacket, PackageNeedRetry
		}

		public EventType mEventType;

		public SessionForSimpleChannel mSession;

		public Object obj;
		
		public SessionOtherEvent(EventType eventType, SessionForSimpleChannel session) {
			this.mEventType = eventType;
			this.mSession = session;
		}

	}

	public static class ServerNotificationEvent {
		public static enum EventType {
			ServerLineBroken, B2tokenExpired, ChannelPubKeyUpdate, ChannelDelPubKey
		}
		public EventType mEventType;

		public Object mObject;

		public ServerNotificationEvent(EventType eventType) {
			this.mEventType = eventType;
		}

		public ServerNotificationEvent(EventType eventType, Object obj) {
			this.mEventType = eventType;
			this.mObject = obj;
		}
	}

	public static class SessionManagerNotificationEvent {
		public static enum EventType {
			GetServiceToken, RecvInvalidPacket
		}

		public EventType mEventType;

		public Object mObject;

		public SessionManagerNotificationEvent(EventType eventType) {
			this.mEventType = eventType;
		}

		public SessionManagerNotificationEvent(EventType eventType, Object obj) {
			this.mEventType = eventType;
			this.mObject = obj;
		}
	}

	public static class SessionManagerStateChangeEvent {
		public static enum EventType {
			SessionStateChange, LoginStateChange
		}

		public EventType mEventType;

		public int mOldState;

		public int mNewState;

		public SessionManagerStateChangeEvent(EventType eventType, int oldState, int newState) {
			this.mEventType = eventType;
			this.mOldState = oldState;
			this.mNewState = newState;
		}
	}

	public static class SystemNotificationEvent {
		public static enum EventType {
			ScreenOn, NetWorkChange
		}

		public EventType mEventType;

		public SystemNotificationEvent(EventType eventType) {
			this.mEventType = eventType;
		}
	}

	public static class ClientActionEvent {
		public static enum EventType {
			ClientRequestCheckConnection, ClientRequestLogin, ClientRequestLogoff, ClientForceOpen
		}

		public EventType mEventType;

		public Object mObject;

		public ClientActionEvent(EventType eventType) {
			this.mEventType = eventType;
		}

		public ClientActionEvent(EventType eventType, Object object) {
			this.mEventType = eventType;
			this.mObject = object;
		}
	}
}
