# 地图编辑器模块 (mapEditor)

[根目录](../../../CLAUDE.md) > [redAlert](../) > **mapEditor**

---

## 变更记录 (Changelog)

### 2026-01-17 11:20:17
- 初始化模块文档
- 识别核心组件与接口
- 整理地图生成算法说明

---

## 模块职责

地图编辑器模块负责游戏地图的可视化编辑、随机地图生成、地形渲染与瓦片管理。支持：
- 可视化地图编辑（鼠标点击放置瓦片）
- 随机地图生成（纯草地/完整地形）
- 地形瓦片智能拼接与边匹配
- 地图文件读写（`.map` 格式）

---

## 入口与启动

### 主入口类
```java
// 编辑器测试入口
redAlert.mapEditor.MapEditorTest

// 面板组件
redAlert.mapEditor.MapEditorPanel
```

### 使用示例
```java
// 启动地图编辑器
MapEditorTest.main(args);

// 创建随机草地地图
RandomMapGenerate.generateGrassMap();

// 创建完整地形地图
RichMapGenerator.main(args);
```

---

## 对外接口

### 核心类接口

#### `MapEditorPanel`
```java
public class MapEditorPanel extends JPanel {
    // 地图瓦片网格
    private Map<String, Tile> tileMap;

    // 当前选中的瓦片类型
    private Tile selectedTile;

    // 鼠标事件处理
    private MapMouseEventDeal mouseDeal;

    // 键盘事件处理
    private MapKeyBoardEventDeal keyBoardDeal;
}
```

**职责：**
- 渲染地图编辑界面
- 处理鼠标点击放置瓦片
- 支持快捷键操作（保存/加载）

#### `RandomMapGenerate`
```java
public class RandomMapGenerate {
    // 生成纯草地地图
    public static void generateGrassMap();

    // 使用噪声算法创建自然地形
    private static double calculateNoise(int m, int n, double frequency);

    // 平滑地形过渡
    private static void smoothTerrain(String[][] tileMap);
}
```

**职责：**
- 使用 Perlin 噪思想生成随机地图
- 区域化地形 + 平滑过渡算法
- 支持多种草地瓦片（clat01-16）

#### `CompleteMapGenerator`
```java
public class CompleteMapGenerator {
    // 生成包含所有地形类型的完整地图
    public static void main(String[] args);

    // 地形分布配置
    private static void generateWithTerrainTypes();
}
```

**职责：**
- 生成包含草地、沙地、雪地、水面等复合地形
- 智能边匹配确保瓦片无缝衔接

#### `Tile`
```java
public class Tile {
    // 瓦片坐标
    private int x, y;

    // 瓦片类型文件名（如 "clat01.tem"）
    private String tileType;

    // 地形类别
    private TerrainType terrainType;

    // 瓦片图片
    private BufferedImage image;
}
```

**职责：**
- 表示单个地图瓦片
- 存储瓦片类型与渲染图像
- 支持菱形网格坐标转换

---

## 关键依赖与配置

### 依赖模块
- `redAlert.utils` - 坐标系统 (`MapCenterPointUtil`)
- `redAlert.enums` - 地形类型枚举 (`TerrainType`, `RampType`)
- `redAlert.other` - 地形渲染组件 (`TerrainJLabel`)

### 配置文件
地图文件路径（在 `GlobalConfig` 中定义）：
```java
public static String mapFilePath = "src/main/resources/temp/map.txt";
```

### 资源文件
- `src/main/resources/temp/*.tem` - 地形瓦片文件
- `src/main/resources/temp/*.map` - 地图数据文件

---

## 数据模型

### 地图文件格式
```
x,y,tileType$
30,15,clat01.tem$
90,15,clat02.tem$
...
```
- 每行一个瓦片：`x坐标,y坐标,瓦片文件名$`
- 支持两类中心点（菱形网格）

### 地形类型枚举
```java
public enum TerrainType {
    GRASS,    // 草地
    SAND,     // 沙地
    SNOW,     // 雪地
    WATER,    // 水面
    RAMP,     // 斜坡
    CLIFF     // 悬崖
}
```

### 瓦片命名规范
- `clat*.tem` - 草地瓦片（16 种）
- `csand*.tem` - 沙地瓦片
- `csnow*.tem` - 雪地瓦片
- `cw*.tem` - 水面瓦片

---

## 算法与实现

### 随机地图生成算法（草地地图）

**第一阶段：区域化地形**
```java
// 使用低频噪声创建大的连续区域
double noise = calculateNoise(m, n, 0.1);

// 将噪声映射到瓦片索引
if (noise < 0.3) {
    tileIndex = 0;  // clat01 - 最常见
} else if (noise < 0.5) {
    tileIndex = 1 + (int)((noise - 0.3) * 10) % 7;  // clat02-08
} else {
    tileIndex = 8 + (int)((noise - 0.5) * 16) % 8;  // clat09-16
}
```

**第二阶段：平滑过渡**
```java
// 统计 8 个邻居的瓦片类型
for (int dm = -1; dm <= 1; dm++) {
    for (int dn = -1; dn <= 1; dn++) {
        // 统计邻居瓦片出现次数
        neighborCount.put(tile, neighborCount.getOrDefault(tile, 0) + 1);
    }
}

// 70% 概率跟随最常见的邻居瓦片
if (random.nextDouble() < 0.7) {
    newTileMap[m][n] = mostCommon;
}
```

**效果：**
- 创建大的连续地形区域（模拟真实地图）
- 瓦片之间平滑过渡，避免突兀边界
- 保留一定的随机变化（30% 保持原样）

### 智能边匹配算法

完整地形地图使用智能边匹配确保瓦片无缝衔接：
- 分析瓦片四边类型（草地/沙地/水面）
- 选择匹配的过渡瓦片（如 `grass_to_sand_01.tem`）
- 递归处理角落与复杂边界

---

## 测试与质量

### 测试类
- `MapEditorTest` - 编辑器主测试类
- `RandomMapGenerate` - 随机生成器测试

### 手动测试流程
1. 运行 `MapEditorTest` 启动编辑器
2. 右侧面板选择瓦片类型
3. 左键点击地图放置瓦片
4. Ctrl+S 保存地图
5. Ctrl+O 加载地图

### 已知问题
- 编辑器撤销功能未实现
- 地图预览缩放功能缺失
- 大地图加载性能待优化

---

## 常见问题 (FAQ)

**Q: 如何添加新地形类型？**

A:
1. 在 `TerrainType` 枚举添加新类型
2. 准备对应的 `.tem` 瓦片文件放入 `resources/temp/`
3. 在 `TilesSourceCenter` 注册新瓦片
4. 更新 `RandomMapGenerate` 生成逻辑

**Q: 地图文件过大如何优化？**

A:
- 使用瓦片索引代替文件名（减少文件大小）
- 实现地图分块加载（Chunk 系统）
- 压缩存储（Zip/Gzip）

**Q: 如何实现地形碰撞检测？**

A:
- 使用 `OverlayType` 枚举标记不可通行区域
- 在 `MoveUtil` 寻路时检查瓦片 `passable` 属性
- 水面、悬崖等标记为阻挡

---

## 相关文件清单

### 核心类
- `MapEditorPanel.java` - 编辑器主面板
- `MapEditorTest.java` - 编辑器入口
- `MapMouseEventDeal.java` - 鼠标事件处理
- `MapKeyBoardEventDeal.java` - 键盘事件处理
- `RandomMapGenerate.java` - 随机地图生成器
- `CompleteMapGenerator.java` - 完整地形生成器
- `RichMapGenerator.java` - 富地图生成器
- `SimpleMapGenerator.java` - 简单地图生成器
- `Tile.java` - 瓦片数据结构
- `TerrainJLabel.java` - 地形渲染组件
- `TilesSelectPanel.java` - 瓦片选择面板
- `MapCenterPointUtil.java` - 地图坐标工具
- `MapCenterPoint.java` - 地图中心点
- `TilesSourceCenter.java` - 瓦片资源中心
- `MapDiagnostic.java` - 地图诊断工具

### 资源目录
- `src/main/resources/temp/` - 地形瓦片文件
- `src/main/resources/temp/*.map` - 地图数据文件

---

*本模块文档由 AI 自适应文档生成系统维护*
