public class DeviceSpec {
    public final int id;
    public final String type;
    public final double initialTemperature;

    public DeviceSpec(int id, String type, double initialTemperature) {
        this.id = id;
        this.type = type;
        this.initialTemperature = initialTemperature;
    }

    public static DeviceSpec light(int id) { return new DeviceSpec(id, "light", Double.NaN); }
    public static DeviceSpec thermostat(int id, double temp) { return new DeviceSpec(id, "thermostat", temp); }
    public static DeviceSpec door(int id) { return new DeviceSpec(id, "door", Double.NaN); }

    public static DeviceSpec fromSimple(String jsonish) {
        int id = 0;
        String type = "light";
        double temp = 20.0;
        String s = jsonish.replaceAll("[{} ]", "");
        String[] parts = s.split(",");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            String v = kv[1].trim().replaceAll("'", "").replaceAll("\"", "");
            if ("id".equalsIgnoreCase(k)) id = Integer.parseInt(v);
            if ("type".equalsIgnoreCase(k)) type = v;
            if ("temperature".equalsIgnoreCase(k)) temp = Double.parseDouble(v);
        }
        return new DeviceSpec(id, type, temp);
    }
}
