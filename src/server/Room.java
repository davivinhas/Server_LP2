package server;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private final String name;
    private final List<ClientHandler> users;

    public Room(String nome) {
        this.name = nome;
        this.users = new ArrayList<>();
    }

    public synchronized void addUser(ClientHandler user){
        users.add(user);
        broadcastToOthers("INFO:" + user.getUsername() + " entrou na sala.", user);
    }

    public synchronized void removeUser(ClientHandler user) {
        users.remove(user);
        broadcastToOthers("INFO:" + user.getUsername() + " saiu da sala.", user);
    }

    public synchronized void broadcastToOthers(String message, ClientHandler sender) {
        for (ClientHandler user : users) {
            if (user != sender && user.isConnected()) {
                user.sendMessage(message);
            }
        }
    }

    public synchronized void broadcastChatMessage(String message, ClientHandler sender) {
        String formattedMessage = "MSG:" + sender.getUsername() + ":" + name + ":" + message;
        broadcastToOthers(formattedMessage, sender);
    }


    public synchronized String getName() {
        return name;
    }

    public synchronized List<ClientHandler> getUsers() {
        return new ArrayList<>(users);
    }

    public synchronized ClientHandler getUser(String name){
        for(ClientHandler u : users){
            if(u.getUsername().equals(name)){
                return u;
            }
        }
        return null;
    }

    public synchronized int getUserCount() {
        return users.size();
    }

    public synchronized boolean hasUser(String nome) {
        for (ClientHandler u : users) {
            if (u.getUsername().equals(nome)) return true;
        }
        return false;
    }
}