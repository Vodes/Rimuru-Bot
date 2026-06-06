FROM azul/zulu-openjdk:21-jre-latest

# To make this show up you have to run:
# ./gradlew clean app:shadowCI
COPY ./app/app.jar .

ENTRYPOINT ["java", "-jar", "app.jar"]