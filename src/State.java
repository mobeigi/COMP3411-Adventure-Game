import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.geom.Point2D;

public class State {
  //Definitions
  
  //Direction (facing)
  final static int UP = 0;
  final static int DOWN = 1;
  final static int LEFT = 2;
  final static int RIGHT = 3;
  
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
        map.put(new Point2D.Double(x,y), OBSTACLE_UNKNOWN);
      }
    }
    
    //Initially, we always consider ourselves to be facing up
    direction = UP;
    map.put(new Point2D.Double(0,0), DIRECTION_UP);
  }
  
  //Update map based on changes in view
  public void updateFromView(char view[][]) {
    //todo remove
    System.out.println("curX: " + curX + ", curY: " + curY);


    //Determine how many times to rotate our view so it is facing UP based on our initial map
    //This is required so we can use the same map update code regardless of the views direction
    int numTimesToRotate = 0;

    switch(direction) {
      case UP:
        //Already facing correct direction
        break;
      case DOWN:
        numTimesToRotate = 2;
        break;
      case LEFT:
        numTimesToRotate = 3;
        break;
      case RIGHT:
        numTimesToRotate = 1;
        break;
    }

    for (int i = 0; i < numTimesToRotate; ++i) {
      view = rotateCW(view);
    }

    //We will treat the [0-4] indexes given by the view as offsets
    //Thus, and x of 0 becomes -2, x of 1 becomes -1 and so on
    //The player is always at (2,2) in the view, the center tile of the view
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 5; ++j) {
        char curTile = view[i][j];
        int xFinal = curX + (j - 2);
        int yFinal = curY + (2 - i);

        //If this is the players tile, show the correct directional character
        if (i == 2 && j == 2) {
          switch(direction) {
            case UP:
              curTile = DIRECTION_UP;
              break;
            case DOWN:
              curTile = DIRECTION_DOWN;
              break;
            case LEFT:
              curTile = DIRECTION_LEFT;
              break;
            case RIGHT:
              curTile = DIRECTION_RIGHT;
              break;
          }
        }

        //Update tile in map
        map.put(new Point2D.Double(xFinal, yFinal), curTile);
        //System.out.println("Placed at ("+(xFinal)+","+ (yFinal)+"): " + curTile);
      }
    }
    
    //todo: remove this
    this.printMap();
  }

  public char makeMove()
  {
      int ch=0;

      System.out.print("Enter Action(s): ");

      try {
          while ( ch != -1 ) {
              // read character from keyboard
              ch  = System.in.read();

              switch( ch ) { // if character is a valid action, return it
                  case 'F': case 'L': case 'R': case 'C': case 'U':
                  case 'f': case 'l': case 'r': case 'c': case 'u':
                    updateFromMove((char)ch);
                    return((char) ch );
              }
          }
      }
      catch (IOException e) {
          System.out.println ("IO error:" + e );
      }

      return 0;
  }

  //Update map based on changes from a move
  //todo: optimize switch statements for direction
  private void updateFromMove(char move){
    switch(move) {
      case 'L':
      case 'l':
        //Moved left
        switch(direction) {
          case UP:
            direction = LEFT;
            break;
          case DOWN:
            direction = RIGHT;
            break;
          case LEFT:
            direction = DOWN;
            break;
          case RIGHT:
            direction = UP;
            break;
        }
        break;
      case 'R':
      case 'r':
        //Moved right
        switch(direction) {
          case UP:
            direction = RIGHT;
            break;
          case DOWN:
            direction = LEFT;
            break;
          case LEFT:
            direction = UP;
            break;
          case RIGHT:
            direction = DOWN;
            break;
        }
        break;
      case 'F':
      case 'f':
        //We moved forward, update our curX, curY
        switch(direction) {
          case UP:
            curY += 1;
            break;
          case DOWN:
            curY -= 1;
            break;
          case LEFT:
            curX -= 1;
            break;
          case RIGHT:
            curX += 1;
            break;
        }
        break;
      case 'C':
      case 'c':
        break;
      case 'U':
      case 'u':
        break;
    }
  }


  //Print map (rotating map)
  public void printMap() {
    System.out.print("\nRotating Map\n");

    //Traverse map showing 12by12 grid from top left to bottom right
    for (int y = 12; y >= -12; --y) {
      for (int x = -12; x <= 12; ++x) {
        char curTile = map.get(new Point2D.Double(x, y));
        System.out.print(curTile);
      }

      System.out.print('\n');
    }
  }

  //Helper function to rotate matrix clockwise
  //From: https://stackoverflow.com/a/2800033/1800854
  private static char[][] rotateCW(char[][] mat) {
    final int M = mat.length;
    final int N = mat[0].length;
    char[][] ret = new char[N][M];
    for (int r = 0; r < M; r++) {
      for (int c = 0; c < N; c++) {
        ret[c][M-1-r] = mat[r][c];
      }
    }
    return ret;
  }
}