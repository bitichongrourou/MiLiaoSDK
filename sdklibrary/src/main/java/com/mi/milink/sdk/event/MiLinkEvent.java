
package com.mi.milink.sdk.event;

import com.mi.milink.sdk.session.persistent.Session;

/**
 * @author chengsimin
 */
public class MiLinkEvent {
    public static class SessionConnectEvent {
        public static enum EventType {
            SessionBuildSuccess, SessionBuildFailed, SessionRunError,AssistSessionConnectSuccess, AssistSessionConnectFailed, AssistSessionRunError
        }

        public EventType mEventType;

        public Session mSession;

        public int mRetCode;

        public SessionConnectEvent(EventType eventType, Session session, int retCode) {
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

        public Session mSession;

        public int mRetCode;

        public SessionLoginEvent(EventType eventType, Session session, int retCode) {
            this.mEventType = eventType;
            this.mSession = session;
            this.mRetCode = retCode;
        }
    }

    public static class SessionOtherEvent {
        public static enum EventType {
            RequestMapIsNotEmpty, RequestMapIsEmpty, RecvInvalidPacket,StatisticsTimeoutPacket
        }

        public EventType mEventType;

        public Session mSession;

        public SessionOtherEvent(EventType eventType, Session session) {
            this.mEventType = eventType;
            this.mSession = session;
        }
    }

    public static class ServerNotificationEvent {
        public static enum EventType {
            ServerLineBroken, B2tokenExpired, ServiceTokenExpired, ShouldUpdate, KickByServer, requireUploadLog,ChannelPubKeyUpdate,requireChannelLogLevel,ListenerShouldUpdate
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
            ServiceTokenExpired, ShouldUpdate, KickByServer, GetServiceToken, RecvInvalidPacket
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
            AlarmArrived,ScreenOn,NetWorkChange,ServiceCreated
        }

        public EventType mEventType;

        public SystemNotificationEvent(EventType eventType) {
            this.mEventType = eventType;
        }
    }

    public static class ClientActionEvent {
        public static enum EventType {
            ClientNotSameUserLogin, ClientRequestCheckConnection, ClientRequestLogin, ClientRequestLogoff, ClientForceOpen,ClientSuspectBadConnection
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
    
    public static class ChannelStatusChangeEvent {
        public static enum EventType {
            channelBusy, channelIdle
        }

        public EventType mEventType;

        public Object mObject;

        public ChannelStatusChangeEvent(EventType eventType) {
            this.mEventType = eventType;
        }

        public ChannelStatusChangeEvent(EventType eventType, Object object) {
            this.mEventType = eventType;
            this.mObject = object;
        }
    }
}
