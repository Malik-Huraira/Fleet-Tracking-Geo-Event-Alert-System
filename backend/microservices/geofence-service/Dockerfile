FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

# Copy the ORIGINAL parent pom
COPY pom.xml ./pom.xml

# Install parent pom to local repo
RUN mvn install -N -DskipTests -Dmaven.buildNumber.skip=true || true

# Build common module
COPY common ./common
RUN mvn install -f common/pom.xml -DskipTests

# Build the service
COPY geofence-service ./geofence-service
RUN mvn package -f geofence-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/geofence-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
