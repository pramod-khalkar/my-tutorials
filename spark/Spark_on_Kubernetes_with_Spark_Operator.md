# Running Spark Project on Kubernetes using Spark Operator

This guide explains how to take a local Apache Spark project and run it on Kubernetes using the Spark Operator.  
It is written for a **Java developer** and follows a **hands-on, production-style approach**.

---

## Big Picture

You are moving from:

```
Mac → spark-submit → local[*]
```

to:

```
Kubernetes
 ├── Spark Operator
 ├── SparkApplication (your job)
 ├── Driver Pod
 ├── Executor Pods
 └── Parquet / Output
```

Spark code stays the same — **only the runtime changes**.

---

## Phase 1 — Dockerize Your Spark Project

Spark on Kubernetes requires a Docker image.

### 1. Build fat JAR
Ensure you have a shaded / fat JAR:

```
build/libs/spark-ecommerce-etl-1.0.jar
```

---

### 2. Create Dockerfile

Create a `Dockerfile` in project root:

```dockerfile
FROM apache/spark:3.5.0

WORKDIR /opt/spark/app

COPY build/libs/spark-ecommerce-etl-1.0.jar app.jar
COPY data/raw_events.json data/raw_events.json

ENTRYPOINT ["/opt/spark/bin/spark-submit"]
```

---

### 3. Build Docker image

```bash
docker build -t spark-ecommerce-etl:1.0 .
```

Verify:

```bash
docker images | grep spark-ecommerce
```

---

## Phase 2 — Kubernetes Setup (Minimal)

You need:
- Kubernetes cluster
- kubectl
- Docker

### Easy options
- Docker Desktop (Kubernetes enabled)
- minikube
- kind

Verify cluster:

```bash
kubectl get nodes
```

---

## Phase 3 — Install Spark Operator

Spark Operator enables Spark jobs as Kubernetes-native resources.

### 1. Install using Helm

```bash
helm repo add spark-operator https://kubeflow.github.io/spark-operator
helm repo update
```

```bash
helm install spark-operator spark-operator/spark-operator \
  --namespace spark-operator \
  --create-namespace
```

---

### 2. Verify installation

```bash
kubectl get pods -n spark-operator
```

Expected:

```
spark-operator-controller
```

---

## Phase 4 — Run Spark Job using SparkApplication

### 1. Create SparkApplication YAML

Create `spark-app.yaml`:

```yaml
apiVersion: sparkoperator.k8s.io/v1beta2
kind: SparkApplication
metadata:
  name: ecommerce-etl
  namespace: ecommerce-etl 
spec:
  type: Java
  mode: cluster
  image: spark-ecommerce-etl:1.0
  imagePullPolicy: IfNotPresent
  mainClass: com.example.etl.EcommerceETL
  mainApplicationFile: local:///opt/spark/app/app.jar
  sparkVersion: "3.5.0"
  restartPolicy:
    type: Never
  driver:
    cores: 1
    memory: "1g"
  executor:
    instances: 2
    cores: 1
    memory: "1g"
```

---

### 2. Submit job

```bash
kubectl apply -f spark-app.yaml -n ecommerce-etl
```

---

### 3. Monitor job

```bash
kubectl get sparkapplications
kubectl get pods
```

You will see:
- 1 Driver Pod
- Multiple Executor Pods

---

### 4. View logs

```bash
kubectl logs pod/ecommerce-etl-driver
```

---

## Spark ↔ Kubernetes Concept Mapping

| Spark | Kubernetes |
|------|------------|
| Driver | Driver Pod |
| Executor | Executor Pod |
| Shuffle | Pod networking |
| Restart | Operator reconciliation |
| spark-submit | kubectl apply |
| Config | YAML |

---

## Concepts You Are Using

- Apache Spark SQL
- Distributed processing
- Docker containers
- Kubernetes Pods
- Custom Resource (SparkApplication)
- Operator pattern
- Declarative job execution

---

## Important Notes

- Container filesystem is **ephemeral**
- For production use:
  - S3 / ADLS / GCS
  - HDFS
  - PersistentVolume (PVC)

---

## Mental Model (Key Takeaway)

> Spark logic stays the same.  
> Only the cluster manager changes.

Local → YARN → Kubernetes

---

## Next Steps (Optional)

- Add PersistentVolume for output
- Access Spark UI on Kubernetes
- Tune executor memory and cores
- Enable retries and restart policies
- Compare Spark Operator vs spark-submit on K8s
