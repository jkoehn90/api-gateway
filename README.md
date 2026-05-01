# API Gateway — Microservices System

Part of the **Microservices System** — a distributed backend built with Java, Spring Boot, and Spring Cloud.

## Overview

The API Gateway is the **single entry point** for all client requests in the Microservices System. It handles JWT authentication centrally, routes requests to the appropriate downstream service via Eureka service discovery, and returns responses back to the client. No request reaches any service without passing through the Gateway first.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 3.5.x | Application framework |
| Spring Cloud Gateway (WebFlux) | Reactive API gateway |
| Spring Security (WebFlux) | Reactive security configuration |
| JWT (jjwt 0.11.5) | Token validation |
| Lombok | Boilerplate reduction |
| Netflix Eureka Client | Service discovery |

## Architecture Role

```
Client
  │
  ▼
API Gateway (port 8080)
  │
  ├── AuthenticationFilter
  │     ├── /auth/register → skip auth (open endpoint)
  │     ├── /auth/login    → skip auth (open endpoint)
  │     └── all others     → validate JWT
  │
  ├── /auth/**      → lb://user-service    (port 8081)
  ├── /products/**  → lb://product-service (port 8082)
  └── /orders/**    → lb://order-service   (port 8083)
```

## Routing Configuration

| Path Pattern | Routes To | Auth Required |
|---|---|---|
| `/auth/**` | User Service | ❌ (register/login only) |
| `/products/**` | Product Service | ✅ |
| `/orders/**` | Order Service | ✅ |

## Authentication Flow

```
Incoming Request
      │
      ▼
Is it /auth/register or /auth/login?
      │
   Yes → Forward directly to User Service
      │
   No  → Check Authorization header
              │
         Missing → 401 Unauthorized
              │
         Present → Extract Bearer token
              │
         Invalid/Expired → 401 Unauthorized
              │
         Valid → Forward to correct service
```

## Project Structure

```
src/main/java/com/yourname/apigateway/
├── filter/
│   ├── AuthenticationFilter.java
│   └── JwtUtil.java
└── config/
    └── SecurityConfig.java
```

## Getting Started

### Prerequisites
- Java 17+
- Maven
- Eureka Server running on port `8761`
- All downstream services running

### Running Locally
```bash
mvn spring-boot:run
```

### Configuration
Update `src/main/resources/application.yml` with your JWT secret — must match the User Service secret exactly:
```yaml
jwt:
  secret: your-256-bit-secret-key-here
```

## Using the API

### 1. Register a new user
```bash
POST http://localhost:8080/auth/register
Content-Type: application/json

{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@email.com",
    "password": "secret123"
}
```

### 2. Login to get a token
```bash
POST http://localhost:8080/auth/login
Content-Type: application/json

{
    "email": "john@email.com",
    "password": "secret123"
}
```

### 3. Use the token for protected endpoints
```bash
GET http://localhost:8080/products
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## Related Services

| Service | Port | Repo |
|---|---|---|
| Eureka Server | 8761 | [eureka-server](../eureka-server) |
| User Service | 8081 | [user-service](../user-service) |
| Product Service | 8082 | [product-service](../product-service) |
| Order Service | 8083 | [order-service](../order-service) |