# Etapa 1: Compilar la aplicación
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
# Descargar dependencias para aprovechar la caché de Docker
RUN mvn dependency:go-offline -B
COPY src ./src
# Compilar el JAR omitiendo los tests para acelerar el build en GCP
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
