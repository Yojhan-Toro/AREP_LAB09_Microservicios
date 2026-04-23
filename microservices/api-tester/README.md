# Twitterito Frontend para Microservices

Este frontend replica el flujo del monolito (login, crear post, feed) pero usando 3 microservicios:

- User Service: 8081
- Post Service: 8082
- Stream Service: 8083

## 1) Levantar microservicios

Antes de arrancar cada servicio, copia su `.env.example` a `.env` si no existe.

Con Docker:

```powershell
cd ..
docker compose up -d
```

O en 3 terminales con Maven:

```powershell
cd ../user-service
mvn spring-boot:run

cd ../post-service
mvn spring-boot:run

cd ../stream-service
mvn spring-boot:run
```

## 2) Levantar frontend

Este frontend es estatico, no usa Maven. Corre en el puerto 8080 (igual que el monolith).

```powershell
python -m http.server 8080
```

Abre en navegador:

http://localhost:8080/

## 3) Swagger por servicio

- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html
- http://localhost:8083/swagger-ui.html

## Notas

- Si se cae un microservicio, solo falla la funcionalidad asociada a ese servicio.
- Si no tienes sesion Auth0, el feed publico sigue funcionando pero publicar y perfil no.
- En Auth0, registra callback y logout con `http://localhost:8080/index.html`.
- En Auth0, registra web origin con `http://localhost:8080`.
- El frontend y el monolith no pueden correr al mismo tiempo (ambos usan el puerto 8080).