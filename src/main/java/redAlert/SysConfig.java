package redAlert;

import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * 系统参数
 * 一般确定后在游戏过程中不再变化
 */
public class SysConfig {

	/**
	 * 窗口大小模式枚举
	 */
	public enum WindowSizeMode {
		/** 全屏模式 */
		FULLSCREEN,
		/** 80%屏幕宽 x 70%屏幕高（默认） */
		LARGE,
		/** 1024x768 标准分辨率 */
		STANDARD_1024x768,
		/** 1280x720 高清 */
		HD_1280x720,
		/** 1600x900 高清+ */
		HD_PLUS_1600x900,
		/** 1920x1080 全高清 */
		FULL_HD_1920x1080,
		/** 自定义尺寸 */
		CUSTOM
	}

	/** 当前窗口大小模式 */
	public static WindowSizeMode windowSizeMode = WindowSizeMode.LARGE;

	/** 自定义窗口宽度（仅在 CUSTOM 模式下使用） */
	public static int customFrameWidth = 1280;

	/** 自定义窗口高度（仅在 CUSTOM 模式下使用） */
	public static int customFrameHeight = 720;

	/** 是否允许调整窗口大小 */
	public static boolean resizable = false;

	/**
	 * 初始化系统配置
	 * 根据选择的窗口大小模式计算窗口尺寸
	 */
	public static void initSysConfig() {
		// 初始化屏幕尺寸
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		SysConfig.screenWidth = dimension.width;
		SysConfig.screenHeight = dimension.height;

		// 根据窗口大小模式设置窗口尺寸
		switch (windowSizeMode) {
			case FULLSCREEN:
				// 全屏模式
				frameWidth = screenWidth;
				frameHeight = screenHeight;
				resizable = false;
				break;

			case LARGE:
				// 80%屏幕宽 x 70%屏幕高（默认）
				frameWidth = (int)(screenWidth * 0.8);
				frameHeight = (int)(screenHeight * 0.70);
				resizable = false;
				break;

			case STANDARD_1024x768:
				// 1024x768 标准分辨率
				frameWidth = 1024;
				frameHeight = 768;
				resizable = false;
				break;

			case HD_1280x720:
				// 1280x720 高清
				frameWidth = 1280;
				frameHeight = 720;
				resizable = false;
				break;

			case HD_PLUS_1600x900:
				// 1600x900 高清+
				frameWidth = 1600;
				frameHeight = 900;
				resizable = false;
				break;

			case FULL_HD_1920x1080:
				// 1920x1080 全高清
				frameWidth = 1920;
				frameHeight = 1080;
				resizable = false;
				break;

			case CUSTOM:
				// 自定义尺寸
				frameWidth = customFrameWidth;
				frameHeight = customFrameHeight;
				resizable = true; // 自定义模式允许调整大小
				break;

			default:
				// 默认使用 LARGE 模式
				frameWidth = (int)(screenWidth * 0.8);
				frameHeight = (int)(screenHeight * 0.70);
				resizable = false;
				break;
		}

		// 确保窗口不超过屏幕尺寸
		if (frameWidth > screenWidth) {
			frameWidth = screenWidth;
		}
		if (frameHeight > screenHeight) {
			frameHeight = screenHeight;
		}

		// 计算视口尺寸（减去选项面板宽度）
		SysConfig.viewportWidth = frameWidth - OptionsPanel.optionWidth;
		SysConfig.viewportHeight = frameHeight - 32; // 32是微软建议的标题栏高度

		// 计算应显示的双人位的个数
		// 钱板 16像素
		// 顶板 32像素
		// 雷达 110像素
		// 背板1 69像素
		// 底板+下选板 26+63   但是最少应该展示一半
		OptionsPanel.side2Num = (SysConfig.viewportHeight - 16 - 32 - 110 - 69 - 89) / 50 + 1;

		// 输出配置信息
		System.out.println("====================================");
		System.out.println("窗口配置信息:");
		System.out.println("屏幕尺寸: " + screenWidth + "x" + screenHeight);
		System.out.println("窗口模式: " + windowSizeMode);
		System.out.println("窗口尺寸: " + frameWidth + "x" + frameHeight);
		System.out.println("视口尺寸: " + viewportWidth + "x" + viewportHeight);
		System.out.println("可调整大小: " + resizable);
		System.out.println("双人位数量: " + OptionsPanel.side2Num);
		System.out.println("====================================");
	}

	/**
	 * 设置窗口大小模式
	 * 必须在 initSysConfig() 之前调用
	 */
	public static void setWindowSizeMode(WindowSizeMode mode) {
		windowSizeMode = mode;
	}

	/**
	 * 设置自定义窗口尺寸
	 * 必须在 initSysConfig() 之前调用，且模式设置为 CUSTOM
	 */
	public static void setCustomWindowSize(int width, int height) {
		customFrameWidth = width;
		customFrameHeight = height;
		windowSizeMode = WindowSizeMode.CUSTOM;
	}

	/**
	 * 根据屏幕尺寸推荐最佳窗口模式
	 */
	public static WindowSizeMode recommendWindowMode() {
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		int screenWidth = dimension.width;
		int screenHeight = dimension.height;

		// 全高清屏幕推荐全高清模式
		if (screenWidth >= 1920 && screenHeight >= 1080) {
			return WindowSizeMode.FULL_HD_1920x1080;
		}
		// 1600+ 屏幕推荐高清+模式
		else if (screenWidth >= 1600 && screenHeight >= 900) {
			return WindowSizeMode.HD_PLUS_1600x900;
		}
		// 1280+ 屏幕推荐高清模式
		else if (screenWidth >= 1280 && screenHeight >= 720) {
			return WindowSizeMode.HD_1280x720;
		}
		// 小屏幕使用默认比例模式
		else {
			return WindowSizeMode.LARGE;
		}
	}
	
	public static int screenWidth;
	public static int screenHeight;
	/**
	 * 游戏场景面板位置
	 * 游戏场景JPanel在JFrame中的坐落位置
	 */
	public static final int locationX = 0;
	public static final int locationY = 0;
	/**
	 * 游戏主画面宽高
	 * 主画面包括MainPanel+OptionsPanel
	 */
	public static int viewportWidth;
	public static int viewportHeight;
	/**
	 * 游戏窗口的宽高
	 */
	public static int frameWidth;
	public static int frameHeight;
	/**
	 * 战场地图的宽高
	 *
	 * 实际有效区域由中心点网格决定:
	 * - 一类中心点: x从30开始,间隔60,共50个,最大2970
	 * - 二类中心点: x从0开始,间隔60,共50个,最大2940
	 * - Y轴最大值: 1485 (一类中心点)
	 *
	 * 配置值略大于实际最大中心点坐标,留出边缘余量
	 */
	public static final int gameMapWidth = 3000;
	public static final int gameMapHeight = 1500;
	
}
