package ua.edu.ukma.candidai.assessment.service;

public interface ResumeContentExtractor {

    String extractText(String resumeUrl, String candidateName);
}
