package ua.edu.ukma.candidai.recruitment.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ApplyForm(
        @NotBlank(message = "{application.candidateName.required}")
        @Size(max = 100, message = "{application.candidateName.size}")
        String candidateName,

        @NotBlank(message = "{application.email.required}")
        @Email(message = "{application.email.invalid}")
        @Size(max = 150, message = "{application.email.size}")
        String email,

        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "{application.phone.invalid}")
        String phone,

        @NotBlank(message = "{application.resumeUrl.required}")
        @URL(message = "{application.resumeUrl.invalid}")
        @Size(max = 500, message = "{application.resumeUrl.size}")
        String resumeUrl,

        @Size(max = 2000, message = "{application.coverLetter.size}")
        String coverLetter
) {
    public static ApplyForm empty() {
        return new ApplyForm("", "", "", "", "");
    }
}
