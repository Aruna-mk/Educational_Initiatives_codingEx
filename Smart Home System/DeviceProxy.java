import java.util.Objects;
import java.util.logging.*;
public class DeviceProxy implements Device {
    private final Device real;
    private final Logger logger = Logger.getLogger(DeviceProxy.class.getName());
    private volatile long lastAccess = 0;
    private DeviceProxy(Device real) {
        this.real = Objects.requireNonNull(real);
    }

    public static Device createProxy(Device real) {
        return new DeviceProxy(real);
    }

    @Override
    public int getId() { return real.getId(); }

    @Override
    public String getType() { return real.getType(); }

    @Override
    public DeviceStatus getStatus() { return real.getStatus(); }

    @Override
    public void turnOn() throws DeviceException {
        throttle();
        try {
            real.turnOn();
        } catch (DeviceException e) {
            logger.log(Level.WARNING, "Device operation failed: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void turnOff() throws DeviceException {
        throttle();
        try {
            real.turnOff();
        } catch (DeviceException e) {
            logger.log(Level.WARNING, "Device operation failed: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void setTemperature(double temp) throws DeviceException {
        throttle();
        real.setTemperature(temp);
    }

    @Override
    public void update(Subject subject, Object data) {
        real.update(subject, data);
    }

    private void throttle() throws DeviceException {
        long now = System.currentTimeMillis();
        if (now - lastAccess < 50) { // simple rate limit
            throw new DeviceException("Too many requests to device " + getId());
        }
        lastAccess = now;
    }

    @Override
    public String toString() {
        return "Proxy[" + real.toString() + "]";
    }
}
