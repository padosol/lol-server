FROM bellsoft/liberica-openjdk-alpine:17

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-Xms2048m -Xmx2048m", "-jar","-Dspring.profiles.active=prod", "/app.jar"]