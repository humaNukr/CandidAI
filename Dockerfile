FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY candidai-spring-boot-starter/build.gradle.kts ./candidai-spring-boot-starter/build.gradle.kts

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon

COPY config ./config
COPY candidai-spring-boot-starter/src ./candidai-spring-boot-starter/src
COPY src ./src

RUN ./gradlew bootJar -x test -x e2eTest -x checkstyleMain -x checkstyleTest --no-daemon \
    && cp build/libs/CandidAI-*.jar /workspace/candidai.jar

FROM eclipse-temurin:25-jre-noble AS runner

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid "${APP_GID}" candidai \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" --no-create-home --shell /usr/sbin/nologin candidai \
    && mkdir -p /app/logs /app/uploads/resumes \
    && chown -R candidai:candidai /app/logs /app/uploads

WORKDIR /app

COPY --from=builder /workspace/candidai.jar /app/candidai.jar

USER candidai:candidai

EXPOSE 8080

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/candidai.jar"]
