
package com.mi.milink.sdk.client;

public abstract class ClientConstants {

    // 消息广播
    public static final String ACTION_DISPATCH_MSG = "com.mi.milink.ACTION_MSG";

    public static final String EXTRA_ACTION_DISPATCH_MSG_ARRAY = "extra_msg_ary";

    public static final String EXTRA_ACTION_TIME = "extra_act_time";

    public static final String EXTRA_KICKED_BY_SERVER_TYPE = "extra_kicked_type";

    public static final String EXTRA_KICKED_BY_SERVER_TIME = "extra_kicked_time";

    public static final String EXTRA_KICKED_BY_SERVER_DEVICE = "extra_kicked_device";

    // 事件广播
    public static final String ACTION_EVENT_GET_SERVICE_TOKEN = "com.mi.milink.ACTION_GET_ST";

    public static final String ACTION_EVENT_SERVICE_TOKEN_EXPIRED = "com.mi.milink.ACTION_ST_EXPIRED";

    public static final String ACTION_EVENT_CHECK_UPDATE = "com.mi.milink.ACTION_CHECK_UPDATE";

    public static final String ACTION_EVENT_INVALID_PACKET = "com.mi.milink.ACTION_INVALID_PACKET";

    public static final String ACTION_EVENT_KICKED_BY_SERVER = "com.mi.milink.ACTION_KICKED_BY_SERVER";

    // 登录相关
    public static final int LOGIN_STATE_NOT_LOGIN = 0; // 没有登录

    public static final int LOGIN_STATE_BY_SYSTEM = 1; // 系统账户登录

    public static final int LOGIN_STATE_BY_PASSWORD = 2; // 用户密码登录

    public static final int LOGIN_RET_CODE_SUCCEED = 10001; // 登录成功

    public static final int LOGIN_RET_CODE_FAILED = 10002; // 登录失败

    public static final int LOGIN_RET_CODE_USER_ID_NEEDED = 20001; // 用户名不能为空

    public static final int LOGIN_RET_CODE_PASSWORD_NEEDED = 20002; // 密码不能为空

    public static final int LOGIN_RET_CODE_PASSTOKEN_NEEDED = 20003; // passtoken不能为空

    public static final int LOGIN_RET_CODE_PASSTOKEN_EXPIRED = 20004; // passtoken过期

    public static final int LOGIN_RET_CODE_SYSTEM_NOT_LOGIN = 30001; // 系统账户没有登录

    public static final int LOGIN_RET_CODE_SYSTEM_NOT_SAME_USER = 30002; // 系统账户与app传来的不一致

    public static final String ACCOUNT_TYPE = "com.xiaomi";

    public static final String SERVICE_ID = "shtapi";
}
