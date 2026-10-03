package ua.edu.ukma.candidai.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.regex.Pattern;

public class MaskingMessageConverter extends CompositeConverter<ILoggingEvent> {

    private static final Pattern SENSITIVE_KV_PATTERN = Pattern.compile(
            "(?i)([\"']?(?:password|passwd|secret|passwordHash|token|apiKey|api-key)[\"']?\\s*[:=]\\s*[\"']?)"
                    + "([^\"'\\s,}\\]]+)([\"']?)"
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
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = SENSITIVE_KV_PATTERN.matcher(message)
                .replaceAll("$1" + MASKED_VALUE + "$3");
        masked = BEARER_TOKEN_PATTERN.matcher(masked)
                .replaceAll(BEARER_MASKED_REPLACEMENT);
        masked = PHONE_NUMBER_PATTERN.matcher(masked)
                .replaceAll(PHONE_MASKED_REPLACEMENT);

        return masked;
    }
}
