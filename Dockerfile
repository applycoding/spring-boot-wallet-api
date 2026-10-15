FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system ledger && useradd --system --gid ledger --home /app ledger
COPY --from=build /src/target/ledger-api.jar /app/ledger-api.jar
USER ledger
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/ledger-api.jar"]
