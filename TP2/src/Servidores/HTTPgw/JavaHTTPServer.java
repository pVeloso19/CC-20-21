package Servidores.HTTPgw;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class JavaHTTPServer implements Runnable{

    static final String DEFAULT_FILE = "index.html";

    // modo verbose (falso por omissão)
    static final boolean verbose = false;

    // Conexao cliente via Socket
    private Socket connect;
    private HttpGw gw;

    public JavaHTTPServer(HttpGw gw, Socket c) {
        connect = c;
        this.gw = gw;
    }

    @Override
    public void run() {

        BufferedReader in = null; PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            fileRequested = parse.nextToken().split("/")[1];

            if (!method.equals("GET")  &&  !method.equals("HEAD")) {
                if (verbose) {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }

                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Trabalho CC Grupo 8 : 1000.0");
                out.println("Date: " + new Date());
                out.println("Connection: Close");
                out.println();
                out.flush();

            } else {
                // GET ou HEAD metodo
                if (fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }

                if(this.gw.existeFicheiro(fileRequested)) {

                    String content = getContentType(fileRequested);

                    if (method.equals("GET")) { // GET metedo
                        byte[] fileData = readFileData(fileRequested);

                        out.println("HTTP/1.1 200 OK");
                        out.println("Server: Trabalho CC Grupo 8 : 1000.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + content);
                        out.println("Content-length: " + fileData.length);
                        out.println();
                        out.flush();

                        dataOut.write(fileData, 0, fileData.length);
                        dataOut.flush();
                    }

                    if (verbose) {
                        System.out.println("File " + fileRequested + " of type " + content + " returned");
                    }

                }else{
                    fileNotFound(out, fileRequested);
                }
            }

        } catch (FileNotFoundException fnfe) {
            fileNotFound(out, fileRequested);
        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close();
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
            this.gw.limpaFicheiro(fileRequested);
        }
    }

    private byte[] readFileData(String nomeFicheiro) throws FileNotFoundException{

        this.gw.iniciaTransferenciaFich(nomeFicheiro);

        long tamFich = this.gw.getTamFicheiro(nomeFicheiro);
        List<String> ipsServersComFich = this.gw.getServers(nomeFicheiro);
        int tamNomeFich = nomeFicheiro.getBytes().length;
        int maxPacote = 1008 - tamNomeFich;
        int numPedidos = (int) Math.round(((double)tamFich / maxPacote)+0.5d);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numPedidos; i++) {

            int of = i * maxPacote;

            ObtemChuncksDados obd = new ObtemChuncksDados(of,nomeFicheiro,ipsServersComFich,i,this.gw,1);
            Thread t = new Thread(obd);
            t.start();

            threads.add(t);
        }

        for (Thread t : threads){
            try {
                t.join(); //espera receber o chuncks de dados
            } catch (InterruptedException ignored) {;}
        }

        return this.gw.criaFicheiro(nomeFicheiro);
    }

    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
            return "text/html";
        else{
            String[] formatoT = fileRequested.split("\\.");
            String formato = formatoT[formatoT.length-1];
            return "application/"+formato;
        }
    }

    private void fileNotFound(PrintWriter out, String fileRequested){

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Trabalho CC Grupo 8 : 1000.0");
        out.println("Date: " + new Date());
        out.println("Connection: Close");
        out.println();
        out.flush();

        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }

}