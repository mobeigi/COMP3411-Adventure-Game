import java.util.*;
import java.awt.geom.Point2D;

/**
 * SpiralSeek class.
 *
 * Contains methods to find a reachable, passable and revealing point (if possible).
 * Uses spiral algorithm which spirals outwards from any given point.
 * The algorithm is a slightly modified version of the code linked below.
 *
 * @author Mohammad Ghasembeigi
 * @version 1.4
 * @see <a href="https://stackoverflow.com/questions/398299/looping-in-a-spiral/">Stack Overflow Spiral Algorithm
 * question (answered by Can Berk Guder)</a>
 * @see <a href="https://stackoverflow.com/questions/398299/looping-in-a-spiral/10607084#10607084">Java version of
 * Spiral algorithm (by JHolta)</a>
 */
public class SpiralSeek {
  private final Point2D.Double start;
  private final Map<Point2D.Double, Character> map;

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
    new Point2D.Double(-2,-2),
    new Point2D.Double(-2,-1),
    new Point2D.Double(-2,0),
    new Point2D.Double(-2,1),
    new Point2D.Double(-2,2)
  );

  /**
   * Constructor.
   *
   * @param map the map containing information about the environment
   * @param start the starting point from which we should spiral from
   */
  public SpiralSeek(Map<Point2D.Double, Character> map, Point2D.Double start) {
    this.map = map;
    this.start = start;
  }

  /**
   * Returns a tile that will reveal new information about the environment once travelled to.
   *
   * @param hasKey  if the player has the key, is used as arguments to FloodFill for visibility checks
   * @param hasAxe  if the player has the axe, is used as arguments to FloodFill for visibility checks
   * @return  true if a reachable, passable, and revealing point is found, otherwise the 'start' point is returned
   * @see FloodFill
   */
  public Point2D.Double getTile(boolean hasKey, boolean hasAxe) {
    //Begin generating points based on current location and spiral
    int x = 0, y = 0, dx = 0, dy = -1;
    int maxX, maxY;
    maxX = maxY = (State.MAX_GRID_X + State.MAX_GRID_Y);
    int maxBlocks = State.MAX_GRID_X * State.MAX_GRID_Y + 1;

    for (int blockCount = 0; blockCount < maxBlocks;) {
      if ((-maxX / 2 <= x) && (x <= maxX / 2) && (-maxY / 2 <= y) && (y <= maxY / 2)) {
        //Create new point to inspect
        //Offset this point by the start X and start Y as the original algorithm spirals from (0,0)
        Point2D.Double newTile = new Point2D.Double(x +(int) start.getX() , y + (int) start.getY());

        ++blockCount; //this is a valid block being explored

        //Ignore this tile if its the start
        if (!newTile.equals(start)) {
          //Ensure this is a valid tile
          if (map.get(newTile) != null) {
            //Ensure tile is passable with our inventory
            char newTileType = map.get(newTile);

            if (State.isTilePassable(newTileType, hasKey, hasAxe)) {
              //Get priority
              if (isRevealingPoint(newTile)) {
                //Guaranteed to reveal unknown tiles
                //Ensure this tile is reachable
                FloodFill ff = new FloodFill(map, start, newTile);
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

  /**
   * This method determines if a point is capable of revealing more information about the environment
   * (revealing unknowns) if it is traversed to. It functions by inspecting the 24 points surrounding
   * the point (equivalent to view range) and checking to see if any of those blocks are unknown. If
   * they are then traversing to the point is guaranteed to reveal at least 1 unknown block, thus
   * giving us more information about the environment overall.
   *
   * @param point the point being tested
   * @return  true if any surrounding block is a block of type State.OBSTACLE_UNKNOWN, false otherwise
   */
  private boolean isRevealingPoint(Point2D.Double point) {
    //For every surrounding block

    for (Object obj : offsets) {
      Point2D.Double offset = (Point2D.Double)obj;
      Point2D.Double surroundingPoint = new Point2D.Double(point.getX() + offset.getX(), point.getY() + offset.getY());

      if (map.get(surroundingPoint) != null) {
        char surroundingPointType = map.get(surroundingPoint);

        //if this condition is true, the original point is revealing, return true
        if (surroundingPointType == State.OBSTACLE_UNKNOWN)
          return true;
      }
    }

    //Point is not revealing
    return false;
  }
}