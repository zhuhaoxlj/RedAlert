package redAlert.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import redAlert.utilBean.CenterPoint;
import redAlert.utilBean.LittleCenterPoint;

/**
 * 小中心点工具类
 */
public class LittleCenterPointUtil {

	
	/**
	 * 找到一个小中心点周围一个步兵可进入的点   逐渐向外围搜索
	 * 找到的这个点不能是exceptLs中的点
	 *
	 * 性能优化：添加搜索深度限制，防止在密集障碍物区域无限搜索导致卡顿
	 */
	public static LittleCenterPoint findSoldierCanOnLcpNearBy(LittleCenterPoint lcp,Set<LittleCenterPoint> exceptLs) {
		// 性能监控：开始寻路计时
		long pathfindStart = PerformanceMonitor.startOperation("寻路");

		ArrayDeque<LittleCenterPoint> rest = new ArrayDeque<>();
		rest.add(lcp);
		Set<LittleCenterPoint> haveGet = new HashSet<>();
		haveGet.add(lcp);

		LittleCenterPoint result = null;
		int maxSearchDepth = 50;  // 最大搜索深度，防止树木密集区域卡顿
		int currentDepth = 0;
		int nodesInCurrentLevel = 1;
		int nodesInNextLevel = 0;

		while(!rest.isEmpty() && currentDepth < maxSearchDepth) {
			LittleCenterPoint start = rest.poll();
			nodesInCurrentLevel--;

			List<LittleCenterPoint> neighbors = LittleCenterPointUtil.getNeighbors(start);
			for(LittleCenterPoint neighbor:neighbors) {
				if(neighbor.isSoldierCanOn() && !exceptLs.contains(neighbor)) {
					// 性能监控：结束寻路计时
					PerformanceMonitor.endOperation("寻路", pathfindStart);
					return neighbor;
				}

				if(!haveGet.contains(neighbor)) {
					haveGet.add(neighbor);
					rest.add(neighbor);
					nodesInNextLevel++;
				}
			}

			// 当前层级处理完毕，进入下一层级
			if(nodesInCurrentLevel == 0) {
				nodesInCurrentLevel = nodesInNextLevel;
				nodesInNextLevel = 0;
				currentDepth++;
			}
		}

		// 搜索深度超限，返回 null 避免卡死
		if(currentDepth >= maxSearchDepth) {
			System.err.println("警告: findSoldierCanOnLcpNearBy 搜索深度超限 (" + maxSearchDepth + ")，目标区域可能障碍物过多");
		}

		// 性能监控：结束寻路计时（即使失败也记录）
		PerformanceMonitor.endOperation("寻路", pathfindStart);

		return result;
	}
	
	/**
	 * 找到一个小中心点周围一个步兵可进入的点   逐渐向外围搜索
	 * 这个点上不能有别的步兵  这个是出兵用的
	 */
	public static LittleCenterPoint findSoldierCanOnLcpNearBy(LittleCenterPoint lcp) {
		ArrayDeque<LittleCenterPoint> rest = new ArrayDeque<>();
		rest.add(lcp);
		Set<LittleCenterPoint> haveGet = new HashSet<>();
		haveGet.add(lcp);
		
		LittleCenterPoint result = null;
		while(!rest.isEmpty()) {
			LittleCenterPoint start = rest.poll();
			List<LittleCenterPoint> neighbors = LittleCenterPointUtil.getNeighbors(start);
			for(LittleCenterPoint neighbor:neighbors) {
				if(neighbor.isSoldierCanOn() && neighbor.soldier==null) {
					return neighbor;
				}
				
				if(!haveGet.contains(neighbor)) {
					haveGet.add(neighbor);
					rest.add(neighbor);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 找到一个小中心点的周围   哪个点是与目标点最近的点
	 * @return
	 */
	public static Map<String,Object> getMinDisOfLcpNeighbor(LittleCenterPoint center,LittleCenterPoint targetLcp,LittleCenterPoint lastPoint,List<LittleCenterPoint>haveGetPoint) {
		List<LittleCenterPoint> ls = getNeighbors(center);
		Map<String,Object> result = new HashMap<>();
		
		int min = 999999;
		LittleCenterPoint minDisInCp = null;
		for(LittleCenterPoint cp:ls) {
			//增加判断条件  这个点是否符合要求
			if(!cp.isSoldierCanOn()) {//禁入点
				continue;
			}
//			if(cp.equals(lastPoint)) {//不寻回头路
//				continue;
//			}
			if(haveGetPoint.contains(cp)) {//不寻回头路
				continue;
			}
			
			if(cp.preBooked.get()==true) {
				continue;
			}
			
			
			
			
			int depX = Math.abs(cp.getX()-targetLcp.getX());
			int depY = Math.abs(2*(cp.getY()-targetLcp.getY()));
			int dis = depX*depX +depY*depY ;
			if(dis<min) {
				min = dis;
				minDisInCp = cp;
			}
		}
		
		if(minDisInCp!=null) {
			if(minDisInCp.preBooked.compareAndSet(false, true)) {
				if(minDisInCp.getSoldier()!=null && !minDisInCp.getSoldier().isMoving()) {
					result.put("cp", minDisInCp);
					result.put("desc", "unacc");//不可达,这个是别人的终点
					minDisInCp.preBooked.compareAndSet(true, false);
					return result;
				}else {
					result.put("cp", minDisInCp);
					result.put("desc", "ok");
					return result;
				}
			}else {
				result.put("cp", null);
				result.put("desc", "occ");//其他单位的临时占用,需要等一下
				return result;
			}
		}else {
			result.put("cp", null);
			result.put("desc", "noway");//没有可以移动的点 //可能需要使用A*算法进行运动   小兵走进死胡同了
			System.out.println("方向式路过程中当前点周围无可移动点");
			return result;
		}
	}
	
	/**
	 * 找到一个小中心点周围的8个小中心点
	 */
	public static List<LittleCenterPoint> getNeighbors(LittleCenterPoint center){
		LittleCenterPoint newCp1 = center.getRight();
		LittleCenterPoint newCp2 = center.getRightDn();
		LittleCenterPoint newCp3 = center.getDn();
		LittleCenterPoint newCp4 = center.getLeftDn();
		LittleCenterPoint newCp5 = center.getLeft();
		LittleCenterPoint newCp6 = center.getLeftUp();
		LittleCenterPoint newCp7 = center.getUp();
		LittleCenterPoint newCp8 = center.getRightUp();
		
		List<LittleCenterPoint> list = new ArrayList<>();
		if(newCp1!=null) list.add(newCp1);
		if(newCp2!=null) list.add(newCp2);
		if(newCp3!=null) list.add(newCp3);
		if(newCp4!=null) list.add(newCp4);
		if(newCp5!=null) list.add(newCp5);
		if(newCp6!=null) list.add(newCp6);
		if(newCp7!=null) list.add(newCp7);
		if(newCp8!=null) list.add(newCp8);
		
		return list;
	}
	//掐头
	public static void test1(LittleCenterPoint curLittleCenterPoint,LinkedList<LittleCenterPoint> path) {
		int min = 999999;
		LittleCenterPoint minDisInCp = null;
		for(LittleCenterPoint cp:path) {
			
			int depX = Math.abs(curLittleCenterPoint.getX()-cp.getX());
			int depY = Math.abs(2*(curLittleCenterPoint.getY()-cp.getY()));
			int dis = depX*depX +depY*depY ;
			if(dis<min) {
				min = dis;
				minDisInCp = cp;
			}
		}
		
		Iterator<LittleCenterPoint> iter = path.iterator();
		while(iter.hasNext()) {
			LittleCenterPoint lcp = iter.next();
			if(!lcp.equals(minDisInCp)) {
				iter.remove();
			}else {
				break;
			}
		}
		
		if(!curLittleCenterPoint.equals(minDisInCp)) {
			path.addFirst(curLittleCenterPoint);
		}
		
	}
	//去尾
	public static void test2(LittleCenterPoint targetLittleCenterPoint,LinkedList<LittleCenterPoint> path) {
		int min = 999999;
		LittleCenterPoint minDisInCp = null;
		for(LittleCenterPoint cp:path) {
			
			int depX = Math.abs(targetLittleCenterPoint.getX()-cp.getX());
			int depY = Math.abs(2*(targetLittleCenterPoint.getY()-cp.getY()));
			int dis = depX*depX +depY*depY ;
			if(dis<min) {
				min = dis;
				minDisInCp = cp;
			}
		}
		
		boolean isGet = false;
		Iterator<LittleCenterPoint> iter = path.iterator();
		while(iter.hasNext()) {
			LittleCenterPoint lcp = iter.next();
			if(!lcp.equals(minDisInCp)) {
				if(isGet) {
					iter.remove();
				}else {
					continue;
				}
			}else {
				isGet = true;
			}
		}
		if(!targetLittleCenterPoint.equals(minDisInCp)) {
			path.addLast(targetLittleCenterPoint);
		}
	}
	
	
	/**
	 * 两个小中心点是否相邻
	 */
	public static boolean isNeibor(LittleCenterPoint lcp1,LittleCenterPoint lcp2) {
		List<LittleCenterPoint> ls = getNeighbors(lcp1);
		if(ls.contains(lcp2)) {
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * 核心方法之一：
	 * 获取一个普通坐标对应的小中心点
	 */
	public static LittleCenterPoint getLittleCenterPoint(int x1,int y1) {

		CenterPoint centerPoint = PointUtil.getCenterPoint(x1, y1);

		// 添加空指针检查，防止崩溃
		if(centerPoint == null) {
			System.err.println("警告: getLittleCenterPoint 无法找到中心点 (" + x1 + ", " + y1 + ")");
			return null;
		}

		LittleCenterPoint lcpLeft = centerPoint.getLeftLittleCenterPoint();
		if(lcpLeft == null) {
			System.err.println("警告: 中心点 (" + centerPoint.getX() + ", " + centerPoint.getY() + ") 的 leftLittleCenterPoint 为 null");
			return null;
		}

		int lx1 = lcpLeft.getX();
		int ly1 = lcpLeft.getY();
		int dis1 = (lx1-x1)*(lx1-x1)+(ly1-y1)*(ly1-y1);

		LittleCenterPoint lcpDown = centerPoint.getDownLittleCenterPoint();
		if(lcpDown == null) {
			System.err.println("警告: 中心点 (" + centerPoint.getX() + ", " + centerPoint.getY() + ") 的 downLittleCenterPoint 为 null");
			return lcpLeft; // 返回一个有效的点
		}

		int lx2 = lcpDown.getX();
		int ly2 = lcpDown.getY();
		int dis2 = (lx2-x1)*(lx2-x1)+(ly2-y1)*(ly2-y1);

		LittleCenterPoint lcpUp = centerPoint.getUpLittleCenterPoint();
		if(lcpUp == null) {
			System.err.println("警告: 中心点 (" + centerPoint.getX() + ", " + centerPoint.getY() + ") 的 upLittleCenterPoint 为 null");
			// 在剩下两个点中找最近的
			int min = Math.min(dis1, dis2);
			return min == dis1 ? lcpLeft : lcpDown;
		}

		int lx3 = lcpUp.getX();
		int ly3 = lcpUp.getY();
		int dis3 = (lx3-x1)*(lx3-x1)+(ly3-y1)*(ly3-y1);

		LittleCenterPoint lcpRight = centerPoint.getRightLittleCenterPoint();
		if(lcpRight == null) {
			System.err.println("警告: 中心点 (" + centerPoint.getX() + ", " + centerPoint.getY() + ") 的 rightLittleCenterPoint 为 null");
			// 在剩下三个点中找最近的
			int min = NumberUtils.min(dis1, dis2, dis3);
			if(min==dis1) return lcpLeft;
			else if(min==dis2) return lcpDown;
			else return lcpUp;
		}

		int lx4 = lcpRight.getX();
		int ly4 = lcpRight.getY();
		int dis4 = (lx4-x1)*(lx4-x1)+(ly4-y1)*(ly4-y1);
		
		int min = NumberUtils.min(dis1,dis2,dis3,dis4);
		if(min==dis1) {
			return lcpLeft;
		}else if(min==dis2) {
			return lcpDown;
		}else if(min==dis3) {
			return lcpUp;
		}else {
			return lcpRight;
		}
	}
	
}