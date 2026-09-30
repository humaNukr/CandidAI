package ua.edu.ukma.candidai.recruitment.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.dto.response.InterviewFeedbackResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;
import ua.edu.ukma.candidai.recruitment.model.InterviewFeedback;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
interface ApplicationMapper {

    ApplicationResponse toResponse(Application application);

    @Mapping(target = "applicationId", source = "application.id")
    InterviewFeedbackResponse toResponse(InterviewFeedback feedback);

    List<InterviewFeedbackResponse> toFeedbackResponseList(List<InterviewFeedback> feedbacks);
}
