# syntax=docker/dockerfile:1

FROM gradle:8.10-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 3434
ENTRYPOINT ["java","-jar","app.jar"]
