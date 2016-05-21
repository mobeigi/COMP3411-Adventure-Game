import java.util.*;
import java.awt.geom.Point2D;

/**
 * AStar class.
 *
 * Capable of performing the A* algorithm on a 2D-grid given a map, a start point and a goal (destination) point.
 * The G cost for each adjacent move (4-way movement) is 1.
 * The heuristic selected is the Manhattan distance heuristic (ideal for our game scenario).
 * Code based on Wikipedia A* algorithm pseudocode (see link below).
 *
 * @author Mohammad Ghasembeigi
 * @version 1.2
 * @see <a href="https://en.wikipedia.org/wiki/A*_search_algorithm#Pseudocode">Wikipedia - A* Search Algorithm
 * Pseudocode</a>
 */
public class AStar {
  private final Point2D.Double start, goal;
  private final Map<Point2D.Double, Character> map;
  private Map<Point2D.Double, Point2D.Double> cameFrom;

  private Map<Point2D.Double, Integer> gScore;
  private Map<Point2D.Double, Integer> fScore;

  private boolean searchCompleted;

  private static final int INFINITY_COST = 999999; //represents an infinite value

  /**
   *  Constructor.
   *
   * @param map the map containing information about the environment
   * @param start the starting point we begin to search from
   * @param goal  the goal point which we will try to find the shortest path to
   */
  public AStar(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
    this.cameFrom = new HashMap<>();

    this.gScore = new HashMap<>();
    this.fScore = new HashMap<>();

    this.searchCompleted = false;
  }

  /**
   * Implements method compare in java.util.comparator
   *
   * Sorts points based on their fScore. Points that have a lower fScore have lower cos
   * and come earlier/before (as they have a higher priority)
   */
  private class FScoreSort implements Comparator<Point2D.Double> {
    @Override
    public int compare(Point2D.Double one, Point2D.Double two) {
      return fScore.get(one) - fScore.get(two);
    }
  }

  /**
   * Performs an A* search on the map environment from start to goal and fills 'cameFrom' with path information
   * to be reconstructed later.
   *
   * @param hasKey if the player has the key, is used as arguments to isTilePassable to determine if we can pass
   *               through doors
   * @param hasAxe if the player has the axe, is used as arguments to isTilePassable to determine if we can pass
   *               through trees
   */
  public void search(boolean hasKey, boolean hasAxe) {
    FScoreSort fss = new FScoreSort();
    PriorityQueue<Point2D.Double> openSet = new PriorityQueue<>(10, fss); //todo: fine tune initial size

    Set<Point2D.Double> closedSet  = new HashSet<>();

    //For every grid element
    //Todo: Can be lowered to cover 80by80 max dimensions
    for (int y = 100; y >= -100; --y) {
      for (int x = -100; x <= 100; ++x) {
        gScore.put(new Point2D.Double(x,y), INFINITY_COST);
        fScore.put(new Point2D.Double(x,y), INFINITY_COST);
      }
    }

    gScore.put(this.start, 0);

    fScore.put(this.start, ManhattanDistanceHeuristic(start, goal));
    openSet.add(this.start); //add start to pq

    while (!openSet.isEmpty()) {
      Point2D.Double currentTile = openSet.remove();

      /*
      //todo: remove, used to debug priority queue proper ordering
      Point2D.Double next = openSet.peek();
      if (next != null) {
        if (fScore.get(currentTile) > fScore.get(next)) {
          System.out.println("ERROR: This should not happen! AStar priority mismatch");
          System.out.println("Current fScore: " + fScore.get(currentTile) + ", next fScore: " + fScore.get(next));
        }
      }
      */

      //Check if current tile is the goal tile
      if (currentTile.equals(this.goal)) {
        //Return here, at this stage, getPath() can be called to reconstruct the path
        searchCompleted = true;
        return;
      }

      openSet.remove(currentTile);
      closedSet.add(currentTile);

      //For each adjacent tile of currentTile (neighbours)
      for (int i = 0; i < 4; ++i) {
        int neighbourX = (int) currentTile.getX();
        int neighbourY = (int) currentTile.getY();

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

        //Check if neighbour is in closedSet
        if (closedSet.contains(neighbour))
          continue;

        //Check if neighbour tile is passable
        char tile = this.map.get(neighbour);

        if (!State.isTilePassable(tile, hasKey, hasAxe))
          continue; //this tile is not passable

        //Calculate distance from start to a neighbour
        int tentative_gScore = gScore.get(currentTile) + 1; //distance between current and neighbour is always 1

        //this is not a better path, ignore it
        if (tentative_gScore >= gScore.get(neighbour))
          continue; //this is not a better path

        //Otherwise, this path is the best so far, record it
        cameFrom.put(neighbour, currentTile);
        gScore.put(neighbour, tentative_gScore);
        fScore.put(neighbour, tentative_gScore + ManhattanDistanceHeuristic(neighbour, goal));

        //Explore this new neighbour
        //This line must go after the fScore update line above so the priority queue updates correctly
        if (!openSet.contains(neighbour))
          openSet.add(neighbour);
      }
    }

    //At this point, failed to find a path and the search is over
    searchCompleted = true;
  }

  /**
   *  Computes the manhattan distance formula for two points: start and goal
   *
   * @param start starting point to be used in the manhattan distance calculation
   * @param goal  goal point to be used in the manhattan distance calculation
   * @return  the heuristic result (cost) of traversing from start to goal (guaranteed to be admissible)
   */
  private int ManhattanDistanceHeuristic(Point2D.Double start, Point2D.Double goal) {
    return Math.abs((int)start.getX() - (int)goal.getX()) + Math.abs((int)start.getY() - (int)goal.getY());
  }

  /**
   * Returns minimum path from start to goal as determined in search() or empty linked list if no path was found.
   * Precondition: a call to search() has been made before this method is called
   *
   * @return LinkedList of Point2D.Double objects that form a path from goal to start (excluding start point) which
   *         should be reversed before being used. Otherwise, empty LinkedList meaning no path was found.
   * @throws IllegalStateException if search() is not called before this method is called
   */
  public LinkedList<Point2D.Double> getPath() {

    if (!searchCompleted)
      throw new IllegalStateException("search() has not been called yet");

    LinkedList<Point2D.Double> sequence = new LinkedList<>();
    Point2D.Double u = this.goal;

    while (this.cameFrom.get(u) != null) {
      sequence.add(u);
      u = this.cameFrom.get(u);
    }

    return sequence;
  }
}