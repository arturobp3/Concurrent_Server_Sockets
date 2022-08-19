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

    private static final int SHUTDOWN_TIMEOUT = 20;
    private static final int DIGITS_INPUT_SIZE = 9;
    private static final String TERMINATE_KEYWORD = "terminate";

    private final Socket clientSocket;
    private final ServerSocket serverSocket;
    private final ThreadPoolExecutor threadPool;
    private final BlockingQueue<String> clientInputsQueue;
    private final Semaphore serverAccess;


    /**
     * Constructor to create an object which is responsible for handling the logic related to a client's connection
     * @param clientSocket Client Socket obtained when it connects to the server
     * @param serverSocket Server Socket instance
     * @param threadPool Thread pool which contains all clients running on different threads
     * @param clientInputsQueue BlockingQueue to add all numbers which meet the conditions to be saved in the file
     * @param serverAccess Semaphore to be used when a permit must be released.
     */
    public ClientHandler(Socket clientSocket, ServerSocket serverSocket, ThreadPoolExecutor threadPool,
                         BlockingQueue<String> clientInputsQueue, Semaphore serverAccess) {
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
        this.threadPool = threadPool;
        this.clientInputsQueue = clientInputsQueue;
        this.serverAccess = serverAccess;
    }

    /**
     * It handles the execution of a client. It uses a BufferedInputStream which allows to read the client's input
     * asynchronously, by using a buffer, and analyse the input to decide whether to add it to the queue or disconnect
     * all or one client.
     */
    @Override
    public void run() {
        try (BufferedInputStream reader = new BufferedInputStream(clientSocket.getInputStream())) {
            String clientInput;
            StringBuilder input = new StringBuilder();
            while (!Thread.interrupted()) {
                clientInput = readInputBuffer(reader, input);
                if (clientInput != null) {
                    if (TERMINATE_KEYWORD.equals(clientInput)) {
                        System.out.println("Found 'terminate' keyword");
                        Utils.shutdownAndAwaitTermination(threadPool, SHUTDOWN_TIMEOUT);
                        serverSocket.close();
                    } else if (!isClientInputValid(clientInput)) {
                        Thread.currentThread().interrupt();
                        clientSocket.close();
                    } else {
                        clientInputsQueue.add(Utils.stripLeadingZeros(clientInput));
                    }
                    input = new StringBuilder();
                }
            }
        } catch (IOException e) {
            System.out.println("Exception while managing the client running on " + Thread.currentThread().getName() + ": " + e.getMessage());
        } finally {
            System.out.println("Releasing access to the server");
            serverAccess.release();
        }
    }

    /**
     * Method that reads the client's input via a BufferedInputStream and stores every single character read in
     * the StringBuilder variable
     * @param bufferedInputStream BufferedInputStream to read the client's input asynchronously
     * @param stringBuilder StringBuilder to let the method handle the string efficiently
     * @return It returns a Null when the client hasn't introduced a line separator character, and the concrete String
     * when the line separator character is recognized
     */
    private String readInputBuffer(BufferedInputStream bufferedInputStream, StringBuilder stringBuilder) throws IOException {
        boolean inputFound = false;

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

    /**
     * Check if the string clientInput is a valid input. It takes into account the size of the string, if they are digits
     * and if it's 0 or a positive number
     * @param clientInput Input introduced by the client
     * @return True if valid, false otherwise
     */
    private boolean isClientInputValid(String clientInput) {
        return clientInput != null
                && clientInput.length() == DIGITS_INPUT_SIZE
                && NumberUtils.isDigits(clientInput)
                && Integer.parseInt(clientInput) >= 0;
    }
}
