package ua.edu.ukma.candidai.user.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> storage = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        storage.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return storage.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return storage.values().stream()
                .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return storage.values().stream()
                .filter(u -> role.equals(u.getRole()))
                .toList();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }
}
