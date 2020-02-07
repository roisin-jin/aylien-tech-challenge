# aylien-tech-challenge
Tech challenge for AYLIEN

* To install ```sbt 1.3.7```, please run installsbt.sh

* To run service locally, run build.sh, it will build a mysql docker image and start a container, then starts the python app and the akka-http app

* To kill off the python app and mysql container, you'll need to run ```ps -a``` and find the python PID and kill it manually. Then go to mysql directive and use ```docker-compose down``` to stop the container (or user ```docker ps``` to find the container and stop it there) 

## Akka-http API service:

1. Hosted on https://localhost:9000 using self-signed ssh certificate. When use Postman to interact please make sure ssl-certificate verifiaction is turned off in settings/general

2. Setup credentials inside http headers with ```X-PAINT-APP-ID``` and ```X-PAINT-APP-KEY```, you can use preloaded data as "testUserAppId" and "testUserAppKey"

3. Hit endpoint ```https://localhost:9000/v1/?input={...}``` should still get the same response as the original python app

4. Or use POST endpoint ```v2/solve``` with body data structure:
```
{
   "totalColors": 2
   "customerDemands": 
   [
       {"customerId": 1, demands:[{"color":1, "type":0}]}, 
       {"customerId": 2, demands:[{"color":1, "type":1}]}
   ]
}
```

5. Valiation rules checked upon input request with unique error codes

6. Hit endpoint ```v2/history```
