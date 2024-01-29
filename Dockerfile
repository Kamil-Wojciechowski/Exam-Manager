FROM maven:3.8.4-openjdk-17 as build

COPY ./pom.xml ./pom.xml
COPY ./src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-alpine

COPY --from=build /target/*.jar app.jar

# Use shell form to introduce a delay before starting the app
ENTRYPOINT sh -c 'sleep 5; java -jar app.jar'
