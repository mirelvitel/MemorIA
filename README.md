# MemorIA — AI Reading Companion

A mobile-responsive web app that acts as an intelligent reading companion for physical books.
The idea is simple: you're sitting with a real book in your hands and your phone on the side. You hit a passage that confuses you — an unfamiliar word, a complex idea, a concept you want to explore further. You open the app, tell it what page you're on, and ask your question. The AI, which has already read the entire book, answers you with full context — then you put the phone down and keep reading.
Unlike existing "chat with PDF" tools that treat a book as a flat document you search through, this app models reading as a progressive journey. Every conversation is anchored to a specific page, the AI never reveals what comes after where you are, and your conversation history becomes a personal map of the moments that made you think.

## Core features

- Search for any book by title or author, or upload your own PDF
- Set your current page before asking — the AI stays within what you've read
- Conversations are anchored to page numbers and saved to your reading history
- Each book in your library has its own timeline of discussions
- Full user authentication — your library and history are private to you

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 20, standalone components, signals |
| Backend | Spring Boot 3.5.7, Java 21 |
| Database | PostgreSQL 17 + pgvector |
| AI | Anthropic API (Claude Haiku) — server-side only |
| Infrastructure | Docker Compose |

## Getting Started

### Prerequisites

- Java 21
- Node.js 22
- Docker

### Local Development

The recommended workflow runs only the database in Docker, with the frontend and backend running natively for live reload.

**1. Create `server/src/main/resources/application-dev.properties`** (gitignored):
```properties
spring.datasource.password=your_db_password
```

**2. Start the database:**
```bash
sudo docker compose up db
```

**3. Start the backend** (from `server/`):
```bash
./mvnw spring-boot:run
```

**4. Start the frontend** (from `client/`):
```bash
npm start
```

Open [http://localhost:4200](http://localhost:4200). API calls are proxied to the backend at `localhost:8080`.

### Full Docker Stack

Create a `.env` file in the project root:
```
DB_USER=memoria
DB_PASSWORD=your_password
```

Then run:
```bash
sudo docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200).

## Project Structure

```
MemorIA/
├── client/          # Angular frontend
├── server/          # Spring Boot backend
└── docker-compose.yaml
```

The backend exposes all endpoints under `/api/*`. In local dev, the Angular dev server proxies these requests via `proxy.conf.json`. In Docker, nginx handles the same proxy role.

## Roadmap

- [ ] JWT authentication (Spring Security)
- [ ] Google Books API integration (search + metadata)
- [ ] Full text sourcing (Open Library / Project Gutenberg)
- [ ] RAG pipeline (chunking, pgvector embeddings, retrieval)
- [ ] Anthropic API integration (page-aware Q&A)
- [ ] Conversation history per book