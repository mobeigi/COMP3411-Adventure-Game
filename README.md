COMP3411 Adventure Game
=========
For this project I implemented an agent to play a simple text-based adventure game. The agent is required to move around a rectangular environment, collecting tools and avoiding (or removing) obstacles along the way. The obstacles and tools within the environment are represented as follows:

|Obstacles|Tools|
|--------|--------|
|T 	tree|a 	axe|
|-	door|k	key|
|~	water|o	stepping stone|
|*	wall|g	gold|

The agent will be represented by one of the characters ^, v, <  or  >, depending on which direction it is pointing. The agent is capable of the following instructions:

L   turn left
R   turn right
F   (try to) move forward
C   (try to) chop down a tree, using an axe
U   (try to) unlock a door, using a key
When it executes an L or R instruction, the agent remains in the same location and only its direction changes. When it executes an F instruction, the agent attempts to move a single step in whichever direction it is pointing. The F instruction will fail (have no effect) if there is a wall, tree or door directly in front of the agent.

When the agent moves to a location occupied by a tool, it automatically picks up the tool. The agent may use a C or U instruction to remove an obstacle immediately in front of it, if it is carrying the appropriate tool. A tree may be removed with a C (chop) instruction, if an axe is held. A door may be removed with a U (unlock) instruction, if a key is held.

If the agent moves forward into the water it will drown unless it is holding a stepping stone, in which case the stone will automatically be placed in the water and the agent can step onto it safely. When the agent steps away, the stone will appear as an upper-case O. The agent can walk again on that stone, but the stone will stay where it is and can never be picked up again.

If the agent attempts to move off the edge of the environment, it dies.

To win the game, the agent must pick up the gold and then return to its initial location.

Capabilities
----
This solution should be able to solve any sample file as long as a solution exists and there is enough information that can be gathered to make an educated move (so solutions that require luck when using stepping stones cannot be solved).

See the sample folder for a series of sample environments that can be solved (s0.in - s9.in).

Demo
----
This is a demo of solving sample **s0.in**.  
A solution is found in **140** moves.  
Note that a delay of 0.25ms has been added between each move for illustrative purposes.

![S0 Demo](/../screenshots/screenshots/s0-demo.gif?raw=true "S0 Demo")

License
----
GNU General Public License v3.0
