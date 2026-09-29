package ua.edu.ukma.candidai.vacancy.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import ua.edu.ukma.candidai.vacancy.model.Skill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

@DataJpaTest
class SkillRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    @DisplayName("findByNameIgnoreCase - should find skill ignoring case")
    void givenExistingSkill_findByNameIgnoreCase_shouldReturnSkillIgnoringCase() {
        Skill skill = aSkillJava();
        entityManager.persistAndFlush(skill);

        Optional<Skill> actual = skillRepository.findByNameIgnoreCase("jAvA");

        assertThat(actual).isPresent();
        assertThat(actual.get()).usingRecursiveComparison().isEqualTo(skill);
    }

    @Test
    @DisplayName("findByNameInIgnoreCase - should find all matching skills")
    void givenMultipleSkills_findByNameInIgnoreCase_shouldReturnMatchingSkills() {
        Skill java = aSkillJava();
        Skill docker = aSkillDocker();
        entityManager.persist(java);
        entityManager.persist(docker);
        entityManager.flush();

        List<Skill> actual = skillRepository.findByNameInIgnoreCase(List.of("JAVA", "docker", "Kubernetes"));

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(List.of(java, docker));
    }

    @Test
    @DisplayName("existsByNameIgnoreCase - should return true when skill exists")
    void givenExistingSkill_existsByNameIgnoreCase_shouldReturnTrue() {
        Skill skill = aSkillJava();
        entityManager.persistAndFlush(skill);

        boolean actual = skillRepository.existsByNameIgnoreCase("JAVA");

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("save - duplicate name should throw DataIntegrityViolationException")
    void givenDuplicateSkillName_save_shouldThrowDataIntegrityViolationException() {
        Skill skill1 = aSkillJava();
        Skill skill2 = Skill.builder()
                .id(UUID.randomUUID())
                .name(SKILL_JAVA)
                .build();
        entityManager.persistAndFlush(skill1);

        assertThatThrownBy(() -> {
            skillRepository.saveAndFlush(skill2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
