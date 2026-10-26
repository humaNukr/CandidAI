package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;

public class ApplicationsListPage {

    private final Page page;

    public ApplicationsListPage(Page page) {
        this.page = page;
    }

    public boolean hasCandidateApplication(String candidateEmail) {
        return page.locator("table.table tbody tr", new Page.LocatorOptions().setHasText(candidateEmail)).count() > 0;
    }

    public String getCandidateStatus(String candidateEmail) {
        return page.locator("table.table tbody tr", new Page.LocatorOptions().setHasText(candidateEmail))
                .locator("td:nth-child(5) .badge")
                .innerText()
                .trim();
    }
}
