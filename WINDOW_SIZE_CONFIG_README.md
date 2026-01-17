# 游戏窗口大小配置指南

## 功能概述

新增灵活的窗口大小配置系统，支持多种预设模式和自定义尺寸，可根据不同屏幕分辨率自动调整。

## 可用模式

### 1. FULLSCREEN（全屏模式）
- 窗口大小：屏幕分辨率 100%
- 适用场景：沉浸式游戏体验
- 示例：1920x1080 屏幕 → 1920x1080 窗口

### 2. LARGE（默认模式）
- 窗口大小：屏幕宽度 80% × 屏幕高度 70%
- 适用场景：通用模式，适合大多数屏幕
- 示例：1920x1080 屏幕 → 1536x756 窗口

### 3. STANDARD_1024x768（标准分辨率）
- 窗口大小：1024 × 768（固定）
- 适用场景：小屏幕或旧显示器
- 兼容性：广泛支持

### 4. HD_1280x720（高清）
- 窗口大小：1280 × 720（固定）
- 适用场景：720p 高清显示器
- 兼容性：较好

### 5. HD_PLUS_1600x900（高清+）
- 窗口大小：1600 × 900（固定）
- 适用场景：1600x900 分辨率显示器
- 兼容性：良好

### 6. FULL_HD_1920x1080（全高清）
- 窗口大小：1920 × 1080（固定）
- 适用场景：1080p 全高清显示器
- 兼容性：现代显示器标准

### 7. CUSTOM（自定义）
- 窗口大小：用户自定义
- 可调整大小：**支持**
- 适用场景：特殊需求或多显示器

## 使用方法

### 方法 1：在代码中设置模式（推荐）

在 `MainTest.main()` 方法中，在 `SysConfig.initSysConfig()` 之前设置模式：

```java
public static void main(String[] args) throws Exception {
    // 初始化游戏日志系统
    GameLogger.init();

    // 设置窗口大小模式（必须在 initSysConfig 之前）
    SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.FULL_HD_1920x1080);

    // 或者使用自动推荐模式
    SysConfig.setWindowSizeMode(SysConfig.recommendWindowMode());

    // 初始化系统配置
    SysConfig.initSysConfig();

    // ... 其他代码 ...
}
```

### 方法 2：使用自定义尺寸

```java
public static void main(String[] args) throws Exception {
    // 设置自定义窗口大小
    SysConfig.setCustomWindowSize(1440, 900);

    // 初始化系统配置
    SysConfig.initSysConfig();

    // ... 其他代码 ...
}
```

### 方法 3：全屏模式

```java
public static void main(String[] args) throws Exception {
    // 设置全屏模式
    SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.FULLSCREEN);

    // 初始化系统配置
    SysConfig.initSysConfig();

    // ... 其他代码 ...
}
```

## 配置输出示例

程序启动时会输出窗口配置信息：

```
====================================
窗口配置信息:
屏幕尺寸: 1920x1080
窗口模式: FULL_HD_1920x1080
窗口尺寸: 1920x1080
视口尺寸: 1728x1048
可调整大小: false
双人位数量: 13
====================================
```

## 自动推荐模式

使用 `SysConfig.recommendWindowMode()` 可以根据屏幕尺寸自动选择最佳模式：

| 屏幕分辨率 | 推荐模式 |
|-----------|---------|
| 1920x1080 或更大 | FULL_HD_1920x1080 |
| 1600x900 或更大 | HD_PLUS_1600x900 |
| 1280x720 或更大 | HD_1280x720 |
| 小于 1280x720 | LARGE（80%×70%） |

## 模式对比表

| 模式 | 窗口大小（1920x1080屏幕） | 可调整 | 适用场景 |
|-----|----------------------|-------|---------|
| FULLSCREEN | 1920x1080 | ❌ | 沉浸式体验 |
| LARGE | 1536x756 | ❌ | 通用（默认） |
| STANDARD_1024x768 | 1024x768 | ❌ | 小屏幕 |
| HD_1280x720 | 1280x720 | ❌ | 高清 |
| HD_PLUS_1600x900 | 1600x900 | ❌ | 高清+ |
| FULL_HD_1920x1080 | 1920x1080 | ❌ | 全高清 |
| CUSTOM | 自定义 | ✅ | 特殊需求 |

## 常见配置示例

### 示例 1：全高清屏幕使用全高清模式

```java
SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.FULL_HD_1920x1080);
SysConfig.initSysConfig();
```

**输出**：
```
屏幕尺寸: 1920x1080
窗口尺寸: 1920x1080
```

### 示例 2：小屏幕使用自适应模式

```java
SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.LARGE);
SysConfig.initSysConfig();
```

**1366x768 屏幕输出**：
```
屏幕尺寸: 1366x768
窗口尺寸: 1092x537
```

### 示例 3：自动推荐最佳模式

```java
SysConfig.setWindowSizeMode(SysConfig.recommendWindowMode());
SysConfig.initSysConfig();
```

**1920x1080 屏幕自动选择**：
```
窗口模式: FULL_HD_1920x1080
窗口尺寸: 1920x1080
```

### 示例 4：自定义窗口大小（可调整）

```java
SysConfig.setCustomWindowSize(1440, 900);
SysConfig.initSysConfig();
```

**输出**：
```
窗口模式: CUSTOM
窗口尺寸: 1440x900
可调整大小: true
```

## 注意事项

### 1. 调用顺序
**必须**在 `SysConfig.initSysConfig()` **之前**调用窗口模式设置方法：

```java
// ✅ 正确
SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.HD_1280x720);
SysConfig.initSysConfig();

// ❌ 错误（无效）
SysConfig.initSysConfig();
SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.HD_1280x720);
```

### 2. 窗口大小限制
- 窗口大小自动限制在屏幕分辨率范围内
- 超过屏幕的配置会自动缩小

### 3. 可调整大小
- 只有 CUSTOM 模式支持窗口调整
- 其他模式固定大小，不可调整

### 4. 视口尺寸计算
视口尺寸 = 窗口尺寸 - 选项面板宽度（约 192px）- 标题栏高度（32px）

例如 1920x1080 窗口：
- 视口宽度 = 1920 - 192 = 1728
- 视口高度 = 1080 - 32 = 1048

## 完整示例代码

```java
public static void main(String[] args) throws Exception {
    // 1. 初始化日志系统
    GameLogger.init();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        GameLogger.shutdown();
    }));

    // 2. 设置窗口大小模式（根据屏幕自动推荐）
    SysConfig.WindowSizeMode recommendedMode = SysConfig.recommendWindowMode();
    SysConfig.setWindowSizeMode(recommendedMode);

    // 或者手动指定模式
    // SysConfig.setWindowSizeMode(SysConfig.WindowSizeMode.FULL_HD_1920x1080);

    // 或者使用自定义尺寸
    // SysConfig.setCustomWindowSize(1600, 900);

    // 3. 初始化系统配置
    SysConfig.initSysConfig();

    // 4. 创建游戏窗口
    JFrame jf = new JFrame("红色警戒");
    JPanel scenePanel = new MainPanel();
    OptionsPanel optionsPanel = new OptionsPanel();

    jf.add(BorderLayout.CENTER, scenePanel);
    jf.add(BorderLayout.EAST, optionsPanel);

    // 5. 设置窗口属性（使用配置的大小）
    jf.setSize(SysConfig.frameWidth, SysConfig.frameHeight);
    jf.setResizable(SysConfig.resizable); // 根据 mode 自动设置
    jf.setLocationRelativeTo(null); // 屏幕居中

    // 6. 显示窗口
    jf.setVisible(true);
    jf.pack();
}
```

## 性能影响

不同窗口尺寸对性能的影响：

| 窗口尺寸 | 像素数量 | 性能影响 | 推荐配置 |
|---------|---------|---------|---------|
| 1024x768 | 78万 | 低 | 集成显卡 |
| 1280x720 | 92万 | 较低 | 入门独显 |
| 1600x900 | 144万 | 中等 | 主流独显 |
| 1920x1080 | 207万 | 较高 | 性能独显 |
| 2560x1440 | 368万 | 高 | 高性能显卡 |

**建议**：根据显卡性能选择合适的窗口尺寸。

---

**创建时间**: 2026-01-17
**版本**: 1.0
**维护**: Claude Code
