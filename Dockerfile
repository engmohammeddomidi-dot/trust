# Combined single-service deployment: builds the React frontend, bundles it into the
# Spring Boot backend's static resources, then packages one runnable jar. Build context
# must be the git root (this file's directory) since it needs both trust-frontend/ and
# trust-backend/.

FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY trust-platform/trust-frontend/package*.json ./
RUN npm ci
COPY trust-platform/trust-frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app
COPY trust-platform/trust-backend/pom.xml ./
RUN mvn -B dependency:go-offline -q
COPY trust-platform/trust-backend/ ./
COPY --from=frontend-builder /app/dist ./src/main/resources/static
RUN mvn -B -DskipTests package -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/trust-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
