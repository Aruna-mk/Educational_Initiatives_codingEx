public class DeviceFactory {
    public Device createDevice(DeviceSpec spec) {
        if (spec == null) throw new IllegalArgumentException("DeviceSpec cannot be null");
        switch (spec.type.toLowerCase()) {
            case "light":
                return new LightDevice(spec.id);
            case "thermostat":
                return new ThermostatDevice(spec.id, spec.initialTemperature);
            case "door":
            case "doorlock":
                return new DoorLockDevice(spec.id);
            default:
                throw new IllegalArgumentException("Unknown device type: " + spec.type);
        }
    }
}
