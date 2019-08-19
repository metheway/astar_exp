package raft.ai.path.coop;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.Soundbank;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import raft.kilavuz.runtime.NoPathException;

public class PathPanel extends JPanel {
    static double time = 0;
    static int unitCount = 4;
    
    final Coordinater coordinater;
    Grid grid = null;
    
    int cellSize = 8;
    static int count ;
    static Object waitObject = new Object();
    /** Creates a new instance of PathPanel */
    public PathPanel(Coordinater coordinater) throws IOException {
        this.coordinater = coordinater;
        System.out.println("grid start");
        this.grid = coordinater.grid;
        for (Unit unit : coordinater.units.values()){
            unitPositions.put(unit.id, new Point(unit.getLocation()));
            usedPositions.put(unit.id, new Point(unit.getLocation()));
        }
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(20, 20);
        
        paintGrid(g2d);
        paintUnits(g2d);
        paintTrajectory(g2d);
    }

    private void paintTrajectory(Graphics2D g2d) {
        int unitRadius = cellSize * 2 / 3;
        int pathRadius = cellSize / 3;
        for (Unit unit : coordinater.units.values()) {

            g2d.setStroke(thickStroke);
            g2d.setColor(getUnitColor(unit));

            for (int i = 0; i < unit.paintNodes.size(); i++) {
                NodePool.Node node = unit.paintNodes.get(i);
                g2d.drawOval(node.x * cellSize + (cellSize - pathRadius) / 2,
                        node.z * cellSize + (cellSize - pathRadius) / 2,
                        pathRadius, pathRadius);
            }
        }
    }

    private Stroke thinStroke = new BasicStroke(1);
    private Stroke thickStroke = new BasicStroke(2);
    
    private void paintUnits(Graphics2D g2d) {
        int unitRadius = cellSize * 2 / 3;
        int pathRadius = cellSize / 3;
        
        boolean allReached = true;
        for (Unit unit : coordinater.units.values()) {
            if (!unit.reached())
                allReached = false;
            
            g2d.setStroke(thinStroke);
            g2d.setColor(getUnitColor(unit));
            g2d.setFont(g2d.getFont().deriveFont(cellSize/2f));
            //NodePool.Point point = unit.getLocation();
            Point point = unitPositions.get(unit.id);
            if (point != null) {
                g2d.fillOval((int)(point.x * cellSize + (cellSize-unitRadius)/2),
                        (int)(point.z * cellSize + (cellSize-unitRadius)/2),
                        unitRadius, unitRadius);
                g2d.setColor(Color.BLACK);
                g2d.drawString(String.valueOf(unit.id), point.x * cellSize + (cellSize/3),
                        point.z * cellSize + (cellSize*2/3));
            }
            //画unit在的地方

            g2d.setColor(getUnitColor(unit));
            g2d.setStroke(thickStroke);
            g2d.drawRect(unit.getDestination().x * cellSize + (cellSize/8),
                    unit.getDestination().z * cellSize + (cellSize/8),
                    cellSize*3/4, cellSize*3/4);
            //画目标
            g2d.setStroke(thinStroke);
            List<Unit.PathPoint> path = unit.getPath();
            for (int i = unit.getPathIndex(); i < path.size(); i++) {
                Unit.PathPoint pathPoint = path.get(i);
                g2d.drawOval(pathPoint.x * cellSize + (cellSize-pathRadius)/2,
                        pathPoint.z * cellSize + (cellSize-pathRadius)/2,
                        pathRadius, pathRadius);
                g2d.setFont(g2d.getFont().deriveFont((cellSize/4f)));
                g2d.drawString(String.valueOf(i), pathPoint.x * cellSize + cellSize/5, 
                        pathPoint.z * cellSize + cellSize/3);
            }
            //画轨迹
        }
        
        if (allReached) {
            g2d.setStroke(thinStroke);
            g2d.setColor(Color.RED);
            g2d.setFont(g2d.getFont().deriveFont(20f));
            String s = "all reached";
//            g2d.drawString(s, 100, 100);
        }
    }
    
    private void paintGrid(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        
        for (int x = 0; x <= grid.columns; x++) {
            g2d.drawLine(x * cellSize, 0, x * cellSize, grid.rows * cellSize);
        }
        
        for (int y = 0; y <= grid.rows; y++) {
            g2d.drawLine(0, y * cellSize, grid.columns * cellSize, y * cellSize);
        }

//        g2d.setColor(Color.BLACK);
        for (Grid.Node node : grid.unwalkables) {
            g2d.fillRect(node.x * cellSize, node.y * cellSize, cellSize, cellSize);
        }
    }
    
    private Map<Integer, Color> unitColors = new HashMap<Integer, Color>();
    private List<Color> colors = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN,
            Color.MAGENTA, Color.ORANGE, Color.YELLOW);
    int lastColorIndex = 0;
    
    private Color getUnitColor(Unit unit) {
        Color color = unitColors.get(unit.id);
        if (color == null) {
            color = colors.get(lastColorIndex);
            unitColors.put(unit.id, color);
            lastColorIndex++;
            if (lastColorIndex == colors.size())
                lastColorIndex = 0;
        }
        return color;
    }

    class ButtonPanel extends JPanel {
        JButton stepButton = new JButton(new AbstractAction("step") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("step");
            }
        });
        JButton AButton = new JButton(new AbstractAction("astar") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("A");
                //用A*算法时候是求路径以后走，这样输出应该是在走的过程中
            }
        });
        JButton RTAAButton = new JButton(new AbstractAction("RTAA") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("RTAA");
            }
        });
        JButton EAAButton = new JButton(new AbstractAction("EAA") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("EAA");
            }
        });
        JButton AReplanButton = new JButton(new AbstractAction("AReplan") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("AReplan");
            }
        });
        JButton RRAButton = new JButton(new AbstractAction("RAA") {
            public void actionPerformed(ActionEvent event) {
                runNextPosition("RRA");
            }
        });

        JButton resetButton = new JButton(new AbstractAction("reset") {
            public void actionPerformed(ActionEvent event) {
                reset();
            }
        });
        JButton restartButton = new JButton(new AbstractAction("restart") {
            public void actionPerformed(ActionEvent event) {
                restart();
            }
        });

        JButton animateButton = new JButton(new AbstractAction("animate") {
            public void actionPerformed(ActionEvent event) {
//                animate();
//                runNextPosition("astar");
                printWriter.println("RTAA");

//                animate("RTAA");
                synchronized (this){
                    animate("RTAA");
                }
                synchronized (this){
//                while(count != 0){
////                    System.out.println("aaa");
//                    if(count == 0){
                        System.out.println("EAA");
                        animate("EAA");
//                        count = 20;
//                        break;
//                    }
                }
////                }
                synchronized (this){
//                while(count !=0){
//                    if(count == 0){
//                        count = 20;
                        System.out.println("AReplan");
                        animate("AReplan");
//                        break;
//                    }
//                }
                }
//                synchronized (waitObject){
//                    try {
//                        if(count!= 0){
//                            waitObject.wait();
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                count = 20;
//                System.out.println("first done");
//                restart();
//                synchronized (this){
//                    try {
//                        this.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                count = 20;
//                restart();
//                printWriter.close();
            }
        });
        
        JButton stopButton = new JButton(new AbstractAction("stop") {
            public void actionPerformed(ActionEvent event) {
                animating = false;
            }
        });
        
        ButtonPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(stepButton);
            add(resetButton);
            add(animateButton);
            add(stopButton);

            add(AButton);
            add(RTAAButton);
            add(AReplanButton);
            add(EAAButton);
            add(RRAButton);
            add(restartButton);
            stepButton.setMnemonic('s');
        }
    }

    private void runNextPosition(String astar) {
        try {
            boolean isReached = false;

            float successNum = 0;
            for (Unit unit : coordinater.units.values()) {
                if(unit.reached()) {
                    unitPositions.put(unit.id, new Point(unit.getLocation()));
                    if(!unit.isOutput){
                        System.out.println("path cost " + unit + "\t" + unit.cost);
                        unit.isOutput = true;
                        //顺便也打印下implementation time
                        System.out.println("rtaa imp :" + unit.imp_rtta / unit.count_rtaa);
                        System.out.println("eaa imp :" + unit.imp_eaa / unit.count_eaa);
                        System.out.println("a rep imp :" + unit.imp_a_rep / unit.count_a_rep);
                    }
                    continue;
                }
//                    if(unit.reached() && !unit.isOutput || "A".equals(type)){
//                        System.out.println("path cost " + unit + "\n\t" + unit.cost);
//                        unit.isOutput = true;
//                    }
                if(unit.getPath().isEmpty() || unit.getPath().size() == unit.getPathIndex()){
                    coordinater.iterate(astar,unit);
                    if(astar.equals("A")){
                        System.out.println("a imp :" + unit.imp_a);
                    }//由于已经超出路径了，可以找另一个5步路径了,而
//                    unit.isAstar = false
//                    //这里因为已经进入下一个路径了，所以要更新路径的时候，;
                    //如果要新路径，那么找下
                }
                if (unit.getPath().size() > unit.getPathIndex()) {
                    if (unit.reached() ) {
                        isReached = true;
                        successNum ++;
                    }

                    unitPositions.put(unit.id, new Point(unit.getLocation()));
                    unit.next();//实际沿着path行走了
                    if(isReached){
                        isReached = false;
                        break;
                    }
                }
            }
            coordinater.currentTime ++;
//            System.out.println("success rate : " + successNum / unitCount);
        } catch (NoPathException npe) {
            npe.printStackTrace();
        }
        PathPanel.this.paintImmediately(PathPanel.this.getBounds());
    }

    Map<Integer, Point> unitPositions = new HashMap<Integer, Point>();
    Map<Integer, Point> usedPositions = new HashMap<Integer, Point>();
    List<Unit> usedUnits = new ArrayList<>();
    List<Point> usedDest = new ArrayList<>();
    Map<Integer, NodePool.Point> unitTargets = new HashMap<Integer, NodePool.Point>();
    File file = new File("output");
//    FileOutputStream fos = new FileOutputStream(file);
    FileWriter fileWriter  = new FileWriter(file,true);
    PrintWriter printWriter = new PrintWriter(fileWriter,true);


    static int suc_num =0 ;
    boolean animating = false;
    void animate(String stringT) {
        if (animating)
            return;
        animating = true;
//        System.out.println("go");
//        String string = "RTAA";
//        Runnable runnable = new Runnable(){
//                public void run() {
//                synchronized (waitObject){
                    while (animating) {
                        time-=System.currentTimeMillis();
                        try {
                            String astar = stringT;
                            try {
                                boolean isReached = false;
                                for (Unit unit : coordinater.units.values()) {
                                    if(unit.reached()) {
                                        unitPositions.put(unit.id, new Point(unit.getLocation()));

                                        if(!unit.isOutput){
                                            unit.isOutput = true;
                                            suc_num++;
                                            System.out.println(grid.file);
                                            System.out.println("path length :"  + unit + "\t" + unit.cost);
                                            System.out.println("rtaa imp :" + unit + "\t" + unit.imp_rtta / unit.count_rtaa);
                                            System.out.println("eaa imp :" + unit + "\t" + unit.imp_eaa / unit.count_eaa);
                                            System.out.println("a rep imp :" + unit + "\t" + unit.imp_a_rep / unit.count_a_rep);
                                            System.out.println("a imp :" + unit + "\t" + unit.imp_a / unit.count_a);
                                            String string;
                                            if(astar.equals("astar")){
                                                string = "map:" + grid.file + "\tpath length:" + unit + "\t" +
                                                        unit.cost;
                                                string = string + "\ta imp:" + unit.imp_a / unit.count_a;
                                                printWriter.println(string);
                                            }else if(astar.equals("AReplan")){
                                                string = "map:" + grid.file + "\tpath length:" + unit + "\t" +
                                                        unit.cost;
                                                string = string + "\ta rep imp:" + unit.imp_a_rep / unit.count_a_rep;
                                                printWriter.println(string);
                                            }else if(astar.equals("RTAA")){
                                                string = "map:" + grid.file + "\tpath length:" + unit + "\t" +
                                                        unit.cost;
                                                string = string + "\trtaa imp:" + unit.imp_rtta / unit.count_rtaa;
                                                printWriter.println(string);
                                            }else if(astar.equals("EAA")){
                                                string = "map:" + grid.file + "\tpath length:" + unit + "\t" +
                                                        unit.cost;
                                                string = string + "\ta eaa:" + unit.imp_eaa / unit.count_eaa;
                                                string += "\tedge time" + unit.edge_time;
                                                printWriter.println(string);
                                            }
                                            printWriter.flush();
                                        }
                                        continue;
                                    }//如果到达了终点，设置位置并且输出
                                    if(unit.getPath().isEmpty() || unit.getPath().size() == unit.getPathIndex()){
                                        double tmp = 0;
                                        if(unit.edge_time > 0){
                                            tmp = 0;
                                        }else{
                                            tmp = 1;
                                        }//只减一次
                                        coordinater.iterate(astar,unit);
//                                        coordinater.iterate(astar,unit);
                                        //找到路径，因为起点是当前在的点，终点是5步后的点

                                        if(tmp == 1){
                                            time -= unit.edge_time;
                                        }
//                                      //只减一次
//                                    unit.isAstar = false;
                                        //如果点了false就需要重新进行astar路径的计算
                                        //如果路径是空或者已经到了路径最长处，要新路径，那么找下
                                    }
                                    if (unit.getPath().size() > unit.getPathIndex()) {
//                                    unitPositions.put(unit.id, new Point(unit.getLocation()));
                                        unit.next();//实际沿着path行走了，从当前位置出发
                                        unitTargets.put(unit.id,unit.getLocation());
                                    }
                                }
                                coordinater.currentTime ++;
                            } catch (NoPathException npe) {
                                npe.printStackTrace();
                            }
                            //动画
                            int fps = 1;
                            for (int i = 0; i < fps; i++) {
                                for (Unit unit : coordinater.units.values()) {
                                    Point current = unitPositions.get(unit.id);
                                    NodePool.Point target = unitTargets.get(unit.id);

                                    if (current == null) {
                                        current = new Point(target);
                                        unitPositions.put(unit.id, current);
                                    }
                                    float move = 1f / fps;
                                    float dX = target.x - current.x;
                                    float dZ = target.z - current.z;

                                    current.x = (Math.abs(dX) < move) ? target.x : current.x + Math.signum(dX) * move;
                                    current.z = (Math.abs(dZ) < move) ? target.z : current.z + Math.signum(dZ) * move;

                                }
//                                SwingUtilities.invokeAndWait(new Runnable() {
//                                    public void run() {
//                                        paintImmediately(PathPanel.this.getBounds());
//                                    }
//                                });
                                Thread.sleep(1000/100/fps * 3 * 2);
                            }
                        } catch (Exception npe) {
                            npe.printStackTrace();
                        }
                        time+= System.currentTimeMillis();
                        System.out.println("time :" + time);
                        if(time > 40000){
                        System.out.println("done");
                            String string = string = "map:" + grid.file + "\tpath length:" + "\t" +
                                    "suc_rate:\t" + (float)suc_num / (float)unitCount;
                            printWriter.println(string);
                            printWriter.flush();
//                        return;
                            restart();
                            time = 0;
                            count = 0;
                            animating = false;
                            break;
                        }
                    }

//            }
//        };
//        Thread thread = new Thread(runnable);
//        thread.start();
//        synchronized (thread){
//            thread.start();
//        }
//        while(count != 0){
//            if(count == 0){
//                break;
//            }
//        }
//        System.out.println("done");
    }
    
    void reset() {
        coordinater.reset();

        List<Grid.Node> nodes = new ArrayList<Grid.Node>(grid.nodes.values());
        Collections.shuffle(nodes);
        usedUnits.clear();
        usedPositions.clear();
        usedDest.clear();
        for (int i = 0; i < unitCount; i++) {
            Unit unit = new Unit();
            usedUnits.add(unit);
            coordinater.addUnit(unit);

            Grid.Node node = nodes.remove(0);
            while (grid.unwalkables.contains(node)) {
                node = nodes.remove(0);
            }
            unit.setLocation(node.x, node.y);
            unitPositions.put(unit.id, new Point(unit.getLocation()));
            usedPositions.put(unit.id, new Point(unit.getLocation()));

            node = nodes.remove(0);
            while (grid.unwalkables.contains(node)) {
                node = nodes.remove(0);
            }
            unit.setDestination(node.x, node.y);
            usedDest.add(new Point(node.x,node.y));
            
            unit.setPath(new ArrayList<Unit.PathPoint>());
        }
        paintImmediately(getBounds());
    }
    synchronized void restart() {
        coordinater.reset();
        time = 0;
        suc_num = 0;

        for (int i = 0; i < unitCount; i++) {
            Unit unit = usedUnits.get(i);
            unit.isAstar = false;
            unit.resetVal();
            unit.paintNodes.clear();//?不可以这样吗
            coordinater.addUnit(unit);
            unit.cost = 0 ;
            unit.isOutput = false;
            Point node = usedPositions.get(unit.id);
            unit.setLocation((int)node.x, (int)node.z);
            unitPositions.put(unit.id, new Point(unit.getLocation()));

            node = usedDest.get(i);
            unit.setDestination((int)node.x, (int)node.z);

            unit.setPath(new ArrayList<Unit.PathPoint>());

            //画的轨迹也可以变
        }
        paintImmediately(getBounds());
    }
    
    class Point {
        float x, z;
        
        Point(float x, float z) {
            this.x = x;
            this.z = z;
        }
        
        Point(NodePool.Point p) {
            this(p.x, p.z);
        }
    }
    
    public static void main(String[] args) throws Exception {
        boolean first = true;
        while (true){
            if(first){
                first = false;
                System.out.println("usage PathPanel [-d <depth>] [-u <unitcount>]");

                ComLineArgs comLine = new ComLineArgs(args);

                int depth = comLine.containsArg("-d") ?
                        Integer.parseInt(comLine.getArg("-d")) : 10;
                int unitCount = comLine.containsArg("-u") ?
                        Integer.parseInt(comLine.getArg("-u")) : 100;
                PathPanel.unitCount = unitCount;
                count = unitCount;
                Coordinater coordinater = new Coordinater(depth);

                PathPanel pathPanel = new PathPanel(coordinater);

                pathPanel.setPreferredSize(new Dimension(640*2, 480*2));
                pathPanel.reset();
                ButtonPanel buttonPanel = pathPanel.new ButtonPanel();
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(pathPanel, BorderLayout.CENTER);
                panel.add(buttonPanel, BorderLayout.EAST);

//                ScrollPane scrollPane = new ScrollPane();
                JFrame frame = new JFrame("EAA* demo");
                frame.add(panel);
                frame.pack();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        }
    }
    
}
