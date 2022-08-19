import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;


public class Server {

    private static final int DEFAULT_MAX_CLIENTS = 5;
    private static final int DEFAULT_PORT_NUMBER = 4000;
    private final int portNumber;
    private final int maxClients;

    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPool;
    private BlockingQueue<String> clientInputsQueue;
    private Semaphore serverAccess;
    private LogFileTask logFileTask;


    /**
     * Constructor when no argument is specified when running the program
     */
    public Server() {
        maxClients = DEFAULT_MAX_CLIENTS;
        portNumber = DEFAULT_PORT_NUMBER;
        commonInitialization();
    }

    /**
     * Constructor to be used if args are specified when running the program
     *
     * @param portNumber Socket's port number. It is the argument-0 when the program has started.
     * @param maxClients Maximum number of concurrent clients.
     */
    public Server(int portNumber, int maxClients) {
        this.maxClients = maxClients;
        this.portNumber = portNumber;
        commonInitialization();
    }

    /**
     * Initialization that must be done for both constructors
     */
    private void commonInitialization() {
        clientInputsQueue = new LinkedBlockingQueue<>();
        serverAccess = new Semaphore(maxClients);
        logFileTask = new LogFileTask(clientInputsQueue);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxClients);
    }

    /**
     * Start running the server by creating a ServerSocket instance, using the specified port number, and handling a
     * semaphore and a thread pool to keep the desired concurrent clients running.
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(portNumber);
            logFileTask.run();

            while (!Thread.interrupted()) {
                serverAccess.acquire();
                Socket clientSocket = serverSocket.accept();

                System.out.println("Connected client: " + clientSocket.getInetAddress());

                threadPool.execute(new ClientHandler(clientSocket, serverSocket, threadPool, clientInputsQueue, serverAccess));
            }
            // InterruptedException is thrown when there are clients waiting on the semaphore, and the thread has been interrupted
            // IOException for the create and accept methods of the socket
        } catch (InterruptedException | IOException e) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        } finally {
            logFileTask.close();
            System.out.println("Server is shutdown: " + threadPool.isShutdown());
            System.out.println("Server is terminated: " + threadPool.isTerminated());
            System.out.println("Server is terminating: " + threadPool.isTerminating());
        }
    }

    /**
     * Get Port Number
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Get maxClients
     */
    public int getMaxClients() {
        return maxClients;
    }

    /**
     * Get serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * Get threadPool
     */
    public ThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    /**
     * Get serverAccess
     */
    public Semaphore getServerAccess() {
        return serverAccess;
    }

}
