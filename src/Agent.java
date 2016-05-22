/*********************************************
 *  Agent.java 
 *  Sample Agent for Text-Based Adventure Game
 *  COMP3411 Artificial Intelligence
 *  UNSW Session 1, 2016
*/

/****** ANSWER TO QUESTION ******
 Question: Briefly describe how your program works, including any algorithms and data structures employed, and explain any design decisions you made along the way.

 My program maintains and internal State for the game as its being played. Information from views are updated in an internal map (rotation happens automatically). The player starts at (0,0) and every other point is an offset from this point. Reachability tests are done using the Flood Fill algorithm. This is a cheap alternative to A* and allows us to determine if the player can reach another block. The function takes booleans for the key and axe possestions so theoretical computations can also take place. The A* algorithm is used only once we are sure a tile is reachable. We then use it as scarcely as possible as it is expensive to travel to locations using the shortest paths. Finally a SpiralSeek algorithm is used which checks blocks ina spiral pattern originating from the current player location. This is used in the exploreation step to find new blocks to explore that will reveal new previously unknown information.

 My main design decisions were for performance and a low number of moves made. This is why my program is fairly memory intensive (although it is still reasonable memory usage). I also refactored by code so the UML it generates is very nice even though this added a little computation time to each run, it was worth the extra code clarity.

 The MoveMaker which decides what moves to make utilises a multi-stage thinking process.
 Essentially:
 1. Are they pending moves in moveQueue?
 -> Yes: Do them now

 2. Do we have Gold?
 Yes: A* to (0,0)

 3. Do we see gold?
 Yes: Do reachability test with various filters.
 If reachable with our inventory -> Do A* traversal to Gold
 Otherwise, do reachability tests with missing inventory items to see what we are needing.

 4. Do we know location of a needed resource?
 Yes: Do reachability test to each location.
 If reachable with our inventory -> Do A* traversal to location to get resource.

 5. Explore using SpiralSeek, Do we find a new tile that can reveal new information?
 Yes: A* to that point

 6. At this point there is no new information that can be gained by moving around. Is there an axe/key/stepping stone we can pick up?
 Yes: Go pick up axe/key/stepping stone

 7. At this point, we must use a stepping stone to to get to a new unreachable area
 We use a clever cost analysis function (Manhattan distance of a collection of points) on a combination
 of different points of all possible water tiles (where all tiles are adjacent/connected).

 We then try to get to a unreachable area which will give us (in order of preference):
 - Gold
 - Stepping Stones
 - Key (if we dont have it already it)
 - Axe (if we dont already have it)
 - Any blank tile (space)

 8. Disaster stage - We should never, ever get here but if we do, as a failsafe, goto: (0,0).

 Please read the javadoc comments in the source code for more information about how each component of the program works.
 */

import java.util.*;
import java.io.*;
import java.net.*;

public class Agent {

  //Class variables
  private MoveMaker movemaker;

  public Agent() {
    //Initialise our move maker which will make our moves for us
    movemaker = new MoveMaker();
  }

  public char get_action( char view[][] ) {
    return movemaker.makeMove(view);
   }

  void print_view( char view[][] )
   {
    int i,j;

    System.out.println("\n+-----+");
    for( i=0; i < 5; i++ ) {
       System.out.print("|");
       for( j=0; j < 5; j++ ) {
          if(( i == 2 )&&( j == 2 )) {
             System.out.print('^');
          }
          else {
             System.out.print( view[i][j] );
          }
       }
       System.out.println("|");
    }
    System.out.println("+-----+");
   }

   public static void main( String[] args )
   {
      InputStream in  = null;
      OutputStream out= null;
      Socket socket   = null;
      Agent  agent    = new Agent();
      char   view[][] = new char[5][5];
      char   action   = 'F';
      int port;
      int ch;
      int i,j;

      if( args.length < 2 ) {
         System.out.println("Usage: java Agent -p <port>\n");
         System.exit(-1);
      }

      port = Integer.parseInt( args[1] );

      try { // open socket to Game Engine
         socket = new Socket( "localhost", port );
         in  = socket.getInputStream();
         out = socket.getOutputStream();
      }
      catch( IOException e ) {
         System.out.println("Could not bind to port: "+port);
         System.exit(-1);
      }

      try { // scan 5-by-5 wintow around current location
         while( true ) {
            for( i=0; i < 5; i++ ) {
               for( j=0; j < 5; j++ ) {
                  if( !(( i == 2 )&&( j == 2 ))) {
                     ch = in.read();
                     if( ch == -1 ) {
                        System.exit(-1);
                     }
                     view[i][j] = (char) ch;
                  }
               }
            }
            agent.print_view( view ); // COMMENT THIS OUT BEFORE SUBMISSION
            action = agent.get_action( view );
            out.write( action );
         }
      }
      catch( IOException e ) {
         System.out.println("Lost connection to port: "+ port );
         System.exit(-1);
      }
      finally {
         try {
            socket.close();
         }
         catch( IOException e ) {}
      }
   }
}
