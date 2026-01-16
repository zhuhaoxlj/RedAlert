package redAlert.mapEditor;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import redAlert.GlobalConfig;
import redAlert.utils.RandomUtil;

/**
 * 丰富的地图生成器
 * 生成包含多种地形特征的大地图
 */
public class RichMapGenerator {

    // 地图配置
    private static final int MAP_WIDTH = 3000;
    private static final int MAP_HEIGHT = 1500;

    // 地形类型
    private enum TerrainType {
        GRASS,       // 草地
        SNOW,        // 雪地
        ROAD,        // 道路
        WATER,       // 水体
        MOUNTAIN,    // 山地
        FOREST,      // 森林
        SAND         // 沙地
    }

    // 地形权重配置
    private static final Map<TerrainType, Integer> TERRAIN_WEIGHTS = new HashMap<>();
    static {
        TERRAIN_WEIGHTS.put(TerrainType.GRASS, 45);   // 45% 草地
        TERRAIN_WEIGHTS.put(TerrainType.SNOW, 30);    // 30% 雪地
        TERRAIN_WEIGHTS.put(TerrainType.ROAD, 5);     // 5% 道路
        TERRAIN_WEIGHTS.put(TerrainType.WATER, 5);    // 5% 水体
        TERRAIN_WEIGHTS.put(TerrainType.MOUNTAIN, 5); // 5% 山地
        TERRAIN_WEIGHTS.put(TerrainType.FOREST, 5);   // 5% 森林
        TERRAIN_WEIGHTS.put(TerrainType.SAND, 5);     // 5% 沙地
    }

    // 地形到瓦片名称的映射
    private static final Map<TerrainType, List<String>> TERRAIN_TILES = new HashMap<>();
    static {
        // 雪地瓦片
        List<String> snowTiles = Arrays.asList(
            "clat01.sno", "clat02.sno", "clat03.sno", "clat04.sno",
            "clat05.sno", "clat06.sno", "clat07.sno", "clat08.sno",
            "clat09.sno", "clat10.sno", "clat11.sno", "clat12.sno",
            "clat13.sno", "clat14.sno", "clat15.sno", "clat16.sno",
            "clat01a.sno", "clat02a.sno", "clat03a.sno", "clat04a.sno",
            "clat05a.sno", "clat06a.sno", "clat07a.sno", "clat08a.sno",
            "clat09a.sno", "clat10a.sno", "clat11a.sno", "clat12a.sno",
            "clat13a.sno", "clat14a.sno", "clat15a.sno", "clat16a.sno"
        );
        TERRAIN_TILES.put(TerrainType.SNOW, snowTiles);

        // 草地瓦片（Temperate地形）
        List<String> grassTiles = Arrays.asList(
            "clat01.tem", "clat02.tem", "clat03.tem", "clat04.tem",
            "clat05.tem", "clat06.tem", "clat07.tem", "clat08.tem",
            "clat09.tem", "clat10.tem", "clat11.tem", "clat12.tem",
            "clat13.tem", "clat14.tem", "clat15.tem", "clat16.tem"
        );
        TERRAIN_TILES.put(TerrainType.GRASS, grassTiles);

        // 道路暂时使用草地瓦片
        TERRAIN_TILES.put(TerrainType.ROAD, grassTiles);

        // 水体暂时使用雪地瓦片（看起来像冰面）
        TERRAIN_TILES.put(TerrainType.WATER, snowTiles);

        // 山地暂时使用雪地瓦片
        TERRAIN_TILES.put(TerrainType.MOUNTAIN, snowTiles);

        // 森林使用草地瓦片
        TERRAIN_TILES.put(TerrainType.FOREST, grassTiles);

        // 沙地暂时使用草地瓦片
        TERRAIN_TILES.put(TerrainType.SAND, grassTiles);
    }

    /**
     * 生成完整的大地图
     */
    public static void generateRichMap() {
        System.out.println("开始生成丰富的大地图...");

        // 初始化所有中心点
        List<MapCenterPoint> allCenterPoints = initializeAllCenterPoints();
        System.out.println("初始化了 " + allCenterPoints.size() + " 个中心点");

        // 为每个中心点分配地形类型
        assignTerrainTypes(allCenterPoints);

        // 生成道路网络
        generateRoadNetwork(allCenterPoints);

        // 生成水体区域
        generateWaterBodies(allCenterPoints);

        // 生成山脉区域
        generateMountainRanges(allCenterPoints);

        // 为每个中心点分配具体的瓦片
        assignTilesToCenterPoints(allCenterPoints);

        // 保存到文件
        saveMapToFile(allCenterPoints);

        System.out.println("地图生成完成！");
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
     * 为中心点分配地形类型
     */
    private static void assignTerrainTypes(List<MapCenterPoint> points) {
        // 使用Perlin噪声类似的平滑效果
        // 创建几个地形区域中心
        List<TerrainRegion> regions = createTerrainRegions();

        for (MapCenterPoint point : points) {
            TerrainType type = determineTerrainType(point, regions);
            point.setTerrainType(type);
        }
    }

    /**
     * 创建地形区域
     */
    private static List<TerrainRegion> createTerrainRegions() {
        List<TerrainRegion> regions = new ArrayList<>();

        // 创建不同类型的区域中心
        regions.add(new TerrainRegion(500, 400, TerrainType.GRASS, 600));
        regions.add(new TerrainRegion(1500, 750, TerrainType.SNOW, 800));
        regions.add(new TerrainRegion(2500, 400, TerrainType.FOREST, 500));
        regions.add(new TerrainRegion(1000, 1200, TerrainType.SAND, 400));
        regions.add(new TerrainRegion(2200, 1100, TerrainType.MOUNTAIN, 450));
        regions.add(new TerrainRegion(800, 600, TerrainType.WATER, 350));
        regions.add(new TerrainRegion(2000, 800, TerrainType.GRASS, 550));

        return regions;
    }

    /**
     * 确定单个点的地形类型
     */
    private static TerrainType determineTerrainType(MapCenterPoint point, List<TerrainRegion> regions) {
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

        // 基于距离和随机因素决定地形
        if (minDistance < nearestRegion.radius * 0.3) {
            return nearestRegion.type;
        } else if (minDistance < nearestRegion.radius) {
            // 边缘区域有一定的随机性
            if (RandomUtil.randomInt(0, 100) < 70) {
                return nearestRegion.type;
            } else {
                return getRandomTerrainType();
            }
        } else {
            return getRandomTerrainType();
        }
    }

    /**
     * 生成道路网络
     */
    private static void generateRoadNetwork(List<MapCenterPoint> points) {
        System.out.println("生成道路网络...");

        // 主干道：从西到东
        createRoad(points, 0, 750, 3000, 750, 60);
        createRoad(points, 0, 450, 3000, 450, 50);
        createRoad(points, 0, 1100, 3000, 1100, 50);

        // 南北向道路
        createRoad(points, 600, 0, 600, 1500, 50);
        createRoad(points, 1500, 0, 1500, 1500, 60);
        createRoad(points, 2400, 0, 2400, 1500, 50);

        // 对角线道路
        createDiagonalRoad(points, 300, 300, 2700, 1200, 40);
        createDiagonalRoad(points, 2700, 300, 300, 1200, 40);
    }

    /**
     * 创建直线道路
     */
    private static void createRoad(List<MapCenterPoint> points, int x1, int y1, int x2, int y2, int width) {
        for (MapCenterPoint point : points) {
            int px = point.getX();
            int py = point.getY();

            // 计算点到线段的距离
            double distance = pointToLineDistance(px, py, x1, y1, x2, y2);
            if (distance <= width) {
                point.setTerrainType(TerrainType.ROAD);
            }
        }
    }

    /**
     * 创建对角线道路
     */
    private static void createDiagonalRoad(List<MapCenterPoint> points, int x1, int y1, int x2, int y2, int width) {
        createRoad(points, x1, y1, x2, y2, width);
    }

    /**
     * 生成水体区域
     */
    private static void generateWaterBodies(List<MapCenterPoint> points) {
        System.out.println("生成水体区域...");

        // 湖泊
        createWaterBody(points, 500, 500, 150);
        createWaterBody(points, 2200, 1300, 200);
        createWaterBody(points, 1000, 200, 100);

        // 河流
        createRiver(points, 900, 0, 900, 600, 40);
        createRiver(points, 900, 600, 1100, 900, 40);
        createRiver(points, 1100, 900, 1100, 1500, 40);
    }

    /**
     * 创建水体
     */
    private static void createWaterBody(List<MapCenterPoint> points, int centerX, int centerY, int radius) {
        for (MapCenterPoint point : points) {
            double distance = calculateDistance(point, centerX, centerY);
            if (distance <= radius) {
                // 添加一些随机性使边缘更自然
                if (distance < radius * 0.7 || RandomUtil.randomInt(0, 100) < 60) {
                    point.setTerrainType(TerrainType.WATER);
                }
            }
        }
    }

    /**
     * 创建河流
     */
    private static void createRiver(List<MapCenterPoint> points, int x1, int y1, int x2, int y2, int width) {
        createRoad(points, x1, y1, x2, y2, width);
    }

    /**
     * 生成山脉区域
     */
    private static void generateMountainRanges(List<MapCenterPoint> points) {
        System.out.println("生成山脉区域...");

        // 山脉1: 左上到右下
        createMountainRange(points, 200, 200, 1200, 800, 80);
        createMountainRange(points, 1800, 200, 2800, 600, 70);

        // 独立山峰
        createMountainPeak(points, 2700, 900, 120);
        createMountainPeak(points, 400, 1300, 100);
    }

    /**
     * 创建山脉
     */
    private static void createMountainRange(List<MapCenterPoint> points, int x1, int y1, int x2, int y2, int width) {
        for (MapCenterPoint point : points) {
            double distance = pointToLineDistance(point.getX(), point.getY(), x1, y1, x2, y2);
            if (distance <= width) {
                if (distance < width * 0.4 || RandomUtil.randomInt(0, 100) < 50) {
                    point.setTerrainType(TerrainType.MOUNTAIN);
                }
            }
        }
    }

    /**
     * 创建山峰
     */
    private static void createMountainPeak(List<MapCenterPoint> points, int centerX, int centerY, int radius) {
        for (MapCenterPoint point : points) {
            double distance = calculateDistance(point, centerX, centerY);
            if (distance <= radius) {
                if (distance < radius * 0.5 || RandomUtil.randomInt(0, 100) < 70) {
                    point.setTerrainType(TerrainType.MOUNTAIN);
                }
            }
        }
    }

    /**
     * 为中心点分配具体的瓦片
     * 使用智能匹配算法根据周围瓦片选择合适的瓦片
     */
    private static void assignTilesToCenterPoints(List<MapCenterPoint> points) {
        System.out.println("分配瓦片到中心点...");

        // 创建一个坐标到点的映射，方便快速查找
        Map<String, MapCenterPoint> pointMap = new HashMap<>();
        for (MapCenterPoint point : points) {
            pointMap.put(point.getX() + "," + point.getY(), point);
        }

        // 按照从上到下、从左到右的顺序处理，确保周围的瓦片已经被处理
        points.sort((p1, p2) -> {
            if (p1.getY() != p2.getY()) {
                return Integer.compare(p1.getY(), p2.getY());
            }
            return Integer.compare(p1.getX(), p2.getX());
        });

        for (MapCenterPoint point : points) {
            Object terrainTypeObj = point.getTerrainType();
            TerrainType terrainType;
            if (terrainTypeObj instanceof TerrainType) {
                terrainType = (TerrainType) terrainTypeObj;
            } else {
                terrainType = TerrainType.GRASS;
            }

            // 获取该地形类型的瓦片列表
            List<String> terrainTiles = TERRAIN_TILES.get(terrainType);

            // 首先尝试基于周围瓦片智能匹配
            if (shouldUseSmartMatching(point, pointMap, terrainType)) {
                String targetType = calculateTargetType(point, pointMap, terrainType);

                // 在对应地形的瓦片中查找匹配
                Tile tile = findTileInTerrainList(targetType, terrainTiles);
                if (tile != null) {
                    point.setTile(tile);
                    continue;
                }
            }

            // 如果智能匹配失败，使用该地形类型的随机瓦片
            String tileName = terrainTiles.get(RandomUtil.randomInt(0, terrainTiles.size() - 1));
            point.setTile(new Tile(tileName, "2222"));
        }
    }

    /**
     * 判断是否应该使用智能匹配
     * 对于道路、水体、山脉等特殊地形，强制使用智能匹配以保持连贯性
     */
    private static boolean shouldUseSmartMatching(MapCenterPoint point, Map<String, MapCenterPoint> pointMap, TerrainType terrainType) {
        // 道路、水体、山脉始终使用智能匹配
        if (terrainType == TerrainType.ROAD || terrainType == TerrainType.WATER || terrainType == TerrainType.MOUNTAIN) {
            return true;
        }
        // 草地和雪地也使用智能匹配，但容错性更高
        return true;
    }

    /**
     * 在特定地形的瓦片列表中查找匹配的瓦片
     */
    private static Tile findTileInTerrainList(String targetType, List<String> terrainTiles) {
        // 解析目标类型
        String a = targetType.substring(0, 1);
        String b = targetType.substring(1, 2);
        String c = targetType.substring(2, 3);
        String d = targetType.substring(3, 4);

        // 在该地形的瓦片中查找匹配的
        for (String tileName : terrainTiles) {
            // 检查这个瓦片是否在TilesSourceCenter的terrainImageList中
            for (Tile existingTile : TilesSourceCenter.terrainImageList) {
                if (existingTile.getName().equals(tileName)) {
                    String type = existingTile.getType();
                    if (isMatch(a, type, 0) && isMatch(b, type, 1) &&
                        isMatch(c, type, 2) && isMatch(d, type, 3)) {
                        return existingTile;
                    }
                    break; // 找到该瓦片后跳出内层循环
                }
            }
        }

        return null;
    }

    /**
     * 检查单个字符是否匹配
     */
    private static boolean isMatch(String target, String type, int position) {
        if (target.equals("2")) {
            return true;
        }
        return target.equals(type.substring(position, position + 1));
    }

    /**
     * 计算目标瓦片类型，基于周围已存在的瓦片和当前地形类型
     */
    private static String calculateTargetType(MapCenterPoint point, Map<String, MapCenterPoint> pointMap, TerrainType terrainType) {
        StringBuilder targetType = new StringBuilder();

        // 右上
        MapCenterPoint rightUp = getNeighborPoint(point, 30, -15, pointMap);
        if (rightUp != null && rightUp.getTile() != null && isSameTerrainType(rightUp, terrainType)) {
            targetType.append(rightUp.getTile().getLeftDownType());
        } else {
            targetType.append("2");
        }

        // 右下
        MapCenterPoint rightDown = getNeighborPoint(point, 30, 15, pointMap);
        if (rightDown != null && rightDown.getTile() != null && isSameTerrainType(rightDown, terrainType)) {
            targetType.append(rightDown.getTile().getLeftUpType());
        } else {
            targetType.append("2");
        }

        // 左下
        MapCenterPoint leftDown = getNeighborPoint(point, -30, 15, pointMap);
        if (leftDown != null && leftDown.getTile() != null && isSameTerrainType(leftDown, terrainType)) {
            targetType.append(leftDown.getTile().getRightUpType());
        } else {
            targetType.append("2");
        }

        // 左上
        MapCenterPoint leftUp = getNeighborPoint(point, -30, -15, pointMap);
        if (leftUp != null && leftUp.getTile() != null && isSameTerrainType(leftUp, terrainType)) {
            targetType.append(leftUp.getTile().getRightDownType());
        } else {
            targetType.append("2");
        }

        return targetType.toString();
    }

    /**
     * 检查邻居点是否与当前地形类型相同
     */
    private static boolean isSameTerrainType(MapCenterPoint neighbor, TerrainType currentType) {
        Object neighborTerrainObj = neighbor.getTerrainType();
        if (neighborTerrainObj instanceof TerrainType) {
            TerrainType neighborTerrain = (TerrainType) neighborTerrainObj;
            return neighborTerrain == currentType;
        }
        return false;
    }

    /**
     * 获取指定相对位置的邻居点
     */
    private static MapCenterPoint getNeighborPoint(MapCenterPoint point, int offsetX, int offsetY, Map<String, MapCenterPoint> pointMap) {
        int targetX = point.getX() + offsetX;
        int targetY = point.getY() + offsetY;
        return pointMap.get(targetX + "," + targetY);
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

    // ========== 辅助方法 ==========

    /**
     * 计算两点之间的距离
     */
    private static double calculateDistance(MapCenterPoint point, int x, int y) {
        int dx = point.getX() - x;
        int dy = point.getY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算点到线段的距离
     */
    private static double pointToLineDistance(int px, int py, int x1, int y1, int x2, int y2) {
        // 计算线段长度
        double lineLength = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        if (lineLength == 0) {
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // 计算投影比例
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (lineLength * lineLength);

        // 限制在线段范围内
        t = Math.max(0, Math.min(1, t));

        // 计算投影点
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);

        // 计算距离
        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }

    /**
     * 根据权重获取随机地形类型
     */
    private static TerrainType getRandomTerrainType() {
        int totalWeight = TERRAIN_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int random = RandomUtil.randomInt(0, totalWeight - 1);

        int currentWeight = 0;
        for (Map.Entry<TerrainType, Integer> entry : TERRAIN_WEIGHTS.entrySet()) {
            currentWeight += entry.getValue();
            if (random < currentWeight) {
                return entry.getKey();
            }
        }

        return TerrainType.GRASS;
    }

    /**
     * 地形区域类
     */
    private static class TerrainRegion {
        int centerX;
        int centerY;
        TerrainType type;
        int radius;

        TerrainRegion(int centerX, int centerY, TerrainType type, int radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.type = type;
            this.radius = radius;
        }
    }

    /**
     * 主函数 - 直接运行生成地图
     */
    public static void main(String[] args) {
        // 确保资源已加载
        TilesSourceCenter.loadResource();

        // 生成地图
        generateRichMap();
    }
}
