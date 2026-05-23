# Sistema de Licencias de Funcionamiento Municipal

Aplicación web construida con **Spring Boot 3 + Thymeleaf + PostgreSQL**.

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.3, Spring MVC |
| Vistas | Thymeleaf + Bootstrap 5 |
| Persistencia | Spring Data JPA + PostgreSQL |
| Seguridad | Spring Security (roles: NEGOCIO, INSPECTOR, ADMIN) |
| PDF | iText 5 |
| Despliegue | Render (Web Service + PostgreSQL) |

---

## Levantar en Codespace (local)

### 1. Requisitos
- Java 21
- Maven 3.9+
- PostgreSQL corriendo en `localhost:5432`

### 2. Crear la base de datos
```sql
CREATE DATABASE licencias_db;
```

### 3. Ejecutar
```bash
mvn spring-boot:run
```

Abre `http://localhost:8080`

### Usuarios de demostración (se crean automáticamente)

| Usuario | Contraseña | Rol |
|---|---|---|
| negocio1 | negocio123 | NEGOCIO |
| inspector1 | inspector123 | INSPECTOR |
| admin | admin123 | ADMIN |

---

## Despliegue en Render

### 1. Build command
```
mvn clean package -DskipTests
```

### 2. Start command (si no usas Docker)
```
java -Dspring.profiles.active=prod -jar target/licencias-0.0.1-SNAPSHOT.jar
```

### 3. Variables de entorno en Render
```
DATABASE_URL     = jdbc:postgresql://<host>:<port>/<dbname>
DATABASE_USER    = <usuario>
DATABASE_PASSWORD= <contraseña>
```

### 4. Con Docker
```bash
# Build local
mvn clean package -DskipTests
docker build -t licencias .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://... \
  -e DATABASE_USER=... \
  -e DATABASE_PASSWORD=... \
  licencias
```

---

## Flujo principal

```
Negocio registra datos → carga plano → paga S/180
  → validación SUNAT → 1.ª inspección automática
    → Conforme → licencia PDF disponible
    → Con observaciones → 30 días hábiles → 2.ª inspección
      → Conforme → licencia
      → No conforme → denegado
Renovación anual: solo S/180 si mismo local
Fiscalización: inspector puede visitar en cualquier momento → multa / revocación
```

---

## Estructura del proyecto

```
src/main/java/com/municipalidad/licencias/
├── controller/    Controllers.java (todos los controllers en un archivo)
├── service/       SolicitudService, InspeccionService, LicenciaService,
│                  FiscalizacionService, SunatService
├── repository/    Repositories.java (todos los JpaRepository)
├── model/         Usuario, Solicitud, Inspeccion, Observacion, Licencia,
│                  Renovacion, Enums
├── dto/           SolicitudDto, ResultadoInspeccionDto, ObservacionDto,
│                  InspeccionOficioDto
├── config/        SecurityConfig, DataInitializer
└── util/          DiasHabilesUtil, PdfLicenciaUtil
```
