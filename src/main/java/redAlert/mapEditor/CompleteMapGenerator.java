package redAlert.mapEditor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

import redAlert.GlobalConfig;
import redAlert.enums.OverlayType;
import redAlert.enums.TerrainType;
import redAlert.utils.RandomUtil;

/**
 * 完整地图生成器 - 支持所有红警地形类型
 *
 * 地形系统说明:
 * 1. 基础地形瓦片 - 雪地(.sno)和草地(.tem)
 * 2. 地形类型 - Rough(野地), Road(道路), Clear(干净地面), Rock(岩石), Water(水面), Beach(沙滩)
 * 3. 覆盖物 - Tree(树), Rock(岩石), Bridge(桥), Tiberium(矿石), Crate(箱子)等
 */
public class CompleteMapGenerator {

    // 地形区域定义
    private static class TerrainRegion {
        int centerX;
        int centerY;
        TerrainType terrainType;
        int radius;

        TerrainRegion(int centerX, int centerY, TerrainType terrainType, int radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.terrainType = terrainType;
            this.radius = radius;
        }
    }

    // 覆盖物区域定义
    private static class OverlayRegion {
        int centerX;
        int centerY;
        OverlayType overlayType;
        int radius;
        int density; // 密度 0-100

        OverlayRegion(int centerX, int centerY, OverlayType overlayType, int radius, int density) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.overlayType = overlayType;
            this.radius = radius;
            this.density = density;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 开始生成完整地形地图 ===");
        System.out.println("地图大小: 3000x1500");
        System.out.println("支持地形: 草地、雪地、道路、水面、岩石、沙滩");
        System.out.println("支持覆盖物: 树木、矿石、岩石、箱子\n");

        // 初始化所有中心点
        List<MapCenterPoint> allCenterPoints = initializeAllCenterPoints();
        System.out.println("✓ 初始化了 " + allCenterPoints.size() + " 个中心点");

        // 定义地形区域
        List<TerrainRegion> terrainRegions = createTerrainRegions();
        System.out.println("✓ 创建了 " + terrainRegions.size() + " 个地形区域");

        // 定义覆盖物区域
        List<OverlayRegion> overlayRegions = createOverlayRegions();
        System.out.println("✓ 创建了 " + overlayRegions.size() + " 个覆盖物区域");

        // 为每个中心点分配地形类型和覆盖物
        assignTerrainAndOverlay(allCenterPoints, terrainRegions, overlayRegions);
        System.out.println("✓ 分配地形和覆盖物");

        // 为每个中心点选择瓦片
        assignTiles(allCenterPoints);
        System.out.println("✓ 分配瓦片");

        // 保存地图
        saveMapToFile(allCenterPoints);
        System.out.println("✓ 保存地图文件");

        // 打印统计信息
        printStatistics(allCenterPoints);

        System.out.println("\n=== 地图生成完成! ===");
    }

    /**
     * 创建地形区域
     */
    private static List<TerrainRegion> createTerrainRegions() {
        List<TerrainRegion> regions = new ArrayList<>();

        // 大片草地/雪地 (Rough - 野地)
        regions.add(new TerrainRegion(300, 300, TerrainType.Rough, 800));  // 左上雪地
        regions.add(new TerrainRegion(2400, 1200, TerrainType.Rough, 700)); // 右下雪地
        regions.add(new TerrainRegion(1500, 750, TerrainType.Rough, 900)); // 中间草地
        regions.add(new TerrainRegion(500, 1200, TerrainType.Rough, 500)); // 左下草地
        regions.add(new TerrainRegion(2500, 300, TerrainType.Rough, 400)); // 右上雪地

        // 道路网络
        regions.add(new TerrainRegion(1500, 750, TerrainType.Road, 100));  // 主干道十字
        regions.add(new TerrainRegion(1500, 400, TerrainType.Road, 60));   // 上路
        regions.add(new TerrainRegion(1500, 1100, TerrainType.Road, 60));  // 下路
        regions.add(new TerrainRegion(1000, 750, TerrainType.Road, 60));   // 左路
        regions.add(new TerrainRegion(2000, 750, TerrainType.Road, 60));   // 右路

        // 水面/湖泊
        regions.add(new TerrainRegion(800, 500, TerrainType.Water, 150));   // 左上湖
        regions.add(new TerrainRegion(2200, 1000, TerrainType.Water, 120)); // 右下湖

        // 岩石山
        regions.add(new TerrainRegion(600, 900, TerrainType.Rock, 100));    // 左岩石区
        regions.add(new TerrainRegion(2400, 400, TerrainType.Rock, 80));    // 右上岩石区

        // 沙滩
        regions.add(new TerrainRegion(750, 580, TerrainType.Beach, 30));    // 左上湖边沙滩
        regions.add(new TerrainRegion(2150, 1080, TerrainType.Beach, 30));  // 右下湖边沙滩

        // 干净地面(基地区域)
        regions.add(new TerrainRegion(1500, 750, TerrainType.Clear, 150));  // 中心广场

        return regions;
    }

    /**
     * 创建覆盖物区域
     */
    private static List<OverlayRegion> createOverlayRegions() {
        List<OverlayRegion> regions = new ArrayList<>();

        // 森林区域
        regions.add(new OverlayRegion(300, 300, OverlayType.Tree, 400, 70));    // 左上森林
        regions.add(new OverlayRegion(500, 1200, OverlayType.Tree, 300, 60));   // 左下树林
        regions.add(new OverlayRegion(2500, 300, OverlayType.Tree, 250, 50));   // 右上小树林

        // 矿石区
        regions.add(new OverlayRegion(1000, 600, OverlayType.Tiberium, 80, 60)); // 左上矿
        regions.add(new OverlayRegion(2000, 900, OverlayType.Tiberium, 80, 60)); // 右下矿

        // 岩石障碍
        regions.add(new OverlayRegion(600, 900, OverlayType.Rock, 80, 40));     // 岩石区障碍
        regions.add(new OverlayRegion(2400, 400, OverlayType.Rock, 60, 40));    // 右上岩石障碍

        // 箱子(奖励)
        regions.add(new OverlayRegion(1500, 750, OverlayType.Crate, 200, 5));    // 中心稀疏箱子

        return regions;
    }

    /**
     * 为中心点分配地形类型和覆盖物
     */
    private static void assignTerrainAndOverlay(List<MapCenterPoint> points,
                                                List<TerrainRegion> terrainRegions,
                                                List<OverlayRegion> overlayRegions) {
        for (MapCenterPoint point : points) {
            // 分配地形类型
            TerrainType terrainType = determineTerrainType(point, terrainRegions);
            point.setTerrainType(terrainType);

            // 分配覆盖物
            OverlayType overlayType = determineOverlayType(point, overlayRegions, terrainType);
            point.setOverlayType(overlayType);
        }
    }

    /**
     * 确定地形类型
     */
    private static TerrainType determineTerrainType(MapCenterPoint point, List<TerrainRegion> regions) {
        TerrainRegion nearestRegion = null;
        double minDistance = Double.MAX_VALUE;

        // 找最近的区域
        for (TerrainRegion region : regions) {
            double distance = calculateDistance(point, region.centerX, region.centerY);
            if (distance < minDistance) {
                minDistance = distance;
                nearestRegion = region;
            }
        }

        // 如果没有找到区域,返回默认野地
        if (nearestRegion == null) {
            return TerrainType.Rough;
        }

        // 根据距离决定是否采用该地形
        if (minDistance < nearestRegion.radius * 0.4) {
            // 区域中心: 强制使用该地形
            return nearestRegion.terrainType;
        } else if (minDistance < nearestRegion.radius * 0.8) {
            // 区域边缘: 高概率使用
            if (RandomUtil.randomInt(0, 100) < 85) {
                return nearestRegion.terrainType;
            }
        }

        // 远离区域: 随机选择,但偏向野地
        if (RandomUtil.randomInt(0, 100) < 70) {
            return TerrainType.Rough;
        } else {
            return TerrainType.Clear;
        }
    }

    /**
     * 确定覆盖物类型
     */
    private static OverlayType determineOverlayType(MapCenterPoint point,
                                                    List<OverlayRegion> regions,
                                                    TerrainType terrainType) {
        // 某些地形不允许覆盖物
        if (terrainType == TerrainType.Water || terrainType == TerrainType.Beach) {
            return OverlayType.None;
        }

        OverlayType nearestType = OverlayType.None;
        double minDistance = Double.MAX_VALUE;
        int density = 0;

        // 找最近的覆盖物区域
        for (OverlayRegion region : regions) {
            double distance = calculateDistance(point, region.centerX, region.centerY);
            if (distance < region.radius && distance < minDistance) {
                minDistance = distance;
                nearestType = region.overlayType;
                density = region.density;
            }
        }

        // 根据密度决定是否放置覆盖物
        if (nearestType != OverlayType.None && RandomUtil.randomInt(0, 100) < density) {
            return nearestType;
        }

        return OverlayType.None;
    }

    /**
     * 为每个中心点选择瓦片
     */
    private static void assignTiles(List<MapCenterPoint> points) {
        for (MapCenterPoint point : points) {
            TerrainType terrainType = (TerrainType) point.getTerrainType();
            String tileName;

            // 根据地形类型选择瓦片
            if (terrainType == TerrainType.Water) {
                // 水面 - 使用草地瓦片(后续可添加专门的水面瓦片)
                tileName = getRandomGrassTile();
            } else if (terrainType == TerrainType.Rock || terrainType == TerrainType.Beach) {
                // 岩石/沙滩 - 使用雪地瓦片
                tileName = getRandomSnowTile();
            } else if (terrainType == TerrainType.Road) {
                // 道路 - 使用草地瓦片
                tileName = getRandomGrassTile();
            } else {
                // Rough/Clear - 随机选择草地或雪地
                if (RandomUtil.randomInt(0, 100) < 50) {
                    tileName = getRandomGrassTile();
                } else {
                    tileName = getRandomSnowTile();
                }
            }

            // 创建瓦片
            Tile tile = new Tile(tileName, "2222");
            point.setTile(tile);
        }
    }

    /**
     * 获取随机草地瓦片
     */
    private static String getRandomGrassTile() {
        int variants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        int variant = variants[RandomUtil.randomInt(0, 15)];
        return "clat" + String.format("%02d", variant) + ".tem";
    }

    /**
     * 获取随机雪地瓦片
     */
    private static String getRandomSnowTile() {
        if (RandomUtil.randomInt(0, 100) < 50) {
            // 基础雪地
            int variants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
            int variant = variants[RandomUtil.randomInt(0, 15)];
            return "clat" + String.format("%02d", variant) + ".sno";
        } else {
            // 雪地变种
            int variants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
            int variant = variants[RandomUtil.randomInt(0, 15)];
            return "clat" + String.format("%02d", variant) + "a.sno";
        }
    }

    /**
     * 计算两点距离
     */
    private static double calculateDistance(MapCenterPoint point, int centerX, int centerY) {
        int dx = point.getX() - centerX;
        int dy = point.getY() - centerY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 初始化所有中心点
     */
    private static List<MapCenterPoint> initializeAllCenterPoints() {
        List<MapCenterPoint> points = new ArrayList<>();

        // 一类中心点 (y从15开始,间隔30)
        for (int m = 0; m < 50; m++) {
            int y = 15 + 30 * m;
            for (int n = 0; n < 50; n++) {
                int x = 30 + 60 * n;
                MapCenterPoint mcp = MapCenterPointUtil.fetchCenterPoint(x, y);
                if (mcp != null) {
                    points.add(mcp);
                }
            }
        }

        // 二类中心点 (y从0开始,间隔30)
        for (int m = 0; m < 50; m++) {
            int y = 30 * m;
            for (int n = 0; n < 50; n++) {
                int x = 60 * n;
                MapCenterPoint mcp = MapCenterPointUtil.fetchCenterPoint(x, y);
                if (mcp != null) {
                    points.add(mcp);
                }
            }
        }

        return points;
    }

    /**
     * 保存地图到文件
     */
    private static void saveMapToFile(List<MapCenterPoint> points) {
        System.out.println("  保存地图到文件...");

        try {
            File mapFile = new File(GlobalConfig.mapFilePath);

            // 构建地图数据
            // 格式: x,y,tileName,terrainType,overlayType$
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < points.size(); i++) {
                MapCenterPoint point = points.get(i);
                TerrainType terrainType = (TerrainType) point.getTerrainType();
                OverlayType overlayType = point.getOverlayType();

                String entry = point.getX() + "," +
                              point.getY() + "," +
                              point.getTile().getName() + "," +
                              terrainType.name() + "," +
                              overlayType.name();

                if (i < points.size() - 1) {
                    text.append(entry).append("$");
                } else {
                    text.append(entry);
                }
            }

            FileUtils.writeStringToFile(mapFile, text.toString(), "UTF-8");
            System.out.println("  地图已保存: " + mapFile.getAbsolutePath());
            System.out.println("  文件大小: " + mapFile.length() / 1024 + " KB");
        } catch (Exception e) {
            System.err.println("  保存地图文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 打印统计信息
     */
    private static void printStatistics(List<MapCenterPoint> points) {
        System.out.println("\n=== 地形统计 ===");

        Map<TerrainType, Integer> terrainCount = new HashMap<>();
        Map<OverlayType, Integer> overlayCount = new HashMap<>();

        for (MapCenterPoint point : points) {
            TerrainType terrainType = (TerrainType) point.getTerrainType();
            OverlayType overlayType = point.getOverlayType();

            terrainCount.put(terrainType, terrainCount.getOrDefault(terrainType, 0) + 1);
            overlayCount.put(overlayType, overlayCount.getOrDefault(overlayType, 0) + 1);
        }

        System.out.println("\n地形类型分布:");
        for (TerrainType type : TerrainType.values()) {
            int count = terrainCount.getOrDefault(type, 0);
            double percent = (count * 100.0) / points.size();
            System.out.println(String.format("  %-12s: %6d (%5.1f%%)", type.desc, count, percent));
        }

        System.out.println("\n覆盖物分布:");
        for (OverlayType type : OverlayType.values()) {
            int count = overlayCount.getOrDefault(type, 0);
            if (count > 0) {
                double percent = (count * 100.0) / points.size();
                System.out.println(String.format("  %-12s: %6d (%5.1f%%)", type.desc, count, percent));
            }
        }
    }
}
