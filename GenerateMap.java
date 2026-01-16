import redAlert.mapEditor.*;
import redAlert.GlobalConfig;
import redAlert.utils.RandomUtil;

/**
 * 地图生成器 - 独立运行生成真实地图
 */
public class GenerateMap {

    public static void main(String[] args) throws Exception {
        System.out.println("开始生成地图...");

        // 加载地形资源
        TilesSourceCenter.loadResource();
        System.out.println("地形资源加载完成");

        // 生成随机地图
        RandomMapGenerate.randomGenerate();
        System.out.println("地图生成完成！");

        // 显示统计信息
        System.out.println("生成的地形图块数量: " + RandomMapGenerate.allMcps.size());
        System.out.println("地图文件路径: " + GlobalConfig.mapFilePath);

        System.exit(0);
    }
}
