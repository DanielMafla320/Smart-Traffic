# Infraestructura local

Kafka (KRaft, sin Zookeeper) y RabbitMQ con consola de administración para
desarrollar los microservicios de SmartTraffic.

## Iniciar

```bash
cd infrastructure
docker compose up -d
docker compose ps
```

Esperar a que ambos servicios aparezcan como `healthy`. La primera vez puede
tardar alrededor de un minuto.

## Accesos

| Servicio | Desde Windows (IDE) | Desde otro contenedor de la red | Consola |
|---|---|---|---|
| Kafka | `localhost:9092` | `kafka:29092` | — |
| RabbitMQ AMQP | `localhost:5672` | `rabbitmq:5672` | — |
| RabbitMQ Management | http://localhost:15672 | — | usuario/clave de desarrollo |

Credenciales locales por defecto: `smarttraffic` / `smarttraffic_dev`.
Se pueden cambiar copiando `.env.example` a `.env`. No usar estas credenciales
en un entorno compartido o productivo.

## Verificar Kafka

```bash
docker exec -it smarttraffic-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 --list

docker exec -it smarttraffic-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:29092 --topic raw_traffic_data --from-beginning
```

## Detener

```bash
cd infrastructure
docker compose down
```

Para detener y **borrar** los datos persistidos en los volúmenes:

```bash
docker compose down -v
```
