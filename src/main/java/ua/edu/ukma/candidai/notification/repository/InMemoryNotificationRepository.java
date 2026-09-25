package ua.edu.ukma.candidai.notification.repository;

import org.springframework.stereotype.Repository;
import ua.edu.ukma.candidai.notification.model.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<UUID, Notification> storage = new ConcurrentHashMap<>();

    @Override
    public Notification save(Notification notification) {
        storage.put(notification.id(), notification);
        return notification;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Notification> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Notification> findByRecipientId(UUID recipientId) {
        return storage.values().stream()
                .filter(notification -> recipientId.equals(notification.recipientId()))
                .toList();
    }
}
