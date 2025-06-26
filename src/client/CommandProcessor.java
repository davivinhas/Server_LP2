package client;
import java.io.PrintWriter;

public class CommandProcessor
{
    private PrintWriter out;

    public CommandProcessor(PrintWriter out)
    {
        this.out = out;
    }

    public void processCommands(String command)
    {
        if (command.startsWith("/login ")) {
            processLogin(command);
        } else if (command.equals("/salas")) {
            out.println("LISTAR_SALAS");
        } else if (command.startsWith("/entrar ")) {
            String sala = command.substring(8).trim();
            out.println("ENTRAR_SALA:" + sala);
        } else if (command.equals("/sair")) {
            out.println("SAIR_SALA");
        } else if (command.startsWith("/msg ")) {
            String mensagem = command.substring(5).trim();
            out.println("MENSAGEM:" + mensagem);
        } else if (command.startsWith("/criar ")) {
            String sala = command.substring(7).trim();
            out.println("CRIAR_SALA:" + sala);
        } else if (command.startsWith("/expulsar ")) {
            String usuario = command.substring(10).trim();
            out.println("EXPULSAR:" + usuario);
        } else if (command.startsWith("/encerrar ")) {
            String sala = command.substring(10).trim();
            out.println("ENCERRAR_SALA:" + sala);
        } else if (command.startsWith("/")) {
            System.out.println("Comando inválido. Digite /ajuda para ver os comandos disponíveis.");
        } else {
            // Se não começar com /, trata como mensagem
            out.println("MENSAGEM:" + command);
        }
    }


    private void processLogin(String command)
    {
        String[] commandParts = command.split(" ");
        if(commandParts.length < 2) {
            System.out.println("Uso: /login <nome>");
        }
        String username = commandParts[1];
        String key = "";
        if(commandParts.length >= 3) {
            key = commandParts[2];
        }
        out.println("Login " + username + ":" + key);
    }
}
