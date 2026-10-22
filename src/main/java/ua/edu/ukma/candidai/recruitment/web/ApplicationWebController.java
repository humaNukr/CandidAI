package ua.edu.ukma.candidai.recruitment.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
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
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.recruitment.web.dto.ApplyForm;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/vacancies/{vacancyId}")
@RequiredArgsConstructor
public class ApplicationWebController {

    private final ApplicationService applicationService;
    private final UserApi userApi;
    private final VacancyApi vacancyApi;
    private final MessageSource messageSource;

    @GetMapping("/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String showApplyForm(
            @PathVariable("vacancyId") UUID vacancyId,
            Model model,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : "";
        String candidateName = "";

        populateVacancyDetails(vacancyId, model);

        if (email != null && !email.isBlank()) {
            Optional<UserNotificationProfile> profileOpt = userApi.getUserNotificationProfileByEmail(email);
            if (profileOpt.isPresent()) {
                UserNotificationProfile profile = profileOpt.get();
                candidateName = profile.fullName() != null ? profile.fullName() : "";
                Optional<ApplicationResponse> existingOpt = applicationService.findCandidateApplication(
                        vacancyId, profile.userId()
                );
                if (existingOpt.isPresent()) {
                    ApplicationResponse existing = existingOpt.get();
                    model.addAttribute("form", new ApplyForm(
                            existing.candidateName(),
                            existing.email(),
                            existing.phone() != null ? existing.phone() : "",
                            existing.resumeUrl() != null ? existing.resumeUrl() : ""
                    ));
                    model.addAttribute("isEdit", true);
                    model.addAttribute("applicationStatus", existing.status().name());
                    model.addAttribute("canEdit", existing.status() == ApplicationStatus.APPLIED);
                    return "applications/apply-form";
                }
            }
        }

        model.addAttribute("isEdit", false);
        model.addAttribute("canEdit", true);
        model.addAttribute("form", new ApplyForm(candidateName, email, "", ""));
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
            Authentication authentication,
            Locale locale) {

        if (bindingResult.hasErrors()) {
            populateVacancyDetails(vacancyId, model);
            populateExistingApplicationState(vacancyId, authentication, model);
            return "applications/apply-form";
        }

        UUID candidateId = userApi.getUserNotificationProfileByEmail(authentication.getName())
                .map(UserNotificationProfile::userId)
                .orElse(null);

        if (candidateId == null) {
            bindingResult.reject("application.candidate.notFound", "Candidate profile not found");
            populateVacancyDetails(vacancyId, model);
            model.addAttribute("isEdit", false);
            model.addAttribute("canEdit", true);
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

        Optional<ApplicationResponse> existingOpt = applicationService.findCandidateApplication(
                vacancyId, candidateId
        );

        if (existingOpt.isPresent()) {
            ApplicationResponse existing = existingOpt.get();
            if (existing.status() != ApplicationStatus.APPLIED) {
                String lockedMessage = messageSource.getMessage(
                        "application.apply.locked",
                        new Object[]{existing.status()},
                        locale
                );
                redirectAttributes.addFlashAttribute("errorMessage", lockedMessage);
                return "redirect:/vacancies";
            }

            applicationService.updateApplication(vacancyId, candidateId, request);
            String updatedMessage = messageSource.getMessage(
                    "application.apply.updated",
                    null,
                    "Вашу заявку успішно оновлено!",
                    locale
            );
            redirectAttributes.addFlashAttribute("successMessage", updatedMessage);
            return "redirect:/vacancies";
        }

        try {
            applicationService.apply(request);
            String successMessage = messageSource.getMessage(
                    "application.apply.success",
                    null,
                    "Вашу заявку успішно надіслано! Дякуємо за відгук.",
                    locale
            );
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/vacancies";
        } catch (DuplicateResourceException ex) {
            log.warn("Application duplicate submission for vacancy {}: {}", vacancyId, ex.getMessage());
            String duplicateMessage = messageSource.getMessage(
                    "application.apply.duplicate",
                    null,
                    "Ви вже подавали заявку на цю вакансію.",
                    locale
            );
            redirectAttributes.addFlashAttribute("errorMessage", duplicateMessage);
            return "redirect:/vacancies";
        } catch (InvalidStateTransitionException ex) {
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

    private void populateExistingApplicationState(UUID vacancyId, Authentication authentication, Model model) {
        String email = authentication != null ? authentication.getName() : null;
        if (email != null && !email.isBlank()) {
            userApi.getUserNotificationProfileByEmail(email)
                    .map(UserNotificationProfile::userId)
                    .flatMap(candidateId -> applicationService.findCandidateApplication(vacancyId, candidateId))
                    .ifPresentOrElse(
                            existing -> {
                                model.addAttribute("isEdit", true);
                                model.addAttribute("applicationStatus", existing.status().name());
                                model.addAttribute("canEdit", existing.status() == ApplicationStatus.APPLIED);
                            },
                            () -> {
                                model.addAttribute("isEdit", false);
                                model.addAttribute("canEdit", true);
                            }
                    );
            return;
        }
        model.addAttribute("isEdit", false);
        model.addAttribute("canEdit", true);
    }

    private void populateVacancyDetails(UUID vacancyId, Model model) {
        model.addAttribute("vacancyId", vacancyId);
        VacancyDetails details = vacancyApi.getVacancyDetails(vacancyId);
        model.addAttribute("vacancyTitle", details != null ? details.title() : "Вакансія");
    }
}

