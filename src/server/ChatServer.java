package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private final int port;
    private boolean isRunning;
    private ExecutorService threadPool;

    // Estruturas de dados para gerenciar clientes e salas
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
        this.isRunning = false;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;

            System.out.println("Servidor de chat iniciado na porta " + port);
            System.out.println("Aguardando conexões...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nova conexão aceita: " + clientSocket.getRemoteSocketAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    threadPool.submit(clientHandler);

                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;

        // Desconecta todos os clientes
        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("Servidor encerrado.");
        } catch (IOException e) {
            System.err.println("Erro ao encerrar servidor: " + e.getMessage());
        }
    }

    // Métodos para gerenciar clientes
    public synchronized void addClient(ClientHandler client) {
        clients.put(client.getUsername(), client);
    }

    public synchronized void removeClient(ClientHandler client) {
        if (client.getUsername() != null) {
            clients.remove(client.getUsername());
        }
    }

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    // Métodos para gerenciar salas
    public synchronized boolean createRoom(String roomName) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        rooms.put(roomName, ConcurrentHashMap.newKeySet());
        System.out.println("Sala '" + roomName + "' criada");
        return true;
    }

    public synchronized boolean enterRoom(ClientHandler client, String roomName) {
        // Cria a sala se não existir
        if (!rooms.containsKey(roomName)) {
            createRoom(roomName);
        }

        Set<ClientHandler> roomClients = rooms.get(roomName);
        roomClients.add(client);

        // Notifica outros usuários na sala
        broadcastToRoom(roomName, "INFO:" + client.getUsername() + " entrou na sala", client);

        System.out.println(client.getUsername() + " entrou na sala: " + roomName);
        return true;
    }

    public synchronized void exitRoom(ClientHandler client, String roomName) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            roomClients.remove(client);

            // Notifica outros usuários na sala
            broadcastToRoom(roomName, "INFO:" + client.getUsername() + " saiu da sala", null);

            // Remove sala se estiver vazia
            if (roomClients.isEmpty()) {
                rooms.remove(roomName);
                System.out.println("Sala '" + roomName + "' removida (vazia)");
            }

            System.out.println(client.getUsername() + " saiu da sala: " + roomName);
        }
    }

    public synchronized boolean closeRoom(String roomName) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients == null) {
            return false;
        }

        // Notifica todos os usuários na sala
        for (ClientHandler client : roomClients) {
            client.sendMessage("INFO:A sala '" + roomName + "' foi encerrada pelo administrador");
        }

        rooms.remove(roomName);
        System.out.println("Sala '" + roomName + "' encerrada por administrador");
        return true;
    }

    public void broadcastToRoom(String roomName, String message, ClientHandler sender) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            for (ClientHandler client : roomClients) {
                if (client != sender && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public String getRoomsList() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<ClientHandler>> entry : rooms.entrySet()) {
            if (sb.isEmpty()) sb.append(",");
            sb.append(entry.getKey()).append("|").append(entry.getValue().size());
        }
        return sb.toString();
    }

    public String getUsersInRoom(String roomName) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : roomClients) {
            if (sb.isEmpty()) sb.append(",");
            sb.append(client.getUsername()).append("|");
            if (client.isAdmin()) {
                sb.append("admin");
            } else {
                sb.append("user");
            }
        }
        return sb.toString();
    }

    public synchronized boolean kickUserFromRoom(String roomName, String username) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients == null) {
            return false;
        }

        ClientHandler targetClient = clients.get(username);
        if (targetClient == null || !roomClients.contains(targetClient)) {
            return false;
        }

        roomClients.remove(targetClient);
        targetClient.sendMessage("INFO:Você foi expulso da sala '" + roomName + "' por um administrador");

        // Remove sala se estiver vazia
        if (roomClients.isEmpty()) {
            rooms.remove(roomName);
        }

        return true;
    }

    public static void main(String[] args) {
        int port = 12345;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando porta padrão: " + port);
            }
        }

        ChatServer server = new ChatServer(port);

        // Adiciona shutdown hook para encerrar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerrando servidor...");
            server.stop();
        }));

        server.start();
    }
}