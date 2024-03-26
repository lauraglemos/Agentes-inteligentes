import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import javafx.scene.control.TableRow;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.stream.Collectors;


// La clase GUI es la interfaz grafica

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JLabel leftPanelTotalGames;
    JLabel leftPanelTotalPlayers;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    DefaultTableModel tableModel = new DefaultTableModel();
    String numRondas;
    
    

    public GUI() {
        initUI();
    }

    //Establece al parametro que recibe como Main Agent e inicializa la interfaz de usuario.
    public GUI (MainAgent agent) {
        mainAgent = agent;
        initUI();
         loggingOutputStream = new LoggingOutputStream (rightPanelLoggingTextArea);
    
    
        }


    public void log (String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine (String s) {
        log(s + "\n");
    }

    public void setPlayersUI (String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    
    }

    // Inicializa la interfaz del usuario
    public void initUI() {
        setTitle("GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1000, 600));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);
        
    }

    // Diseño de la interfaz grafica del juego
    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        pane.setBackground(new Color(166, 177, 225));
  
        //LEFT PANEL
        
        gc.gridx = 0;
        gc.weightx = 1.5;
        pane.add(leftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 8;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 4; 
        pane.add(createRightPanel(), gc);

       
    
        return pane;
    }

    // Panel de la derecha, que contiene un panel con la informacion de los parametros y otro con botones
    private JPanel leftPanel(){

        JPanel leftPanelt = new JPanel(new GridLayout(6,1,5,20));
        leftPanelt.setBackground(new Color(166, 177, 225));
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 2;
        gc.weighty = 1;
        gc.gridheight= 1;
        leftPanelt.add(createInformativePanel(), gc);
        gc.gridy = 3;
        gc.weighty = 4;
        leftPanelt.add(createLeftPanel(), gc);


        int topMargin = 15;
        int leftMargin = 15;
        int bottomMargin = 15;
        int rightMargin = 15;
        leftPanelt.setBorder(BorderFactory.createEmptyBorder(topMargin, leftMargin, bottomMargin, rightMargin));
        return leftPanelt;


    }

    // Panel con los parametros de las rondas, los juegos jugados y numero de jugadores

    private JPanel createInformativePanel(){
        GridBagConstraints gc = new GridBagConstraints();
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 1));
        
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(10, 20, 10, 20);
        
        int rondasTotales = mainAgent.rondasTotales();

        leftPanelRoundsLabel = new JLabel("Round " + 0 + "/" + rondasTotales);
        leftPanelRoundsLabel.setHorizontalAlignment(JLabel.CENTER);
        
        leftPanelTotalGames = new JLabel("Games played:" + 0);
        leftPanelTotalGames.setHorizontalAlignment(JLabel.CENTER);

        leftPanelTotalPlayers = new JLabel("Number of players: "+ mainAgent.numeroJugadores);
        leftPanelTotalPlayers.setHorizontalAlignment(JLabel.CENTER);

        leftPanelRoundsLabel.setFont(new Font("Arial", Font.BOLD, 20));
        leftPanelTotalGames.setFont(new Font("Arial", Font.BOLD, 20));
        leftPanelTotalPlayers.setFont(new Font("Arial", Font.BOLD, 20));

        gc.gridy = 0;
        gc.gridx = 0;
        infoPanel.add(leftPanelRoundsLabel,gc);

        
        gc.gridx = 0;
        gc.gridy = 1;
        infoPanel.add(leftPanelTotalGames,gc);

       
        gc.gridx = 0;
        gc.gridy = 2;
        infoPanel.add(leftPanelTotalPlayers,gc);

        infoPanel.setBackground(new Color(220,214,247));
   
        infoPanel.setBorder(new CompoundBorder(
                new LineBorder(Color.BLACK, 5), // Borde exterior gris
                new EmptyBorder(2, 2, 2, 2)     // Espaciado interno
        ));
  
        return infoPanel;

    }

    // El panel que contiene los botones New, Stop y Continue
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        leftPanel.setBackground(new Color(166, 177, 225));
        JButton leftPanelNewButton = new JButton("New");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.StopGame());
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.ContinueGame());

        

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;
        gc.weightx = 1;
        gc.weighty = 0.5;
        gc.insets = new Insets(5, 100, 5, 100);
      

       
        gc.gridy = 3;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 9;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 12;
        leftPanel.add(leftPanelContinueButton, gc);
        gc.gridy = 15;
        gc.weighty = 10;
        

       

       
        return leftPanel;
    }

    // Muestra el valor actualizado de los jugadores totales

    public void setTotalPlayers(int num){

        SwingUtilities.invokeLater(() -> leftPanelTotalPlayers.setText("Number of players: "+ mainAgent.numeroJugadores));
    }

    // Muestra el valor actualizado de las rondas

    public void setRoundsLabel(int rondaActual){

            SwingUtilities.invokeLater(() -> leftPanelRoundsLabel.setText("Round " + rondaActual+ "/" + mainAgent.rondasTotales()));

        }

    // Muestra el valor actualizado de los juegos jugados

     public void setGamesPlayed(int juegoActual){

            SwingUtilities.invokeLater(() -> leftPanelTotalGames.setText("Games played: " + mainAgent.gamesPlayed));

        }

    // El panel central que contiene los resultados de los jugadores, ademas de algunos botones.
   
    private JPanel createCentralPanel() {
        JPanel centralPanel = new JPanel(new GridBagLayout());
        centralPanel.setBackground(new Color(166, 177, 225));
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);
    
        int topMargin = 15;
        int leftMargin = 15;
        int bottomMargin = 15;
        int rightMargin = 15;
        centralPanel.setBorder(BorderFactory.createEmptyBorder(topMargin, leftMargin, bottomMargin, rightMargin));
        return centralPanel;

    }

    // Crea el panel de arriba de la parte central del juego, que contiene la lista de jugadores y los botones para reiniciar los jugadores o eliminarlos. 
    private JPanel createCentralTopSubpanel() {
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());
        centralTopSubpanel.setBackground(new Color(166, 177, 225));
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        JButton delatePlayersButton = new JButton("Delate players");
        delatePlayersButton.addActionListener(actionEvent -> removePlayerAction());
        JButton updatePlayersButton = new JButton("Update players");
        updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 666;
        gc.fill = GridBagConstraints.BOTH;
        centralTopSubpanel.add(listScrollPane, gc);
        gc.gridx = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        centralTopSubpanel.add(delatePlayersButton, gc);
        gc.gridy = 1;
        centralTopSubpanel.add(updatePlayersButton, gc);

        return centralTopSubpanel;
    }

    // Crea la tabla con los resultados de los jugadores: su nombre, los puntos del último juego y la cantidad de juegos que ha ganado
    private JPanel createCentralBottomSubpanel() {
        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());
        centralBottomSubpanel.setBackground(new Color(166, 177, 225));
       

        tableModel.addColumn("Player");
        tableModel.addColumn("Points in this Game");
        tableModel.addColumn("Games Won");
            
            
        Object[] datosColumnas = {"Player","Points in the Game","Games Won"};
            
        tableModel.addRow(datosColumnas);


        JLabel payoffLabel = new JLabel("PLAYERS RESULTS");

        payoffLabel.setFont(new Font("Arial", Font.BOLD, 18));
        payoffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JTable payoffTable = new JTable(tableModel);
        payoffTable.setTableHeader(null);
        payoffTable.setEnabled(false);
        

        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

        payoffTable.setDefaultRenderer(Object.class, new CustomHeaderRenderer());
         

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.25;
        gc.weightx = 0.25;
        payoffLabel.setHorizontalAlignment(JLabel.CENTER);
        centralBottomSubpanel.add(payoffLabel, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 2;
        centralBottomSubpanel.add(player1ScrollPane, gc);
        
        return centralBottomSubpanel;
    }

    // Metodo para añadir todo en la tabla con sus valores correspondientes

    public void anadirTabla(String nombre, int pos1, int partidasGanadas){
        JTable payoffTable1 = new JTable(tableModel);
        DefaultTableModel tableModel = (DefaultTableModel)payoffTable1.getModel();

        SwingUtilities.invokeLater(() -> {
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                if (tableModel.getValueAt(i, 0).equals(nombre)) {
                    // Actualiza los valores en la misma fila
                    tableModel.setValueAt(pos1, i, 1);
                    tableModel.setValueAt(partidasGanadas, i, 2);
                    return;
                }
            }

        tableModel.addRow(new Object[]{nombre,pos1,partidasGanadas});
         
        }
        
    );}

    // Metodo que actualiza la tabla para que los valores de todos los jugadores seab 0
     public void updatePlayerFromTable(){

        JTable payoffTable = new JTable(tableModel);
        DefaultTableModel tableModel = (DefaultTableModel)payoffTable.getModel();

        
            int rowCount = tableModel.getRowCount();
            for (int i = 1; i < rowCount; i++) {

                    tableModel.setValueAt(0, i, 1);
                    tableModel.setValueAt(0, i, 2);
                    
                
            }
        

    }

    // Metodo que elimina a un jugador de la tabla
    public void removePlayerFromTable(String playerName){

        JTable payoffTable1 = new JTable(tableModel);
        DefaultTableModel tableModel = (DefaultTableModel)payoffTable1.getModel();

        SwingUtilities.invokeLater(() -> {
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                if (tableModel.getValueAt(i, 0).equals(playerName)) {
                    tableModel.removeRow(i);
                    return;
                }
            }
        }
        
    );

    }
   

    // Crea la parte derecha de la interfaz, que es la consola.

    private JPanel createRightPanel() {
        rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        rightPanel.setBackground(new Color(166, 177, 225));
        c.weighty = 1d;
        c.weightx = 1d;

        rightPanelLoggingTextArea = new JTextArea("");
        rightPanelLoggingTextArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        rightPanel.add(jScrollPane, c);

        int topMargin = 15;
        int leftMargin = 10;
        int bottomMargin = 15;
        int rightMargin = 10;
        rightPanel.setBorder(BorderFactory.createEmptyBorder(topMargin, leftMargin, bottomMargin, rightMargin));
        return rightPanel;
    }

    // Crea el menú con distintas opciones
    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem exitFileMenu = new JMenuItem("Exit");
        exitFileMenu.setToolTipText("Exit application");
        exitFileMenu.addActionListener(new ActionListener() {

            @Override

            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        GUI.this, // El componente padre del cuadro de diálogo
                        "Are you sure you want to exit?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
        
                if (confirm == JOptionPane.YES_OPTION) {
                    System.out.println("Exiting application");
                    System.exit(0);
                }
            }
        });

        JMenuItem newGameFileMenu = new JMenuItem("New Game");
        newGameFileMenu.setToolTipText("Start a new game");
        newGameFileMenu.addActionListener(actionEvent -> mainAgent.newGame());

        menuFile.add(newGameFileMenu);
        menuFile.add(exitFileMenu);
        menuBar.add(menuFile);

        JMenu menuEdit = new JMenu("Edit");
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Reset all player");
        resetPlayerEditMenu.setActionCommand("reset_players");
        resetPlayerEditMenu.addActionListener(this);

        JMenuItem removePlayerEditMenu = new JMenuItem("Remove player");
        removePlayerEditMenu.setToolTipText("Remove a player");
        

        removePlayerEditMenu.addActionListener(actionEvent -> removePlayerAction());

        menuEdit.add(resetPlayerEditMenu);
        menuEdit.add(removePlayerEditMenu);
        menuBar.add(menuEdit);

        JMenu menuRun = new JMenu("Run");

        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(actionEvent ->mainAgent.newGame());

        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(actionEvent ->mainAgent.StopGame());

        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(actionEvent -> mainAgent.ContinueGame());

        JMenuItem roundNumberRunMenu = new JMenuItem("Number of rounds");
        roundNumberRunMenu.setToolTipText("Change the number of rounds");
        roundNumberRunMenu.addActionListener(actionEvent -> {
        
            numRondas = JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?");
            logLine (numRondas + " rounds");
            SwingUtilities.invokeLater(() ->mainAgent.updateRounds(numRondas));
        });
        
        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuBar.add(menuRun);

        JMenu menuWindow = new JMenu("Window");

        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        JMenu menuHelp = new JMenu("Help");
        JMenuItem authorMenu = new JMenuItem("About");
        authorMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = "Juego programado por Laura Gonzalez Lemos";
                String title = "Informacion";
                int messageType = JOptionPane.INFORMATION_MESSAGE;
        
                JOptionPane.showMessageDialog(GUI.this, message, title, messageType);
            }
        });
        menuHelp.add(authorMenu);
        menuBar.add(menuHelp);

        return menuBar;
    }

    // Crea un cuadro de dialogo para seleccionar el jugador a eliminar y lo elimina

    private void removePlayerAction() {

        String[] playerNames = getPlayerNames();
        String selectedPlayer = (String) JOptionPane.showInputDialog(
                this,
                "Select a player to remove:",
                "Remove Player",
                JOptionPane.QUESTION_MESSAGE,
                null,
                playerNames,
                playerNames[0]
        );

        if (selectedPlayer != null) {
            // Confirmar la eliminación del jugador
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to remove " + selectedPlayer + "?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {

                mainAgent.removePlayer(selectedPlayer);
            }
        }
    }

    // Obtener los nombres de los jugadores del modelo de la lista
    private String[] getPlayerNames() {

    
        DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
        int size = listModel.getSize();
        String[] playerNames = new String[size];
        for (int i = 0; i < size; i++) {
            playerNames[i] = listModel.getElementAt(i);
        }
        return playerNames;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            logLine("Button " + button.getText());
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            logLine("Menu " + menuItem.getText());
        }
    }


    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

   // Renderizador personalizado para la primera fila
    private class CustomHeaderRenderer extends DefaultTableCellRenderer {
        public CustomHeaderRenderer() {

            setForeground(Color.BLACK);  // Color del texto
            setBackground(new Color(220, 214, 247));   // Color de fondo
            setHorizontalAlignment(JLabel.CENTER);  // Alineación del texto
            setFont(getFont().deriveFont(Font.BOLD, 14f));  // Tamaño y estilo de la fuente
        }
    
}}

 

