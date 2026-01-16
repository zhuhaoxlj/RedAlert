package redAlert.mapEditor;

import java.io.File;
import java.util.*;

import org.apache.commons.io.FileUtils;

import redAlert.GlobalConfig;
import redAlert.utils.RandomUtil;

/**
 * 简化版地图生成器 - 生成有明显地形分区的地图
 */
public class SimpleMapGenerator {

    // 地形类型
    private enum TileType {
        GRASS,   // 草地 - 使用.tem文件
        SNOW     // 雪地 - 使用.sno文件
    }

    public static void generateMap() {
        System.out.println("开始生成简化版地图...");

        // 初始化所有中心点
        List<MapCenterPoint> allCenterPoints = initializeAllCenterPoints();
        System.out.println("初始化了 " + allCenterPoints.size() + " 个中心点");

        // 定义地形区域（简化为几个大的连续区域）
        List<TerrainRegion> regions = createTerrainRegions();

        // 为每个中心点分配地形
        assignTerrainToPoints(allCenterPoints, regions);

        // 保存地图
        saveMapToFile(allCenterPoints);

        System.out.println("地图生成完成！");
    }

    /**
     * 创建大的地形区域
     */
    private static List<TerrainRegion> createTerrainRegions() {
        List<TerrainRegion> regions = new ArrayList<>();

        // 左上角：大片雪地区域
        regions.add(new TerrainRegion(300, 300, TileType.SNOW, 800));

        // 右下角：大片雪地区域
        regions.add(new TerrainRegion(2400, 1200, TileType.SNOW, 700));

        // 中间：大片草地区域
        regions.add(new TerrainRegion(1500, 750, TileType.GRASS, 900));

        // 左下：小片草地
        regions.add(new TerrainRegion(500, 1200, TileType.GRASS, 500));

        // 右上：小片雪地
        regions.add(new TerrainRegion(2500, 300, TileType.SNOW, 400));

        return regions;
    }

    /**
     * 为中心点分配地形
     */
    private static void assignTerrainToPoints(List<MapCenterPoint> points, List<TerrainRegion> regions) {
        for (MapCenterPoint point : points) {
            // 找到最近的区域
            TerrainRegion nearestRegion = null;
            double minDistance = Double.MAX_VALUE;

            for (TerrainRegion region : regions) {
                double distance = calculateDistance(point, region.centerX, region.centerY);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestRegion = region;
                }
            }

            // 基于距离决定地形
            TileType tileType;
            if (minDistance < nearestRegion.radius * 0.5) {
                // 区域中心：强制使用该区域的地形
                tileType = nearestRegion.type;
            } else if (minDistance < nearestRegion.radius) {
                // 区域边缘：有一定随机性，但倾向该区域地形
                if (RandomUtil.randomInt(0, 100) < 80) {
                    tileType = nearestRegion.type;
                } else {
                    // 随机选择另一种地形
                    tileType = (nearestRegion.type == TileType.GRASS) ? TileType.SNOW : TileType.GRASS;
                }
            } else {
                // 远离所有区域：随机选择，但偏向草地
                tileType = (RandomUtil.randomInt(0, 100) < 60) ? TileType.GRASS : TileType.SNOW;
            }

            // 根据地形类型选择瓦片
            String tileName;
            if (tileType == TileType.GRASS) {
                // 草地瓦片（16种）
                int grassVariants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
                int variant = grassVariants[RandomUtil.randomInt(0, 15)];
                tileName = "clat" + String.format("%02d", variant) + ".tem";
            } else {
                // 雪地瓦片（32种）
                if (RandomUtil.randomInt(0, 100) < 50) {
                    // 基础雪地
                    int snowVariants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
                    int variant = snowVariants[RandomUtil.randomInt(0, 15)];
                    tileName = "clat" + String.format("%02d", variant) + ".sno";
                } else {
                    // 雪地变种
                    int snowVariants[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
                    int variant = snowVariants[RandomUtil.randomInt(0, 15)];
                    tileName = "clat" + String.format("%02d", variant) + "a.sno";
                }
            }

            // 创建Tile并设置
            Tile tile = new Tile(tileName, "2222");
            point.setTile(tile);
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

        // 一类中心点 (y从15开始，间隔30)
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

        // 二类中心点 (y从0开始，间隔30)
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
        System.out.println("保存地图到文件...");

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            MapCenterPoint point = points.get(i);
            if (i < points.size() - 1) {
                text.append(point.getX())
                    .append(",")
                    .append(point.getY())
                    .append(",")
                    .append(point.getTile().getName())
                    .append("$");
            } else {
                text.append(point.getX())
                    .append(",")
                    .append(point.getY())
                    .append(",")
                    .append(point.getTile().getName());
            }
        }

        try {
            File mapFile = new File(GlobalConfig.mapFilePath);
            FileUtils.writeStringToFile(mapFile, text.toString(), "UTF-8");
            System.out.println("地图已保存到: " + mapFile.getAbsolutePath());
            System.out.println("文件大小: " + mapFile.length() / 1024 + " KB");
        } catch (Exception e) {
            System.err.println("保存地图文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 地形区域类
     */
    private static class TerrainRegion {
        int centerX;
        int centerY;
        TileType type;
        int radius;

        TerrainRegion(int centerX, int centerY, TileType type, int radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.type = type;
            this.radius = radius;
        }
    }

    public static void main(String[] args) {
        generateMap();
    }
}
