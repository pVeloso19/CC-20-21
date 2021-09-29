package Servidores.HTTPgw;

import Servidores.FS_Chunk_Protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class ObtemChuncksDados implements Runnable{

    private final int timeOut = 3000;

    private final String ipFastFileServer;
    private final int portaFileFastServer;
    private int offset;
    private String nomeFicheiro;

    private final List<String> ips;
    private int randRobin;

    private HttpGw gw;

    private int profundidade;

    public ObtemChuncksDados(int offset, String nomeFicheiro, List<String> ips, int randRobin, HttpGw gw, int v) {
        this.offset = offset;
        this.nomeFicheiro = nomeFicheiro;
        this.ips = ips;
        this.randRobin = randRobin;
        this.gw = gw;
        this.profundidade = v;
        this.ipFastFileServer = ips.get(this.randRobin % this.ips.size());
        this.portaFileFastServer = this.gw.getPortaServidor(this.ipFastFileServer);
    }

    @Override
    public void run() {

        FS_Chunk_Protocol fs = new FS_Chunk_Protocol('T', offset, nomeFicheiro.length(), nomeFicheiro, 0, null, 'F');
        byte[] aEnviar = fs.getBytes();

        this.gw.obtemLock(nomeFicheiro);
        boolean fim = this.gw.existeChunkFile(fs.getNome(), fs.getOffset());
        this.gw.fazUnlock(nomeFicheiro);

        int numTentativas = 0;
        while (!fim && (numTentativas < 2)) {
            try {

                DatagramSocket ds = new DatagramSocket();
                DatagramPacket p = new DatagramPacket(aEnviar, aEnviar.length, InetAddress.getByName(ipFastFileServer), this.portaFileFastServer);
                ds.send(p);

                this.gw.obtemLock(nomeFicheiro);
                fim = this.gw.existeChunkFile(fs.getNome(), fs.getOffset());
                if(!fim){
                    this.gw.Adormece(nomeFicheiro, timeOut, offset);
                    fim = this.gw.existeChunkFile(fs.getNome(), fs.getOffset());
                }
                this.gw.fazUnlock(nomeFicheiro);
                numTentativas++;

            } catch (IOException | InterruptedException ignored) {;}
        }

        if(!fim && (this.profundidade < this.ips.size())){
            ObtemChuncksDados obd = new ObtemChuncksDados(this.offset,this.nomeFicheiro,this.ips,(++randRobin), this.gw, (++profundidade));
            Thread t = new Thread(obd);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
