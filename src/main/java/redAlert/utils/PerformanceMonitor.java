package redAlert.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控工具类
 *
 * 功能：
 * 1. 实时帧率监控
 * 2. 关键操作耗时统计（寻路、渲染等）
 * 3. 性能日志写入文件
 * 4. 实时控制台输出
 *
 * 使用示例：
 * <pre>
 * // 初始化
 * PerformanceMonitor.init();
 *
 * // 记录帧率（在渲染循环中）
 * PerformanceMonitor.recordFrame();
 *
 * // 记录操作耗时
 * long startTime = PerformanceMonitor.startOperation("寻路");
 * // ... 执行寻路操作 ...
 * PerformanceMonitor.endOperation("寻路", startTime);
 *
 * // 输出统计信息（每秒）
 * PerformanceMonitor.printStatistics();
 *
 * // 写入日志文件
 * PerformanceMonitor.writeToFile();
 * </pre>
 */
public class PerformanceMonitor {

	/** 是否启用性能监控 */
	private static boolean enabled = true;

	/** 是否输出到控制台 */
	private static boolean consoleOutput = true;

	/** 日志文件路径 */
	private static String logFilePath = "performance_log.txt";

	/** 日志写入器 */
	private static BufferedWriter logWriter;

	/** 帧率统计 */
	private static long frameCount = 0;
	private static long lastFrameTime = System.nanoTime();
	private static double currentFPS = 0.0;
	private static double avgFPS = 0.0;
	private static double minFPS = Double.MAX_VALUE;
	private static double maxFPS = 0.0;

	/** FPS 计算间隔（纳秒） */
	private static final long FPS_INTERVAL = 5_000_000_000L; // 5秒（减少输出频率）

	/** 操作耗时统计（操作名 -> 总耗时、调用次数） */
	private static Map<String, OperationStats> operationStats = new ConcurrentHashMap<>();

	/** 性能警告阈值（毫秒） */
	private static final Map<String, Long> warningThresholds = new HashMap<>();

	static {
		// 初始化警告阈值
		warningThresholds.put("寻路", 10L);        // 寻路超过 10ms 警告
		warningThresholds.put("渲染", 16L);         // 渲染超过 16ms（60fps）警告
		warningThresholds.put("鼠标事件", 5L);     // 鼠标事件超过 5ms 警告
		warningThresholds.put("资源加载", 100L);   // 资源加载超过 100ms 警告
	}

	/**
	 * 操作统计数据结构
	 */
	private static class OperationStats {
		private final AtomicLong totalTime = new AtomicLong(0);    // 总耗时（纳秒）
		private final AtomicLong callCount = new AtomicLong(0);     // 调用次数
		private volatile long maxTime = 0;                          // 最大耗时（纳秒）
		private volatile long minTime = Long.MAX_VALUE;             // 最小耗时（纳秒）

		public synchronized void addTime(long timeNs) {
			totalTime.addAndGet(timeNs);
			callCount.incrementAndGet();

			if (timeNs > maxTime) {
				maxTime = timeNs;
			}
			if (timeNs < minTime) {
				minTime = timeNs;
			}
		}

		public long getAverageTimeNs() {
			long count = callCount.get();
			return count > 0 ? totalTime.get() / count : 0;
		}

		public long getTotalTimeMs() {
			return totalTime.get() / 1_000_000L;
		}

		public long getAverageTimeMs() {
			return getAverageTimeNs() / 1_000_000L;
		}

		public long getMaxTimeMs() {
			return maxTime / 1_000_000L;
		}

		public long getMinTimeMs() {
			return minTime == Long.MAX_VALUE ? 0 : minTime / 1_000_000L;
		}

		public long getCallCount() {
			return callCount.get();
		}
	}

	/**
	 * 初始化性能监控
	 */
	public static void init() {
		if (!enabled) {
			return;
		}

		try {
			// 创建日志写入器
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String timestamp = sdf.format(new Date());
			logFilePath = "performance_log_" + timestamp + ".txt";
			logWriter = new BufferedWriter(new FileWriter(logFilePath));

			// 写入日志头
			logWriter.write("====================================\n");
			logWriter.write("性能监控日志\n");
			logWriter.write("开始时间: " + new Date() + "\n");
			logWriter.write("====================================\n\n");
			logWriter.flush();

			System.out.println("[性能监控] 已启动，日志文件: " + logFilePath);

		} catch (IOException e) {
			System.err.println("[性能监控] 初始化失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 关闭性能监控
	 */
	public static void shutdown() {
		if (!enabled) {
			return;
		}

		try {
			// 写入最终统计
			logWriter.write("\n====================================\n");
			logWriter.write("性能监控结束\n");
			logWriter.write("结束时间: " + new Date() + "\n");
			logWriter.write("====================================\n\n");
			logWriter.flush();

			if (logWriter != null) {
				logWriter.close();
			}

			System.out.println("[性能监控] 已关闭，日志已保存到: " + logFilePath);

		} catch (IOException e) {
			System.err.println("[性能监控] 关闭失败: " + e.getMessage());
		}
	}

	/**
	 * 记录一帧（在渲染循环中调用）
	 */
	public static void recordFrame() {
		if (!enabled) {
			return;
		}

		frameCount++;
		long currentTime = System.nanoTime();
		long deltaTime = currentTime - lastFrameTime;

		// 每秒计算一次 FPS
		if (deltaTime >= FPS_INTERVAL) {
			currentFPS = (double) frameCount * 1_000_000_000.0 / deltaTime;

			// 更新统计
			if (avgFPS == 0.0) {
				avgFPS = currentFPS;
			} else {
				// 指数移动平均
				avgFPS = avgFPS * 0.9 + currentFPS * 0.1;
			}

			if (currentFPS < minFPS) {
				minFPS = currentFPS;
			}
			if (currentFPS > maxFPS) {
				maxFPS = currentFPS;
			}

			// 输出到控制台
			if (consoleOutput) {
				System.out.printf("[帧率] %.1f FPS (平均: %.1f, 最低: %.1f, 最高: %.1f)%n",
					currentFPS, avgFPS, minFPS, maxFPS);
			}

			// 写入日志
			logToDisk(String.format("[帧率] %.1f FPS (平均: %.1f, 最低: %.1f, 最高: %.1f)",
				currentFPS, avgFPS, minFPS, maxFPS));

			// 重置计数
			frameCount = 0;
			lastFrameTime = currentTime;
		}
	}

	/**
	 * 开始记录操作耗时
	 * @param operationName 操作名称（如"寻路"、"渲染"）
	 * @return 开始时间（纳秒）
	 */
	public static long startOperation(String operationName) {
		if (!enabled) {
			return 0;
		}
		return System.nanoTime();
	}

	/**
	 * 结束记录操作耗时
	 * @param operationName 操作名称
	 * @param startTimeNano 开始时间（纳秒）
	 */
	public static void endOperation(String operationName, long startTimeNano) {
		if (!enabled || startTimeNano == 0) {
			return;
		}

		long endTime = System.nanoTime();
		long duration = endTime - startTimeNano;
		long durationMs = duration / 1_000_000L;

		// 记录统计
		operationStats.computeIfAbsent(operationName, k -> new OperationStats()).addTime(duration);

		// 只在严重超时时才输出警告，减少控制台开销
		Long threshold = warningThresholds.get(operationName);
		if (threshold != null && durationMs > threshold * 2) {  // 提高阈值到 2 倍
			String warning = String.format("[性能警告] %s 耗时 %d ms，超过阈值 %d ms",
				operationName, durationMs, threshold * 2);
			if (consoleOutput && durationMs > threshold * 3) {  // 只在超过 3 倍时才输出
				System.err.println(warning);
			}
			logToDisk(warning);
		}
	}

	/**
	 * 打印统计信息到控制台
	 */
	public static void printStatistics() {
		if (!enabled || operationStats.isEmpty()) {
			return;
		}

		System.out.println("\n========== 性能统计 ==========");
		System.out.printf("[帧率] 当前: %.1f FPS, 平均: %.1f FPS, 最低: %.1f FPS, 最高: %.1f FPS%n",
			currentFPS, avgFPS, minFPS, maxFPS);
		System.out.println("----------------------------------");

		for (Map.Entry<String, OperationStats> entry : operationStats.entrySet()) {
			String operationName = entry.getKey();
			OperationStats stats = entry.getValue();

			System.out.printf("[%s] 调用次数: %d, 总耗时: %d ms, 平均: %d ms, 最大: %d ms, 最小: %d ms%n",
				operationName,
				stats.getCallCount(),
				stats.getTotalTimeMs(),
				stats.getAverageTimeMs(),
				stats.getMaxTimeMs(),
				stats.getMinTimeMs());
		}

		System.out.println("==================================\n");
	}

	/**
	 * 写入详细统计到日志文件
	 */
	public static void writeToFile() {
		if (!enabled || operationStats.isEmpty()) {
			return;
		}

		try {
			logWriter.write("\n========== 性能统计 ==========\n");
			logWriter.write(String.format("[帧率] 当前: %.1f FPS, 平均: %.1f FPS, 最低: %.1f FPS, 最高: %.1f FPS%n",
				currentFPS, avgFPS, minFPS, maxFPS));
			logWriter.write("----------------------------------\n");

			for (Map.Entry<String, OperationStats> entry : operationStats.entrySet()) {
				String operationName = entry.getKey();
				OperationStats stats = entry.getValue();

				logWriter.write(String.format("[%s] 调用次数: %d, 总耗时: %d ms, 平均: %d ms, 最大: %d ms, 最小: %d ms%n",
					operationName,
					stats.getCallCount(),
					stats.getTotalTimeMs(),
					stats.getAverageTimeMs(),
					stats.getMaxTimeMs(),
					stats.getMinTimeMs()));
			}

			logWriter.write("==================================\n\n");
			logWriter.flush();

		} catch (IOException e) {
			System.err.println("[性能监控] 写入日志失败: " + e.getMessage());
		}
	}

	/**
	 * 写入日志到磁盘
	 */
	private static synchronized void logToDisk(String message) {
		if (logWriter == null) {
			return;
		}

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			String timestamp = sdf.format(new Date());
			logWriter.write("[" + timestamp + "] " + message + "\n");
			logWriter.flush();
		} catch (IOException e) {
			System.err.println("[性能监控] 写入日志失败: " + e.getMessage());
		}
	}

	/**
	 * 重置统计数据
	 */
	public static void reset() {
		frameCount = 0;
		lastFrameTime = System.nanoTime();
		currentFPS = 0.0;
		avgFPS = 0.0;
		minFPS = Double.MAX_VALUE;
		maxFPS = 0.0;
		operationStats.clear();

		System.out.println("[性能监控] 统计数据已重置");
		logToDisk("[性能监控] 统计数据已重置");
	}

	/**
	 * 设置是否启用性能监控
	 */
	public static void setEnabled(boolean enabled) {
		PerformanceMonitor.enabled = enabled;
		System.out.println("[性能监控] " + (enabled ? "已启用" : "已禁用"));
	}

	/**
	 * 设置是否输出到控制台
	 */
	public static void setConsoleOutput(boolean consoleOutput) {
		PerformanceMonitor.consoleOutput = consoleOutput;
	}

	/**
	 * 获取当前 FPS
	 */
	public static double getCurrentFPS() {
		return currentFPS;
	}

	/**
	 * 获取平均 FPS
	 */
	public static double getAvgFPS() {
		return avgFPS;
	}
}
