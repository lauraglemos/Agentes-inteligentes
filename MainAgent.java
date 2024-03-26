
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import java.util.concurrent.Semaphore;

// Clase que configura el agente principal

public class MainAgent extends Agent {

    private GUI gui;
    public AID[] playerAgents;
    private GameParametersStruct parameters = new GameParametersStruct();
    private Semaphore pausaSemaforo = new Semaphore(1);
    public int rondas;
    public int gamesPlayed=0;
    public int numeroJugadores;
    private int ron = parameters.R;
    private int pausado = 0;
    

    @Override
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));
        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
        
    }

    public int updatePlayers() {
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
                numeroJugadores = result.length;

                SwingUtilities.invokeLater(() -> gui.setTotalPlayers(numeroJugadores));
            }
            playerAgents = new AID[result.length];

            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
          

            

           updatePlayersUI();  // Actualiza la UI aquí
            
            gui.updatePlayerFromTable();

        
    
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }

        return 0;
    }



    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {

            //Assign the IDs
            ArrayList<PlayerInformation> players = new ArrayList<>();
            int lastId = 0;
            for (AID a : playerAgents) {
                players.add(new PlayerInformation(a, lastId++));
            }

            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," +  parameters.R );
                msg.addReceiver(player.aid);
                send(msg);
            }

            //Organize the matches
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) { //too lazy to think, let's see if it works or it breaks
                    
              PlayerInformation player1 = players.get(i);
              PlayerInformation player2 = players.get(j);

              if(playerAgentsContains(player1) && playerAgentsContains(player2)){

              
                    playGame(players.get(i), players.get(j));
            }}
        }}


        //Empieza el juego y las interacciones entre los agentes

        private void playGame(PlayerInformation player1, PlayerInformation player2) {
            
            //Assuming player1.id < player2.id
            
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("NewGame#" + player1.id + "#" + player2.id);
            SwingUtilities.invokeLater(() -> gui.setGamesPlayed(gamesPlayed));
            send(msg);

            int payoff1=0;
            int payoff2=0;
            
           
            

            for(int i=0; i<parameters.R; i++){
               
                    try{
                    pausaSemaforo.acquire();
                    }catch(InterruptedException e){


                    }

            char pos1, pos2;

            int resultado1=0;
            int resultado2=0;

            msg = new ACLMessage(ACLMessage.REQUEST);
            
            msg.setContent("Action");
            msg.addReceiver(player1.aid);
            send(msg);

            gui.logLine("Main Waiting for movement");
            ACLMessage move1 = blockingReceive();
            gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
            String posString1=  move1.getContent().split("#")[1];
            pos1 = posString1.charAt(0);
            
            msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setContent("Action");
            msg.addReceiver(player2.aid);
            send(msg);

            gui.logLine("Main Waiting for movement");
            ACLMessage move2 = blockingReceive();
            gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
            String posString2=  move2.getContent().split("#")[1];
            pos2 = posString2.charAt(0);

            if(pos1=='H'&& pos2=='H'){

                resultado1 = -1;
                resultado2= -1;

                payoff1 = payoff1-1;
                payoff2 = payoff2 -1; 

            }

            if(pos1=='H'&& pos2=='D'){

                resultado1 = 10;
                resultado2= 0;

                payoff1 = payoff1 + 10;
                payoff2 = payoff2 +0; 

            }

            if(pos1=='D'&& pos2=='H'){

                resultado1 = 0;
                resultado2= 10;

                payoff1 = payoff1 + 0;
                payoff2 = payoff2 + 10; 

            }

             if(pos1=='D'&& pos2=='D'){

                resultado1 = 5;
                resultado2= 5;
                payoff1 = payoff1 + 5;
                payoff2 = payoff2 + 5; 

            }

            
            msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("Results#"+ player1.id + "," + player2.id + "#"+ pos1 + ","+pos2 + "#"+ resultado1 + "," + resultado2);
            gui.logLine("Results#"+ player1.id + "," + player2.id + "#"+ pos1 + ","+pos2 + "#"+ resultado1 + "," + resultado2);
            send(msg);

            rondas = i+1;

            SwingUtilities.invokeLater(() -> gui.setRoundsLabel(rondas));
            try{

              Thread.sleep(5);  
            }catch(Exception e){

            }

           if (pausaSemaforo.availablePermits() == 0){

           
             pausaSemaforo.release();
            
            }

    }

          

           player1.pointsThisGame = payoff1;
           player2.pointsThisGame = payoff2;

           if (payoff1 > payoff2){

            player1.gamesWon++;
           } else if(payoff2 > payoff1){
            player2.gamesWon++;
           }
           if(pausado==0){
            msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("GameOver#" + player1.id + "," + player2.id + "#" + payoff1 + "," + payoff2);
            gui.logLine("GameOver#" + player1.id + "," + player2.id + "#" + payoff1 + "," + payoff2);
            send(msg);


            gui.anadirTabla(player1.aid.getName(),player1.pointsThisGame ,player1.gamesWon);
            gui.anadirTabla(player2.aid.getName(),player2.pointsThisGame ,player2.gamesWon);
        }}


        @Override
        public boolean done() {
            return true;
        }

    }


    // Metodo que para el juego
      public void StopGame() {

        pausado++;
       
        if (pausado == 1){
         try{
           pausaSemaforo.acquire();
             }catch(InterruptedException e){

                        
                    } 

        System.out.println("Game paused, if you want to continue press the Continue button");
                }
                else{

                    System.out.println("Press Continue");
                }
    }

    //Metodo que hace que continúe el juego
    public void ContinueGame(){

        pausado=0;

        if (pausaSemaforo.availablePermits() == 0) {
            // El juego está en pausa, continúa
            pausaSemaforo.release();
            System.out.println("Continue");
    }}


    // Metodo que elimina a un jugador del array de agentes

    public void removePlayer(String playerName) {
 
        AID removedAID = Arrays.stream(playerAgents)
        .filter(aid -> aid.getName().equals(playerName))
        .findFirst()
        .orElse(null);

if (removedAID != null) {
    // Elimina al jugador de la lista de agentes
    List<AID> updatedPlayerList = new ArrayList<>(Arrays.asList(playerAgents));
    updatedPlayerList.remove(removedAID);
    playerAgents = updatedPlayerList.toArray(new AID[0]);

    // Actualiza el número de jugadores
    numeroJugadores = updatedPlayerList.size();
    SwingUtilities.invokeLater(() -> gui.setTotalPlayers(numeroJugadores));

    // Deregistra al jugador del servicio de páginas amarillas
    deregisterPlayer(removedAID);

    // Remueve al jugador de la interfaz gráfica
    gui.removePlayerFromTable(playerName);

    // Actualiza la interfaz de usuario
    updatePlayersUI();
}
       
    }

    // Deregistra a los agentes

    private void deregisterPlayer(AID playerAID) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(playerAID);
    
        try {
            DFService.deregister(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    // Obtiene los nombres de los jugadores y actualiza la interfaz gráfica
    private void updatePlayersUI() {
        
        String[] playerNames = Arrays.stream(playerAgents)
                .map(aid -> aid.getName())
                .toArray(String[]::new);
                 
                gui.setPlayersUI(playerNames);
                  
        
    }



    // Inicio de un nuevo juego (con las variables con sus valores iniciales)
    public int newGame() {
        if((pausaSemaforo.availablePermits() == 0)){
        pausaSemaforo.release();
        }
        pausado = 0;
        rondas = 0;
        gamesPlayed++;
        // updatePlayersUI();
        addBehaviour(new GameManager());
        return 0;
    }



    private boolean playerAgentsContains(PlayerInformation player) {
        return Arrays.asList(playerAgents).contains(player.aid);
    }

    // Metodo que actualiza las rondas
    public void updateRounds(String num){

        ron= Integer.parseInt(num);
        SwingUtilities.invokeLater(() ->parameters.R = ron);
    }

       public int rondasTotales(){
        
       return parameters.R;
       
    }

    // Clase con la información de cada jugador
    
    public class PlayerInformation {

        AID aid;
        int id;
        int pointsThisGame;
        int totalPoints;
        int gamesWon;

        public PlayerInformation(AID a, int i) {
            aid = a;
            id = i;
            pointsThisGame = 0;
            totalPoints = 0;
            gamesWon = 0;
        }

        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }
    }

    // Clase con los parámetros, en este caso el número de jugadores y las rondas

    public class GameParametersStruct {

        int N;                      // numero total de jugadores
        //int S;
        int R;                      // numero de rondas en cada juego
        //int I;
        //int P;

        public GameParametersStruct() {
            N = 2;
            //S = 4;
            R = 10;

            
            //I = 0;
            //P = 10;
        }
    }
}

