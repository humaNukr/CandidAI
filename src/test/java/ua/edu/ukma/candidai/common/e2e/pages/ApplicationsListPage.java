package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ApplicationsListPage {

    private final Page page;

    public ApplicationsListPage(Page page) {
        this.page = page;
    }

    public Locator candidateRow(String candidateEmail) {
        return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(candidateEmail));
    }
}
