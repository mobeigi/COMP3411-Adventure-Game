import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

//Source: http://www.javajee.com/algorithm-dijkstras-algorithm-for-finding-shortest-path-for-a-graph
public class Dijkstra {

  private Point2D.Double start, goal;
  private Map<Point2D.Double, Integer> distance;
  private Map<Point2D.Double, Character> map;
  private Map<Point2D.Double, Point2D.Double> path;

  public Dijkstra(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
    this.distance = new HashMap<Point2D.Double, Integer>();
    this.path = new HashMap<Point2D.Double, Point2D.Double>();
  }

  static class PQsort implements Comparator<Pair<Point2D.Double, Integer>> {
    public int compare(Pair<Point2D.Double, Integer> one, Pair<Point2D.Double, Integer> two) {
      return two.second - one.second;
    }
  }

  //Returns true if goal is reachable from start
  public void search() {
    PQsort pqs = new PQsort();
    PriorityQueue<Pair<Point2D.Double, Integer>> pq = new PriorityQueue<Pair<Point2D.Double, Integer>>(10, pqs); //todo: fine tune initial size

    pq.add(new Pair<Point2D.Double, Integer>(this.start, 0)); //add start to pq

    //For every grid element
    //Todo: Can be lowered to cover 80by80 max dimensions
    for (int y = 100; y >= -100; --y) {
      for (int x = -100; x <= 100; ++x) {
        distance.put(new Point2D.Double(x,y), -1);
      }
    }

    //Start has 0 distance (to itself)
    distance.put(this.start, 0);

    while (!pq.isEmpty()) {
      Point2D.Double vTile = pq.remove().first; //remove and delete tile with minimum distance

      //for all adjacent tiles (wTile) of vTile
      for (int i = 0; i < 4; ++i) {
        int wX = (int)vTile.getX();
        int wY = (int)vTile.getY();

        switch(i) {
          case 0:
            //Tile to right
            wX += 1;
            break;
          case 1:
            //Tile to left
            wX -= 1;
            break;
          case 2:
            //Tile above
            wY += 1;
            break;
          case 3:
            //Tile below
            wY -= 1;
            break;
        }

        Point2D.Double wTile = new Point2D.Double(wX, wY);

        //If unpassable tile, ignore it as an adjacent tile
        if (this.map.get(wTile) == null) //todo: temp?
          continue;

        char tileType = this.map.get(wTile);

        //todo: much more logic needed here based on if we have key, axe...etc, etc
        if (tileType == State.OBSTACLE_WATER) {
          continue;
        }

        //Compute new distance
        //assuming weight[vTile][wTile] is always 1
        int d = this.distance.get(wTile) + 1;

        int wTileDistance = this.distance.get(wTile);

        if (wTileDistance == -1) {
          this.distance.put(wTile, d);
          pq.add(new Pair<Point2D.Double, Integer>(wTile, d));
          this.path.put(wTile, vTile);
        }
        else if (wTileDistance > d) {
          this.distance.put(wTile, d);

          //Update priority of wTile to be d
          //System.out.println("Prev size: " + pq.size()); todo temp
          pq.remove(new Pair<Point2D.Double, Integer>(wTile, wTileDistance));
          pq.add(new Pair<Point2D.Double, Integer>(wTile, d));
          //System.out.println("Post size: " + pq.size()); todo temp
          this.path.put(wTile, vTile);
        }
      }
    }
  }

  //Check to see if goal is reachable
  //Call search() before this
  public LinkedList<Point2D.Double> getPath() {
    LinkedList<Point2D.Double> sequence = new LinkedList<Point2D.Double>();
    Point2D.Double u = goal;
    boolean reached = false;

    while (this.path.get(u) != null) {
      sequence.add(u);

      if (u == start)
        reached = true;

      u = this.path.get(u);
    }

    if (reached)
      return sequence;
    else
      return sequence;
  }
}