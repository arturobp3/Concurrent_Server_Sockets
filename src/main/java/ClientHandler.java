import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private static final int SHUTDOWN_TIMEOUT = 30; //TODO: CAMBIAR A 60
    private static final int DIGITS_INPUT_SIZE = 4; //TODO: CAMBIAR A 9
    private static final String TERMINATE_KEYWORD = "terminate";

    private final Socket clientSocket;
    private final ServerSocket serverSocket;
    private final ThreadPoolExecutor threadPool;
    private final BlockingQueue<String> clientInputsQueue;
    private final Semaphore serverAccess;


    public ClientHandler(Socket clientSocket, ServerSocket serverSocket, ThreadPoolExecutor threadPool,
                         BlockingQueue<String> clientInputsQueue, Semaphore serverAccess) {
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
        this.threadPool = threadPool;
        this.clientInputsQueue = clientInputsQueue;
        this.serverAccess = serverAccess;
    }

    @Override
    public void run() {
        LOGGER.info("Processing a new client on " + Thread.currentThread().getName() );
        LOGGER.info("Thread pool active count: " + threadPool.getActiveCount());
        try (BufferedInputStream reader = new BufferedInputStream(clientSocket.getInputStream())) {
            String clientInput;
            StringBuilder input = new StringBuilder();
            while (!Thread.interrupted()) {
                clientInput = readInputBuffer(reader, input);
                if (clientInput != null) {
                    if (TERMINATE_KEYWORD.equals(clientInput)) {
                        //TODO: que pasa si termina y hay elementos en la cola esperando que no han podido entrar al servidor
                        LOGGER.info("Found 'terminate' keyword");
                        Utils.shutdownAndAwaitTermination(threadPool, SHUTDOWN_TIMEOUT, LOGGER);
                        serverSocket.close();
                    } else if (!isClientInputValid(clientInput)) {
                        //TODO: lanzar socketException??
                        Thread.currentThread().interrupt();
                        clientSocket.close();
                    } else {
                        LOGGER.info("Sending input to be processed");
                        clientInputsQueue.add(Utils.stripLeadingZeros(clientInput));
                    }
                    input = new StringBuilder();
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error while managing the client running on " + Thread.currentThread().getName() + ": " + e.getMessage());
        } finally {
            LOGGER.info("Releasing access to the server");
            serverAccess.release();
        }
    }

    private String readInputBuffer(BufferedInputStream bufferedInputStream, StringBuilder stringBuilder) throws IOException {
        boolean inputFound = false;

        // read until a single byte is available
        while(bufferedInputStream.available() > 0 && !inputFound) {
            char c = (char) bufferedInputStream.read();
            if (c == '\b') {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            } else {
                stringBuilder.append(c);
            }
            if (StringUtils.endsWith(stringBuilder.toString(), System.lineSeparator())) {
                inputFound = true;
            }
        }
        if (inputFound){
            return Utils.removeEscapeCharacters(stringBuilder.toString());
        }
        return null;
    }

    private boolean isClientInputValid(String clientInput) {
        return clientInput != null
                && clientInput.length() == DIGITS_INPUT_SIZE
                && NumberUtils.isDigits(clientInput)
                && Integer.parseInt(clientInput) > 0;
    }


/*
    public static void main(String[] args) throws IOException, InterruptedException {
        InetAddress host = InetAddress.getLocalHost();
        Socket clientSocket = new Socket(host.getHostAddress(), 4000);
        Socket clientSocket2 = new Socket(host.getHostAddress(), 4000);
        OutputStream output = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        Scanner teclado = new Scanner(System.in);
        String text = "";

        while (!"terminate".equals(text)) {
            text = teclado.nextLine();
            writer.println(text);
        }

        clientSocket.close();
        clientSocket2.close();
        output.close();
        writer.close();
        teclado.close();
    }*/


}
