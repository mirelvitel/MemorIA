# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MemorIA** is a full-stack web app that lets users read a physical book with their phone as a companion. Users select their current page, ask questions about the book, and receive AI-powered answers grounded in the book's full text via a RAG pipeline. Conversations are anchored to page numbers, creating a personal reading log over time.

**Stack**
- **Frontend**: Angular 20, standalone components, signals for state, HttpClient for API calls
- **Backend**: Spring Boot 3.5.7, Java 21, REST API under `/api/*`, layered architecture (controller → service → persistence)
- **Database**: PostgreSQL + pgvector extension (currently MySQL — migration pending)
- **AI**: Anthropic API (Claude Haiku) — server-side only, key never exposed to client
- **External APIs**: Google Books API (search + metadata), Open Library / Project Gutenberg (full text)
- **Infrastructure**: Docker Compose (pending) wrapping Angular dev server, Spring Boot, and PostgreSQL

## Commands

### Client (Angular) — run from `client/`

```bash
npm start        # Dev server on http://localhost:4200 (with API proxy to :8080)
npm run build    # Production build to dist/
npm test         # Run unit tests (Karma + Jasmine)
```

### Server (Spring Boot) — run from `server/`

```bash
./mvnw spring-boot:run              # Start dev server on http://localhost:8080
./mvnw clean package                # Build JAR
./mvnw test                         # Run all tests (JUnit 5)
./mvnw test -Dtest=ClassName        # Run a single test class
```

## Architecture

### Client (Angular 20)
- Entry: `client/src/main.ts` → `app/app.ts`
- Standalone components only — no NgModules
- Signals over RxJS for state management
- `app.routes.ts` defines routing; `app.config.ts` wires `HttpClient` and router

### Server (Spring Boot 3.5.7, Java 21)
- Entry: `server/src/main/java/com/memoria/server/ServerApplication.java`
- Context path: `/api` — all endpoints are under `/api/*`
- Layered: `controller/` → `service/` → `persistance/`
- Auth: JWT-based via Spring Security (pending)
- All API responses wrapped in a consistent response envelope

### Client–Server Communication
- Dev proxy in `client/proxy.conf.json` forwards `client:4200/api/*` → `server:8080`
- All Anthropic API calls go through the backend — the AI key is never sent to the client

### Database
- Target: PostgreSQL + pgvector (currently MySQL on `localhost:3307`, database `memoria`)
- Connection config in `server/src/main/resources/application.properties`
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