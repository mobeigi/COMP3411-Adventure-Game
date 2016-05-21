import java.util.*;
import java.io.*;
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
  
  //Tools
  final static char TOOL_AXE = 'a';
  final static char TOOL_KEY = 'k';
  final static char TOOL_STEPPING_STONE = 'o';
  final static char TOOL_STEPPING_STONE_PLACED = 'O';
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

  private int curX;
  private int curY;
  private int direction;  //direction we are currently facing

  private int totalNumMoves; //includes NOP moves and C/U moves
  private Queue<Character> pendingMoves;

  private boolean goldVisible;
  private Point2D.Double goldLocation; //coordinate of gold once it has been found
  private LinkedList<Point2D.Double> axeLocations;
  private LinkedList<Point2D.Double> keyLocations;

  /**
   * Constructor.
   */
  public State() {
    //Init variables
    haveAxe = false;
    haveKey = false;
    haveGold = false;
    num_stones_held = 0;

    needKey = false;
    needAxe = false;

    totalNumMoves = 0;
    pendingMoves = new LinkedList<Character>();
    
    //(0,0) is the origin
    curX = curY = 0;
    
    //Prefill map with unknowns for reasonable bounds
    map = new HashMap<Point2D.Double, Character>();

    //You may assume that the specified environment is no larger than 80 by 80
    //However, as our starting origin is (0,0), our total boundary should be at least 80*2 by 80*2 or 160x160
    for (int x = -80; x < 81; ++x) {
      for (int y = -80; y < 81; ++y) {
        map.put(new Point2D.Double(x, y), OBSTACLE_UNKNOWN);
      }
    }
    
    //Initially, we always consider ourselves to be facing up
    direction = UP;
    map.put(new Point2D.Double(0, 0), DIRECTION_UP);

    goldVisible = false;
    axeLocations = new LinkedList<Point2D.Double>();
    keyLocations = new LinkedList<Point2D.Double>();
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

        //Update tile in map
        map.put(newTile, curTile);
      }
    }
    
    //todo: remove this
    //this.printMap();
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
          new Point2D.Double(0, 0), this.direction, this.haveKey, this.haveAxe);
        break;
      }

      //Stage 3: Do we see gold?
      if (goldVisible) {
        //Yes: Can we reach the gold? (from our current position with current inventory)
        FloodFill ff = new FloodFill(this.map, new Point2D.Double(curX, curY), goldLocation);
        if (ff.isReachable(this.haveKey, this.haveAxe)) {
          //Yes: Do A* traversal to gold
          addAStarPathToPendingMoves(new Point2D.Double(curX, curY), goldLocation,
            this.direction, this.haveKey, this.haveAxe);
          break;
        } else {
          //Now we do some theoretical reachability tests
          //If we don't have the key, see if we can reach gold with a key
          if (!this.haveKey) {
            if (ff.isReachable(true, this.haveAxe))
              needKey = true;
          }

          //If we don't have the axe, see if we can reach gold with a axe
          if (!this.haveAxe) {
            if (ff.isReachable(this.haveKey, true))
              needAxe = true;
          }

          //If we don't have a key or axe, see if its possible to reach with both
          if (!this.haveKey && !this.haveAxe) {
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
        for (int i = 0; i < keyLocations.size(); ++i) {
          Point2D.Double location = keyLocations.get(i);

          //Sanity check
          if (this.map.get(location) == null || this.map.get(location) != TOOL_KEY) {
            assert (false); //todo remove
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(this.map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(this.haveKey, this.haveAxe)) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(new Point2D.Double(curX, curY), location,
              this.direction, this.haveKey, this.haveAxe);
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

        for (int i = 0; i < axeLocations.size(); ++i) {
          Point2D.Double location = axeLocations.get(i);

          //Sanity check
          if (this.map.get(location) == null || this.map.get(location) != TOOL_AXE) {
            assert (false); //todo remove
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(this.map, new Point2D.Double(curX, curY), location);
          if (ff.isReachable(this.haveKey, this.haveAxe)) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(new Point2D.Double(curX, curY), location,
              this.direction, this.haveKey, this.haveAxe);
            isAxeAttainable = true;
            break; //any axe will do, we only need one
          }
        }

        //Leave loop so we can get the axe
        if (isAxeAttainable)
          break;
      }

      //Stage 5: Explore to reveal unknown blocks
      SpiralSeek s = new SpiralSeek(this.map, new Point2D.Double(curX, curY));
      Point2D.Double explorationDestination = s.getTile(this.haveKey, this.haveAxe);
      //If the spiral seek algorithm successfully found a destination, it is guaranteed to be passable/reachable
      if (!explorationDestination.equals(new Point2D.Double(curX, curY))) {
        //Do A* traversal to exploration destination
        addAStarPathToPendingMoves(new Point2D.Double(curX, curY), explorationDestination,
          this.direction, this.haveKey, this.haveAxe);
        break; //todo was continue
      }

      //Stage 6: Cannot explore any further, is there an axe or tool we can pick up to perhaps help us explore more
      boolean canGetResource = false;

      if (!needKey && !haveKey && !keyLocations.isEmpty()) {
        needKey = true;
        canGetResource = true;
      }

      if (!needAxe &&  !haveAxe && !axeLocations.isEmpty()) {
        needAxe = true;
        canGetResource = true;
      }

      //If we can get a resource, go to next iteration so stage 4 can get us the key
      if (canGetResource)
        continue;

      //Stage 7


    }

    //Stage 1: If we reach this stage, we already had pending moves
    //Or decisions have been made above which added pending moves for us
    //Lets complete the pending moves
    //The conditional guard of the previous while loop ensures there are pending moves if we reach here
    if (!pendingMoves.isEmpty()) {  //this check is required as pendingMoves may change after the first check
      //Todo: remove before submission, slow down moves for us
      //try {
      //  Thread.sleep(100);
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
        nextTile = getTileInFront(new Point2D.Double(curX, curY));

        //Moving forwards against a wall, door or tree is a NOP
        //We have to use C and U to remove doors/trees and walls cant be moved into at all
        //When C and U moved are used, the view provided will reflect the changes
        //Our curX, curY do not change
        if ((nextTile == OBSTACLE_WALL) ||
          (nextTile == OBSTACLE_DOOR) ||
          (nextTile == OBSTACLE_TREE)
          )
          break;

        if (nextTile == OBSTACLE_WATER) {
          if (num_stones_held > 0) {
            --num_stones_held; //we will place a stone on the water
          } else {
            //Certain death
            //todo: handle this maybe
            System.out.println("CERTAIN DEATH IN updateFromMove()");
          }
        }

        if (nextTile == OBSTACLE_BOUNDARY) {
          //Certain death
          //todo: handle this maybe
          System.out.println("CERTAIN DEATH IN updateFromMove()");
        }

        //Collect tools
        if (nextTile == TOOL_STEPPING_STONE)
          ++num_stones_held;
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
   * Delegates to getTileInFront(Point2D.Double tile, int curDirection) with curDirection equalling this.direction.
   *
   * @param tile the tile we wish to look in front of
   * @return  the tile in front of tile
   */
  private char getTileInFront(Point2D.Double tile) {
    return getTileInFront(tile, this.direction);
  }

  /**
   *  Returns the tile directly in front of tile.
   *  Used to make decisions regarding the forward move.
   *
   * @param tile  the tile we wish to look in front of
   * @param curDirection  the direction we are facing (UP, RIGHT, DOWN, LEFT)
   * @return  the tile in front of tile
   */
  private char getTileInFront(Point2D.Double tile, int curDirection) {
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
    return this.map.get(new Point2D.Double(nextX, nextY));
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
              (tile == State.TOOL_STEPPING_STONE_PLACED) ||
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
  //Always returns the minimum costing moves (least number of moves to face final diretion)


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
    LinkedList<Character> l = new LinkedList<Character>();

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
    AStar a = new AStar(this.map, start, goal);
    a.search(hasKey, hasAxe);

    //Get optimal path
    LinkedList<Point2D.Double> path = a.getPath();
    path.addLast(start); //add starting position to end of path (before reversal)

    //Iterate through moves in reverse so they are presented as moves from start -> goal
    //We do not process the final (landing) tile as it is our destination, ie dont process i = 0
    for (int i = path.size() - 1; i >= 1; --i) {
      Point2D.Double element = path.get(i);

      //Check what direction we are going in (UP, DOWN, LEFT, RIGHT)
      //We compare adjacent tiles at i and (i-1)
      int directionHeaded = getAdjacentTileDirection(element, path.get(i - 1));

      //Get list of moves needed before we go forward (ie do we need to rotate, use L/R moves?)
      LinkedList<Character> alignMoves = getAlignmentMoves(curDirection, directionHeaded);

      //Add alignment moves to pendingMoves
      this.pendingMoves.addAll(alignMoves);

      //Update curDirection to reflect alignMoves changes
      curDirection = directionHeaded;

      //Check if we need to cut down a tree or unlock a door
      char nextTile = getTileInFront(element, curDirection);
      if (nextTile == OBSTACLE_TREE) {
        this.pendingMoves.add(MOVE_CHOPTREE);
      } else if (nextTile == OBSTACLE_DOOR) {
        this.pendingMoves.add(MOVE_UNLOCKDOOR);
      }

      //Now we also need 1 forward move
      this.pendingMoves.add(MOVE_GOFORWARD);
    }
  }

}