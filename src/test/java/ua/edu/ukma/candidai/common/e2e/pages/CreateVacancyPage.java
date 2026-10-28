package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class CreateVacancyPage {

    private final Page page;

    public CreateVacancyPage(Page page) {
        this.page = page;
    }

    public void navigate() {
        page.navigate("/vacancies/new");
    }

    public void fillForm(
            String title,
            String category,
            String specialization,
            String seniorityLevel,
            String minYears,
            String requiredSkills,
            String salaryMin,
            String salaryMax,
            String description
    ) {
        page.getByLabel("Назва посади").fill(title);
        page.getByLabel("Категорія").selectOption(category);
        page.getByLabel("Спеціалізація").fill(specialization);
        page.getByLabel("Рівень кваліфікації").fill(seniorityLevel);
        page.getByLabel("Мінімум років досвіду").fill(minYears);
        page.getByLabel("Обов'язкові навички").fill(requiredSkills);
        page.getByLabel("Зарплата від").fill(salaryMin);
        page.getByLabel("Зарплата до").fill(salaryMax);
        page.getByLabel("Валюта").fill("USD");
        page.getByLabel("Опис вакансії").fill(description);
    }

    public void submit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Зберегти вакансію")).click();
    }
}
