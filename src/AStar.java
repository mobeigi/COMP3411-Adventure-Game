/*
 * Using G = 1 for 4-way movement
 * Using Manhattan heuristic
 *
 * Psuedocode: https://en.wikipedia.org/wiki/A*_search_algorithm#Pseudocode
*/

import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

public class AStar {
  private Point2D.Double start, goal;
  private Map<Point2D.Double, Character> map;
  private Set<Point2D.Double> closedSet;
  private Map<Point2D.Double, Point2D.Double> cameFrom;

  private Map<Point2D.Double, Integer> gScore;
  private Map<Point2D.Double, Integer> fScore;

  private static final int INFINITY_COST = 90; //represents infinity

  public AStar(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
    this.closedSet = new HashSet<Point2D.Double>();
    this.cameFrom = new HashMap<Point2D.Double, Point2D.Double>();

    this.gScore = new HashMap<Point2D.Double, Integer>();
    this.fScore = new HashMap<Point2D.Double, Integer>();
  }

  private class PQsort implements Comparator<Point2D.Double> {
    public int compare(Point2D.Double one, Point2D.Double two) {
      //System.out.println("fscore one: " + fScore.get(one) + ", fscore two:" + fScore.get(two)); //todo: remove
      return fScore.get(one) - fScore.get(two);
    }
  }

  //Standard AStar
  public void search() {
    search(false, false);
  }

  public void search(boolean hasKey, boolean hasAxe) {
    PQsort pqs = new PQsort();
    PriorityQueue<Point2D.Double> openSet = new PriorityQueue<Point2D.Double>(10, pqs); //todo: fine tune initial size

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
          System.out.println("Current fscore: " + fScore.get(currentTile) + ", next fscore: " + fScore.get(next));
        }
      }
      */

      //Check if current tile is the goal tile
      if (currentTile.equals(this.goal)) {
        //Return here, at this stage, getPath() can be called to reconstruct the path
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

        //Check if neighbour is in closedset
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
        //This line must go after the fscore update line above so the priority queue updates correctly
        if (!openSet.contains(neighbour))
          openSet.add(neighbour);
      }
    }

    //At this point, failed to find path
  }

  private int ManhattanDistanceHeuristic(Point2D.Double start, Point2D.Double goal) {
    return Math.abs((int)start.getX() - (int)goal.getX()) + Math.abs((int)start.getY() - (int)goal.getY());
  }

  //Returns path traversed
  //Call search() before this
  public LinkedList<Point2D.Double> getPath() {
    LinkedList<Point2D.Double> sequence = new LinkedList<Point2D.Double>();
    Point2D.Double u = this.goal;

    while (this.cameFrom.get(u) != null) {
      sequence.add(u);

      u = this.cameFrom.get(u);
    }

    return sequence;
  }
}