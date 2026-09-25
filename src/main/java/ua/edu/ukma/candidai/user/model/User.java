package ua.edu.ukma.candidai.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ua.edu.ukma.candidai.user.UserRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @EqualsAndHashCode.Include
    private UUID id;

    @Setter
    private String fullName;

    private String email;

    private String passwordHash;

    private UserRole role;

    @Setter
    private String telegramChatId;

    private Instant createdAt;
}
