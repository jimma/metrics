##Throttle And Metrics Demo

This project demostrates the new feature in cxf 3.x to throttle webservice call and new metrics based on dropwizard metrics. 

###Throttle###
In production environment online service like ![amazon web service](http://docs.aws.amazon.com/general/latest/gr/aws_service_limits.html#limits_cloudfront) or ![twitter](https://dev.twitter.com/rest/public/rate-limiting), there is 
configuration to limit the requtest per second or minute to make sure web service is not occupied by certain user. Throttling is the important action for the service level agreement.  For some commercial
service, there is pricing for different rate limit for trail and paid user. Customers pay more , they can get more service invocation count or more resources to server her requests.
###Dropwizard Metrics###
To implement this throttling, it needs a metric monitor to know the request count for a period time, then it can take action to throttle the service call. 
Dropwizard metrics provids some simple utitly class to collect different type metric and easily be accessed these metrics through registry from local(same jvm), jmx or rest 
from remote. 
###About this demo###
This demo is a simple webservice is designed to server for 3 level customer, and each level has different request limit. 

|Cumster Level | Request limit|
|------------- | -------------|
|Trail         | Only allow 10 times requests in total  | 
|Regular       |request rate per min < 25 && request rate in 5 minute < 10|
|Premium       |Unlimited     |

Server will reject or slow down the request if it exceeds these limit.  
Run this demo and simply run **mvn clean install -Pserver** to start server side , and open another terminal to run client with 
**mvn clean install -Pclient**

There is rest api to monitor all the metrics with url ![http://localhost:8888/metrics](http://localhost:8888/metrics).
![rest api](images/rest.jpg)
You can also start jconsole to connect the client or server local process. 
![jconsole](images/jconsole.jpg)

