FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/uploads /app/uploads

RUN mkdir -p /app/uploads
RUN mkdir -p /app/uploads/profile-photos

RUN chmod -R 777 /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
