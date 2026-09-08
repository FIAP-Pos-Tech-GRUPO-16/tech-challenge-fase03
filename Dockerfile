# Build único para os três serviços: um estágio de build compila o reactor Maven completo
# (módulos comuns + os três serviços), e cada serviço tem seu próprio estágio final, enxuto,
# contendo apenas o seu jar. `docker compose build` seleciona o estágio via `target:`.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common ./common
COPY scheduling-service ./scheduling-service
COPY notification-service ./notification-service
COPY history-service ./history-service

RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS scheduling-service
WORKDIR /app
COPY --from=build /workspace/scheduling-service/target/scheduling-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre AS notification-service
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre AS history-service
WORKDIR /app
COPY --from=build /workspace/history-service/target/history-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
