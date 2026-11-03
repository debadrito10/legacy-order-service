FROM openjdk:8-jre
COPY target/legacy-order-service-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-XX:+UseConcMarkSweepGC", "-jar", "/app/app.jar"]
