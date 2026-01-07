FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

# Copy the ORIGINAL parent pom (not pom-docker.xml)
COPY pom.xml ./pom.xml

# Install parent pom to local repo (non-recursive, ignores missing modules)
RUN mvn install -N -DskipTests -Dmaven.buildNumber.skip=true || true

# Build common module
COPY common ./common
RUN mvn install -f common/pom.xml -DskipTests

# Build the service
COPY vehicle-tracking-service ./vehicle-tracking-service
RUN mvn package -f vehicle-tracking-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/vehicle-tracking-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
