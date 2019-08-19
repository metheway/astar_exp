package raft.ai.path.coop;

import java.io.*;
import java.util.*;
import raft.kilavuz.runtime.*;

/**
 *
 * @author hakan eryargi (r a f t)
 */
public class Grid {
    static String[] GRID = new String[1073/5 ];
    // /10
//    static String[] GRID = {
//        "........X..X.",
//        "...XXXX.X..X.",
//        "....X.X.X..X.",
//        "..X...X......",
//        ".XXXXXXXX..X.",
//        "......X....X.",
//        ".X..XXX.XX.X.",
//        ".X...........",
//        ".XXXX.XXXXXX.",
//        ".............",
//    };
    
    int rows = 0;
    int columns = 0;
    
    Node[][] grid = null;
    Set<Node> unwalkables = new HashSet<Node>(); // tmp
//    public String name ="";
    final SortedMap<Integer, Node> nodes = new TreeMap<Integer, Node>();
    private final SortedMap<NodePair, Float> actualCosts = new TreeMap<NodePair, Float>();
    public String file = "Sydney_2_1024" ;
    /** Creates a new instance of Grid */
    public Grid() throws IOException {
//        file = "img";
        File fileName = new File(file);
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line = "";
        int index = 0;
        while((line = bufferedReader.readLine())!= null){
            GRID[index++] = line;
        }
        bufferedReader.close();
        fileReader.close();

        this.rows = GRID.length;
        this.columns = GRID[0].length();
        System.out.println(rows);
        System.out.println(columns);

        this.grid = new Node[columns][rows];
        
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Node node = new Node(x, y);
                grid[x][y] = node;
                nodes.put(node.getId(), node);
                if (GRID[y].charAt(x) == 'X')
                    unwalkables.add(node);
            }
        }
        initializeNeigbours();
//        calculateActualCosts();
    }
    
    private void initializeNeigbours() {
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Node node = grid[x][y];
                List<Node> neighbours = new ArrayList<Node>();
                for (int xi = -1; xi <=1; xi++) {
                    for (int yi = -1; yi <=1; yi++) {
                        if ((xi == 0) && (yi == 0))
                            continue;
//                        if ((xi != 0) && (yi != 0))
//                            continue;
                        //斜线也加上
                        try {
                            if(x + xi>=columns || x+ xi < 0 ||
                            y + yi >= rows || y + yi < 0){
                                continue;
                            }
                            Node neighbour = grid[x+xi][y+yi];
                            if (getCost(node, neighbour) >= 0)
                                neighbours.add(neighbour);
                        } catch (ArrayIndexOutOfBoundsException e) {}
                    }
                }
                node.setNeighbours(neighbours);
            }
        }
    }

    private void calculateActualCosts() {
        AStar astar = new AStar();
        PathContext context = new PathContext();

        for (Node from : nodes.values()) {
            for (Node to : nodes.values()) {
                NodePair pair = new NodePair(from, to);
                float cost = AStar.Transition.INFINITE_COST;
                try {
                    AStar.Path path = astar.findPath(from, to, context);
                    cost = path.cost;
                } catch (NoPathException npe) {}
                actualCosts.put(pair, cost);
            }
        }
    }
    private AStar.Path calculateActualCosts(Node from, Node to) throws NoPathException {
        AStar astar = new AStar();
        PathContext context = new PathContext();

        NodePair pair = new NodePair(from, to);
//        float cost = AStar.Transition.INFINITE_COST;
        AStar.Path path = astar.findPath(from, to, context);
//        cost = path.cost;
        return path;

    }
    
    
    private float getCost(Node from, Node to) {
        // tmp
        if (unwalkables.contains(to) || unwalkables.contains(from))
            return TimeAStar.Transition.INFINITE_COST;
        
        if ((from.x == to.x) || (from.y == to.y))
            return 1f;
        return 1.4f;
    }
    
    List<Node> getNeighbours(int x, int y) {
        return grid[x][y].neighbours;
    }
    /** returns the timeless precalculated cost */
    float getActualCost(int fromX, int fromY, int toX, int toY) throws NoPathException {
        Node from = grid[fromX][fromY];
        Node to = grid[toX][toY];
        AStar.Path path = calculateActualCosts(from,to);
        return path.cost;
    }
    public class Node extends AStar.Node {
        public final int x;
        public final int y;
        private List<Node> neighbours;
        private List<AStar.Transition> transitions;
        
        private Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        private void setNeighbours(List<Node> neighbours) {
            this.neighbours = neighbours;
            
            this.transitions = new ArrayList<AStar.Transition>();
            for (Node neighbour : neighbours)
                this.transitions.add(new Transition(this, neighbour));
        }
        
        /** manhattan distance */
        public float getCostEstimate(AStar.Node dest, PathContext context) {
            Node node = (Node) dest;
            return (Math.abs(x - node.x) + Math.abs(y - node.y));
        }
        
        public Collection<AStar.Transition> getTransitions() {
            return transitions;
        }
        
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
    
    public class Transition implements AStar.Transition {
        final Node fromNode, toNode;
        
        private Transition(Node fromNode, Node toNode) {
            this.fromNode = fromNode;
            this.toNode = toNode;
        }
        
        public AStar.Node fromNode() {
            return fromNode;
        }
        
        public AStar.Node toNode() {
            return toNode;
        }
        /** manhattan distance */
        public float getCost(PathContext context) {
            return (Math.abs(fromNode.x - toNode.x) + Math.abs(fromNode.y - toNode.y));
        }
        
        public String toString() {
            return "tr: " + fromNode + " - " + toNode;
        }
    }
    
    private class NodePair implements Comparable<NodePair> {
        final Node from;
        final Node to;
        
        private NodePair(Node from, Node to) {
            this.from = from;
            this.to = to;
        }
        
        public int compareTo(NodePair other) {
            if ((from.getId() == other.from.getId()) && (to.getId() == other.to.getId())) {
                return 0;
            } else if (from.getId() < other.from.getId()) {
                return -1;
            } else if (from.getId() > other.from.getId()) {
                return 1;
            } else {
                return (to.getId() > other.to.getId()) ? 1 : -1;
            }
        }
        
        public String toString() {
            return "(" + from.x + ", " + from.y + ") - (" + to.x + ", " + to.y + ")";
        }
    }
}
