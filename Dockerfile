# syntax=docker/dockerfile:1

# --- Etapa de build (compila el JAR) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Cachea el repo local de Maven para builds más rápidos
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# --- Etapa de runtime (imagen liviana) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copia cualquier JAR construido (fat/uber jar)
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
