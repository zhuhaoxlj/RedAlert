package redAlert.mapEditor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;

import redAlert.GlobalConfig;
import redAlert.utils.TmpFileReader;

/**
 * 增强版地图编辑器
 * 功能：可视化地图编辑、保存/加载、画笔工具、填充工具、撤销重做、小地图导航
 */
public class EnhancedMapEditor extends JFrame {

    private static final long serialVersionUID = 1L;

    // 界面组件
    private MapCanvas mapCanvas;
    private TilePalettePanel tilePalette;
    private ToolBarPanel toolBar;
    private MiniMapPanel miniMap;
    private StatusBarPanel statusBar;

    // 地图数据
    private Map<String, MapCenterPoint> mapData = new HashMap<>();
    private List<MapCenterPoint> mapPoints = new ArrayList<>();

    // 编辑状态
    private redAlert.mapEditor.Tile selectedTile = null;
    private String currentTool = "brush"; // brush, fill, eraser, select
    private boolean isDrawing = false;
    private Stack<List<MapEditAction>> undoStack = new Stack<>();
    private Stack<List<MapEditAction>> redoStack = new Stack<>();

    // 视口
    private int viewportOffX = 0;
    private int viewportOffY = 0;
    private final int viewportWidth = 1600;
    private final int viewportHeight = 800;

    // 地图尺寸
    private final int mapWidth = 3000;
    private final int mapHeight = 1500;

    // 瓦片列表
    private List<redAlert.mapEditor.Tile> availableTiles = new ArrayList<>();

    public EnhancedMapEditor() {
        setTitle("红色警戒增强版地图编辑器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 初始化
        initializeTiles();
        initializeUI();
        initializeEvents();

        // 加载默认地图（如果存在）
        loadDefaultMap();

        setSize(1920, 1080);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * 初始化瓦片列表
     */
    private void initializeTiles() {
        try {
            // 雪地瓦片（32种）
            String[] snowTiles = {
                "clat01.sno", "clat02.sno", "clat03.sno", "clat04.sno",
                "clat05.sno", "clat06.sno", "clat07.sno", "clat08.sno",
                "clat09.sno", "clat10.sno", "clat11.sno", "clat12.sno",
                "clat13.sno", "clat14.sno", "clat15.sno", "clat16.sno",
                "clat01a.sno", "clat02a.sno", "clat03a.sno", "clat04a.sno",
                "clat05a.sno", "clat06a.sno", "clat07a.sno", "clat08a.sno",
                "clat09a.sno", "clat10a.sno", "clat11a.sno", "clat12a.sno",
                "clat13a.sno", "clat14a.sno", "clat15a.sno", "clat16a.sno"
            };

            // 草地瓦片（16种）
            String[] grassTiles = {
                "clat01.tem", "clat02.tem", "clat03.tem", "clat04.tem",
                "clat05.tem", "clat06.tem", "clat07.tem", "clat08.tem",
                "clat09.tem", "clat10.tem", "clat11.tem", "clat12.tem",
                "clat13.tem", "clat14.tem", "clat15.tem", "clat16.tem"
            };

            // 加载瓦片
            for (String tileName : snowTiles) {
                BufferedImage img = TmpFileReader.test(tileName);
                availableTiles.add(new redAlert.mapEditor.Tile(tileName, "2222"));
                availableTiles.get(availableTiles.size() - 1).setImage(img);
            }

            for (String tileName : grassTiles) {
                BufferedImage img = TmpFileReader.test(tileName);
                availableTiles.add(new redAlert.mapEditor.Tile(tileName, "2222"));
                availableTiles.get(availableTiles.size() - 1).setImage(img);
            }

            // 默认选择第一个瓦片
            if (!availableTiles.isEmpty()) {
                selectedTile = availableTiles.get(0);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载瓦片失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
            }
    }

    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 主画布（左侧）
        mapCanvas = new MapCanvas();
        JScrollPane scrollPane = new JScrollPane(mapCanvas);
        scrollPane.setPreferredSize(new Dimension(viewportWidth, viewportHeight));
        add(scrollPane, BorderLayout.CENTER);

        // 右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));

        // 瓦片选择面板
        tilePalette = new TilePalettePanel();
        JScrollPane tileScroll = new JScrollPane(tilePalette);
        tileScroll.setBorder(BorderFactory.createTitledBorder("瓦片选择"));
        rightPanel.add(tileScroll, BorderLayout.CENTER);

        // 工具栏
        toolBar = new ToolBarPanel();
        rightPanel.add(toolBar, BorderLayout.NORTH);

        add(rightPanel, BorderLayout.EAST);

        // 顶部工具栏
        createTopToolBar();

        // 底部状态栏
        statusBar = new StatusBarPanel();
        add(statusBar, BorderLayout.SOUTH);

        // 小地图（左下角）
        miniMap = new MiniMapPanel();
        miniMap.setPreferredSize(new Dimension(200, 150));
    }

    /**
     * 创建顶部工具栏
     */
    private void createTopToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 文件菜单
        JButton btnNew = new JButton("新建");
        btnNew.addActionListener(e -> newMap());
        toolBar.add(btnNew);

        JButton btnOpen = new JButton("打开");
        btnOpen.addActionListener(e -> openMap());
        toolBar.add(btnOpen);

        JButton btnSave = new JButton("保存");
        btnSave.addActionListener(e -> saveMap());
        toolBar.add(btnSave);

        JButton btnSaveAs = new JButton("另存为");
        btnSaveAs.addActionListener(e -> saveMapAs());
        toolBar.add(btnSaveAs);

        toolBar.addSeparator();

        // 编辑菜单
        JButton btnUndo = new JButton("撤销");
        btnUndo.addActionListener(e -> undo());
        toolBar.add(btnUndo);

        JButton btnRedo = new JButton("重做");
        btnRedo.addActionListener(e -> redo());
        toolBar.add(btnRedo);

        toolBar.addSeparator();

        // 生成菜单
        JButton btnGenerate = new JButton("生成地图");
        btnGenerate.addActionListener(e -> generateMap());
        toolBar.add(btnGenerate);

        toolBar.addSeparator();

        // 帮助
        JButton btnHelp = new JButton("帮助");
        btnHelp.addActionListener(e -> showHelp());
        toolBar.add(btnHelp);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * 初始化事件
     */
    private void initializeEvents() {
        // 键盘快捷键
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { undo(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { redo(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { saveMap(); }
        });
    }

    /**
     * 地图画布
     */
    private class MapCanvas extends JPanel {
        private BufferedImage canvas;
        private Graphics2D g2d;

        public MapCanvas() {
            canvas = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
            g2d = canvas.createGraphics();
            g2d.setColor(new Color(128, 128, 128));
            g2d.fillRect(0, 0, mapWidth, mapHeight);
            g2d.dispose();

            setPreferredSize(new Dimension(mapWidth, mapHeight));

            // 鼠标事件
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePress(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseRelease(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseDrag(e);
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(canvas, 0, 0, this);
        }

        /**
         * 绘制地图
         */
        public void renderMap() {
            Graphics2D g = canvas.createGraphics();

            // 清空背景
            g.setColor(new Color(128, 128, 128));
            g.fillRect(0, 0, mapWidth, mapHeight);

            // 绘制所有瓦片
            for (MapCenterPoint point : mapPoints) {
                if (point.getTile() != null && point.getTile().getImage() != null) {
                    BufferedImage img = point.getTile().getImage();
                    g.drawImage(img, point.getX() - 30, point.getY() - 15, null);
                }
            }

            g.dispose();
            repaint();
        }
    }

    /**
     * 瓦片选择面板
     */
    private class TilePalettePanel extends JPanel {
        public TilePalettePanel() {
            setLayout(new GridLayout(0, 2, 2, 2));
            setBackground(Color.LIGHT_GRAY);

            // 添加所有瓦片
            for (redAlert.mapEditor.Tile tile : availableTiles) {
                TileButton btn = new TileButton(tile);
                add(btn);
            }
        }

        private class TileButton extends JButton {
            private redAlert.mapEditor.Tile tile;

            public TileButton(redAlert.mapEditor.Tile tile) {
                this.tile = tile;
                Icon icon = new ImageIcon(tile.getImage());
                setIcon(icon);
                setPreferredSize(new Dimension(60, 30));
                setToolTipText(tile.getName());

                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectedTile = tile;
                        statusBar.setText("已选择: " + tile.getName());
                    }
                });
            }
        }
    }

    /**
     * 工具栏面板
     */
    private class ToolBarPanel extends JPanel {
        public ToolBarPanel() {
            setLayout(new GridLayout(4, 1, 2, 2));
            setBorder(BorderFactory.createTitledBorder("工具"));

            ButtonGroup group = new ButtonGroup();

            JRadioButton btnBrush = new JRadioButton("画笔", true);
            btnBrush.addActionListener(e -> currentTool = "brush");
            group.add(btnBrush);
            add(btnBrush);

            JRadioButton btnFill = new JRadioButton("填充");
            btnFill.addActionListener(e -> currentTool = "fill");
            group.add(btnFill);
            add(btnFill);

            JRadioButton btnEraser = new JRadioButton("橡皮擦");
            btnEraser.addActionListener(e -> currentTool = "eraser");
            group.add(btnEraser);
            add(btnEraser);

            JRadioButton btnSelect = new JRadioButton("选择");
            btnSelect.addActionListener(e -> currentTool = "select");
            group.add(btnSelect);
            add(btnSelect);
        }
    }

    /**
     * 小地图面板
     */
    private class MiniMapPanel extends JPanel {
        private BufferedImage miniMapImage;

        public MiniMapPanel() {
            miniMapImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
            setBorder(BorderFactory.createTitledBorder("小地图"));
        }

        public void updateMiniMap() {
            Graphics2D g = miniMapImage.createGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 200, 100);

            // 绘制缩略图
            float scaleX = 200.0f / mapWidth;
            float scaleY = 100.0f / mapHeight;

            for (MapCenterPoint point : mapPoints) {
                if (point.getTile() != null && point.getTile().getImage() != null) {
                    int x = (int)(point.getX() * scaleX);
                    int y = (int)(point.getY() * scaleY);
                    g.drawImage(point.getTile().getImage(), x, y, 2, 1, null);
                }
            }

            // 绘制视口框
            g.setColor(Color.RED);
            int viewX = (int)(viewportOffX * scaleX);
            int viewY = (int)(viewportOffY * scaleY);
            int viewW = (int)(viewportWidth * scaleX);
            int viewH = (int)(viewportHeight * scaleY);
            g.drawRect(viewX, viewY, viewW, viewH);

            g.dispose();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(miniMapImage, 0, 0, this);
        }
    }

    /**
     * 状态栏面板
     */
    private class StatusBarPanel extends JPanel {
        private JLabel label;

        public StatusBarPanel() {
            setLayout(new BorderLayout());
            label = new JLabel("就绪");
            add(label, BorderLayout.WEST);
        }

        public void setText(String text) {
            label.setText(text);
        }
    }

    // ========== 鼠标处理 ==========

    private void handleMousePress(MouseEvent e) {
        if (selectedTile == null && !currentTool.equals("eraser")) {
            JOptionPane.showMessageDialog(this, "请先选择一个瓦片", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        isDrawing = true;
        applyTool(e.getX(), e.getY());
    }

    private void handleMouseRelease(MouseEvent e) {
        isDrawing = false;
        saveEditAction();
    }

    private void handleMouseDrag(MouseEvent e) {
        if (isDrawing && (currentTool.equals("brush") || currentTool.equals("eraser"))) {
            applyTool(e.getX(), e.getY());
        }
    }

    /**
     * 应用工具
     */
    private void applyTool(int screenX, int screenY) {
        int mapX = screenX + viewportOffX;
        int mapY = screenY + viewportOffY;

        MapCenterPoint point = MapCenterPointUtil.getCenterPoint(mapX, mapY);
        if (point == null) return;

        if (currentTool.equals("brush")) {
            if (point.getTile() != selectedTile) {
                point.setTile(selectedTile);
                mapCanvas.renderMap();
            }
        } else if (currentTool.equals("eraser")) {
            point.setTile(null);
            mapCanvas.renderMap();
        } else if (currentTool.equals("fill")) {
            floodFill(point, point.getTile(), selectedTile);
            mapCanvas.renderMap();
        }
    }

    /**
     * 泛洪填充
     */
    private void floodFill(MapCenterPoint start, Tile oldTile, Tile newTile) {
        if (oldTile == newTile) return;

        Queue<MapCenterPoint> queue = new LinkedList<>();
        Set<MapCenterPoint> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            MapCenterPoint current = queue.poll();
            current.setTile(newTile);

            // 检查四个邻居
            List<MapCenterPoint> neighbors = MapCenterPointUtil.getNeighbors(current);
            for (MapCenterPoint neighbor : neighbors) {
                if (neighbor != null && !visited.contains(neighbor)) {
                    Tile neighborTile = neighbor.getTile();
                    boolean sameTile = (oldTile == null && neighborTile == null) ||
                                     (oldTile != null && neighborTile != null &&
                                      oldTile.getName().equals(neighborTile.getName()));

                    if (sameTile) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    // ========== 编辑操作 ==========

    private void saveEditAction() {
        // 记录编辑操作用于撤销
        // 简化实现：每次操作后保存整个地图状态
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            List<MapEditAction> actions = undoStack.pop();
            // TODO: 实现撤销逻辑
            mapCanvas.renderMap();
            statusBar.setText("已撤销");
        } else {
            statusBar.setText("无法撤销");
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            List<MapEditAction> actions = redoStack.pop();
            // TODO: 实现重做逻辑
            mapCanvas.renderMap();
            statusBar.setText("已重做");
        } else {
            statusBar.setText("无法重做");
        }
    }

    // ========== 文件操作 ==========

    private void newMap() {
        int result = JOptionPane.showConfirmDialog(this,
            "确定要新建地图吗？未保存的更改将丢失。",
            "新建地图", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            mapData.clear();
            mapPoints.clear();
            mapCanvas.renderMap();
            statusBar.setText("已新建地图");
        }
    }

    private void openMap() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".text");
            }

            @Override
            public String getDescription() {
                return "地图文件 (*.text)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadMapFromFile(chooser.getSelectedFile());
        }
    }

    private void saveMap() {
        File mapFile = new File(GlobalConfig.mapFilePath);
        saveMapToFile(mapFile);
        statusBar.setText("已保存到: " + mapFile.getName());
    }

    private void saveMapAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".text");
            }

            @Override
            public String getDescription() {
                return "地图文件 (*.text)";
            }
        });

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            saveMapToFile(chooser.getSelectedFile());
            statusBar.setText("已保存到: " + chooser.getSelectedFile().getName());
        }
    }

    /**
     * 从文件加载地图
     */
    private void loadMapFromFile(File file) {
        try {
            String mapText = FileUtils.readFileToString(file, "UTF-8");
            String[] strs = mapText.split("\\$");

            mapData.clear();
            mapPoints.clear();

            for (String str : strs) {
                if (str == null || str.trim().isEmpty()) continue;

                String[] infos = str.split(",");
                if (infos.length < 3) continue;

                int x = Integer.valueOf(infos[0].trim());
                int y = Integer.valueOf(infos[1].trim());
                String name = infos[2].trim();

                MapCenterPoint point = MapCenterPointUtil.fetchCenterPoint(x, y);
                if (point != null) {
                    // 查找对应的Tile
                    for (redAlert.mapEditor.Tile tile : availableTiles) {
                        if (tile.getName().equals(name)) {
                            point.setTile(tile);
                            break;
                        }
                    }
                    mapData.put(x + "," + y, point);
                    mapPoints.add(point);
                }
            }

            mapCanvas.renderMap();
            statusBar.setText("已加载: " + file.getName());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载地图失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 保存地图到文件
     */
    private void saveMapToFile(File file) {
        try {
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < mapPoints.size(); i++) {
                MapCenterPoint point = mapPoints.get(i);
                if (point.getTile() != null) {
                    if (i < mapPoints.size() - 1) {
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
            }

            FileUtils.writeStringToFile(file, text.toString(), "UTF-8");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存地图失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 加载默认地图
     */
    private void loadDefaultMap() {
        File defaultMap = new File(GlobalConfig.mapFilePath);
        if (defaultMap.exists()) {
            loadMapFromFile(defaultMap);
        } else {
            // 初始化空地图
            initializeEmptyMap();
        }
    }

    /**
     * 初始化空地图
     */
    private void initializeEmptyMap() {
        // 创建所有中心点
        for (int m = 0; m < 50; m++) {
            int y = 15 + 30 * m;
            for (int n = 0; n < 50; n++) {
                int x = 30 + 60 * n;
                MapCenterPoint mcp = MapCenterPointUtil.fetchCenterPoint(x, y);
                if (mcp != null) {
                    mapPoints.add(mcp);
                    mapData.put(x + "," + y, mcp);
                }
            }
        }

        for (int m = 0; m < 50; m++) {
            int y = 30 * m;
            for (int n = 0; n < 50; n++) {
                int x = 60 * n;
                MapCenterPoint mcp = MapCenterPointUtil.fetchCenterPoint(x, y);
                if (mcp != null) {
                    mapPoints.add(mcp);
                    mapData.put(x + "," + y, mcp);
                }
            }
        }
    }

    /**
     * 生成地图
     */
    private void generateMap() {
        int choice = JOptionPane.showConfirmDialog(this,
            "选择生成方式:\n是 - SimpleMapGenerator (简化版,明显的地形分区)\n否 - RichMapGenerator (复杂版,多种地形)",
            "生成地图", JOptionPane.YES_NO_OPTION);

        try {
            if (choice == JOptionPane.YES_OPTION) {
                // 使用SimpleMapGenerator
                SimpleMapGenerator.main(new String[]{});
            } else {
                // 使用RichMapGenerator
                RichMapGenerator.main(new String[]{});
            }

            // 重新加载
            loadDefaultMap();
            statusBar.setText("已生成新地图");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "生成地图失败: " + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 显示帮助
     */
    private void showHelp() {
        String helpText = "红色警戒增强版地图编辑器使用说明\n\n" +
            "工具:\n" +
            "  画笔: 单击或拖动绘制单个瓦片\n" +
            "  填充: 点击填充相连的相同瓦片区域\n" +
            "  橡皮擦: 删除瓦片\n" +
            "  选择: 选择位置\n\n" +
            "快捷键:\n" +
            "  Ctrl+S: 保存地图\n" +
            "  Ctrl+Z: 撤销\n" +
            "  Ctrl+Y: 重做\n\n" +
            "瓦片:\n" +
            "  共48种瓦片 (32种雪地 + 16种草地)\n" +
            "  点击右侧面板选择瓦片\n\n" +
            "小地图:\n" +
            "  左下角显示整个地图的缩略图\n" +
            "  红色框表示当前视口位置";

        JOptionPane.showMessageDialog(this, helpText, "帮助", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 编辑操作类
     */
    private class MapEditAction {
        MapCenterPoint point;
        redAlert.mapEditor.Tile oldTile;
        redAlert.mapEditor.Tile newTile;

        public MapEditAction(MapCenterPoint point, redAlert.mapEditor.Tile oldTile, redAlert.mapEditor.Tile newTile) {
            this.point = point;
            this.oldTile = oldTile;
            this.newTile = newTile;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EnhancedMapEditor().setVisible(true);
            }
        });
    }
}
