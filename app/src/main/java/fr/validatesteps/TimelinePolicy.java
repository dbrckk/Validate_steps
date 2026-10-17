package fr.validatesteps;

/** Pure bounded text history policy: independent of Android and easy to test. */
public final class TimelinePolicy {
    public static final int MAX_EVENTS = 80;
    private TimelinePolicy() {}

    public static String append(String current, String event) {
        if (event == null || event.isEmpty() || event.indexOf('\n') >= 0)
            throw new IllegalArgumentException("An event must be one line");
        String[] lines = current == null || current.isEmpty()
            ? new String[0] : current.split("\n");
        StringBuilder builder = new StringBuilder();
        int first = Math.max(0, lines.length - MAX_EVENTS + 1);
        for (int index = first; index < lines.length; index++) {
            if (!lines[index].isEmpty()) builder.append(lines[index]).append('\n');
        }
        builder.append(event);
        return builder.toString();
    }

    public static String latest(String history, int count) {
        if (history == null || history.isEmpty() || count <= 0) return "aucun événement";
        String[] lines = history.split("\n");
        StringBuilder result = new StringBuilder();
        for (int index = Math.max(0, lines.length - count); index < lines.length; index++) {
            if (result.length() > 0) result.append('\n');
            result.append(lines[index]);
        }
        return result.toString();
    }
}
