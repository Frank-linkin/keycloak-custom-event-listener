# Keycloak Extension - Custom Event Listener

"User create" and "register" events, listen and Call Rest API with Java


### Clone :
```shell
git clone https://github.com/cevheri/keycloak-custom-event-listener

cd keycloak-custom-event-listener
```

### Generate JAR:
```shell
mvn clean package
```

### Attach keycloak dockerVolume (already in docker-compose file)
```yaml
    volumes:
      - ../../../target/custom-event-listener.jar://opt/jboss/keycloak/standalone/deployments/custom-event-listener.jar
```


### Run on Docker:
```shell
docker-compose -f src/main/docker/docker-compose.yml up -d
```

### Test :
#### First
![](files/event-configuration.png)


#### Then
![](files/create-new-user.png)

#### API Design
![keycloak-event-listener-call-api.png](..%2F..%2F..%2FDocuments%2Fkeycloak-event-listener-call-api.png)

### Test Result on MockAPI: 
#### Setup Mock API
![](files/mockapi-view-api.png)

#### View api result
![](files/mockapi-view-user.png)

### Stop Docker:
```shell
docker-compose -f src/main/docker/docker-compose.yml down
```
