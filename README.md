# Atoti AutoPivot
Atoti AutoPivot is a standalone application for online analysis (OLAP) of CSV files.

Atoti AutoPivot discovers the structure of CSV files, field separator, column names, column types, and loads data in memory with a high throughput parallel CSV source. Atoti AutoPivot exposes the data as a cube with hierarchies and metrics that can be manipulated in the Atoti UI frontend or directly from the Microsoft Excel Pivot Table, using the XMLA protocol and MDX query language.

This project is packaged using Spring Boot

## Launching Atoti AutoPivot
Build the project with Maven using the standard `mvn clean install` command. This will generate a jar file, which can be run using standard java commands. Atoti UI, ActiveViam's user interface for exploring the cube, will be available from `http://localhost:9090/ui`.

## Performance
The multithreaded CSV source usually parses CSV data at several hundreds of MB/s. Of course this kind of throughput can only be reached with fast storage, a local SSD drive for instance or network storage accessed through a 10Gbps network at least.

Atoti AutoPivot is powered by the Atoti Server technology, the in-memory analytical platform developed by ActiveViam. Atoti Server runs on all sizes of hardware, from laptops to large servers with hundreds of cores and tens of terabytes of memory. When used in fire and forget mode, AutoPivot targets files up to a few hundreds of gigabytes.

## CSV Format
Atoti AutoPivot expects a standard CSV file, with headers (column names) on the first row.

## Options
The most common options can be set in the `/src/main/resources/application.properties` file. All the supported options can also be passed as JVM parameters such as `-DfileName=data.csv`.

## Tweaking The Project
Atoti AutoPivot tries to guess what's in the data and do everything automatically. More generally it illustrates Atoti cubes can be configured programmatically and started on the fly, a very powerful concept for Atoti developers that can be reused beyond the simple usage of AutoPivot.

Here are some entry points to jump into the code, starting from `src/main/java`:
* `com.av.csv.discover.CSVDiscovery` logic to discover the CSV separator character and the data types of the columns
* `com.av.autopivot.AutoPivotGenerator` logic to create an ActivePivot cube (hierarchies, aggregates...) based on the file format
* `com.av.autopivot.spring` this package contains the Spring configuration of the AutoPivot application
* `src/main/resources/application.properties` options of the AutoPivot application

## Licensing
The code of the Atoti AutoPivot application is open source, licensed under the Apache License 2.0. The AutoPivot application depends on the Atoti Server (commercial) software, the Atoti Server jar files distributed by ActiveViam must be available in the maven repository for the application to build. Running the Atoti AutoPivot application requires a license for the Atoti software. To use the Atoti UI frontend, the Atoti license must have the Atoti UI option enabled.
