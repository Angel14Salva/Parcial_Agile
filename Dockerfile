FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/licencias-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /tmp/uploads
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Duser.timezone=America/Lima", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
