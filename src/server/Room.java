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

    public synchronized void addUser(ClientHandler usuario){
        users.add(usuario);
        broadcast("INFO:" + usuario.getUsername() + " entrou na sala.");
    }

    public synchronized void removeUser(ClientHandler usuario) {
        users.remove(usuario);
        broadcast("INFO:" + usuario.getUsername() + " saiu da sala.");
    }

    public synchronized void broadcast(String mensagem) {
        for (ClientHandler usuario : users) {
            usuario.sendMessage(mensagem);
        }
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
