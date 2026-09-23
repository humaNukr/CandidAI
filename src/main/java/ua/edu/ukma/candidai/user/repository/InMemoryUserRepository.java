package ua.edu.ukma.candidai.user.repository;

import org.springframework.stereotype.Repository;
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
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and its id must not be null");
        }
        storage.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }
}
