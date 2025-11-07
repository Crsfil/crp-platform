FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY . /build
RUN mvn -q -DskipTests -pl agreement-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/agreement-service/target/agreement-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]

