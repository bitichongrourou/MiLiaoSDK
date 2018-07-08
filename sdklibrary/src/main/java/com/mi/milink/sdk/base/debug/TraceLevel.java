package com.mi.milink.sdk.base.debug;

/**
 * 日志级别常量集合<br>
 * 类似于Android的日志分级，将日志级别划分为:<br>
 *
 * <pre>
 * 详尽(VERBOSE) —— 最基础的、最详细的信息
 * 调试(DEBUG) —— 对于调试有帮助的信息
 * 信息(INFO) —— 程序执行的关键点、关键信息
 * 警告(WARN) —— 不影响程序执行，但非常态或可能引起问题的信息
 * 错误(ERROR) —— 错误信息
 * 断言(ASSERT) —— 不可能的事情发生的信息
 * </pre>
 *
 * 本接口类中包含了这些级别常量以及它们的常用组合，若要在某个类代码中方便地使用这些常量，请让该类实现本接口
 *
 * @author MK
 */
public interface TraceLevel {
	/**
	 * 日志级别：详尽——俗称“啰嗦”
	 */
	public static final int VERBOSE = 1;

	/**
	 * 日志级别：调试
	 */
	public static final int DEBUG = 2;

	/**
	 * 日志级别：信息
	 */
	public static final int INFO = 4;

	/**
	 * 日志级别：警告
	 */
	public static final int WARN = 8;

	/**
	 * 日志级别：错误
	 */
	public static final int ERROR = 16;

	/**
	 * 日志级别：断言
	 */
	public static final int ASSERT = 32;

	/**
	 * 日志级别：详尽以上的所有级别（不含详尽）
	 */
	public static final int ABOVE_VERBOSE = DEBUG | INFO | WARN | ERROR
			| ASSERT;

	/**
	 * 日志级别：调试及调试以上的级别
	 */
	public static final int DEBUG_AND_ABOVE = ABOVE_VERBOSE;

	/**
	 * 日志级别：调试以上的所有级别（不含调试）
	 */
	public static final int ABOVE_DEBUG = INFO | WARN | ERROR | ASSERT;

	/**
	 * 日志级别：信息及信息以上的级别
	 */
	public static final int INFO_AND_ABOVE = ABOVE_VERBOSE;

	/**
	 * 日志级别：信息以上的所有级别（不含信息）
	 */
	public static final int ABOVE_INFO = WARN | ERROR | ASSERT;

	/**
	 * 日志级别：警告及警告以上的级别
	 */
	public static final int WARN_AND_ABOVE = ABOVE_INFO;

	/**
	 * 日志级别：警告以上的所有级别（不含警告）
	 */
	public static final int ABOVE_WARN = ERROR | ASSERT;

	/**
	 * 日志级别：所有级别（详尽、调试、信息、警告、错误和断言）
	 */
	public static final int ALL = VERBOSE | DEBUG | INFO | WARN | ERROR
			| ASSERT;
}
