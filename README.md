# Resource Booking System

A RESTful **Resource Booking System** built with Spring Boot, Java 17, Spring Security, JWT and JPA/Hibernate. Users can browse bookable resources (rooms, vehicles, equipment) and manage their own reservations; administrators have full CRUD access over resources and all reservations.

## Tech stack

- Java 17, Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security 6
- JWT (`io.jsonwebtoken` / jjwt 0.12) for stateless authentication
- PostgreSQL (default) or MySQL via Spring profile
- Bean Validation (`spring-boot-starter-validation`)
- springdoc-openapi (Swagger UI)
- JUnit 5, MockMvc, H2 (in-memory) for integration tests

## Features

- JWT login (`POST /auth/login`) and self-service registration (`POST /auth/register`, always as `USER`)
- Role-based access control: `ADMIN` (full CRUD on resources & reservations) vs `USER` (read-only resources, manage only their own reservations)
- Reservation identity is always taken from the authenticated JWT — the request body has no user field, so a user cannot book on someone else's behalf
- Reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Reservation price stored as a `BigDecimal`, computed automatically from `resource.pricePerHour × duration`
- Double-booking prevention (overlapping time ranges on the same resource are rejected)
- Filtering by `status`, `minPrice`, `maxPrice`; pagination via `page`/`size`; optional sorting via `sort=field,dir`
- Centralized error handling with a consistent JSON error shape (400/401/403/404/409/500)
- Swagger UI / OpenAPI docs, with a Bearer-token "Authorize" button
- Idempotent data seeder for ADMIN/USER test accounts and sample resources

## Project structure

```
src/main/java/com/spironet/booking/
  config/        SecurityConfig, OpenApiConfig, DataSeeder
  security/      JwtService, JwtAuthenticationFilter, UserPrincipal, CustomUserDetailsService,
                 RestAuthenticationEntryPoint, RestAccessDeniedHandler
  controller/    AuthController, ResourceController, ReservationController
  service/       AuthService, ResourceService, ReservationService
  repository/    UserRepository, ResourceRepository, ReservationRepository (+ spec/ for filtering)
  entity/        User, Resource, Reservation, Role, ResourceType, ReservationStatus
  dto/           auth/, resource/, reservation/, common/ (ApiErrorResponse, PageResponse)
  exception/     GlobalExceptionHandler + custom exceptions
  util/          SecurityUtils (current authenticated user helper)
src/test/java/...   Integration tests (MockMvc + H2) for auth, RBAC, ownership, filtering
```

## Prerequisites

- Java 17+
- PostgreSQL 13+ (or MySQL 8+) — or use the provided `docker-compose.yml`
- No local Maven install required; use the bundled `./mvnw` wrapper

## Setup

### 1. Start a database

Using Docker Compose (PostgreSQL by default):

```bash
docker compose up -d postgres
# or, for MySQL:
docker compose --profile mysql up -d mysql
```

Or point at any existing PostgreSQL/MySQL instance — just create the database/role:

```sql
CREATE ROLE booking_user LOGIN PASSWORD 'booking_pass';
CREATE DATABASE booking_system OWNER booking_user;
```

### 2. Configure environment variables

Copy `.env.example` to `.env` and adjust as needed (or export the variables directly):

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/booking_system` | JDBC URL |
| `DB_USERNAME` | `booking_user` | DB username |
| `DB_PASSWORD` | `booking_pass` | DB password |
| `DB_DRIVER` | `org.postgresql.Driver` | JDBC driver class |
| `SPRING_PROFILES_ACTIVE` | *(none)* | Set to `mysql` to use `application-mysql.yml` defaults |
| `JPA_DDL_AUTO` | `update` | Hibernate schema strategy |
| `JWT_SECRET` | *(dev default, insecure)* | HMAC-SHA signing key — **must** be ≥ 256 bits; override in any real deployment |
| `JWT_EXPIRATION_MS` | `3600000` (1h) | Access token lifetime |
| `LOG_LEVEL` | `INFO` | Log level for `com.spironet.booking` |

To use MySQL instead of PostgreSQL: set `SPRING_PROFILES_ACTIVE=mysql` and point `DB_URL` at your MySQL instance (e.g. `jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=UTC`); `application-mysql.yml` supplies matching driver/URL defaults if you don't override them.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/resource-booking-system-0.0.1-SNAPSHOT.jar
```

The API listens on `http://localhost:8080` by default.

### 4. Run the tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`src/test/resources/application-test.yml`) — no external database required.

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Postman collection: [`postman_collection.json`](postman_collection.json) (import into Postman; set the `baseUrl`, `adminToken`, `userToken` collection variables from the login responses)

Click "Authorize" in Swagger UI and paste `Bearer <token>` (obtained from `/auth/login`) to call protected endpoints interactively.

## Seed users

The application seeds these accounts on startup (idempotent — safe on every restart):

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | `ADMIN` |
| `user1` | `User@123` | `USER` |
| `user2` | `User@123` | `USER` |

Sample resources are seeded too: *Conference Room A* (ROOM), *Company Sedan* (VEHICLE), *Projector Kit* (EQUIPMENT).

## API overview

### Auth (public)

| Method | Path | Description |
|---|---|---|
| POST | `/auth/login` | Authenticate, returns `{ accessToken, tokenType, username, role, expiresInMs }` |
| POST | `/auth/register` | Self-register a new `USER` account |

### Resources

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/resources` | ADMIN, USER | Paginated list; filter by `type`, `active` |
| GET | `/api/resources/{id}` | ADMIN, USER | Get one resource |
| POST | `/api/resources` | ADMIN | Create resource |
| PUT | `/api/resources/{id}` | ADMIN | Update resource |
| DELETE | `/api/resources/{id}` | ADMIN | Delete resource |

### Reservations

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/reservations` | ADMIN, USER | ADMIN sees all; USER sees only their own. Filter by `status`, `minPrice`, `maxPrice`; paginate with `page`/`size`; sort with `sort=startTime,desc` |
| GET | `/api/reservations/{id}` | ADMIN, USER | USER may only fetch their own (403 otherwise) |
| POST | `/api/reservations` | ADMIN, USER | Create a reservation for the authenticated user; price is computed server-side |
| PUT | `/api/reservations/{id}` | ADMIN | Full update, including `status` |
| PATCH | `/api/reservations/{id}/cancel` | ADMIN, USER | Cancel a reservation; USER may only cancel their own |
| DELETE | `/api/reservations/{id}` | ADMIN | Delete a reservation |

### Example flow

```bash
# 1. Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"User@123"}'
# => { "accessToken": "...", "role": "USER", ... }

# 2. List resources
curl http://localhost:8080/api/resources \
  -H "Authorization: Bearer <accessToken>"

# 3. Create a reservation (userId is derived from the JWT, never from the body)
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2027-01-10T09:00:00","endTime":"2027-01-10T11:00:00"}'

# 4. Filter own reservations
curl "http://localhost:8080/api/reservations?status=PENDING&minPrice=10&maxPrice=100&page=0&size=10&sort=startTime,desc" \
  -H "Authorization: Bearer <accessToken>"
```

## Security notes

- Passwords are hashed with BCrypt (`BCryptPasswordEncoder`) — never stored or logged in plaintext.
- Sessions are stateless (`SessionCreationPolicy.STATELESS`); every request is authenticated via the `Authorization: Bearer <jwt>` header, validated by `JwtAuthenticationFilter`.
- Method-level `@PreAuthorize` on controllers enforces role checks; row-level ownership (a `USER` may only see/cancel their own reservations) is enforced in `ReservationService`, independent of the URL/role check.
- Unauthenticated requests return `401` (`RestAuthenticationEntryPoint`); authenticated-but-unauthorized requests return `403` (`RestAccessDeniedHandler` / `GlobalExceptionHandler`), both in the same JSON error shape as validation/not-found errors.

## Error response shape

```json
{
  "timestamp": "2026-08-30T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "fieldErrors": [
    { "field": "startTime", "message": "Start time must be in the future" }
  ]
}
```
