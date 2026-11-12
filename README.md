# Job Application Tracker API

A REST API for tracking job applications through every stage of a job hunt, with automatic follow-up reminders by email.

![Swagger UI](docs/swagger-ui.png)

## Features

- Register and log in with JWT authentication
- Create, edit, list and delete job applications; every user sees only their own data
- Status workflow (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`) with a full status history timeline
- Filtering by status, pagination and sorting
- Daily email digest of applications with no update for 7 days, sent through Brevo and never repeated within the reminder window
- Request validation and consistent error responses (RFC 9457 problem details)
- Interactive API documentation with Swagger UI
- Versioned database migrations with Flyway
- Unit tests and integration tests running against a real PostgreSQL (Testcontainers)

## Tech stack

Java 21, Spring Boot 4, Spring Security (OAuth2 resource server, JWT), Spring Data JPA, PostgreSQL, Flyway, Brevo REST API, Lombok, springdoc-openapi, JUnit 5, Mockito, Testcontainers, Docker.

## Quick start

You need Docker (and Git).

```bash
git clone https://github.com/Cavsteeek/JobApplicationTrackerAPI.git
cd JobApplicationTrackerAPI
cp .env.example .env        # then edit .env and fill in your values
docker compose up --build
```

The API is now at `http://localhost:8080` and the docs at `http://localhost:8080/swagger-ui.html`.

To try it:
1. `POST /api/auth/register` to create an account and get a token.
2. Click **Authorize** in Swagger UI and paste the token.
3. Use the `/api/applications` endpoints.

### Running from your IDE instead

Start only the database, then run the application class (Java 21):

```bash
docker compose up -d db
```

The default `.env` points at `localhost:5433`, which is the port Docker exposes for the database.

## Configuration

Set these in `.env` (see `.env.example`):

| Variable | Purpose |
| --- | --- |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Database credentials, shared by Docker and Spring |
| `DB_URL` | JDBC URL used when running the app outside Docker |
| `BREVO_API_KEY` | Brevo API key for sending email |
| `BREVO_SENDER_EMAIL` | A sender address verified in Brevo |
| `JWT_SECRET` | Signing key, at least 32 bytes. Generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | Token lifetime (default 60) |

Reminder behaviour is set in `application.properties`: `reminder.stale-after-days`, `reminder.repeat-after-days`, `reminder.cron` and `reminder.zone`.

## API overview

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/register` | Create an account, returns a JWT |
| POST | `/api/auth/login` | Log in, returns a JWT |
| GET | `/api/applications` | List your applications (`status`, `page`, `size`, `sort`) |
| POST | `/api/applications` | Create an application |
| GET | `/api/applications/{id}` | Get one application |
| PUT | `/api/applications/{id}` | Replace an application's details |
| PATCH | `/api/applications/{id}/status` | Change status and record the change |
| GET | `/api/applications/{id}/history` | Status timeline, newest first |
| DELETE | `/api/applications/{id}` | Delete an application |

All `/api/applications` endpoints require `Authorization: Bearer <token>`.

## Design decisions

- **Modular monolith, organised by feature.** Three tables and one deployable do not justify microservices. Packages (`auth`, `jobapplication`, `reminder`, `email`) have clear boundaries and could be split later.
- **Ownership enforced in every query.** Repositories look up by `id` and `userId` together, so another user's resource appears as not found instead of forbidden, which avoids leaking which ids exist.
- **Stateless JWT via Spring's resource server.** Token validation uses Spring Security's built-in support rather than a hand-written filter.
- **Passwords are stored as BCrypt hashes**, never in plain text.
- **Flyway owns the schema.** Hibernate only validates it (`ddl-auto=validate`), so entity and schema drift fails at startup.
- **Reminders are idempotent.** A `last_reminded_at` column stops an application being reminded about repeatedly, a failure for one user does not block the others, and the update never touches `last_updated`.
- **Email over HTTPS.** Brevo's REST API is used instead of SMTP, because mobile networks and many hosts block SMTP ports.
- **DTO records, never entities, cross the API boundary.**

## Tests

```bash
./mvnw test        # Windows: mvnw.cmd test
```

Unit tests need nothing. The integration tests start a throwaway PostgreSQL container with Testcontainers, so Docker must be running.

## Project structure

```text
src/main/java/.../
├── auth/            registration, login, JWT, security config
├── user/            user entity and repository
├── jobapplication/  entities, service, controller, DTOs
├── reminder/        scheduled follow-up reminders
├── email/           Brevo client
└── common/          exception handling, OpenAPI config
src/main/resources/db/migration/   Flyway migrations
```


## Author

Uzochukwu Ikemdinachi · [LinkedIn](https://linkedin.com/in/uzoikemdinachi)