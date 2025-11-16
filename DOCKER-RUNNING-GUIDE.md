# 🐳 Docker Running Guide - Inventory Service with Kafka & Zookeeper

This guide explains how to run the Inventory Service using Docker with Kafka and Zookeeper for event-driven communication.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Architecture](#architecture)
5. [Detailed Setup](#detailed-setup)
6. [Kafka Communication Flow](#kafka-communication-flow)
7. [Verification & Testing](#verification--testing)
8. [Troubleshooting](#troubleshooting)
9. [Docker Commands Reference](#docker-commands-reference)

---

## 🎯 Overview

The Inventory Service uses **Apache Kafka** for asynchronous event-driven communication with the Orders Service:

- **Orders Service** publishes order events to Kafka when orders are created/cancelled
- **Inventory Service** consumes these events and updates stock accordingly
- **Zookeeper** is required by Kafka for cluster coordination

The `docker-compose.yml` file in this folder provides a complete setup including:
- ✅ Zookeeper (for Kafka cluster management)
- ✅ Kafka (message broker)
- ✅ MySQL (database for inventory)
- ✅ Inventory Service (Spring Boot application)

---

## ✅ Prerequisites

Before you start, make sure you have:

- ✅ **Docker Desktop** installed and running
  - Download: https://www.docker.com/products/docker-desktop
  - Verify: `docker --version`
- ✅ **Docker Compose** installed (usually comes with Docker Desktop)
  - Verify: `docker-compose --version`
- ✅ At least **4GB RAM** available for Docker
- ✅ Ports available: `2181` (Zookeeper), `9092` (Kafka), `3306` (MySQL), `8085` (Inventory Service)

---

## ⚡ Quick Start

### Step 1: Navigate to Inventory Folder

```bash
cd inventory
```

### Step 2: Build and Start All Services

```bash
docker-compose up -d --build
```

This will:
1. Build the Inventory Service Docker image
2. Start Zookeeper, Kafka, MySQL, and Inventory Service
3. Create necessary Docker networks and volumes

### Step 3: Verify Services are Running

```bash
docker-compose ps
```

You should see all 4 services with status "Up" and "healthy":
- ✅ `zookeeper` - Port 2181
- ✅ `kafka` - Port 9092
- ✅ `mysql-inventory` - Port 3306
- ✅ `inventory-service` - Port 8085

### Step 4: Check Service Health

```bash
# Check Inventory Service health
curl http://localhost:8085/actuator/health

# Should return: {"status":"UP"}
```

---

## 🏗️ Architecture

```
┌─────────────────┐
│  Orders Service │
│    (Port 8086)  │
└────────┬────────┘
         │
         │ Publishes OrderEvent
         │ (via REST)
         ▼
┌─────────────────────────────────────┐
│         Kafka Broker                │
│         (Port 9092)                 │
│  Topic: orders-events               │
│  Topic: inventory-events            │
└────────┬────────────────────────────┘
         │
         │ Consumes OrderEvent
         │ (via Kafka Listener)
         ▼
┌─────────────────┐         ┌──────────────┐
│Inventory Service│◄────────┤   MySQL      │
│   (Port 8085)   │         │  (Port 3306) │
└────────┬────────┘         └──────────────┘
         │
         │ Queries Menu Service
         │ for dish ingredients
         ▼
┌─────────────────┐
│  Menu Service   │
│   (Port 8084)   │
└─────────────────┘
```

### Event Flow

1. **Order Created**:
   - Orders Service validates stock (REST call)
   - Orders Service creates order in database
   - Orders Service publishes `OrderEvent` to Kafka topic `orders-events`

2. **Inventory Processing**:
   - Inventory Service consumes `OrderEvent` from Kafka
   - For each dish in the order:
     - Query Menu Service for dish ingredients
     - Calculate total ingredients needed (dish quantity × ingredient quantity)
     - Update stock for each ingredient by name
   - Publish `InventoryEvent` to Kafka topic `inventory-events` (optional)

3. **Order Cancelled**:
   - Orders Service publishes `OrderEvent` with status "CANCELLED"
   - Inventory Service consumes event and restores stock

---

## 🔧 Detailed Setup

### Option 1: Using docker-compose (Recommended)

The `docker-compose.yml` file in this folder contains everything you need:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f inventory-service

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: Deletes database data)
docker-compose down -v
```

### Option 2: Manual Docker Run

If you prefer to run containers individually:

#### 1. Start Zookeeper

```bash
docker run -d \
  --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  confluentinc/cp-zookeeper:7.5.0
```

#### 2. Start Kafka

```bash
docker run -d \
  --name kafka \
  -p 9092:9092 \
  --link zookeeper:zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:7.5.0
```

#### 3. Start MySQL

```bash
docker run -d \
  --name mysql-inventory \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=admin \
  -e MYSQL_DATABASE=inventory \
  -e MYSQL_USER=inventory_user \
  -e MYSQL_PASSWORD=admin \
  mysql:8.0
```

#### 4. Build Inventory Service

```bash
# From the inventory folder
docker build -t inventory-service:latest .
```

#### 5. Run Inventory Service

```bash
docker run -d \
  --name inventory-service \
  -p 8085:8085 \
  --link kafka:kafka \
  --link mysql-inventory:mysql-inventory \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-inventory:3306/inventory \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=admin \
  inventory-service:latest
```

---

## 📡 Kafka Communication Flow

### Topics Used

1. **orders-events** (Created automatically)
   - Used by Orders Service to publish order events
   - Consumed by Inventory Service

2. **inventory-events** (Created automatically)
   - Used by Inventory Service to publish inventory status updates
   - Can be consumed by other services (analytics, notifications, etc.)

### Event Structure

#### OrderEvent (from Orders Service)

```json
{
  "orderId": 1,
  "items": [
    {
      "dishId": 1,
      "dishName": "Pizza Margherita",
      "quantity": 2,
      "price": 12.99
    }
  ],
  "status": "CREATED",
  "timestamp": "2025-01-15T18:30:00",
  "tableNumber": 5
}
```

#### InventoryEvent (from Inventory Service)

```json
{
  "productId": 1,
  "productName": "Mozzarella",
  "quantity": 45,
  "status": "LOW_STOCK",
  "timestamp": "2025-01-15T18:30:01"
}
```

---

## ✅ Verification & Testing

### 1. Check Kafka is Running

```bash
# List Kafka topics
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# You should see:
# - orders-events
# - inventory-events
# - __consumer_offsets (internal Kafka topic)
```

### 2. Monitor Kafka Messages

#### Watch Order Events

```bash
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders-events \
  --from-beginning
```

#### Watch Inventory Events

```bash
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-events \
  --from-beginning
```

### 3. Check Inventory Service Logs

```bash
# View all logs
docker-compose logs inventory-service

# Follow logs in real-time
docker-compose logs -f inventory-service

# View last 100 lines
docker-compose logs --tail=100 inventory-service
```

### 4. Test Inventory API

```bash
# Health check
curl http://localhost:8085/actuator/health

# Get all products
curl http://localhost:8085/api/v1/inventory/products

# Get stock by ingredient name
curl http://localhost:8085/api/v1/inventory/ingredients/tomato/stock
```

### 5. Verify Database Connection

```bash
# Connect to MySQL
docker exec -it mysql-inventory mysql -uroot -padmin

# Use inventory database
USE inventory;

# Show tables
SHOW TABLES;

# Check products
SELECT * FROM products;
```

---

## 🔍 Troubleshooting

### Issue: "Connection refused" when connecting to Kafka

**Solution**:
```bash
# Check if Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker-compose logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Issue: "Topic not found"

**Solution**:
Topics are auto-created, but you can create them manually:

```bash
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic orders-events \
  --partitions 3 \
  --replication-factor 1

docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic inventory-events \
  --partitions 3 \
  --replication-factor 1
```

### Issue: "Inventory not updating"

**Solution**:
1. Check Inventory Service logs:
   ```bash
   docker-compose logs inventory-service | grep -i "order event"
   ```

2. Verify consumer group:
   ```bash
   docker exec -it kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --list
   ```

3. Check consumer lag:
   ```bash
   docker exec -it kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group inventory-service-group \
     --describe
   ```

### Issue: "Cannot connect to MySQL"

**Solution**:
```bash
# Check MySQL is running
docker ps | grep mysql

# Check MySQL logs
docker-compose logs mysql-inventory

# Verify connection from container
docker exec -it inventory-service ping mysql-inventory
```

### Issue: "Services can't communicate in Docker"

**Solution**:
- All services must be on the same Docker network
- The `docker-compose.yml` creates a network called `inventory-network`
- Services use container names as hostnames (e.g., `kafka:9093`, `mysql-inventory:3306`)

---

## 📚 Docker Commands Reference

### Basic Operations

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v

# Rebuild and start
docker-compose up -d --build

# View logs
docker-compose logs -f [service-name]

# Restart a service
docker-compose restart [service-name]

# View service status
docker-compose ps
```

### Container Management

```bash
# List all containers
docker ps -a

# Stop a container
docker stop [container-name]

# Start a container
docker start [container-name]

# Remove a container
docker rm [container-name]

# Execute command in container
docker exec -it [container-name] [command]
```

### Kafka Operations

```bash
# List topics
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe topic
docker exec -it kafka kafka-topics.sh --describe \
  --bootstrap-server localhost:9092 \
  --topic orders-events

# Create topic
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic [topic-name] \
  --partitions 1 \
  --replication-factor 1

# Delete topic
docker exec -it kafka kafka-topics.sh --delete \
  --bootstrap-server localhost:9092 \
  --topic [topic-name]

# List consumer groups
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list

# Describe consumer group
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group inventory-service-group \
  --describe
```

### Database Operations

```bash
# Connect to MySQL
docker exec -it mysql-inventory mysql -uroot -padmin

# Backup database
docker exec mysql-inventory mysqldump -uroot -padmin inventory > backup.sql

# Restore database
docker exec -i mysql-inventory mysql -uroot -padmin inventory < backup.sql
```

---

## 🔄 Development Workflow

### Running Locally (Outside Docker)

If you want to run the Inventory Service locally but use Docker for Kafka:

1. Start only Kafka infrastructure:
   ```bash
   docker-compose up -d zookeeper kafka mysql-inventory
   ```

2. Run Inventory Service locally:
   ```bash
   mvn spring-boot:run
   ```

3. Update `application.properties`:
   ```properties
   spring.kafka.bootstrap-servers=localhost:9092
   spring.datasource.url=jdbc:mysql://localhost:3306/inventory
   ```

### Rebuilding After Code Changes

```bash
# Rebuild Inventory Service image
docker-compose build inventory-service

# Restart service with new image
docker-compose up -d inventory-service
```

---

## 📝 Environment Variables

You can override configuration using environment variables in `docker-compose.yml`:

```yaml
environment:
  # Kafka
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9093
  
  # Database
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql-inventory:3306/inventory
  
  # Service Registry (if using Eureka)
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka:8761/eureka/
```

---

## 🎯 Best Practices

1. **Always start Zookeeper before Kafka**
   - Zookeeper must be healthy before Kafka starts

2. **Wait for services to be healthy**
   - Use `docker-compose ps` to check health status
   - Services wait for dependencies via `depends_on` with `condition: service_healthy`

3. **Monitor Kafka topics**
   - Use console consumers to debug event flow
   - Check consumer lag regularly

4. **Backup database regularly**
   - Use `mysqldump` to backup inventory data
   - Store backups outside Docker volumes

5. **Use Docker networks**
   - Services communicate via container names
   - Don't use `localhost` for inter-container communication

---

## ✅ Success Checklist

After running `docker-compose up -d`, verify:

- [ ] Zookeeper is running on port 2181
- [ ] Kafka is running on port 9092
- [ ] MySQL is running on port 3306
- [ ] Inventory Service is running on port 8085
- [ ] Health endpoint returns `{"status":"UP"}`
- [ ] Kafka topics `orders-events` and `inventory-events` exist
- [ ] Can connect to MySQL database
- [ ] Inventory Service logs show "Started InventoryServiceApplication"

---

## 📖 Additional Resources

- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Zookeeper Documentation**: https://zookeeper.apache.org/documentation.html
- **Docker Compose Documentation**: https://docs.docker.com/compose/
- **Spring Kafka Documentation**: https://spring.io/projects/spring-kafka

---

## 🆘 Need Help?

If you encounter issues:

1. Check service logs: `docker-compose logs [service-name]`
2. Verify all services are healthy: `docker-compose ps`
3. Check network connectivity: `docker network ls` and `docker network inspect inventory_inventory-network`
4. Review this guide's troubleshooting section

---

**Happy Coding! 🚀**

