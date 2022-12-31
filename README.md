# RSO: Image metadata microservice

## Prerequisites

```bash
docker run -d --name pg-image-metadata -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=image-metadata -p 5432:5432 postgres:13
```

## Build and run commands
```bash
mvn clean package
cd api/target
java -jar optimizator-api-1.0.0-SNAPSHOT.jar
```
Available at: localhost:8080/v1/images

## Run in IntelliJ IDEA
Add new Run configuration and select the Application type. In the next step, select the module api and for the main class com.kumuluz.ee.EeApplication.

Available at: localhost:8080/v1/images

## Docker commands
```bash
docker build -t novaslika .   
docker images
docker run novaslika    
docker tag novaslika prporso/novaslika   
docker push prporso/novaslika
docker ps
```

```bash
docker network ls  
docker network rm rso
docker network create rso
docker run -d --name pg-users -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=optimizator -p 5432:5432 --network rso postgres
docker inspect pg-image-metadata
docker run -p 8080:8080 --network rso -e KUMULUZEE_DATASOURCES0_CONNECTIONURL=jdbc:postgresql://pg-users:5432/optimizator
 rso-users:1.0.0
```

## Consul
```bash
consul agent -dev
```
Available at: localhost:8500

Key: environments/dev/services/optimizator-service/1.0.0/config/rest-properties/maintenance-mode

Value: true or false

## Kubernetes
```bash
kubectl version
kubectl --help
kubectl get nodes
kubectl create -f optimizator-deployment.yaml 
kubectl apply -f optimizator-deployment.yaml 
kubectl get services 
kubectl get deployments
kubectl get pods
kubectl logs optimizator-deployment-6f59c5d96c-rjz46
kubectl delete pod optimizator-deployment-6f59c5d96c-rjz46
```
Secrets: https://kubernetes.io/docs/concepts/configuration/secret/

