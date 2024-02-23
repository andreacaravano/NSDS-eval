package it.polimi.nsds.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

/*
 * Group number: 14
 *
 * Group members
 *  - Andrea Caravano
 *  - Biagio Cancelliere
 *  - Marco Pozzi
 */

public class SparkGroup14 {
    private static final int numCourses = 3000;

    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> profsFields = new ArrayList<>();
        profsFields.add(DataTypes.createStructField("prof_name", DataTypes.StringType, false));
        profsFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType profsSchema = DataTypes.createStructType(profsFields);

        final List<StructField> coursesFields = new ArrayList<>();
        coursesFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        coursesFields.add(DataTypes.createStructField("course_hours", DataTypes.IntegerType, false));
        coursesFields.add(DataTypes.createStructField("course_students", DataTypes.IntegerType, false));
        final StructType coursesSchema = DataTypes.createStructType(coursesFields);

        final List<StructField> videosFields = new ArrayList<>();
        videosFields.add(DataTypes.createStructField("video_id", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("video_duration", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType videosSchema = DataTypes.createStructType(videosFields);

        // Professors: prof_name, course_name
        final Dataset<Row> profs = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(profsSchema)
                .csv(filePath + "files/profs.csv");

        // Courses: course_name, course_hours, course_students
        final Dataset<Row> courses = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(coursesSchema)
                .csv(filePath + "files/courses.csv");

        // Videos: video_id, video_duration, course_name
        final Dataset<Row> videos = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(videosSchema)
                .csv(filePath + "files/videos.csv");

        // Visualizations: value, timestamp
        // value represents the video id
        final Dataset<Row> visualizations = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 10)
                .load()
                .withColumn("value", col("value").mod(numCourses));

        /**
         * TODO: Enter your code below
         */

        // We choose not to cache and re-use static (joined) datasets as a single Dataframe is used in Query Q2
        // and course duplication may cause problems in Query Q1 as multiple videos are joined to courses and professors,
        // duplicating resulting entries

        // We chose to cache the Dataframes that are used multiple times (see next queries)
        courses.cache();
        videos.cache();

        /*
         * Query Q1. Compute the total number of lecture hours per prof
         */

        final Dataset<Row> q1 = profs
                .join(courses, profs.col("course_name").equalTo(courses.col("course_name")))
                .groupBy(col("prof_name").as("Professor Name"))
                .agg(sum("course_hours").as("Total hours"));

        q1.show();

        /*
         * Query Q2. For each course, compute the total duration of all the visualizations of videos of that course,
         * computed over a minute, updated every 10 seconds
         */

        final StreamingQuery q2 = visualizations
                .join(videos, videos.col("video_id").equalTo(visualizations.col("value")))
                .groupBy(window(col("timestamp"), "60 seconds", "10 seconds"),
                        col("course_name").as("Course name"))
                .sum("video_duration")
                .writeStream()
                .outputMode("update")
                .format("console")
                .option("truncate", false) // Does not truncate values displayed on the console
                .option("numRows", 30) // Allows to display more rows on the console
                .start();

        /*
         * Query Q3. For each video, compute the total number of visualizations of that video
         * with respect to the number of students in the course in which the video is used.
         */

        final StreamingQuery q3 = visualizations
                .join(videos.as("videos"), videos.col("video_id").equalTo(visualizations.col("value")))
                .join(courses.as("courses"), courses.col("course_name").equalTo(col("videos.course_name")))
                .groupBy(col("videos.video_id"), col("videos.course_name"), col("courses.course_students"))
                .count()
                .select(col("videos.video_id").as("Video id"),
                        format_number(col("count")
                                .divide(col("courses.course_students"))
                                .multiply(100), 3)
                                .as("Percentage of students that visualized the video"))
                .writeStream()
                .outputMode("update")
                .format("console")
                .option("truncate", false) // Does not truncate values displayed on the console
                .option("numRows", 30) // Allows to display more rows on the console
                .start();

        try {
            q2.awaitTermination();
            q3.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }
}