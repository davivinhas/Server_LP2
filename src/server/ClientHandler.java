package server;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String currentRoom;
    private boolean connected;
    private ChatServer server;
    private boolean isAdmin;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
        this.username = null;
        this.currentRoom = null;
        this.isAdmin = false;

        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch(IOException e){
            System.err.println("Erro ao inicializar ClientHandler: " + e.getMessage());
            disconnect();
        }
    }

    @Override
    public void run() {
        try{
            String message;
            while(connected && (message = in.readLine()) != null){
                processMessage(message);
            }
        }catch(IOException e){
            System.err.println("Erro na comunicação com o ClientHandler: " + e.getMessage());
        }finally{
            disconnect();
        }
    }

    private void processMessage(String message){
        String[] parts = message.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1? parts[1] : "";

        switch(command){
            case "login":
                processLogin(data);
                break;
            case "LISTAR_SALAS":
                listRooms();
                break;
            case "ENTRAR_SALA":
                enterRoom(data);
                break;
            case "SAIR_SALA":
                exitRoom();
                break;
            case "MENSAGEM":
                sendChatMessage(data);
                break;
            case "CRIAR_SALA":
                createRoom(data);
                break;
            case "EXPULSAR":
                kickUser(data);
                break;
            case "ENCERRAR_SALA":
                closeRoom(data);
                break;
            case "DESCONECTAR":
                disconnect();
                break;
            default:
                sendError("Comando nao reconhecido " + command);
        }

    }

    private void processLogin(String data) {
        String[] loginParts = data.split(":");
        String username = loginParts[0];
        String key = loginParts.length > 1 ? loginParts[1] : "";

        // Verifica se o nome já está em uso
        if (server.isUsernameTaken(username)) {
            sendError("Nome de usuário já está em uso!");
            return;
        }

        // Verifica se é admin
        if (key.equals("admin123")) {
            isAdmin = true;
        }

        this.username = username;
        server.addClient(this);

        if (isAdmin) {
            sendSuccess("Login realizado como administrador: " + username);
        } else {
            sendSuccess("Login realizado: " + username);
        }

        System.out.println("Usuario " + username + " conectado" + (isAdmin ? " (Admin)" : ""));
    }

    private void listRooms() {
        if (!isLoggedIn()) return;

        String roomsList = server.getRoomsList();
        out.println("SALAS:" + roomsList);
    }

    private void enterRoom(String roomName) {
        if (!isLoggedIn()) return;

        if (currentRoom != null) {
            sendError("Você já está em uma sala. Use /sair primeiro.");
            return;
        }

        if (server.enterRoom(this, roomName)) {
            currentRoom = roomName;
            sendSuccess("Entrou na sala: " + roomName);

            // Lista usuários na sala
            String usersList = server.getUsersInRoom(roomName);
            out.println("USUARIOS:" + usersList);
        } else {
            sendError("Não foi possível entrar na sala: " + roomName);
        }
    }

    private void exitRoom() {
        if (!isLoggedIn()){
            return;
        }

        if (currentRoom == null) {
            sendError("Você não está em nenhuma sala.");
            return;
        }

        server.exitRoom(this, currentRoom);
        String oldRoom = currentRoom;
        currentRoom = null;
        sendSuccess("Saiu da sala: " + oldRoom);
    }

    private void sendChatMessage(String message) {
        if (!isLoggedIn()) return;

        if (currentRoom == null) {
            sendError("Você precisa estar em uma sala para enviar mensagens.");
            return;
        }

        server.broadcastToRoom(currentRoom, "MSG:" + username + ":" + currentRoom + ":" + message, this);

        // Echo da própria mensagem
        out.println("[" + currentRoom + "] Você: " + message);
    }

    private void createRoom(String roomName) {
        if (!isLoggedIn()) return;

        if (!isAdmin) {
            sendError("Apenas administradores podem criar salas.");
            return;
        }

        if (server.createRoom(roomName)) {
            sendSuccess("Sala '" + roomName + "' criada com sucesso!");
        } else {
            sendError("Não foi possível criar a sala. Ela já pode existir.");
        }
    }

    private void kickUser(String targetUsername) {
        if (!isLoggedIn()) return;

        if (!isAdmin) {
            sendError("Apenas administradores podem expulsar usuários.");
            return;
        }

        if (currentRoom == null) {
            sendError("Você precisa estar em uma sala para expulsar usuários.");
            return;
        }

        if (server.kickUserFromRoom(currentRoom, targetUsername)) {
            sendSuccess("Usuário " + targetUsername + " expulso da sala.");
            server.broadcastToRoom(currentRoom, "INFO:" + targetUsername + " foi expulso da sala pelo admin " + username, null);
        } else {
            sendError("Não foi possível expulsar o usuário. Verifique se ele está na sala.");
        }
    }

    private void closeRoom(String roomName) {
        if (!isLoggedIn()) return;

        if (!isAdmin) {
            sendError("Apenas administradores podem encerrar salas.");
            return;
        }

        if (server.closeRoom(roomName)) {
            sendSuccess("Sala '" + roomName + "' encerrada.");
        } else {
            sendError("Não foi possível encerrar a sala: " + roomName);
        }
    }

    private boolean isLoggedIn() {
        if (username == null) {
            sendError("Você precisa fazer login primeiro. Use /login <nome>");
            return false;
        }
        return true;
    }

    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    private void sendError(String message) {
        out.println("ERRO:" + message);
    }

    private void sendSuccess(String message) {
        out.println("SUCESSO:" + message);
    }

    private void sendInfo(String message) {
        out.println("INFO:" + message);
    }

    public void disconnect() {
        connected = false;

        if (currentRoom != null) {
            server.exitRoom(this, currentRoom);
        }

        if (username != null) {
            server.removeClient(this);
            System.out.println("Usuario " + username + " desconectado");
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isConnected() {
        return connected;
    }

    public Socket getSocket() {
        return socket;
    }
}
