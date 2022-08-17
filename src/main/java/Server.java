import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;


//TODO: Debo quitar el semaforo de esta clase?? probar sin semaforo
public class Server {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int DEFAULT_MAX_CLIENTS = 5;
    private static final int DEFAULT_PORT_NUMBER = 4000;
    private static final int NUM_ADDITIONAL_TASKS = 1;
    // TODO: Escribir comentario: Tareas adicionales para el threadPool. Se mete el logs handler ahi para que se pueda interrumpir al shutdown la pool

    private final int numThreads;
    private final int portNumber;
    private final int maxClients;

    private ServerSocket serverSocket;
    private List<Socket> clientSocketList;
    private ThreadPoolExecutor threadPool;
    private BlockingQueue<String> clientInputsQueue;
    private Semaphore serverAccess;
    private LogFileHandler logFileHandler;


    /* Constructor when no argument is specified when running the program */
    public Server() {
        maxClients = DEFAULT_MAX_CLIENTS;
        numThreads = maxClients + NUM_ADDITIONAL_TASKS;
        portNumber = DEFAULT_PORT_NUMBER;
        commonInitialization();
    }

    /* Constructor to be used if args are specified when running the program */
    public Server(int portNumber, int maxClients) {
        this.maxClients = maxClients;
        this.numThreads = maxClients + NUM_ADDITIONAL_TASKS;
        this.portNumber = portNumber;
        commonInitialization();
    }

    private void commonInitialization() {
        clientInputsQueue = new LinkedBlockingQueue<>();
        serverAccess = new Semaphore(maxClients);
        logFileHandler = new LogFileHandler(clientInputsQueue);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
        clientSocketList = new ArrayList<>();
    }

    public void run() {
        LOGGER.info("Initializing server");
        try {
            serverSocket = new ServerSocket(portNumber);
            LOGGER.info("Adding logFileHandler to thread pool");
            threadPool.submit(logFileHandler);

            while (!Thread.interrupted()) {
                LOGGER.info("Waiting for clients...");
                serverAccess.acquire();
                Socket clientSocket = serverSocket.accept();

                LOGGER.info("Connected client: " + clientSocket.getInetAddress());

                threadPool.submit(new ClientHandler(clientSocket, serverSocket, threadPool, clientInputsQueue, serverAccess));
                clientSocketList.add(clientSocket);
            }
            //TODO: Como interactuan este catch y la funcion !Thread.interrupted?
        } catch (IOException | InterruptedException e) {
            // TODO: Chequear esta condicion
            if (!serverSocket.isClosed()) {
                LOGGER.severe("Unexpected error: " + e.getMessage());
            }

            LOGGER.info("Thread pool is shutdown: " + threadPool.isShutdown());
            LOGGER.info("Thread pool is terminated: " + threadPool.isTerminated());
            LOGGER.info("Thread pool is terminating: " + threadPool.isTerminating());
        }
    }
}
