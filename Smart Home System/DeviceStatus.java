public final class DeviceStatus {
    private final boolean on;
    private final boolean locked;
    private final Double temperature; 
    public DeviceStatus(boolean on, boolean locked, Double temperature) {
        this.on = on;
        this.locked = locked;
        this.temperature = temperature;
    }
    public boolean isOn() { return on; }
    public boolean isLocked() { return locked; }
    public Double getTemperature() { return temperature; }
    @Override
    public String toString() {
        if (temperature != null) {
            return String.format("Thermostat: %s, Temp=%.1f", on ? "On" : "Off", temperature);
        }
        if (locked) {
            return "Locked: " + locked;
        }
        return "On: " + on;
    }
}
