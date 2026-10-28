package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class VacancyListPage {

    private final Page page;

    public VacancyListPage(Page page) {
        this.page = page;
    }

    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/vacancies");
    }

    public void clickCreateVacancy() {
        page.locator("#btn-add-vacancy").click();
    }

    public boolean hasVacancyWithTitle(String title) {
        return page.locator("table.table tbody tr", new Page.LocatorOptions().setHasText(title)).count() > 0;
    }

    public void clickApplyForVacancy(String title) {
        Locator row = page.locator("table.table tbody tr", new Page.LocatorOptions().setHasText(title));
        row.locator("a[href*='/apply']").click();
    }

    public void clickViewApplicationsForVacancy(String title) {
        Locator row = page.locator("table.table tbody tr", new Page.LocatorOptions().setHasText(title));
        row.locator("a[href*='/applications']").click();
    }

    public boolean hasSuccessAlert() {
        return page.locator(".alert-success").isVisible();
    }

    public void logout() {
        page.locator("form[action*='/logout'] button[type='submit']").click();
    }
}
