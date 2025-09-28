import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
public class SchedulerService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final List<ScheduledFuture<?>> tasks = new CopyOnWriteArrayList<>();
    private final Hub hub;
    private final Logger logger = Logger.getLogger(SchedulerService.class.getName());
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

    public SchedulerService(Hub hub) {
        this.hub = Objects.requireNonNull(hub);
    }

    public void scheduleCommand(int deviceId, String timeHHmm, Runnable command) {
        LocalTime now = LocalTime.now();
        LocalTime target = LocalTime.parse(timeHHmm, fmt);
        long initialDelay = Duration.between(now, target).toMillis();
        if (initialDelay < 0) initialDelay += Duration.ofDays(1).toMillis(); // schedule for next day
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                command.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Scheduled command failed", e);
            }
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        tasks.add(future);
        logger.info("Scheduled device " + deviceId + " at " + timeHHmm);
    }

    public String listSchedules() {
        return "Scheduled tasks count: " + tasks.size();
    }

    public void shutdown() {
        try {
            scheduler.shutdownNow();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error shutting down scheduler", e);
        }
    }
}
