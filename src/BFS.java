/*
import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

public class BFS {
  static int r,c;
  private static Point2D.Double start;
  private static Point2D.Double goal;
  static int[] dx={1,-1,0,0};//right, left, NA, NA
  static int[] dy={0,0,1,-1};//NA, NA, bottom, top

  public BFS(Point2D.Double start, Point2D.Double goal) {
    this.start = start;
    this.goal = goal;
  }

  //Returns true if goal is reachable from start
  public boolean search()
  {
    if(start.getX() == goal.getX() && start.getY() == goal.getY())
      return true;
    else
    {
      //grid [f1][f2]='G';//finish
      Queue<int[]> q = new LinkedList<int[]>();
      int[] start={ (int)this.start.getX(), (int)this.start.getY()}; //Start Coordinates
      q.add(start); //Adding start to the queue since we're already visiting it
      //grid[s1][s2]='B';

      while(q.peek() != null)
      {
        int[] curr = q.poll();//poll or remove. Same thing
        for(int i=0;i<4;i++)//for each direction
        {
          if((curr[0]+dx[i]>=0&&curr[0]+dx[i]<r)&&(curr[1]+dy[i]>=0&&curr[1]+dy[i]<c))
          {
            //Checked if x and y are correct. ALL IN 1 GO
            int xc=curr[0]+dx[i];//Setting current x coordinate
            int yc=curr[1]+dy[i];//Setting current y coordinate
            if(grid[xc][yc]=='G')//Destination found
            {
              //System.out.println(xc+" "+yc);
              return true;
            }
            else if(grid[xc][yc]=='E')//Movable. Can't return here again so setting it to 'B' now
            {
              //System.out.println(xc+" "+yc);
              grid[xc][yc]='B';//now BLOCKED
              int[]temp={xc,yc};
              q.add(temp);//Adding current coordinates to the queue
            }
          }
        }
      }
      return false;//Will return false if no route possible
    }
  }

}

*/