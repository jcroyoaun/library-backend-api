# library-backend-api 

This is a study case on how to approach the container creation and architecture for a Spring Boot app as a DevOps engineer using an nginx reverse proxy to the Java backend and a mariadb for writing data.

This app works in conjunction with repos

*library-db
*library-webserver

1. Library DB should get started first
2. Library Backend API should get started second
3. Lastly, we start Library Webserver


### Step #2 to run Library Backend API
1. Build a local image with the backend library api code
```
minikube image build -t library-backend-container .
```

2. Start the kubernetes deployment and services
```
kubectl apply -f .
```