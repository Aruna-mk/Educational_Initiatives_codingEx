public class LightDevice extends AbstractDevice {
    public LightDevice(int id) { super(id, "Light"); }

    @Override
    public synchronized void turnOn() throws DeviceException {
        if (on) {
            logger.fine("Light " + id + " already on.");
            return;
        }
        on = true;
        logger.info("Light " + id + " turned ON.");
    }

    @Override
    public synchronized void turnOff() throws DeviceException {
        if (!on) {
            logger.fine("Light " + id + " already off.");
            return;
        }
        on = false;
        logger.info("Light " + id + " turned OFF.");
    }
}
