import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to represent a node
class Node {
  private final int x;
  private final int y;
  private Color color;
  // arraylist of edges, again in clockwise order, starting from this
  private final ArrayList<Edge> edges;

  // helper constructor for node 
  Node(int y, int x) {
    this.x = x;
    this.y = y;
    this.color = Color.LIGHT_GRAY;
    this.edges = new ArrayList<Edge>();
  }

  Node(int y, int x, Color color, ArrayList<Edge> edges) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.edges = edges;
  }

  // EFFECT: creates and adds an edge to edges, from this to given node
  void makeEdge(Node to, int weight) {
    Edge made = new Edge(this, to, weight);
    this.edges.add(made);
    to.edges.add(made);
  }

  void randomizeEdgeWeights(Boolean horizPref, Boolean vertPref) {
    for (Edge e : this.edges) {
      e.randomizeWeight(horizPref, vertPref);
    }
  }

  //EFFECT: sets this pixel's colour to given colour
  void changeColor(Color c) {
    this.color = c;
  }

  //EFFECT: adds an edge to edges
  void addEdge(Edge edge) {
    this.edges.add(edge);
    edge.otherEnd(this).edges.add(edge);
  }

  // returns true if this node has x/y pos equivalent to other node + given dir
  boolean isAdjacent(Node adjacent, boolean horiz, int difference) {
    if (horiz) {
      return this.y == adjacent.y + difference;
    }
    else {
      return this.x == adjacent.x + difference;
    }
  }

  //EFFECT: adds this node's edges to the given ArrayList of edges
  void addEdges(ArrayList<Edge> edges) {
    for (Edge e : this.edges) {
      if (!edges.contains(e)) {
        edges.add(e);
      }
    }
  }

  //EFFECT: adds all nodes connected to this to given list of nodes, either front or back of list
  void addConnections(ArrayList<Node> nodes, ArrayList<Node> seen, ArrayList<Edge> pathsForward,
      HashMap<Node, Node> pathBehind, boolean depthFirst) {
    for (Edge e : edges) {
      if (pathsForward.contains(e)) {
        Node next = e.otherEnd(this);
        if (!seen.contains(next)) {
          if (depthFirst) {
            nodes.add(0, next);
          }
          else {
            nodes.add(next);
          }
          pathBehind.put(next, this);
        }
      }
    }
  }


  WorldImage draw(ArrayList<Edge> connections) {
    WorldImage nodeWorldImage = new RectangleImage(15, 15, OutlineMode.SOLID, this.color);
    for (Edge e : this.edges) {
      if (!connections.contains(e)) {
        nodeWorldImage = e.addWalls(this, nodeWorldImage);
      }
    }
    return nodeWorldImage;
  }

  boolean isValidMove(Node node, ArrayList<Edge> edges) {
    for (Node n : this.validAdjacent(edges)) {
      if (n == node) {
        return true;
      }
    }
    return false;
  }

  ArrayList<Node> validAdjacent(ArrayList<Edge> edges) {
    ArrayList<Node> valid = new ArrayList<Node>();
    for (Edge e : this.edges) {
      if (edges.contains(e)) {
        valid.add(e.otherEnd(this));
      }
    }
    return valid;
  }
}

// to represent an edge (node to node)
class Edge {
  // not directional, but from and to make it easy to remember/distinguish the points
  private final Node from;
  private final Node to;
  private int weight;

  // constructor
  Edge(Node from, Node to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  //  boolean equals(Edge edge) {
  //    boolean thisToEqTo = this.to == edge.to;
  //    boolean thisFromEqFrom = this.from == edge.from;
  //    boolean thisToEqFrom = this.to == edge.from;
  //    boolean thisFromEqTo = this.from == edge.to;
  //    return ((thisToEqTo && thisFromEqFrom) || (thisToEqFrom && thisFromEqTo));
  //  }

  // returns the other vertice of given node in this
  Node otherEnd(Node node) {
    if (node == this.from) {
      return to;
    }
    return from;
  }

  // returns if from has same representative as to in the nodeUnionFind hashmap
  boolean repsSame(NodeUnionFind nuf) {
    return nuf.find(this.from) == nuf.find(this.to);
  }

  // EFFECT: unions the representatives of this' to and from
  void unionToFrom(NodeUnionFind nuf) {
    nuf.union(this.to, this.from);
  }

  // adds the walls of the maze to given WorldImage of a single node
  // EFFECT: adds the walls to the given WorldImage, changing it
  WorldImage addWalls(Node node, WorldImage nodeWorldImage) {
    Node otherEnd = this.otherEnd(node);
    if (otherEnd.isAdjacent(node, true, -1)) {
      nodeWorldImage = new AboveImage(new LineImage(new Posn(15, 0), Color.black), nodeWorldImage);
    }
    else if (otherEnd.isAdjacent(node, true, 1)) {
      nodeWorldImage = new AboveImage(nodeWorldImage, new LineImage(new Posn(15, 0), Color.black));
    }
    else if (otherEnd.isAdjacent(node, false, 1)) {
      nodeWorldImage = new BesideImage(nodeWorldImage, new LineImage(new Posn(0, 15), Color.black));
    }
    else {
      nodeWorldImage = new BesideImage(new LineImage(new Posn(0, 15), Color.black), nodeWorldImage);
    }
    return nodeWorldImage;
  }

  int compareWeight(Edge other) {
    return this.weight - other.weight;
  }

  //EFFECT: changes this.weight to a random number based on horiz or vert preference
  void randomizeWeight(Boolean horizPref, Boolean vertPref) {
    Random rand = new Random();
    if (horizPref) { 
      if (to.isAdjacent(from, true, -1) || to.isAdjacent(from, true, 1)) {
        this.weight = rand.nextInt(100);
      }
      else {
        this.weight = rand.nextInt(15);
      }
    }
    else if (vertPref) { 
      if (to.isAdjacent(from, false, -1) || to.isAdjacent(from, false, 1)) {
        this.weight = rand.nextInt(100);
      }
      else {
        this.weight = rand.nextInt(15);
      }
    }
    else {
      this.weight = rand.nextInt(20);
    }
  }
}

class Maze extends World {
  private ArrayList<ArrayList<Node>> nodes;
  private ArrayList<Edge> connections;
  private final int width;
  private final int height;
  private boolean depthFirstSearch;
  private final Node start;
  private final Node end;
  private ArrayList<Node> path;
  private ArrayList<Node> nodesSeen;
  private boolean toggleAnimate;
  private boolean player;
  private boolean playerToggleAnimate;
  private int playerY;
  private int playerX;
  private final ArrayList<Node> playerNodesSeen;
  private String overlay;
  private int findTick;
  private int drawTick;
  private int playerFindTick;
  private boolean vertPref;
  private boolean horizPref;

  Maze(int width, int height) {
    this.nodes = new ArrayList<ArrayList<Node>>();
    this.width = width;
    this.height = height;
    //add "normal" nodes (to be edited)
    for (int h = 0; h < height; h += 1) {
      this.nodes.add(new ArrayList<Node>());
      for (int w = 0; w < width; w += 1) {
        this.nodes.get(h).add(new Node(h, w));
      }
    }
    Random rand = new Random();
    //add all edges right + down
    if (vertPref) {
      for (int h = 0; h < height; h += 1) {
        for (int w = 0; w < width; w += 1) {
          if (h < height - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h + 1).get(w), rand.nextInt(10));
          }
          if (w < width - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h).get(w + 1), rand.nextInt(100));
          }
        }
      }
    }
    else if (horizPref) {
      for (int h = 0; h < height; h += 1) {
        for (int w = 0; w < width; w += 1) {
          if (h < height - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h + 1).get(w), rand.nextInt(100));
          }
          if (w < width - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h).get(w + 1), rand.nextInt(10));
          }
        }
      }
    }
    else {
      for (int h = 0; h < height; h += 1) {
        for (int w = 0; w < width; w += 1) {
          if (h < height - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h + 1).get(w), rand.nextInt(100));
          }
          if (w < width - 1) {
            this.nodes.get(h).get(w).makeEdge(this.nodes.get(h).get(w + 1), rand.nextInt(100));
          }
        }
      }
    }
    this.connections = this.cheapestSpanningPath();

    this.start = this.nodes.get(0).get(0);
    this.start.changeColor(Color.green);
    this.end = this.nodes.get(height - 1).get(width - 1);
    this.end.changeColor(Color.red);
    this.depthFirstSearch = false;
    this.overlay = "Breadth-first";
    this.path = new ArrayList<Node>();
    this.nodesSeen = new ArrayList<Node>();
    this.playerNodesSeen = new ArrayList<Node>();
    this.toggleAnimate = true;
    this.playerToggleAnimate = false;
    this.player = false;
    this.findTick = 0;
    this.drawTick = 0;
    this.playerFindTick = 0;

    this.pathFromTo(this.start, this.end, this.depthFirstSearch);
  }

  ArrayList<Edge> cheapestSpanningPath() {
    NodeUnionFind nuf = new NodeUnionFind();
    nuf.setRepresentatives(this.nodes);
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> workList = new ArrayList<Edge>();
    for (int w = 0; w < this.height; w += 1) {
      for (int h = 0; h < this.width; h += 1) {
        Node node = this.nodes.get(w).get(h);
        node.addEdges(workList);
      }
    }
    workList.sort(new WeightSort());
    while (nuf.size() > 1) {
      Edge next = workList.remove(0);
      if (next.repsSame(nuf)) {
        // already discarded by using .remove(0);
      }
      else {
        edgesInTree.add(next);
        next.unionToFrom(nuf);
      }
    }
    return edgesInTree;
  }

  //EFFECT: sets this' path to the path from given beginning and end
  void pathFromTo(Node start, Node end, boolean depthFirstSearch) {
    ArrayList<Node> seen = new ArrayList<Node>();
    ArrayList<Node> workList = new ArrayList<Node>();
    HashMap<Node, Node> path = new HashMap<Node, Node>();
    workList.add(start);
    path.put(start, start);
    while (!workList.isEmpty()) {
      Node next = workList.remove(0);
      if (next == end) {
        ArrayList<Node> done = new ArrayList<Node>();
        this.path = done;
        this.nodesSeen = seen;
        this.drawTick = 0;
        this.findTick = seen.size() - 1;
        return;
      }
      else if (seen.contains(next)) {
        // already removed, nothing else to do
      }
      else {
        next.addConnections(workList, seen, connections, path, depthFirstSearch);
        seen.add(0, next);
      }
    }
  }

  WorldImage draw() {
    WorldImage cols = new EmptyImage();
    for (ArrayList<Node> nodes : this.nodes) {
      WorldImage row = new EmptyImage();
      for (Node n : nodes) {
        row = new BesideImage(row, n.draw(this.connections));
      }
      cols = new AboveImage(cols, row);
    }
    return cols;
  }

  // must be public, inherited from super class
  public WorldScene makeScene() {
    WorldScene maze = new WorldScene(this.width * 18, this.height * 18);
    maze.placeImageXY(this.draw(), this.width * 18 / 2, this.height * 18 / 2);
    maze.placeImageXY(new TextImage(
        this.overlay, Color.black), this.width * 8 / 2, this.height * 18 / 20);
    if (vertPref) {
      maze.placeImageXY(new TextImage(
          "Vertical Preference", Color.black), this.width * 12, this.height * 18 / 20);
    }
    else if (horizPref) {
      maze.placeImageXY(new TextImage(
          "Horizontal Preference", Color.black), this.width * 12, this.height * 18 / 20);
    }
    else {
      maze.placeImageXY(new TextImage(
          "Random preference", Color.black), this.width * 12, this.height * 18 / 20);
    }
    return maze;
  }

  //must be public, inherited from super class
  public void onTick() {
    if (this.toggleAnimate) {
      if (this.findTick >= 0) {
        this.nodesSeen.get(this.findTick).changeColor(Color.CYAN);
        this.findTick -= 1;
      }
      else if (this.drawTick <= this.path.size() - 1) {
        this.path.get(this.drawTick).changeColor(Color.green);
        this.drawTick += 1;
      }
      else {
        this.toggleAnimate = false;
        this.findTick = this.nodesSeen.size() - 1;
        this.drawTick = 0;
      }
    }

    if (this.playerToggleAnimate) {
      if (this.playerFindTick <= this.playerNodesSeen.size() - 1) {
        this.playerNodesSeen.get(this.playerFindTick).changeColor(Color.cyan);
        this.playerFindTick += 1;
      }
      else if (this.drawTick <= this.path.size() - 1) {
        this.path.get(this.drawTick).changeColor(Color.green);
        this.drawTick += 1;
      }
      else {
        this.playerToggleAnimate = false;
        this.playerFindTick = 0;
        this.drawTick = 0;
      }
    }
  }

  void clearColour() {
    for (int h = 0; h < this.nodes.size(); h += 1) {
      for (int w = 0; w < this.nodes.get(h).size(); w += 1) {
        this.nodes.get(h).get(w).changeColor(Color.LIGHT_GRAY);
      }
    }
    this.nodes.get(0).get(0).changeColor(Color.green);
    this.nodes.get(this.height - 1).get(this.width - 1).changeColor(Color.red);
  }

  // must be public, inherited from super class
  public void onKeyEvent(String key) {
    if (key.equals("d")) {
      this.clearColour();
      this.overlay = "Depth-first";
      this.toggleAnimate = true;
      this.depthFirstSearch = true;
      this.pathFromTo(start, end, depthFirstSearch);
    }
    if (key.equals("b")) {
      this.clearColour();
      this.overlay = "Breadth-first";
      this.toggleAnimate = true;
      this.depthFirstSearch = false;
      this.pathFromTo(start, end, depthFirstSearch);
    }
    if (key.equals("r")) {
      for (ArrayList<Node> aln : nodes) {
        for (Node n : aln) {
          n.randomizeEdgeWeights(this.horizPref, this.vertPref);
        }
      }
      this.connections = this.cheapestSpanningPath();
      if (this.player || this.playerToggleAnimate) {
        this.playerToggleAnimate = false;
        this.onKeyEvent("p");
      }
      else if (this.depthFirstSearch) {
        this.onKeyEvent("d");
      }
      else {
        this.onKeyEvent("b");
      }
    }
    if (key.equals("p")) {
      this.clearColour();
      this.playerX = 0;
      this.playerY = 0;
      this.overlay = "Player";
      this.toggleAnimate = false;
      this.player = true;
    }
    if (this.player && !this.toggleAnimate) {
      Node pos = this.nodes.get(this.playerY).get(this.playerX);
      if (key.equals("up")) {
        if (this.playerY > 0 && pos.isValidMove(
            this.nodes.get(this.playerY - 1).get(this.playerX), this.connections)) {
          this.playerY -= 1;
          pos.changeColor(Color.LIGHT_GRAY);
        }
      }
      if (key.equals("down")) {
        if (this.playerY < this.height - 1 && pos.isValidMove(
            this.nodes.get(this.playerY + 1).get(this.playerX), this.connections)) {
          this.playerY += 1;
          pos.changeColor(Color.LIGHT_GRAY);
        }
      }
      if (key.equals("left")) {
        if (this.playerX > 0 && pos.isValidMove(
            this.nodes.get(this.playerY).get(this.playerX - 1), this.connections)) {
          this.playerX -= 1;
          pos.changeColor(Color.LIGHT_GRAY);
        }
      }
      if (key.equals("right")) {
        if (this.playerX < this.width - 1 && pos.isValidMove(
            this.nodes.get(this.playerY).get(this.playerX + 1), this.connections)) {
          this.playerX += 1;
          pos.changeColor(Color.LIGHT_GRAY);
        }
      }
      this.nodes.get(this.playerY).get(this.playerX).changeColor(Color.green);
      this.playerNodesSeen.add(pos);
      if (this.playerY == this.height - 1 && this.playerX == this.width - 1) {
        this.overlay = "You Won!";
        this.player = false;
        this.playerToggleAnimate = true;
      }
    }
    if (key.equals("s")) {
      this.clearColour();
      this.vertPref = false;
      this.horizPref = false;
      this.toggleAnimate = true;
      this.pathFromTo(start, end, depthFirstSearch);
      this.onKeyEvent("r");
    }
    if (key.equals("v")) {
      this.clearColour();
      this.vertPref = true;
      this.horizPref = false;
      this.toggleAnimate = true;
      this.pathFromTo(start, end, depthFirstSearch);
      this.onKeyEvent("r");
    }
    if (key.equals("h")) {
      this.clearColour();
      this.vertPref = false;
      this.horizPref = true;
      this.toggleAnimate = true;
      this.pathFromTo(start, end, depthFirstSearch);
      this.onKeyEvent("r");
    }
  }

  // specializes the big bang function to look better, no access of fields :D
  void bigBang() {
    this.bigBang(this.width * 18, this.height * 18, 0.01);
  }
}

class WeightSort implements Comparator<Edge> {
  public int compare(Edge firstEdge, Edge secondEdge) {
    return firstEdge.compareWeight(secondEdge);
  }
}

class NodeUnionFind {
  private final HashMap<Node, Node> representatives;

  NodeUnionFind() {
    this.representatives = new HashMap<Node, Node>();
  }

  //for testing
  NodeUnionFind(HashMap<Node, Node> representatives) {
    this.representatives = representatives;
  }

  //EFFECT: sets the representatives table, all nodes referring to themselves
  void setRepresentatives(ArrayList<ArrayList<Node>> nodes) {
    for (int w = 0; w < nodes.size(); w += 1) {
      for (int h = 0; h < nodes.get(w).size(); h += 1) {
        Node put = nodes.get(w).get(h);
        this.representatives.put(put, put);
      }
    }
  }

  //EFFECT: unions two node's by setting the first's representative to the second's
  void union(Node first, Node second) {
    this.representatives.put(this.find(first), this.find(second));
  }

  // finds representative of given node by looping through until finds itself
  Node find(Node node) {
    if (node == this.representatives.get(node)) {
      return node;
    }
    else {
      return this.find(this.representatives.get(node));
    }
  }

  int size() {
    ArrayList<Node> differentRepresentatives = new ArrayList<Node>();
    for (Node n : this.representatives.keySet()) {
      if (!differentRepresentatives.contains(this.find(n))) {
        differentRepresentatives.add(this.find(n));
      }
    }
    return differentRepresentatives.size();
  }
}

class ExamplesMaze {
  Node mid;
  Node top;
  Node right;
  Node bot;
  Node left;
  Edge midright;
  ArrayList<Edge> edgesEx;
  Maze maze;
  ArrayList<Node> col1;
  ArrayList<Node> col2;
  ArrayList<ArrayList<Node>> cols;

  void initializeVars() {
    mid = new Node(1, 1);
    top = new Node(0, 1);
    right = new Node(1, 2);
    bot = new Node(2, 1);
    left = new Node(1, 0);
    midright = new Edge(mid, right, 5);
    edgesEx = new ArrayList<Edge>();
    maze = new Maze(30, 30);
    col1 = new ArrayList<Node>();
    col2 = new ArrayList<Node>();
    cols = new ArrayList<ArrayList<Node>>();
    col1.add(left);
    col1.add(right);
    col2.add(top);
    col2.add(bot);
    cols.add(col1);
    cols.add(col2);
  }

  void testEdges(Tester t) {
    initializeVars();
    mid.makeEdge(top, 5);
    mid.addEdge(midright);
    mid.addEdges(edgesEx);
    ArrayList<Edge> expected = new ArrayList<Edge>();
    expected.add(new Edge(mid, top, 5));
    expected.add(midright);
    t.checkExpect(edgesEx, expected);
  }

  void testAdjacents(Tester t) {
    initializeVars();
    t.checkExpect(mid.validAdjacent(edgesEx), new ArrayList<Edge>());
    t.checkExpect(mid.isAdjacent(top, false, 1), false);
  }

  void testAddConnections(Tester t) {
    initializeVars();
    mid.makeEdge(right, 5);
    mid.addConnections(new ArrayList<Node>(), 
        new ArrayList<Node>(), edgesEx, new HashMap<Node, Node>(), false);
    t.checkExpect(edgesEx, new ArrayList<Node>());
  }


  void testBigBang(Tester t) {
    initializeVars();
    this.maze.bigBang();
  }

  void testOtherEnd(Tester t) {
    initializeVars();
    Edge edge = new Edge(left, right, 5);
    t.checkExpect(edge.otherEnd(left), right);
  }

  void testUnionFind(Tester t) {
    initializeVars();
    NodeUnionFind nuf = new NodeUnionFind();
    nuf.setRepresentatives(cols);
    t.checkExpect(nuf.find(left), left);
    nuf.union(left, right);
    t.checkExpect(nuf.find(right), right);
    // after unioning, size goes down to 3
    t.checkExpect(nuf.size(), 3);
  }
}