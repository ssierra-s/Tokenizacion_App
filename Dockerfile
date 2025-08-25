# Etapa de build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa final (runtime)
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/tokenizacion-app-0.0.1-SNAPSHOT.jar app.jar

# Arranca en modo producción
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]
