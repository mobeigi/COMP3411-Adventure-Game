/*
 * FloodFill
 *
 * Can take in any start and goal (as Points) and will tell you if its reachable.
 * Can be filtered to treat different obstacles as passable
*/

import java.util.*;
import java.awt.geom.Point2D;

public class FloodFill {


  private Point2D.Double start, goal;
  private Map<Point2D.Double, Character> map;
  private Set<Point2D.Double> isConnected;

  public FloodFill(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
    this.isConnected = new HashSet<Point2D.Double>();
  }

  //Standard flood fill
  public boolean isReachable() {
    return isReachable(false, false);
  }

  public boolean isReachable(boolean hasKey, boolean hasAxe) {
    LinkedList<Point2D.Double> q = new LinkedList<Point2D.Double>();
    q.add(start);

    while (!q.isEmpty()) {
      Point2D.Double first = q.removeFirst();
      char tile = this.map.get(first);

      //We should convert the direction player tile to a space so the program is not confused
      if (tile == State.DIRECTION_DOWN || tile == State.DIRECTION_LEFT
        || tile == State.DIRECTION_RIGHT || tile == State.DIRECTION_UP) {
        tile = State.OBSTACLE_SPACE;
      }

      //If not processed
      if (!isConnected.contains(first)) {
        //Apply filters
        //Essentially, the list below details acceptable tiles that are passable
        //Doors/Trees are judged passable based on passed parameters
        if (!((tile == State.OBSTACLE_SPACE) ||
          (tile == State.TOOL_STEPPING_STONE_PLACED) ||
          (tile == State.TOOL_AXE) ||
          (tile == State.TOOL_KEY) ||
          (tile == State.TOOL_GOLD) ||
          (tile == State.TOOL_STEPPING_STONE) ||
          ((tile == State.OBSTACLE_DOOR) && hasKey) ||
          ((tile == State.OBSTACLE_TREE) && hasAxe)
          ))
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

    return isConnected.contains(this.goal);
  }
}