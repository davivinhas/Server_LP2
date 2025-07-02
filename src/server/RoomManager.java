package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final Map<String, Room> rooms;

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
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
                target.sendMessage("INFO:Voce foi expulso da sala '" + roomName + "'.");
                return true;
            }
        }
        return false;
    }
}