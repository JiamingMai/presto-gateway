# presto-gateway

A Lightweight High Available Solution for Presto.

## Introduction
This is a lightweight gateway for Presto supporting High Available Presto Coordinator.

It aims to solve the single point problem. As the application is developed based on Spring Boot, 
one can easily run it up and expand on it.

<img src="https://raw.githubusercontent.com/JiamingMai/presto-gateway/master/docs/fig1_overview.png" width="375"/>

## Requirements
1. JDK 1.8+
2. Maven 3.6.x
3. MySQL

## Usage
1. Run presto-gateway-ddl.sql to create necessary tables in your MySQL.
2. Compile the application with Maven:
```shell script
% mvn clean install -DskipTests
```
3. Run the executable jar file:
```shell script
% java -jar presto-gateway-1.0-SNAPSHOT.jar
```
