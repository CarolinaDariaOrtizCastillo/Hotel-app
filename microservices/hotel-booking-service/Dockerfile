# Etapa 1: Build con Maven y JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen de ejecución liviana con JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8082
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
