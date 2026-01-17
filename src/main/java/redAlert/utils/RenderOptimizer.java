package redAlert.utils;

import java.awt.Rectangle;

/**
 * 渲染优化工具类
 *
 * 优化策略：
 * 1. 视锥剔除（Frustum Culling）：只渲染视口内的瓦片
 * 2. 批处理（Batching）：减少绘制调用次数
 * 3. 缓存（Caching）：缓存不变的内容
 * 4. LOD（Level of Detail）：根据距离调整细节
 */
public class RenderOptimizer {

	/** 视口缓存（避免重复计算） */
	private static Rectangle cachedViewport = new Rectangle();
	private static long lastViewportUpdate = 0;
	private static final long VIEWPORT_CACHE_TTL = 50_000_000L; // 50ms

	/** 渲染统计 */
	private static int totalTiles = 0;
	private static int visibleTiles = 0;
	private static int culledTiles = 0;

	/**
	 * 计算可见的瓦片范围（视锥剔除）
	 *
	 * @param viewportOffX 视口 X 偏移
	 * @param viewportOffY 视口 Y 偏移
	 * @param viewportWidth 视口宽度
	 * @param viewportHeight 视口高度
	 * @param tileSize 瓦片大小
	 * @param mapWidth 地图总宽度
	 * @param mapHeight 地图总高度
	 * @return [startX, startY, endX, endY] 瓦片坐标范围
	 */
	public static int[] calculateVisibleTileRange(
		int viewportOffX,
		int viewportOffY,
		int viewportWidth,
		int viewportHeight,
		int tileSize,
		int mapWidth,
		int mapHeight) {

		// 计算视口在地图上的实际区域
		int visibleX = viewportOffX;
		int visibleY = viewportOffY;
		int visibleWidth = viewportWidth;
		int visibleHeight = viewportHeight;

		// 边界检查
		if (visibleX < 0) {
			visibleWidth += visibleX;
			visibleX = 0;
		}
		if (visibleY < 0) {
			visibleHeight += visibleY;
			visibleY = 0;
		}
		if (visibleX + visibleWidth > mapWidth) {
			visibleWidth = mapWidth - visibleX;
		}
		if (visibleY + visibleHeight > mapHeight) {
			visibleHeight = mapHeight - visibleY;
		}

		// 计算瓦片索引范围
		int startTileX = visibleX / tileSize;
		int startTileY = visibleY / tileSize;
		int endTileX = (visibleX + visibleWidth + tileSize - 1) / tileSize;  // 向上取整
		int endTileY = (visibleY + visibleHeight + tileSize - 1) / tileSize;

		// 确保不超出地图边界
		int maxTileX = (mapWidth + tileSize - 1) / tileSize;
		int maxTileY = (mapHeight + tileSize - 1) / tileSize;

		startTileX = Math.max(0, startTileX);
		startTileY = Math.max(0, startTileY);
		endTileX = Math.min(maxTileX, endTileX);
		endTileY = Math.min(maxTileY, endTileY);

		// 更新统计
		totalTiles = maxTileX * maxTileY;
		visibleTiles = (endTileX - startTileX) * (endTileY - startTileY);
		culledTiles = totalTiles - visibleTiles;

		return new int[] { startTileX, startTileY, endTileX, endTileY };
	}

	/**
	 * 判断瓦片是否在视口内
	 *
	 * @param tileX 瓦片 X 坐标
	 * @param tileY 瓦片 Y 坐标
	 * @param viewportOffX 视口 X 偏移
	 * @param viewportOffY 视口 Y 偏移
	 * @param viewportWidth 视口宽度
	 * @param viewportHeight 视口高度
	 * @param tileSize 瓦片大小
	 * @return true 如果可见
	 */
	public static boolean isTileVisible(
		int tileX,
		int tileY,
		int viewportOffX,
		int viewportOffY,
		int viewportWidth,
		int viewportHeight,
		int tileSize) {

		int tilePixelX = tileX * tileSize;
		int tilePixelY = tileY * tileSize;

		// 简单的 AABB 碰撞检测
		boolean visibleX = tilePixelX + tileSize > viewportOffX && tilePixelX < viewportOffX + viewportWidth;
		boolean visibleY = tilePixelY + tileSize > viewportOffY && tilePixelY < viewportOffY + viewportHeight;

		return visibleX && visibleY;
	}

	/**
	 * 获取渲染统计信息
	 */
	public static String getStatistics() {
		if (totalTiles == 0) {
			return "渲染统计: 暂无数据";
		}

		double cullRatio = (double) culledTiles / totalTiles * 100;

		return String.format(
			"渲染统计: 总瓦片=%d, 可见=%d (%.1f%%), 剔除=%d (%.1f%%)",
			totalTiles,
			visibleTiles,
			(double) visibleTiles / totalTiles * 100,
			culledTiles,
			cullRatio
		);
	}

	/**
	 * 重置统计
	 */
	public static void resetStatistics() {
		totalTiles = 0;
		visibleTiles = 0;
		culledTiles = 0;
	}

	/**
	 * 获取剔除率（用于调试）
	 */
	public static double getCullRatio() {
		if (totalTiles == 0) {
			return 0.0;
		}
		return (double) culledTiles / totalTiles;
	}

	/**
	 * 缓存视口信息（避免重复计算）
	 */
	public static boolean shouldUpdateViewport(int viewportOffX, int viewportOffY, int viewportWidth, int viewportHeight) {
		long currentTime = System.nanoTime();

		// 检查是否需要更新
		boolean needUpdate =
			cachedViewport.x != viewportOffX ||
			cachedViewport.y != viewportOffY ||
			cachedViewport.width != viewportWidth ||
			cachedViewport.height != viewportHeight;

		if (needUpdate) {
			cachedViewport.setBounds(viewportOffX, viewportOffY, viewportWidth, viewportHeight);
			lastViewportUpdate = currentTime;
			return true;
		}

		// 检查缓存是否过期
		if (currentTime - lastViewportUpdate > VIEWPORT_CACHE_TTL) {
			lastViewportUpdate = currentTime;
			return true;
		}

		return false;
	}
}
