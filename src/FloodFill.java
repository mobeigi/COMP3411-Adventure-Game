import java.util.*;
import java.awt.geom.Point2D;

/**
 * FloodFill class.
 *
 * Implements the FloodFill algorithm to do quick (low computation time) and easy reachability tests.
 * Based on Flood Fill pseudocode from Wikipedia (see link).
 * Alternative non-recursive implementation was selected to avoid heap space issues in Java runtime.
 *
 * @author Mohammad Ghasembeigi
 * @version 1.0
 * @see <a href="https://en.wikipedia.org/wiki/Flood_fill#Alternative_implementations">Wikipedia - Flood Fill
 * Pseudocode Alternative implementation</a>
 */
public class FloodFill {

  private final Point2D.Double start, goal;
  private final Map<Point2D.Double, Character> map;

  public FloodFill(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
  }

  /**
   * Performs a FloodFill reachable test on the map environment from start to goal.
   *
   * @param hasKey if the player has the key, is used as arguments to isTilePassable to determine if we can pass
   *               through doors
   * @param hasAxe if the player has the axe, is used as arguments to isTilePassable to determine if we can pass
   *               through trees
   * @return true if goal point is reachable from start point, false otherwise
   */
  public boolean isReachable(boolean hasKey, boolean hasAxe) {
    Queue<Point2D.Double> q = new ArrayDeque<>();
    Set<Point2D.Double> isConnected = new HashSet<>();

    q.add(start);

    while (!q.isEmpty()) {
      Point2D.Double first = q.remove();

      if (map.get(first) == null) //sanity check
        continue;

      char tile = map.get(first);

      //If not processed
      if (!isConnected.contains(first)) {
        //Apply filters
        //Non-passable tiles are ignored
        if (!State.isTilePassable(tile, hasKey, hasAxe))
          continue; //this tile is not passable

        //Mark first as processed
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

    return isConnected.contains(goal);
  }
}