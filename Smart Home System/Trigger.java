import java.util.Objects;
public class Trigger {
    public enum Operator { GT, LT, EQ;
        public static Operator fromSymbol(String s) {
            switch (s) {
                case ">": return GT;
                case "<": return LT;
                case "==": case "=": return EQ;
                default: throw new IllegalArgumentException("Unknown operator: " + s);
            }
        }
    }
    private final String metric;
    private final Operator op;
    private final double threshold;
    private final Runnable action;
    public Trigger(String metric, Operator op, double threshold, Runnable action) {
        this.metric = Objects.requireNonNull(metric);
        this.op = Objects.requireNonNull(op);
        this.threshold = threshold;
        this.action = Objects.requireNonNull(action);
    }
    public String getMetric() { return metric; }
    public Operator getOp() { return op; }
    public Runnable getAction() { return action; }
    public boolean evaluate(double value) {
        switch (op) {
            case GT: return value > threshold;
            case LT: return value < threshold;
            case EQ: return Double.compare(value, threshold) == 0;
            default: return false;
        }
    }
    @Override
    public String toString() {
        return String.format("{condition: \"%s %s %.2f\"}", metric, op, threshold);
    }
}
