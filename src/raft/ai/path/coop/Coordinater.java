package raft.ai.path.coop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import raft.kilavuz.runtime.NoPathException;

public class Coordinater {
    
    Grid grid = new Grid();
    NodePool pool = new NodePool(grid);
    TimeAStar timeAStar = new TimeAStar();
    
    final List<Unit> newUnits = new ArrayList<Unit>();
    final SortedMap<Integer, Unit> units = new TreeMap<Integer, Unit>();
    
    private int depth = 0;
    public long currentTime = 0;
    int index = 0;
    
    
    /** Creates a new instance of Coordinater */
    public Coordinater(int depth) throws IOException {
        setDepth(depth);
    }
    
    void reset() {
        units.clear();
        newUnits.clear();
//        pool.releaseAllNodes();
        pool.reclaimAll();
    }

    private void setDepth(int depth) {
        if (depth < 4)
            throw new IllegalArgumentException("depth: " + depth);
        this.depth = depth;
    }
    
    public void addUnit(Unit unit) {
        if (units.put(unit.id, unit) != null)
            throw new IllegalStateException("already has unit, id: " + unit.id);
        newUnits.add(unit);
    }

    private double findPath(Unit unit, String type) throws NoPathException {
//        pool.releaseAllNodes();
        double time = 0;
        NodePool.Point location = unit.getLocation();
        NodePool.Node from = pool.acquireNode(location.x, location.z, currentTime);
        NodePool.Node to = pool.acquireNode(unit.getDestination().x, unit.getDestination().z, 0);

//        System.out.println("from " + from);
        try {
//            TimeAStar.Path path = timeAStar.findPath(from, to, unit, depth);
            //可以和不加边缘计算，还有直接timeAStar的depth数少的比
            NodePool.Node next;
            TimeAStar.Path path;
            //这里记录时间，implementation time
            if("RTAA".equals(type)){
                unit.imp_rtta -= System.currentTimeMillis();
                path = timeAStar.findRTAAPath(from, to, unit, depth);
                unit.imp_rtta += System.currentTimeMillis();
                unit.count_rtaa ++;
            }else if("Adepth".equals(type)){
                unit.imp_a_rep -= System.currentTimeMillis();
                path = timeAStar.findAStarPath(from, to, unit, depth);
                unit.imp_a_rep += System.currentTimeMillis();
                unit.count_a_rep++;
            }else if("A".equals(type)){
                unit.imp_a -= System.currentTimeMillis();
                path = timeAStar.findAStarPath(from, to, unit, 1100);
                unit.imp_a += System.currentTimeMillis();
                unit.count_a++;
            }else if("EAA".equals(type)){
                //对于一个单元来说，计算一次A*就够了，这个是边缘端的计算
                TimeAStar.Path tmpPath;
                if(!unit.isAstar){
                    time -= System.currentTimeMillis();
                    unit.isAstar = true;
                    unit.aStarPath = timeAStar.findAStarPath(from,to,unit,1100);
                    time += System.currentTimeMillis();
                    unit.edge_time = time;
                }
                tmpPath = unit.aStarPath;
//                tmpPath = timeAStar.findAStarPath(from,to,unit,1100);
                //选路径往后5个点,但是怎么选择路径后的5个点
                unit.imp_eaa -= System.currentTimeMillis();
                //怎么初始化，每次iterate的时候初始化
                index = (index + 2) >= tmpPath.transitions.size() ?
                        tmpPath.transitions.size() - 1:index + 2;
                TimeAStar.Node nextNode = tmpPath.transitions.get(index).toNode();
//                System.out.println("from " + from);
//                System.out.println("index  " + index);
                path = timeAStar.findRTAAPathDisA(from,nextNode,unit,3);
//                System.out.println("path " + path);
                unit.imp_eaa += System.currentTimeMillis();
                unit.count_eaa ++;
                //记录重新计算的负载
            }else if("RRA".equals(type)){
                path = timeAStar.findRRAPath(from,to,unit,depth);
            } else{
                path = timeAStar.findAStarPath(from, to, unit, depth);
            }
            //发送path到边缘端，边缘端用RRA进行计算轨迹
            //从边缘端接受轨迹，设置为reserved逐个进行
            unit.cost = unit.cost + path.cost;
            //到终点的时候立即输出,且只输出一次
            if(unit.reached() && !unit.isOutput || "A".equals(type)){
                System.out.println("path cost " + unit + "\n\t" + unit.cost);
                unit.isOutput = true;
            }
            List<Unit.PathPoint> unitPath = new ArrayList<Unit.PathPoint>();
            unitPath.add(new Unit.PathPoint((NodePool.Node) path.startNode));
            //开始的节点为什么回事空值
            for (TimeAStar.Transition t : path.transitions) {
                NodePool.Node nextNode = (NodePool.Node) t.toNode();
                pool.reserve(unit, nextNode);
                unitPath.add(new Unit.PathPoint((NodePool.Node) nextNode));
                unit.paintNodes.add(nextNode);

                if (NodePool.RESERVE_TWO) {
                    if (!((NodePool.Transition)t).wait) {
                        NodePool.Node prevNode = (NodePool.Node) t.fromNode();
                        pool.reserve(unit, nextNode.x, nextNode.z, nextNode.t - 1);
                    }
                }
            }
            unit.setPath(unitPath);
        } catch (NoPathException npe) {
            System.out.println("couldnt found a path, setting empty for unit " + unit.id);
            npe.printStackTrace();
            reserveEmptyPath(unit);
        }
        return time;
    }
    
    private void clearReservations(Unit unit) {
        if (NodePool.RESERVE_TWO) {
//         pair reservations for non wait
            List<Unit.PathPoint> path = unit.getPath();
            for (int i = 0; i < path.size(); i++) {
                Unit.PathPoint point = path.get(i);
                pool.reclaim(point.x, point.z, point.t);
                
                if (i < path.size()-1) {
                    Unit.PathPoint next = path.get(i+1);
                    if (! point.isSamePlace(next))
                        pool.reclaim(next.x, next.z, next.t - 1);
                }
            }
        } else {
//         single reservation
            for (Unit.PathPoint point : unit.getPath()) {
                pool.reclaim(point.x, point.z, point.t);
            }
        }
        NodePool.Point location = unit.getLocation();
        pool.reserve(unit, location.x, location.z, currentTime);
    }
    
    private void reserveEmptyPath(Unit unit) {
            NodePool.Point location = unit.getLocation();
            // reserve empty wait path
            List<Unit.PathPoint> path = new ArrayList<Unit.PathPoint>();
            for (int i = 0; i <= depth; i++) {
                Unit.PathPoint point = new Unit.PathPoint(location.x, location.z, currentTime + i);
                try {
                    pool.reserve(unit, point.x, point.z, point.t);
                    path.add(point);
                } catch (Exception e) {
                    System.out.println("skipping " + point);
                }
                unit.setPath(path);
            }
    }
    
    public boolean iterate(String type) throws NoPathException {
        System.out.println("iterate time: " + currentTime);
        boolean iterated = false;
        //newUnits里面有所有的起点和终点
        if (! newUnits.isEmpty()) {
//            for (Unit unit : newUnits)
//                reserveEmptyPath(unit);
//                初始化路径的所有点
            for (Unit unit : newUnits) {
//                clearReservations(unit);
//                    清楚路径上的点
//                findPath(unit,"RTAA");
//                findPath(unit,"A");
//                findPath(unit,"Adepth");
//                findPath(unit,"A");
                if("step".equals(type)){
                    findPath(unit,"EAA");
                }else if("A".equals(type)){
                    findPath(unit,"A");
                }else if("AReplan".equals(type)){
                    findPath(unit,"Adepth");
                }else if("RTAA".equals(type)){
                    findPath(unit,"RTAA");
                }else if("EAA".equals(type)){
//                    index = 0;
                    findPath(unit,"EAA");
                }
            }
                iterated = true;
        }
//        newUnits.clear();
        currentTime++;
        return iterated;
    }


    public double iterate(String type, Unit unit) throws NoPathException {

        double time = 0;
        if("step".equals(type)){
            findPath(unit,"EAA");
        }else if("A".equals(type)){
            findPath(unit,"A");
        }else if("AReplan".equals(type)){
            findPath(unit,"Adepth");
        }else if("RTAA".equals(type)){
            findPath(unit,"RTAA");
        }else if("EAA".equals(type)){
//            index = 0;
            time = findPath(unit,"EAA");
        }
        return time;
    }

}
