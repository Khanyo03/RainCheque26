# Design — Google Maps Live Event Discovery

## 1. Architecture overview

The feature uses four backend capabilities:

1. Event Service: system of record for event lifecycle. Reuse the existing Rain Cheque Event Service if present.
2. Location Service: maintains a read-optimised PostGIS projection of eligible public events.
3. Discovery Service: exposes map query APIs and delegates spatial reads to the Location Service.
4. Real-Time Service: consumes Kafka events and broadcasts update notifications over WebSockets.

The API Gateway routes external REST and WebSocket traffic. React owns Google Maps rendering. Google Maps is not a backend microservice.

## 2. Event flow

### Event publication
1. Organiser publishes an event through the existing Event Service.
2. Event Service validates publication rules and persists the event.
3. Event Service writes an `EventPublished` record to a transactional outbox in the same database transaction, if an outbox is not already implemented.
4. An outbox publisher sends the versioned message to Kafka.
5. Location Service consumes the message and upserts the public event projection.
6. Real-Time Service consumes the message and broadcasts a relevant update notification.
7. React receives the notification and refreshes or reconciles the affected marker.

### Event update
Use `EventUpdated` with the event ID, monotonically increasing version, changed public fields or an instruction to refetch, and occurrence timestamp. The Location Service ignores stale versions.

### Event cancellation
Use `EventCancelled`. Location Service removes or marks the projection ineligible. Real-Time Service broadcasts removal to relevant subscribers.

## 3. Kafka contract

Suggested topic names:
- `raincheque.events.lifecycle.v1`

Suggested message envelope:

```json
{
  "eventId": "uuid",
  "eventType": "EVENT_PUBLISHED",
  "eventVersion": 3,
  "occurredAt": "2026-11-10T11:00:00Z",
  "schemaVersion": 1,
  "payload": {
    "id": "event-uuid",
    "title": "Summer Music Festival",
    "category": "MUSIC",
    "venueName": "Example Venue",
    "address": "Johannesburg, South Africa",
    "latitude": -26.2041,
    "longitude": 28.0473,
    "startDate": "2026-12-20T16:00:00Z",
    "endDate": "2026-12-20T23:00:00Z",
    "ticketPrice": 250.00,
    "status": "PUBLISHED"
  }
}
```

Use the event ID as Kafka message key to preserve per-event ordering within a partition. The payload is illustrative; align it with the existing Rain Cheque event model and avoid publishing private fields.

Consumers must:
- Validate schema version and required fields.
- Be idempotent.
- Track the last applied event version.
- Handle duplicates and stale messages.
- Use retry and dead-letter handling for poison messages.
- Emit metrics for lag, retries, and failures.

## 4. Location projection

A projection table in the Location Service database may contain:

```sql
CREATE TABLE event_location_projection (
    event_id UUID PRIMARY KEY,
    event_version BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(80),
    venue_name VARCHAR(255),
    address TEXT,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ,
    ticket_price NUMERIC(12,2),
    status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_event_location_projection_location
ON event_location_projection USING GIST (location);

CREATE INDEX idx_event_location_projection_start_date
ON event_location_projection (start_date);

CREATE INDEX idx_event_location_projection_status
ON event_location_projection (status);
```

Use a database migration, not manual production schema changes. Confirm PostGIS is installed and enabled in the selected PostgreSQL image.

Nearby query example:

```sql
SELECT event_id, title, category, venue_name, address,
       ST_Y(location::geometry) AS latitude,
       ST_X(location::geometry) AS longitude,
       start_date, end_date, ticket_price
FROM event_location_projection
WHERE status = 'PUBLISHED'
  AND ST_DWithin(
      location,
      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
      :radiusMeters
  )
  AND start_date >= :fromDate
  AND start_date < :toDate
ORDER BY start_date
LIMIT :limit;
```

Longitude must be passed as X and latitude as Y. Validate latitude between -90 and 90 and longitude between -180 and 180.

For map bounds, use a PostGIS envelope or geography-aware bounding strategy. Explicitly handle bounds crossing the antimeridian if global coverage is ever enabled; the MVP may restrict searches to supported regional bounds.

## 5. REST API design

Expose through the gateway. Final paths must be checked against existing Rain Cheque routes to avoid collisions.

### GET `/api/discovery/events`
Query parameters:
- `north`, `south`, `east`, `west`
- `fromDate`, `toDate`
- `category` (repeatable or comma-separated, choose one contract)
- `limit` (default 100, maximum 200)
- `cursor` or page parameters if pagination is required

Returns a bounded list of public event map summaries and a next cursor where applicable.

### GET `/api/discovery/events/nearby`
Query parameters:
- `latitude`
- `longitude`
- `radiusMeters`
- `fromDate`
- `toDate`
- `category`
- `limit`

Returns events within the requested radius.

### GET `/api/discovery/events/{eventId}`
Returns the public map summary for one event if eligible.

Use a consistent response envelope, for example:

```json
{
  "items": [
    {
      "eventId": "uuid",
      "title": "Summer Music Festival",
      "category": "MUSIC",
      "venueName": "Example Venue",
      "latitude": -26.2041,
      "longitude": 28.0473,
      "startDate": "2026-12-20T16:00:00Z",
      "ticketPrice": 250.00,
      "eventUrl": "/events/uuid",
      "version": 3
    }
  ],
  "nextCursor": null
}
```

Use appropriate HTTP status codes and a standard error body. Do not expose internal exception messages.

## 6. WebSocket design

Suggested endpoint: `/ws/discovery`.

Suggested STOMP destination:
- Client subscribes to `/topic/discovery/updates` for a simple MVP global feed, or a bounded geographic subscription if implemented safely.
- Server sends event update messages with event ID, event type, version, and optionally a compact public summary.

A global topic is acceptable only for a small MVP deployment with modest event volume. Prefer viewport- or region-scoped subscriptions before scaling broadly. Validate subscription permissions and prevent clients from creating unbounded subscriptions.

The frontend sequence:
1. Fetch current HTTP snapshot.
2. Connect and subscribe to WebSocket.
3. Apply updates by event ID and version.
4. On disconnect, show reconnecting status.
5. On reconnect, refetch current snapshot and then resume update handling.

Do not treat WebSocket as durable event storage.

## 7. Frontend design

Components:
- `EventDiscoveryPage.jsx`: page composition and URL/query state.
- `EventDiscoveryMap.jsx`: Google Map lifecycle and viewport.
- `EventMapMarker.jsx`: marker representation.
- `EventInfoWindow.jsx`: selected event summary.
- `MapSearch.jsx`: Places Autocomplete.
- `MapFilters.jsx`: category and date filters.
- `useMapEvents.js`: HTTP snapshot fetching, cancellation, debouncing.
- `useLiveMapUpdates.js`: WebSocket connection and reconnect handling.
- `discoveryService.js`: Axios requests.
- `mapSocketService.js`: socket/STOMP connection abstraction.

Frontend rules:
- Use the existing React version and conventions.
- Use `.js` and `.jsx`, not TypeScript.
- Do not create duplicate event cards or API clients if existing components can be reused.
- Debounce viewport searches and cancel stale requests.
- Avoid recreating the map instance on every React render.
- Cluster markers when useful.
- Keep API keys out of source control.
- Provide loading, empty, permission-denied, offline, and error states.
- Ensure keyboard-accessible event cards and controls.

## 8. Google Maps configuration

Enable only the APIs actually used:
- Maps JavaScript API.
- Places API if using autocomplete.
- Routes API only if directions are added later; not required for the discovery MVP.
- Geocoding API only if address-to-coordinate conversion is needed and not already supplied by Places or organiser-entered coordinates.

Use a browser key restricted by HTTP referrer and API. Configure billing alerts and quotas. Never put server-side secrets in frontend configuration.

## 9. Resilience and observability

- HTTP timeouts between services.
- Bounded retries with backoff for transient Kafka processing failures.
- Dead-letter topic for messages that cannot be processed.
- Correlation ID propagated through gateway and services.
- Health and readiness endpoints.
- Metrics: request latency, query count, Kafka consumer lag, socket connection count, update delivery count, projection failures.
- Structured logs; never log JWTs or API keys.

## 10. Deployment

For local MVP:
- Docker Compose for PostgreSQL/PostGIS and Kafka.
- Run Spring Boot services locally or in containers.
- Run React development server locally.
- Redis is optional; omit until measured need exists.

For deployment:
- Deploy services using the existing platform conventions.
- Configure secrets externally.
- Use managed PostgreSQL where practical.
- Add Kubernetes only if it is already part of the platform deployment approach; it is not a prerequisite for proving the MVP.
