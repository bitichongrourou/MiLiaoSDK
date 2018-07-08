
package com.mi.milink.sdk.data;

import com.mi.milink.sdk.base.debug.TraceLevel;

/**
 * @author MK
 */
public final class Const {

    public static final String STASTIC_SERVER_HOST = "d.g.mi.com";

    public static final String STASTIC_SERVER_ADDR = "https://d.g.mi.com/c.do​";

    public static final String STASTIC_SERVER_ADDR_IP = "http://120.92.24.141/c.do​";


    public static final String STASTIC_ZHIBO_SERVER_HOST = "dzb.g.mi.com";

    public static final String STASTIC_ZHIBO_SERVER_ADDR = "http://dzb.g.mi.com/c.do";

    public static final String STASTIC_ZHIBO_SERVER_ADDR_IP = "http://dzb.g.mi.com/c.do";

    
    public static final String SPEED_TEST_SERVER_ADDR = "http://d.g.mi.com/tr.do​";

    public static final String SPEED_TEST_SERVER_ADDR_IP = "http://120.92.24.141/tr.do​";

    public static final String PARAM_APP_ID = "appid"; // app id

    public static final String PARAM_CLIENT_VERSION = "cversion"; // 客户端版本

    public static final String PARAM_MI_LINK_VERSION = "mversion"; // milink 版本

    public static final String PARAM_SYSTEM_VERSION = "sversion"; // 系统版本

    public static final String PARAM_DEVICE_ID = "did"; // 设备id

    public static final String PARAM_DEVICE_INFO = "dinfo"; // 设备信息，传model

    public static final String PARAM_PACKET_VID = "vid"; // app user id

    // 采样率（上报值为分母，主要用于还原访问次数，成功量按1/10抽样（填10），失败量全量上报（填1)）
    public static final String PARAM_FREQ = "freq"; // freq

    public static final String PARAM_DATA = "data"; // apn

    public static final String PARAM_CHANNEL = "channel"; // 渠道

    public static final String TRACE_AC = "ac"; // mlink_cmd

    public static final String TRACE_AC_VALUE = "mlink_cmd";

    public static final String DATA_EXTRA_CMD = "data.extra";

    public static final String DATA_CLIENTIP_EXTRA_CMD = "data.clientip";
    public static final String DATA_ANONYMOUSWID_EXTRA_CMD = "data.anonymouswid";
    public static final String DATA_CHANNEL_ANONYMOUSWID_EXTRA_CMD = "data.channel.anonymouswid";
    public static final String DATA_LOGLEVEL_CMD = "data.loglevel";

    
    public static boolean isMnsCmd(String cmd) {
        return cmd != null ? cmd.startsWith("milink") : false;
    }

    public static interface MiLinkCode {
        public int MI_LINK_CODE_OK = 0;

        public int MI_LINK_CODE_SERVICE_TOKEN_EXPIRED = 100; // login的时候servicetoken过期

        public int MI_LINK_CODE_B2_TOKEN_EXPIRED = 101; // 发包的时候b2Token过期

        public int MI_LINK_CODE_KICKED_BY_SERVER = 102; // 被踢, 以后不会通过返回码来kick

//        public int MI_LINK_CODE_SHOULD_CHECK_UPDATE = 103; // 需要检查更新

        public int MI_LINK_CODE_TIMEOUT = 109; // 处理业务包超时

        public int MI_LINK_CODE_ACC_NEED_RETRY = 118; // 服务器希望带上clientinfo重试

        public int MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN = 110; // 服务器专线断了

        public int MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN_URGENT = 119; // 服务器专线断了
        
        public int MI_LINK_CODE_SERVER_UPADTE_CHANNEL_PUB_KEY = 129; // 服务器更换通道key
        public int MI_LINK_CODE_SERVER_DELETE_CHANNEL_PUB_KEY = 223; // 服务器更换通道key
    }

    public static interface InternalErrorCode {
        public final static int SUCCESS = 0;

        public final static int READ_TIME_OUT = 515;

        public final static int CONNECT_FAIL = 516;

        public final static int READ_FAIL = 517;

        public final static int ENCRYPT_FAILED = 518;
        
        public final static int MNS_PACKAGE_ERROR = 526;

        public final static int MNS_NOT_LOGIN = 533;

        public final static int IP_ADDRESS_NULL = 557;

        public final static int MNS_LOAD_LIBS_FAILED = 562;

        public final static int MNS_PACKAGE_INVALID = 998;// 最小值
    }

    public static interface MnsCmd {

        public String MNS_HEARTBEAT = "milink.heartbeat";

        public String MNS_FIRST_HEARTBEAT = "milink.firstheartbeat";

        public String MNS_PUSH_CMD = "milink.push";

        public String MNS_PUSH__ACK_CMD = "milink.push.ack";

        public String MNS_PING_CMD = "milink.ping"; // 仅用于上报，ping包实际并没有cmd

        public String MNS_KICK_CMD = "milink.kick"; // 踢下线的push

        public String MNS_GET_CONFIG = "milink.getconfig"; // 获取配置

        public String MNS_LOGOFF = "milink.loginoff";

        public String MNS_HAND_SHAKE = "milink.handshake";

        public String MNS_FAST_LOGIN = "milink.fastlogin"; // 使用serviceToken加密

        public String MNS_ANONYMOUS_FAST_LOGIN = "milink.anonymous"; // 使用serviceToken加密
        
        public String MNS_CHANNEL_FAST_LOGIN = "milink.channel";
        
        @Deprecated
        public String MNS_REGISTER_BIND = "milink.registerbind";// 使用B2Token加密，
                                                                // 曾经这是为miliao准备的，现在不用了

        // 统计的cmd，并没有这种包
        public String MNS_OPEN_CMD = "milink.open"; // 跑马过程的上报

        public String MNS_DNS_FAIL_CMD = "milink.dnsfail"; // dns解析失败

        public String MNS_START_SERVICE = "milink.startservice"; // 启动service

        public String MNS_BIND_SERVICE = "milink.bindservice"; // 绑定service

        public String MNS_GET_REMOTE_SERVICE = "milink.getremoteservice"; // 启动service
        
        public String MNS_MILINK_PUSH_LOG = "milink.push.log"; // 请求上传日志
        
        public String MNS_MILINK_UPLOADLOG = "milink.uploadlog"; // 上传日志
        
        
        public String MNS_MILINK_PUSH_LOGLEVEL = "milink.push.loglevel"; // 请求上传日志
    }

    public static interface PushLogType {
    	
    	public int PUSH_UPLOADLOG = 0;
    	public int PUSH_LOGLEVEL= 1;
    	
    }
    
    public static interface ServerPort {
        public int PORT_443 = 443;

        public int PORT_80 = 80;

        public int PORT_8080 = 8080;

        public int PORT_14000 = 14000;

        public int[] PORT_ARRAY = new int[] {// 端口顺序不要改动，隐含着优先级
                PORT_443, PORT_80, PORT_8080, PORT_14000
        };
    }

    /**
     * MNS服务常量
     */
    public static interface Service {
        public long DefHeartBeatInterval = 3 * 60 * 1000L; // 首次/默认心跳周期：3分钟

        public long DefPowerSaveHeartBeatInterval = 20 * 60 * 1000L; // 后台/默认心跳周期：20分钟

        public int DefPingTime = 3; // 默认后台心跳周期的PING包数

        public String ActionName = "com.milink.sdk.heartbeat"; // 心跳Action名称
    }

    /**
     * MNS额外参数配置常量
     */
    public static interface Extra {

        public String OnStartCommandReturn = "onStartCommandReturn";

        /* 后台模式参数 */
        public String BackgroundMode = "idle.timespan";

        /* 自杀机制参数 */
        public String SuicideEnabled = "suicide.enabled";

        public String SuicideTimespan = "suicide.time.startup";

        /* 来宾模式上报后缀 */
        public String GuestPostfix = "guest.postfix"; // "xxx.xxx;xxx.xxx;xxx.xxx"

        /* 调试模式IP */
        public String MnsDebugIPList = "mns.debug.iplist"; // "192.168.1.1:80"

        public boolean DefSuicideEnabled = true; // 默认自杀功能：开启

        public long DefSuicideTimespan = 12 * 60 * 60 * 1000L; // 默认自杀周期：12小时

        public long DefBackgroundTimespan = 15 * 60 * 1000L; // 默认省电模式延时：15分钟

        String ReportLogTitle = "report_log_title";// 用户反馈标题

        String ReportLogContent = "report_log_content";// 用户反馈内容
    }

    /**
     * ACCESS统计上报常量
     */
    public static interface Access {
        public String WtLogin = "mns.internal.login.wt";

        public String B2Login = "mns.internal.login.b2";

        public String HandShake = "mns.internal.handshake";

        public String Heartbeat = "mns.internal.heartbeat";

        public String Connect = "mns.internal.connect";

        public String OpenSession = "mns.internal.opensession";

        public String DnsResolve = "mns.internal.dnsresolve";

        public String NetMatchInfo = "mns.internal.netmatchinfo";

        public String GuestPostfix = ".mi";

        public String SampleRate = "access.samplerate";

        public String DataThreshold = "access.data.count";

        public String TimeThreshold = "access.time.interval";

        public String BackupIP = "access.server.backup";

        public int DefSampleRate = 10; // 默认采样率：10

        public int DefDataThreshold = 50; // 默认数据量阈值：50条

        public long DefTimeThreshold = 10 * 60 * 1000L; // 默认时间阈值： 10分钟

        public String DefBackupIP = null; // 默认备份IP：空
    }

    /**
     * 调试常量
     */
    public static interface Debug {
        public boolean Enabled = true; // 调试全局开关

        public boolean FileTracerEnabled = true; // 文件日志开关

        public boolean InfiniteTraceFile = false; // 文件日志无限记录开关

        public boolean LogcatTracerEnabled = true; // Logcat开关

        public boolean NeedAttached = false; // 是否允许服务等待调试器

        public boolean ShowErrorCode = false; // 错误提示信息显示错误码

        public long MinSpaceRequired = 80 * 1024 * 1024; // 外存最小空间要求：80M

        public int DefFileBlockSize = 512 * 1024; // 固定分片大小：512K

        public int DefDataThreshold = 8 * 1024; // 数据量阈值：默认16K，单位字符 //为节约内存转为8K

        public int DefTimeThreshold = 15 * 1000; // 时间阈值

        public int DefFileBlockCount = 48; // 36MB

        public int DefFileTraceLevel = TraceLevel.ALL; // 全部打印

        public int DefLogcatTraceLevel = TraceLevel.ALL; // 全部打印

        public long DefFileKeepPeriod = 3 * 24 * 60 * 60 * 1000L; // 3天

        public String FileRoot = "Xiaomi";

        public String FileTracerName = "Mns.File.Tracer";

        public String ClientFileTracerName = "Mns.Client.File.Tracer";

        public String FileExt = ".m.log";

        public String ClientFileExt = ".app.log";
        
        public String ChannelFileExt = ".c.log";

        public String FileBlockCount = "debug.file.blockcount";

        public String FileKeepPeriod = "debug.file.keepperiod";

        public String FileTraceLevel = "debug.file.tracelevel";

        public String LogcatTraceLevel = "debug.logcat.tracelevel";

        public String ClientFileBlockCount = "client.debug.file.blockcount";

        public String ClientFileKeepPeriod = "client.debug.file.keepperiod";

        public String ClientFileTraceLevel = "client.debug.file.tracelevel";

        public String ClientLogcatTraceLevel = "client.debug.logcat.tracelevel";

    }

    /**
     * 服务器状态常量
     */
    public static interface SessionState {
        public int Connected = 2; // 已连接

        public int Connecting = 1; // 正在连接

        public int Disconnected = 0; // 尚未连接
    }

    /**
     * 登录状态常量
     */
    public static interface LoginState {
        public int Logined = 2; // 已登录

        public int Logining = 1; // 正在登录

        public int NotLogin = 0; // 尚未登录
    }

    public static final int NONE = Integer.MIN_VALUE;

    /**
     * 标签常量
     */
    public static interface Tag {

        public String Client = "MiLinkClient";

        public String Service = "MiLinkServiceBinder";

    }

    /**
     * 通信常量
     */
    public static interface IPC {
        public int MaxRestartTimes = 3;

        public String ClientInfo = "ipc.client.info";

        public String ClientNotifier = "ipc.client.notifier";

        public String ServiceName = "com.mi.milink.sdk.service.MiLinkService";

        public int Auth = 1;

        public int Register = 2;

        public int StatePass = 3;

        public int Login = 4;

        public int Transfer = 5;

        public int Logout = 6;

        public int ReportLog = 7;// 上传log

        public long DefAsyncTimeout = 150 * 1000L; // 默认服务调用请求超时

        public long TransferAsyncTimeoutEx = 90 * 1000L; // 透传调用请求额外超时

        long LogoutAsyncTimeout = 15 * 1000L;// 登出时异步调用超时时间

        long LogoutAsyncTellServerTimeout = 20 * 1000L;// 登出时异步调用超时时间,通知服务器
    }

    /**
     * 服务端恢复功能
     */
    public static interface Protection {
        public String Client = "protect.client";
    }

    /**
     * 客户端事件常量
     */
    public static interface Event {
        public String Extra = "event.extra";

        public String Extra2 = "event.extra2";

        public int CONFIG_UPDATED = 1; // 配置更新

        public int TICKET_UPDATED = 2; // 票据更新

        public int SUICIDE_TIME = 4; // 自杀事件

        public int AUTH_FAIL = 5; // A2验证失败

        public int SERVER_STATE_UPDATED = 6; // 服务器连接状态更新

        public int SERVER_LOGIN_FAIL = 7; // B2登录失败

        public int MNS_HEARTBEAT = 8; // 心跳事件

        public int MNS_INTERNAL_ERROR = 9; // 全局错误

        public int SERVICE_CONNECTED = 10; // 服务连接消息

        public int EXP_VERSION_LIMIT = 11; // 体验版本限制

        public int RUNTIME_CHANGED = 12; // 运行状态改变

        public int SAFE_MODE_B2_SUCC = 13;// 登录成功

        public int MI_LINK_LOGIN_STATE_CHANGED = 14;// 登录
    }

    /**
     * 消息推送常量
     */
    public static interface Push {
        public String ActionFormat = "mns.push.to.%s";

        public String TypeField = "push.type";

        public String CountField = "push.count";

        public String TimeField = "push.time";

        public String DataField = "push.data";

        public int TYPE_NONE = 0; // 无类型消息

        public int TYPE_BIZ_PUSH = 1; // 业务推送消息

        public int TYPE_REBORN = 2; // 定时重生消息
    }

}
