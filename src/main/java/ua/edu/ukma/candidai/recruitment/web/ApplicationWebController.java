package ua.edu.ukma.candidai.recruitment.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.recruitment.web.dto.ApplyForm;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/vacancies/{vacancyId}")
@RequiredArgsConstructor
public class ApplicationWebController {

    private final ApplicationService applicationService;
    private final UserApi userApi;
    private final VacancyApi vacancyApi;

    @GetMapping("/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String showApplyForm(
            @PathVariable("vacancyId") UUID vacancyId,
            Model model,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : "";
        String candidateName = "";

        if (email != null && !email.isBlank()) {
            candidateName = userApi.getUserNotificationProfileByEmail(email)
                    .map(UserNotificationProfile::fullName)
                    .orElse("");
        }

        populateVacancyDetails(vacancyId, model);
        model.addAttribute("form", new ApplyForm(candidateName, email, "", "", ""));
        return "applications/apply-form";
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String submitApplication(
            @PathVariable("vacancyId") UUID vacancyId,
            @Valid @ModelAttribute("form") ApplyForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            populateVacancyDetails(vacancyId, model);
            return "applications/apply-form";
        }

        UUID candidateId = userApi.getUserNotificationProfileByEmail(authentication.getName())
                .map(UserNotificationProfile::userId)
                .orElse(null);

        if (candidateId == null) {
            bindingResult.reject("application.candidate.notFound", "Candidate profile not found");
            populateVacancyDetails(vacancyId, model);
            return "applications/apply-form";
        }

        String phone = (form.phone() != null && !form.phone().isBlank()) ? form.phone() : null;

        ApplyForVacancyRequest request = new ApplyForVacancyRequest(
                vacancyId,
                candidateId,
                form.candidateName(),
                form.email(),
                phone,
                form.resumeUrl()
        );

        try {
            applicationService.apply(request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Вашу заявку успішно надіслано! Дякуємо за відгук."
            );
            return "redirect:/vacancies";
        } catch (DuplicateResourceException | InvalidStateTransitionException ex) {
            log.warn("Application submission error for vacancy {}: {}", vacancyId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/vacancies";
        }
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('TECH_LEAD', 'RECRUITER', 'ADMIN')")
    public String viewApplications(
            @PathVariable("vacancyId") UUID vacancyId,
            Model model) {

        populateVacancyDetails(vacancyId, model);
        List<ApplicationResponse> applications = applicationService.getApplicationsByVacancy(vacancyId, true);
        model.addAttribute("applications", applications);
        return "applications/list";
    }

    private void populateVacancyDetails(UUID vacancyId, Model model) {
        model.addAttribute("vacancyId", vacancyId);
        try {
            VacancyDetails details = vacancyApi.getVacancyDetails(vacancyId);
            model.addAttribute("vacancyTitle", details != null ? details.title() : "Вакансія");
        } catch (Exception ex) {
            log.debug("Could not resolve vacancy title for {}: {}", vacancyId, ex.getMessage());
            model.addAttribute("vacancyTitle", "Вакансія");
        }
    }
}
