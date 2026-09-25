package ua.edu.ukma.candidai.user.repository;

import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(UUID id);

    List<User> findByRole(UserRole role);

    List<User> findAll();
}
