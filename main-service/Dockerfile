FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY . .
RUN mvn clean package -DskipTests -pl main-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/main-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]