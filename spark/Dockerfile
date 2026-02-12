FROM apache/spark:3.5.0

WORKDIR /opt/spark/app

COPY build/libs/spark-ecommerce-etl-1.0.jar app.jar
COPY data/raw_events.json data/raw_events.json

ENTRYPOINT ["/opt/spark/bin/spark-submit"]