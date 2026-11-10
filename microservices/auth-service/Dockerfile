FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -N install
COPY common-library/pom.xml common-library/
COPY common-library/src common-library/src
WORKDIR /workspace/common-library
RUN mvn install -DskipTests
WORKDIR /workspace
COPY auth-service/pom.xml auth-service/
COPY auth-service/src auth-service/src
WORKDIR /workspace/auth-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/auth-service/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx300m -Xss512k -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
