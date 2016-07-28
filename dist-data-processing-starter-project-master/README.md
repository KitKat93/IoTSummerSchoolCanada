Simple Maven example project
=============

Requirements
======
To build, you need

* Java 8 or higher (<http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>) 
* Maven 3 or higher (<http://maven.apache.org/>)
* Bower (<http://bower.io>) (install using ```npm install bower```)

Building and Setup
======
Navigate to ```src/main/resources/webroot``` and run ``bower install``. 
To build the project, run `mvn package`, this will build the program and place the generated jar file in the directory `target/`.

Running
======
To Run, use `mvn exec:java`.

