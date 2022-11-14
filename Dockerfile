FROM maven:3.8.6-openjdk-18 as builder

WORKDIR /usr/app/
ENV DB_HOST=mydb
COPY . /usr/app
RUN mvn package -Dmaven.test.skip
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app.jar

FROM openjdk:18
ENV DB_HOST=mydb
WORKDIR /usr/app
COPY --from=builder /app.jar /usr/app
EXPOSE 8080

CMD ["java","-jar","app.jar"]
