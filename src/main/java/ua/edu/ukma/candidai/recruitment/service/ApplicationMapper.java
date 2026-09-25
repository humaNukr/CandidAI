package ua.edu.ukma.candidai.recruitment.service;

import org.mapstruct.Mapper;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.model.Application;

@Mapper(config = GlobalMapperConfig.class)
interface ApplicationMapper {

    ApplicationResponse toResponse(Application application);
}
