import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogMonitor - A simple Java CLI tool for:
 *   1) Analyzing log files (counts by level, error extraction)
 *   2) Monitoring system resources (CPU load, memory usage)
 *
 * Usage:
 *   java LogMonitor --log <path-to-log-file>
 *   java LogMonitor --system
 *   java LogMonitor --log <path-to-log-file> --system
 *   java LogMonitor --log <path-to-log-file> --watch <seconds>
 */
public class LogMonitor {

    // Matches typical log lines like: 2026-09-16 10:15:23 ERROR Something failed
    private static final Pattern LOG_LEVEL_PATTERN =
            Pattern.compile("\\b(ERROR|WARN|WARNING|INFO|DEBUG|TRACE|FATAL)\\b");

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);

        if (options.isEmpty()) {
            printUsage();
            return;
        }

        if (options.containsKey("log")) {
            analyzeLogFile(options.get("log"));
        }

        if (options.containsKey("system")) {
            printSystemStats();
        }

        if (options.containsKey("watch")) {
            int seconds;
            try {
                seconds = Integer.parseInt(options.get("watch"));
            } catch (NumberFormatException e) {
                System.err.println("Invalid value for --watch, expected an integer number of seconds.");
                return;
            }
            watchSystem(seconds);
        }
    }

    // ---------------------------------------------------------------
    // Argument parsing
    // ---------------------------------------------------------------
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--log") && i + 1 < args.length) {
                options.put("log", args[++i]);
            } else if (arg.equals("--system")) {
                options.put("system", "true");
            } else if (arg.equals("--watch") && i + 1 < args.length) {
                options.put("watch", args[++i]);
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            }
        }
        return options;
    }

    private static void printUsage() {
        System.out.println("LogMonitor - Log analysis & system monitoring CLI");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java LogMonitor --log <path>            Analyze a log file");
        System.out.println("  java LogMonitor --system                Show current CPU/memory usage");
        System.out.println("  java LogMonitor --log <path> --system   Do both");
        System.out.println("  java LogMonitor --watch <seconds>       Continuously monitor system stats");
    }

    // ---------------------------------------------------------------
    // Log file analysis
    // ---------------------------------------------------------------
    private static void analyzeLogFile(String path) {
        Map<String, Integer> levelCounts = new HashMap<>();
        int totalLines = 0;

        System.out.println("=== Log Analysis: " + path + " ===");

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                Matcher matcher = LOG_LEVEL_PATTERN.matcher(line);
                if (matcher.find()) {
                    String level = matcher.group(1).toUpperCase();
                    levelCounts.merge(level, 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read log file: " + e.getMessage());
            return;
        }

        System.out.println("Total lines: " + totalLines);
        if (levelCounts.isEmpty()) {
            System.out.println("No recognizable log levels found.");
        } else {
            System.out.println("Breakdown by level:");
            levelCounts.forEach((level, count) ->
                    System.out.printf("  %-8s : %d%n", level, count));
        }
        System.out.println();
    }

    // ---------------------------------------------------------------
    // System monitoring
    // ---------------------------------------------------------------
    private static void printSystemStats() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        double loadAverage = osBean.getSystemLoadAverage();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        System.out.println("=== System Stats ===");
        System.out.println("Available processors : " + osBean.getAvailableProcessors());
        System.out.println("System load average   : " +
                (loadAverage < 0 ? "n/a on this platform" : String.format("%.2f", loadAverage)));
        System.out.printf("JVM memory used        : %d MB / %d MB (max %d MB)%n",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024));
        System.out.println();
    }

    private static void watchSystem(int intervalSeconds) {
        System.out.println("Watching system stats every " + intervalSeconds + "s. Press Ctrl+C to stop.");
        System.out.println();
        try {
            while (true) {
                printSystemStats();
                Thread.sleep(intervalSeconds * 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Stopped.");
        }
    }
}
