package raft.ai.path.coop;

import java.util.*;

import raft.kilavuz.runtime.NoPathException;
import raft.kilavuz.runtime.PathContext;

public class TimeAStar {
    private Node lastNode;
    private static final boolean DEBUG = false;
    public static final boolean USE_BITSET = true;
    public static final boolean USE_INTSET = false;

    private static final Comparator<Node> comparator = new Comparator<Node>() {
        public int compare(Node one, Node two) {
            if (one.id == two.id)
                return 0;
            if (one.f < two.f)
                return -1;
            if (one.f > two.f)
                return 1;
            return (one.id < two.id) ? -1 : 1;
        }
    };

    private final SortedSet<Node> openList = new TreeSet<Node>(comparator);
    
    private final BitSet bitSetClosedList = new BitSet(1024);
    private final SortedSet<Integer> intSetClosedList = new TreeSet<Integer>();
    
    private final boolean useBitSet;
    
    /** creates a new AStar pathfinder */
    public TimeAStar() {
        this(USE_BITSET);
    }
    
    public TimeAStar(boolean useBitSet) {
        this.useBitSet = useBitSet;
    }

    private Node originalCurr = null;

    public synchronized Path findRTAAPath(Node from, Node to, Unit unit, int depth) throws NoPathException {
        if (unit == null)
            throw new NullPointerException("context is null");

        try {
            boolean solved = false;
            Node current = null;

            openList.clear();
            if (useBitSet)
                bitSetClosedList.clear();
            else intSetClosedList.clear();

            int reachedDepth = 0;

            from.transition = null;
            from.h = from.getActualTimelessCost(to);
            if (from.h < 0)
                throw new NoPathException("initial cost: " + from.h);
            from.g = 0;
            from.f = from.h;
            from.depth = 0;

            openList.add(from);
            originalCurr = from;
            int tmpDepth = 0;
            while (! openList.isEmpty()) {
                current = openList.first();
                if (! openList.remove(current))
                    assert false;

                if (useBitSet) {
                    bitSetClosedList.set(current.id);
                } else {
                    intSetClosedList.add(current.id);
                }

                if (tmpDepth > reachedDepth) {
                    reachedDepth = tmpDepth;
                    if (reachedDepth == depth) {
                        solved = true;
                        break;
                    }
                }
                tmpDepth ++;
                for (Transition transition : current.getTransitions()) {
                    float cost = transition.getCost(unit);
                    if (cost < 0)
                        continue;

                    Node neighbour = transition.toNode();

                    if (useBitSet) {
                        if (bitSetClosedList.get(neighbour.id))
                            continue;
                    } else {
                        if (intSetClosedList.contains(neighbour.id))
                            continue;
                    }

                    if (openList.contains(neighbour)) {
                        // check if this path is better
                        if (current.g + cost < neighbour.g) {

                            if (! openList.remove(neighbour))
                                assert false;

                            neighbour.transition = transition;
                            neighbour.g = current.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
//                            neighbour.depth = current.depth + 1;

                            openList.add(neighbour);

                        }
                    } else { // if neighbour in openList

                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
                        neighbour.h = neighbour.getActualTimelessCost(to);
                        neighbour.f = neighbour.g + neighbour.h;
//                        neighbour.depth = current.depth + 1;

                        openList.add(neighbour);

                    } // if-else neighbour in openList
                }
            }
            //if (reachedDepth >= depth) {
            float minF = 9999;
            float f = 9999;
            Transition finalTransition = null;
            if (solved) {
                for (Node node:openList
                     ) {
                    if(minF > node.f){
                        minF = node.f;
                    }
                }
                for (Transition transition:originalCurr.getTransitions()
                     ) {
                    Node neighbour = transition.toNode();

                    float cost = transition.getCost(unit);
//                    System.out.println("abc cost: " + cost);
                    if (cost < 0)
                        continue;
                    float h = 0;
                    if(bitSetClosedList.get(neighbour.getId())){
                        h = minF - neighbour.g;
                    }
                    float tmp = originalCurr.g + transition.getCost(unit) + neighbour.h;
                    if(f > tmp){//也制止了它回退
                        f = tmp;
                        finalTransition = transition;
                    }
                }
                Path path = new Path(from , Collections.singletonList(finalTransition),f);
                lastNode = finalTransition.toNode();
                path.lastNode = lastNode;
                return path;
            }
            return null;
        } finally {}
    }


    public synchronized Path findAStarPath(Node from, Node to, Unit unit, int depth) throws NoPathException {
        if (unit == null)
            throw new NullPointerException("context is null");

        try {
            boolean solved = false;
            Node current = null;

            openList.clear();
            if (useBitSet)
                bitSetClosedList.clear();
            else intSetClosedList.clear();

            int maxOpenSize = 0; int maxCloseSize = 0; int reachedDepth = 0;

            from.transition = null;
            from.h = from.getActualTimelessCost(to);
            if (from.h < 0)
                throw new NoPathException("initial cost: " + from.h);
            from.g = 0;
            from.f = from.h;
            from.depth = 0;

            openList.add(from);

            while (! openList.isEmpty()) {

                current = openList.first();
                if (! openList.remove(current))
                    assert false;

                if (useBitSet) {
                    bitSetClosedList.set(current.id);
                } else {
                    intSetClosedList.add(current.id);
                }

                if (to.equals(current)) {
                    solved = true;
                    break;
                }

                for (Transition transition : current.getTransitions()) {
                    float cost = transition.getCost(unit);
                    if (cost < 0)
                        continue;

                    Node neighbour = transition.toNode();

                    if (useBitSet) {
                        if (bitSetClosedList.get(neighbour.id))
                            continue;
                    } else {
                        if (intSetClosedList.contains(neighbour.id))
                            continue;
                    }

                    if (openList.contains(neighbour)) {
                        // check if this path is better
                        if (current.g + cost < neighbour.g) {

                            if (! openList.remove(neighbour))
                                assert false;

                            neighbour.transition = transition;
                            neighbour.g = current.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
                            neighbour.depth = current.depth + 1;

                            openList.add(neighbour);
                        }

                    } else { // if neighbour in openList

                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
                        neighbour.h = neighbour.getActualTimelessCost(to);
                        neighbour.f = neighbour.g + neighbour.h;
                        neighbour.depth = current.depth + 1;

                        openList.add(neighbour);

                    } // if-else neighbour in openList
                }
            }

            //if (reachedDepth >= depth) {
            if (solved) {
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
                return new Path(from, transitions, totalCost);
            } else {
                throw new NoPathException();
            }
        } finally {}
    }

    /** result of a successfull path find attempt */
    public class Path {
        /** first node in path */
        public final Node startNode;
        /** transitions to next nodes in path */
        public final List<Transition> transitions;
        public Node lastNode;
        /** more information about pathfinding enviroment  */
//        public final PathContext context;
//        /** actual cost of this path */
        public final float cost;
        
        private Path(Node startNode, List<Transition> transitions, float cost) {
            this.startNode = startNode;
//            this.context = context;
            this.cost = cost;
            this.transitions = Collections.unmodifiableList(transitions);
        }
        public String toString() {
            return "path, cost: " + cost + ", " + transitions;
        }
    }
    
    /** base class of all A* nodes */
    public abstract static class Node implements java.io.Serializable {
        private static final long serialVersionUID = 1;

        private static int lastId = 0;
        private static synchronized int nextId() { return lastId++; }

        /** id of this node, assigned during creation */
        private final int id = nextId();

        /** total estimated cost from source to dest passing through this node: g + h */
        private transient float f;
        /** cost of the shortest path found till now from source to this node */
        private transient float g;
        /** estimated cost from this node to destination, ie: heuristic */
        private transient float h;
        /** depth of this node */
        private transient int depth = 0;
        /** transition that leads to this node */
        private transient Transition transition;

        /** only subclasses may call this constructor */
        protected Node() {}

        /** returns id of this node. id's are assigned during creation of nodes. */
        public final int getId() {
            return id;
        }

        /** returns a collection of transitions from this node.
         * if an adjacent node is unreachable result may contain it
         * with a negative cost */
        public abstract Collection<Transition> getTransitions();

        /** returns the cost estimate from this node to destination node.
         * if cannot be estimated, returning 0 will also result in finding a solution
         * but generally should take much time */
        public abstract float getActualTimelessCost(Node dest) throws NoPathException;

    }

    /** a transition between two nodes */
    public static interface Transition {
        /** constant to indicate cost for transition is infinite and
         * hence transition is not possible */
        public static final float INFINITE_COST = -1f;

        /** returns the node this transition originates from */
        public Node fromNode();
        /** returns the node this transition leads to */
        public Node toNode();
        public float getCost(Unit unit);
    }
}
