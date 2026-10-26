package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/login");
    }

    public void login(String username, String password) {
        page.locator("#username").fill(username);
        page.locator("#password").fill(password);
        page.locator("button[type='submit']").click();
    }

    public boolean isErrorMessageVisible() {
        return page.locator(".alert-danger").isVisible();
    }
}
