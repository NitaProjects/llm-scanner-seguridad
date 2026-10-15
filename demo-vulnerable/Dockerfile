FROM eclipse-temurin:21.0.8_9-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src/main src/main
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre-jammy
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/demo-vulnerable-0.0.1-SNAPSHOT.jar /app/demo.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/demo.jar"]
