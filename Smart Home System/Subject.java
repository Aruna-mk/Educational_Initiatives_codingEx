public interface Subject {
    void registerDevice(Device d);
    void unregisterDevice(Device d);
    void notifyAllDevices(Object data);
}
public interface Observer {
    void update(Subject subject, Object data);
}
