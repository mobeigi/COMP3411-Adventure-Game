import java.awt.geom.Point2D;
import java.util.*;

/**
 * MoveMaker class.
 *
 * Makes decisions about the next move (or moves) to make based on internal state (the map environment).
 * Functions by adding moves to a move queue which are carried out first before deciding what other moves to make.
 *
 * @author Mohammad Ghasembeigi
 * @version 1.0
 */
public class MoveMaker {
  private State state;
  private Queue<Character> pendingMoves;

  private boolean needKey;
  private boolean needAxe;
  private boolean needSS;

  public MoveMaker() {
    this.state = new State();
    this.pendingMoves = new LinkedList<>();

    this.needKey = false;
    this.needAxe = false;
    this.needSS = false;
  }

  /**
   * This method makes our game playing decisions and returns 1 valid move when called (L,R,F,C,U).
   *
   * @return a valid move (as a character) corresponding to next move the player will make
   */
  public char makeMove(char view[][]) {
    //First update our state view
    state.updateFromView(view);

    //Stage 1
    //If we have no pending moves, then we must decide what to do
    while (pendingMoves.isEmpty()) {

      //Stage 2: Do we have gold
      //Yes: Do A* traversal to starting location, aka (0,0)
      if (state.haveGold()) {
        addAStarPathToPendingMoves(state.getPlayerLocation(),
          new Point2D.Double(0, 0), state.getDirection(), state.haveKey(), state.haveAxe());
        break;
      }

      //Stage 3: Do we see gold?
      if (state.isGoldVisible()) {
        //Yes: Can we reach the gold? (from our current position with current inventory)
        FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), state.getGoldLocation());
        if (ff.isReachable(state.haveKey(), state.haveAxe())) {
          //Yes: Do A* traversal to gold
          addAStarPathToPendingMoves(state.getPlayerLocation(), state.getGoldLocation(), state.getDirection(), state.haveKey(), state.haveAxe());
          break;
        } else {
          //Now we do some theoretical reachability tests
          //If we don't have the key, see if we can reach gold with a key
          if (!state.haveKey()) {
            if (ff.isReachable(true, state.haveAxe()))
              needKey = true;
          }

          //If we don't have the axe, see if we can reach gold with a axe
          if (!state.haveAxe()) {
            if (ff.isReachable(state.haveKey(), true))
              needAxe = true;
          }

          //If we don't have a key or axe, see if its possible to reach with both
          if (!state.haveKey() && !state.haveAxe()) {
            if (ff.isReachable(true, true)) {
              needKey = true;
              needAxe = true;
            }
          }
        }
      }

      //Stage 4: Do we know location of a needed resources?
      if (needKey && !state.getKeyLocations().isEmpty()) {
        //Yes: Check if reachable with current inventory and traverse to it if so
        boolean isKeyAttainable = false;

        for (Point2D.Double location : state.getKeyLocations()) {
          //Sanity check
          if (state.getMap().get(location) == null || state.getMap().get(location) != State.TOOL_KEY) {
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(state.getPlayerLocation(), location, state.getDirection(), state.haveKey(), state.haveAxe());
            isKeyAttainable = true;
            break; //any key will do, we only need one
          }
        }

        //Leave loop so we can get the key
        if (isKeyAttainable)
          break;
      }

      if (needAxe && !state.getAxeLocations().isEmpty()) {
        //Yes: Check if reachable with current inventory and traverse to it if so
        boolean isAxeAttainable = false;

        for (Point2D.Double location : state.getAxeLocations()) {
          //Sanity check
          if (state.getMap().get(location) == null || state.getMap().get(location) != State.TOOL_AXE) {
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(state.getPlayerLocation(), location, state.getDirection(), state.haveKey(), state.haveAxe());
            isAxeAttainable = true;
            break; //any axe will do, we only need one
          }
        }

        //Leave loop so we can get the axe
        if (isAxeAttainable)
          break;
      }

      if (needSS && !state.getSSLocations().isEmpty()) {
        boolean isSSAttainable = false;

        for (Point2D.Double location : state.getSSLocations()) {
          //Sanity check
          if (state.getMap().get(location) == null || state.getMap().get(location) != State.TOOL_STEPPING_STONE) {
            continue;
          }

          //Is this location reachable?
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            //Do A* traversal to location
            addAStarPathToPendingMoves(state.getPlayerLocation(), location, state.getDirection(), state.haveKey(), state.haveAxe());
            isSSAttainable = true;
            break; //get any reachable SS
          }
        }

        //Leave loop so we can get the stepping stone
        if (isSSAttainable)
          break;
      }

      //Stage 5: Explore to reveal unknown blocks
      SpiralSeek s = new SpiralSeek(state.getMap(), state.getPlayerLocation());
      Point2D.Double explorationDestination = s.getTile(state.haveKey(), state.haveAxe());
      //If the spiral seek algorithm successfully found a destination, it is guaranteed to be passable/reachable
      if (!explorationDestination.equals(state.getPlayerLocation())) {
        //Do A* traversal to exploration destination
        addAStarPathToPendingMoves(state.getPlayerLocation(), explorationDestination, state.getDirection(), state.haveKey(), state.haveAxe());
        break;
      }

      //Stage 6: Cannot explore any further, is there a reachable axe or tool we can pick up to perhaps help us explore more
      boolean canGetResource = false;

      if (!needKey && !state.haveKey() && !state.getKeyLocations().isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : state.getKeyLocations()) {
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            needKey = true;
            canGetResource = true;
            break;
          }
        }
      }

      if (!needAxe && !state.haveAxe() && !state.getAxeLocations().isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : state.getAxeLocations()) {
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            needAxe = true;
            canGetResource = true;
            break;
          }
        }
      }

      if (!state.getSSLocations().isEmpty()) {
        //Ensure at least one is reachable
        for (Point2D.Double location : state.getSSLocations()) {
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
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
      if (state.isGoldVisible()) {
        if (useSteppingStoneTowardsGoal(state.getGoldLocation()))
          break;
      }

      //Try to get to the area near another stepping stone
      if (!state.getSSLocations().isEmpty()) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : state.getSSLocations()) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Try to get to the area near another key (don't prefer if we already have key)
      if (!state.getKeyLocations().isEmpty() && !state.haveKey()) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : state.getKeyLocations()) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Try to get to the area near another axe (don't prefer if we already have axe)
      if (!state.getAxeLocations().isEmpty() && !state.haveAxe()) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : state.getAxeLocations()) {
          if (useSteppingStoneTowardsGoal(location)) {
            canGetToNewArea = true;
            break;
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Try to get to the area near another axe (don't prefer if we already have axe)
      if (!state.getSpaceLocations().isEmpty()) {
        boolean canGetToNewArea = false;
        for (Point2D.Double location : state.getSpaceLocations()) {
          //Ensure this blank space is reachable from our current player location
          FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), location);
          if (ff.isReachable(state.haveKey(), state.haveAxe())) {
            if (useSteppingStoneTowardsGoal(location)) {
              canGetToNewArea = true;
              break;
            }
          }
        }

        if (canGetToNewArea)
          break;
      }

      //Stage 8: Disaster stage
      //Okay we really should never get here unless there is no solution possible or something odd happens
      //If we do though, lets A* to (0,0) home and hope we can recover
      addAStarPathToPendingMoves(state.getPlayerLocation(), new Point2D.Double(0, 0),
        state.getDirection(), state.haveKey(), state.haveAxe());
      break;
    }

    //Stage 1: If we reach this stage, we already had pending moves
    //Or decisions have been made above which added pending moves for us
    //Lets complete the pending moves
    //The conditional guard of the previous while loop ensures there are pending moves if we reach here
    if (!pendingMoves.isEmpty()) {  //this check is required as pendingMoves may change after the first check
      char moveToMake = pendingMoves.remove();
      char nextTile = state.getMap().get(state.getTileInFront(state.getPlayerLocation()));

      if (moveToMake == State.MOVE_GOFORWARD) {
        //If we happen to be finding a key/axe
        if (nextTile == State.TOOL_AXE) {
          needAxe = false; //no longer need axes for rest of game
        }
        else if (nextTile == State.TOOL_KEY) {
          needKey = false; //no longer need keys for rest of game
        }
        //Failsafe mechanism
        //Should never happen but here just in case
        else if ((nextTile == State.OBSTACLE_WATER && state.getNumSteppingStones() == 0) ||
          (nextTile == State.OBSTACLE_BOUNDARY)){
          //This move results in certain death, aka game over
          //So lets just do an unlock door move instead
          //Which will simply act as a NOP (no operation)
          //Hopefully then we can recover and continue making useful moves
          moveToMake = State.MOVE_UNLOCKDOOR;
        }
      }

      state.updateFromMove(moveToMake);

      return moveToMake;
    }

    return 0;
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
    AStar a = new AStar(state.getMap(), start, goal);
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
      char nextTile = state.getMap().get(state.getTileInFront(element, curDirection));
      if (nextTile == State.OBSTACLE_TREE) {
        pendingMoves.add(State.MOVE_CHOPTREE);
      } else if (nextTile == State.OBSTACLE_DOOR) {
        pendingMoves.add(State.MOVE_UNLOCKDOOR);
      }

      //Now we also need 1 forward move
      pendingMoves.add(State.MOVE_GOFORWARD);
    }
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
   * @see MoveMaker#addAStarPathToPendingMoves(Point2D.Double, Point2D.Double, int, boolean, boolean)
   */
  private boolean useSteppingStoneTowardsGoal(Point2D.Double goal) {
    boolean moveMade = false;
    List<Point2D.Double[]> solutionGroup = new ArrayList<>(); //stores all possible solutions

    for (int i = 1; i <= state.getNumSteppingStones() && !moveMade; ++i) {
      List<Point2D.Double[]> combinationList = new ArrayList<>();
      Point2D.Double[] arr = state.getWaterLocations().toArray(new Point2D.Double[state.getWaterLocations().size()]);
      getAdjacentcombinations(i, arr, combinationList); //get combinations

      for (Point2D.Double[] group : combinationList) {

        //Replace every waterTile with a temporary water block for now
        for (Point2D.Double waterTile : group) {
          state.getMap().put(waterTile, State.OBSTACLE_TEMPORARY_WATER);
        }

        //Perform a reachability test to the goal
        FloodFill ff = new FloodFill(state.getMap(), state.getPlayerLocation(), goal);
        if (ff.isReachable(state.haveKey(), state.haveAxe())) {
          //Add to solution group
          solutionGroup.add(group);
          moveMade = true;
        }

        //Restore stepping stones with original water
        for (Point2D.Double waterTile : group) {
          state.getMap().put(waterTile, State.OBSTACLE_WATER);
        }
      }
    }

    //Check for best solution based on distance to gold
    if (!solutionGroup.isEmpty()) {
      int selectedIndex = 0; //initially pick first solution (default)

      List<Point2D.Double[]> goldSolutions = new ArrayList<>();

      //Check to see if we can pick a solution that will get us closer to gold
      if (state.isGoldVisible()) {
        int minCost = 999999; //represents infinity

        for (int i = 0; i < solutionGroup.size(); ++i) {
          Point2D.Double[] group = solutionGroup.get(i);
          int groupCost = 0;

          //Pick solution closest to gold
          for (Point2D.Double solution : group) {
            //Manhattan distance to gold
            groupCost += (Math.abs((int) solution.getX() - (int) state.getGoldLocation().getX()) +
              Math.abs((int) solution.getY() - (int) state.getGoldLocation().getY()));

            //If this solution has same X/Y as gold then its a solution that will eventually lead to gold!
            if (solution.getX() == state.getGoldLocation().getX() || solution.getY() == state.getGoldLocation().getY()) {
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
      if (goldSolutions.isEmpty() && !state.getSSLocations().isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, state.getSSLocations());
        madePlan = true;
      }

      //A solution that leads to stepping stone is not possible
      //Lets find a solution that leads to keys
      if (!madePlan && !state.getKeyLocations().isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, state.getKeyLocations());
        madePlan = true;
      }

      //A solution that leads to keys is not possible
      //Lets find a solution that leads to axes
      if (!madePlan && !state.getAxeLocations().isEmpty()) {
        selectedIndex = getOptimalWaterTile(solutionGroup, state.getAxeLocations());
        madePlan = true;
      }

      //At this stage, using the stepping stone on any water tile has same effect
      //Each one has the same benefit so simply just pick any solution (ie the first one)

      //Replace every waterTile with a temporary water block
      for (Point2D.Double waterTile : solutionGroup.get(selectedIndex)) {
        state.getMap().put(waterTile, State.OBSTACLE_TEMPORARY_WATER);
      }
    }

    //We can now traverse to the goal
    addAStarPathToPendingMoves(state.getPlayerLocation(), goal, state.getDirection(), state.haveKey(), state.haveAxe());

    return moveMade;
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
        retDirection = State.LEFT;
      } else { //>0
        retDirection = State.RIGHT;
      }
    }
    else if (yDiff != 0) {
      //Either up or down
      if (yDiff < 0) {
        retDirection = State.DOWN;
      }
      else { //>0
        retDirection = State.UP;
      }
    }

    return retDirection;
  }

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
        l.add(State.MOVE_TURNLEFT);

    } else { //right moves are better
      for (int i = 0; i < numRightMoves; ++i)
        l.add(State.MOVE_TURNRIGHT);
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
  private static int mod(int n, int m) {
    int result = n % m;
    if (result < 0)
      result += m;

    return result;
  }

  /**
   * Given an arr array with date, creates combinations of depth n where each combination is a adjacent point group
   * See link for source.
   *
   * @param n depth of combinations
   * @param arr array containing data
   * @param list  empty array that is to be filled with all of the combinations
   * @see <a href="https://stackoverflow.com/a/29910788/1800854">Algorithm to get all the combinations of size n
   * from an array(by Raniz)</a>
   * @see MoveMaker#isPointGroupAdjacent(List) uses this function to filter list (memory optimization)
   */
  private void getAdjacentcombinations(int n, Point2D.Double[] arr, List<Point2D.Double[]> list) {
    // Calculate the number of arrays we should create
    long numArrays = binomial(arr.length, n); //optimization: binomial here instead of power as positions do not matter

    // Create each array
    for(int i = 0; i < numArrays; i++) {
      Point2D.Double[] current = new Point2D.Double[n];
      // Calculate the correct item for each position in the array
      for(int j = 0; j < n; j++) {
        // This is the period with which this position changes, i.e.
        // a period of 5 means the value changes every 5th array
        int period = (int) Math.pow(arr.length, n - j - 1);
        // Get the correct item and set it
        int index = i / period % arr.length;
        current[j] = arr[index];
      }

      //Check current to see if all points inside are adjacent
      //Filter non adjacent point groups
      //The only way to reach the objective is to use stepping stones on adjacent water tiles
      //This filter results in severe performance gains as we do cheap connectivity tests with no map compared to
      //more costly flood fill searches on the actual map
      if (isPointGroupAdjacent(Arrays.asList(current)))
        list.add(current);
    }
  }


  /**
   * Perform a connectivity test using a cheap connectedWalk.
   * A connected group of points is one where every point is reachable from any other point in the group.
   *
   * @param group group of points that are to be compared
   * @return true if group array is of size 1 or less or all points are connected, false otherwise
   */
  private static boolean isPointGroupAdjacent(List<Point2D.Double> group) {
    BitSet notVisited = new BitSet(group.size());
    notVisited.set(0, group.size());
    walkConnected(group, notVisited, 0);
    return notVisited.isEmpty();
  }

  /**
   * Helper function to check if a list of points are connected.
   * Utilises a bitset for performance gains (less allocations made overall)
   *
   * @param points list of points to compare
   * @param notVisited a bitset that keeps track of what has been visited and what hasn't
   * @param visit identifies what Point is currently being inspected
   */
  private static void walkConnected(List<Point2D.Double> points, BitSet notVisited, int visit) {
    notVisited.set(visit, false);
    Point2D.Double here = points.get(visit);
    for (int i = 0; i < points.size(); i++) {
      if (i != visit && notVisited.get(i)) {
        Point2D.Double other = points.get(i);
        boolean connected = false;
        if (here.x == other.x) {
          if (Math.abs(here.y - other.y) <= 1) {
            connected = true;
          }
        } else if (here.y == other.y) {
          if (Math.abs(here.x - other.x) <= 1) {
            connected = true;
          }
        }

        //Calls recursively
        if (connected) {
          walkConnected(points, notVisited, i);
        }
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