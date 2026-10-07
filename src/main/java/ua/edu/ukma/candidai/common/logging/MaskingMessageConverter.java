package ua.edu.ukma.candidai.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.regex.Pattern;

public class MaskingMessageConverter extends CompositeConverter<ILoggingEvent> {

    private static final Pattern QUOTED_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)([\"']?(?:password|passwd|secret|passwordHash|token|apiKey|api-key)[\"']?\\s*[:=]\\s*)"
                    + "([\"'])((?:\\\\.|(?!\\2).)*)\\2"
    );

    private static final Pattern UNQUOTED_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)([\"']?(?:password|passwd|secret|passwordHash|token|apiKey|api-key)[\"']?\\s*[:=]\\s*)"
                    + "([^\"'\\s,}\\]]+)"
    );

    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(?i)Bearer\\s+[A-Za-z0-9\\-._~+/]+=*"
    );

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
            "(?<=\\s|^|[(\"':])(\\+380|0\\d{2}|\\+\\d{1,3})\\d{3,6}(\\d{4})(?=\\s|$|[)\"',;.]|$)"
    );

    private static final String MASKED_VALUE = "***";
    private static final String BEARER_MASKED_REPLACEMENT = "Bearer ***";
    private static final String PHONE_MASKED_REPLACEMENT = "$1****$2";

    @Override
    public String convert(ILoggingEvent event) {
        if (getChildConverter() == null) {
            return mask(event.getFormattedMessage());
        }
        return super.convert(event);
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return mask(in);
    }

    public String mask(String message) {
        if (message == null || message.isEmpty() || !mightContainSensitiveData(message)) {
            return message;
        }

        String masked = QUOTED_SENSITIVE_PATTERN.matcher(message)
                .replaceAll("$1$2" + MASKED_VALUE + "$2");
        masked = UNQUOTED_SENSITIVE_PATTERN.matcher(masked)
                .replaceAll("$1" + MASKED_VALUE);
        masked = BEARER_TOKEN_PATTERN.matcher(masked)
                .replaceAll(BEARER_MASKED_REPLACEMENT);
        masked = PHONE_NUMBER_PATTERN.matcher(masked)
                .replaceAll(PHONE_MASKED_REPLACEMENT);

        return masked;
    }

    private boolean mightContainSensitiveData(String message) {
        return containsIgnoreCase(message, "pass")
                || containsIgnoreCase(message, "secret")
                || containsIgnoreCase(message, "token")
                || containsIgnoreCase(message, "key")
                || containsIgnoreCase(message, "bearer")
                || message.indexOf('+') >= 0
                || containsPhoneCandidate(message);
    }

    private boolean containsPhoneCandidate(String s) {
        int len = s.length();
        for (int i = 0; i <= len - 10; i++) {
            char c = s.charAt(i);
            if (c == '0' && Character.isDigit(s.charAt(i + 1)) && Character.isDigit(s.charAt(i + 2))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0) {
            return true;
        }

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp) {
                continue;
            }
            if (src.regionMatches(true, i, what, 0, length)) {
                return true;
            }
        }
        return false;
    }
}
