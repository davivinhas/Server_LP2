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
        this.connected = false;
        this.username = null;
        this.currentRoom = null;
        this.isAdmin = false;

        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWritter(socket.getOutputStream(), true);
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
            case "Login":
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
                sendMessage(data);
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
}
