package ua.edu.ukma.candidai.vacancy.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.EmploymentType;
import ua.edu.ukma.candidai.vacancy.model.EnglishLevel;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.LocationType;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;
import ua.edu.ukma.candidai.vacancy.web.dto.CreateVacancyForm;
import ua.edu.ukma.candidai.vacancy.web.model.VacancyView;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/vacancies")
@RequiredArgsConstructor
public class VacancyWebController {

    private final VacancyService vacancyService;
    private final UserApi userApi;
    private final MessageSource messageSource;

    @GetMapping
    public String listVacancies(
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(required = false) JobCategory category,
            @RequestParam(defaultValue = "TABLE") VacancyView view,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        Page<VacancyResponse> vacancies = vacancyService.getAllVacancies(status, category, pageable);

        model.addAttribute("vacancies", vacancies);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedView", view);
        model.addAttribute("statuses", VacancyStatus.values());
        model.addAttribute("categories", JobCategory.values());
        model.addAttribute("views", VacancyView.values());

        return switch (view) {
            case CARDS -> "vacancies/cards";
            case TABLE -> "vacancies/list";
        };
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", CreateVacancyForm.empty());
        }
        populateReferenceData(model);
        return "vacancies/create-form";
    }

    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PostMapping
    public String handleCreateVacancy(
            @Valid @ModelAttribute("form") CreateVacancyForm form,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            Locale locale,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            populateReferenceData(model);
            return "vacancies/create-form";
        }

        UUID authorId = Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .flatMap(userApi::getUserNotificationProfileByEmail)
                .map(UserNotificationProfile::userId)
                .orElse(null);

        if (authorId == null) {
            bindingResult.reject("vacancies.author.notFound", "Recruiter profile not found");
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            populateReferenceData(model);
            return "vacancies/create-form";
        }

        vacancyService.createVacancy(form.toRequest(authorId));
        String successMessage = messageSource.getMessage(
                "vacancies.flash.created",
                null,
                "Vacancy successfully created!",
                locale
        );
        redirectAttributes.addFlashAttribute("successMessage", successMessage);

        return "redirect:/vacancies";
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("categories", JobCategory.values());
        model.addAttribute("englishLevels", EnglishLevel.values());
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("locationTypes", LocationType.values());
    }
}
