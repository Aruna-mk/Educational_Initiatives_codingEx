import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
public class Hub implements Subject {
    private final Map<Integer, Device> devices = new ConcurrentHashMap<>();
    private final List<Trigger> triggers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService internal = Executors.newSingleThreadScheduledExecutor();
    private final Logger logger = Logger.getLogger(Hub.class.getName());

    public Hub() {
        // Periodic health check or broadcast
        internal.scheduleAtFixedRate(this::healthCheck, 10, 10, TimeUnit.SECONDS);
    }

    private void healthCheck() {
        logger.fine("Hub health check. Devices count: " + devices.size());
    }

    @Override
    public void registerDevice(Device d) {
        if (d == null) throw new IllegalArgumentException("Device cannot be null");
        devices.put(d.getId(), d);
        logger.info("Registered device: " + d);
    }

    @Override
    public void unregisterDevice(Device d) {
        if (d != null) {
            devices.remove(d.getId());
            logger.info("Unregistered device: " + d);
        }
    }

    @Override
    public void notifyAllDevices(Object data) {
        for (Device d : devices.values()) {
            try {
                d.update(this, data);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to notify device " + d.getId(), e);
            }
        }
    }

    public void addTrigger(Trigger t) {
        triggers.add(t);
    }

    public void removeTrigger(Trigger t) {
        triggers.remove(t);
    }

    public String listTriggers() {
        return triggers.toString();
    }

    public void executeCommandOnDevice(int id, DeviceCommand cmd, RetryPolicy retryPolicy) {
        Device d = devices.get(id);
        if (d == null) {
            logger.warning("Device not found: " + id);
            return;
        }
        try {
            retryPolicy.executeWithRetry(() -> {
                switch (cmd) {
                    case TURN_ON:
                        d.turnOn();
                        break;
                    case TURN_OFF:
                        d.turnOff();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported command " + cmd);
                }
                // after state change notify hub subscribers/triggers
                notifyAllDevices(null);
                evaluateTriggers();
                return null;
            });
        } catch (RetryExhaustedException e) {
            logger.log(Level.SEVERE, "Failed to execute command after retries for device " + id, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error executing command: " + e.getMessage(), e);
        }
    }

    private void evaluateTriggers() {
        // For now only temperature-based triggers are implemented
        for (Trigger t : triggers) {
            if ("temperature".equalsIgnoreCase(t.getMetric())) {
                for (Device d : devices.values()) {
                    DeviceStatus st = d.getStatus();
                    Double temp = st.getTemperature();
                    if (temp != null && t.evaluate(temp)) {
                        try {
                            t.getAction().run();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Trigger action failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    public String statusReport() {
        StringBuilder sb = new StringBuilder();
        for (Device d : devices.values()) {
            sb.append(String.format("Device %d (%s) -> %s%n", d.getId(), d.getType(), d.getStatus()));
        }
        return sb.toString();
    }

    public void shutdown() {
        try {
            internal.shutdownNow();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error shutting down hub", e);
        }
    }
}
