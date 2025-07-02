package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final Map<String, Room> rooms;

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
    }

    public synchronized boolean createRoom(String name) {
        if (rooms.containsKey(name)) {
            return false; // Sala já existe
        }
        rooms.put(name, new Room(name));
        System.out.println("Sala '" + name + "' criada");
        return true;
    }

    public synchronized boolean endRoom(String name) {
        Room room = rooms.remove(name);
        return room != null;
    }

    public Room getRoom(String name) {
        return rooms.get(name);
    }

    public synchronized String listRooms(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, Room> entry : rooms.entrySet()){
            String name = entry.getKey();
            int count = entry.getValue().getUserCount();
            sb.append(name).append("|").append(count).append(",");
        }

        if(sb.length() > 0 ){
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public String getUsersInRoom(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (ClientHandler user : room.getUsers()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(user.getUsername()).append("|");
            if (user.isAdmin()) {
                sb.append("admin");
            } else {
                sb.append("user");
            }
        }
        return sb.toString();
    }

    public boolean enterRoom(String roomName, ClientHandler user){
        Room room = rooms.get(roomName);
        if(room != null){
            room.addUser(user);
            return true;
        }
        return false;
    }

    public boolean exitRoom(String roomName, ClientHandler user){
        Room room = rooms.get(roomName);
        if(room != null){
            room.removeUser(user);
            System.out.println(user.getUsername() + " saiu da sala: " + roomName);
            return true;
        }
        return false;
    }

    public boolean kickUser(String roomName, String userName){
        Room room = rooms.get(roomName);
        if(room != null){
            ClientHandler target = room.getUser(userName);
            if(target != null){
                room.removeUser(target);
                target.setCurrentRoom(null);
                target.sendMessage("INFO:Voce foi expulso da sala '" + roomName + "'.");
                return true;
            }
        }
        return false;
    }

    public void broadcastToRoom(String roomName, String message, ClientHandler sender){
        Room room = rooms.get(roomName);
        if (room != null) {
            if (message.startsWith("MSG:")) {
                // É uma mensagem de chat
                String[] parts = message.split(":", 4);
                if (parts.length >= 4) {
                    String chatMessage = parts[3];
                    room.broadcastChatMessage(chatMessage, sender);
                }
            } else {
                // Outras mensagens (INFO, etc.)
                room.broadcastToOthers(message, sender);
            }
        }
    }
}