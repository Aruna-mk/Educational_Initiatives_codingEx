import java.util.logging.*;

/**
 * Base class containing common behaviors for devices.
 */
public abstract class AbstractDevice implements Device {
    protected final int id;
    protected final String type;
    protected volatile boolean on = false;
    protected volatile boolean locked = false;
    protected volatile Double temperature = null;
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected AbstractDevice(int id, String type) {
        this.id = id;
        this.type = type;
    }

    public int getId() { return id; }
    public String getType() { return type; }

    public synchronized DeviceStatus getStatus() {
        return new DeviceStatus(on, locked, temperature);
    }

    public void update(Subject subject, Object data) {
        // default no-op. Devices can override if they care about hub changes.
    }

    public String toString() {
        return String.format("%s(id=%d)", type, id);
    }

    public void setTemperature(double temp) throws DeviceException {
        throw new DeviceException("setTemperature unsupported for device: " + type);
    }
}
