package ua.edu.ukma.candidai.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.NON_EXISTENT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.SECOND_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUser;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserBuilder;

class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    @DisplayName("save should store and return user, findById should retrieve it")
    void shouldSaveAndFindById() {
        User user = sampleUser();

        User saved = repository.save(user);

        assertThat(saved).isEqualTo(user);
        assertThat(repository.findById(DEFAULT_USER_ID)).contains(user);
    }

    @Test
    @DisplayName("findById should return empty when user not found or id is null")
    void shouldReturnEmptyWhenNotFoundOrIdIsNull() {
        assertThat(repository.findById(NON_EXISTENT_USER_ID)).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findByEmail should return user when email matches case-insensitively")
    void shouldFindByEmail() {
        User user = sampleUser();
        repository.save(user);

        Optional<User> found = repository.findByEmail(DEFAULT_EMAIL);
        Optional<User> foundUpper = repository.findByEmail(DEFAULT_EMAIL.toUpperCase());

        assertThat(found).contains(user);
        assertThat(foundUpper).contains(user);
    }

    @Test
    @DisplayName("findByEmail should return empty when email not found or null")
    void shouldReturnEmptyWhenEmailNotFoundOrNull() {
        assertThat(repository.findByEmail("nonexistent@example.com")).isEmpty();
        assertThat(repository.findByEmail(null)).isEmpty();
    }

    @Test
    @DisplayName("findAll should return all stored users")
    void shouldFindAll() {
        User user1 = sampleUser();
        User user2 = sampleUserBuilder()
                .id(SECOND_USER_ID)
                .email("second@example.com")
                .build();

        repository.save(user1);
        repository.save(user2);

        List<User> all = repository.findAll();

        assertThat(all).containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    @DisplayName("save should throw IllegalArgumentException when user or id is null")
    void shouldThrowExceptionWhenSavingNullOrNullId() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User and its id must not be null");

        User userWithNullId = sampleUserBuilder().id(null).build();
        assertThatThrownBy(() -> repository.save(userWithNullId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User and its id must not be null");
    }
}
