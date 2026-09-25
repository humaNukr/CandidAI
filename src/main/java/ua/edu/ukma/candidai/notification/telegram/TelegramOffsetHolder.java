package ua.edu.ukma.candidai.notification.telegram;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelegramOffsetHolder {

    private final AtomicLong offset = new AtomicLong(0);

    public long getOffset() {
        return offset.get();
    }

    public void setOffset(long newOffset) {
        offset.set(newOffset);
    }
}
