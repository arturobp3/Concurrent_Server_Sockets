import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class LogFileHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(LogFileHandler.class.getName());
    private static final String LOGS_FILE_NAME = "numbers.log";
    private static final Integer MAX_NUM_UNIQUE_INPUTS = 1_000_000_000;
    private static final Integer TEN_SECONDS_IN_MILLISECONDS = 5_000; //TODO: CAMBIAR A 10
    private final Lock lock;
    private final Timer timer;
    private final BlockingQueue<String> clientInputsQueue;
    private final BitSet receivedNumbers;
    private Integer uniqueNumbers;
    private Integer uniqueNumbersTotal;
    private Integer duplicatedNumbers;

    public LogFileHandler(BlockingQueue<String> clientInputsQueue) {
        deleteFileIfExists();
        this.clientInputsQueue = clientInputsQueue;
        receivedNumbers = new BitSet(MAX_NUM_UNIQUE_INPUTS);
        lock = new ReentrantLock();
        uniqueNumbers = 0;
        uniqueNumbersTotal = 0;
        duplicatedNumbers = 0;
        timer = new Timer();
        timer.scheduleAtFixedRate(new ReportTimerTask(), TEN_SECONDS_IN_MILLISECONDS, TEN_SECONDS_IN_MILLISECONDS);
    }

    @Override
    public void run() {
        try (FileWriter fileWriter = new FileWriter(LOGS_FILE_NAME, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            LOGGER.info("Creating file 'numbers.log'");
            LOGGER.info("Reading numbers from queue");
            while (!Thread.interrupted()) {
                String number = clientInputsQueue.take();
                synchronized (lock) {
                    if (receivedNumbers.get(Integer.parseInt(number))) {
                        duplicatedNumbers++;
                    } else {
                        uniqueNumbers++;
                        uniqueNumbersTotal++;
                        receivedNumbers.set(Integer.parseInt(number));
                        bufferedWriter.write(number);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        LOGGER.info("A number has been written in log file");
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error while managing 'numbers.log' file: " + e.getMessage());
        } catch (InterruptedException e) { //Take() exception
            LOGGER.info("LogFileHandler thread has been interrupted");
            timer.cancel();
            timer.purge();
        }
    }

    private void deleteFileIfExists() {
        File file = new File(LOGS_FILE_NAME);
        if (file.exists()) {
            if (file.delete()) {
                LOGGER.info("'numbers.log' already existed and it has been deleted");
            } else {
                LOGGER.warning("'numbers.log' may not have been deleted properly");
            }
        }
    }


    final class ReportTimerTask extends TimerTask {
        @Override
        public void run() {
            synchronized (lock) {
                LOGGER.info("Received " + uniqueNumbers + " unique numbers, " + duplicatedNumbers + " duplicates. " +
                        "Unique total: " + uniqueNumbersTotal);
                uniqueNumbers = 0;
                duplicatedNumbers = 0;
            }

        }
    }



}