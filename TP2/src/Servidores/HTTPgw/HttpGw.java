package Servidores.HTTPgw;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.of;

public class HttpGw {

    private final int porta; // Porta a que o GW está conectado.

    private List<String> servers; //IP dos servidores conectados ao Servidores.HTTPgw

    private Map<String,Integer> portaServidores; // chave = ip do servidor,
                                                 // value = porta em que o servidor está à escuta

    private Map<String, Map<Integer,byte[]>> ficheiros;    // chave = nomeFicheiro
                                                           // value = map com (chave = offset value = bytes do ficheiro)

    private Map<String, List<String>> servidores;   // chave = NomeFicheiro
                                                    // value = lista com os servidores que teem o ficheiro

    private Map<String, Long> dadosFicheiros;    // chave = nomeFicheiro
                                                 // value = tamanho do ficheiro

    private ReentrantLock l; // lock usado para controlo de concorrencia

    private Map<String, Map<Integer,Condition>> ficheirosEspera;

    public HttpGw(int porta) {

        this.porta = porta;

        this.servers = new ArrayList<>();
        this.portaServidores = new HashMap<>();
        this.ficheiros = new HashMap<>();
        this.servidores = new HashMap<>();
        this.dadosFicheiros = new HashMap<>();

        this.ficheirosEspera = new HashMap<>();

        this.l = new ReentrantLock();
    }


    public void run() throws IOException {
        //Criar um thread responsavel por receber inicilizações/finalizações/dados dos servidores
        ConexaoServidores as = new ConexaoServidores(this);
        Thread t = new Thread(as);
        t.start();

        //Cria thread responsavel por receber pedidos e pedir aos servers
        AceitaPedidosCliente a = new AceitaPedidosCliente(this.porta,this);
        Thread t2 = new Thread(a);
        t2.start();
    }

    public int getPorta(){
        try {
            l.lock();
            return this.porta;
        }finally {
            l.unlock();
        }
    }

    public boolean ipValido(InetAddress ip) {
        try{
            this.l.lock();
            return this.servers.contains(ip.getHostAddress());
        }finally {
            this.l.unlock();
        }
    }

    public void addServidorValido(InetAddress address, int porta) {
        try{
            this.l.lock();
            if(!ipValido(address)){
                this.servers.add(address.getHostAddress());
                this.portaServidores.put(address.getHostAddress(),porta);
            }
        }finally {
            this.l.unlock();
        }
    }

    public int getPortaServidor(String ip){
        try {
            this.l.lock();
            return this.portaServidores.get(ip);
        }finally {
            this.l.unlock();
        }
    }

    public void addParteFicheiro(String nome, int offset, byte[] bytes) {
        try{
            this.l.lock();
            if(!this.ficheiros.get(nome).containsKey(offset))
                this.ficheiros.get(nome).put(offset,bytes);
        }finally {
            this.l.unlock();
        }
    }

    public List<String> getServers(String nomeFicheiro) {
        try {
            this.l.lock();
            return this.servidores.get(nomeFicheiro);
        }finally {
            this.l.unlock();
        }

    }

    public long getTamFicheiro(String nomeFicheiro) {
        try {
            this.l.lock();
            return this.dadosFicheiros.get(nomeFicheiro);
        }finally {
            this.l.unlock();
        }

    }

    public byte[] criaFicheiro(String nome) throws FileNotFoundException{
        this.l.lock();
        List<byte[]> t = this.ficheiros.get(nome).entrySet().parallelStream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());
        this.l.unlock();

        long tamFicheiro = this.getTamFicheiro(nome);
        byte[] b = new byte[(int)tamFicheiro];

        if(t.isEmpty()){
            System.out.println("ERRO: Ficheiro incompleto");
            throw new FileNotFoundException();
        }

        int j = 0;
        for (byte[] temp : t){
            if(temp.length > 0){
                for (byte value : temp) {
                    if(j<tamFicheiro)
                        b[j++] = value;
                }
            }else{
                System.out.println("ERRO: Ficheiro incompleto");
                throw new FileNotFoundException();
            }
        }

        return b;
    }

    public void iniciaTransferenciaFich(String nomeFicheiro) {
        try {
            this.l.lock();

            if(!this.ficheiros.containsKey(nomeFicheiro))
                this.ficheiros.put(nomeFicheiro,new HashMap<>());

            if(!this.ficheirosEspera.containsKey(nomeFicheiro))
                this.ficheirosEspera.put(nomeFicheiro, new HashMap<>());

        }finally {
            this.l.unlock();
        }
    }

    public boolean existeChunkFile(String nomeFicheiro, int of) {
        return (this.ficheiros.get(nomeFicheiro).get(of) != null);
    }

    public void insereDadosFicheirosServer(String hostAddress, String dados) {
        try{
            this.l.lock();
            String[] f = dados.split(" ");
            List<String[]> fs = new ArrayList<>();

            for (String temp : f){
                fs.add(temp.split(":"));
            }

            for (String[] temp : fs){
                if(temp.length == 2){
                    String nomeFile = temp[0].trim();
                    Long tam = Long.parseLong(temp[1].trim());
                    //System.out.println(nomeFile);
                    if(this.servidores.containsKey(nomeFile)){
                        if(!this.servidores.get(nomeFile).contains(hostAddress)){

                            this.servidores.get(nomeFile).add(hostAddress);
                        }
                    }else{
                        this.servidores.put(nomeFile,new ArrayList<>());
                        this.servidores.get(nomeFile).add(hostAddress);
                    }
                    this.dadosFicheiros.put(nomeFile,tam);
                }
            }
        }finally {
            this.l.unlock();
        }
    }

    public boolean existeFicheiro(String nomeFile) {
        try {
            this.l.lock();
            if(this.servidores.containsKey(nomeFile))
                return !this.servidores.get(nomeFile).isEmpty();
            else
                return false;
        }finally {
            this.l.unlock();
        }
    }

    public void removeDados(String hostAddress) {
        try {
            this.l.lock();
            boolean r = this.servers.remove(hostAddress);
            if(r){
                this.servidores.values().forEach(l->l.remove(hostAddress));
            }else {
                System.out.println("ERRO! Endereço não existia. Impossivel remover.");
            }
        }finally {
            this.l.unlock();
        }
    }

    public void limpaFicheiro(String nomeFicheiro) {
        try {
            l.lock();
            this.ficheiros.get(nomeFicheiro).clear();
            this.ficheiros.remove(nomeFicheiro);
        }finally {
            l.unlock();
        }
    }

    public void obtemLock(String nomeFicheiro) {
        this.l.lock();
    }

    public void fazUnlock(String nomeFicheiro) {
        this.l.unlock();
    }


    public void Adormece(String nomeFicheiro, int timeout, int off) throws InterruptedException {
        Condition c = this.l.newCondition();
        this.ficheirosEspera.get(nomeFicheiro).put(off,c);
        c.await(timeout, MILLISECONDS);
    }

    public void alertaRececaoFicheiro(String nome, int off) {
        try {
            l.lock();
            Condition c = this.ficheirosEspera.get(nome).get(off);
            if(c!=null)
                c.signalAll();
        }finally {
            this.l.unlock();
        }
    }
}
