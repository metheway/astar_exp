package raft.ai.path.coop;

import raft.kilavuz.runtime.NoPathException;

import java.util.*;

public class NodePool {
    
    static final boolean RESERVE_TWO = true;
    private final SortedMap<String, Node> usedNodes = new TreeMap<String, Node>();
    private final List<Node> pool = new ArrayList<Node>();

    private final SortedMap<String, Unit> reserved = new TreeMap<String, Unit>();
    
    private final SortedMap<Integer, Unit> units = new TreeMap<Integer, Unit>();

    final Grid grid;
    
    /** Creates a new instance of NodePool */
    public NodePool(Grid grid) {
        this.grid = grid;
    }

    public boolean isReserved(Node node) {
        return isReserved(node.x, node.z, node.t);
    }
    
    public boolean isReserved(int x, int y, long t) {
        String key = x + ":" + y + ":" + t;
        return reserved.containsKey(key);
    }
    
    public void reserve(Unit unit, Node node) {
        reserve(unit, node.x, node.z, node.t);
    }
    public void reserve(Unit unit, int x, int y, long t) {
        String key = x + ":" + y + ":" + t;
        Unit oldUnit = reserved.get(key);
        if (oldUnit != null)
            throw new IllegalStateException("already reserved: " + key + " by " + oldUnit.id + " attempting: " + unit.id);
        reserved.put(key, unit);
    }

    public void reclaim(Node node) {
        reclaim(node.x, node.z, node.t);
    }
    public void reclaim(int x, int y, long t) {
        String key = x + ":" + y + ":" + t;
        if (reserved.remove(key) == null)
            throw new IllegalStateException("not reserved: " + key);
    }
    public void reclaimAll() {
        reserved.clear();
    }

    
    private int count = 0;
    public Node acquireNode(int x, int y, long t) {
        String key = x + ":" + y + ":" + t;
        Node node = usedNodes.get(key);
        if (node == null) {
            if (pool.isEmpty()) {
                node = new Node(x, y, t);
            } else {
                node = pool.remove(0);
                node.init(x, y, t);
            }
            usedNodes.put(key, node);
        } else {
        }
        return node;
//        Node node = new Node(x,y,t);
//        return node;
    }
    
    public class Node extends TimeAStar.Node implements Comparable{
        int x;
        int z;
        long t;
        
        private List<TimeAStar.Transition> transitions;
        
        private Node(int x, int z, long t) {
            init(x, z, t);
        }
        
        private void init(int x, int z, long t) {
            this.x = x;
            this.z = z;
            this.t = t;
            transitions = null;
        }
        
        public Collection<TimeAStar.Transition> getTransitions() {
            if (transitions == null) {
                transitions = new ArrayList<TimeAStar.Transition>();
                for (Grid.Node node : grid.getNeighbours(x, z)) {
                    transitions.add(new Transition(this, acquireNode(node.x, node.y, t + 1)));
                transitions.add(new Transition(this, acquireNode(x, z, t + 1)));
            }
            // wait
        }
            return transitions;
        }
        public Collection<TimeAStar.Transition> getReTransitions() {
            if (transitions == null) {
                transitions = new ArrayList<TimeAStar.Transition>();
                for (Grid.Node node : grid.getNeighbours(x, z)) {
                    transitions.add(new Transition(acquireNode(node.x, node.y, t + 1),this));
                    transitions.add(new Transition(acquireNode(x, z, t + 1),this));
                }
                // wait
            }
            return transitions;
        }
        /** actual timeless cost */
        public float getActualTimelessCost(TimeAStar.Node dest, String type) throws NoPathException {
            Node node = (Node) dest;
            if("astar".equals(type)){
                return grid.getActualCost(x, z, node.x, node.z);
            }else if("manhattan".equals(type)){
                return Math.abs(this.x - node.x) + Math.abs(this.z - node.z);
            }else{
                return (float) (Math.pow(this.x - node.x,2) + Math.pow(this.z - node.z,2));
            }
        }
        
        public String toString() {
            return "(" + x + ", " + z + ", " + t + ")";
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Node){
                return (x - ((Node) o).x) * (z - ((Node) o).z);
            }
            return -1;
        }


    }

    public class Transition implements TimeAStar.Transition {
        final Node fromNode;
        final Node toNode;
        final boolean wait;
        
        private Transition(Node fromNode, Node toNode) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.wait = (fromNode.x == toNode.x) && (fromNode.z == toNode.z);
        }
        
        public TimeAStar.Node fromNode() {
            return fromNode;
        }
        
        public TimeAStar.Node toNode() {
            return toNode;
        }
        
        public float getCost(Unit unit) {
            if (isReserved(toNode))
                return INFINITE_COST;
            
            if (RESERVE_TWO) {
                if (!wait && isReserved(toNode.x, toNode.z, toNode.t - 1))
                    return INFINITE_COST;
            }

            if (wait && (unit.getDestination().x == fromNode.x) &&
                    (unit.getDestination().z == fromNode.z)) {
                return 0;
            }
            if(fromNode.x == toNode.x ||fromNode.z == toNode.z){
                return 1;
            }else{
                return 1.4f;
            }
        }
        
        public String toString() {
            return "tr to: " + toNode;
        }
    }
    
    static class Point {
        public int x;
        public int z;
        
        Point(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public boolean equals(Object o) {
            return (o instanceof Point) ? equals((Point)o) : false;
        }
        
        public boolean equals(Point other) {
            return (this.x == other.x) && (this.z == other.z);
        }
        
        public String toString() {
            return "P " + x + "," + z;
        }
        
    }
}
