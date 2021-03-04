FROM openjdk:11-jre-slim
COPY ./build/libs/podlister*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
