package ua.edu.ukma.candidai.assessment.service.parser;

import java.io.InputStream;

public interface ResumeParser {

    boolean supports(String contentType);

    String parseText(InputStream inputStream);
}
