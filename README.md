# Heart-Garden Reservation
https://heartgardenreservation.appspot.com/

## Setup
```
gcloud config set project heartgardenreservation
```

## Run locally
```
gcloud auth application-default login
gcloud beta emulators datastore start --host-port localhost:8484
mvn spring-boot:run
```

## Deploy
```
mvn package appengine:deploy
```

## Docker
Build image:
```
mvn package
```
Run on `localhost:8081`:
```
docker run -p 8081:8080 kavai77/heartgardenreservation:1.0
```
Push image to Amazon ECS:
```
docker tag kavai77/heartgardenreservation:1.0 633136578871.dkr.ecr.us-east-2.amazonaws.com/heartgardenreservation

```