package com.dstorage;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

public class App 
{
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder().appName("Simple Application").getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");
        Dataset files = spark.read().json("jfs://myjfs/user/thinkpad/2023/02/28/");
        
        files.show(40);

    
        spark.stop();
      }
}
