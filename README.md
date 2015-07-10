# tic-tac-toe

Real-Time Remote Tic-tac-toe

__Goal__


This PoC is probably bigger than you expect because it aims at being an exercise / test / challenge to be reused for other companies also. 
Its goal is to show what we can do with several SaaS and PaaS in 2 days, by using components to build fast and scalable micro-service apps in the cloud.

__Source code__

[https://github.com/mathieucarbou/tic-tac-toe](https://github.com/mathieucarbou/tic-tac-toe)

__Live Demo__

[http://tic-tac-toe-rt.mathieu.carbou.me](http://tic-tac-toe-rt.mathieu.carbou.me)

WARNING: first startup can be slow if the Heroku dyno is asleep

__Technologies and libraries used__

  * Java 8, JSR-330, JSR-250, JSR-310, JSR-353, JAX-RS
  * Guice, Jersey, Logback, SLF4J, Groovy, Redis, Undertow
  * [Pusher](http://pusher.com) for Real-Time com.
  * [Heroku](https://www.heroku.com) for deployment
  * [Redis Cloud](https://redislabs.com/redis-cloud) for session clustering 
  * [MongoLab](https://www.mongolab.com) for NoSQL database 
  * [Mandrill](http://mandrillapp.com) for mailing
  * Librato & Logentries for monitoring and logging
  * Facebook
 
__Application stack used__

  * [Mycila](http://mycila.com) open-source libraries I developed. I used Guice extensions.
  * [Guestful](http://oss.guestful.com/) open-source libraries I developed at Guestful, mainly focusing on JAX-RS client communication and JAX-RS addons. See this pom.xml for some information and Guestful GitHub. These libraries provides:
    * JAX-RS Security and session clustering
    * `@Cache`
    * `@Jsend` support (useful for JSON-P calls to return http status codes)
    * CORS
    * JAX-RS Generic Json mapping (implemented by Groovy, JSR-353, etc)
    * JSR-310 extensions (for mappings and for ZonedInterval)
    * Undertow support for Jersey. Undertow is the most performing container right now.
    * Logging extensions for logback, etc.
    * A lot of rewritten clients for Mandrill, Pusher, etc., all based on JAX-RS client to reuse JAX-RS ecosystem, improve performance for connection pooling, avoid having a lot of of libs in our classspath and be able to switch the underlying http implementation.  

__Run the app on Heroku__

I strongly suggest you look at the demo. Otherwise, if you want to deploy on Heroku:

  1. Fork the repo and associate an heroku app
  2. Add the following (free) addons, and configure them:
    * Mandrill
    * MongoLab
    * Pusher
    * Redis Cloud
  3. Add a DOMAIN env variable to point to your domain name
  4. Create a Facebook app for the login and export FACEBOOK_APP_ID and FACEBOOK_APP_SECRET env var 
  5. Deploy!

__Run the app locally__

You'll have to run the project `Main` class with  the following env vars:

  * FACEBOOK_APP_ID
  * FACEBOOK_APP_SECRET
  * MANDRILL_APIKEY
  * MANDRILL_USERNAME
  * MONGOLAB_URI
  * PUSHER_URL
  * REDISCLOUD_URL

I didn't take the time to create mock services. Mycila Guice extensions enables to create override modules for Guice in test folder.

## Important Notes

* All Mycila and Guestful libraries used are libraries I developed at Guestful and Mycila and they are open-sourced at https://github.com/guestful and https://github.com/mycila.  These libraries provides components to quickly start a micro-service app completely based on JAX-RS with DI with performance and scalability in mind. 

* The Heroku app only uses free addons (thus limited in terms of DB size, requests, etc). So if the app does not work due to these limitations, you'll have to setup your own environment, either locally or on Heroku

* Java is mainly used, but Groovy is used where Java lacks (i.e. Json support for REST and Mongo calls)

* There is intentionally no model classes, no service classes surrounding DB calls and no data binding (event if the serializer we are using supports Jackson). KISS is the rule here for this PoC. 

* `console.log` and other debugging and traces have been left intentionally 

* the app uses session clustering - it is entirely scalable on Heroku over several nodes

* No validation! Validation in REST services has been kept as minimal as possible. There is no validation framework or custom made and no error handling client-side

* There is no optimization on resources: like css min, js min, concat, etc. I am used to that, but, hey... This is a PoC ;-)

* No namespaced in js code - just plain functions attached to window object (this is bad but works for this little poc)

* Ideally, this app would only be an API and there would be another app based on Node.JS to serve static files and templates merged with the data from the API. To simplify I've done a little service which delivers those files through Jersey. This is not optimal.

* If the website is slow, it could be that the heroku dyno was asleep. Its wake-up can take little time.

* No templating (i.e. handlebars) client-side: => quick'n'dirty PoC. I am used to Node.JS style templating.

* No unit test: there's no algorithm or no code relevant to be tested here. 95% of the code has already been tested and run in production since several years. The new code for the Tic Tac Toe is merely integration code plus a small logic to find the completed board. This could be a wider discussion, but I don't think that unit tests the way most people do guarantees anything and even with a good coverage. I am much more in favor of fewer test, but well-designed and which covers complex stuff and all possible cases than having a lot of tests that covers everything but test nothing. Code review and pairing is to my mind better practices which should be coupled with efficient unit tests, not a soup of unit tests.  
