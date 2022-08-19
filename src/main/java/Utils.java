import org.apache.commons.lang3.math.NumberUtils;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {

    /**
     * Private constructor to avoid creating objects
     */
    private Utils() {
    }

    /**
     * The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks,
        and then calling shutdownNow, if necessary, to cancel any lingering tasks. Ref: Oracle docs
     */
    public static void shutdownAndAwaitTermination(ThreadPoolExecutor pool, Integer shutdownTimeoutSeconds) {
        System.out.println("Shutting down thread pool");
        pool.shutdown();
        try {
            System.out.println("Waiting for existing tasks to terminate");
            if (!pool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                pool.shutdownNow();

                if (!pool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    System.out.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * It takes a number as parameter and strips its leading zeros
     * @param number String variable which represents a number
     * @return String of the number without leading zeros or NumberFormatException if it couldn't be processed
     */
    public static String stripLeadingZeros(String number) {
        if (NumberUtils.isDigits(number)) {
            int clientNumber = Integer.parseInt(number);
            return Integer.toString(clientNumber);
        } else {
            throw new NumberFormatException("Error while trying to strip the following input data: " + number);
        }
    }

    /**
     * It takes a string as parameter and remove any escape character that might appear on it"
     * @param input String which corresponds with the client input
     * @return Input without escape characters
     */
    public static String removeEscapeCharacters(String input) {
        input = input.replace("\t", "");
        input = input.replace("\b", "");
        input = input.replace("\n", "");
        input = input.replace("\r", "");
        input = input.replace("\f", "");
        input = input.replace("\\'", "");
        input = input.replace("\\\"", "");
        input = input.replace("\\\\", "");
        return input;
    }
}
