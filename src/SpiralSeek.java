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
              if (isRevealingPoint(newTile)) {
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

      //Update dx,dy if end of spirals straight line path
      if ((x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1 - y))) {
        int tmp = dx;
        dx = -dy;
        dy = tmp;
      }

      x += dx;
      y += dy;
   }

    return start;
  }

  //This acts as a evaluation method which tells us if a point is capable of revealing
  //More information about the environment (revealing unknowns) if we traverse to it
  //Essentially, we inspect the surrounding 24 points around it (view/sight range)
  //and return true if any one of those blocks is an unknown, otherwise we return false
  private boolean isRevealingPoint(Point2D.Double point) {
    //For every surrounding block
    for (int i = 0; i < offsets.size(); ++i) {
      Point2D.Double offset = (Point2D.Double)offsets.get(i);
      Point2D.Double surroundingPoint = new Point2D.Double(point.getX() + offset.getX(), point.getY() + offset.getY());

      if (this.map.get(surroundingPoint) != null) {
        char surroundingPointType = this.map.get(surroundingPoint);

        //if this condition is true, the original point is revealing, return true
        if (surroundingPointType == State.OBSTACLE_UNKNOWN)
          return true;
      }
    }

    //Point is not revealing
    return false;
  }
}