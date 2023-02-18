ARG VERSION=14

FROM adoptopenjdk:${VERSION}-jdk as BUILD

COPY . /src
WORKDIR /src
RUN ./gradlew --no-daemon shadowJar

FROM adoptopenjdk:${VERSION}-jre

COPY --from=BUILD /src/build/libs/*.jar /bin/runner/debot.jar

WORKDIR /bin/runner

CMD ["java","-jar","debot.jar"]