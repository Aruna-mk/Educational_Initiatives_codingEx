import java.util.concurrent.Callable;
import java.util.logging.*;

/**
 * Simple configurable retry policy with exponential backoff for transient errors.
 */
public class RetryPolicy {
    private final int maxAttempts;
    private final long baseDelayMillis;
    private final double multiplier;
    private final Logger logger = Logger.getLogger(RetryPolicy.class.getName());

    public RetryPolicy(int maxAttempts, long baseDelayMillis, double multiplier) {
        if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts > 0");
        if (baseDelayMillis < 0) throw new IllegalArgumentException("baseDelayMillis >= 0");
        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
        this.multiplier = multiplier;
    }

    public <T> T executeWithRetry(Callable<T> task) throws Exception {
        int attempt = 0;
        long delay = baseDelayMillis;
        while (true) {
            try {
                attempt++;
                return task.call();
            } catch (DeviceException de) {
                if (attempt >= maxAttempts) {
                    throw new RetryExhaustedException("Retries exhausted after " + attempt, de);
                }
                logger.info("Transient failure attempt " + attempt + ". Retrying in " + delay + " ms");
                Thread.sleep(delay);
                delay = (long) (delay * multiplier);
            }
        }
    }
}
