# Stage 1: Build
FROM gradle:8.14.1-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/mesoql-*.jar mesoql.jar
ENTRYPOINT ["java", "-jar", "mesoql.jar"]
