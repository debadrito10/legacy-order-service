FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/legacy-order-service-*.jar app.jar
COPY target/dependency/ dependency/
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
