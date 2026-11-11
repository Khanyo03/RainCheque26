# Requirements — Google Maps Live Event Discovery

## 1. Purpose
Implement an MVP location-based event discovery experience in the existing Rain Cheque platform using React JavaScript, Java 17, Spring Boot, Google Maps, PostgreSQL/PostGIS, Kafka, and WebSockets.

## 2. Actors
- Visitor or authenticated attendee: searches and views public events.
- Organiser: creates, publishes, updates, or cancels events using existing event-management functionality.
- Platform services: maintain a geographical read projection and notify connected map clients.

## 3. Functional requirements

### FR-1 Map display
The system shall display an interactive Google Map on an Event Discovery page.
The map shall initially use a configured default centre, not require browser geolocation.
If the user grants location permission, the UI may offer a "near me" action.
The map shall show loading, empty, and error states.

### FR-2 Event markers
The system shall display markers only for events that are published, not cancelled, and eligible under the selected date filters.
Selecting a marker shall display a concise event card containing title, venue, start date/time, category, and ticket price when public.
The card shall link to the existing event details route.
The map shall cluster markers at low zoom levels or when marker density warrants it.

### FR-3 Search and filters
Users shall be able to search by location text using Places Autocomplete where enabled.
Users shall be able to filter by category and date range.
Users shall be able to search the visible map area.
The UI shall provide a clear-filters action.
The backend shall validate all filter inputs.

### FR-4 Nearby search
The system shall support a nearby search using latitude, longitude, and radius in metres.
The MVP shall configure a safe maximum radius, initially 50,000 metres, subject to product approval.
The backend shall reject invalid coordinates, negative radius, and radius above the configured maximum.

### FR-5 Map bounds search
The system shall support searching within north, south, east, and west map bounds.
The backend shall reject invalid or excessively large bounds.
The frontend shall debounce map movement and avoid a request for every intermediate pan event.
The frontend shall request a new snapshot after the user stops moving the map.

### FR-6 Live updates
When an event is published, updated, or cancelled, the Event Service shall emit a versioned domain event through Kafka.
The Location Service shall idempotently update or remove the geographical projection.
The Real-Time Service shall notify relevant connected clients.
The frontend shall update markers without a full page refresh.
On socket reconnect, the frontend shall refetch the current map snapshot to recover missed messages.

### FR-7 Security and privacy
Only public event fields shall be returned by discovery endpoints.
The system shall not track attendees or organisers.
The browser Google Maps key shall be restricted by HTTP referrer and enabled APIs.
Server credentials and JWT signing secrets shall never be exposed to the browser.

### FR-8 Reliability
Kafka consumers shall be idempotent.
The system shall handle duplicate and out-of-order event messages using event ID and version / updated timestamp.
If WebSocket is unavailable, the map shall remain usable through HTTP snapshot requests and a visible degraded-state indication.
The system shall log failures with correlation IDs without logging secrets or sensitive user data.

## 4. Non-functional requirements
- Initial map snapshot API target: p95 under 800 ms under agreed MVP test load, excluding Google Maps tile rendering.
- Map query responses shall be bounded and support a configurable result limit.
- Initial default result limit: 100 events; maximum: 200, configurable.
- Nearby searches shall use a spatial index.
- WebSocket messages shall be small notifications or compact public event payloads.
- All timestamps shall be represented consistently in UTC over service boundaries.
- APIs shall be documented with OpenAPI.
- Services shall expose health/readiness checks.
- All critical logic shall have automated tests.

## 5. Acceptance criteria
1. A user can open the discovery page and see published events on a map.
2. Cancelled and unpublished events are not displayed.
3. Selecting a marker shows correct public event information.
4. Selecting the event card opens the existing event details page.
5. Searching a location or moving the map returns events in the requested area.
6. Category and date filters work individually and together.
7. A newly published event appears for a subscribed user without manually refreshing the page.
8. An updated event marker reflects the new public data.
9. A cancelled event marker is removed.
10. A reconnecting browser fetches a fresh snapshot and does not rely on missed socket messages.
11. Invalid coordinates, radii, and bounds return a documented 4xx response.
12. Existing event creation, ticketing, authentication, and event details flows continue to work.
13. No TypeScript files are introduced.
