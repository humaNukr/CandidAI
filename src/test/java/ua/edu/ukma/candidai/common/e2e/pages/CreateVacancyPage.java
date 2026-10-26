package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;

public class CreateVacancyPage {

    private final Page page;

    public CreateVacancyPage(Page page) {
        this.page = page;
    }

    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/vacancies/new");
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
        page.locator("#title").fill(title);
        page.locator("#category").selectOption(category);
        page.locator("#specialization").fill(specialization);
        page.locator("#seniorityLevel").fill(seniorityLevel);
        page.locator("#minYearsOfExperience").fill(minYears);
        page.locator("#requiredSkills").fill(requiredSkills);
        page.locator("#salaryMin").fill(salaryMin);
        page.locator("#salaryMax").fill(salaryMax);
        page.locator("#currency").fill("USD");
        page.locator("#description").fill(description);
    }

    public void submit() {
        page.locator("#btn-submit-vacancy").click();
    }
}
