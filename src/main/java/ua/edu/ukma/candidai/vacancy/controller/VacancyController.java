package ua.edu.ukma.candidai.vacancy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Vacancies", description = "Operations related to job vacancy management")
@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;

    @Operation(summary = "Create a new vacancy", description = "Publishes a new job vacancy with requirements")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vacancy created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<VacancyResponse> createVacancy(@RequestBody @Valid CreateVacancyRequest request) {
        VacancyResponse created = vacancyService.createVacancy(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get all vacancies", description = "Retrieves a paginated list of vacancies with filters")
    @ApiResponse(responseCode = "200", description = "List of vacancies retrieved successfully")
    @GetMapping
    public Page<VacancyResponse> getAllVacancies(
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(required = false) JobCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return vacancyService.getAllVacancies(status, category, pageable);
    }

    @Operation(summary = "Get vacancy by ID", description = "Retrieves detailed information about a specific vacancy")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vacancy found"),
            @ApiResponse(responseCode = "404", description = "Vacancy not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public VacancyResponse getVacancyById(@PathVariable UUID id) {
        return vacancyService.getVacancyById(id);
    }

    @Operation(summary = "Update vacancy status", description = "Transitions vacancy to a new status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Vacancy not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public VacancyResponse updateVacancyStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVacancyStatusRequest request
    ) {
        return vacancyService.updateVacancyStatus(id, request);
    }

    @Operation(summary = "Delete vacancy", description = "Deletes a vacancy by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vacancy deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vacancy not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVacancy(@PathVariable UUID id) {
        vacancyService.deleteVacancy(id);
        return ResponseEntity.noContent().build();
    }
}
