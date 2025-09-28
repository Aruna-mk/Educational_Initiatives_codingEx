public class DoorLockDevice extends AbstractDevice {
    public DoorLockDevice(int id) {
        super(id, "DoorLock");
        this.locked = true; // default locked
    }

    @Override
    public synchronized void turnOn() throws DeviceException {
        locked = false; // "on" equals unlocked
        logger.info("Door " + id + " unlocked.");
    }

    @Override
    public synchronized void turnOff() throws DeviceException {
        locked = true;
        logger.info("Door " + id + " locked.");
    }
}
