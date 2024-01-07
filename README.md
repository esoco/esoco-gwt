# The esoco-gwt project

[![](https://github.com/esoco/esoco-gwt/workflows/Build/badge.svg)](https://github.com/esoco/esoco-gwt/actions)

This project contains the esoco GWT framework library which is a bridge between the esoco-business framework and GEWT. It provides the business back-end (processes and persistent entities) with an automatically generated web application user interface based on GWT's Java-to-Javascript translation. The result is a consistent and easy to use framework for building client-server web applications. It handles the communication between the server side and the client code running in a web browser, automatically transforming business data into visual representations in the browser and transferring user input to the server for process execution and persistence. It also features high-level components that are the building blocks for the implementation of complete web applications.

The esoco-gwt framework also contains the base classes and core elements necessary to create a configurable application that can be deployed into an application server which uses a connection to a standard relational database like PostgreSQL or MySQL. The GwtApplication already supports the automatic historization of changes in persistent data, database and file logging, and background processing. It is only necessary to implement the business-specific entities and processes to create a full application.

For more information please see the [Javadoc](http://esoco.github.io/esoco-gwt/javadoc/).

# License

This project is licensed under the Apache 2.0 license (see LICENSE file for details).  