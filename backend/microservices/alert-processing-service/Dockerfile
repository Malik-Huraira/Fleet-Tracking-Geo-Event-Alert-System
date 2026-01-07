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
COPY alert-processing-service ./alert-processing-service
RUN mvn package -f alert-processing-service/pom.xml -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Add libstdc++ for RocksDB (Kafka Streams state store)
RUN apk add --no-cache libstdc++
COPY --from=build /app/alert-processing-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
