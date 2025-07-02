package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer
{
    private ServerSocket serverSocket;
    private final int port;
    private boolean isRunning;
    private ExecutorService threadPool;
    private RoomManager roomManager;
    // Estruturas de dados para gerenciar clientes e salas
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public ChatServer(int port) {  //construtor
        this.port = port;
        this.isRunning = false;
        this.threadPool = Executors.newCachedThreadPool();
        this.roomManager = new RoomManager();
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
    public synchronized boolean isUsernameTaken(String nome) {
        return clients.containsKey(nome);
    }

    public synchronized void addClient(ClientHandler handler) {
        clients.put(handler.getUsername(), handler);
    }

    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler.getUsername());
    }

    // Métodos para gerenciar salas
    public boolean createRoom(String roomName) {
        return roomManager.createRoom(roomName);
    }

    public boolean enterRoom(ClientHandler client, String roomName) {
        return roomManager.enterRoom(roomName, client);
    }

    public boolean exitRoom(ClientHandler client, String roomName) {
        return (roomManager.exitRoom(roomName, client));
    }

    public boolean closeRoom(String roomName) {
        return roomManager.endRoom(roomName);
    }

    public void broadcastToRoom(String roomName, String message, ClientHandler sender) {
        roomManager.broadcastToRoom(roomName, message, sender);
    }

    public String getRoomsList() {
        return roomManager.listRooms();
    }

    public String getUsersInRoom(String roomName) {
        return roomManager.getUsersInRoom(roomName);
    }

    public boolean kickUserFromRoom(String roomName, String username) {
        return roomManager.kickUser(roomName, username);
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