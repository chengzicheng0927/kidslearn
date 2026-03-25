# Dockerfile (Maven)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -Pprod

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 非 root 运行
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
