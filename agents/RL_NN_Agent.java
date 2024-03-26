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

// Clase en la que se configura el agente Neural Network el cual cada vez que juega elige su jugada utilizando la jugada que le da la red neuronal SOM. Las 4 primeras jugadas son aleatorias para poder introducirle al principio la información al SOM.
// A esta red neuronal SOM yo le doy de información las 4 últimas jugadas y de quinto elemento añado los puntos que gana el jugador, para así poder pasarle ese valor al método Q-Learning
public class RL_NN_Agent extends Agent {

    private State state;
    private AID mainAgent;
    private ACLMessage msg;

    private SOM som;
    private char pos1, pos2;
    private int res1, res2;

    boolean random; // este boolean lo utilizo para que las 4 primeras jugadas sean aleatorias y el
                    // resto utilice el SOM

    private char action;

    private ArrayList<Integer> jugadas; // lista de las últimas jugadas

    private int sumaResultadosJugador1 = 0;
    private int sumaResultadosJugador2 = 0;
    private int puntos;

    int posInt2, posInt1;

    // Method used to register in the yellow pages as a player
    protected void setup() {

        // The first state is assigned
        state = State.s0NoConfig;

        som = new SOM(10, 4); // llamamos a la clase SOM
        jugadas = new ArrayList<>();
        random = true;
        sumaResultadosJugador1 = 0;
        sumaResultadosJugador1 = 0;

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
        // The behavior is called to start playing
        addBehaviour(new Play());
        System.out.println("NN_Agent " + getAID().getName() + " is ready.");

    }

    // Method used to deregister the agent from the yellow pages
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("Error during deregistration NN Player: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("NNPlayer " + getAID().getName() + " terminating.");
    }

    // The different states in which the player could be
    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    // Clase con los distintos estados por los que pasa el agente
    private class Play extends CyclicBehaviour {

        @Override
        public void action() {
            // Waiting to receive a message
            msg = blockingReceive();
            if (msg != null) {

                // -------- Agent logic

                switch (state) {

                    // If INFORM Id#_#_,_ PROCESS SETUP --> go to state 1
                    // Else ERROR

                    case s0NoConfig:
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                // The message is validated
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

                    // If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                    // If INFORM Id#_#_,_ PROCESS SETUP --> stay at s1
                    // Else ERROR

                    case s1AwaitingGame:
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            // Game settings updated
                            if (msg.getContent().startsWith("Id#")) {
                                try {
                                    // The message is validated
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                // New game
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    // The message is validated
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
                    // If REQUEST POSITION --> INFORM POSITION --> go to state 3
                    // If INFORM CHANGED stay at state 2
                    // If INFORM ENDGAME go to state 1
                    // Else error
                    case s2Round:
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);

                            action = chooseAction();

                            msg.setContent("Action#" + action);

                            send(msg);
                            state = State.s3AwaitingResult;
                            // When the rounds of the game are over, the message with the final results
                            // arrives
                        } else if (msg.getPerformative() == ACLMessage.INFORM
                                && msg.getContent().startsWith("GameOver")) {
                            // He returns to the state where he waits for another new game or change in the
                            // game parameters
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:"
                                    + msg.getContent());
                        }
                        break;

                    case s3AwaitingResult:

                        // If INFORM RESULTS --> go to state 2
                        // Else error

                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {

                            String[] partes = msg.getContent().split("#");
                            String[] posiciones = partes[2].split(",");

                            // posiciones[0] contendrá pos1
                            // posiciones[1] contendrá pos2

                            pos1 = posiciones[0].charAt(0);
                            pos2 = posiciones[1].charAt(0);
                            // Dividir partes[3] usando ","

                            String[] resultados = partes[3].split(",");

                            // resultados[0] contendrá resultado1
                            // resultados[1] contendrá resultado2

                            res1 = Integer.parseInt(resultados[0]);
                            res2 = Integer.parseInt(resultados[1]);

                            procesarResultadosYJugadas();

                            state = State.s2Round;
                        } else {
                            // System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected
                            // message");
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

        // Método para extraer resultados y jugadas de la cadena de contenido

        // Después de extraer los valores lo añade a la lista de jugadas. También
        // proceso los puntos para la recompensa
        private void procesarResultadosYJugadas() {

            sumaResultadosJugador1 += res1;
            sumaResultadosJugador2 += res2;

            if (pos1 == 'H') {

                posInt1 = 0;

            }
            if (pos1 == 'D') {

                posInt1 = 1;
            }

            if (pos2 == 'H') {

                posInt2 = 0;

            }
            if (pos2 == 'D') {

                posInt2 = 1;
            }

            if (action == pos1) {

                jugadas.add(posInt1);
                puntos = res1;

            } else {

                jugadas.add(posInt2);
                puntos = res2;
            }

        }

        public int getSumaResultadosJugador1() {
            return sumaResultadosJugador1;
        }

        public int getSumaResultadosJugador2() {
            return sumaResultadosJugador2;
        }

        // Método para generar el vector que se va a pasar a la red neuronal SOM. Cada
        // valor del array es una jugada.

        private int[] generateInputVector() {

            int[] inputVector = new int[5];
            // Agrega la información al vector de entrada

            // Recorrer la lista y asignar cada valor al array
            for (int i = 0; i < jugadas.size(); i++) {

                inputVector[i] = jugadas.get(i);
            }

            inputVector[4] = puntos;

            jugadas.clear(); // borro la llista para poder añadirle nuevas jugadas, que ya estarán generadas
                             // por el SOM

            random = false; // cambiamos el valor de random para que las siguientes jugadas las genere el
                            // SOM

            return inputVector;

        }

        // Metodo para elegir la jugada

        private char chooseAction() {
            char[] letters = { 'D', 'H' };
            char accion;
            int accionInt = 0;

            // Las primeras jugadas aleatorias
            if (random == true && (jugadas == null || jugadas.size() < 4)) {
                accion = letters[new Random().nextInt(letters.length)];

                System.out.println("es el random");

                if (jugadas.size() == 4) {
                    random = false;
                }
            }

            else {
                int[] inputVector = generateInputVector(); // Ajusta el vector de entrada
                som.sGetBMU(inputVector, true); // Le pasamos el vector con los datos al SOM
                int bmuDecision = som.jugada(); // Desde el SOM tomamos el valor que, después de procesar todo con el
                                                // SOM y Q-Learning, queremos jugar

                random = false;
                accion = letters[bmuDecision];
            }

            return accion;

        }
    }
}
//Clase que crea la red neuronal SOM que vamos a utilizar para elegir la nueva jugada

/**
  * This class provides a SOM neural network
  *
  * @author  Juan C. Burguillo Rial
  * @version 1.0
  */
   class SOM {
    // WorldGrid.java, DlgInfoBrain.java
   
    //DECLARACIÓN DE VARIABLES
   private int iGridSide;            // Side of the SOM 2D grid
   private int iCellSize;            // Size in pixels of a SOM neuron in the grid
   private int[][] iNumTimesBMU;         // Number of times a cell has been a BMU
   private int[] iBMU_Pos = new int[2];     // BMU position in the grid
   
   private int iInputSize;               // Size of the input vector
   private int iRadio;                 // BMU radio to modify neurons
   private double dLearnRate = 1.0;          // Learning rate for this SOM
   private double dDecLearnRate = 0.999 ;      // Used to reduce the learning rate
   private double[] dBMU_Vector = null;        // BMU state
   private double[][][] dGrid;             // SOM square grid + vector state per neuron
   
   private String bmuDecision;
   
   
   private LearningTools learningTools;    //inicializamos la clase LearningTools 
   
   
   /**
    * This is the class constructor that creates the 2D SOM grid
    * 
    * @param iSideAux  the square side
    * @param iInputSizeAux  the dimensions for the input data
    * 
    */
   public SOM (int iSideAux, int iInputSizeAux) {
    iInputSize = iInputSizeAux;
    iGridSide = iSideAux;
   // iCellSize = MainWindow.iMapSize / iGridSide;
    iRadio = iGridSide / 10;
    dBMU_Vector = new double[iInputSize];
    dGrid = new double [iGridSide][iGridSide][iInputSize];
    iNumTimesBMU = new int[iGridSide][iGridSide];
    
    learningTools = new LearningTools();
    vResetValues();
   }
   
   
   // Reinicia los valores de la red SOM
   
   public void vResetValues() {
    dLearnRate = 1.0;
    iNumTimesBMU = new int[iGridSide][iGridSide];
    iBMU_Pos[0] = -1;
    iBMU_Pos[1] = -1;
    
    for (int i=0; i<iGridSide; i++)          // Initializing the SOM grid/network
     for (int j=0; j<iGridSide; j++)
      for (int k=0; k<iInputSize; k++)
       dGrid[i][j][k] = Math.random(); 
   }
   
   
   public double[] dvGetBMU_Vector() {
    return dBMU_Vector;
   }
   
   public double dGetLearnRate() {
    return dLearnRate;
   }
   
   //devuelve los pesos de la neurona en la posicion (x,y) de la cuadricula SOM
   public double[] dGetNeuronWeights (int x, int y) {
    return dGrid[x][y];
   }
   
   public String getBMUDecision(){
       return bmuDecision;
   }
   
   public int jugada(){
   
     int jug = learningTools.iNewAction2Play;
   
     return jug;
   }
   
   //este es el metodo principal que encuentra la mejor bmu para un vector de entrada, ajusta el vecindario y actualiza la red SOM. Dependiendo del valor de 'bTrain', puede realizar o no la fase de entrenamiento.
   
   /**
    * This is the main method that returns the coordinates of the BMU and trains its neighbors
    * 
    * @param dmInput  contains the input vector
    * @param bTrain  training or testing phases
    * 
    */
   public String sGetBMU (int[] dmInput, boolean bTrain)
     {
       int x=0, y=0;
       double dNorm, dNormMin = Double.MAX_VALUE;
       String sReturn;
       
       
       // if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
       //  System.out.print ("\n\n\n\n-------------------- SOM -------------------\ndmInput: \t");
       //  for (int k=0; k<iInputSize; k++) 
       //    System.out.print ("  " + String.format (Locale.ENGLISH, "%.5f", dmInput[k]) );
       // }
      
       
       for (int i=0; i<iGridSide; i++)            // Finding the BMU
        for (int j=0; j<iGridSide; j++) {
         dNorm = 0;
         for (int k=0; k<iInputSize; k++)           // Calculating the norm
   
         if(k !=4){                                //Descartamos el k=4 porque ahí no va una jugada, van los puntos del jugador para mandárselos a la clase LearningTools
          dNorm += (dmInput[k] - dGrid[i][j][k]) * ((dmInput[k] - dGrid[i][j][k]));
         
         if (dNorm < dNormMin) {
          dNormMin = dNorm; 
          x = i;
          y = j;
         }
       }
        }                       // Leaving the loop with the x,y positions for the BMU
       
       
       // if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
       //  System.out.print ("\ndBMU_pre: \t");
       //  for (int k=0; k<iInputSize; k++) 
       //    System.out.print ("  " + String.format (Locale.ENGLISH, "%.5f", dGrid[x][y][k]) );
       // }
       
      
       if (bTrain) {
        int xAux=0;
        int yAux=0;
        for (int v=-iRadio; v<=iRadio; v++)       // Adjusting the neighborhood
         for (int h=-iRadio; h<=iRadio; h++) {
          xAux = x+h;
          yAux = y+v;
          
          if (xAux < 0)                // Assuming a torus world
           xAux += iGridSide;
          else if (xAux >= iGridSide)
           xAux -= iGridSide;
       
          if (yAux < 0)
           yAux += iGridSide;
          else if (yAux >= iGridSide)
           yAux -= iGridSide;
       
          for (int k=0; k<iInputSize; k++)
           dGrid[xAux][yAux][k] += dLearnRate * (dmInput[k] - dGrid[xAux][yAux][k]) / (1 + v*v + h*h);
        }
        
       //  if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
       //   System.out.print ("\ndBMU_post: \t");
       //   for (int k=0; k<iInputSize; k++) 
       //     System.out.print ("  " + String.format (Locale.ENGLISH, "%.5f", dGrid[x][y][k]) );
       //  }
      
       }
      
       
       sReturn = "" + x + "," + y;
       iBMU_Pos[0] = x;
       iBMU_Pos[1] = y;
       dBMU_Vector = dGrid[x][y].clone();
       iNumTimesBMU[x][y]++;
       dLearnRate *= dDecLearnRate;
       
       // le pasamos al método que implementa de Q-Learning de estado el String de las coordenadas y de recompensa los últimos puntos ganados por el jugador
       learningTools.vGetNewActionQLearning(sReturn, 2, dmInput[4]);
       return sReturn;
   
   
   
    }
   
   //Clase que implementa Q-Learnig.
   
   /**
     * This is a basic class with some learning tools: statistical learning, learning automata (LA) and Q-Learning (QL)
     *
     * @author  Juan C. Burguillo Rial
     * @version 2.0
     */
     class LearningTools
     {
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
       *  1. I start with the last action a, the previous state s and find the actual state s'
       *  2. Select the new action with Qmax{a'}
       *  3. Adjust:   Q(s,a) = Q(s,a) + dLearnRateLR [R + dGamma . Qmax{a'}(s',a') - Q(s,a)]
       *  4. Select the new action by a epsilon-greedy methodology
       *
       * @param sState    contains the present state
      * @param iNActions contains the number of actions that can be applied in this
      *                  state
      * @param dReward   is the reward obtained after performing the last action.
      */
   
      // En mi caso, yo le paso como estado la cadena con las coordenadas de la neurona y
     // como recompensa le paso los puntos que sacó el jugador en esa jugada.
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
   
   }}
   
