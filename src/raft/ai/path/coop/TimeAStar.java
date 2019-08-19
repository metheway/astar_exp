package raft.ai.path.coop;

import java.util.*;

import raft.kilavuz.runtime.NoPathException;

public class TimeAStar {
    private Node lastNode;
    private static final boolean DEBUG = false;
    public static final boolean USE_BITSET = true;
    public static final boolean USE_INTSET = false;

    private static final Comparator<Node> comparator = new Comparator<Node>() {
        public int compare(Node one, Node two) {
            if (one.id == two.id)
                return 0;
            if (one.f  < two.f )
                return -1;
            if (one.f  > two.f )
                return 1;
            return (one.id < two.id) ? -1 : 1;
        }
    };
    private static final Comparator<Node> Gcomparator = new Comparator<Node>() {
        public int compare(Node one, Node two) {
            if (one.id == two.id)
                return 0;
            if (one.f   < two.f  )
                return -1;
            if (one.f  > two.f  )
                return 1;
            return (one.id < two.id) ? -1 : 1;
        }
    };

    private final SortedSet<Node> openList = new TreeSet<Node>(comparator);
    private final SortedSet<Node> GopenList = new TreeSet<Node>(Gcomparator);

    private final BitSet bitSetClosedList = new BitSet(1024);
    private final BitSet reBitSetClosedList = new BitSet(1024);
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
            GopenList.clear();
            if (useBitSet)
                bitSetClosedList.clear();
            else intSetClosedList.clear();
            int reachedDepth = 0;

            from.transition = null;
//            from.h = from.getActualTimelessCost(to,"manhattan");
            from.h = from.getActualTimelessCost(to,"sqr");
//            from.h = from.getActualTimelessCost(to, "astar");
            if (from.h < 0)
                throw new NoPathException("initial cost: " + from.h);
            from.g = 0;
            from.f = from.h;
            from.depth = 0;

            GopenList.add(from);
            originalCurr = from;
            int tmpDepth = 0;
            while (! GopenList.isEmpty()) {
                current = GopenList.first();
                if (! GopenList.remove(current))
                    assert false;

                if(bitSetClosedList.get(current.id)){
                    continue;
                }

                if (useBitSet) {
                    bitSetClosedList.set(current.id);
                } else {
                    intSetClosedList.add(current.id);
                }

                if (tmpDepth > reachedDepth ) {
                    reachedDepth = tmpDepth;
                    if (reachedDepth == depth ) {
                        solved = true;
                        break;
                    }
                }
                if(to.equals(current)){
                    break;
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

                    if (GopenList.contains(neighbour)) {
                        // check if this path is better
                        if (current.g + cost < neighbour.g) {
                            if (! GopenList.remove(neighbour))
                                assert false;
                            neighbour.transition = transition;
                            neighbour.g = current.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
//                            neighbour.depth = current.depth + 1;
                            GopenList.remove(neighbour);
                            GopenList.add(neighbour);
                        }
                    } else { // if neighbour in openList
                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
//                        neighbour.h = neighbour.getActualTimelessCost(to,"manhattan");
                        neighbour.h = neighbour.getActualTimelessCost(to,"sqr");
//                        neighbour.h = neighbour.getActualTimelessCost(to, "astar");
                        neighbour.f = neighbour.g + neighbour.h;
//                        neighbour.depth = current.depth + 1;
                        GopenList.add(neighbour);
                    } // if-else neighbour in openList
                }
            }
            float minF = 99999;
            if(solved){
//                for (Node node: openList
//                     ) {
//                    if(minF > node.f){
//                        minF = node.f;
//                    }
//                }
                minF = GopenList.first().f;
                //直接从起点出发一直到current
                Node reCurr = from;
                openList.clear();
                openList.add(reCurr);
//                bitSetClosedList.clear();
                int reTmpDepth = 0;
                while(!openList.isEmpty()){
                    reCurr = openList.first();
//                    System.out.println("solving " + reCurr);
                    if (! openList.remove(reCurr)) {
                        assert false;
                    }
//                    System.out.println("contains " + openList.contains(reCurr));

//                    if(bitSetClosedList.get(reCurr.getId())){
//                        continue;
//                    }
                    if(reBitSetClosedList.get(reCurr.getId())){
                        continue;
                    }
                    reBitSetClosedList.set(reCurr.getId());

                    if(reTmpDepth < 20){
                        break;
                    }
                    if(reCurr.equals(current)){
                        break;
                    }//如果reCurr到了一定深度的点

                    if(to.equals(reCurr)){
                        break;
                    }//这个是到了终点
//                    bitSetClosedList.set(reCurr.getId());
                    reTmpDepth ++;
                    for (Transition transition:reCurr.getTransitions()
                         ) {
                        float cost = transition.getCost(unit);
                        if(cost < 0)continue;
                        Node neighbour = transition.toNode();
//                        if(bitSetClosedList.get(neighbour.getId())){
//                            continue;
//                        }
                        if(reBitSetClosedList.get(neighbour.getId())){
                            continue;
                        }
                        if(openList.contains(neighbour)){
                            if(current.g + cost < neighbour.g){
                                openList.remove(neighbour);
                                neighbour.g = current.g + cost;
                                neighbour.f = neighbour.g + neighbour.h;
                                neighbour.transition = transition;
                                openList.add(neighbour);
                            }
                        }else{
                            if(bitSetClosedList.get(neighbour.getId())){
                                neighbour.h = minF - neighbour.g;
                            }
                            neighbour.g = current.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
                            neighbour.transition = transition;
                            openList.add(neighbour);
                        }
                    }
                }
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
//                while (current.transition != null) {
//                    transitions.add(0, current.transition);
//                    current = current.transition.fromNode();
//                }
                Transition tmpTrans = null;
                while(current.transition != null){
                    tmpTrans = current.transition;
                    current = current.transition.fromNode();
                }
                transitions.add(0,tmpTrans);
                Path path = new Path(from, transitions, totalCost);
//                System.out.println("check path1 " + path);
                path.lastNode = current;
                return path;
            }else{
//                如果直接找到了终点，那么直接用路径就可以了
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
                Path path = new Path(from, transitions, totalCost);
//                System.out.println("check path2 " + path);
                path.lastNode = current;
                return path;
            }
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
            int reachedDepth = 0;

            from.transition = null;
            from.h = from.getActualTimelessCost(to,"manhattan");
//            from.h = from.getActualTimelessCost(to,"sqr");
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
                ///??????????????////
                if(bitSetClosedList.get(current.id)){
                    continue;
                }//??????

                if (useBitSet) {
                    bitSetClosedList.set(current.id);
                } else {
                    intSetClosedList.add(current.id);
                }

//                System.out.println("current  " + current);
                if (current.depth > reachedDepth || to.equals(current)) {
                    reachedDepth = current.depth;
                    if(current.equals(to)){
                        break;
                    }
                    if (reachedDepth == depth) {
                        solved = true;
                        break;
                    }
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

                    } else { // if neighbour not in openList
                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
                        neighbour.h = neighbour.getActualTimelessCost(to, "manhattan");
//                        neighbour.h = neighbour.getActualTimelessCost(to, "sqr");
                        neighbour.f = neighbour.g + neighbour.h;
                        neighbour.depth = current.depth + 1;

                        openList.add(neighbour);
                    } // if-else neighbour in openList
                }
            }
            if (solved) {
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
//                System.out.println("done");
                return new Path(from, transitions, totalCost);
            } else {
                throw new NoPathException();
            }
        } finally {}
    }

    public Path findRRAPath(Node from, Node to, Unit unit, int depth) throws NoPathException {
        SortedSet<Node> reOpenList = new TreeSet<Node>(comparator);
        BitSet reBitSetClosedList = new BitSet(1024);

        if (unit == null)
            throw new NullPointerException("context is null");
        try {
            boolean solved = false;
            Node current = null;
            Node reCurr = null;
            openList.clear();
            bitSetClosedList.clear();
            reOpenList.clear();
            reBitSetClosedList.clear();

            from.transition = null;
//            from.h = from.getActualTimelessCost(to,"manhattan");
            from.h = from.getActualTimelessCost(to,"sqr");
            if (from.h < 0)
                throw new NoPathException("initial cost: " + from.h);
            from.g = 0;
            from.f = from.h;

            to.transition = null;
//            to.h = to.getActualTimelessCost(from,"manhattan");
            to.h = to.getActualTimelessCost(from,"sqr");

            if(to.h <0){
                throw new NoPathException("initial cost from end " + to.h);
            }
            to.g = 0;
            to.f = to.h;

            openList.add(from);
            reOpenList.add(to);

            while (! openList.isEmpty() && !reOpenList.isEmpty()) {
                current = openList.first();
                reCurr = reOpenList.first();
                if (! openList.remove(current))
                    assert false;
                reOpenList.remove(reCurr);

                if(bitSetClosedList.get(current.id)){
                    continue;
                }//??????
                if(reBitSetClosedList.get(reCurr.id)){
                    continue;
                }

                bitSetClosedList.set(current.id);
                reBitSetClosedList.set(reCurr.id);


                if(bitSetClosedList.get(reCurr.id)){
                    solved = true;
                    break;
                }

                for (Transition transition : current.getTransitions()) {
                    float cost = transition.getCost(unit);
                    if (cost < 0)
                        continue;
                    Node neighbour = transition.toNode();

                    if(bitSetClosedList.get(neighbour.id)){
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
                            openList.add(neighbour);
                        }
                    } else { // if neighbour not in openList
                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
//                        neighbour.h = neighbour.getActualTimelessCost(to, "manhattan");
                        neighbour.h = neighbour.getActualTimelessCost(to, "sqr");
                        neighbour.f = neighbour.g + neighbour.h;
                        openList.add(neighbour);
                    } // if-else neighbour in openList
                }

                for (Transition transition : reCurr.getReTransitions()) {
                    float cost = transition.getCost(unit);
                    if (cost < 0)
                        continue;
                    Node neighbour = transition.fromNode();

                    if(bitSetClosedList.get(neighbour.id)){
                        continue;
                    }

                    if (reOpenList.contains(neighbour)) {
                        if (current.g + cost < neighbour.g) {
                            if (! reOpenList.remove(neighbour))
                                assert false;
                            neighbour.transition = transition;
                            neighbour.g = reCurr.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
                            reOpenList.add(neighbour);
                        }
                    } else { // if neighbour not in openList
                        neighbour.transition = transition;
                        neighbour.g = reCurr.g + cost;
//                        neighbour.h = neighbour.getActualTimelessCost(to, "manhattan");
                        neighbour.h = neighbour.getActualTimelessCost(to, "sqr");
                        neighbour.f = neighbour.g + neighbour.h;
                        reOpenList.add(neighbour);
                    } // if-else neighbour in openList
                }
            }
            //其实保存的时间是错的，而且路径是反的
            if (solved) {
                //如果遇到了求路径
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
                while(reCurr.transition != null){
                    transitions.add(transitions.size(),reCurr.transition);
                    reCurr = reCurr.transition.toNode();
                }
//                System.out.println("done");
                return new Path(from, transitions, totalCost);
            } else {
                throw new NoPathException();
            }
        } finally {}
    }

    public Path findRTAAPathDisA(Node from, Node to, Unit unit, int depth) throws NoPathException {
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
//            from.h = from.getActualTimelessCost(to,"manhattan");
//            from.h = from.getActualTimelessCost(to,"sqr");
            from.h = from.getActualTimelessCost(to, "astar");
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

                if(bitSetClosedList.get(current.id)){
                    continue;
                }

                if (useBitSet) {
                    bitSetClosedList.set(current.id);
                } else {
                    intSetClosedList.add(current.id);
                }

                if (tmpDepth > reachedDepth ) {
                    reachedDepth = tmpDepth;
                    if (reachedDepth == depth ) {
                        solved = true;
                        break;
                    }
                }
                if(to.equals(current)){
                    break;
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
                            openList.remove(neighbour);
                            openList.add(neighbour);
                        }
                    } else { // if neighbour in openList
                        neighbour.transition = transition;
                        neighbour.g = current.g + cost;
//                        neighbour.h = neighbour.getActualTimelessCost(to,"manhattan");
//                        neighbour.h = neighbour.getActualTimelessCost(to,"sqr");
                        neighbour.h = neighbour.getActualTimelessCost(to, "astar");
                        neighbour.f = neighbour.g + neighbour.h;
//                        neighbour.depth = current.depth + 1;
                        openList.add(neighbour);
                    } // if-else neighbour in openList
                }
            }
            float minF = 99999;
            if(solved){
//                for (Node node: openList
//                     ) {
//                    if(minF > node.f){
//                        minF = node.f;
//                    }
//                }
                minF = openList.first().f;
                //直接从起点出发一直到current
                Node reCurr = from;
                openList.clear();
                openList.add(reCurr);
//                bitSetClosedList.clear();
                int reTmpDepth = 0;
                while(!openList.isEmpty()){
                    reCurr = openList.first();
//                    System.out.println("solving " + reCurr);
                    if (! openList.remove(reCurr)) {
                        assert false;
                    }
//                    System.out.println("contains " + openList.contains(reCurr));

//                    if(bitSetClosedList.get(reCurr.getId())){
//                        continue;
//                    }
                    if(reBitSetClosedList.get(reCurr.getId())){
                        continue;
                    }
                    reBitSetClosedList.set(reCurr.getId());

                    if(reTmpDepth < 20){
                        break;
                    }
                    if(reCurr.equals(current)){
                        break;
                    }//如果reCurr到了一定深度的点

                    if(to.equals(reCurr)){
                        break;
                    }//这个是到了终点
//                    bitSetClosedList.set(reCurr.getId());
                    reTmpDepth ++;
                    for (Transition transition:reCurr.getTransitions()
                    ) {
                        float cost = transition.getCost(unit);
                        if(cost < 0)continue;
                        Node neighbour = transition.toNode();
//                        if(bitSetClosedList.get(neighbour.getId())){
//                            continue;
//                        }
                        if(reBitSetClosedList.get(neighbour.getId())){
                            continue;
                        }
                        if(openList.contains(neighbour)){
                            if(current.g + cost < neighbour.g){
                                openList.remove(neighbour);
                                neighbour.g = current.g + cost;
                                neighbour.f = neighbour.g + neighbour.h;
                                neighbour.transition = transition;
                                openList.add(neighbour);
                            }
                        }else{
//                            if(bitSetClosedList.get(neighbour.getId())){
                                neighbour.h = minF - neighbour.g;
//                            }
                            neighbour.g = current.g + cost;
                            neighbour.f = neighbour.g + neighbour.h;
                            neighbour.transition = transition;
                            openList.add(neighbour);
                        }
                    }
                }
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
                Path path = new Path(from, transitions, totalCost);
//                System.out.println("check path1 " + path);
                path.lastNode = current;
                return path;
            }else{
//                如果直接找到了终点，那么直接用路径就可以了
                float totalCost = current.g;
                List<Transition> transitions = new ArrayList<Transition>();
                while (current.transition != null) {
                    transitions.add(0, current.transition);
                    current = current.transition.fromNode();
                }
                Path path = new Path(from, transitions, totalCost);
//                System.out.println("check path2 " + path);
                path.lastNode = current;
                return path;
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
        public abstract Collection<Transition> getReTransitions();

        /** returns the cost estimate from this node to destination node.
         * if cannot be estimated, returning 0 will also result in finding a solution
         * but generally should take much time */
        public abstract float getActualTimelessCost(Node dest, String type) throws NoPathException;

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
