FROM eclipse-temurin:17-jre-alpine
RUN mkdir /app

WORKDIR /app

ADD optimizator-api-*.jar /app

EXPOSE 8080

CMD java -jar optimizator-api-*.jar
