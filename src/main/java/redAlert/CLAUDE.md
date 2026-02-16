# 核心模块 (redAlert)

[根目录](../../CLAUDE.md) > **redAlert**

---

## 变更记录 (Changelog)

### 2026-01-17 11:20:17
- 初始化核心模块文档
- 整理启动流程与核心类职责
- 补充配置系统说明

---

## 模块职责

核心模块提供游戏的基础设施，包括：
- 程序入口与初始化 (`MainTest`)
- 渲染引擎 (`MainPanel`, `MainPanelJava`)
- 游戏上下文管理 (`GameContext`)
- 单位建造与放置 (`Constructor`)
- 系统配置 (`SysConfig`, `GlobalConfig`)
- 事件处理 (`MouseEventDeal`, `KeyBoardEventDeal`)

---

## 入口与启动

### 主入口类
```java
redAlert.MainTest
```

### 启动流程
```java
public static void main(String[] args) throws Exception {
    // 1. 创建游戏窗口
    JFrame jf = new JFrame("红色警戒");

    // 2. 初始化系统配置
    SysConfig.initSysConfig();

    // 3. 初始化鼠标指针与建造预览
    Mouse.initMouseCursor();
    Place.initPlaceRect();

    // 4. 创建渲染面板（OpenGL / Java2D）
    JPanel scenePanel = isUseOpenGL
        ? new MainPanel()      // OpenGL 渲染
        : new MainPanelJava(); // Java2D 渲染

    // 5. 创建选项面板
    OptionsPanel optionsPanel = new OptionsPanel();

    // 6. 组装 UI
    jf.add(BorderLayout.CENTER, scenePanel);
    jf.add(BorderLayout.EAST, optionsPanel);

    // 7. 初始化事件处理器
    EventHandlerManager.init();
    MouseEventDeal.init(scenePanel);
    KeyBoardEventDeal.init(scenePanel);

    // 8. 启动伤害计算线程
    ShapeUnitResourceCenter.startDamageCalculate();

    // 9. 放置初始单位（测试代码）
    // ...
}
```

---

## 对外接口

### 核心类接口

#### `MainTest` (程序入口)
```java
public class MainTest {
    // 渲染模式切换
    public static boolean isUseOpenGL = true;

    // 程序入口
    public static void main(String[] args);

    // 生成草地地图（已禁用）
    private static void generateGrassMap();
}
```

**职责：**
- 初始化游戏窗口与 UI 组件
- 配置渲染模式（OpenGL/Java2D）
- 加载初始游戏场景

#### `GameContext` (游戏上下文)
```java
public class GameContext {
    // 选项面板引用
    public static OptionsPanel optionPanel;
}
```

**职责：**
- 存储全局游戏状态
- 提供跨组件数据共享

#### `Constructor` (建造管理器)
```java
public class Constructor {
    // 放置一个建筑到场景
    public static void putOneBuilding(Building building);

    // 放置一个单位到场景
    public static void putOneShapeUnit(ShapeUnit unit);

    // 播放音效
    public static void playOneMusic(String musicName);
}
```

**职责：**
- 管理所有单位的创建与销毁
- 处理建造队列
- 播放游戏音效

#### `SysConfig` (系统配置)
```java
public class SysConfig {
    // 初始化系统参数
    public static void initSysConfig();

    // 窗口尺寸
    public static int frameWidth = 1280;
    public static int frameHeight = 720;

    // 屏幕尺寸
    public static int screenWidth;
    public static int screenHeight;
}
```

**职责：**
- 读取系统配置参数
- 初始化窗口与屏幕尺寸
- 加载游戏常量

#### `GlobalConfig` (全局配置)
```java
public class GlobalConfig {
    // 地图文件路径
    public static String mapFilePath;

    // 资源路径配置
    public static String resourcePath;
}
```

**职责：**
- 存储全局路径配置
- 管理资源加载位置

---

## 关键依赖与配置

### 依赖模块
- `redAlert.shapeObjects` - 所有单位基类
- `redAlert.event` - 事件系统
- `redAlert.ui` - 界面组件
- `redAlert.utils` - 工具类
- `redAlert.resourceCenter` - 资源管理

### 渲染模式配置

**OpenGL 模式 (推荐)**
- 使用 JOGL (Java Binding for OpenGL)
- 高性能硬件加速渲染
- 支持大量单位同屏
- 类：`MainPanel`

**Java2D 模式 (兼容)**
- 使用 Swing Graphics2D
- 无需额外依赖
- 性能较低
- 类：`MainPanelJava`

### 线程模型
```
主线程 (AWT Event Dispatch Thread)
  ├─ UI 事件处理
  └─ 组件重绘

渲染线程 (MainPanel Rendering Thread)
  ├─ 60 FPS 持续渲染
  └─ 按优先级绘制单位

帧计算线程池 (ShapeUnit Calculate Thread Pool)
  ├─ 计算下一帧动画
  ├─ 处理移动逻辑
  └─ 处理攻击逻辑

伤害计算线程 (Damage Calculate Thread)
  └─ 计算战斗伤害
```

---

## 数据模型

### 窗口布局
```
┌─────────────────────────────────────────┐
│                                          │
│          MainPanel (场景渲染)            │
│            - 地图地形                    │
│            - 游戏单位                    │
│            - 特效动画                    │
│                                          │
├──────────────────┬──────────────────────┤
│                  │   OptionsPanel       │
│                  │   ┌──────────────┐  │
│                  │   │ Tab 0: 建筑   │  │
│                  │   ├──────────────┤  │
│                  │   │ Tab 1: 防御   │  │
│                  │   ├──────────────┤  │
│                  │   │ Tab 2: 步兵   │  │
│                  │   ├──────────────┤  │
│                  │   │ Tab 3: 车辆   │  │
│                  │   └──────────────┘  │
│                  │                      │
│                  │   [电力] [雷达]      │
│                  │   [出售] [维修]      │
└──────────────────┴──────────────────────┘
```

---

## 测试与质量

### 测试类
- `MainTest` - 主程序入口（包含测试代码）
- `Atest` - 临时测试类
- `Test` - 通用测试类

### 测试场景
当前 `MainTest` 包含以下测试场景：
- 建筑建造动画
- 坦克移动与攻击
- 狙击手寻路
- 核弹发射（已注释）
- 基地车展开（已注释）

---

## 常见问题 (FAQ)

**Q: 如何切换渲染模式？**

A: 修改 `MainTest.isUseOpenGL` 变量：
```java
public static boolean isUseOpenGL = false;  // 改为 false 使用 Java2D
```

**Q: 如何调整窗口大小？**

A: 修改 `SysConfig` 中的常量：
```java
public static int frameWidth = 1920;
public static int frameHeight = 1080;
```

**Q: 如何修改默认地图？**

A: 修改 `GlobalConfig.mapFilePath`：
```java
public static String mapFilePath = "path/to/your/map.txt";
```

**Q: 如何禁用启动测试代码？**

A: 注释掉 `MainTest.main()` 中的单位放置代码：
```java
// Constructor.putOneBuilding(afPill);  // 注释掉测试建筑
```

---

## 相关文件清单

### 核心类
- `MainTest.java` - 程序入口
- `MainPanel.java` - OpenGL 渲染面板
- `MainPanelJava.java` - Java2D 渲染面板
- `GameContext.java` - 游戏上下文
- `Constructor.java` - 建造管理器
- `SysConfig.java` - 系统配置
- `GlobalConfig.java` - 全局配置
- `MouseEventDeal.java` - 鼠标事件处理
- `KeyBoardEventDeal.java` - 键盘事件处理
- `MusicPlayer.java` - 音效播放器
- `OptionsPanel.java` - 选项面板
- `ShapeUnitFrame.java` - 帧数据结构
- `CustomToolTip.java` - 自定义提示框

### 测试类
- `Test.java`
- `Atest.java`

---

*本模块文档由 AI 自适应文档生成系统维护*
