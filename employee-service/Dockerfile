FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Darum Microservices"
LABEL service="employee-service"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY target/employee-service-*.jar app.jar

EXPOSE 9002

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
