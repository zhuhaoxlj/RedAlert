# 形状对象模块 (shapeObjects)

[根目录](../../../CLAUDE.md) > [redAlert](../) > **shapeObjects**

---

## 变更记录 (Changelog)

### 2026-01-17 11:20:17
- 初始化形状对象模块文档
- 整理类层次结构与接口定义
- 补充动画与渲染机制说明

---

## 模块职责

形状对象模块定义游戏中所有可渲染对象的抽象与实现，包括：
- **ShapeUnit** - 所有单位的抽象基类
- **Building** - 军事建筑基类
- **Vehicle** - 载具单位（坦克、车辆）
- **Soldier** - 步兵单位
- **接口系统** - 移动、攻击、血量、扩展等能力接口

---

## 类层次结构

```
ShapeUnit (抽象基类)
  ├─ Building (建筑抽象类)
  │   ├─ PowerPlant (发电厂接口)
  │   └─ [军事建筑实现见 militaryBuildings 包]
  │
  ├─ MovableUnit (可移动单位抽象类)
  │   ├─ Soldier (步兵抽象类)
  │   │   ├─ Sniper (狙击手)
  │   │   ├─ Tany (谭雅)
  │   │   ├─ Gi (美国大兵)
  │   │   ├─ Engn (工程师)
  │   │   └─ Adog (警犬)
  │   │
  │   └─ Vehicle (载具抽象类)
  │       ├─ GrizTank (灰熊坦克)
  │       ├─ XiniuTank2 (犀牛坦克)
  │       ├─ Sref (光棱坦克)
  │       ├─ Ifv (多功能步兵车)
  │       ├─ Mcv (基地车)
  │       ├─ Zep (基洛夫飞艇)
  │       └─ TankTurret (坦克炮塔)
  │
  └─ [其他动画对象]
      └─ TankShell (坦克炮弹)
```

---

## 入口与启动

### 核心抽象类
```java
// 所有单位的基类
redAlert.shapeObjects.ShapeUnit

// 建筑单位基类
redAlert.shapeObjects.Building

// 可移动单位基类
redAlert.shapeObjects.MovableUnit
```

### 典型使用方式
```java
// 创建建筑
CenterPoint cp = PointUtil.getCenterPoint(500, 300);
AfCnst base = new AfCnst(cp, SceneType.TEM, UnitColor.Blue);
Constructor.putOneBuilding(base);

// 创建载具
GrizTank tank = new GrizTank(100, 200, UnitColor.Red);
Constructor.putOneShapeUnit(tank);

// 创建步兵
LittleCenterPoint lcp = cp.getUpLittleCenterPoint();
Sniper sniper = new Sniper(lcp, UnitColor.Blue);
Constructor.putOneShapeUnit(sniper);
```

---

## 对外接口

### ShapeUnit (核心基类)

```java
public abstract class ShapeUnit implements Comparable<ShapeUnit> {
    // ========== 核心属性 ==========

    /** 渲染优先级（越小越优先） */
    public int priority = 50;

    /** 位置坐标 */
    public int positionX, positionY;

    /** 中心点偏移 */
    public int centerOffX, centerOffY;

    /** 当前帧图像 */
    public ShapeUnitFrame curFrame;

    /** 单位唯一编号 */
    public int unitNo;

    /** 阵营颜色 */
    public UnitColor unitColor;

    /** 是否被移除 */
    public volatile boolean end = false;

    /** 所在中心点 */
    public CenterPoint curCenterPoint;

    // ========== 核心方法 ==========

    /** 计算下一帧（抽象方法） */
    public abstract void calculateNextFrame();

    /** 比较渲染优先级 */
    public int compareTo(ShapeUnit o);

    /** 应用阵营颜色 */
    public void giveFrameUnitColor(BufferedImage image, ShapeUnitFrame frame);
}
```

**关键机制：**
- **渲染优先级**：`priority` → `positionMinY` → `positionMinX`
- **遮挡处理**：`isHided` 标志控制是否被其他物体遮挡
- **帧动画**：通过 `calculateNextFrame()` 更新 `curFrame`

### Building (建筑抽象类)

```java
public abstract class Building extends ShapeUnit implements Bloodable {
    // ========== 建筑风格枚举 ==========
    public enum SceneType {
        URBAN("城市", "uniturb"),
        SNOW("雪地", "unitsno"),
        TEM("野外", "unittem"),
        ANIM("动画", "anim")
    }

    // ========== 建筑状态枚举 ==========
    public enum BuildingStatus {
        DEMAGED("受损"),
        UNDEMAGED("完好")
    }

    // ========== 建造阶段枚举 ==========
    public enum BuildingStage {
        UnderConstruct("建设中"),
        ConstructComplete("建设完成"),
        Selling("贱卖中")
    }

    // ========== 核心属性 ==========

    /** SHP 文件前缀 */
    public String constShpFilePrefix = "";

    /** 工作动画前缀集合 */
    public List<String> aniShpPrefixLs = new ArrayList<>();

    /** 建造动画帧 */
    public List<ShapeUnitFrame> constructFrames;

    /** 工作动画帧（多个 SHP 文件组合） */
    public List<List<ShapeUnitFrame>> workingFrames;

    // ========== 核心方法 ==========

    /** 初始化建筑数值 */
    public void initBuildingValue(CenterPoint centerPoint,
                                   SceneType sceneType,
                                   UnitColor unitColor);

    /** 计算下一帧 */
    public void calculateNextFrame();

    /** 获取建筑占用的中心点列表 */
    public List<CenterPoint> getOccupiedCenterPoints();
}
```

**SHP 命名规则：**
- 建造动画：`{team}{scene}{basicName}mk` (如 `gtcnstmk`)
- 工作动画：`{team}{team}{basicName}` (如 `ggcnst`)
- 附加动画：`{prefix}_{suffix}` (如 `ggcnst_a`)

### Vehicle (载具抽象类)

```java
public abstract class Vehicle extends MovableUnit
        implements Turnable, Attackable {

    // ========== 核心属性 ==========

    /** 移动方向（0-7，8 个方向） */
    public int direction = 0;

    /** 目标方向 */
    public int targetDirection = 0;

    /** 炮塔对象（独立旋转） */
    public TankTurret turret;

    // ========== 核心方法 ==========

    /** 计算下一帧 */
    public abstract void calculateNextFrame();

    /** 移动到目标点 */
    public void moveToTarget(CenterPoint target);

    /** 攻击目标 */
    public void attack(ShapeUnit target);

    /** 转向 */
    public void turnTo(int direction);
}
```

**移动机制：**
- 使用 `MoveUtil` 进行寻路
- 支持转向动画（8 方向）
- 炮塔独立于车身旋转

### Soldier (步兵抽象类)

```java
public abstract class Soldier extends MovableUnit
        implements Turnable, Attackable {

    // ========== 核心属性 ==========

    /** 移动状态 */
    public SoldierStatus status;

    /** 当前路径节点 */
    public LittleCenterPoint nextTarget;

    /** 最终目标点 */
    public LittleCenterPoint endTarget;

    // ========== 核心方法 ==========

    /** 寻路移动 */
    public void xunLuMove();

    /** 攻击目标 */
    public void attack(ShapeUnit target);
}
```

**寻路机制：**
- 使用 `XunLuBean` 进行 A* 寻路
- 支持多路径点导航
- 小单位使用 `LittleCenterPoint` (4 个/中心点)

---

## 关键接口

### Bloodable (可受伤接口)
```java
public interface Bloodable {
    // 设置血量
    public void setBlood(int blood);

    // 获取当前血量
    public int getBlood();

    // 受到伤害
    public void underAttack(int damage);
}
```

### Attackable (可攻击接口)
```java
public interface Attackable {
    // 攻击目标
    public void attack(ShapeUnit target);

    // 获取攻击力
    public int getAttackPower();

    // 获取攻击范围
    public int getAttackRange();
}
```

### MovableUnit (可移动接口)
```java
public abstract class MovableUnit extends ShapeUnit {
    // 移动标志
    public boolean move = false;

    // 移动速度
    public int speed = 1;

    // 移动到目标
    public abstract void moveToTarget(CenterPoint target);
}
```

### Turnable (可转向接口)
```java
public interface Turnable {
    // 转向目标方向
    public void turnTo(int direction);

    // 获取当前方向
    public int getDirection();
}
```

### Expandable (可展开接口)
```java
public interface Expandable {
    // 展开（如基地车展开成基地）
    public void expand();

    // 收起
    public void collapse();
}
```

---

## 关键依赖与配置

### 依赖模块
- `redAlert.utilBean` - 坐标系统 (`CenterPoint`, `LittleCenterPoint`)
- `redAlert.utils` - 工具类 (`PointUtil`, `MoveUtil`, `CanvasPainter`)
- `redAlert.resourceCenter` - 资源加载 (`ShpResourceCenter`)
- `redAlert.enums` - 枚举定义 (`UnitColor`, `Direction`)

### 资源文件映射
所有单位的 SHP 文件在 `ShpResourceCenter` 中注册：
```java
// 载具示例
GrizTank: "gtnk.shp" (灰熊坦克)
XiniuTank2: "htnk.shp" (犀牛坦克)
Sref: "sref.shp" (光棱坦克)

// 步兵示例
Gi: "gi.shp" (美国大兵)
Sniper: "sniper.shp" (狙击手)
Tany: "tany.shp" (谭雅)
```

---

## 数据模型

### 坐标系统

**CenterPoint (菱形网格)**
```
    ┌─────┐
   ╱       ╲
  │    ●    │  中心点
   ╲       ╱
    └─────┘

4 个 LittleCenterPoint (上右下左)
```

**位置计算：**
```java
// 中心点坐标
int centerX = positionX + centerOffX;
int centerY = positionY + centerOffY;

// 小坐标点
LittleCenterPoint up = centerPoint.getUpLittleCenterPoint();
LittleCenterPoint right = centerPoint.getRightLittleCenterPoint();
```

### 动画帧系统

**ShapeUnitFrame 结构：**
```java
public class ShapeUnitFrame {
    // 帧图像
    public BufferedImage image;

    // 颜色点列表（用于阵营色）
    public List<ColorPoint> colorPointList;

    // 非透明像素边界
    public int minX, maxX, minY, maxY;
}
```

**帧动画流程：**
```
初始化
  ↓
加载 SHP → List<ShapeUnitFrame>
  ↓
设置 frameSpeed (降频，如 4)
  ↓
每 N 帧调用 calculateNextFrame()
  ↓
更新 curFrame = frames[frameNum % frames.size()]
  ↓
渲染线程绘制 curFrame.image
```

---

## 算法与实现

### 渲染优先级算法

```java
@Override
public int compareTo(ShapeUnit o) {
    // 1. 先比优先级（越小越优先）
    if (o.priority != this.priority) {
        return o.priority < this.priority ? 1 : -1;
    }

    // 2. 再比遮挡状态
    if (o.isHided && !this.isHided) return 1;
    if (!o.isHided && this.isHided) return -1;

    // 3. 再比中心点 Y 坐标（越小越优先）
    int thisCpY = positionY + centerOffY;
    int oCpY = o.positionY + o.centerOffY;
    if (thisCpY != oCpY) {
        return thisCpY > oCpY ? 1 : -1;
    }

    // 4. 最后比 X 坐标
    int thisCpX = positionX + centerOffX;
    int oCpX = o.positionX + o.centerOffX;
    if (thisCpX != oCpX) {
        return thisCpX > oCpX ? 1 : -1;
    }

    return 0;
}
```

**结果：** 优先级高的 → Y 坐标小的 → X 坐标小的 先渲染

### 阵营色应用算法

```java
public void giveFrameUnitColor(BufferedImage image, ShapeUnitFrame frame) {
    List<ColorPoint> colorPointLs = frame.getColorPointList();
    if (colorPointLs != null && !colorPointLs.isEmpty()) {
        for (ColorPoint cp : colorPointLs) {
            int oriColor = image.getRGB(cp.getX(), cp.getY());
            // 转换颜色（保持亮度/阴影，改变色相）
            int newColor = CanvasPainter.transColor(oriColor, this.unitColor);
            image.setRGB(cp.getX(), cp.getY(), newColor);
        }
    }
}
```

---

## 测试与质量

### 单位创建测试
```java
// 建筑
AfCnst base = new AfCnst(cp, SceneType.TEM, UnitColor.Blue);
Constructor.putOneBuilding(base);

// 载具
GrizTank tank = new GrizTank(100, 200, UnitColor.Red);
Constructor.putOneShapeUnit(tank);

// 步兵
Sniper sniper = new Sniper(lcp, UnitColor.Blue);
Constructor.putOneShapeUnit(sniper);
```

### 性能优化点
- 使用对象池减少单位创建/销毁开销
- 帧动画降频（`frameSpeed`）减少 CPU 占用
- 资源预加载（`ShpResourceCenter`）避免重复读取

---

## 常见问题 (FAQ)

**Q: 如何添加新单位？**

A:
1. 继承对应基类（`Vehicle`/`Soldier`）
2. 定义 SHP 文件名前缀
3. 实现 `calculateNextFrame()` 方法
4. 在 `ShpResourceCenter` 注册资源
5. 在对应 `TabIcon` 添加建造按钮

**Q: 单位不显示怎么办？**

A: 检查以下项：
1. `curFrame` 是否正确初始化
2. `isVisible` 是否为 `true`
3. `end` 是否为 `false`
4. `positionX/Y` 是否在屏幕范围内
5. SHP 文件是否正确加载

**Q: 如何调整单位渲染优先级？**

A: 设置 `priority` 属性：
```java
// 建筑优先级 50（默认）
building.setPriority(50);

// 飞行物优先级 10（最优先）
zep.setPriority(10);

// 地面单位优先级 30
vehicle.setPriority(30);
```

**Q: 步兵和载具寻路有何区别？**

A:
- **步兵**: 使用 `LittleCenterPoint`（4 个/中心点），支持精细路径
- **载具**: 使用 `CenterPoint`，占用更大空间，需考虑转弯半径

---

## 相关文件清单

### 抽象基类
- `ShapeUnit.java` - 所有单位基类
- `MovableUnit.java` - 可移动单位基类
- `Building.java` - 建筑基类
- `Vehicle.java` - 载具基类
- `Soldier.java` - 步兵基类
- `TankTurret.java` - 坦克炮塔基类

### 接口定义
- `Bloodable.java` - 可受伤接口
- `Attackable.java` - 可攻击接口
- `Turnable.java` - 可转向接口
- `Expandable.java` - 可展开接口

### 步兵实现
- `soldier/Sniper.java` - 狙击手
- `soldier/Tany.java` - 谭雅
- `soldier/Gi.java` - 美国大兵
- `soldier/Engn.java` - 工程师
- `soldier/Adog.java` - 警犬
- `soldier/Tany2.java` - 谭雅变体

### 载具实现
- `vehicle/GrizTank.java` - 灰熊坦克
- `vehicle/GrizTankTurret.java` - 灰熊坦克炮塔
- `vehicle/XiniuTank2.java` - 犀牛坦克
- `vehicle/XiniuTankTurret.java` - 犀牛坦克炮塔
- `vehicle/Sref.java` - 光棱坦克
- `vehicle/SrefTurret.java` - 光棱坦克炮塔
- `vehicle/Ifv.java` - 多功能步兵车
- `vehicle/IfvTurret.java` - 多功能步兵车炮塔
- `vehicle/Mcv.java` - 基地车
- `vehicle/Zep.java` - 基洛夫飞艇
- `vehicle/ZepIn.java` - 基洛夫影子
- `vehicle/VehicleUtil.java` - 载具工具类

### 动画对象
- `animation/TankShell.java` - 坦克炮弹

---

*本模块文档由 AI 自适应文档生成系统维护*
