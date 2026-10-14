# ott-platform

An OTT streaming platform (Netflix/Hotstar/Prime Video-style) — Spring Boot microservices.

## Modules

- `common-lib`, `eureka-server`, `config-server`, `api-gateway` — platform foundation
- `user-auth-service` — identity & auth
- `content-service` — catalog metadata, admin/CMS publishing, and search (merges the former
  catalog, cms-admin, and search services)
- `subscription-billing-service` — plans, subscriptions & payments
- `media-service` — video transcoding pipeline and streaming playback (merges the former
  video-transcoding and streaming-playback services)
- `engagement-service` — notifications, audit log, analytics, recommendations, and reviews
  (merges the former notification, audit-log, analytics, recommendation, and review-rating
  services)

## Local dev infra

```
docker compose -f infra/docker-compose.yml up -d
```

## Branching

- `main` — production
- `uat` — staging
- `develop` — integration

Branch off `develop` for each piece of work, open a PR back into `develop`.
`develop` → `uat` → `main` only ever move by merge.
