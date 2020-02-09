# aylien-tech-challenge

## Paint-factory API service:

This API is built on top of the original python app, with akka-http as the new front API gateway service. Akka-http is mainly responsible for user credentials authentication and authorization, rate limite check. Also it validates the incoming requests from client, once its validated then it forwards the request to the internal python webserivce

1. When in local enviorment, just run ```docker-compose up``` (This could take a coule of minutes, please wait until the akka-http container launches)

2. Hosted on https://0.0.0.0:9000 with self-signed ssh certificate to support local HTTPS context setting. When using Postman to interact please turn off ssl-certificate verifiaction in settings/general; when using ```curl```, please add flag ```--insecure```

3. Setup credentials inside http headers with ```X-PAINT-APP-ID``` and ```X-PAINT-APP-KEY```, you can use preloaded data as "testUserAppId" and "testUserAppKey"

3. Hit ```https://0.0.0.0:9000/v1/?input={...}``` with GET request and the above headers should still get the same response as the original python app. (Alternatively, use POST endpoint ```/v1/input``` with the same input string as post data body, I find its easier to work with curl that way)

4. Use POST endpoint ```/v2/solve``` with body data structure:
```
{
   "totalColors": 2
   "customerDemands": 
   [
       {"customerId": 1, demands:[{"color":1, "type":0}]}, 
       {"customerId": 2, demands:[{"color":2, "type":1}]}
   ]
}
```
would give response as ```{ solutions: [{"color":1, "type":0}, {"color":2, "type":1}]}```

5. There's formate valiation rules applied upon input request with unique error codes

6. Use GET endpoint ```/v2/history``` should give you most recent requests history containing requests with proper credentials. Available parameters are ```offset```(default 0) and ```pageSize```(default 0 and max 100)

7. If you want to add more credentials for testing purpose, you can use POST endpoint ```/admin/user``` with user body:
```
{
   "id": 0, 
   "appId": TEST_APP_ID, 
   "appKEY": TEST_KEY,
   "email": TEST_EMAIL
}
```
This will generate a user with no access to ```V1``` endpoints by default. If you wish to grant V1 access, simply add another field ```"hasV1Access":true```

Afterwards, use ```/admin/users``` to check for new users just being added in

8. Cache is turned on for user credentials. So if any user's access is turned off it wont activate until the cache expires. Also ws result from the python app can be cached as well

9. Rate limite is currently set to 5 requests per second

10. Deployment to kubenetes is supported inside ```k8s``` module, please update the yaml file with your own container registry path once all images are built and pushed to the proper places
