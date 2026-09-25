package ua.edu.ukma.candidai.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    void givenValidUser_saveAndFindById_shouldStoreAndRetrieveUser() {
        User user = sampleUser();

        User saved = repository.save(user);

        assertThat(saved).isEqualTo(user);
        assertThat(repository.findById(DEFAULT_USER_ID)).contains(user);
    }

    @Test
    @DisplayName("findById should return empty when user not found")
    void givenNonExistentId_findById_shouldReturnEmpty() {
        Optional<User> foundNonExistent = repository.findById(NON_EXISTENT_USER_ID);

        assertThat(foundNonExistent).isEmpty();
    }

    @Test
    @DisplayName("findByEmail should return user when email matches case-insensitively")
    void givenMatchingEmail_findByEmail_shouldReturnUserCaseInsensitively() {
        User user = sampleUser();
        repository.save(user);

        Optional<User> found = repository.findByEmail(DEFAULT_EMAIL);
        Optional<User> foundUpper = repository.findByEmail(DEFAULT_EMAIL.toUpperCase());

        assertThat(found).contains(user);
        assertThat(foundUpper).contains(user);
    }

    @Test
    @DisplayName("findByEmail should return empty when email not found")
    void givenNonExistentEmail_findByEmail_shouldReturnEmpty() {
        Optional<User> foundNonExistent = repository.findByEmail("nonexistent@example.com");

        assertThat(foundNonExistent).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail should return true when user with email exists case-insensitively")
    void givenExistingEmail_existsByEmail_shouldReturnTrue() {
        User user = sampleUser();
        repository.save(user);

        boolean exists = repository.existsByEmail(DEFAULT_EMAIL.toUpperCase());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail should return false when email does not exist")
    void givenNonExistentEmail_existsByEmail_shouldReturnFalse() {
        boolean existsNonExistent = repository.existsByEmail("nonexistent@example.com");

        assertThat(existsNonExistent).isFalse();
    }

    @Test
    @DisplayName("deleteById should remove user when id exists")
    void givenExistingId_deleteById_shouldRemoveUser() {
        User user = sampleUser();
        repository.save(user);

        repository.deleteById(DEFAULT_USER_ID);

        assertThat(repository.findById(DEFAULT_USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("deleteById should do nothing when id does not exist")
    void givenNonExistentId_deleteById_shouldDoNothing() {
        repository.deleteById(NON_EXISTENT_USER_ID);

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findByRole should return users matching role")
    void givenMatchingRole_findByRole_shouldReturnMatchingUsers() {
        User recruiter = sampleUserBuilder().id(DEFAULT_USER_ID).role(UserRole.RECRUITER).build();
        User candidate = sampleUserBuilder()
                .id(SECOND_USER_ID)
                .role(UserRole.CANDIDATE)
                .email("cand@example.com")
                .build();
        repository.save(recruiter);
        repository.save(candidate);

        List<User> recruiters = repository.findByRole(UserRole.RECRUITER);

        assertThat(recruiters).containsExactly(recruiter);
    }

    @Test
    @DisplayName("findByRole should return empty list when no users match role")
    void givenNoMatchingRole_findByRole_shouldReturnEmptyList() {
        User recruiter = sampleUserBuilder().id(DEFAULT_USER_ID).role(UserRole.RECRUITER).build();
        repository.save(recruiter);

        List<User> admins = repository.findByRole(UserRole.ADMIN);

        assertThat(admins).isEmpty();
    }

    @Test
    @DisplayName("findAll should return all stored users")
    void givenMultipleUsers_findAll_shouldReturnAllUsers() {
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
}
