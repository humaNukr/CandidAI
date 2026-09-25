package ua.edu.ukma.candidai.notification.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramOffsetHolderTest {

    @Test
    @DisplayName("getOffset should return default value 0 and updated value after setOffset")
    void givenOffsetHolder_whenSettingOffset_shouldReturnUpdatedValue() {
        TelegramOffsetHolder holder = new TelegramOffsetHolder();

        assertThat(holder.getOffset()).isZero();

        holder.setOffset(42L);

        assertThat(holder.getOffset()).isEqualTo(42L);
    }
}
