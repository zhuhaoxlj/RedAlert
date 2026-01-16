package redAlert.mapEditor;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 地图文件诊断工具
 */
public class MapDiagnostic {

    public static void main(String[] args) {
        try {
            // 读取地图文件
            String mapText = FileUtils.readFileToString(new File("test_map.text"), "UTF-8");
            String[] strs = StringUtils.split(mapText, "$");

            System.out.println("=== 地图文件诊断 ===");
            System.out.println("总地块数: " + strs.length);
            System.out.println();

            // 统计瓦片类型分布
            java.util.Map<String, Integer> tileCount = new java.util.HashMap<>();
            java.util.Map<String, Integer> positionCount = new java.util.HashMap<>();

            for (int i = 0; i < Math.min(100, strs.length); i++) {
                String info = strs[i];
                if (StringUtils.isBlank(info)) {
                    continue;
                }

                String[] infos = StringUtils.split(info, ",");
                if (infos.length < 3) {
                    continue;
                }

                int x = Integer.valueOf(infos[0].trim());
                int y = Integer.valueOf(infos[1].trim());
                String name = infos[2].trim();

                // 统计瓦片类型
                tileCount.put(name, tileCount.getOrDefault(name, 0) + 1);

                // 记录前10个位置
                if (i < 10) {
                    positionCount.put(x + "," + y + " -> " + name, 1);
                }
            }

            System.out.println("前10个地块详情:");
            int count = 0;
            for (String info : strs) {
                if (StringUtils.isBlank(info)) {
                    continue;
                }
                String[] infos = StringUtils.split(info, ",");
                if (infos.length < 3) {
                    continue;
                }
                int x = Integer.valueOf(infos[0].trim());
                int y = Integer.valueOf(infos[1].trim());
                String name = infos[2].trim();

                // 计算tileIndex（模拟MainPanel的逻辑）
                java.util.List<String> terrainNameList = new java.util.ArrayList<>();
                terrainNameList.add("clat01.sno");
                terrainNameList.add("clat02.sno");
                terrainNameList.add("clat03.sno");
                terrainNameList.add("clat04.sno");
                terrainNameList.add("clat05.sno");
                terrainNameList.add("clat06.sno");
                terrainNameList.add("clat07.sno");
                terrainNameList.add("clat08.sno");
                terrainNameList.add("clat09.sno");
                terrainNameList.add("clat10.sno");
                terrainNameList.add("clat11.sno");
                terrainNameList.add("clat12.sno");
                terrainNameList.add("clat13.sno");
                terrainNameList.add("clat14.sno");
                terrainNameList.add("clat15.sno");
                terrainNameList.add("clat16.sno");
                terrainNameList.add("clat01a.sno");
                terrainNameList.add("clat02a.sno");
                terrainNameList.add("clat03a.sno");
                terrainNameList.add("clat04a.sno");
                terrainNameList.add("clat05a.sno");
                terrainNameList.add("clat06a.sno");
                terrainNameList.add("clat07a.sno");
                terrainNameList.add("clat08a.sno");
                terrainNameList.add("clat09a.sno");
                terrainNameList.add("clat10a.sno");
                terrainNameList.add("clat11a.sno");
                terrainNameList.add("clat12a.sno");
                terrainNameList.add("clat13a.sno");
                terrainNameList.add("clat14a.sno");
                terrainNameList.add("clat15a.sno");
                terrainNameList.add("clat16a.sno");

                int index = terrainNameList.indexOf(name);

                System.out.println(String.format("位置(%4d,%4d) -> %-15s -> tileIndex=%2d", x, y, name, index));

                count++;
                if (count >= 20) {
                    break;
                }
            }

            System.out.println();
            System.out.println("瓦片类型分布统计（前20个地块）:");
            tileCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));

            System.out.println();
            System.out.println("=== 诊断完成 ===");
            System.out.println();
            System.out.println("如果前20个地块的tileIndex都相同，说明地图生成有问题。");
            System.out.println("如果tileIndex不同但游戏显示相同，说明是渲染或缓存问题。");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
