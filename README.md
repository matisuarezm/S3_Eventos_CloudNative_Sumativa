# Eventos Cloud Native – Semana 1 (CDY2204)

Sistema de **registro de eventos y venta de entradas**, construido con dos microservicios Spring Boot,
persistencia en **Oracle Cloud (Autonomous Database)** y despliegue continuo con
**GitHub Actions → Docker Hub → AWS EC2**.

## Arquitectura

```
                 push a main
 Desarrollador ───────────────► GitHub Actions
                                  │ 1. test   (mvn verify, H2 en memoria)
                                  │ 2. docker (build + push a Docker Hub)
                                  │ 3. deploy (SSH a EC2: docker compose pull/up)
                                  ▼
                       ┌──────────────── EC2 (Docker) ────────────────┐
 Postman ──:8081──────►│ eventos-service  ◄──── REST ────  ventas-service │◄──:8082── Postman
                       └────────┬───────────────────────────────┬─────┘
                                │            JDBC (TLS)          │
                                ▼                                ▼
                        Oracle Autonomous Database (tablas EVENTOS y COMPRAS)
```

| Microservicio     | Puerto | Responsabilidad                                         |
|-------------------|--------|---------------------------------------------------------|
| `eventos-service` | 8081   | Listar y crear eventos, controlar entradas disponibles  |
| `ventas-service`  | 8082   | Comprar entradas y guardar el resumen de cada compra    |

`ventas-service` llama a `eventos-service` para descontar las entradas (con un `UPDATE` atómico,
así nunca se venden más entradas que la capacidad) y obtener el precio vigente.

## Cómo se cumple cada requisito

| Requisito del caso | Implementación |
|---|---|
| Endpoint que lista eventos disponibles con nombre, fecha y precio | `GET /api/eventos` |
| Endpoint para agregar eventos, persistido en Oracle Cloud | `POST /api/eventos` → tabla `EVENTOS` |
| Endpoint de compra con resumen (evento, nº entradas, precio unitario y total) guardado en Oracle | `POST /api/compras` → tabla `COMPRAS` |
| Despliegue automático al hacer push a `main`, imagen en Docker Hub y despliegue en EC2 | `.github/workflows/ci-cd.yml` + `compose.yml` |

## Endpoints

### eventos-service (`http://<host>:8081`)

| Método | Ruta | Descripción |
|---|---|---|
| GET  | `/api/eventos` | Eventos disponibles (fecha futura y con entradas) |
| GET  | `/api/eventos?todos=true` | Todos los eventos |
| GET  | `/api/eventos/{id}` | Detalle de un evento |
| POST | `/api/eventos` | Crear evento |
| POST | `/api/eventos/{id}/reservas` | Descuenta entradas (uso interno de ventas-service) |
| GET  | `/actuator/health` | Estado del servicio |

**Crear evento**

```json
POST /api/eventos
{
  "nombre": "Lollapalooza Chile 2027",
  "descripcion": "Festival de musica - dia 1",
  "lugar": "Parque Cerrillos, Santiago",
  "fecha": "2027-03-20T14:00:00",
  "precioEntrada": 95000,
  "capacidad": 1000
}
```

### ventas-service (`http://<host>:8082`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/compras` | Comprar entradas |
| GET  | `/api/compras` | Listar compras |
| GET  | `/api/compras/{id}` | Detalle de una compra |
| GET  | `/actuator/health` | Estado del servicio |

**Comprar entradas**

```json
POST /api/compras
{
  "eventoId": 1,
  "cantidad": 3,
  "nombreCliente": "Camila Rojas",
  "emailCliente": "camila.rojas@correo.cl"
}
```

Respuesta `201 Created`:

```json
{
  "idCompra": 1,
  "eventoId": 1,
  "evento": "Lollapalooza Chile 2027",
  "fechaEvento": "2027-03-20T14:00:00",
  "nombreCliente": "Camila Rojas",
  "emailCliente": "camila.rojas@correo.cl",
  "cantidadEntradas": 3,
  "precioUnitario": 95000,
  "precioTotal": 285000,
  "fechaCompra": "2026-10-23T18:30:12.512"
}
```

### Errores

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos (campos vacíos, fecha pasada, cantidad < 1 o > 10, email inválido) |
| 404 | El evento o la compra no existen |
| 409 | No quedan entradas suficientes o el evento ya se realizó |
| 503 | ventas-service no logra comunicarse con eventos-service |

---

## Puesta en marcha (paso a paso)

### 1. Base de datos Oracle Cloud

1. En OCI crea una **Autonomous Database** (Always Free sirve), tipo *Transaction Processing*.
2. En la base de datos → **Network** → *Access control list*: agrega la **IP elástica de tu EC2**
   (y tu IP pública si quieres probar desde tu PC).
3. En la misma sección pon **Mutual TLS (mTLS) authentication: Not required**. Así te conectas por TLS
   **sin wallet**, que es mucho más simple en Docker.
4. **Database connection** → elige *TLS* y copia la cadena del servicio `_tp`. Tu `DB_URL` queda así:

   ```
   jdbc:oracle:thin:@(description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.sa-santiago-1.oraclecloud.com))(connect_data=(service_name=xxxx_eventosdb_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
   ```

   (es `jdbc:oracle:thin:@` + la cadena copiada tal cual).
5. Usuario: `ADMIN` (o uno que crees) y su contraseña. **Evita en la contraseña los caracteres
   `'` `"` `$` y `\`**, porque se pasan por variables de entorno.

Las tablas `EVENTOS` y `COMPRAS` las crea Hibernate solo al iniciar (`ddl-auto=update`).
Si la tabla de eventos está vacía, `eventos-service` carga 3 eventos de ejemplo
(se desactiva con `APP_DATOS_INICIALES=false`).

### 2. Docker Hub

Crea un **Access Token** en Docker Hub (*Account settings → Personal access tokens*, permiso Read & Write).
Las imágenes se publican como `<usuario>/eventos-service` y `<usuario>/ventas-service`.

### 3. Instancia EC2 (Amazon Linux 2023)

Security Group – reglas de entrada: **22** (SSH), **8081** y **8082** (TCP, 0.0.0.0/0).

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# Plugin docker compose (no viene con el paquete docker de Amazon Linux)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Cierra la sesión SSH y vuelve a entrar para que aplique el grupo docker
docker compose version
```

> Recomendado: una instancia `t3.small` (2 GB). Con `t2.micro`/`t3.micro` (1 GB) corre igual,
> pero conviene agregar swap: `sudo dd if=/dev/zero of=/swapfile bs=1M count=1024 && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile`

### 4. Secrets en GitHub

En el repositorio: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | tu usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | el access token de Docker Hub |
| `EC2_HOST` | IP elástica de la EC2 |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | contenido completo del archivo `.pem` (incluye las líneas BEGIN/END) |
| `DB_URL` | la URL JDBC del paso 1 |
| `DB_USERNAME` | usuario de Oracle (`ADMIN`) |
| `DB_PASSWORD` | contraseña de Oracle |

### 5. Desplegar

Cada `git push` a `main` ejecuta el pipeline:

1. **test** – compila y corre las pruebas de ambos microservicios (H2 en memoria, no toca Oracle).
2. **docker** – construye las imágenes y las publica en Docker Hub con tags `latest` y `sha-<commit>`.
3. **deploy** – copia `compose.yml` a la EC2, crea el `.env` con los secrets y ejecuta
   `docker compose pull && docker compose up -d --wait`. El job falla si los servicios no quedan *healthy*.

Los *pull requests* a `main` solo ejecutan las pruebas.

Verificación en la EC2:

```bash
cd ~/eventos-app
docker compose ps
docker compose logs -f eventos-service
```

---

## Ejecutar en tu PC

Requisitos: Java 21 y Maven (o el Maven que trae IntelliJ / VS Code).

Sin Oracle (H2 en archivo, perfil `local`):

```bash
# Terminal 1
cd eventos-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2
cd ventas-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Con Oracle: define las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` y ejecuta sin perfil.

Solo pruebas:

```bash
mvn -f eventos-service/pom.xml verify
mvn -f ventas-service/pom.xml verify
```

Con Docker (usa las imágenes publicadas en Docker Hub): copia `.env.example` a `.env`, complétalo y ejecuta `docker compose up -d`.

## Pruebas con Postman

Importa `postman/Eventos-CloudNative.postman_collection.json`. En las variables de la colección
cambia `host` por la IP elástica de la EC2 (o deja `localhost`). Ejecuta las requests en orden:
el *Crear evento* guarda el `eventoId` que usa *Comprar entradas*. Cada request trae tests que
validan el código de respuesta y el cálculo del total.

## Estructura

```
.
├── .github/workflows/ci-cd.yml    # Pipeline CI/CD
├── compose.yml                    # Despliegue de ambos servicios en EC2
├── .env.example                   # Plantilla de variables (el .env real no se sube)
├── postman/                       # Colección de pruebas
├── eventos-service/
│   ├── Dockerfile                 # Multi-stage, usuario no-root
│   └── src/main/java/cl/duoc/eventos/
│       ├── controller/  service/  repository/  model/  dto/  exception/
│       └── DatosIniciales.java
└── ventas-service/
    ├── Dockerfile
    └── src/main/java/cl/duoc/ventas/
        ├── client/EventosClient.java   # Llamada REST a eventos-service
        └── controller/  service/  repository/  model/  dto/  exception/
```

## Buenas prácticas aplicadas

- Arquitectura por capas (controller → service → repository) y DTOs separados de las entidades.
- Validación de entrada con Bean Validation y manejo centralizado de errores (`@RestControllerAdvice`).
- Credenciales solo en variables de entorno / GitHub Secrets; nada sensible en el repositorio.
- Pruebas automáticas con MockMvc en el pipeline; el despliegue solo ocurre si pasan.
- Imágenes multi-stage, livianas (JRE Alpine) y ejecutadas con usuario sin privilegios.
- Healthchecks con Spring Boot Actuator; `ventas-service` espera a que `eventos-service` esté sano.
- Imágenes versionadas por commit (`sha-<commit>`) para saber exactamente qué versión está en la EC2.
- Descuento de entradas atómico en la base de datos para evitar sobreventa.
