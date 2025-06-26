package client;

import java.io.*;
import java.util.Scanner;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private CommandProcessor processor;
    private boolean conected = false;
    private String username = "";

    public ChatClient(String host, int port) {
        try{
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            processor = new CommandProcessor(output);
            conected = true;

            System.out.println("Conectado ao servidor " + host + ":" + port);
            System.out.println("Digite /login <nome> para começar");
            System.out.println("Digite /ajuda para ver os comandos disponíveis");
        }catch(IOException e){
            System.err.println("Erro ao conectar ao servidor: " + e.getMessage());
        }
    }


    public void start(){
        if(!conected){
            System.err.println("Não foi possível conectar ao servidor.");
            return;
        }
        Thread threadReception = new Thread(new MessageReceiver());
        threadReception.start();

        // Thread principal para entrada do usuário
        Scanner scanner = new Scanner(System.in);
        String command;

        while (conected && (command = scanner.nextLine()) != null) {
            if (command.trim().isEmpty()) {
                continue;
            }

            // Processa comandos especiais do cliente
            if (command.equals("/sairServidor") || command.equals("/quit")) {
                disconnect();
                break;
            } else if (command.equals("/ajuda") || command.equals("/help")) {
                showHelp();
            } else if (command.startsWith("/login ")) {
                // Extrai o nome do usuário para uso local
                String[] partes = command.split(" ");
                if (partes.length >= 2) {
                    username = partes[1];
                }
                processor.processCommands(command);
            } else {
                // Processa outros comandos
                processor.processCommands(command);
            }
        }
        scanner.close();
    }


    private void disconnect(){
        try{
            conected = false;
            if(output != null){
                output.println("DESCONECTAR");
            }
            if(socket != null && !socket.isClosed()){
                socket.close();
            }
            System.out.println("Desconectado do servidor com sucesso.");
        }catch(IOException e){
            System.err.println("Erro ao desconectar do servidor: " + e.getMessage());
        }
    }


    private void showHelp(){
        System.out.println("\n=== COMANDOS DISPONÍVEIS ===");
        System.out.println("/login <nome> [senha] - Fazer login (use senha de admin se for administrador)");
        System.out.println("/salas - Listar salas disponíveis");
        System.out.println("/entrar <sala> - Entrar em uma sala");
        System.out.println("/sair - Sair da sala atual");
        System.out.println("/msg <mensagem> - Enviar mensagem na sala");
        System.out.println("/criar <sala> - Criar nova sala (apenas admin)");
        System.out.println("/expulsar <usuario> - Expulsar usuário da sala (apenas admin)");
        System.out.println("/encerrar <sala> - Encerrar uma sala (apenas admin)");
        System.out.println("/sairServidor - Desconectar do servidor");
        System.out.println("/ajuda - Mostrar esta ajuda");
        System.out.println("=============================\n");
    }


    private class MessageReceiver implements Runnable{
        @Override
        public void run(){
            try{
                String message;
                while(conected && ((message = input.readLine()) != null)){
                    if (message.startsWith("ERRO:")) {
                        System.err.println("❌ " + message.substring(5));
                    } else if (message.startsWith("SUCESSO:")) {
                        System.out.println("✅ " + message.substring(8));
                    } else if (message.startsWith("INFO:")) {
                        System.out.println("ℹ️  " + message.substring(5));
                    } else if (message.startsWith("SALAS:")) {
                        showRooms(message.substring(6));
                    } else if (message.startsWith("USUARIOS:")) {
                        showUsers(message.substring(9));
                    } else if (message.startsWith("MSG:")) {
                        // Mensagem de chat - formato: MSG:usuario:sala:conteudo
                        String[] partes = message.split(":", 4);
                        if (partes.length >= 4) {
                            String usuario = partes[1];
                            String sala = partes[2];
                            String conteudo = partes[3];

                            if (!usuario.equals(username)) {
                                System.out.println("[" + sala + "] " + usuario + ": " + conteudo);
                            }
                        }
                    } else {
                        // Mensagem genérica do servidor
                        System.out.println(message);
                    }
                }
            }catch(IOException e){
                System.err.println("Conexão perdida com o servidor: " + e.getMessage());
                conected = false;
            }
        }
    }


    private void showUsers(String usersString){
        System.out.println("\n=== USUÁRIOS NA SALA ===");
        if(usersString.trim().isEmpty()){
            System.out.println("Sala Vazia");
        }else{
            String[] users = usersString.split(",");
            for(String user : users){
                String[] info = user.split("\\|");
                if(info.length >= 2){
                    String name = info[0];
                    String type = info[1];
                    if(type.equalsIgnoreCase("admin")) {
                        System.out.println("Usuario: " + name + "(Admin)");
                    }else{
                        System.out.println("Usuario: " + name);
                    }
                }
            }
        }
        System.out.println("=========================\n");
    }


    private void showRooms(String roomsString){
        System.out.println("\n=== SALAS DISPONÍVEIS ===");
        if (roomsString.trim().isEmpty()) {
            System.out.println("Nenhuma sala disponível");
        } else {
            String[] salas = roomsString.split(",");
            for (String sala : salas) {
                String[] info = sala.split("\\|");
                if (info.length >= 2) {
                    System.out.println(info[0] + " (" + info[1] + " usuários)");
                } else {
                    System.out.println(sala);
                }
            }
        }
        System.out.println("==========================\n");
    }


    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        // Permite passar host e porta como argumentos
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando porta padrão: " + port);
            }
        }

        System.out.println("Iniciando cliente do chat...");
        ChatClient client = new ChatClient(host, port);
        client.start();
    }
}


