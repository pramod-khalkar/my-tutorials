
### How to run locally this spark application
- Install `spark-submit` locally
- Then build `./gradlew clean build`
- run project using below command
```
spark-submit --class com.example.etl.EcommerceETL --master "local[*]" build/libs/spark-ecommerce-etl-1.0.jar
```