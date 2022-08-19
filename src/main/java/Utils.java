import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Utils {

    /* Private constructor to avoid creating objects */
    private Utils() {
    }

    /* The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks,
        and then calling shutdownNow, if necessary, to cancel any lingering tasks. Ref: Oracle docs */
    public static void shutdownAndAwaitTermination(ThreadPoolExecutor pool, Integer shutdownTimeoutSeconds) {
        System.out.println("Shutting down thread pool");
        pool.shutdown();
        try {
            System.out.println("Waiting for existing tasks to terminate");
            if (!pool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                pool.shutdownNow();

                System.out.println("Waiting for tasks to respond to being cancelled");
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

    public static String stripLeadingZeros(String number) {
        if (NumberUtils.isDigits(number)) {
            int clientNumber = Integer.parseInt(number);
            return Integer.toString(clientNumber);
        } else {
            throw new NumberFormatException("Error while trying to strip the following input data: " + number);
        }
    }

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
