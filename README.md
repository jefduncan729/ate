ATE - Android Topology Editor
===

Android Topology Editor is an Android (4.1 and above) application which employs API Server's management REST API to view and edit API Server topologies.

In my initial attempt to create this tool I tried to integrate the Sun Jersey client jars into my Android environment. 
However, I ultimately found that Android's Java implementation did not provide the necessary support for Jersey. In the end,
I chose Spring's Android framework for REST API support and I'm quite happy with it. Although, I did roll my own Json marshalling 
because Spring's framework didn't like Jersey's JAXB annotations.

### Features ###

* Connection Manager - to manage connections to various servers
* SSL Support - allows for easy SSL certificate trusting
* Work locally - allows saving/loading topologies to/from files

### Dependencies ###
* API Server's common.jar and server.jar
* spring-android-core 
* spring-android-rest-template
* httpclient-4.2.2.jar
* gson-2.1.jar

### Android Components used ###
I experimented with a lot of the different framework components. I started out with an Android Service object to which I binded my Activities.
This worked fairly well, but required that all activities bind themselves to the service, else it would be destroyed/re-created. I also tried
just doing the bulk of the work in one main activity with some AsyncTasks but I didn't like all the "listeners" I ended up having to implement.
I finally decided to use an IntentService to do the async work. I use them a lot; they're nice because the async part is built right in and 
there's no need to worry about AsyncTasks. Fragment objects are used for display and Activity objects for communication with the IntentService. 
The Connection Manager is backed by a ContentProvider implementation and SQLite database.

### Wishlist ###
* Finish topology comparison functionality

### Known issues ###
