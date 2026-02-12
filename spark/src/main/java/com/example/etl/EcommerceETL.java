package com.example.etl;

import org.apache.spark.sql.*;
import static org.apache.spark.sql.functions.*;

/**
 * How to run it
 *
 * spark-submit \
 *   --class com.example.etl.EcommerceETL \
 *   --master local[*] \
 *   build/libs/spark-ecommerce-etl-1.0.jar
 */

public class EcommerceETL {
    public static void main(String[] args) {

        SparkSession spark = SparkSession.builder()
                .appName("Ecommerce ETL")
                .master("local[*]")
                .getOrCreate();

        Dataset<Row> rawDf = spark.read()
                .option("inferSchema", true)
                .json("data/raw_events.json");

        Dataset<Row> cleanDf = rawDf
                .filter(col("user_id").isNotNull())
                .filter(col("event_type").isNotNull());

        Dataset<Row> enrichedDf = cleanDf
                .withColumn("event_timestamp", to_timestamp(col("event_time")))
                .withColumn("event_date", to_date(col("event_time")));

        enrichedDf.createOrReplaceTempView("events");

        Dataset<Row> dailyRevenue = spark.sql(
                "SELECT event_date, SUM(price) AS revenue " +
                "FROM events WHERE event_type = 'purchase' " +
                "GROUP BY event_date"
        );

        Dataset<Row> productSales = spark.sql(
                "SELECT product_id, COUNT(*) AS purchases " +
                "FROM events WHERE event_type = 'purchase' " +
                "GROUP BY product_id"
        );

        Dataset<Row> activeUsers = spark.sql(
                "SELECT event_date, COUNT(DISTINCT user_id) AS active_users " +
                "FROM events GROUP BY event_date"
        );

        dailyRevenue.write()
                .mode("overwrite")
                .partitionBy("event_date")
                .parquet("output/daily_revenue");

        productSales.write()
                .mode("overwrite")
                .parquet("output/product_sales");

        activeUsers.write()
                .mode("overwrite")
                .partitionBy("event_date")
                .parquet("output/active_users");

        spark.stop();
    }
}
