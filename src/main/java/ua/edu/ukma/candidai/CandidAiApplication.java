package ua.edu.ukma.candidai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CandidAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidAiApplication.class, args);
    }
}
