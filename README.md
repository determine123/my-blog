# InkSpace

InkSpace is a personal blog built with a Next.js frontend and a Spring Boot/PostgreSQL backend.

## Article visibility

- `GET /api/articles` only lists articles with `PUBLISHED` status.
- `GET /api/articles/{slugOrId}` is public only for `PUBLISHED` articles.
- The author can preview their own `DRAFT` article by sending a valid `Authorization: Bearer <token>` header.
- Drafts are hidden from anonymous readers and other authenticated users with an HTTP `404` response, so the API does not reveal whether a draft exists.

## Local verification

Backend tests require JDK 17:

```powershell
cd blog-backend
.\mvnw.cmd test
```

Frontend production build:

```powershell
cd blog-frontend
npm ci
npm run build
```
