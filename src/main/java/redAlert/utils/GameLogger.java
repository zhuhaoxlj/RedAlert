package redAlert.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 游戏日志系统
 *
 * 功能：
 * 1. 拦截所有 System.out 和 System.err 输出
 * 2. 同时输出到控制台和日志文件
 * 3. 自动按日期分割日志文件
 * 4. 支持实时日志写入
 *
 * 使用示例：
 * <pre>
 * // 初始化（在程序启动时调用一次）
 * GameLogger.init();
 *
 * // 正常使用 System.out/err，会自动保存到文件
 * System.out.println("游戏开始");
 * System.err.println("警告信息");
 * </pre>
 */
public class GameLogger {

	/** 是否启用日志系统 */
	private static boolean enabled = true;

	/** 日志文件路径 */
	private static String logFilePath = null;

	/** 日志写入器 */
	private static BufferedWriter logWriter = null;

	/** 原始 System.out */
	private static PrintStream originalOut = System.out;

	/** 原始 System.err */
	private static PrintStream originalErr = System.err;

	/** 日期格式化器 */
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/** 时间戳格式化器（用于文件名） */
	private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	/**
	 * 初始化日志系统
	 * 必须在程序启动时调用，通常在 MainTest.main() 的最开始
	 */
	public static void init() {
		if (!enabled) {
			return;
		}

		try {
			// 生成日志文件名
			String timestamp = fileDateFormat.format(new Date());
			logFilePath = "game_log_" + timestamp + ".txt";

			// 创建日志写入器
			logWriter = new BufferedWriter(new FileWriter(logFilePath, true));

			// 创建自定义 PrintStream
			PrintStream loggingOut = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					originalOut.write(b);
					logToFile(String.valueOf((char) b), false);
				}

				@Override
				public void write(byte[] b, int off, int len) {
					originalOut.write(b, off, len);
					String msg = new String(b, off, len);
					logToFile(msg, false);
				}

				@Override
				public void flush() {
					originalOut.flush();
					flushLog();
				}
			}, true); // 自动刷新

			PrintStream loggingErr = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) {
					originalErr.write(b);
					logToFile(String.valueOf((char) b), true);
				}

				@Override
				public void write(byte[] b, int off, int len) {
					originalErr.write(b, off, len);
					String msg = new String(b, off, len);
					logToFile(msg, true);
				}

				@Override
				public void flush() {
					originalErr.flush();
					flushLog();
				}
			}, true);

			// 重定向 System.out 和 System.err
			System.setOut(loggingOut);
			System.setErr(loggingErr);

			// 写入日志头部
			String header = "====================================\n" +
					"游戏日志\n" +
					"开始时间: " + dateFormat.format(new Date()) + "\n" +
					"====================================\n\n";
			logToFile(header, false);

			System.out.println("日志系统已初始化，日志文件: " + logFilePath);

		} catch (IOException e) {
			originalErr.println("无法初始化日志系统: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 将日志写入文件
	 *
	 * @param message 日志消息
	 * @param isError 是否为错误输出
	 */
	private static void logToFile(String message, boolean isError) {
		if (logWriter == null) {
			return;
		}

		try {
			// 如果消息包含换行符，添加时间戳前缀
			if (message.contains("\n")) {
				String[] lines = message.split("\n");
				for (String line : lines) {
					if (!line.isEmpty()) {
						String timestamp = dateFormat.format(new Date());
						String prefix = isError ? "[ERROR] " : "[INFO] ";
						logWriter.write(timestamp + " " + prefix + line);
						logWriter.newLine();
					}
				}
			} else if (!message.isEmpty()) {
				String timestamp = dateFormat.format(new Date());
				String prefix = isError ? "[ERROR] " : "[INFO] ";
				logWriter.write(timestamp + " " + prefix + message);
				logWriter.newLine();
			}

		} catch (IOException e) {
			originalErr.println("写入日志失败: " + e.getMessage());
		}
	}

	/**
	 * 刷新日志到磁盘
	 */
	private static void flushLog() {
		if (logWriter != null) {
			try {
				logWriter.flush();
			} catch (IOException e) {
				originalErr.println("刷新日志失败: " + e.getMessage());
			}
		}
	}

	/**
	 * 关闭日志系统
	 * 程序退出前调用
	 */
	public static void shutdown() {
		if (logWriter != null) {
			try {
				String footer = "\n====================================\n" +
						"日志结束\n" +
						"结束时间: " + dateFormat.format(new Date()) + "\n" +
						"====================================\n";
				logToFile(footer, false);
				flushLog();
				logWriter.close();
				logWriter = null;

				System.out.println("日志系统已关闭");

			} catch (IOException e) {
				originalErr.println("关闭日志系统失败: " + e.getMessage());
			}
		}
	}

	/**
	 * 获取当前日志文件路径
	 */
	public static String getLogFilePath() {
		return logFilePath;
	}

	/**
	 * 设置是否启用日志系统
	 */
	public static void setEnabled(boolean enabled) {
		GameLogger.enabled = enabled;
	}

	/**
	 * 手动写入日志（不经过 System.out/err）
	 *
	 * @param message 日志消息
	 */
	public static void log(String message) {
		logToFile(message, false);
	}

	/**
	 * 手动写入错误日志
	 *
	 * @param message 错误消息
	 */
	public static void error(String message) {
		logToFile(message, true);
	}
}
