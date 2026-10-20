package fr.validatesteps;

/** Never treat null or negative Health Connect aggregates as a successful read. */
public final class StepValuePolicy {
    private StepValuePolicy() {}

    public static boolean valid(Long value) {
        return value != null && value >= 0L;
    }

    public static String status(Long value) {
        return valid(value) ? "steps_read" : "invalid_steps_data";
    }

    public static long storedCount(Long value, String status) {
        return "steps_read".equals(status) && valid(value) ? value : -1L;
    }

    public static String storedStatus(Long value, String status) {
        return "steps_read".equals(status) && !valid(value)
            ? "invalid_steps_data" : status;
    }
}
