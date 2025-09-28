public interface Device extends Observer {
    int getId();
    String getType();
    DeviceStatus getStatus();
    void turnOn() throws DeviceException;
    void turnOff() throws DeviceException;
    void setTemperature(double temp) throws DeviceException; // no-op for non-thermostat
}
