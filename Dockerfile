FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/worker-enrollment-management-1.0.0.jar worker-enrollment-management-1.0.0.jar
ENTRYPOINT ["java","-jar", "worker-enrollment-management-1.0.0.jar"]
EXPOSE 8080