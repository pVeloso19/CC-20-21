package Servidores.HTTPgw;

import Servidores.FS_Chunk_Protocol;
import Servidores.GeneratePassWord;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ConexaoServidores implements Runnable{

    private final int timeOut = 3000;

    private HttpGw gw;

    private DatagramSocket s;

    private final String password = "CC_2021_TP2_PL1_G08";

    private List<Integer> portasDisponiveis;

    private ReentrantLock l;

    public ConexaoServidores(HttpGw gw) {
        this.portasDisponiveis = new ArrayList<>();
        this.l = new ReentrantLock();

        //Cria 50 portas para inicio de conexoes com os servidores
        for (int i = 0; i < 50; i++){
            int p = 12343 + i;
            this.portasDisponiveis.add(p);
        }

        try {
            this.gw = gw;
            this.s = new DatagramSocket(this.gw.getPorta());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void getFicheirosServidor(DatagramPacket pedido, int portaUsar, int portaPedido) throws SocketException {
        byte[] aReceber = new byte[1024];
        DatagramSocket ds = new DatagramSocket(portaUsar);
        List<byte[]> temp = new ArrayList<>();

        Runnable worker = () -> {
            this.gw.addServidorValido(pedido.getAddress(), pedido.getPort());

            boolean fim = false;
            int bytesLidos = 0;

            // Enquanto não recebe os metadados todos
            while(!fim) {

                int offset = bytesLidos;
                // Pede o primeiro chunk de metadados
                Runnable worker2 = () -> {

                    FS_Chunk_Protocol fsEnviar = new FS_Chunk_Protocol('G', offset, 0, null, 0, null, 'F');

                    while (true) {
                        try {
                            DatagramPacket p = new DatagramPacket(fsEnviar.getBytes(), fsEnviar.getBytes().length, pedido.getAddress(), portaPedido);
                            ds.send(p);
                        } catch (IOException ignored) {
                            System.out.println("Impossivel conectar-se com o servidor ("+ pedido.getAddress()+" | "+ portaPedido +")");
                        }

                        try {
                            Thread.sleep(timeOut);
                        } catch (InterruptedException ignored) {;}
                    }

                };
                Thread t = new Thread(worker2);
                t.start();

                try {
                    //Espera receber o primeiro chunck de metadados
                    DatagramPacket pedidoTemp = new DatagramPacket(aReceber, aReceber.length);
                    ds.receive(pedidoTemp);

                    FS_Chunk_Protocol fsTemp = new FS_Chunk_Protocol(aReceber);
                    t.stop();

                    bytesLidos += fsTemp.getTamanhoDados();

                    temp.add(fsTemp.getDados());

                    // Se ainda há mais chuncks de metadados, realiza o pedido do chunck seguinte
                    fim = fsTemp.isLast();
                    if (fim) {

                        fsTemp = new FS_Chunk_Protocol('G', 0, 0, null, 0, null, 'T');

                        try {
                            DatagramPacket p = new DatagramPacket(fsTemp.getBytes(), fsTemp.getBytes().length, pedido.getAddress(), portaPedido);
                            ds.send(p);
                        } catch (IOException ignored) {
                            System.out.println("Impossivel conectar-se com o servidor ("+ pedido.getAddress()+" | "+ portaPedido +")");
                        }
                    }

                }catch (IOException ignored) {;}
            }

            //Junta todos os chuncks recebidos
            int tam = temp.stream().mapToInt(a->a.length).sum();
            byte[] dados = new byte[tam];
            int j = 0;
            for (byte[] ab : temp){
                for (int i = 0; i<ab.length; i++){
                    dados[j] = ab[i];
                    j++;
                }
            }

            String dadosS = new String(dados);

            //Guarda os metadados
            this.gw.insereDadosFicheirosServer(pedido.getAddress().getHostAddress(), dadosS);

            //Fecha a conexão
            ds.close();

            //Liberta a porta que lhe foi atribuida
            this.l.lock();
            this.portasDisponiveis.add(portaUsar);
            this.l.unlock();
        };
        new Thread(worker).start();
    }

    @Override
    public void run() {
        while(true){
            byte[] aReceber = new byte[1024];
            try {
                DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);
                s.receive(pedido);
                int portaPedido = pedido.getPort();
                FS_Chunk_Protocol fs = new FS_Chunk_Protocol(aReceber);

                switch (fs.getTipo()) {
                    case 'I' -> { // se recebeu um pedido para inicializar a conexao com o servidor
                        if (GeneratePassWord.isAuthenticated(password, fs.getDados())) {
                            // Obtem uma porta especifica para os clientes
                            this.l.lock();
                            int porta = -1;
                            if (!this.portasDisponiveis.isEmpty())
                                porta = this.portasDisponiveis.remove(0);
                            this.l.unlock();

                            // Se houver portas disponiveis atende-se o pedido do cliente
                            if(porta>0)
                                this.getFicheirosServidor(pedido, porta, portaPedido);
                        }
                    }
                    case 'F' -> { // Se recebeu um pedido para finalizar a conexao com o servidor
                        Runnable worker = () -> {
                            if (this.gw.ipValido(pedido.getAddress()) && GeneratePassWord.isAuthenticated(password, fs.getDados())) {
                                this.gw.removeDados(pedido.getAddress().getHostAddress());

                                FS_Chunk_Protocol fsf = new FS_Chunk_Protocol('F', 0, 0, null, 0, null, 'T');
                                DatagramPacket p = new DatagramPacket(fsf.getBytes(), fsf.getBytes().length, pedido.getAddress(), portaPedido);
                                try{
                                    s.send(p);
                                } catch (IOException ignored) {
                                    System.out.println("Impossivel conectar-se com o servidor ("+ pedido.getAddress()+" | "+ portaPedido +")");
                                }
                            }
                        };
                        Thread t = new Thread(worker);
                        t.start();
                    }
                    case 'T' -> { // Se recebeu chuncks com dados de ficheiros
                        Runnable worker = () -> {
                            if(this.gw.ipValido(pedido.getAddress())){
                                //System.out.println("Recebeu (offset: " + fs.getOffset() + ", Nome: " + fs.getNome());
                                this.gw.addParteFicheiro(fs.getNome(), fs.getOffset(), fs.getDados());
                                this.gw.alertaRececaoFicheiro(fs.getNome(), fs.getOffset());
                            }
                        };
                        Thread t = new Thread(worker);
                        t.start();
                    }
                }
            } catch (IOException ignored) {;}
        }
    }
}
