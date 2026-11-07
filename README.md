# Eventos Cloud Native – Semanas 1, 2 y 3 (CDY2204)

## Cinthya Guzman / Matias Suarez

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

## Semana 3 – Tickets con Amazon EFS + Amazon S3

Sistema de gestión de reservas con **tickets personalizados**: por cada entrada de una compra se genera un
ticket PDF. El PDF se escribe primero en **Amazon EFS** (almacenamiento temporal compartido, montado en la EC2)
y luego se sube a **Amazon S3** (almacenamiento definitivo), organizado **por evento y por usuario**.

```
POST /api/compras/{id}/tickets
        │
        ▼
ventas-service ── TicketPdfService (1 PDF por entrada)
        │
        ▼
 EFS  /app/efs/tickets/evento-5/camila_correo.cl/ticket-12.pdf      (temporal)
        │   AlmacenamientoS3Service.subir(...)
        ▼
 S3   s3://<bucket>/tickets/evento-5/camila_correo.cl/ticket-12.pdf  (definitivo)
        │
        └── se borra el temporal de EFS; la tabla TICKETS guarda estado e historial
```

| Estado del ticket | Dónde está el PDF |
|---|---|
| `GENERADO_EFS` | Solo en EFS (se generó sin subida automática, o S3 falló) |
| `SUBIDO_S3` | En S3 (ya se borró de EFS) |

### Endpoints (ventas-service, `http://<host>:8082`)

| Método | Ruta | Requisito |
|---|---|---|
| POST   | `/api/compras/{id}/tickets` | **Generar tickets** (EFS) y subirlos automáticamente a S3. `?subirAutomaticamente=false` los deja solo en EFS |
| POST   | `/api/tickets/{id}/s3` | **Subir** un ticket a S3 |
| POST   | `/api/tickets/s3/pendientes` | Subir todos los tickets que siguen en EFS (opcional `?eventoId=`) |
| GET    | `/api/tickets?eventoId=&usuario=&estado=` | **Historial** de archivos generados con filtros |
| GET    | `/api/tickets/{id}` | Detalle de un ticket |
| GET    | `/api/tickets/{id}/pdf` | **Descargar** un ticket |
| GET    | `/api/tickets/descargar?eventoId=` / `?usuario=` | **Descargar por evento o por usuario** (ZIP con carpetas) |
| PUT    | `/api/tickets/{id}` | **Modificar** detalles (`nombreAsistente`, `sector`); sobrescribe el PDF en S3 |
| DELETE | `/api/tickets/{id}` | **Eliminar** un ticket (S3 + EFS + registro) |
| GET    | `/api/estadisticas/eventos` | **Estadísticas** de ventas y reservas por evento |
| GET    | `/api/estadisticas/eventos/{eventoId}` | Estadísticas de un evento |

Ejemplo de estadísticas:

```json
{
  "eventoId": 1, "evento": "Concierto Rock en el Parque",
  "comprasRealizadas": 4, "entradasVendidas": 9, "recaudacionTotal": 225000,
  "ticketsGenerados": 9, "ticketsEnEfs": 0, "ticketsEnS3": 9,
  "capacidad": 500, "entradasDisponibles": 491, "porcentajeOcupacion": 1.8
}
```

### Clases nuevas (ventas-service)

| Clase | Responsabilidad |
|---|---|
| `model/Ticket`, `model/EstadoTicket` | Tabla `TICKETS`: datos del ticket, ruta del archivo, estado y fechas (historial) |
| `repository/TicketRepository` | Búsqueda con filtros opcionales y conteo por evento/estado |
| `service/AlmacenamientoEfsService` | Lee/escribe/borra archivos en la carpeta del EFS (`java.nio.file.Files`) |
| `service/TicketPdfService` | Arma el PDF del ticket (OpenPDF) |
| `service/TicketService` | Flujo EFS → S3, historial, ZIP, modificar, eliminar |
| `service/EstadisticaService` | Ventas por evento (`GROUP BY` en BD) + tickets + capacidad desde eventos-service |
| `controller/TicketController`, `controller/EstadisticaController` | Endpoints |

## Semana 2 – Resumen de compra en Amazon S3

El resumen de cada compra se genera como **archivo PDF** y se administra en un bucket de **Amazon S3**.
Cada resumen se guarda en una **carpeta con el número de la compra**:

```
s3://<bucket>/
└── resumenes/
    ├── 1/
    │   └── resumen-compra-1.pdf
    ├── 2/
    │   └── resumen-compra-2.pdf
    └── ...
```

### Endpoints (ventas-service, `http://<host>:8082`)

| Método | Ruta | Descripción |
|---|---|---|
| GET    | `/api/compras/{id}/resumen`    | Genera el PDF del resumen y lo descarga (archivo físico) |
| POST   | `/api/compras/{id}/resumen/s3` | Genera el PDF y lo **sube** a `resumenes/{id}/` en S3 |
| GET    | `/api/compras/{id}/resumen/s3` | **Descarga** el PDF guardado en S3 |
| PUT    | `/api/compras/{id}/resumen/s3` | **Modifica** el PDF en S3: sin archivo lo regenera con los datos actuales; con `archivo` (form-data, PDF) lo reemplaza |
| DELETE | `/api/compras/{id}/resumen/s3` | **Elimina** el PDF de S3 (404 si no existe) |
| PUT    | `/api/compras/{id}`            | Corrige nombre y email del cliente (luego se regenera el resumen con PUT …/resumen/s3) |

Flujo para corregir un resumen con error:

1. `PUT /api/compras/{id}` con el nombre/email correcto.
2. `PUT /api/compras/{id}/resumen/s3` (sin archivo) → el PDF en S3 se regenera con los datos corregidos.

Respuesta de las operaciones en S3:

```json
{
  "idCompra": 1,
  "bucket": "mibucketduocs3msuarez",
  "key": "resumenes/1/resumen-compra-1.pdf",
  "tamanoBytes": 1843,
  "mensaje": "Resumen subido a S3"
}
```

| Código | Cuándo |
|---|---|
| 404 | La compra no existe, o el resumen no está en S3 (descargar/modificar/eliminar) |
| 400 | El archivo enviado para reemplazar no es PDF |
| 413 | El archivo supera 5 MB |
| 502 | Error al comunicarse con S3 (credenciales vencidas, bucket incorrecto) |

### Clases nuevas (ventas-service)

| Clase | Responsabilidad |
|---|---|
| `config/S3Config` | Crea el `S3Client` (AWS SDK v2) con las credenciales temporales del Learner Lab |
| `service/ResumenPdfService` | Arma el PDF del resumen (OpenPDF) |
| `service/AlmacenamientoS3Service` | Única clase que habla con S3: subir, descargar, existe, eliminar |
| `service/ResumenCompraService` | Lógica del caso: define la carpeta, valida existencia, coordina PDF + S3 |
| `controller/ResumenCompraController` | Expone los endpoints `/api/compras/{id}/resumen…` |

### Configuración de S3

- Bucket en `us-east-1` con **todo el acceso público bloqueado**: la aplicación accede con credenciales, no necesita acceso público.
- Variables de entorno de ventas-service: `S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`.
- ⚠️ Las credenciales del Learner Lab **vencen al cerrar la sesión**: al iniciar una nueva, actualizar los 3 secrets `AWS_*` en GitHub y volver a ejecutar el workflow (*Re-run all jobs*).

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

### 3b. Amazon EFS (Semana 3)

1. Consola AWS → **EFS** → **Crear sistema de archivos** → nombre `efs-eventos`, VPC por defecto → **Crear**.
2. En el EFS → pestaña **Red** → **Administrar**: en cada zona de disponibilidad asigna el **grupo de seguridad de la EC2** → Guardar.
3. En el grupo de seguridad de la EC2 agrega la regla de entrada **NFS (TCP 2049)** con origen **el mismo grupo de seguridad**
   (así la EC2 puede hablar con el EFS).
4. En la EC2:

```bash
sudo mkdir -p /mnt/efs
# Comando "Adjuntar" -> "Mediante el cliente NFS" del EFS, cambiando el destino a /mnt/efs:
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
  fs-XXXXXXXX.efs.us-east-1.amazonaws.com:/ /mnt/efs
df -h | grep efs

# Montaje permanente (sobrevive a reinicios): agregar esta línea a /etc/fstab
#   fs-XXXXXXXX.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0
sudo nano /etc/fstab
sudo mount -a
```

El pipeline verifica que `/mnt/efs` esté montado y crea `/mnt/efs/tickets` con dueño UID 1000
(el usuario sin privilegios del contenedor).

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
| `S3_BUCKET` | nombre del bucket (Semana 2) |
| `AWS_ACCESS_KEY_ID` | Learner Lab → AWS Details (Semana 2) |
| `AWS_SECRET_ACCESS_KEY` | Learner Lab → AWS Details (Semana 2) |
| `AWS_SESSION_TOKEN` | Learner Lab → AWS Details (Semana 2) |

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
