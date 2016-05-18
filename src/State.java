import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.geom.Point2D;

public class State {
  //Definitions
  
  //Direction (facing)
  final static int RIGHT = 0;
  final static int UP = 1;
  final static int LEFT = 2;
  final static int DOWN = 3;
  
  //Direction characters
  final static char DIRECTION_UP = '^';
  final static char DIRECTION_DOWN = 'v';
  final static char DIRECTION_LEFT = '<';
  final static char DIRECTION_RIGHT = '>';
  
  //Obstacles
  final static char OBSTACLE_TREE = 'T';
  final static char OBSTACLE_DOOR = '-';
  final static char OBSTACLE_WATER = '~';
  final static char OBSTACLE_WALL = '*';
  final static char OBSTACLE_UNKNOWN = '?';
  
  //Tools
  final static char TOOL_AXE = 'a';
  final static char TOOL_KEY = 'k';
  final static char TOOL_STEPPING_STONE = 'o';
  final static char TOOL_STEPPING_STONE_PLACED = 'O';
  final static char TOOL_GOLD = 'g';
  
  //Class variables
  private Map<Point2D.Double, Character> map;
  
  private boolean haveAxe;
  private boolean haveKey;
  private boolean haveGold;
  
  private int num_stones_held;
  
  private int curX;
  private int curY;
  private int direction;  //direction we are currently facing
 
  private Stack<Character> pastMoves;
  private Queue<Character> pendingMoves;
  
  public State() {
    //Init variables
    haveAxe = false;
    haveKey = false;
    haveGold = false;
    num_stones_held = 0;
    
    pastMoves = new Stack<Character>();
    pendingMoves = new LinkedList<Character>();
    
    //(0,0) is the origin
    curX = curY = 0;
    
    //Prefill map with unknowns for reasonable bounds
    map = new HashMap<Point2D.Double, Character>();
     
    //You may assume that the specified environment is no larger than 80 by 80
    //However, as our starting origin is (0,0), our total boundary should be at least 80*2 by 80*2 or 160x160
    for (int x = -80; x < 81; ++x) {
      for (int y = -80; y < 81; ++y) {
        map.put(new Point2D.Double(x,y), new Character(OBSTACLE_UNKNOWN));
      }
    }
    
    //Initially, we always consider ourselves to be facing up
    direction = UP;
    map.put(new Point2D.Double(0,0), new Character(DIRECTION_UP));
  }
  
  //Update map based on changes in view
  public void updateFromView(char view[][]) {
    //Print view
    /*
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 5; ++j) {
        System.out.println(view[i][j]);
      }
    }
    */
    
    //Debug
    this.printMap();
  }
  
  //Print map
  public void printMap() {
    System.out.print('\n');
    
    for (int x = -6; x < 6; ++x) {
      for (int y = -6; y < 6; ++y) {
        System.out.print(map.get(new Point2D.Double(x,y)));
      }
        System.out.print('\n');
    }
  }
}