package ua.edu.ukma.candidai.company.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.edu.ukma.candidai.company.dto.request.CreateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.request.UpdateCompanyRequest;
import ua.edu.ukma.candidai.company.dto.response.CompanyResponse;
import ua.edu.ukma.candidai.company.dto.response.CompanySummaryResponse;
import ua.edu.ukma.candidai.company.service.CompanyService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Companies", description = "Operations related to company profiles and organizations")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Create new company", description = "Registers a new hiring company in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Company created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Company with this name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CompanyResponse> createCompany(@RequestBody @Valid CreateCompanyRequest request) {
        CompanyResponse created = companyService.createCompany(request);
        return ResponseEntity.created(URI.create("/api/v1/companies/" + created.id())).body(created);
    }

    @Operation(summary = "Get company by ID", description = "Retrieves complete company profile by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public CompanyResponse getCompanyById(@PathVariable UUID id) {
        return companyService.getCompanyById(id);
    }

    @Operation(summary = "List companies", description = "Retrieves all companies or filters by name substring")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Companies list retrieved successfully")
    })
    @GetMapping
    public List<CompanySummaryResponse> getCompanies(
            @RequestParam(name = "name", required = false) String name
    ) {
        return companyService.getCompanies(name);
    }

    @Operation(summary = "Update company profile", description = "Updates details of an existing company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Company not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Company with updated name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public CompanyResponse updateCompany(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCompanyRequest request
    ) {
        return companyService.updateCompany(id, request);
    }

    @Operation(summary = "Delete company", description = "Removes a company from the system")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Company deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable UUID id) {
        companyService.deleteCompany(id);
    }
}
