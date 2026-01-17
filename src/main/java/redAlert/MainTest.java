package redAlert;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import redAlert.enums.UnitColor;
import redAlert.event.EventHandlerManager;
import redAlert.militaryBuildings.AfPill;
import redAlert.militaryBuildings.SfMisl;
import redAlert.other.Mouse;
import redAlert.other.Place;
import redAlert.resourceCenter.ShapeUnitResourceCenter;
import redAlert.shapeObjects.Building.SceneType;
import redAlert.shapeObjects.soldier.Sniper;
import redAlert.shapeObjects.vehicle.GrizTank;
import redAlert.shapeObjects.vehicle.Mcv;
import redAlert.shapeObjects.vehicle.Sref;
import redAlert.shapeObjects.vehicle.XiniuTank2;
import redAlert.utilBean.CenterPoint;
import redAlert.utilBean.LittleCenterPoint;
import redAlert.utils.LittleCenterPointUtil;
import redAlert.utils.PerformanceMonitor;
import redAlert.utils.PointUtil;

/**
 * 程序启动类
 *
 */
public class MainTest {

	/**
	 * 生成红警风格的自然地图
	 * 参考红警原版实现：
	 * 1. 使用智能边匹配算法
	 * 2. 创建大的连续地形区域
	 * 3. 瓦片之间无缝衔接
	 */
	private static void generateGrassMap() {
		try {
			File mapFile = new File(GlobalConfig.mapFilePath);
			FileWriter writer = new FileWriter(mapFile);

			// 16种草地瓦片
			String[] grassTiles = {
				"clat01.tem", "clat02.tem", "clat03.tem", "clat04.tem",
				"clat05.tem", "clat06.tem", "clat07.tem", "clat08.tem",
				"clat09.tem", "clat10.tem", "clat11.tem", "clat12.tem",
				"clat13.tem", "clat14.tem", "clat15.tem", "clat16.tem"
			};

			// 简化：使用Perlin噪声思想创建自然分布
			// 先用少量噪声确定大致区域，再填充细节
			java.util.Random random = new java.util.Random(42); // 固定种子保证可重现

			// 创建地形图（模拟区域分布）
			String[][] tileMap = new String[50][50];

			// 第一阶段：创建大的区域（模拟红警的区域化地形）
			for(int m = 0; m < 50; m++) {
				for(int n = 0; n < 50; n++) {
					// 使用简单的噪声函数创建区域
					double noise = calculateNoise(m, n, 0.1); // 低频率 = 大区域

					// 将噪声映射到瓦片索引
					// clat01-08 主要，clat09-16 较少
					int tileIndex;
					if (noise < 0.3) {
						tileIndex = 0; // clat01 - 最常见
					} else if (noise < 0.5) {
						tileIndex = 1 + (int)((noise - 0.3) * 10) % 7; // clat02-08
					} else {
						tileIndex = 8 + (int)((noise - 0.5) * 16) % 8; // clat09-16
					}

					tileMap[m][n] = grassTiles[tileIndex];
				}
			}

			// 第二阶段：平滑过渡（模拟红警的智能边匹配）
			for(int pass = 0; pass < 2; pass++) { // 做2次平滑
				String[][] newTileMap = new String[50][50];
				for(int m = 0; m < 50; m++) {
					for(int n = 0; n < 50; n++) {
						// 统计周围瓦片
						java.util.Map<String, Integer> neighborCount = new java.util.HashMap<>();

						// 检查8个邻居
						for(int dm = -1; dm <= 1; dm++) {
							for(int dn = -1; dn <= 1; dn++) {
								if(dm == 0 && dn == 0) continue;
								int nm = m + dm;
								int nn = n + dn;
								if(nm >= 0 && nm < 50 && nn >= 0 && nn < 50) {
									String tile = tileMap[nm][nn];
									neighborCount.put(tile, neighborCount.getOrDefault(tile, 0) + 1);
								}
							}
						}

						// 选择最常见的邻居瓦片（平滑过渡）
						String mostCommon = tileMap[m][n];
						int maxCount = 0;
						for(java.util.Map.Entry<String, Integer> entry : neighborCount.entrySet()) {
							if(entry.getValue() > maxCount) {
								maxCount = entry.getValue();
								mostCommon = entry.getKey();
							}
						}

						// 70%概率跟随邻居，30%保持原样（保留一些变化）
						if(random.nextDouble() < 0.7) {
							newTileMap[m][n] = mostCommon;
						} else {
							newTileMap[m][n] = tileMap[m][n];
						}
					}
				}
				tileMap = newTileMap;
			}

			// 写入地图文件
			// 一类中心点
			for(int m = 0; m < 50; m++) {
				int y = 15 + 30 * m;
				for(int n = 0; n < 50; n++) {
					int x = 30 + 60 * n;
					writer.write(x + "," + y + "," + tileMap[m][n] + "$\n");
				}
			}

			// 二类中心点
			for(int m = 0; m < 50; m++) {
				int y = 30 * m;
				for(int n = 0; n < 50; n++) {
					int x = 60 * n;
					writer.write(x + "," + y + "," + tileMap[m][n] + "$\n");
				}
			}

			writer.close();
			System.out.println("✓ 已生成红警风格草地地图: " + mapFile.getAbsolutePath());
			System.out.println("  - 使用区域化地形 + 平滑过渡算法");
			System.out.println("  - 模拟红警原版的自然分布效果");
		} catch (IOException e) {
			System.err.println("✗ 生成地图失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 简单的噪声函数（模拟Perlin噪声的低频部分）
	 * 用于创建大的连续地形区域
	 */
	private static double calculateNoise(int m, int n, double frequency) {
		// 简化的正弦波叠加（模拟Perlin噪声）
		double value = 0;
		value += Math.sin(m * frequency) * Math.cos(n * frequency);
		value += 0.5 * Math.sin(m * frequency * 2) * Math.cos(n * frequency * 2);
		// 归一化到0-1
		return (value + 1.5) / 3.0;
	}
	/**
	 * 是否使用OpenGL来渲染,默认使用
	 */
	public static boolean isUseOpenGL = true;
	
	public static void main(String[] args) throws Exception{
		// 程序窗口
		JFrame jf = new JFrame("红色警戒");

		// 初始化性能监控系统
		PerformanceMonitor.init();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			PerformanceMonitor.shutdown();
		}));

		// 生成纯平草地地图
		// generateGrassMap(); // 已禁用，使用完整地形地图代替

		SysConfig.initSysConfig();//初始化系统参数

		//初始化鼠标指针形状图片
		Mouse.initMouseCursor();
		//初始化建造预选块
		Place.initPlaceRect();
		
		//游戏主界面
		JPanel scenePanel = null;
		if(isUseOpenGL) {
			scenePanel = new MainPanel();//基于OpenGL的渲染
		}else {
			scenePanel = new MainPanelJava();//基于JavaSwing的渲染
		}
		jf.add(BorderLayout.CENTER,scenePanel);//格式布局放中间
		
		//选项卡页面
		OptionsPanel optionsPanel = new OptionsPanel();
		jf.add(BorderLayout.EAST,optionsPanel);//格式布局放右边
		
		jf.setSize(SysConfig.frameWidth,SysConfig.frameHeight);
		jf.setResizable(false);//不可调整大小
		jf.setAlwaysOnTop(false);//不置顶
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		jf.setLocationRelativeTo(null);//屏幕居中
		jf.setLocation( (SysConfig.screenWidth-SysConfig.frameWidth)/2 , (SysConfig.screenHeight-SysConfig.frameHeight)/2);
		jf.setVisible(true);//JFrame默认不可见,设置为可见
		jf.pack();
		
		/*
		 * 红警事件管理器
		 */
		EventHandlerManager.init();
		/**
		 * 鼠标事件的处理
		 */
		MouseEventDeal.init(scenePanel);
		/**
		 * 键盘事件的处理
		 */
		KeyBoardEventDeal.init(scenePanel);
		/**
		 * 伤害计算器
		 */
		ShapeUnitResourceCenter.startDamageCalculate();
		
		
		
		
		
		int time = 2000;
		
		//改变建筑颜色的操作
		/*
		AfAirc afAric = new AfAirc(SceneType.TEM,UnitColor.LightBlue,500,550);
		Constructor.putOneBuilding(afAric,scenePanel);//空指部
		afAric.setUnitColor(UnitColor.Purple);
		
		Thread.sleep(3000);
		afAric.setUnitColor(UnitColor.Gray);//改变颜色  实现工程师占领
		Thread.sleep(3000);
		Constructor.putOneBuilding(new AfAirc(SceneType.TEM,UnitColor.Green,600,300),scenePanel);//盟军空指部
		*/
		
		//建造动画
//		AfCnst afCnst = new AfCnst(SceneType.TEM,UnitColor.Orange,400,300);afCnst.setStatus(BuildingStatus.UNDEMAGED);
//		Constructor.putOneBuilding(afCnst,scenePanel);//盟军基地
//		Thread.sleep(time);
//		AfPowr afPowr = new AfPowr(SceneType.TEM,UnitColor.Blue,900,320);afPowr.setStatus(BuildingStatus.DEMAGED);
//		Constructor.putOneBuilding(afPowr,scenePanel);//盟军发电场
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfPile(SceneType.TEM,UnitColor.Green,600,300),scenePanel);//盟军兵营
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfRefn(SceneType.TEM,UnitColor.Gray,700,425),scenePanel);//盟军矿场
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfYard(SceneType.TEM,UnitColor.Yellow,500,120),scenePanel);//船坞
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfDept(SceneType.TEM,UnitColor.Red,420,220),scenePanel);//维修厂
//		Thread.sleep(time);
//		AfTech afte = new AfTech(SceneType.TEM,UnitColor.Red,700,630);
//		Constructor.putOneBuilding(afte,scenePanel);//作战实验室
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfOrep(SceneType.TEM,UnitColor.Green,515,800),scenePanel);//矿石精炼厂
//		Thread.sleep(time);
		CenterPoint cp = PointUtil.getCenterPoint(800, 550);
		AfPill targetPill = new AfPill(cp,SceneType.TEM,UnitColor.Pink);
		Constructor.putOneBuilding(targetPill);//机枪碉堡
		
		CenterPoint cp2222 = PointUtil.getCenterPoint(200, 550);
		AfPill targetPill2 = new AfPill(cp2222,SceneType.TEM,UnitColor.Pink);
		Constructor.putOneBuilding(targetPill2);//机枪碉堡
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfSam(SceneType.TEM,UnitColor.LightBlue,550,550),scenePanel);//爱国者飞弹
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfPris(SceneType.TEM,UnitColor.Yellow,400,550),scenePanel);//光棱塔
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfGap(SceneType.TEM,UnitColor.Purple,200,200),scenePanel);//裂缝产生器
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfGcan(SceneType.TEM,UnitColor.Purple,500,600),scenePanel);//巨炮
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfCsph(SceneType.TEM,UnitColor.Red,200,450),scenePanel);//超时空转换器
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfSpst(SceneType.TEM,UnitColor.Blue,300,600),scenePanel);//间谍卫星
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfWeth(SceneType.TEM,UnitColor.Green,300,700),scenePanel);//天气控制器
//		Thread.sleep(time);
//		Constructor.putOneBuilding(new AfComm(SceneType.TEM,UnitColor.Gray,300,150),scenePanel);//通信中心
		
		
		//核弹打小人
//		Constructor.putOneShapeUnit(new Sniper(LittleCenterPointUtil.getLittleCenterPoint(500, 500),UnitColor.Red));//狙击手
//		CenterPoint cpMisl = PointUtil.getCenterPoint(100, 300);
//		SfMisl mislSilo = new SfMisl(cpMisl,SceneType.TEM,UnitColor.Blue);
//		Constructor.putOneBuilding(mislSilo);//苏军核弹井
//		Thread.sleep(2000);
//		Constructor.playOneMusic("ceva001");//warning nuclear silo detected
//		Thread.sleep(3000);
//		mislSilo.nuclearSiloExpand();//展开核弹井
//		Thread.sleep(5000);
//		mislSilo.nuclearSiloLaunch();//发射核弹
		
		
		
		//重工生产坦克和基洛夫
		/*
		AfWeap weap = new AfWeap(SceneType.TEM,UnitColor.Purple,610,310);
		Constructor.putOneBuilding(weap,scenePanel);//建设工厂
		Thread.sleep(time);
		weap.setMakingVehicle(true);//正在生产坦克
		XiniuTank xt = new XiniuTank(weap.getPositionX()+weap.getCenterOffX()-64-32,weap.getPositionY()+weap.getCenterOffY()-64-16,"",UnitColor.Blue);
		Constructor.putOneShapeUnit(xt, scenePanel);//建造坦克
		xt.move = true;//坦克移动 
		xt.moveToTarget( PointUtil.getCenterPoint(xt.positionX+64+32*4, xt.positionY+64+16*4));
		Thread.sleep(5000);
		weap.setMakingVehicle(false);//恢复正常状态
		
		
		Thread.sleep(1000);
		ZepIn zepIn = new ZepIn(weap.getPositionX()+weap.getCenterOffX()-70 , weap.getPositionY()+weap.getCenterOffY()-70,"",UnitColor.Blue);
		Zep zep = new Zep(weap.getPositionX()+weap.getCenterOffX()-70 , weap.getPositionY()+weap.getCenterOffY()-80,"",UnitColor.Blue);
		zep.zepIn = zepIn;
		weap.setMakingFly(true);//正在生产飞行物
		Thread.sleep(100);
		Constructor.putOneShapeUnit(zep, scenePanel);//建造基洛夫
		Constructor.putOneShapeUnit(zepIn, scenePanel);//建造基洛夫影子
		Constructor.playOneMusic("vkirsea");
		Thread.sleep(100);
		zep.move = true;//基洛夫移动
		
		Thread.sleep(2000);
		weap.setMakingFly(false);//恢复正常状态
		*/
		
		
		//坦克寻路移动
		
//		CanvasPainter.drawRhombusDebug(PointUtil.getCenterPoint(1080,600), GameContext.getMainPanel().getGuidelinesCanvas());//辅助Debug的框框
//		CanvasPainter.drawRhombusDebug(PointUtil.getCenterPoint(32, 500), GameContext.getMainPanel().getGuidelinesCanvas());//辅助Debug的框框
//		CanvasPainter.drawRhombusDebug(PointUtil.getCenterPoint(1080, 100), GameContext.getMainPanel().getGuidelinesCanvas());//辅助Debug的框框
//		CanvasPainter.drawGuidelines(GameContext.getMainPanel().getGuidelinesCanvas());
		
		Thread.sleep(500);
		GrizTank gtank = new GrizTank(64*2-64,32*3-64,UnitColor.Pink);
		Constructor.putOneShapeUnit(gtank);//灰熊坦克
//		
		CenterPoint cc1 = PointUtil.getCenterPoint(450, 100);
		XiniuTank2 xnTank = new XiniuTank2(cc1.getX()-64,cc1.getY()-64,UnitColor.Pink);
		Constructor.putOneShapeUnit(xnTank);//犀牛坦克
		
//		CenterPoint cc = PointUtil.getCenterPoint(300, 100);
//		Ifv xt2 = new Ifv(cc.getX()-64,cc.getY()-64,UnitColor.Orange);
//		Constructor.putOneShapeUnit(xt2);//多功能步兵车
		
		CenterPoint cc = PointUtil.getCenterPoint(300, 100);
		Sref xt2 = new Sref(cc.getX()-64,cc.getY()-64,UnitColor.Orange);
		Constructor.putOneShapeUnit(xt2);//光棱坦克
		
		Thread.sleep(1000);
		
		CenterPoint dd = PointUtil.getCenterPoint(600, 450);
		Mcv mcv = new Mcv(dd.getX()-64,dd.getY()-64,UnitColor.Pink);//基地车
		Constructor.putOneShapeUnit(mcv);
		
//		Thread.sleep(3000);
//		mcv.status = Mcv.MCV_STATUS_EXPANDING;
		
		
		
		
		CenterPoint cp1 = PointUtil.getCenterPoint(800, 600);
		CenterPoint cp2 = PointUtil.getCenterPoint(700, 600);
		CenterPoint cp3 = PointUtil.getCenterPoint(650, 650);
		CenterPoint cp4 = PointUtil.getCenterPoint(375, 620);
		
		LittleCenterPoint lcp1Up = cp1.getUpLittleCenterPoint();
		Sniper sniper1Lcp1Up = new Sniper(lcp1Up,UnitColor.Red);
		sniper1Lcp1Up.setUnitName("狙击手0");
		LittleCenterPoint lcp1Right = cp1.getRightLittleCenterPoint();
		Sniper sniper1Lcp1Right = new Sniper(lcp1Right,UnitColor.Red);
		sniper1Lcp1Right.setUnitName("狙击手2");
		
		Constructor.putOneShapeUnit(sniper1Lcp1Up);//狙击手
		Constructor.putOneShapeUnit(sniper1Lcp1Right);//狙击手
		
		
		LittleCenterPoint lcpRight = cp2.getRightLittleCenterPoint();
		Sniper sniper3 = new Sniper(lcpRight,UnitColor.Red);
		sniper3.setUnitName("狙击手3");
		Constructor.putOneShapeUnit(sniper3);//狙击手
		
		LittleCenterPoint lcpDown = cp3.getDownLittleCenterPoint();
		Sniper sniper4 = new Sniper(lcpDown,UnitColor.Red);
		sniper4.setUnitName("狙击手4");
		Constructor.putOneShapeUnit(sniper4);//狙击手
		
//		LittleCenterPoint lcp1 = cp4.getDownLittleCenterPoint();
//		Tany tany = new Tany(lcp1,UnitColor.Red);
//		Constructor.putOneShapeUnit(tany, scenePanel);//谭雅
//		
//		LittleCenterPoint lcp2 = cp4.getLeftLittleCenterPoint();
//		Tany tany2 = new Tany(lcp2,UnitColor.Blue);
//		Constructor.putOneShapeUnit(tany2, scenePanel);//谭雅
//		
//		LittleCenterPoint lcp3 = cp1.getLeftLittleCenterPoint();
//		Tany tany3 = new Tany(lcp3,UnitColor.Green);
//		Constructor.putOneShapeUnit(tany3, scenePanel);//谭雅
//		
//		LittleCenterPoint lcp4 = cp2.getLeftLittleCenterPoint();
//		Tany tany4 = new Tany(lcp4,UnitColor.Orange);
//		Constructor.putOneShapeUnit(tany4, scenePanel);//谭雅
		
//		sniper2.status = SoldierStatus.UMove;
//		sniper3.status = SoldierStatus.UMove;
//		sniper4.status = SoldierStatus.UMove;
		
//		LittleCenterPoint endTarget = PointUtil.getCenterPoint(492, 485).getLeftLittleCenterPoint();
//		sniper.nextTarget = endTarget;
//		sniper.endTarget = endTarget;
//		sniper.status = SoldierStatus.UMove;
		
//		afCnst.setToFetchCrate(true);
		
		//简单的攻击效果
		while(!targetPill.end) {
			gtank.attack(targetPill);
		}
		while(!targetPill2.end) {
			gtank.attack(targetPill2);
		}
		
//		-Xms1024m
//		-XX:+UseG1GC -XX:MaxGCPauseMillis=1
		
		
//		TankShell ts = new TankShell(200,200,500,500);
//		Constructor.putOneShapeUnit(ts, scenePanel);//炮弹
	}
}
