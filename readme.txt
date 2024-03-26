Laura González Lemos   
ID: 19

Command lines needed to compile and run the practice:

-To compile:

javac -cp .;jade.jar *.java 

javac -cp .;jade.jar agents/*.java 

-To execute with 20 Random players:

java -cp .;jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:agents.RandomAgent;RandomAgent2:agents.RandomAgent;RandomAgent3:agents.RandomAgent;RandomAgent4:agents.RandomAgent;RandomAgent5:agents.RandomAgent;RandomAgent6:agents.RandomAgent;RandomAgent7:agents.RandomAgent;RandomAgent8:agents.RandomAgent;RandomAgent9:agents.RandomAgent;RandomAgent10:agents.RandomAgent;RandomAgent11:agents.RandomAgent;RandomAgent12:agents.RandomAgent;RandomAgent13:agents.RandomAgent;RandomAgent14:agents.RandomAgent;RandomAgent15:agents.RandomAgent;RandomAgent16:agents.RandomAgent;RandomAgent17:agents.RandomAgent;RandomAgent18:agents.RandomAgent;RandomAgent19:agents.RandomAgent;RandomAgent20:agents.RandomAgent"

-To execute with 15 Random players and 5 RL players:

java -cp .;jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:agents.RandomAgent;RandomAgent2:agents.RandomAgent;RandomAgent3:agents.RandomAgent;RandomAgent4:agents.RandomAgent;RandomAgent5:agents.RandomAgent;RandomAgent6:agents.RandomAgent;RandomAgent7:agents.RandomAgent;RandomAgent8:agents.RandomAgent;RandomAgent9:agents.RandomAgent;RandomAgent10:agents.RandomAgent;RandomAgent11:agents.RandomAgent;RandomAgent12:agents.RandomAgent;RandomAgent13:agents.RandomAgent;RandomAgent14:agents.RandomAgent;RandomAgent15:agents.RandomAgent;RL1:agents.RL_Agent;RL2:agents.RL_Agent;RL3:agents.RL_Agent;RL4:agents.RL_Agent;RL5:agents.RL_Agent"

-To execute with 15 Random players and 5 RL_NN players:

java -cp .;jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:agents.RandomAgent;RandomAgent2:agents.RandomAgent;RandomAgent3:agents.RandomAgent;RandomAgent4:agents.RandomAgent;RandomAgent5:agents.RandomAgent;RandomAgent6:agents.RandomAgent;RandomAgent7:agents.RandomAgent;RandomAgent8:agents.RandomAgent;RandomAgent9:agents.RandomAgent;RandomAgent10:agents.RandomAgent;RandomAgent11:agents.RandomAgent;RandomAgent12:agents.RandomAgent;RandomAgent13:agents.RandomAgent;RandomAgent14:agents.RandomAgent;RandomAgent15:agents.RandomAgent;RLNN1:agents.RL_NN_Agent;RLNN2:agents.RL_NN_Agent;RLNN3:agents.RL_NN_Agent;RLNN4:agents.RL_NN_Agent;RLNN5:agents.RL_NN_Agent"

-To execute with 5 Random players, 5 RL players and 5 RL_NN players:

java -cp .;jade.jar jade.Boot -notmp -gui -agents "MainAgent:MainAgent;RandomAgent1:agents.RandomAgent;RandomAgent2:agents.RandomAgent;RandomAgent3:agents.RandomAgent;RandomAgent4:agents.RandomAgent;RandomAgent5:agents.RandomAgent;RL1:agents.RL_Agent;RL2:agents.RL_Agent;RL13:agents.RL_Agent;RL4:agents.RL_Agent;RL5:agents.RL_Agent;RLNN1:agents.RL_NN_Agent;RLNN2:agents.RL_NN_Agent;RLNN3:agents.RL_NN_Agent;RLNN4:agents.RL_NN_Agent;RLNN5:agents.RL_NN_Agent"






I didn't do the NN_Agent, I already did the RL_NN_Agent directly.


--RL_Agent--

This agent implements an intelligent player that uses the Q-Learning algorithm to make decisions in a game.
The basic structure is the same as that of the RandomAgent, but in this case to choose its move it uses Q-Learning which is defined in the LearningTools class.

In the LearningTools class I have added the dGamma and dLearnRate variables:
dLearnRate: Learning rate that determines how much the agent adjusts its Q values ​​in each iteration.

dGamma: Discount factor that weights future rewards in calculating Q values.

In this class the vGetNewActionQLearning method is important, which implements the Q-Learning algorithm.
Calculates and adjusts Q values ​​based on rewards and previous best action and decide the next action to take, balancing between exploiting current knowledge and exploring new actions.
In this method I introduced as a state a string that contains the player's last move and that of the opponent. And as a reward I introduce the points earned in the last round by the player.
And as nActions I set 2 because in this game there are only two possibilities, 'D' or 'H'.

The variable iNewAction2Play is the chosen action by the algorithm Q-Learning.

The StateAction class encapsulates information about a specific state and the Q values ​​associated with the possible actions in that state, making it easy to track and update key information for the agent to learn.

The agent's first move is always random because it doesn't have any previously saved moves to send as a state to the LearningTools class.

As values ​​in the LearningTools class to use Q-Learning I have used numbers that I think give good results.




--RL_NN_Agent --

This agent, called RL_NN_Agent, implements a player in a game by using a SOM (Self-Organizing Map) neural network and Q-Learning.
The basic structure is the same as that of the RandomAgent, but in this case to choose its move it uses a SOM and Q-Learning which is defined in the SOM class.

The first 4 actions are random for initial exploration (random = true), and then uses the SOM neural network and Q-Learning for more informed decisions (random = false).

I give to this SOM neural network information about the last 4 plays and as a fifth element I add the points that the player wins, in order to be able to send that value to the Q-Learning method.

I enter all this information into a vector that will later be the one I send to the SOM.


The SOM class implements a self-organizing map (SOM) neural network used for decision making in a game.

Two important methods are:

sGetBMU(int[] dmInput, boolean bTrain): Finds the Best Unit Neuron (BMU) for an input vector, adjusts the neighborhood, and updates the SOM network. Returns the coordinates of the BMU.
jugada(): Returns a play decision based on information from the learning tool (LearningTools).

The string that this first method returns is what I introduce as state in the Q-learning method.

As nActions, I continue to introduce 2, and as the reward I send the fifth element of the input vector of SOM, which is the one that stores the points of the agent's last move.

The learningtools and state classes are configured the same as in the RL_Agent










