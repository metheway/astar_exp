package raft.ai.path.coop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Unit {
    
    private static int lastId = 0;
    public boolean isOutput = false;

    public double imp_rtta = 0;
    public double imp_a_rep = 0;
    public double imp_eaa = 0;
    public double imp_a = 0;

    public int count_rtaa = 0;
    public int count_a_rep = 0;
    public int count_eaa = 0;
    public int count_a = 0;
    public double edge_time = 0;

    private static synchronized final int nextId() {
        return lastId++;
    }

    public final int id = nextId();
    public float cost = 0;
    NodePool.Point destination = null;
    
    private int x = 0;
    private int z = 0;
    public ArrayList<NodePool.Node> paintNodes = new ArrayList<>();

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    private int pathIndex = 0;
    private final List<PathPoint> path = new ArrayList<PathPoint>();
    TimeAStar.Path aStarPath;
    public boolean isAstar = false;
    /** Creates a new instance of Unit */
    public Unit() {
    }
    
    public void setLocation(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    public NodePool.Point getLocation() {
        return new NodePool.Point(x, z);
    }
    int getPathIndex() {
        return pathIndex;
    }
    
    boolean reached() {
        return (this.x == destination.x) && (this.z == destination.z);
    }
    
    public void next() {
        pathIndex++;
        if (pathIndex < path.size()) {
            PathPoint location = path.get(pathIndex);
            setLocation(location.x, location.z);
        }
    }
    
    public void setDestination(int destX, int destZ) {
        this.destination = new NodePool.Point(destX, destZ);
    }
    
    public NodePool.Point getDestination() {
        return destination;
    }
    
    void setPath(List<PathPoint> path) {
        this.path.clear();
        this.path.addAll(path);
        
        this.pathIndex = 0;
        if (! path.isEmpty()) {
            PathPoint location = path.get(0);
            setLocation(location.x, location.z);
        }
    }
    
    List<PathPoint> getPath() {
        return Collections.unmodifiableList(path);
    }

    public int hashCode() {
        return id;
    }
    
    public boolean equals(Object o) {
        return (o instanceof Unit) ? equals((Unit)o) : false;
    }

    public String toString() {
        return "Unit: " + id;
    }

    public TimeAStar.Path getAPath() {
        return aStarPath;
    }

    public void resetVal() {
        this.count_a_rep = this.count_a= this.count_eaa = this.count_rtaa = 0;
        this.imp_a_rep = this.imp_a = this.imp_eaa = this.imp_rtta = 0;
    }

    static class PathPoint {
        public final int x;
        public final int z;
        public final long t;
        
        public PathPoint(NodePool.Node node) {
            this(node.x, node.z, node.t);
        }
        
        public PathPoint(int x, int z, long t) {
            this.x = x;
            this.z = z;
            this.t = t;
        }
        public boolean isSamePlace(PathPoint other) {
            return (this.x == other.x) && (this.z == other.z);
        }
    }
}
