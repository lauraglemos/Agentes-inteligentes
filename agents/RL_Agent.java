package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.*;
import java.io.*;

// Clase en la que se configura el agente Reinforcement Learning el cual cada vez que juega elige su jugada utilizando Q-Learning 
public class RL_Agent extends Agent {

  private State state;
  private AID mainAgent;
  private ACLMessage msg;

  private LearningTools learningTools;

  int puntos; // recompensa que después se va a pasar a la clase LearningTools
  char accion; // accion que va a realizar el jugador
  String estado; // estado que se le va a pasar al jugador

  protected void setup() {

    // The first state is assigned
    state = State.s0NoConfig;

    learningTools = new LearningTools(); // llamamos a la clase LearningTools

    // Method used to register in the yellow pages as a player

    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("Player");
    sd.setName("agents");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    } catch (FIPAException fe) {
      fe.printStackTrace();
    }

    addBehaviour(new Play());
    // System.out.println("RL_Agent " + getAID().getName() + " is ready.");

  }

  // Method used to deregister the agent from the yellow pages
  protected void takeDown() {
    try {
      DFService.deregister(this);
    } catch (FIPAException e) {
      System.err.println("Error during deregistration RL_Agent: " + e.getMessage());
      e.printStackTrace();
    }
    // System.out.println("RL_Player " + getAID().getName() + " terminating.");
  }

  // The different states in which the player could be
  private enum State {
    s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
  }

  // Clase con los distintos estados por los que pasa el agente

  private class Play extends CyclicBehaviour {

    char[] letters = { 'D', 'H' };

    @Override
    public void action() {

      // System.out.println(getAID().getName() + ":" + state.name());

      msg = blockingReceive();
      if (msg != null) {

        // -------- Agent logic

        switch (state) {

          case s0NoConfig:

            // If INFORM Id#_#_,_ PROCESS SETUP --> go to state 1
            // Else ERROR
            if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
              boolean parametersUpdated = false;
              try {

                parametersUpdated = validateSetupMessage(msg);
              } catch (NumberFormatException e) {
                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
              }
              if (parametersUpdated)
                state = State.s1AwaitingGame;

            } else {
              System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
            }
            break;

          case s1AwaitingGame:
            // If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
            // If INFORM Id#_#_,_ PROCESS SETUP --> stay at s1
            // Else ERROR

            if (msg.getPerformative() == ACLMessage.INFORM) {

              if (msg.getContent().startsWith("Id#")) {
                try {

                  validateSetupMessage(msg);
                } catch (NumberFormatException e) {
                  System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                }
                // New game
              } else if (msg.getContent().startsWith("NewGame#")) {
                boolean gameStarted = false;
                try {

                  gameStarted = validateNewGame(msg.getContent());
                } catch (NumberFormatException e) {
                  System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                }
                if (gameStarted)
                  state = State.s2Round;
              }
            } else {
              System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
            }
            break;

          case s2Round:
            // If REQUEST POSITION --> INFORM POSITION --> go to state 3
            // If INFORM CHANGED stay at state 2
            // If INFORM ENDGAME go to state 1
            // Else error

            if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {

              if (estado != null) {

                learningTools.vGetNewActionQLearning(estado, 2, puntos);

                int selectedActionIndex = learningTools.iNewAction2Play;

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(mainAgent);

                accion = letters[selectedActionIndex];
                msg.setContent("Action#" + accion);
                send(msg);
              }

              else {

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(mainAgent);
                accion = letters[new Random().nextInt(letters.length)];
                msg.setContent("Action#" + accion);
                send(msg);
              }

              System.out.println(getAID().getName() + " sent " + msg.getContent());

              state = State.s3AwaitingResult;
            } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
              // Process changed message, in this case nothing
            } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver#")) {
              state = State.s1AwaitingGame;
            } else {
              System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
            }
            break;
          case s3AwaitingResult:
            // If INFORM RESULTS --> go to state 2
            // Else error

            if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {

              // Procesado de los resultados

              String[] partes = msg.getContent().split("#");
              String[] posiciones = partes[2].split(",");

              // posiciones[0] contendrá pos1
              // posiciones[1] contendrá pos2

              char pos1 = posiciones[0].charAt(0);
              char pos2 = posiciones[1].charAt(0);
              // Dividir partes[3] usando ","

              String[] resultados = partes[3].split(",");

              // resultados[0] contendrá resultado1
              // resultados[1] contendrá resultado2

              int res1 = Integer.parseInt(resultados[0]);
              int res2 = Integer.parseInt(resultados[1]);

              if (accion == pos1) {

                puntos = res1;

                estado = String.valueOf(pos1) + String.valueOf(pos2);

              }

              if (accion == pos2) {

                puntos = res2;

                estado = String.valueOf(pos2) + String.valueOf(pos1);

              }

              state = State.s2Round;
            } else {
              System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
            }
            break;

        }
      }

    }

    /**
     * Validates the setup message
     * 
     * @param msg ACLMessage to process
     * @return true on success, false on failure
     */
    private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
      String msgContent = msg.getContent();

      String[] contentSplit = msgContent.split("#");
      if (contentSplit.length != 3)
        return false;
      if (!contentSplit[0].equals("Id"))
        return false;

      String[] parametersSplit = contentSplit[2].split(",");
      if (parametersSplit.length != 2)
        return false;

      // The AID of the Main Agents is obtained
      mainAgent = msg.getSender();

      return true;
    }

    /**
     * Validates the New Game message
     * 
     * @param msgContent Content of the message
     * @return true if the message is valid
     */
    public boolean validateNewGame(String msgContent) {
      String[] contentSplit = msgContent.split("#");
      if (contentSplit.length != 3)
        return false;
      if (!contentSplit[0].equals("NewGame")) {
        return false;
      } else {
        return true;
      }
    }
  }
}

//Clase que implementa Q-Learnig.
/**
 * This is a basic class with some learning tools: statistical learning,
 * learning automata (LA) and Q-Learning (QL)
 *
 * @author Juan C. Burguillo Rial
 * @version 2.0
 */
class LearningTools {
  final double dDecFactorLR = 0.99; // Value that will decrement the learning rate in each generation
  final double dEpsilon = 0.95; // Used to avoid selecting always the best action
  final double dMINLearnRate = 0.05; // We keep learning, after convergence, during 5% of times
  final double dGamma = 0.95;
  double dLearnRate = 0.95;

  // los valores de dLearnRate y dGamma dan un resultado bastante bueno, por eso
  // he dejado eses valores.

  boolean bAllActions = false; // At the beginning we did not try all actions
  int iNewAction2Play; // This is the new action to be played
  int iNumActions = 2; // For H or D for instance
  int iLastAction; // The last action that has been played by this player
  int[] iNumTimesAction = new int[iNumActions]; // Number of times an action has been played
  double[] dPayoffAction = new double[iNumActions]; // Accumulated payoff obtained by the different actions
  StateAction oLastStateAction;
  StateAction oPresentStateAction;
  Vector<StateAction> oVStateActions = new Vector<>();; // A vector containing strings with the possible States and
                                                        // Actions available at each one

  /**
   * This method is used to implement Q-Learning:
   * 1. I start with the last action a, the previous state s and find the actual
   * state s'
   * 2. Select the new action with Qmax{a'}
   * 3. Adjust: Q(s,a) = Q(s,a) + dLearnRateLR [R + dGamma . Qmax{a'}(s',a') -
   * Q(s,a)]
   * 4. Select the new action by a epsilon-greedy methodology
   *
   * @param sState    contains the present state
   * @param iNActions contains the number of actions that can be applied in this
   *                  state
   * @param dReward   is the reward obtained after performing the last action.
   */

  // En mi caso, yo le paso como estado su última jugada y la del otro jugador y
  // como recompensa le paso los puntos que sacó en esa jugada.
  // iNActions es igual a 2 siempre porque sólo hay dos posibilidades para jugar,
  // D o H

  public void vGetNewActionQLearning(String sState, int iNActions, int dReward) {
    boolean bFound;
    int iBest = -1, iNumBest = 1;
    double dR, dQmax;
    StateAction oStateAction;

    bFound = false; // Searching if we already have the state
    // if(oVStateActions != null){
    for (int i = 0; i < oVStateActions.size(); i++) {
      oStateAction = (StateAction) oVStateActions.elementAt(i);
      if (oStateAction.sState.equals(sState)) {
        oPresentStateAction = oStateAction;
        bFound = true;
        break;
      }
      // }
    }

    // If we didn't find it, then we add it
    if (!bFound) {
      oPresentStateAction = new StateAction(sState, iNActions);
      oVStateActions.add(oPresentStateAction);
    }

    dQmax = 0;
    for (int i = 0; i < iNActions; i++) { // Determining the action to get Qmax{a'}
      if (oPresentStateAction.dValAction[i] > dQmax) {
        iBest = i;
        iNumBest = 1; // Reseting the number of best actions
        dQmax = oPresentStateAction.dValAction[i];
      } else if ((oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0)) { // If there is another one equal we must
                                                                                // select one of them randomly
        iNumBest++;
        if (Math.random() < 1.0 / (double) iNumBest) { // Choose randomly with reducing probabilities
          iBest = i;
          dQmax = oPresentStateAction.dValAction[i];
        }
      }
    }
    // Adjusting Q(s,a) using the formula
    if (oLastStateAction != null)
      oLastStateAction.dValAction[iLastAction] += dLearnRate
          * (dReward + dGamma * dQmax - oLastStateAction.dValAction[iLastAction]);

    if ((iBest > -1) && (Math.random() > dEpsilon)) // Using the e-greedy policy to select the best action or any of the
                                                    // rest
      iNewAction2Play = iBest;
    else
      do {
        iNewAction2Play = (int) (Math.random() * (double) iNumActions);
      } while (iNewAction2Play == iBest);

    oLastStateAction = oPresentStateAction; // Updating values for the next time
    dLearnRate *= dDecFactorLR; // Reducing the learning rate
    if (dLearnRate < dMINLearnRate)
      dLearnRate = dMINLearnRate;
  }

} // from class LearningTools

/**
 * This is the basic class to store Q values (or probabilities) and actions for
 * a certain state
 *
 * @author Juan C. Burguillo Rial
 * @version 2.0
 */

class StateAction implements Serializable {
  String sState;
  double[] dValAction;

  StateAction(String sAuxState, int iNActions) {
    sState = sAuxState;
    dValAction = new double[iNActions];
  }

  StateAction(String sAuxState, int iNActions, boolean bLA) {
    this(sAuxState, iNActions);
    if (bLA)
      for (int i = 0; i < iNActions; i++) // This constructor is used for LA and sets up initial probabilities
        dValAction[i] = 1.0 / iNActions;
  }

  public String sGetState() {
    return sState;
  }

  public double dGetQAction(int i) {
    return dValAction[i];
  }
}
