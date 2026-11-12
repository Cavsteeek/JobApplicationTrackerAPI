# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
# Windows checkouts can leave CRLF line endings that break the script on Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q dependency:go-offline

COPY src src
# Tests are skipped here: the integration tests need Docker, which isn't available inside a build
RUN ./mvnw -q package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]