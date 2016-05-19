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

  private static final int INFINITY_COST = 99999;

  public AStar(Map<Point2D.Double, Character> map, Point2D.Double start, Point2D.Double goal) {
    this.map = map;
    this.start = start;
    this.goal = goal;
    this.closedSet = new HashSet<Point2D.Double>();
    this.cameFrom = new HashMap<Point2D.Double, Point2D.Double>();

    this.gScore = new HashMap<Point2D.Double, Integer>();
    this.fScore = new HashMap<Point2D.Double, Integer>();
  }

  class PQsort implements Comparator<Point2D.Double> {
    public int compare(Point2D.Double one, Point2D.Double two) {
      if (fScore.get(two) == null || fScore.get(one) == null) {
        System.out.println(one.getX());
        System.out.println(one.getY());
        System.out.println(two.getX());
        System.out.println(two.getY());
        System.out.println("THEY ARE NULL!!");
      }

      return fScore.get(two)- fScore.get(one);
    }
  }

  public void search() {
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

    openSet.add(this.start); //add start to pq
    fScore.put(this.start, ManhattenDistanceHeuristic(start, goal));

    while (!openSet.isEmpty()) {
      Point2D.Double currentTile = openSet.remove();

      Boolean b = (currentTile == this.goal);
      String str3 = b.toString();
      System.out.println("Current Tile: ("+ currentTile.getX() + "," + currentTile.getY() +"), matchesGoal:" + str3 );

      //Check if current tile is the goal tile
      //todo: (currentTile == this.goal) comparison does not work! why?
      if (currentTile.getX() == goal.getX() && currentTile.getY() == goal.getY()) {
        //todo reconstruct path
        System.out.println("WE FOUND THE PATH!!!");
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

        if (!((tile == State.OBSTACLE_SPACE) ||
          (tile == State.TOOL_STEPPING_STONE_PLACED) ||
          (tile == State.TOOL_AXE) ||
          (tile == State.TOOL_KEY) ||
          (tile == State.TOOL_GOLD) ||
          (tile == State.TOOL_STEPPING_STONE) ||
          (tile == State.OBSTACLE_DOOR) ||
          (tile == State.OBSTACLE_TREE)
        ))
          continue; //this tile is not passable

        //Calculate distance from start to a neighbour
        int tentative_gScore = gScore.get(currentTile) + 1; //distance betweencurrent and neighbour is always 1

        if (!openSet.contains(neighbour)) //explore new neighbour
          openSet.add(neighbour);
        else if (tentative_gScore >= gScore.get(neighbour))
          continue; //this is not a better path

        //Otherwise, this path is the best so far, record it
        cameFrom.put(neighbour, currentTile);
        gScore.put(neighbour, tentative_gScore);
        fScore.put(neighbour, tentative_gScore + ManhattenDistanceHeuristic(neighbour, goal));
      }
    }

    //At this point, failed to find path
  }

  private int ManhattenDistanceHeuristic(Point2D.Double start, Point2D.Double goal) {
    return Math.abs((int)start.getX() - (int)goal.getX()) + Math.abs((int)start.getY() - (int)goal.getY());
  }
}