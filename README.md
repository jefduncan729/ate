Android Topology Editor
=======================

Android Topology Editor is an Android (4.1 and above) application which employs API Server's management REST API to view and edit API Server topologies.

In my initial attempt to create this tool I tried to integrate the Sun Jersey client jars into my Android environment. However, I ultimately found that Android's Java implementation did not provide the necessary support for Jersey. In the end, I chose Spring's Android framework for REST API support and I'm quite happy with it. Although, I did roll my own Json marshalling because Spring's framework didn't like Jersey's JAXB annotations.

Features
========
*Connection Manager - to manage connections to various servers
*SSL Support - allows for easy SSL certificate trusting
*Work locally - allows saving/loading topologies to/from files

Dependencies
===
API Server's common.jar and server.jar
spring-android-core
spring-android-rest-template
httpclient-4.2.2.jar
gson-2.1.jar

