package ua.edu.ukma.candidai.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BYTES = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY_KILOBYTES = 16384;
    private static final int ITERATIONS = 3;

    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2PasswordEncoder = new Argon2PasswordEncoder(
                SALT_LENGTH_BYTES,
                HASH_LENGTH_BYTES,
                PARALLELISM,
                MEMORY_KILOBYTES,
                ITERATIONS
        );

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2", argon2PasswordEncoder);

        DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("argon2", encoders);
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(argon2PasswordEncoder);

        return delegatingPasswordEncoder;
    }

    @Bean
    public CommandLineRunner logSamplePasswordHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String samplePassword = "SecretPassword123!";
            String sampleHash = passwordEncoder.encode(samplePassword);
            log.info("Argon2id PasswordEncoder initialized (sample hash: {})", sampleHash);
        };
    }
}
