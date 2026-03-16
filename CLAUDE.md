# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MemorIA** is a full-stack web app that lets users read a physical book with their phone as a companion. Users select their current page, ask questions about the book, and receive AI-powered answers grounded in the book's full text via a RAG pipeline. Conversations are anchored to page numbers, creating a personal reading log over time.

**Stack**
- **Frontend**: Angular 20, standalone components, signals for state, HttpClient for API calls
- **Backend**: Spring Boot 3.5.7, Java 21, REST API under `/api/*`, layered architecture (controller → service → persistence)
- **Database**: PostgreSQL + pgvector extension
- **AI**: Anthropic API (Claude Haiku) — server-side only, key never exposed to client
- **External APIs**: Google Books API (search + metadata), Open Library / Project Gutenberg (full text)
- **Infrastructure**: Docker Compose wrapping Spring Boot, Angular (nginx), and PostgreSQL

## Running the App

### Local development (recommended)

```bash
# Terminal 1 — database only
sudo docker compose up db

# Terminal 2 — backend (from server/)
./mvnw spring-boot:run

# Terminal 3 — frontend (from client/)
npm start
```

The `dev` Spring profile is active by default locally. It loads `application-dev.properties` (gitignored) which contains the local DB password. This file must exist on each developer's machine — it is not committed.

### Full Docker stack

```bash
sudo docker compose up --build
```

Requires a `.env` file in the project root with `DB_USER` and `DB_PASSWORD`.

### Other commands

```bash
# Client (from client/)
npm run build    # Production build
npm test         # Unit tests (Karma + Jasmine)

# Server (from server/)
./mvnw clean package                # Build JAR
./mvnw test                         # Run all tests
./mvnw test -Dtest=ClassName        # Run a single test class
```

## Architecture

### Client (Angular 20)
- Entry: `client/src/main.ts` → `app/app.ts`
- Standalone components only — no NgModules
- Signals over RxJS for state management
- `app.routes.ts` defines routing; `app.config.ts` wires `HttpClient` and router
- `proxy.conf.json` forwards `/api/*` → `localhost:8080` during local dev (`npm start` only)
- In Docker, nginx (`client/nginx.conf`) handles the same proxy role

### Server (Spring Boot 3.5.7, Java 21)
- Entry: `server/src/main/java/com/memoria/server/ServerApplication.java`
- Context path: `/api` — all endpoints are under `/api/*`
- Layered: `controller/` → `service/` → `persistance/`
- Auth: JWT-based via Spring Security (pending)
- All API responses wrapped in a consistent response envelope

### Spring Profiles
- `application.properties` — shared base config; default profile is `dev`
- `application-dev.properties` — local overrides (DB password); gitignored, never committed
- Docker overrides properties via env vars set in `docker-compose.yaml` from `.env`

### Database
- PostgreSQL + pgvector on port `5432`, database `memoria`
- In Docker: `pgvector/pgvector:pg17` image, data persisted in `postgres_data` volume
- pgvector stores chunk embeddings for RAG retrieval

### RAG Pipeline (to be built)
- Book text is chunked (~400 tokens, page-aligned) and stored in pgvector with `start_page`/`end_page`
- On each user question: embed the query, retrieve top-k chunks, boost chunks within ±30 pages of the user's current page
- Use Anthropic prompt caching on repeated book context / system prompts to reduce costs

## Domain Model

| Concept | Description |
|---|---|
| `Library` | A user's collection of books |
| `Book` | Has metadata + current page (set before asking a question) |
| `Conversation` | Scoped to a book, anchored to a specific page number |
| `Message` | User or AI turn within a conversation |
| `BookChunk` | ~400-token text slice with `start_page`, `end_page`, and a vector embedding |

## Coding Conventions

- **Java 21**: records, pattern matching, sealed classes are all fine
- **Spring**: prefer constructor injection over field injection
- **Angular**: standalone components only; signals over RxJS where possible
- **Secrets**: never hardcode — use environment variables with Spring `@Value` or `application.properties` placeholders
- **API responses**: wrap all responses in a consistent response envelope
