FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/legacy-order-service-1.0.0.jar app.jar
COPY target/dependency/ dependency/
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
