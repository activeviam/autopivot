# ActiveViam AutoPivot
AutoPivot is a standalone application for online analysis (OLAP) of CSV files.

AutoPivot discovers the structure of CSV files, field separator, column names, column types, and loads data in memory with a high throughput parallel CSV source. AutoPivot exposes the data as a cube with hierarchies and aggregates that can be manipulated in the ActiveUI frontend or directly from the Microsoft Excel Pivot Table, using the XMLA protocol and MDX query language.

This project is built using Spring Boot

## Launching AutoPivot
Build the project with Maven using the standard `mvn clean install` command. This will generate a jar file, which can be run using standard java commands. ActiveUI, ActiveViam's user interface for exploring the cube, will be available from `http://localhost:9090/ui`.

## Performance
The multithreaded CSV source usually parses CSV data at several hundreds of MB/s. Of course this kind of throughput can only be reached with fast storage, a local SSD drive for instance or network storage accessed through a 10Gbps network at least.

AutoPivot is powered by the ActivePivot technology, the in-memory analytical platform developed by ActiveViam. ActivePivot runs on all sizes of hardware, from laptops to large servers with hundreds of cores and tens of terabytes of memory. When used in fire and forget mode, AutoPivot targets files up to a few hundreds of gigabytes.

## CSV Format
AutoPivot expects a standard CSV file, with headers (column names) on the first row.

## Options
The most common options can be set in the `/src/main/resources/application.properties` file. All the supported options can also be passed as JVM parameters such as `-DfileName=data.csv`.

## Tweaking The Project
AutoPivot tries to guess what's in the data and do everything automatically. More generally it illustrates ActivePivot cubes can be configured programmatically and started on the fly, a very powerful concept for ActivePivot developers that can be reused beyond the simple usage of AutoPivot.

Here are some entry points to jump into the code, starting from `src/main/java`:
* `com.av.csv.discover.CSVDiscovery` logic to discover the CSV separator character and the data types of the columns
* `com.av.autopivot.AutoPivotGenerator` logic to create an ActivePivot cube (hierarchies, aggregates...) based on the file format
* `com.av.autopivot.spring` this package contains the Spring configuration of the AutoPivot application
* `src/main/resources/application.properties` options of the AutoPivot application

## Licensing
The code of the AutoPivot application is open source, licensed under the Apache License 2.0. The AutoPivot application depends on the ActivePivot (commercial) software, the ActivePivot jar files distributed by ActiveViam must be available in the maven repository for the application to build. Running the AutoPivot application requires a license for the ActivePivot software. To use the ActiveUI frontend, the ActivePivot license must have the ActiveUI option enabled.
