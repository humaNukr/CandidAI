package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class VacancyListPage {

    private final Page page;

    public VacancyListPage(Page page) {
        this.page = page;
    }

    public void navigate() {
        page.navigate("/vacancies");
    }

    public Locator createVacancyLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("+ Створити вакансію"));
    }

    public void clickCreateVacancy() {
        createVacancyLink().click();
    }

    public Locator row(String title) {
        return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(title));
    }

    public void clickApplyForVacancy(String title) {
        row(title).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Подати заявку")).click();
    }

    public void clickViewApplicationsForVacancy(String title) {
        row(title).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Заявки")).click();
    }

    public Locator alert() {
        return page.getByRole(AriaRole.ALERT);
    }

    public void logout() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Вийти")).click();
    }
}
