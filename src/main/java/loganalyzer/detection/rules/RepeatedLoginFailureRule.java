package loganalyzer.detection.rules;

import loganalyzer.detection.DetectionRule;
import loganalyzer.detection.Finding;
import loganalyzer.parser.LogEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatedLoginFailureRule implements DetectionRule {
    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\w.:-])(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
                    + "(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}(?![\\w.]|:[0-9A-Fa-f]*:)");
    private static final Pattern FAILURE = Pattern.compile(
            "(?i)(failed|failure|invalid|denied).{0,30}(login|log.?in|authentication|password)"
                    + "|(login|log.?in|authentication).{0,30}(failed|failure|invalid|denied)");

    private final int threshold;
    private final Duration window;

    public RepeatedLoginFailureRule(int threshold, Duration window) {
        if (threshold < 2) throw new IllegalArgumentException("Threshold must be at least 2");
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Window must be positive");
        }
        this.threshold = threshold;
        this.window = window;
    }

    @Override
    public String name() { return "Failed login attempts"; }

    @Override
    public List<Finding> evaluate(List<LogEvent> events) {
        List<Attempt> attempts = events.stream()
                .filter(event -> event.timestamp() != null)
                .filter(event -> FAILURE.matcher(event.message()).find())
                .map(event -> new Attempt(event, findIpv4(event.message())))
                .filter(attempt -> attempt.ip() != null)
                .sorted(Comparator.comparing(attempt -> attempt.event().timestamp()))
                .toList();

        List<Finding> findings = new ArrayList<>();
        Map<String, ArrayDeque<Instant>> recent = new HashMap<>();
        for (Attempt current : attempts) {
            ArrayDeque<Instant> times = recent.computeIfAbsent(current.ip(), key -> new ArrayDeque<>());
            Instant now = current.event().timestamp();
            while (!times.isEmpty() && Duration.between(times.peekFirst(), now).compareTo(window) > 0) times.removeFirst();
            times.addLast(now);
            int matchingAttempts = times.size();
            String severity;
            String title;
            String description;

            if (matchingAttempts == 1) {
                severity = "LOW";
                title = "Failed login attempt from " + current.ip();
                description = "First failed login attempt from this IPv4 address. Event: "
                        + current.event().message();
            } else if (matchingAttempts == 2 && threshold != 2) {
                severity = "MEDIUM";
                title = "Second failed login attempt from " + current.ip();
                description = "A second failed login from this IPv4 address occurred within "
                        + windowLabel() + ". Event: " + current.event().message();
            } else if (matchingAttempts == threshold) {
                severity = "HIGH";
                title = "Repeated failed logins from " + current.ip();
                description = matchingAttempts + " failed login attempts from this IPv4 address occurred within "
                        + windowLabel() + ". Latest event: " + current.event().message();
            } else {
                continue;
            }

            LogEvent event = current.event();
            findings.add(new Finding(name(), severity, title, description, event.timestamp(), event.source()));
        }
        return findings;
    }

    private static String findIpv4(String message) {
        Matcher matcher = IPV4.matcher(message);
        return matcher.find() ? matcher.group() : null;
    }

    private String windowLabel() {
        long minutes = window.toMinutes();
        if (minutes > 0 && window.equals(Duration.ofMinutes(minutes))) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        long seconds = window.toSeconds();
        if (seconds > 0 && window.equals(Duration.ofSeconds(seconds))) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }
        return window.toString();
    }

    private record Attempt(LogEvent event, String ip) { }
}
