package Servidores.FastFileServer;

public class MainServer {
    public static void main(String[] args) {
        if(args.length >= 2){
            String ip = args[0];
            int porta = Integer.parseInt(args[1]);
            System.out.println("Ipv4 do servidor: " + ip);
            String pasta = ".";
            if(args.length==3){
                pasta = args[2];
                System.out.println("Pasta com os ficheiros: " + pasta);
            }
            FastFileServer s = new FastFileServer(ip, pasta,porta);
            s.run();
        }else{
            System.out.println("Faltou os argumentos referente ao ip/porta do servidor.");
        }
    }
}
