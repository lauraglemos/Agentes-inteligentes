
package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

// Clase en la que se configura el agente Random el cual cada vez que juega lo hace con un valor aleatorio.

public class RandomAgent extends Agent {

    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private int N, S, R, I, P;
    private ACLMessage msg;

    protected void setup() {
        state = State.s0NoConfig;

        // Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());
        // System.out.println("RandomAgent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        // System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    // Clase con los distintos estados por los que pasa el agente
    private class Play extends CyclicBehaviour {
        Random random = new Random();
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
                                // System.out.println(getAID().getName() + ":" + state.name() + " - Bad
                                // message");
                            }
                            if (parametersUpdated)
                                state = State.s1AwaitingGame;

                        } else {
                            // System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected
                            // message");
                        }
                        break;
                    case s1AwaitingGame:
                        // If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        // If INFORM Id#_#_,_ PROCESS SETUP --> stay at s1
                        // Else ERROR

                        if (msg.getPerformative() == ACLMessage.INFORM) {

                            if (msg.getContent().startsWith("Id#")) { // Game settings d

                                try {
                                    validateSetupMessage(msg);

                                } catch (NumberFormatException e) {
                                    // System.out.println(getAID().getName() + ":" + state.name() + " - Bad
                                    // message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;

                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                } catch (NumberFormatException e) {
                                    // System.out.println(getAID().getName() + ":" + state.name() + " - Bad
                                    // message");
                                }

                                if (gameStarted)
                                    state = State.s2Round;
                                else
                                    state = State.s1AwaitingGame;

                            }
                        } else {
                            // System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected
                            // message");
                        }
                        break;
                    case s2Round:
                        // If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        // If INFORM CHANGED stay at state 2
                        // If INFORM ENDGAME go to state 1
                        // Else error

                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);
                            int randomIndex = random.nextInt(letters.length);
                            msg.setContent("Action#" + letters[randomIndex]);
                            // System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;
                        } else if (msg.getPerformative() == ACLMessage.INFORM
                                && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM
                                && msg.getContent().startsWith("GameOver#")) {
                            state = State.s1AwaitingGame;

                        } else {
                            // System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected
                            // message:" + msg.getContent());
                        }
                        break;

                    case s3AwaitingResult:
                        // If INFORM RESULTS --> go to state 2
                        // Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            // Process results
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
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tS, tR, tI, tP, tMyId;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3)
                return false;
            if (!contentSplit[0].equals("Id"))
                return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 2)
                return false;
            tN = Integer.parseInt(parametersSplit[0]);
            // tS = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[1]);
            // tI = Integer.parseInt(parametersSplit[3]);
            // tP = Integer.parseInt(parametersSplit[4]);

            // At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            // S = tS;
            R = tR;
            // I = tI;
            // P = tP;
            myId = tMyId;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * 
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3)
                return false;
            if (!contentSplit[0].equals("NewGame"))
                return false;
            String[] idSplit = contentSplit[1].split("#");
            if (idSplit.length != 1)
                return false;
            msgId0 = Integer.parseInt(contentSplit[1]);
            msgId1 = Integer.parseInt(contentSplit[2]);

            if (myId == msgId0) {
                opponentId = msgId1;
                return true;
            } else if (myId == msgId1) {
                opponentId = msgId0;
                return true;
            }
            return false;
        }
    }
}
