# tic-tac-toe
Real-Time Remote Tic-tac-toe

* Source code on my GitHub at https://github.com/mathieucarbou/tic-tac-toe

* All Mycila and Guestful libraries I used are libraries I developed at Guestful and Mycila and I open-sourced them at https://github.com/guestful and https://github.com/mycila. 
These libraries focus on specialized modules for quickly start an application completely JAX-RS based with DI. 

* Application is deployed on Heroku at http://tic-tac-toe-rt.mathieu.carbou.me/

* The Heroku app only used Free addons (thus limited in terms of DB size, requests, etc). So If the app does not work due to these limitations, you'll have to setup your own environment, either locally or on Heroku

* Java is mainly used, but Groovy is used where Java lacks (i.e. Json support for REST and Mongo calls)

* console.log and other debugging and traces have been left intentionally 

* for a simple app like this, I've avoided any model class and data binding

* app is bigger than you probably wants because it aims at being an exercise / test / challenge to ebe reused for other companies also.

* session clustering / entirely scalable

* No validation !

* goal: capacity to integrate several services in a cloud and choose right tech for the needs

* no optimization on resources: like css min, js min, concat, etc.

* no namespaced in js code - juste plain functions attached to window object (bad but fast for a poc)

https://developers.facebook.com/quickstarts/971673849550778/?platform=web


Librato
Logentries
Mandrill
MongoLab
Pusher
Redis Cloud
