package ua.edu.ukma.candidai.vacancy.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.vacancy.TestResources.*;

class VacancyTest {

    @Test
    @DisplayName("addSkill - should add skill to vacancy skill list")
    void givenNewSkill_addSkill_shouldAddToList() {
        Vacancy vacancy = aVacancy();
        Skill springBoot = Skill.builder().id(SKILL_2_ID).name("Spring Boot").build();

        vacancy.addSkill(springBoot);

        assertThat(vacancy.getSkills()).contains(springBoot);
    }

    @Test
    @DisplayName("removeSkill - should remove skill from vacancy skill list")
    void givenExistingSkill_removeSkill_shouldRemoveFromList() {
        Vacancy vacancy = aVacancy();
        Skill skillToRemove = aSkillJava();

        vacancy.removeSkill(skillToRemove);

        assertThat(vacancy.getSkills()).doesNotContain(skillToRemove);
    }

    @Test
    @DisplayName("updateStatus - valid transition from OPEN to CLOSED should succeed")
    void givenValidStatus_updateStatus_shouldUpdateStatusAndTimestamp() {
        Vacancy vacancy = aVacancy();
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);

        vacancy.updateStatus(VacancyStatus.CLOSED, updatedAt);

        assertThat(vacancy.getStatus()).isEqualTo(VacancyStatus.CLOSED);
        assertThat(vacancy.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("updateStatus - invalid transition from CLOSED to OPEN should throw InvalidStateTransitionException")
    void givenInvalidTransition_updateStatus_shouldThrowInvalidStateTransitionException() {
        Vacancy vacancy = aVacancy(VacancyStatus.CLOSED);
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);

        assertThatThrownBy(() -> vacancy.updateStatus(VacancyStatus.OPEN, updatedAt))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition vacancy status from CLOSED to OPEN");
    }

    @Test
    @DisplayName("softDelete - should mark deleted as true and set deletedAt timestamp")
    void givenActiveVacancy_softDelete_shouldMarkDeleted() {
        Vacancy vacancy = aVacancy();
        Instant deletedAt = DEFAULT_NOW.plusSeconds(7200);

        vacancy.softDelete(deletedAt);

        assertThat(vacancy.isDeleted()).isTrue();
        assertThat(vacancy.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(vacancy.getUpdatedAt()).isEqualTo(deletedAt);
    }
}
