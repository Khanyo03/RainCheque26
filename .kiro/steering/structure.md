# Repository and Service Structure

Adapt this structure to the existing Rain Cheque repository. Do not duplicate an existing service or frontend.

```text
rain-cheque/
├── frontend/
│   └── RAIN-CHEQUE/
│       └── src/
│           ├── components/map/
│           │   ├── EventDiscoveryMap.jsx
│           │   ├── EventMapMarker.jsx
│           │   ├── EventInfoWindow.jsx
│           │   ├── MapSearch.jsx
│           │   └── MapFilters.jsx
│           ├── hooks/
│           │   ├── useMapEvents.js
│           │   └── useLiveMapUpdates.js
│           ├── services/
│           │   ├── discoveryService.js
│           │   └── mapSocketService.js
│           └── pages/
│               └── EventDiscoveryPage.jsx
├── services/
│   ├── event-service/          # Extend existing service if present
│   ├── location-service/
│   ├── discovery-service/
│   └── realtime-service/
├── infrastructure/
│   ├── kafka/
│   ├── postgres/
│   └── docker/
└── docker-compose.yml
```

Each Spring Boot service should follow the established project conventions. A typical service layout is:

```text
src/main/java/<base-package>/
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── mapper/
├── repository/
├── service/
└── messaging/
src/main/resources/
├── db/migration/
└── application.yml
```

Use Flyway or the migration tool already present in the project. Do not add a second migration framework.
