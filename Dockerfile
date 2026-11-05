FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY FarmerMarketServer.java .

RUN javac FarmerMarketServer.java

CMD ["java", "FarmerMarketServer"]