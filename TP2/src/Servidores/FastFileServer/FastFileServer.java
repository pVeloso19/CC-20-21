package Servidores.FastFileServer;

import Servidores.FS_Chunk_Protocol;
import Servidores.GeneratePassWord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FastFileServer {
    private Map<String,Long> ficheiros;

    private final String ipHTTPgw;
    private final int portaGW;
    private final String pasta;

    private DatagramSocket s;

    private Map<String,byte[]> ficheirosBytes;

    private final String password = "CC_2021_TP2_PL1_G08";

    public FastFileServer(String ip, String p, int porta) {
        this.ipHTTPgw = ip;
        this.portaGW = porta;
        this.pasta = p;
        this.ficheiros = new HashMap<>();
        this.ficheirosBytes = new HashMap<>();

        // Le que ficehiros estão disponiveis para disponibilizar
        File file = new File(pasta);
        Arrays.stream(file.listFiles())
                .filter(f -> f.isFile())
                .forEach(f -> this.getInfoFich(f));

        // Abre um DatagramaSocket na porta 9888.
        try {
            this.s = new DatagramSocket(9888);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void getInfoFich(File f){
        String s = f.getAbsolutePath();
        s = s.replace('\\','/');
        String[] nomes = s.split("/");
        String nome2 = nomes[nomes.length-1];
        String[] temp = nome2.split("\\.");
        String nome = "";
        for (int i = 0; i<temp.length; i++){
            if((i+1)==temp.length){
                nome = nome + "." + temp[i].toLowerCase();
            }else{
                nome = nome + temp[i];
            }
        }
        this.ficheiros.put(nome,f.length());

        try {
            byte[] file1 = new byte[(int)f.length()];
            String nomeFich = pasta+"/"+ nome;
            FileInputStream input = new FileInputStream(nomeFich);
            input.read(file1);
            this.ficheirosBytes.put(nome,file1);
        }catch(IOException ignored) {;}
    }

    private void iniciarConexao(){
        try{
            //Inicia Conexao
            byte[] passwordCripto = GeneratePassWord.generateHMAC(this.password);
            FS_Chunk_Protocol fs = new FS_Chunk_Protocol('I',0,0,null,passwordCripto.length,passwordCripto,'T');
            byte[] aEnviar = fs.getBytes();
            final DatagramPacket p = new DatagramPacket(aEnviar, aEnviar.length, InetAddress.getByName(ipHTTPgw), this.portaGW);

            Runnable worker2 = () -> {
                while (true) {
                    try {
                        s.send(p);
                    } catch (IOException ignored) {;}

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {;}
                }
            };
            Thread t = new Thread(worker2);
            t.start();

            //Prepara a info a enviar
            String dadosTotaisS = "";
            for (Map.Entry<String,Long> temp : this.ficheiros.entrySet()){
                dadosTotaisS = dadosTotaisS + temp.getKey()+":"+temp.getValue()+" ";
            }
            byte[] dados = dadosTotaisS.getBytes();

            //Recebe pedido de dados
            boolean fim = false;
            while(!fim){
                byte[] aReceber = new byte[1024];
                DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);
                s.receive(pedido);
                t.stop();

                int porta = pedido.getPort();

                fs = new FS_Chunk_Protocol(aReceber);

                if(fs.isLast()){
                    fim = true;
                }
                else{
                    int offsett = fs.getOffset();

                    byte[] d = new byte[968];
                    int j = 0;
                    int i = offsett;
                    for (; i< 968 && i<dados.length; i++){
                        d[j++] = dados[i];
                    }

                    char ultimo = (i==dados.length) ? 'T' : 'F';

                    fs = new FS_Chunk_Protocol('G', offsett,0,null, d.length, d, ultimo);
                    aEnviar = fs.getBytes();

                    DatagramPacket p2 = new DatagramPacket(aEnviar, aEnviar.length, InetAddress.getByName(ipHTTPgw), porta);
                    s.send(p2);
                }
            }
        }catch (IOException ignored){;}
    }

    private void finalizaConeccao(){
        try{
            //Finaliza Conexao
            byte[] passwordCripto = GeneratePassWord.generateHMAC(this.password);
            FS_Chunk_Protocol fs = new FS_Chunk_Protocol('F',0,0,null,passwordCripto.length,passwordCripto,'T');
            byte[] aEnviar = fs.getBytes();
            DatagramSocket ds = new DatagramSocket(9889);
            final DatagramPacket p = new DatagramPacket(aEnviar, aEnviar.length, InetAddress.getByName(ipHTTPgw), this.portaGW);

            Runnable worker2 = () -> {
                while (true) {
                    try {
                        ds.send(p);
                    } catch (IOException ignored) {;}

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {;}
                }
            };
            Thread t = new Thread(worker2);
            t.start();

            //Recebe a confirmação que finalizou a conexao
            boolean fim = false;
            while(!fim){
                byte[] aReceber = new byte[1024];
                DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);
                ds.receive(pedido);

                fs = new FS_Chunk_Protocol(aReceber);

                if(fs.isLast()){
                    t.stop();
                    fim = true;
                }
            }
            ds.close();
        }catch (IOException ignored){;}
    }

    public void run(){
        //Iniciar Conexão
        iniciarConexao();

        Runnable worker3 = () -> {
            while (true){
                try {
                    byte[] aReceber = new byte[1024];
                    DatagramPacket pedido = new DatagramPacket(aReceber, aReceber.length);
                    s.receive(pedido);

                    if(pedido.getAddress().getHostAddress().equals(ipHTTPgw)){

                        Runnable worker = () -> {
                                FS_Chunk_Protocol fs = new FS_Chunk_Protocol(aReceber);

                                System.out.println("Recebeu pedido de ficheiro (offset: " + fs.getOffset() + ", Nome: " + fs.getNome() + ")");

                                int offset = fs.getOffset();
                                String nomeFicheiro = fs.getNome();
                                int tamNomeFich = nomeFicheiro.getBytes().length;
                                int maxPacote = 1008 - tamNomeFich;
                                byte[] dados = new byte[maxPacote];

                                //encher o array de dados
                                try {
                                    int j = 0;
                                    byte[] file = this.ficheirosBytes.get(nomeFicheiro);
                                    for (int i = offset; j < maxPacote && i < this.ficheiros.get(nomeFicheiro); i++) {
                                        dados[j++] = file[i];
                                    }

                                    FS_Chunk_Protocol fsEnviar = new FS_Chunk_Protocol('T', offset, tamNomeFich, nomeFicheiro, dados.length, dados, 'T');
                                    byte[] aEnviar = fsEnviar.getBytes();

                                    DatagramPacket p = new DatagramPacket(aEnviar, aEnviar.length, InetAddress.getByName(ipHTTPgw), this.portaGW);
                                    s.send(p);

                                } catch (IOException ignored) {
                                    System.out.println("Impossivel enviar dados ao HttpGw!");
                                }
                        };
                        new Thread(worker).start();
                    }
                } catch (IOException ignored) {;}
            }
        };
        Thread t = new Thread(worker3);
        t.start();

        Scanner s = new Scanner(System.in);
        boolean entrar = true;
        System.out.println("O servidor está ativo. Presionar a tecla 1 para desconectar-se.");
        while (entrar){
            String input = s.next();
            entrar = input.compareToIgnoreCase("1\n") == 0;

            if(!entrar){
                System.out.println("A finalizar a conecção");
                finalizaConeccao();
                this.s.close();
                t.stop();
                System.out.println("Conecção finalizada");
            }
        }
    }

}
