# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY . .
# Si usas perfil Vaadin production, ajústalo a tu proyecto (por ejemplo -Pproduction)
RUN mvn -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -Dserver.port=${PORT} -jar /app/app.jar"]
