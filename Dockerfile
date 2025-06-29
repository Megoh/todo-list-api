FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app

COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]