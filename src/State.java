import java.util.*;
import java.awt.geom.Point2D;

/**
 * State class.
 *
 * Maintains internal map (model) of environment as well as the locations of resources (tools) and the gold.
 *
 * @author Mohammad Ghasembeigi
 * @version 1.8
 */
public class State {
  //Class definitions
  
  //Direction (facing)
  final static int UP = 0;
  final static int RIGHT = 1;
  final static int DOWN = 2;
  final static int LEFT = 3;

  //Direction characters
  final static char DIRECTION_UP = '^';
  final static char DIRECTION_DOWN = 'v';
  final static char DIRECTION_LEFT = '<';
  final static char DIRECTION_RIGHT = '>';
  
  //Obstacles
  final static char OBSTACLE_SPACE = ' ';
  final static char OBSTACLE_TREE = 'T';
  final static char OBSTACLE_DOOR = '-';
  final static char OBSTACLE_WATER = '~';
  final static char OBSTACLE_WALL = '*';
  final static char OBSTACLE_UNKNOWN = '?';
  final static char OBSTACLE_BOUNDARY = '.';
  final static char OBSTACLE_TEMPORARY_WATER = '#'; //water that will soon be turned into OBSTACLE_STEPPING_STONE_PLACED
  final static char OBSTACLE_STEPPING_STONE_PLACED = 'O';
  
  //Tools
  final static char TOOL_AXE = 'a';
  final static char TOOL_KEY = 'k';
  final static char TOOL_STEPPING_STONE = 'o';
  final static char TOOL_GOLD = 'g';

  //Moves
  final static char MOVE_TURNLEFT = 'L';
  final static char MOVE_TURNRIGHT = 'R';
  final static char MOVE_GOFORWARD= 'F';
  final static char MOVE_CHOPTREE = 'C';
  final static char MOVE_UNLOCKDOOR = 'U';

  //Map dimensions
  //You may assume that the specified environment is no larger than 80 by 80
  final static int MAX_GRID_X = 80;
  final static int MAX_GRID_Y = 80;

  //Class variables
  private Map<Point2D.Double, Character> map;

  //Tool inventory
  private boolean haveAxe;
  private boolean haveKey;
  private boolean haveGold;
  private int num_stones_held;


  private int curX;
  private int curY;
  private int direction;  //direction we are currently facing

  private int totalNumMoves; //includes NOP moves and C/U moves

  private boolean isGoldVisible;
  private Point2D.Double goldLocation; //coordinate of gold once it has been found
  private LinkedList<Point2D.Double> axeLocations;
  private LinkedList<Point2D.Double> keyLocations;
  private LinkedList<Point2D.Double> ssLocations; //ss short for stepping stones
  private LinkedList<Point2D.Double> waterLocations;

  /**
   * Constructor.
   */
  public State() {
    //Init variables
    this.haveAxe = false;
    this.haveKey = false;
    this.haveGold = false;
    this.num_stones_held = 0;

    this.totalNumMoves = 0;
    
    //(0,0) is the origin
    this.curX = this.curY = 0;
    
    //Prefill map with unknowns for reasonable bounds
    this.map = new HashMap<>();

    //However, as our starting origin is (0,0), our total boundary should be at least MAX_GRID_X*2 by MAX_GRID_Y*2
    for (int x = -MAX_GRID_X; x <= MAX_GRID_X; ++x) {
      for (int y = -MAX_GRID_Y; y <= MAX_GRID_Y; ++y) {
        this.map.put(new Point2D.Double(x, y), OBSTACLE_UNKNOWN);
      }
    }
    
    //Initially, we always consider ourselves to be facing up
    this.direction = UP;
    this.map.put(new Point2D.Double(0, 0), DIRECTION_UP);

    this.isGoldVisible = false;
    this.axeLocations = new LinkedList<>();
    this.keyLocations = new LinkedList<>();
    this.ssLocations = new LinkedList<>();
    this.waterLocations = new LinkedList<>();
  }


  /**
   * This method updated the internal map model based on the view provided.
   * To accomplish this, the view is first rotated so it is aligned with our initial map direction (UP).
   * Then all the tiles in the view are put into the map (overwriting any old values)
   *
   * This method also tracks new tools as they are found: goldLocation, keyLocations, axeLocation
   * It also sets isGoldVisible to true if the gold is present in the view.
   *
   * @param view Grid containing tiles around our player.
   */
  public void updateFromView(char view[][]) {

    //Determine how many times to rotate our view so it is facing UP based on our initial map
    //This is required so we can use the same map update code regardless of the views direction
    int numTimesToRotate = 0;

    switch (direction) {
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
          switch (direction) {
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

        Point2D.Double newTile = new Point2D.Double(xFinal, yFinal);

        //Save the locations of important tools
        if (curTile == TOOL_GOLD && !isGoldVisible) {
          goldLocation = newTile;
          isGoldVisible = true;
        }
        else if (curTile == TOOL_AXE && !axeLocations.contains(newTile)) {
          axeLocations.add(newTile);
        }
        else if (curTile == TOOL_KEY && !keyLocations.contains(newTile)) {
          keyLocations.add(newTile);
        }
        else if (curTile == TOOL_STEPPING_STONE && !ssLocations.contains(newTile)) {
          ssLocations.add(newTile);
        }
        else if (curTile == OBSTACLE_WATER && !waterLocations.contains(newTile)) {
          waterLocations.add(newTile);
        }


        //Special filter, don't replace this tile with water!
        //That will be handled when we reach the temporary water
        if (map.get(newTile) != null && map.get(newTile) == OBSTACLE_TEMPORARY_WATER)
          continue;

        //Update tile in map
        map.put(newTile, curTile);
      }
    }
    
    //todo: remove this
    //printMap();
  }


  /**
   * Updates the internal map as well as other variables (ie tool inventory) based on move the player is about to make.
   *
   * @param move the move (as a character) that has been made
   */
  public void updateFromMove(char move) {
    ++totalNumMoves;

    move = Character.toUpperCase(move); //moves should be uppercase
    char nextTile;

    switch (move) {
      case 'L':
        //Moved left
        switch (direction) {
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
        //Moved right
        switch (direction) {
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
        //Get tile directly in front of us, this is the tile we will be moving onto in this next move
        Point2D.Double nextTilePoint = getTileInFront(new Point2D.Double(curX, curY));
        nextTile = map.get(nextTilePoint);

        //Moving forwards against a wall, door or tree is a NOP
        //We have to use C and U to remove doors/trees and walls cant be moved into at all
        //When C and U moved are used, the view provided will reflect the changes
        //Our curX, curY do not change
        if ((nextTile == OBSTACLE_WALL) ||
          (nextTile == OBSTACLE_DOOR) ||
          (nextTile == OBSTACLE_TREE)
          )
          break;

        //Handle water and temporary water
        if (nextTile == OBSTACLE_WATER || nextTile == OBSTACLE_TEMPORARY_WATER) {
          if (num_stones_held > 0) {
            --num_stones_held; //we will place a stone on the water
          }

          if (nextTile == OBSTACLE_TEMPORARY_WATER) {
            if (num_stones_held > 0) {
              --num_stones_held; //we will place a stone on the water
            }
            map.put(nextTilePoint, OBSTACLE_STEPPING_STONE_PLACED);
          }

          waterLocations.remove(nextTilePoint); //no longer water
        }

        //Collect tools
        if (nextTile == TOOL_STEPPING_STONE) {
          if (ssLocations.contains(nextTilePoint)) //remove this stepping stone as we pick it up
            ssLocations.remove(nextTilePoint);

          ++num_stones_held;
        }
        else if (nextTile == TOOL_AXE) {
          haveAxe = true;
        }
        else if (nextTile == TOOL_KEY) {
          haveKey = true;
        }
        else if (nextTile == TOOL_GOLD) {
          haveGold = true;
        }

        //We moved forward, update our curX, curY
        switch (direction) {
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
        break;
      case 'U':
        break;
    }
  }

  /**
   * For debugging purposes.
   *
   * This method can be called, ideally at the end of updateFromView() to display the internal map which models the
   * environment. Some useful statistic are printed above the map such as current location coordinates and resources.
   */
  private void printMap() {
    System.out.print("\nInternal Map\n");
    System.out.print("------------------------\n");

    String strGold = haveGold ? "true" : "false";
    String strKey = haveKey ? "true" : "false";
    String strAxe = haveAxe ? "true" : "false";

    System.out.println("curX: " + curX + ", curY: " + curY);
    System.out.print("Total moves: " + totalNumMoves + "| Gold: " + strGold + "| Key: " + haveKey + "| Axe: " +
      haveAxe + "| Stepping Stones: " + num_stones_held);
    System.out.print("\n");

    //Traverse map showing grid from top left to bottom right
    for (int y = 12; y >= -12; --y) {
      for (int x = -12; x <= 12; ++x) {
        char curTile = map.get(new Point2D.Double(x, y));
        System.out.print(curTile);
      }

      System.out.print('\n');
    }
  }

  /**
   * Helper method which rotates a character grid clockwise.
   * Adapted from source linked below.
   *
   * @param mat matrix that is to be rotated in a clockwise direction
   * @return matrix rotated in a clockwise direction
   * @see <a href="https://stackoverflow.com/a/2800033/1800854">Matrix CW rotation code (by polygenelubricants)</a>
   */
  private static char[][] rotateCW(char[][] mat) {
    final int M = mat.length;
    final int N = mat[0].length;
    char[][] ret = new char[N][M];
    for (int r = 0; r < M; r++) {
      for (int c = 0; c < N; c++) {
        ret[c][M - 1 - r] = mat[r][c];
      }
    }
    return ret;
  }

  /**
   * Determines if a tile is passable. A passable tile is any tile that can me moved into
   * (so player is standing on it) that does not cause the player to lose the game.
   *
   * @param tile  the tile we are checking to see if it is passable
   * @param hasKey  does the player possess the key, used to determine if doors are passable
   * @param hasAxe  does the player possess the axe, used to determine if trees are passable
   * @return  true if tile is passable, false otherwise
   */
  public static boolean isTilePassable(char tile, boolean hasKey, boolean hasAxe) {
    //If tile does not meet one of the conditions below, it is NOT passable
    return (  (tile == State.OBSTACLE_SPACE) ||
              (tile == State.OBSTACLE_STEPPING_STONE_PLACED) ||
              (tile == State.OBSTACLE_TEMPORARY_WATER) ||
              (tile == State.TOOL_AXE) ||
              (tile == State.TOOL_KEY) ||
              (tile == State.TOOL_GOLD) ||
              (tile == State.TOOL_STEPPING_STONE) ||
              ((tile == State.OBSTACLE_DOOR) && hasKey) ||
              ((tile == State.OBSTACLE_TREE) && hasAxe) ||
              (tile == State.DIRECTION_UP) ||
              (tile == State.DIRECTION_DOWN) ||
              (tile == State.DIRECTION_LEFT) ||
              (tile == State.DIRECTION_RIGHT)
            );
  }

  /**
   * Delegates to getTileInFront(Point2D.Double tile, int curDirection) with curDirection equalling direction.
   *
   * @param tile the tile we wish to look in front of
   * @return  the tile in front of tile
   */
  public Point2D.Double getTileInFront(Point2D.Double tile) {
    return getTileInFront(tile, direction);
  }

  /**
   *  Returns the tile directly in front of tile.
   *  Used to make decisions regarding the forward move.
   *
   * @param tile  the tile we wish to look in front of
   * @param curDirection  the direction we are facing (UP, RIGHT, DOWN, LEFT)
   * @return  the tile point in front of tile
   */
  public Point2D.Double getTileInFront(Point2D.Double tile, int curDirection) {
    int nextX = (int)tile.getX();
    int nextY = (int)tile.getY();

    switch (curDirection) {
      case UP:
        nextY += 1;
        break;
      case DOWN:
        nextY -= 1;
        break;
      case LEFT:
        nextX -= 1;
        break;
      case RIGHT:
        nextX += 1;
        break;
    }

    //Get element at (nextX, nextY)
    return new Point2D.Double(nextX, nextY);
  }


  /**
   * @return true if player currently possesses the gold
   */
  public boolean haveGold() {
    return haveGold;
  }

  /**
   * @return true if player can see gold on the map
   */
  public boolean isGoldVisible() {
    return isGoldVisible;
  }

  /**
   * @return player location as a point
   */
  public Point2D.Double getPlayerLocation() {
    return new Point2D.Double(curX, curY);
  }

  /**
   * @return direction player is currently facing
   * @see State#UP
   * @see State#DOWN
   * @see State#LEFT
   * @see State#RIGHT
   */
  public int getDirection() {
    return direction;
  }

  /**
   * @return true if player has the key
   */
  public boolean haveKey() {
    return haveKey;
  }

  /**
   * @return true if player has the axe
   */
  public boolean haveAxe() {
    return haveAxe;
  }

  /**
   * @return the environment map (internal state map)
   */
  public Map<Point2D.Double, Character> getMap() {
    return map;
  }

  /**
   * @return the location of the gold
   */
  public Point2D.Double getGoldLocation() {
    return goldLocation;
  }

  /**
   * @return the locations of any keys
   */
  public List<Point2D.Double> getKeyLocations() {
    return keyLocations;
  }

  /**
   * @return the locations of any axes
   */
  public List<Point2D.Double> getAxeLocations() {
    return axeLocations;
  }

  /**
   * @return the locations of any stepping stones
   */
  public List<Point2D.Double> getSSLocations() {
    return ssLocations;
  }

  /**
   * @return the locations of any water tiles
   */
  public List<Point2D.Double> getWaterLocations() {
    return waterLocations;
  }

  /**
   * @return the number of stepping stones in the players inventory
   */
  public int getNumSteppingStones() {
    return num_stones_held;
  }

}