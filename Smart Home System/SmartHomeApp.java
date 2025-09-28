import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
public class SmartHomeApp {
    private static final Logger logger = Logger.getLogger(SmartHomeApp.class.getName());

    public static void main(String[] args) {
        configureLogging();

        // Create core components
        Hub hub = new Hub();
        SchedulerService scheduler = new SchedulerService(hub);
        RetryPolicy retryPolicy = new RetryPolicy(3, 200, 2.0);

        // Initialize some devices via factory and proxies
        DeviceFactory factory = new DeviceFactory();
        Device light1 = DeviceProxy.createProxy(factory.createDevice(DeviceSpec.light(1)));
        Device thermostat2 = DeviceProxy.createProxy(factory.createDevice(DeviceSpec.thermostat(2, 70)));
        Device door3 = DeviceProxy.createProxy(factory.createDevice(DeviceSpec.door(3)));

        // Register devices with hub (observer pattern)
        hub.registerDevice(light1);
        hub.registerDevice(thermostat2);
        hub.registerDevice(door3);

        // Example scheduling and triggers
        scheduler.scheduleCommand(2, "06:00", () -> {
            logger.info("Scheduled task executing: turnOn(2)");
            thermostat2.turnOn();
        });

        // Add a trigger: when thermostat > 75 --> turn off light 1
        hub.addTrigger(new Trigger("temperature", Trigger.Operator.GT, 75, () -> {
            logger.info("Trigger fired: temperature > 75 -> turnOff(1)");
            safeExecute(() -> light1.turnOff(), retryPolicy);
        }));

        // Interactive command loop using an AtomicBoolean flag 
        AtomicBoolean running = new AtomicBoolean(true);
        Scanner scanner = new Scanner(System.in);

        printHelp();
        while (running.get() && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    running.set(false);
                    break;
                } else if (line.startsWith("turnOn(")) {
                    int id = parseId(line);
                    hub.executeCommandOnDevice(id, DeviceCommand.TURN_ON, retryPolicy);
                } else if (line.startsWith("turnOff(")) {
                    int id = parseId(line);
                    hub.executeCommandOnDevice(id, DeviceCommand.TURN_OFF, retryPolicy);
                } else if (line.startsWith("setSchedule(")) {
                    // Example: setSchedule(2, "06:00", "Turn On")
                    String[] parts = extractCsv(line);
                    int id = Integer.parseInt(parts[0]);
                    String time = stripQuotes(parts[1]);
                    String command = stripQuotes(parts[2]);
                    scheduler.scheduleCommand(id, time, () -> hub.executeCommandOnDevice(id,
                            "Turn On".equalsIgnoreCase(command) ? DeviceCommand.TURN_ON : DeviceCommand.TURN_OFF,
                            retryPolicy));
                    logger.info("Scheduled command added.");
                } else if (line.startsWith("addTrigger(")) {
                    // Example: addTrigger("temperature", ">", 75, "turnOff(1)")
                    String[] parts = extractCsv(line);
                    String metric = stripQuotes(parts[0]);
                    String op = stripQuotes(parts[1]);
                    double threshold = Double.parseDouble(parts[2]);
                    String action = stripQuotes(parts[3]);
                    Runnable actionRunnable = parseAction(action, hub);
                    hub.addTrigger(new Trigger(metric, Trigger.Operator.fromSymbol(op), threshold, actionRunnable));
                    logger.info("Trigger added.");
                } else if (line.startsWith("addDevice(")) {
                    // addDevice({id:4, type:'light'})
                    DeviceSpec spec = DeviceSpec.fromSimple(line.substring(line.indexOf("{")));
                    Device d = DeviceProxy.createProxy(factory.createDevice(spec));
                    hub.registerDevice(d);
                    logger.info("Device added: " + d);
                } else if ("status".equalsIgnoreCase(line)) {
                    System.out.println(hub.statusReport());
                } else if ("scheduled".equalsIgnoreCase(line)) {
                    System.out.println(scheduler.listSchedules());
                } else if ("triggers".equalsIgnoreCase(line)) {
                    System.out.println(hub.listTriggers());
                } else if ("help".equalsIgnoreCase(line)) {
                    printHelp();
                } else {
                    System.out.println("Unknown command. Type 'help' for commands.");
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Command processing failed: " + ex.getMessage(), ex);
            }
        }

        // shutdown
        scheduler.shutdown();
        hub.shutdown();
        scanner.close();
        logger.info("Smart Home App stopped.");
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for (Handler h : handlers) {
            if (h instanceof ConsoleHandler) {
                h.setLevel(Level.INFO);
            }
        }
        root.setLevel(Level.INFO);
    }

    private static void printHelp() {
        System.out.println("Smart Home CLI - commands:");
        System.out.println("turnOn(<id>)  - turn device on");
        System.out.println("turnOff(<id>) - turn device off");
        System.out.println("setSchedule(<id>, \"HH:mm\", \"Turn On\"/\"Turn Off\")");
        System.out.println("addTrigger(\"metric\",\"op\", threshold, \"action\") e.g. addTrigger(\"temperature\", \">\", 75, \"turnOff(1)\")");
        System.out.println("addDevice({id:4, type:'light'})");
        System.out.println("status - show status report");
        System.out.println("scheduled - list scheduled tasks");
        System.out.println("triggers - list triggers");
        System.out.println("help");
        System.out.println("exit");
        System.out.println();
    }

    private static int parseId(String s) {
        String t = s.substring(s.indexOf('(') + 1, s.indexOf(')')).trim();
        return Integer.parseInt(t);
    }

    private static String[] extractCsv(String s) {
        int start = s.indexOf('(') + 1;
        int end = s.lastIndexOf(')');
        String inner = s.substring(start, end);
        // naive csv split, assumes no nested commas in quotes
        List<String> parts = new ArrayList<>();
        int i = 0;
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        while (i < inner.length()) {
            char c = inner.charAt(i);
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
                cur.append(c);
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString().trim());
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
            i++;
        }
        parts.add(cur.toString().trim());
        return parts.toArray(new String[0]);
    }

    private static String stripQuotes(String s) {
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static Runnable parseAction(String action, Hub hub) {
        action = action.trim();
        if (action.startsWith("turnOff(")) {
            int id = Integer.parseInt(action.substring(action.indexOf('(') + 1, action.indexOf(')')));
            return () -> hub.executeCommandOnDevice(id, DeviceCommand.TURN_OFF, new RetryPolicy(3, 100, 2.0));
        } else if (action.startsWith("turnOn(")) {
            int id = Integer.parseInt(action.substring(action.indexOf('(') + 1, action.indexOf(')')));
            return () -> hub.executeCommandOnDevice(id, DeviceCommand.TURN_ON, new RetryPolicy(3, 100, 2.0));
        } else {
            return () -> logger.info("No-op action: " + action);
        }
    }

    private static void safeExecute(Runnable r, RetryPolicy retryPolicy) {
        try {
            retryPolicy.executeWithRetry(() -> {
                r.run();
                return null;
            });
        } catch (RetryExhaustedException e) {
            logger.log(Level.WARNING, "Operation failed after retries: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected failure: " + e.getMessage(), e);
        }
    }
}
