# Heart-Garden Reservation
https://heartgardenreservation.himadri.eu/

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
Save image:
```
docker save -o dockerimage kavai77/heartgardenreservation:1.0
```

### Upload image
```
docker login docker.himadri.eu:5000
docker push docker.himadri.eu:5000/kavai77/heartgardenreservation:1.0
```