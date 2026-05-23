FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/licencias-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /tmp/uploads
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
