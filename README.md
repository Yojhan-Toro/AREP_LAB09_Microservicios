# Twitter-like Application – Monolith → Microservices

A simplified Twitter-like application that allows authenticated users to create short posts (max 140 characters) displayed in a single public stream.  
The project starts as a **Spring Boot monolith** and is then decomposed into **three serverless microservices** deployed on **AWS Lambda**, all secured with **Auth0**.

---

## Architecture Overview

### Phase 1 – Monolith

```
Browser / S3 Frontend
        │  HTTPS + JWT (Auth0)
        ▼
Spring Boot Monolith (port 8080)
  ├── UserController   → GET /api/me, GET /api/me/posts
  ├── PostController   → GET/POST /api/posts
  ├── StreamController → GET /api/stream
  └── H2 / PostgreSQL
```

### Phase 2 – Microservices (current branch)

```
Browser / S3 Frontend
        │  HTTPS + JWT (Auth0)
        ▼
AWS API Gateway (HTTP API)
  ├── /api/me, /api/me/posts  ──► Lambda: user-service   ─┐
  ├── /api/posts/**           ──► Lambda: post-service   ─┤── RDS PostgreSQL
  └── /api/stream             ──► Lambda: stream-service ─┘
```

Each Lambda is an independent Spring Boot application packaged as an uber-JAR using the [`aws-serverless-java-container`](https://github.com/aws/serverless-java-container) library.

### Auth0 Security Flow

```
User ──login──► Auth0 ──JWT──► Frontend
Frontend ──Bearer JWT──► API Gateway ──► Lambda (validates JWT via Auth0 JWKS)
```

---

## Repository Structure

```
AREP_LAB09_Microservicios/
├── monolith/                   Spring Boot monolith (Phase 1)
│   └── src/...
└── microservices/              Serverless microservices (Phase 2)
    ├── user-service/           GET /api/me, GET /api/me/posts
    ├── post-service/           GET|POST /api/posts, GET /api/posts/{id|user}
    ├── stream-service/         GET /api/stream  (public)
    └── template.yaml           AWS SAM deployment template
```

---

## Auth0 Configuration

### Create an API in Auth0

| Field | Value |
|---|---|
| Name | `Twitter Monolith API` |
| Identifier (Audience) | `https://twitter-monolith-api` |
| Signing Algorithm | `RS256` |

### Create a Single-Page Application in Auth0

- Application Type: **Single Page Application**
- Allowed Callback URLs: your S3 URL and `http://localhost:3000`
- Allowed Logout URLs: same as above
- Allowed Web Origins: same as above

### Scopes / Permissions (recommended)

Add these permissions to the API:

| Permission | Description |
|---|---|
| `read:posts` | Read posts and stream |
| `write:posts` | Create posts |
| `read:profile` | Read own profile |

---

## Phase 1 – Monolith

### Prerequisites

- Java 17+
- Maven 3.8+

### Environment Variables

```bash
export AUTH0_DOMAIN=dev-xxxx.us.auth0.com
export AUTH0_AUDIENCE=https://twitter-monolith-api
export AUTH0_CLIENT_ID=your-spa-client-id
```

### Run Locally

```bash
cd monolith
mvn spring-boot:run
```

### Access

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Frontend | http://localhost:8080/index.html |
| H2 Console | http://localhost:8080/h2-console |

### Run Tests

```bash
cd monolith
mvn test
```

### Production – PostgreSQL

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/twitterdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=secret
export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```

---

## Phase 2 – Microservices on AWS Lambda

### Prerequisites

- Java 17+
- Maven 3.8+
- [AWS CLI](https://aws.amazon.com/cli/) configured (`aws configure`)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)
- An RDS PostgreSQL instance (or any accessible PostgreSQL database)

### Service Breakdown

| Service | Endpoints | Auth |
|---|---|---|
| **user-service** | `GET /api/me` `GET /api/me/posts` | JWT required |
| **post-service** | `GET /api/posts` `GET /api/posts/{id}` `GET /api/posts/user/{auth0Id}` | Public |
| **post-service** | `POST /api/posts` | JWT required |
| **stream-service** | `GET /api/stream` | Public |

### Build All Services

```bash
cd microservices

# Build each service (produces a shaded uber-JAR for Lambda)
cd user-service  && mvn package -DskipTests && cd ..
cd post-service  && mvn package -DskipTests && cd ..
cd stream-service && mvn package -DskipTests && cd ..
```

### Deploy to AWS with SAM

```bash
cd microservices

sam deploy --guided \
  --stack-name twitter-microservices \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    Auth0Domain=dev-xxxx.us.auth0.com \
    Auth0Audience=https://twitter-monolith-api \
    DbUrl=jdbc:postgresql://<RDS-HOST>:5432/twitterdb \
    DbUsername=postgres \
    DbPassword=<YOUR-PASSWORD>
```

After deployment, SAM prints the API Gateway base URL in the **Outputs** section.  
Use this URL in the frontend's API configuration.

### Local Testing with SAM

```bash
cd microservices
sam local start-api \
  --env-vars env.json
```

`env.json` example:

```json
{
  "UserServiceFunction": {
    "AUTH0_DOMAIN": "dev-xxxx.us.auth0.com",
    "AUTH0_AUDIENCE": "https://twitter-monolith-api",
    "SPRING_DATASOURCE_URL": "jdbc:postgresql://host.docker.internal:5432/twitterdb",
    "SPRING_DATASOURCE_USERNAME": "postgres",
    "SPRING_DATASOURCE_PASSWORD": "secret"
  },
  "PostServiceFunction": { "...": "same vars" },
  "StreamServiceFunction": { "...": "same vars" }
}
```

### Environment Variables (each Lambda)

| Variable | Description |
|---|---|
| `AUTH0_DOMAIN` | Auth0 tenant domain |
| `AUTH0_AUDIENCE` | API audience identifier |
| `SPRING_DATASOURCE_URL` | JDBC URL to RDS PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |

---

## API Endpoints Reference

### User Service – `GET /api/me`
Returns the authenticated user's profile (JIT provisioning on first call).  
**Authorization:** `Bearer <JWT>`

### User Service – `GET /api/me/posts`
Returns all posts created by the authenticated user.  
**Authorization:** `Bearer <JWT>`

### Post Service – `GET /api/posts`
Returns all posts (no pagination). Public.

### Post Service – `GET /api/posts/{id}`
Returns a single post by ID. Public.

### Post Service – `POST /api/posts`
Creates a new post (max 140 chars).  
**Authorization:** `Bearer <JWT>`  
**Body:** `{ "content": "Hello world!" }`

### Post Service – `GET /api/posts/user/{auth0Id}`
Returns all posts by a specific user. Public.

### Stream Service – `GET /api/stream?page=0&size=20`
Returns the public paginated post feed, newest first. Public.

---

## Testing

### Monolith Unit Tests

```bash
cd monolith && mvn test
```

### Testing the APIs Manually

Use the monolith's Swagger UI at `http://localhost:8080/swagger-ui.html` to test all endpoints interactively with Auth0 OAuth2 authentication.

For the microservices, use curl or any REST client:

```bash
# Public – no token needed
curl https://<API_GW_URL>/api/stream

# Protected – Bearer token required
curl -H "Authorization: Bearer <JWT>" \
     https://<API_GW_URL>/api/me

# Create a post
curl -X POST \
     -H "Authorization: Bearer <JWT>" \
     -H "Content-Type: application/json" \
     -d '{"content":"Hello from microservices!"}' \
     https://<API_GW_URL>/api/posts
```

---

## Security Notes

- All JWTs are RS256-signed by Auth0 and validated using the JWKS endpoint
- The audience claim is validated to prevent token misuse across services
- CORS is configured to allow requests from any origin (restrict to your S3 URL in production)
- **Never commit** `AUTH0_DOMAIN`, `AUTH0_AUDIENCE`, `SPRING_DATASOURCE_PASSWORD`, or any other secret to Git

---

## Frontend (S3)

The frontend is a JavaScript SPA deployed as a static website on Amazon S3.  
It uses the **Auth0 SPA JS SDK** to handle login, logout, silent token refresh, and Bearer token injection.

> Frontend deployment and URL: _to be filled by the frontend developer_

---

## Live Links

| Resource | URL |
|---|---|
| Frontend (S3) | _TBD_ |
| Swagger UI (monolith) | _TBD – screenshot in `/docs`_ |
| API Gateway Base URL | _TBD after SAM deploy_ |
