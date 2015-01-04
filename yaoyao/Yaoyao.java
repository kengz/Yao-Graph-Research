// v0.9, 8/1/13

import java.io.*;
import acm.graphics.*;
import acm.program.*;
import acm.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import acm.gui.*;
import java.text.DecimalFormat;


public class Yaoyao extends GraphicsProgram implements ItemListener{
    public static final int
        APPLICATION_WIDTH = 1200,
        APPLICATION_HEIGHT = 900, // windows size
        DIAG = APPLICATION_WIDTH + APPLICATION_HEIGHT,
        MAX = 1000, // max number of points
        SIZE = 8; // size of pts
    
    
    
    GOval[] pt; // pts
    int cnt = 0; // num of pts
    GPoint lastPoint; // last location of the selected point
    GPoint selectedLoc = new GPoint(APPLICATION_WIDTH/2, APPLICATION_HEIGHT/2);
    GOval draggedPt; 
    boolean isDragging = false;
    boolean isDrawCone = true; //draw the cones or not
    boolean isDrawDirection = true; //draw the directions or not
    boolean isDrawPath = false; // draw shortest path or not
    boolean isDrawLabel = false; // draw label of points or not
    int k = 5; // num of cones
    
    IntField kField, startField, endField;
    DoubleField xField, yField; // textfield for num of cones
    GLine[] line = new GLine[2*MAX*MAX]; // set of lines
    int lineCnt = 0; // count of lines
    
    int typeIdx = 0; // type of graph, see type below
    String currentType; 
    JLabel typeLabel, rLabel;
    
    GLabel[] label;
    
    String[] type = {"Yao", "Yao-Yao", "Sym-Yao"};
    JComboBox typeChooser = new JComboBox(type); // GUI for choosing types
    
    
    // initialize
    public void init() {
        currentType=type[typeIdx]; // get the type
        typeLabel= new JLabel(currentType); // set the label
        
        pt = new GOval[MAX]; // create array of pts
        label = new GLabel[MAX];
        
        kField = new IntField(k); // create textfield for the number of cones
        kField.addActionListener(this); 
        add(new JLabel("V.9 k =  "), NORTH);
        
        add(kField, NORTH); 
        
        add(new JButton("Cones"), NORTH); // turn on and off cones
        add(new JButton("Direct"), NORTH); // turn on and off directions
        add(typeChooser, NORTH); // add type chooser GUI
        
        
        typeChooser.setSelectedIndex(0); // default type 0: Yao
        typeChooser.addItemListener(this);
        
        xField = new DoubleField(0);
        yField = new DoubleField(0);
        add(xField, NORTH);   
        add(yField, NORTH); 
        add(new JButton("Add Pt"), NORTH); // add a point by x and y coordinate
        
        add(new JButton("Load"), NORTH); // load from and save to file the points locations
        add(new JButton("Save"), NORTH); 
        
        add(new JButton("Label"), NORTH);  // draw label of points or not
        
        
        startField = new IntField(0); // start and end point
        endField = new IntField(1);
        
        startField.addActionListener(this); 
        add(startField, NORTH); 
        
        endField.addActionListener(this); 
        add(endField, NORTH); 
        
        add(new JButton("Path"), NORTH); // find the shortest path
        rLabel = new JLabel("1.00000000");
        add(new JLabel("r = "), NORTH); // show stretch factor
        add(rLabel, NORTH); 
        
        addActionListeners();
        addMouseListeners();
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        //System.out.println(cmd);
        
        if (cmd.equals("Cones")) { // turn on and off cones
            
            isDrawCone = !isDrawCone;
            draw();
            
        } else if (cmd.equals("Direct")) { // turn on and off directions
            
            isDrawDirection = !isDrawDirection;
            draw();
            
        } else if (cmd.equals("Add Pt")) { // turn on and off directions
            
            addPt(new GPoint(selectedLoc.getX()+xField.getValue(), selectedLoc.getY()-yField.getValue()));
            
        } else if (cmd.equals("Load")) { // load from file points locations
            
            loadFile();
            
        } else if (cmd.equals("Save")) { // save to file points locations
            
            saveFile();
            
        } else if (cmd.equals("Path")) { // save to file points locations
            
            isDrawPath = !isDrawPath;
            draw();
            
        } else if (cmd.equals("Label")) { // save to file points locations
            
            isDrawLabel = !isDrawLabel;
            draw();
            
        } else { // change number of cones
            
            k = kField.getValue();
            draw();
            
        }
    }
    
    // change graph type
    public void itemStateChanged(ItemEvent e) {
        
        if (e==null || e.getStateChange() == ItemEvent .SELECTED) {
            //currentType = (String)(e.getItem());
            draw();
        }
        
    }
    
    
    // load a file from disk
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        
        // if user has chosen a file
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            try {
                BufferedReader input = new BufferedReader(new FileReader(file));
                
                String line = input.readLine(); // read a line from the file
                
                
                
                while(line != null && line.length() > 0) { // not at the end of the file yet
                    String[] pair = line.split(" "); // a line contains the x- and y-coordinates of a point
                    double x = Double.parseDouble(pair[0]);
                    double y = Double.parseDouble(pair[1]);
                    addPt(new GPoint(x,y));
                    line = input.readLine();
                }
                
                input.close();
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e);
            }
            
        }
    }
    
    // save to a file
    private void saveFile() {
        JFileChooser chooser = new JFileChooser();
        
        // if user choose a file to save to
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            
            try {
                PrintWriter out = new PrintWriter(new FileWriter(file));
                out.println(parsePts());
                out.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: "+ e);
            }  
        }
        
    }
    
    public String parsePts() {
        String line = "";
        for (int i=0; i < cnt; i++) {
            line = line + pt[i].getX() + " " + pt[i].getY() + "\n";
        }
        return line;
    }
    
    
    public void mousePressed(MouseEvent e) {
        // when mouse if pressed
        if (e.getButton() == MouseEvent.BUTTON1) { // left click
            GPoint point = new GPoint(e.getPoint());
            
            // turn off previous selected point
            if (draggedPt != null) {
                draggedPt.setFillColor(Color.black);
            }
            
            // check if any existing point is selected
            for (int i = 0; i < cnt; i++) {
                if (pt[i].contains(point)) {
                    lastPoint = point;
                    draggedPt = pt[i];
                    selectedLoc = point;
                    isDragging = true;
                }
            }
            
            if (isDragging) { // if an existing point is selected
                draggedPt.setFillColor(Color.red);
                draw();
            } else { // if no point is selected, add a point
                addPt(point);
            }
        } else { // right click
            GPoint point = new GPoint(e.getPoint());
            
            // turn off selected point
            if (draggedPt != null) {
                draggedPt.setFillColor(Color.black);
            }
            
            // check if any point if pressed on
            int indRemovePt = -1;
            for (int i = 0; i < cnt; i++) {
                if (pt[i].contains(point)) {
                    indRemovePt = i;
                }
            }
            
            // remove the point that is pressed on
            if (indRemovePt != -1) {
                remove(pt[indRemovePt]);
                for(int i=indRemovePt; i < cnt-1; i++) {
                    pt[i] = pt[i+1];
                }
                cnt--;
            }    
            selectedLoc = new GPoint(APPLICATION_WIDTH/2, APPLICATION_HEIGHT/2);
            draw();
        }
    }
    
    // dragging mouse
    public void mouseDragged(GPoint point) {
        if (isDragging) {
            draggedPt.move(point.getX()-lastPoint.getX(),
                           point.getY()-lastPoint.getY());
            lastPoint = point;
            selectedLoc = point;
            draw();            
        }
    }
    
    public void mouseReleased(GPoint point) {
        if (isDragging) {
            isDragging = false; 
            //removeAll();
            //redraw();            
        }
        
    }
    
    
    // add a point
    private void addPt(GPoint point) {
        pt[cnt] = new GOval(point.getX()-SIZE/2, point.getY()-SIZE/2, SIZE, SIZE);
        add(pt[cnt]);
        pt[cnt].setFilled(true);
        cnt++;
        selectedLoc = point;
        draw();  
    }
    
    
    
    // redraw all the lines
    private void draw() {
        //System.out.println(cnt);
        
        // first remove all exisitng lines
        for (int i=0; i < lineCnt; i++) {
            remove(line[i]);
        }
        
        
        lineCnt = 0; //reset line count
        
        // first remove all exisitng lines
        for (int i=0; i < MAX; i++) {
            if (label[i] != null) remove(label[i]);
        }
        
        
        boolean[][] ptDirect = new boolean[cnt][cnt]; // a matrix of booleans indicating directed connectivity after step 1
        boolean[][] ptDirect2 = new boolean[cnt][cnt]; // a matrix of booleans indicating directed connectivity after step 2
        boolean[][] ptConnect = new boolean[cnt][cnt]; // a matrix of booleans indicating undirected connectivity
        for (int i=0; i < cnt; i++) {
            for (int j=0; j < cnt; j++) {
                ptDirect[i][j] = false;
                ptDirect2[i][j] = false;
                ptConnect[i][j] = false;
            }
        }
        
        
        
        // enumerate through all pts
        for (int i=0; i < cnt; i++) {
            
            
            // for each cone
            for (int j=0; j < k; j++) {
                
                
                // neb records the selected pt in the cone
                int neb = -1;
                double min = DIAG;
                
                // find the closest pt
                for (int h=0; h < cnt; h++) {
                    if (i != h) {
                        
                        double ang = GMath.angle(pt[i].getX(), pt[i].getY(), pt[h].getX(), pt[h].getY());
                        double dist = GMath.distance(pt[i].getX(), pt[i].getY(), pt[h].getX(), pt[h].getY());
                        if (ang < 0) ang = ang + 360;
                        
                        if  (dist < min && ang >= j*360/k && ang < (j+1)*360/k) {
                            min=dist;
                            neb = h;
                        }
                    }
                }
                
                
                if (neb != -1) {
                    // indicate i chooses neb
                    ptDirect[i][neb] = true;
                }
            }
        }
        
        // find the current type of graph
        String currentType = type[typeChooser.getSelectedIndex()];
        Color lineColor;
        
        //System.out.println(currentType);
        
        if (currentType.equals("Yao-Yao")) {
            lineColor = Color.green;
            // inverse selection for Yao-Yao
            // enuemrate through all pts
            for (int i=0; i < cnt; i++) {
                // for each cone
                for (int j=0; j < k; j++) {
                    int neb = -1;
                    double min = DIAG;
                    
                    // enumerate through all pts with an edge incoming to i
                    for (int h=0; h < cnt; h++) {
                        
                        if (ptDirect[h][i]) { // if pt h connects to pt i
                            
                            double ang = GMath.angle(pt[i].getX(), pt[i].getY(), pt[h].getX(), pt[h].getY());
                            double dist = GMath.distance(pt[i].getX(), pt[i].getY(), pt[h].getX(), pt[h].getY());
                            if (ang < 0) ang = ang + 360;
                            
                            
                            if  (dist < min && ang >= j*360/k && ang < (j+1)*360/k) {
                                min = dist;
                                neb = h;
                            }
                        }
                    }
                    
                    //after the shortest incoming edge is found, the other end has index neb
                    if (neb != -1) {
                        ptDirect2[neb][i] = true; // record the directed edge, only record selected edges
                        ptConnect[i][neb] = true; // record the undirected edge
                        ptConnect[neb][i] = true;
                        
                        
                        //drawLine(i, neb, ptDirect[i][neb], ptDirect[neb][i], Color.green);
                        
                    }   
                    
                }
            } 
            
        } else if (currentType.equals("Sym-Yao"))  {
            
            lineColor = Color.blue;
            
            for (int i=0; i < cnt; i++) {
                
                for (int j=i+1; j < cnt; j++) {
                    // if selected by both ends
                    if (ptDirect[i][j] && ptDirect[j][i]) {
                        ptDirect2[i][j] = true; // record the directed edge, only record bidirectional edges
                        ptDirect2[j][i] = true;
                        ptConnect[i][j] = true; // record the undirected edge
                        ptConnect[j][i] = true;
                        //drawLine(i, j, ptDirect[i][j], ptDirect[j][i], Color.blue);
                        
                    }   
                    
                }
            }
            
            
        } else {
            
            lineColor = Color.orange;
            
            for (int i=0; i < cnt; i++) {
                
                for (int j=i+1; j < cnt; j++) {
                    // if selected by either ends
                    if (ptDirect[i][j] || ptDirect[j][i]) {
                        ptDirect2[i][j] = ptDirect[i][j]; // record the directed edge, all edges are kept
                        ptDirect2[j][i] = ptDirect[j][i]; 
                        ptConnect[i][j] = true; // record the undirected edge
                        ptConnect[j][i] = true;
                        //drawLine(i, j, ptDirect[i][j], ptDirect[j][i], Color.orange);
                        
                    }   
                    
                }
            }
            
        }
        
        int[] pre = new int[cnt]; // predecessor in the shortest path
        for (int i=0; i < cnt; i++) {
            pre[i] = -1;
        }
        
        if (isDrawPath) {
            int start = startField.getValue();
            int end = endField.getValue();
            if (start >= 0 && start < cnt && end >= 0 && end < cnt) {
                shortestPath(ptConnect, ptDirect, pre, cnt, start, end);
            }
        }
        
        for (int i=0; i < cnt; i++) {
            for (int j=i+1; j < cnt; j++) {
                if (ptConnect[i][j]) {
                    drawLine(i, j, ptDirect2[i][j], ptDirect2[j][i], pre, lineColor);
                }
            }
        }
        
        // draw cones and highline those of the selected point
        if (isDrawCone) drawCone();
        
        // draw labels of point
        if (isDrawLabel) drawLabel();
        
    }
    
    // an inefficient implementation of the dijkstra's alg for shortest path
    private void shortestPath(boolean[][] ptConnect, boolean[][] ptDirect, int[] pre2, int cnt, int start, int end) {
        
        
        boolean[] include = new boolean[cnt]; // include points whose shortest paths are find
        double[] path = new double[cnt]; // shortest path distance from start to very point
        int[] pre = new int[cnt]; // predecessor of ALL vertices in the shortest path
        
        for (int i=0; i < cnt; i++) {
            if (i == start) {
                include[i] = true; // initially only start is included
                path[i] = 0;
            } else {
                include[i] = false;
                path[i] = cnt*DIAG;
            }
            pre[i] = -1;
        }
        
                // search until end is included
        while (!include[end]) {
            double min = cnt * DIAG; // find the closest point to start that is connected to the included pts
            int next = -1; // the one to be included next
            int current = -1; // the point in the included set that is the prec of the one to be included next
            
            for (int i=0; i < cnt; i++) { 
                
                for (int j=0; j < cnt; j++) {
                    //System.out.println(i + ", " + j);
                    
                    if (include[i] && !include[j] && ptConnect[i][j]) { // consider all connected pairs with only one included
                        // compute the distance
                        double dist = path[i] + GMath.distance(pt[i].getX(), pt[i].getY(), pt[j].getX(), pt[j].getY());
                        if (dist < min) { // update the min and next
                            min = dist;
                            current = i;
                            next = j;
                        }
                    }
                }
            }
            
            //System.out.print(next);
            //for (int i=0; i < cnt; i++) { 
            //   System.out.print(" | " + include[i]);
            //}
            //System.out.println();
            
            include[next] = true;
            pre[next] = current;
            path[next] = min;
            
        }
        double euclidean = GMath.distance(pt[start].getX(), pt[start].getY(), pt[end].getX(), pt[end].getY());
        rLabel.setText(String.format("%.8f", path[end]/euclidean));
        
        
        // backtrack, only keep those end in the path in pre2
        int last = end;
        int backtrack = pre[last];
        while (backtrack >= 0) {
            pre2[last] = backtrack;
            last = backtrack;
            backtrack = pre[last];
        }
        
               
    }
    
    private void drawLine(int i, int j, boolean ij, boolean ji, int[] pre, Color color) {
        double x1, x2, x3, y1, y2, y3;
        x1 = pt[i].getX()+SIZE/2;
        x2 = pt[j].getX()+SIZE/2;
        y1 = pt[i].getY()+SIZE/2;
        y2 = pt[j].getY()+SIZE/2;
        
        x3 = (x1+x2)/2;
        y3 = (y1+y2)/2;
        
        GLine aline = new GLine(x1, y1, x3, y3);
        if (ji || !isDrawDirection) {
            if (pre[i] == j || pre[j] == i) { // if part of the shortest path, color red
                aline.setColor(Color.red);
            } else {
                aline.setColor(color);
            }
        }
        add(aline);
        aline.sendToBack();
        line[lineCnt] = aline;
        lineCnt++;
        
        aline = new GLine(x3, y3, x2, y2);
        if (ij || !isDrawDirection) {
            if (pre[i] == j || pre[j] == i) { // if part of the shortest path, color red
                aline.setColor(Color.red);
            } else {
                aline.setColor(color);
            }
        }
        add(aline);
        aline.sendToBack();
        line[lineCnt] = aline;
        lineCnt++;
        
        
    }
    
    
    // draw the cones
    private void drawCone() {
        
        for (int i=0; i < cnt; i++) {
            
            for (int j=0; j < k; j++) {
                
                GLine cline = new GLine(pt[i].getX()+SIZE/2, pt[i].getY()+SIZE/2, 
                                        pt[i].getX()+SIZE/2+DIAG*GMath.cosDegrees(j*360/k), pt[i].getY()+SIZE/2+DIAG*GMath.sinDegrees(j*360/k));
                add(cline);
                if (pt[i].getFillColor().equals(Color.RED)) {
                    cline.setColor(Color.cyan);
                } else {
                    cline.setColor(new Color(240,240,240));
                }
                cline.sendToBack();
                line[lineCnt] = cline;
                lineCnt++;
                
            }
        }
    }
    
    // draw the cones
    private void drawLabel() {
        
        for (int i=0; i < cnt; i++) {
            label[i] = new GLabel(i+"", pt[i].getX(), pt[i].getY());
            add(label[i]);

        }
    }
}
