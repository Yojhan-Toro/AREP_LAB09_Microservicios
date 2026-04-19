# Monolito

## Configuración de Auth0

### Crear una API en Auth0

* Nombre: `Twitter Monolith API`
* Identificador (Audience): `https://twitter-monolith-api`
* Algoritmo de Firma: RS256

---

## Prerrequisitos

* Java 17+
* Maven 3.8+

## Variables de Entorno

```bash
export AUTH0_DOMAIN=dev-xxxx.us.auth0.com
export AUTH0_AUDIENCE=https://twitter-monolith-api
export AUTH0_CLIENT_ID=your-spa-client-id
```

## Ejecución

```bash
cd monolith
mvn spring-boot:run
```

## Acceso

* Swagger UI: http://localhost:8080/swagger-ui.html
* Front: http://localhost:8080/index.html

---

## Ejecución de Pruebas

```bash
mvn test
```

---

## Producción (PostgreSQL)

Sobrescribe el datasource en `application.yml` o mediante variables de entorno:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/twitterdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=secret
export SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```
