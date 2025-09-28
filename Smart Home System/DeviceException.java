public class DeviceException extends Exception {
    public DeviceException(String message) { super(message); }
    public DeviceException(String message, Throwable cause) { super(message, cause); }
}

public class RetryExhaustedException extends Exception {
    public RetryExhaustedException(String message) { super(message); }
    public RetryExhaustedException(String message, Throwable cause) { super(message, cause); }
}
