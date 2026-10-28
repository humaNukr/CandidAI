package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public void navigate() {
        page.navigate("/login");
    }

    public void login(String email, String password) {
        page.getByLabel("Електронна пошта").fill(email);
        page.getByLabel("Пароль").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Увійти")).click();
    }

    public Locator alert() {
        return page.getByRole(AriaRole.ALERT);
    }
}
