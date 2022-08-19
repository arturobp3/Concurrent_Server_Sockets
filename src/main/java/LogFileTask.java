import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class LogFileTask {

    private static final String LOGS_FILE_NAME = "numbers.log";
    private static final Integer MAX_NUM_UNIQUE_INPUTS = 1_000_000_000;
    private static final Integer TEN_SECONDS = 10;
    private static final int SHUTDOWN_TIMEOUT = 20;
    private final Lock lock;
    private final BlockingQueue<String> clientInputsQueue;
    private final BitSet receivedNumbers;
    private Integer uniqueNumbers;
    private Integer uniqueNumbersTotal;
    private Integer duplicatedNumbers;
    private ScheduledThreadPoolExecutor threadPool;

    /**
     * Constructor to create an object which is responsible for handling the logic related to the saving of logs.
     * It also starts a timer that logs stats related to the information that must be saved.
     * @param clientInputsQueue BlockingQueue which contains the information to be saved in the log file
     */
    public LogFileTask(BlockingQueue<String> clientInputsQueue) {
        deleteFileIfExists();
        this.clientInputsQueue = clientInputsQueue;
        receivedNumbers = new BitSet(MAX_NUM_UNIQUE_INPUTS);
        lock = new ReentrantLock();
        uniqueNumbers = 0;
        uniqueNumbersTotal = 0;
        duplicatedNumbers = 0;
        threadPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    }

    /**
     * It initializes the execution of the thread pool by submitting a task to pick data from the queue and adding a
     * timed task to print stats about the data received
     */
    public void run() {
        threadPool.submit(this::execute);
        threadPool.scheduleWithFixedDelay(this::printReport, TEN_SECONDS, TEN_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * It handles the execution of saving logs and logging stats about these savings. It uses a lock to avoid race conditions
     * when generating the stats about the received numbers.
     */
    private void execute() {
        try (FileWriter fileWriter = new FileWriter(LOGS_FILE_NAME, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            System.out.println("Numbers are going to be read from queue and written to file");
            while (!Thread.interrupted()) {
                String number = clientInputsQueue.take();
                synchronized (lock) {
                    if (receivedNumbers.get(Integer.parseInt(number))) {
                        duplicatedNumbers++;
                    } else {
                        uniqueNumbers++;
                        uniqueNumbersTotal++;
                        receivedNumbers.set(Integer.parseInt(number));
                        try {
                            bufferedWriter.write(number);
                        } catch (IOException e) {
                            System.out.println("There was an error while writing in the file. A number is going to be queued again");
                            clientInputsQueue.add(number);
                            continue;
                        }
                        bufferedWriter.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error while managing 'numbers.log' file: " + e.getMessage());
        } catch (InterruptedException e) { //Take() exception
            System.out.println("LogFileTask thread has been interrupted");
        }
    }

    /**
     * It is responsible for shutting down the thread pool for handling the queue and the timed task.
     */
    public void close() {
        System.out.println("Closing LogFileTask");
        Utils.shutdownAndAwaitTermination(threadPool, SHUTDOWN_TIMEOUT);
    }

    /**
     * It runs a method which logs stats about the numbers received by the clients. It uses a lock to avoid race conditions when
     * the variables are modified.
     */
    public void printReport() {
        synchronized (lock) {
            System.out.println("Received " + uniqueNumbers + " unique numbers, " + duplicatedNumbers + " duplicates. " +"Unique total: " + uniqueNumbersTotal);
            uniqueNumbers = 0;
            duplicatedNumbers = 0;
        }
    }

    /**
     * Method that checks if the file 'numbers.log' exists, and deletes it if so.
     */
    private void deleteFileIfExists() {
        File file = new File(LOGS_FILE_NAME);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("'numbers.log' already existed and it has been deleted");
            } else {
                System.out.println("'numbers.log' may not have been deleted properly");
            }
        }
    }
}