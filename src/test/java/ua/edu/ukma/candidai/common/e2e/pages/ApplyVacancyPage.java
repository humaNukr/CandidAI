package ua.edu.ukma.candidai.common.e2e.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ApplyVacancyPage {

    private final Page page;

    public ApplyVacancyPage(Page page) {
        this.page = page;
    }

    public void fillForm(String candidateName, String email, String phone, String resumeUrl) {
        page.getByLabel("Повне ім'я").fill(candidateName);
        page.getByLabel("Електронна пошта").fill(email);
        page.getByLabel("Номер телефону").fill(phone);
        page.getByLabel("Посилання на резюме").fill(resumeUrl);
    }

    public void submit() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Надіслати заявку")).click();
    }
}
