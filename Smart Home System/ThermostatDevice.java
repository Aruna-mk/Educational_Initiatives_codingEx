public class ThermostatDevice extends AbstractDevice {
    public ThermostatDevice(int id, double initialTemp) {
        super(id, "Thermostat");
        this.temperature = initialTemp;
    }

    @Override
    public synchronized void turnOn() throws DeviceException {
        on = true;
        logger.info("Thermostat " + id + " turned ON.");
    }

    @Override
    public synchronized void turnOff() throws DeviceException {
        on = false;
        logger.info("Thermostat " + id + " turned OFF.");
    }

    @Override
    public synchronized void setTemperature(double temp) {
        this.temperature = temp;
        logger.info("Thermostat " + id + " temperature set to " + temp);
    }

    @Override
    public void update(Subject subject, Object data) {
        // Thermostat might react to hub broadcasts - left extensible.
    }
}
