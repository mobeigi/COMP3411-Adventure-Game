import java.util.*;
import java.awt.geom.Point2D;

/**
 * State class.
 *
 * Maintains internal map (model) of environment as well as the locations of resources (tools) and the gold.
 * Also acts as a decision maker which makes new moves based on the current environment.
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


  //Class variables
  private Map<Point2D.Double, Character> map;

  //Tool inventory
  private boolean haveAxe;
  private boolean haveKey;
  private boolean haveGold;
  private int num_stones_held;

  private boolean needKey;
  private boolean needAxe;
  private boolean needSS;

  private int curX;
  private int curY;
  private int direction;  //direction we are currently facing

  private int totalNumMoves; //includes NOP moves and C/U moves
  private Queue<Character> pendingMoves;

  private boolean goldVisible;
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

    this.needKey = false;
    this.needAxe = false;
    this.needSS = false;

    this.totalNumMoves = 0;
    this.pendingMoves = new LinkedList<>();
    
    //(0,0) is the origin
    this.curX = this.curY = 0;
    
    //Prefill map with unknowns for reasonable bounds
    this.map = new HashMap<>();

    //You may assume that the specified environment is no larger than 80 by 80
    //However, as our starting origin is (0,0), our total boundary should be at least 80*2 by 80*2 or 160x160
    for (int x = -80; x < 81; ++x) {
      for (int y = -80; y < 81; ++y) {
        this.map.put(new Point2D.Double(x, y), OBSTACLE_UNKNOWN);
      }
    }
    
    //Initially, we always consider ourselves to be facing up
    this.direction = UP;
    this.map.put(new Point2D.Double(0, 0), DIRECTION_UP);

    this.goldVisible = false;
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
   * It also sets goldVisible to true if the gold is present in the view.
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
      default:
        assert(false); //todo remove
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
        if (curTile == TOOL_GOLD && !goldVisible) {
          goldLocation = newTile;
          goldVisible = true;
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
   * This method makes our game playing decisions and returns 1 valid move when called (L,R,F,C,U).
   *
   * todo: either link to program flow discussion or paste it here
   *
   * @return a valid move (as a character) corresponding to next move the player will make
   */
  public char makeMove() {

    //Stage 1
    //If we have no pending moves, then we must decide what to do
    while (pendingMoves.isEmpty()) {

      //Stage 2: Do we have gold
      //Yes: Do A* traversal to starting location, aka (0,0)
      if (haveGold) {
        addAStarPathToPendingMoves(new Point2D.Double(curX, curY),
          new Point2D.Double(0, 0), direction, haveKey, haveAxe);
        break;
      }

      //Stage 3: Do we see gold?
      if (goldVisible) {
        //Yes: Can we reach the gold? (from our current position with current inventory)
        FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), goldLocation);
        if (ff.isReachable(haveKey, haveAxe)) {
          //Yes: Do A* traversal to gold
          addAStarPathToPendingMoves(new Point2D.Double(curX, curY), goldLocation, direction, haveKey, haveAxe);
          break;
        } else {
          //Now we do some theoretical reachability tests
          //If we don't have the key, see if we can reach gold with a key
          if (!haveKey) {
            if (ff.isReachable(true, haveAxe))
              needKey = true;
          }

          //If we don't have the axe, see if we can reach gold with a axe
          if (!haveAxe) {
            if (ff.isReachable(haveKey, true))
              needAxe = true;
          }

          //If we don't have a key or axe, see if its possible to reach with both
          if (!haveKey && !haveAxe) {
            if (ff.isReachable(true, true)) {
              needKey = true;
              needAxe = true;
            }
          }
        }
      }

      //Stage 4: Do we know location of a needed resources?
      //todo: add logic to pick closest (man dist) one rather than breaking
      if (needKey && !keyLocations.isEmpty()) {
        //Yes: Check if reachable with current inventory and traverse to it if so
        boolean isKeyAttainable = false;

        for (Point2D.Double location : keyLocations) {
          //Sanity check
          if (map.get(location) == null || map.get(location) != TOOL_KEY) {
            assert (false); //todo remove
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(new Point2D.Double(curX, curY), location, direction, haveKey, haveAxe);
            isKeyAttainable = true;
            break; //any key will do, we only need one
          }
        }

        //Leave loop so we can get the key
        if (isKeyAttainable)
          break;
      }

      if (needAxe && !axeLocations.isEmpty()) {
        //Yes: Check if reachable with current inventory and traverse to it if so
        boolean isAxeAttainable = false;

        for (Point2D.Double location : axeLocations) {
          //Sanity check
          if (map.get(location) == null || map.get(location) != TOOL_AXE) {
            assert (false); //todo remove
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(new Point2D.Double(curX, curY), location, direction, haveKey, haveAxe);
            isAxeAttainable = true;
            break; //any axe will do, we only need one
          }
        }

        //Leave loop so we can get the axe
        if (isAxeAttainable)
          break;
      }

      if (needSS && !ssLocations.isEmpty()) {
        boolean isSSAttainable = false;

        for (Point2D.Double location : ssLocations) {
          //Sanity check
          if (map.get(location) == null || map.get(location) != TOOL_STEPPING_STONE) {
            assert (false); //todo remove
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(new Point2D.Double(curX, curY), location, direction, haveKey, haveAxe);
            isSSAttainable = true;
            break; //get any reachable SS
          }
        }

        //Leave loop so we can get the stepping stone
        if (isSSAttainable)
          break;
      }

      //Stage 5: Explore to reveal unknown blocks
      SpiralSeek s = new SpiralSeek(map, new Point2D.Double(curX, curY));
      Point2D.Double explorationDestination = s.getTile(haveKey, haveAxe);
      //If the spiral seek algorithm successfully found a destination, it is guaranteed to be passable/reachable
      if (!explorationDestination.equals(new Point2D.Double(curX, curY))) {
        //Do A* traversal to exploration destination
        addAStarPathToPendingMoves(new Point2D.Double(curX, curY), explorationDestination, direction, haveKey, haveAxe);
        break; //todo was continue
      }

      //Stage 6: Cannot explore any further, is there a reachable axe or tool we can pick up to perhaps help us explore more
      boolean canGetResource = false;

      if (!needKey && !haveKey && !keyLocations.isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : keyLocations) {
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            needKey = true;
            canGetResource = true;
            break;
          }
        }
      }

      if (!needAxe && !haveAxe && !axeLocations.isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : axeLocations) {
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            needAxe = true;
            canGetResource = true;
            break;
          }
        }
      }

      if (!ssLocations.isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : ssLocations) {
          FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(haveKey, haveAxe)) {
            needSS = true;
            canGetResource = true;
            break;
          }
        }
      }

      //If we can get a resource, go to next iteration so stage 4 can get us the resource
      if (canGetResource)
        continue;

      //Stage 7: Need to use our stepping stones to get to a new unreachable area
      //Note at this stage we have all resources that are reachable to us
      //So any tools we still see on the map are guaranteed to be unreachable (without using stepping stones)

      //Try to get to the area near gold
      if (goldVisible) {
        if (useSteppingStoneTowardsGoal(goldLocation))
          break;
      }

      //Try to get to the area near another stepping stone
      if (!ssLocations.isEmpty()) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : ssLocations) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Try to get to the area near another key (don't prefer if we already have key)
      if (!keyLocations.isEmpty() && !haveKey) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : keyLocations) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Try to get to the area near another axe (don't prefer if we already have axe)
      if (!axeLocations.isEmpty() && !haveAxe) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : axeLocations) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }



    }

    //Stage 1: If we reach this stage, we already had pending moves
    //Or decisions have been made above which added pending moves for us
    //Lets complete the pending moves
    //The conditional guard of the previous while loop ensures there are pending moves if we reach here
    if (!pendingMoves.isEmpty()) {  //this check is required as pendingMoves may change after the first check
      //Todo: remove before submission, slow down moves for us
      //try {
      // Thread.sleep(100);
      //} catch(InterruptedException ex) {
      //  Thread.currentThread().interrupt();
      //}

      ++totalNumMoves;
      char moveToMake = pendingMoves.remove();
      updateFromMove(moveToMake);
      return moveToMake;
    }


    //todo: remove below manual movement
    /*
    int ch = 0;

    System.out.print("Enter Action(s): ");

    try {
      while (ch != -1) {
        // read character from keyboard
        ch = System.in.read();

        switch (ch) { // if character is a valid action, return it
          case 'F':
          case 'L':
          case 'R':
          case 'C':
          case 'U':
          case 'f':
          case 'l':
          case 'r':
          case 'c':
          case 'u':
            ++totalNumMoves;
            updateFromMove((char) ch);
            return ((char) ch);
        }
      }
    } catch (IOException e) {
      System.out.println("IO error:" + e);
    }

    return 0;
    */

    return 0;
  }

  //Update map based on changes from a move
  //todo: optimize switch statements for direction


  /**
   * Updates the internal map as well as other variables (ie tool inventory) based on move the player is about to make.
   *
   * @param move the move (as a character) that has been made
   */
  private void updateFromMove(char move) {
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
          default:
            assert (false); //todo remove
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
          default:
            assert (false); //todo remove
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


        if (nextTile == OBSTACLE_WATER && num_stones_held == 0) {
          //Certain death
          //todo: handle this maybe
          System.out.println("CERTAIN DEATH IN updateFromMove()");
        }

        if (nextTile == OBSTACLE_BOUNDARY) {
          //Certain death
          //todo: handle this maybe
          System.out.println("CERTAIN DEATH IN updateFromMove()");
        }

        //Collect tools
        if (nextTile == TOOL_STEPPING_STONE) {
          if (ssLocations.contains(nextTilePoint)) //remove this stepping stone as we pick it up
            ssLocations.remove(nextTilePoint);

          ++num_stones_held;
        }
        else if (nextTile == TOOL_AXE) {
          haveAxe = true;
          needAxe = false;  //no longer need axes for rest of game
        }
        else if (nextTile == TOOL_KEY) {
          haveKey = true;
          needKey = false; //no longer need keys for rest of game
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
          default:
            assert (false); //todo remove
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
   * Delegates to getTileInFront(Point2D.Double tile, int curDirection) with curDirection equalling direction.
   *
   * @param tile the tile we wish to look in front of
   * @return  the tile in front of tile
   */
  private Point2D.Double getTileInFront(Point2D.Double tile) {
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
  private Point2D.Double getTileInFront(Point2D.Double tile, int curDirection) {
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
      default:
        assert(false); //todo remove
    }

    //Get element at (nextX, nextY)
    return new Point2D.Double(nextX, nextY);
  }

  //Returns true if tile is passable based on items we have

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
   *  Returns the direction (4-way) that you must travel to get from the start point
   *  to the goal point.
   *
   * @param start the starting point
   * @param goal  the goal point
   * @return  direction to travel(UP, RIGHT, DOWN, LEFT) to get from start to goal
   *          or -1 if non-adjacent tiles or start and goal points are the same
   */
  private int getAdjacentTileDirection(Point2D.Double start, Point2D.Double goal) {
    int xDiff = (int)(goal.getX() - start.getX());
    int yDiff = (int)(goal.getY() - start.getY());
    int retDirection = -1;

    if (xDiff != 0) {
      //Either left or right
      if (xDiff < 0) {
        retDirection = LEFT;
      } else { //>0
        retDirection = RIGHT;
      }
    }
    else if (yDiff != 0) {
      //Either up or down
      if (yDiff < 0) {
        retDirection = DOWN;
      }
      else { //>0
        retDirection = UP;
      }
    }

    return retDirection;
  }

  //Returns moves that result in player facing the final direction based on initial direction
  //Always returns the minimum costing moves (least number of moves to face final direction)


  /**
   * Returns a list of moves needed to ensure that the initial and final directions are aligned.
   * That is, to ensure that after moves are carried out, initialDirection will be equal to finalDirection
   *
   * @param initialDirection  direction player is facing
   * @param finalDirection  final direction player should be facing
   * @return  list of moves that should be completed to ensure direction alignment
   *          or empty list if initialDirection equals finalDirection
   */
  private LinkedList<Character> getAlignmentMoves(int initialDirection, int finalDirection) {
    LinkedList<Character> l = new LinkedList<>();

    if (initialDirection == finalDirection) //no moves need, already aligned
      return l;

    int numLeftMoves, numRightMoves;

    //Calculate number of left and right moves
    if (initialDirection > finalDirection) {
      numLeftMoves = mod(initialDirection - finalDirection, 4);
      numRightMoves = mod(4 - numLeftMoves, 4);
    }
    else { //finalDirection > initialDirection
      numRightMoves = finalDirection - initialDirection;
      numLeftMoves = mod(4 - numRightMoves, 4);
    }

    //Determine best result (the one with less overall moves)
    if (numLeftMoves <= numRightMoves) {
      //Left moves are better or the same
      for (int i = 0; i < numLeftMoves; ++i)
        l.add(MOVE_TURNLEFT);

    } else { //right moves are better
      for (int i = 0; i < numRightMoves; ++i)
        l.add(MOVE_TURNRIGHT);
    }

    return l;
  }

  /**
   * Helper modulo function.
   * The value of an integer modulo m is equal to the remainder left when the number is divided by m.
   *
   * @param n the integer to be divided
   * @param m the divisor (modulus)
   * @return positive modulo result, that is: n (mod m)) where n >= 0 && n < m
   */
  private int mod(int n, int m) { //todo: make static?
    int result = n % m;
    if (result < 0)
      result += m;

    return result;
  }

  /**
   * Utilises the AStar class to perform an A* algorithm on the current map to get from start to goal
   * given the current direction and inventory.
   *
   * Then gathers the path and adjusts it to create a list of moves that the player can take to reach
   * the goal. This list of moves is then added to the pendingMoves queue.
   *
   * Precondition: This method assumes that goal is reachable from start. Only call this method if a
   * successful reachability test has been completed from start to goal given the map, current direction
   * and inventory.
   *
   * @param start the starting point (typically the current player position)
   * @param goal the goal point (destination player is attempting to reach)
   * @param curDirection  the direction player is facing
   * @param hasKey  if the player has the key
   * @param hasAxe  if te player has the axe
   */
  private void addAStarPathToPendingMoves(Point2D.Double start, Point2D.Double goal, int curDirection,
                                          boolean hasKey, boolean hasAxe) {
    //New AStar search
    AStar a = new AStar(map, start, goal);
    a.search(hasKey, hasAxe);

    //Get optimal path
    LinkedList<Point2D.Double> path = a.getPath();
    path.addLast(start); //add starting position to end of path (before reversal)

    //Iterate through moves in reverse so they are presented as moves from start -> goal
    //We do not process the final (landing) tile as it is our destination, ie don't process i = 0
    for (int i = path.size() - 1; i >= 1; --i) {
      Point2D.Double element = path.get(i);

      //Check what direction we are going in (UP, DOWN, LEFT, RIGHT)
      //We compare adjacent tiles at i and (i-1)
      int directionHeaded = getAdjacentTileDirection(element, path.get(i - 1));

      //Get list of moves needed before we go forward (ie do we need to rotate, use L/R moves?)
      LinkedList<Character> alignMoves = getAlignmentMoves(curDirection, directionHeaded);

      //Add alignment moves to pendingMoves
      pendingMoves.addAll(alignMoves);

      //Update curDirection to reflect alignMoves changes
      curDirection = directionHeaded;

      //Check if we need to cut down a tree or unlock a door
      char nextTile = map.get(getTileInFront(element, curDirection));
      if (nextTile == OBSTACLE_TREE) {
        pendingMoves.add(MOVE_CHOPTREE);
      } else if (nextTile == OBSTACLE_DOOR) {
        pendingMoves.add(MOVE_UNLOCKDOOR);
      }

      //Now we also need 1 forward move
      pendingMoves.add(MOVE_GOFORWARD);
    }
  }

  /**
   * Given an arr array with date, creates combinations of depth n.
   * See link for source.
   *
   * @param n depth of combinations
   * @param arr array containing data
   * @param list  empty array that is to be filled with all of the combinations
   * @see <a href="https://stackoverflow.com/a/29910788/1800854">Algorithm to get all the combinations of size n
   * from an array(by Raniz)</a>
   */
  private static void combinations(int n, Point2D.Double[] arr, List<Point2D.Double[]> list) {
    // Calculate the number of arrays we should create
    long numArrays = binomial(arr.length, n); //use binomial here instead of power as positions do not matter
    
    // Create each array
    for(int i = 0; i < numArrays; ++i) {
      list.add(new Point2D.Double[n]);
    }
    // Fill up the arrays
    for(int j = 0; j < n; ++j) {
      // This is the period with which this position changes, i.e.
      // a period of 5 means the value changes every 5th array
      int period = (int) Math.pow(arr.length, n - j - 1);
      for(int i = 0; i < numArrays; i++) {
        Point2D.Double[] current = list.get(i);
        // Get the correct item and set it
        int index = i / period % arr.length;
        current[j] = arr[index];
      }
    }
  }

  /**
   * Helper function: Calculate binomial coefficient.
   *
   * @param n number of possibilities to pick
   * @param k unordered outcomes
   * @return  binomial coefficient, ie (n, k)
   * @see <a href="https://stackoverflow.com/a/36926193/1800854">Binomial Coeffecient method (by Chris Sherlock)</a>
   */
  private static long binomial(int n, int k)
  {
    if (k>n-k)
      k=n-k;

    long b=1;
    for (int i=1, m=n; i<=k; i++, m--)
      b=b*m/i;
    return b;
  }

  /**
   * Perform a connectivity test using the FloodFill algorithm.
   * A connected group of points is one where every point is reachable from any other point in the group.
   *
   * @param group group of points that are to be compared
   * @return true if group array is of size 1 or less or all points are connected, false otherwise
   */
  private boolean isPointGroupAdjacent(Point2D.Double[] group) {
    if (group.length <= 1)  //no points or single points are adjacent
      return true;

    Map<Point2D.Double, Character> tempMap = new HashMap<>();

    //Do a flood fill search on a simplified map
    LinkedList<Point2D.Double> q = new LinkedList<>();
    Set<Point2D.Double> isConnected = new HashSet<>();
    q.add(group[0]); //Pick first element to be the 'start' (any element will do)

    while (!q.isEmpty()) {
      Point2D.Double first = q.removeFirst();

      //If not processed
      if (!isConnected.contains(first)) {

        //See if this is one of the elements in the group, if so we will process it
        //Otherwise, it can be considered a unaccessible block
        boolean isInGroup = false;
        for (Point2D.Double point : group) {
          if (first.equals(point)) {
            isInGroup = true;
            break;
          }
        }

        if (!isInGroup)
          continue;

        //Mark as processed
        isConnected.add(first);

        //Add west, east, north, south nodes
        for (int i = 0; i < 4; ++i) {
          int neighbourX = (int)first.getX();
          int neighbourY = (int)first.getY();

          switch (i) {
            case 0:
              //Tile to right
              neighbourX += 1;
              break;
            case 1:
              //Tile to left
              neighbourX -= 1;
              break;
            case 2:
              //Tile above
              neighbourY += 1;
              break;
            case 3:
              //Tile below
              neighbourY -= 1;
              break;
          }

          Point2D.Double neighbour = new Point2D.Double(neighbourX, neighbourY);

          if (!isConnected.contains(neighbour))
            q.add(neighbour);
        }
      }
    }

    //Now ensure every point is connected
    for (Point2D.Double point : group) {
      if (!isConnected.contains(point))
        return false;
    }

    //Here all points would have been connected so every tile is connected
    return true;
  }

  /**
   * Given an unreachable destination goal, creates all possible combinations of waters of increasing depth
   * starting from n = 1, n = 2...etc until n = num_stones_held. Then all combinations where water tiles are not
   * adjacent (touching) are eliminated as they will never allow you to reach a new previously unreachable area
   * (ie they are impossible solutions).
   *
   * Finally, a reachability test is completed for each group to ensure new area is reachable if stepping stones
   * are placed over water in the group. If new area is reachable, an A* traversal is completed using
   * addAStarPathToPendingMoves.
   *
   * @param goal unreachable destination goal
   * @return true if goal is reachable if stepping stones are used on various water tiles
   *              (moves also added via addAStarPathToPendingMoves as side effect), false otherwise
   * @see State#addAStarPathToPendingMoves(Point2D.Double, Point2D.Double, int, boolean, boolean)
   */
  private boolean useSteppingStoneTowardsGoal(Point2D.Double goal) {
    boolean moveMade = false;
    List<Point2D.Double[]> solutionGroup = new ArrayList<Point2D.Double[]>(); //stores all possible solutions

    for (int i = 1; i <= num_stones_held && !moveMade; ++i) {
      List<Point2D.Double[]> combinationList = new ArrayList<Point2D.Double[]>();
      Point2D.Double[] arr = (Point2D.Double[]) waterLocations.toArray(new Point2D.Double[waterLocations.size()]);
      combinations(i, arr, combinationList); //get combinations

      for (Point2D.Double[] group : combinationList) {

        //Filter non adjacent point groups
        //The only way to reach the objective is to use stepping stones on adjacent water tiles
        //This filter results in severe performance gains as we do cheap connectivity tests with no map compared to
        //more costly flood fill searches on the actual map
        if (!isPointGroupAdjacent(group))
          continue;

        //Replace every waterTile with a temporary water block for now
        for (Point2D.Double waterTile : group) {
          map.put(waterTile, OBSTACLE_TEMPORARY_WATER);
        }

        //Perform a reachability test to the goal
        FloodFill ff = new FloodFill(map, new Point2D.Double(curX, curY), goal);
        if (ff.isReachable(haveKey, haveAxe)) {
          //Do A* traversal to location
          //addAStarPathToPendingMoves(new Point2D.Double(curX, curY), goal, direction, haveKey, haveAxe);
          solutionGroup.add(group);
          moveMade = true;
          //break;
        }

        //Restore stepping stones with original water
        for (Point2D.Double waterTile : group) {
          map.put(waterTile, OBSTACLE_WATER);
        }
      }
    }

    //Check for best solution based on distance to gold
    if (!solutionGroup.isEmpty()) {
      int selectedIndex = 0; //initially pick first solution (default)

      List<Point2D.Double[]> goldSolutions = new ArrayList<Point2D.Double[]>();

      //Check to see if we can pick a solution that will get us closer to gold
      if (goldVisible) {
        int minCost = 999999; //represents infinity

        for (int i = 0; i < solutionGroup.size(); ++i) {
          Point2D.Double[] group = solutionGroup.get(i);
          int groupCost = 0;

          //Pick solution closest to gold
          for (Point2D.Double solution : group) {
            //Manhattan distance to gold
            groupCost += (Math.abs((int) solution.getX() - (int) goldLocation.getX()) +
              Math.abs((int) solution.getY() - (int) goldLocation.getY()));

            //If this solution has same X/Y as gold then its a solution that will eventually lead to gold!
            if (solution.getX() == goldLocation.getX() || solution.getY() == goldLocation.getY()) {
              if (!goldSolutions.contains(group))
                goldSolutions.add(group);
            }
          }

          if (groupCost < minCost) {
            minCost = groupCost;
            selectedIndex = i;
          }
        }
      }

      boolean madePlan = false;

      //A solution that leads to gold is not possible if goldSolutions is empty
      //Lets find a solution that leads to stepping stones
      if (goldSolutions.isEmpty() && !ssLocations.isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, ssLocations);
        madePlan = true;
      }

      //A solution that leads to stepping stone is not possible
      //Lets find a solution that leads to keys
      if (!madePlan && !keyLocations.isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, keyLocations);
        madePlan = true;
      }

      //A solution that leads to keys is not possible
      //Lets find a solution that leads to axes
      if (!madePlan && !axeLocations.isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, axeLocations);
        madePlan = true;
      }

      //At this stage, using the stepping stone on any water tile has same effect
      //Each one has the same benefit so simply just pick any solution (ie the first one)

      //Replace every waterTile with a temporary water block
      for (Point2D.Double waterTile : solutionGroup.get(selectedIndex)) {
        map.put(waterTile, OBSTACLE_TEMPORARY_WATER);
      }
    }

    //We can now traverse to the goal
    addAStarPathToPendingMoves(new Point2D.Double(curX, curY), goal, direction, haveKey, haveAxe);

    return moveMade;
  }

  /**
   * Given a list of solution points and some interest points calculated the cost of each solution.
   * It does this by applying the manhattan distance formula from each point (water tile) in the solution to
   * every interest point and cumulatively adding this to a total. The index of the minimum costing solution is
   * maintained and eventually returned. This is used to determine which water tile should be removed (via stepping
   * stones) to travel based on the effectiveness of the move rather than just picking at random. If all moves have
   * the same cost (ie it doesn't matter which one you pick), 0 is returned (first solution index).
   *
   * @param solutionPoints list of solutions (adjacent water tiles) that are to be examined
   * @param interestPoints list of interest points that should be included in solution cost calculation
   * @return the index of the best solution (lowest cost) in solutionGroup, 0 by default if no best solution or all
   *         solutions have the same cost
   */
  private int getOptimalWaterTile(List<Point2D.Double[]> solutionPoints, List<Point2D.Double> interestPoints) {
    int selectedIndex = 0; //default solution is 0

    if (!interestPoints.isEmpty()) {
      int minCost = 999999; //represents infinity

      for (int i = 0; i < solutionPoints.size(); ++i) {
        Point2D.Double[] group = solutionPoints.get(i);
        int groupCost = 0;

        //Pick solution closest to gold
        for (Point2D.Double solution : group) {
          //For every stepping stone we see
          for (Point2D.Double location : interestPoints) {
            //Manhattan distance to each interest point
            groupCost += (Math.abs((int) solution.getX() - (int) location.getX()) +
              Math.abs((int) solution.getY() - (int) location.getY()));
          }
        }

        //If new minimum costing solution, save index
        if (groupCost < minCost) {
          minCost = groupCost;
          selectedIndex = i;
        }
      }
    }

    return selectedIndex;
  }
}