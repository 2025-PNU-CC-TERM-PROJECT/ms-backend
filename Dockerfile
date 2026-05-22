# 1단계: 빌드
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . /app
RUN gradle build --no-daemon --stacktrace

# 2단계: 실행
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
