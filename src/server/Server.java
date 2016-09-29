/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package server;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.Timer;

/**
 *
 * @author GaoYifei
 */
public class Server extends javax.swing.JFrame {
    ArrayList clientOutputStreams;
    ArrayList<String> nickNameList;
    ServerThread serverThread;
    ClientThread clientThread;
    ArrayList<ClientThread> clientThreadList;
    DefaultListModel listModel;
    ServerSocket serverSocket;
    int portNumber;
    static Server server;
    
    
    Boolean serverStarted;
    Calendar calendar;
    /**
     *  ClientThread are responsible to handle the receiving data from the
     *  corresponding client and sending data to the client.
     *  The Thread Would be created when a new request
     *  for socket connection received by server thread.
     *
     *
     */
    public class ClientThread extends Thread {
        private BufferedReader reader;
        private Socket sock;
        private String nickName;
        private PrintWriter writer;
        private Timer timer;
        public ClientThread(Socket clientSocket){
            try{
                
                sock = clientSocket;
                InputStreamReader inputStreamReader =
                        new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(inputStreamReader);
                writer = new PrintWriter(sock.getOutputStream());
                nickName = reader.readLine();
                if(nickNameList.contains(nickName)){
                    writer.println("SAME");
                    writer.flush();
                    
                    
                }
                else{
                    writer.println("You have Connected Successfully !\n");
                    writer.flush();
                    
                    // Update the online user list for the new client
                    writer.println("LIST");
                    writer.println(Integer.toString(nickNameList.size()));
                    for(String temp: nickNameList){
                        if(temp != nickName){
                            writer.println(temp);
                            System.out.println("The list name is "+ nickName);
                        }
                        
                    }
                    writer.flush();
                }
                
                
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        
        public void run(){
            String message;
            
            try{
                while((message = getReader().readLine()) != null){
                    System.out.println("Server Read" + message);
                    if(message.equals("CLOSE")){
                        logText.append(nickName + " "
                                + " disconnected with the server\n");
                        for(ClientThread clientT: clientThreadList){
                            clientT.getWriter().println("OFFLINE");
                            clientT.getWriter().println(nickName);
                            clientT.getWriter().flush();
                        }
                        reader.close();
                        writer.close();
                        sock.close();
                        listModel.removeElement(nickName);
                        userList.setModel(listModel);
                        clientThreadList.remove(this);
                        nickNameList.remove(nickName);
                        this.stop();
                        break;
                        
                    }
                    
                    else if(message.equals("WHISPER")){
                        String whisperUser = reader.readLine();
                        String whisperMessage = reader.readLine();
                        // handle the exception which the whisper user is offline
                        if(!nickNameList.contains(whisperUser)){
                            writer.print("NOTEXIST");
                            writer.flush();
                            logText.append(whisperMessage +
                                    "failed since no target user\n");
                        }
                        // Broadcast to every online user
                        else{
                            logText.append(whisperMessage + "\n");
                            for(ClientThread clientT: clientThreadList){
                                if(clientT.nickName.equals(whisperUser )){
                                    clientT.getWriter().println(whisperMessage);
                                    clientT.getWriter().flush();
                                }
                            }
                        }
                        
                    }
                    else{
                        server.broadcast(message);
                        logText.append(message + "\n");

                    }
                    
                    
                }
                
                
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        
        /**
         * @return the writer
         */
        public PrintWriter getWriter() {
            return writer;
        }
        
        /**
         * @return the reader
         */
        public BufferedReader getReader() {
            return reader;
        }
    }
    
    /**
     *  The Server Thread are responsible to listen to the request for connection
     *  sent from clients.Once the server Socket accept the socket sent by client,
     *  it would create a new socket connecting to the server and go back to wait
     *  for other clients.
     *
     */
    public class ServerThread extends Thread{
        private ServerSocket serverSocket;
        public ServerThread(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
        }
        
        @Override
        public void run() {
            System.out.println("Server started");
            while(true){
                try {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                    clientOutputStreams.add(writer);
                    
                    clientThread = new ClientThread(clientSocket);
                    clientThread.start();
                    if(nickNameList.contains(clientThread.nickName)){
                        clientThread.stop();
                        System.out.println("Server failed to estabilish a connection since the same name");
                        logText.append(clientThread.nickName + " has not connected to the server because the same nick name!\n");
                    }
                    
                    else{
                        clientThreadList.add(clientThread);
                        System.out.println("Server has got a connection from client");
                        logText.append(clientThread.nickName + " has connected to the server !\n");
                        //                    listModel = (DefaultListModel)userList.getModel();
                        listModel.addElement(clientThread.nickName);
                        nickNameList.add(clientThread.nickName);
                        userList.setModel(listModel);
                        
                        server.broadcastCommand("ONLINE", clientThread.nickName);
                        
                    }
                    
                    
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        }
        
        
    }
    
    public class TestThread extends Thread{
        public void run(){
            while(true){
                try{
                    this.sleep(1000);
                    server.broadcast("test message");
                }
                catch(InterruptedException ex){
                    ex.printStackTrace();
                }
                
            }
        }
    }
    /**
     * Creates new form NewServer
     */
    public Server() {
        initComponents();
        clientOutputStreams = new ArrayList();
        clientThreadList = new ArrayList();
        serverStarted = false;
        nickNameList = new ArrayList();
        
        listModel = new DefaultListModel();
        userList.setModel(listModel);
        this.setTitle("Server");
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        userList = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        logText = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        label1 = new javax.swing.JLabel();
        label2 = new javax.swing.JLabel();
        label3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        portLabel = new javax.swing.JLabel();
        ipLabel = new javax.swing.JLabel();
        testButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        startButton.setText("Strart Server");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("Stop Server");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        userList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "admin", " " };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        userList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        userList.setToolTipText("Online Users\n");
        jScrollPane1.setViewportView(userList);

        logText.setEditable(false);
        logText.setBackground(new java.awt.Color(153, 153, 153));
        logText.setColumns(20);
        logText.setRows(5);
        jScrollPane2.setViewportView(logText);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        jLabel1.setText("Online Users");

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        jLabel2.setText("Server Log");

        label1.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        label1.setText("Server Status: ");

        label2.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        label2.setText("IP Address : ");

        label3.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        label3.setText("Port:");

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pic/circled_play.png"))); // NOI18N

        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pic/shutdown.png"))); // NOI18N

        statusLabel.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        statusLabel.setForeground(new java.awt.Color(255, 51, 51));
        statusLabel.setText("Stopped");

        portLabel.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        portLabel.setText("?");

        ipLabel.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        ipLabel.setText("?");

        testButton.setText("Broadcast for Test");
        testButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(label1)
                                    .addComponent(label2)
                                    .addComponent(label3))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(portLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(ipLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(testButton)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(startButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                                                .addGap(15, 15, 15))
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel7)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2)
                        .addGap(353, 353, 353))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(testButton))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label1)
                            .addComponent(statusLabel))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label2)
                            .addComponent(ipLabel))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(label3)
                            .addComponent(portLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(32, 32, 32)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(startButton)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(67, 67, 67)
                                .addComponent(stopButton)
                                .addGap(19, 19, 19))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addGap(10, 10, 10))))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap(47, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (serverStarted) {
            JOptionPane.showMessageDialog(this, "The server is already started！",
                    "error", JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        
        try {
            
            this.startServer();
            
            
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }//GEN-LAST:event_startButtonActionPerformed
    
    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        try {
            // TODO add your handling code here:
            if(!serverStarted){
                JOptionPane.showMessageDialog(this, "The server is already stopped！",
                        "error", JOptionPane.ERROR_MESSAGE);
                
                return;
            }
            
            this.stopServer();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_stopButtonActionPerformed
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        int option = JOptionPane.showConfirmDialog(this, "Sure to Exit", "Confirmation",  JOptionPane.OK_CANCEL_OPTION);
        if (JOptionPane.OK_OPTION == option) {
            if(serverStarted){
                try {
                    this.stopServer();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.exit(0);
        }else{
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        }
    }//GEN-LAST:event_formWindowClosing

    private void testButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testButtonActionPerformed
        // TODO add your handling code here:
        Thread testThread = new TestThread();
        testThread.start();
    }//GEN-LAST:event_testButtonActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
        * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
        */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                server = new Server();
                server.setVisible(true);
                
            }
        });
    }

    
    public void startServer() throws IOException{
        try{
            portNumber = 10000;
            serverSocket = new ServerSocket(portNumber);
            serverThread = new ServerThread(serverSocket);
            serverThread.start();
            serverStarted = true;
            JOptionPane.showMessageDialog(this,
                    "The Server Started Successfully！");
            //      logText.append(calendar.getTime().toString());
            logText.append("The server started. IP Address 127.0.0.1 Port:"+
                    portNumber +"\n");
        }
        catch(BindException ex){
            serverStarted = false;
            JOptionPane.showMessageDialog(this, "The port has been occupied！",
                    "error", JOptionPane.ERROR_MESSAGE);
        }
        statusLabel.setText("Started");
        statusLabel.setForeground(Color.GREEN);
        ipLabel.setText("127.0.0.1");
        portLabel.setText(Integer.toString(portNumber));
        
        
        
    }
    
    public void stopServer() throws IOException{
        
        for(ClientThread clientT: clientThreadList){
            clientT.getWriter().println("STOP");
            
            clientT.getWriter().flush();
        }
        if(serverThread != null){
            serverThread.stop();
        }
        for(ClientThread clientT: clientThreadList){
            clientT.getReader().close();
            clientT.getWriter().close();
            clientT.sock.close();
        }
        clientThreadList.clear();
        if(serverSocket != null){
            serverSocket.close();
        }
        nickNameList.clear();
        listModel.removeAllElements();
        userList.setModel(listModel);
        System.out.println("Server stopped");
        JOptionPane.showMessageDialog(this, "The server stopped successfully！");
        logText.append("The server stopped \n");
        serverStarted = false;
        
        statusLabel.setText("Stopped");
        statusLabel.setForeground(Color.RED);
        ipLabel.setText("?");
        portLabel.setText("?");
        
    }
    /**
     *
     * @author GaoYifei
     */
    public synchronized void broadcastCommand(String command, String s){
        
        for(ClientThread clientT: clientThreadList){
            System.out.println(command);
            clientT.getWriter().println(command);
            clientT.getWriter().println(s);
            clientT.getWriter().flush();
            
        }
    }
    
    public synchronized void broadcast(String message){
        Iterator iterator = clientOutputStreams.iterator();
        while(iterator.hasNext()){
            try{
                PrintWriter writer = (PrintWriter) iterator.next();
                writer.println(message);
                writer.flush();
                
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            
        }
        
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ipLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel label1;
    private javax.swing.JLabel label2;
    private javax.swing.JLabel label3;
    private javax.swing.JTextArea logText;
    private javax.swing.JLabel portLabel;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JButton stopButton;
    private javax.swing.JButton testButton;
    private javax.swing.JList userList;
    // End of variables declaration//GEN-END:variables
}
