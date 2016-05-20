/*
 * SpiralSeek
 *
 * Spiral method adapted from source: https://stackoverflow.com/questions/398299/looping-in-a-spiral/10607084#10607084
*/

import java.util.*;
import java.awt.geom.Point2D;

public class SpiralSeek {
  private Point2D.Double start;
  private Map<Point2D.Double, Character> map;

  //Offsets to reach 24 surrounding blocks of any block
  private static final List offsets = Arrays.asList(
    new Point2D.Double(0,-2),
    new Point2D.Double(0,-1),
    new Point2D.Double(0,1),
    new Point2D.Double(0,2),
    new Point2D.Double(1,-2),
    new Point2D.Double(1,-1),
    new Point2D.Double(1,0),
    new Point2D.Double(1,1),
    new Point2D.Double(1,2),
    new Point2D.Double(2,-2),
    new Point2D.Double(2,-1),
    new Point2D.Double(2,0),
    new Point2D.Double(2,1),
    new Point2D.Double(2,2),
    new Point2D.Double(-1,-2),
    new Point2D.Double(-1,-1),
    new Point2D.Double(-1,0),
    new Point2D.Double(-1,1),
    new Point2D.Double(-1,2),
    new Point2D.Double(2,-2),
    new Point2D.Double(2,-1),
    new Point2D.Double(2,0),
    new Point2D.Double(2,1),
    new Point2D.Double(2,2)
  );

  public SpiralSeek(Map<Point2D.Double, Character> map, Point2D.Double start) {
    this.map = map;
    this.start = start;
  }

  //Standard getTile
  public Point2D.Double getTile() {
    return getTile(false, false);
  }

  //Returns a point that is guaranteed to reveal unknown tiles when traversed to
  //and is guaranteed to be reachable.
  //If no suitable point is found, the start point is returned
  public Point2D.Double getTile(boolean hasKey, boolean hasAxe) {
    //Begin generating points based on current location and spiral
    int x = 0, y = 0, dx = 0, dy = -1;
    int maxX, maxY;
    maxX = maxY = 200; //Todo: Can be lowered to cover 80by80 max dimensions

    int maxI = Math.max(maxX, maxY) * 8; //todo: this needs to be more cleverly calculated as too low = fail

    for (int i = 0; i < maxI; i++) {
      if ((-maxX / 2 <= x) && (x <= maxX / 2) && (-maxY / 2 <= y) && (y <= maxY / 2)) {
        //Create new point to inspect
        //Offset this point by the start X and start Y as the original algorithm spirals from (0,0)
        Point2D.Double newTile = new Point2D.Double(x +(int) this.start.getX() , y + (int) this.start.getY());

        //Ignore this tile if its the start
        if (newTile != start) {
          //Ensure this is a valid tile
          if (this.map.get(newTile) != null) {
            //Ensure tile is passable with our inventory
            char newTileType = this.map.get(newTile);

            if (State.isTilePassable(newTileType, hasKey, hasAxe)) {
              //Get priority
              if (getPointPriority(newTile) >= 1) {
                //Guaranteed to reveal unknown tiles
                //Ensure this tile is reachable
                FloodFill ff = new FloodFill(this.map, start, newTile);
                if (ff.isReachable(hasKey, hasAxe)) {
                  //Guaranteed to be reachable, return it
                  return newTile;
                }
              }
            }
          }
        }
      }

      if ((x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1 - y))) {
        int tmp = dx;
        dx = -dy;
        dy = tmp;
      }

      x += dx;
      y += dy;
   }

    System.out.println("Failed spiral!");
    return start;
  }

  //This acts as a evaluation function (cost analysis)
  //Essentially, for every point we inspect the surrounding 24 points around it (view/sight range)
  //We add priority if surrounding points are unknowns (as we would like to inspect them)
  private int getPointPriority(Point2D.Double point) {
    int priority = 0; //initially zero

    //For every surrounding block
    for (int i = 0; i < offsets.size(); ++i) {
      Point2D.Double offset = (Point2D.Double)offsets.get(i);
      Point2D.Double surroundingPoint = new Point2D.Double(point.getX() + offset.getX(), point.getY() + offset.getY());

      if (this.map.get(surroundingPoint) != null) {
        char surroundingPointType = this.map.get(surroundingPoint);

        //todo: we can probably just break here, return a bool?
        if (surroundingPointType == State.OBSTACLE_UNKNOWN)
          ++priority;
      }
    }

    return priority;
  }
}