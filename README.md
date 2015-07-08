# tic-tac-toe
Real-Time Remote Tic-tac-toe

* Source code on my GitHub at https://github.com/mathieucarbou/tic-tac-toe

* All Mycila and Guestful libraries I used are libraries I developed at Guestful and Mycila and I open-sourced them at https://github.com/guestful and https://github.com/mycila. 
These libraries focus on specialized modules for quickly start an application completely JAX-RS based with DI. 

* Application is deployed on Heroku at http://tic-tac-toe-rt.mathieu.carbou.me/

* The Heroku app only used Free addons (thus limited in terms of DB size, requests, etc). So If the app does not work due to these limitations, you'll have to setup your own environment, either locally or on Heroku

* Java is mainly used, but Groovy is used where Java lacks (i.e. Json support for REST and Mongo calls)

* console.log and other debugging and traces have been left intentionnaly 

* No validation !

ENV:                 production
FACEBOOK_APP_ID:     971673849550778
FACEBOOK_APP_SECRET: ab3f8b61c40e0eee78e3cd35ea029d85
JAVA_OPTS:           -Xss512k -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Xmn152m -Xms512m -Xmx512m -Djava.awt.headless=true
LIBRATO_PASSWORD:    c34f39de110527b1
LIBRATO_TOKEN:       0df839720fccffee0a4a33d463b9573c55f9ace411028cf4de875d29f1616bd8
LIBRATO_USER:        app38655475@heroku.com
MANDRILL_APIKEY:     L4qVhy00BFdIK8xL8cL0jA
MANDRILL_USERNAME:   app38655475@heroku.com
MONGOLAB_URI:        mongodb://heroku_gcjbwz04:sc63jbveotn1pktcme9c7m9mgr@ds047632.mongolab.com:47632/heroku_gcjbwz04
PUSHER_SOCKET_URL:   ws://ws.pusherapp.com/app/a2b07892c3fcfc642d7d
PUSHER_URL:          http://a2b07892c3fcfc642d7d:db425803e5a9af0bdf94@api.pusherapp.com/apps/128968

https://developers.facebook.com/quickstarts/971673849550778/?platform=web

<script>
  window.fbAsyncInit = function() {
    
  };

  (function(d, s, id){
     var js, fjs = d.getElementsByTagName(s)[0];
     if (d.getElementById(id)) {return;}
     js = d.createElement(s); js.id = id;
     js.src = "//connect.facebook.net/en_US/sdk.js";
     fjs.parentNode.insertBefore(js, fjs);
   }(document, 'script', 'facebook-jssdk'));
</script>

<div
  class="fb-like"
  data-share="true"
  data-width="450"
  data-show-faces="true">
</div>

<div class="fb-share-button" data-href="https://developers.facebook.com/docs/plugins/" data-layout="button"></div>

