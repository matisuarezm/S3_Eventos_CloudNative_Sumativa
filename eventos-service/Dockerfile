# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom para aprovechar la cache de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Las pruebas ya corren en el job "test" del pipeline
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen final liviana ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Usuario sin privilegios (buena practica de seguridad)
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/app.jar app.jar
USER app
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
