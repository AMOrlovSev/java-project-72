FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY app/ .

RUN chmod +x gradlew
RUN ./gradlew clean build

CMD ["./gradlew", "run"]