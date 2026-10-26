package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;

public class ApplyVacancyPage {

    private final Page page;

    public ApplyVacancyPage(Page page) {
        this.page = page;
    }

    public void fillForm(String candidateName, String email, String phone, String resumeUrl) {
        page.locator("#candidateName").fill(candidateName);
        page.locator("#email").fill(email);
        page.locator("#phone").fill(phone);
        page.locator("#resumeUrl").fill(resumeUrl);
    }

    public void submit() {
        page.locator("form[action*='/apply'] button[type='submit']").click();
    }
}
