package ua.edu.ukma.candidai.vacancy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.ukma.candidai.vacancy.model.Skill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByNameInIgnoreCase(Collection<String> names);

    boolean existsByNameIgnoreCase(String name);
}
