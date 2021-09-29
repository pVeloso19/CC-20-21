package Servidores.HTTPgw;

import java.io.IOException;

public class MainHttpGw {

    public static void main(String[] args) throws IOException {
        if(args.length==1){
            int porta = Integer.parseInt(args[0]);
            HttpGw gw = new HttpGw(porta);
            gw.run();
        }else {
            System.out.println("Erro: falta especificar a porta para ficar à escuta de pedidos HTTP.");
        }
    }
}
