# syntax=docker/dockerfile:1
# Build Maven puis image runtime légère (JRE seulement).

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /src

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /src/target/auth-server-*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
