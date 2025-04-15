# Start with base image
FROM eclipse-temurin:21-jre

# Add Maintainer Info
LABEL maintainer="fugary"

# Add a temporary volume
VOLUME /tmp

# Expose Port 9086
EXPOSE 9890

ENV JAVA_OPTS="-Xmx512M"

# Application Jar File
ARG JAR_FILE=openapi-generator-ui/target/openapi-generator-ui*.jar

# Add Application Jar File to the Container
ADD ${JAR_FILE} openapi-generator-ui.jar

# Run the JAR file
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /openapi-generator-ui.jar"]
