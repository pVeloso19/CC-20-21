package Servidores.HTTPgw;

import java.io.IOException;
import java.net.ServerSocket;

public class AceitaPedidosCliente implements Runnable{

    private final int PORT;
    private HttpGw gw;

    public AceitaPedidosCliente(int PORT, HttpGw gw) {
        this.PORT = PORT;
        this.gw = gw;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Servidor Iniciado. Conectado na porta: " + PORT);

            // we listen until user halts server execution
            while (true) {
                JavaHTTPServer myServer = new JavaHTTPServer(this.gw, serverConnect.accept());

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
}
