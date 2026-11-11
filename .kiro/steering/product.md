# Product Steering — Live Event Discovery

## Product
Rain Cheque Live Event Discovery is a location-based event discovery feature integrated into the existing Rain Cheque ticketing platform.

## Problem
Users need a convenient way to discover published events around a chosen location without manually searching through long event lists.

## Goals
- Display published events on an interactive Google Map.
- Search by city, venue, address, and visible map area.
- Find events within a configurable radius.
- Filter by event category and date range.
- Show concise event details from map markers.
- Reflect event publication, update, and cancellation changes without a page refresh.
- Link users to the existing event details and ticket-purchase flow.

## MVP users
- Event attendees discovering events.
- Event organisers publishing and updating events through the existing platform.

## MVP scope
- Google Maps JavaScript API integration in React.
- Event markers, marker clustering, info windows, map bounds search.
- Nearby search using PostGIS.
- Category and date filters.
- Kafka-driven location projection updates.
- WebSocket push notifications for relevant map updates.
- Authentication and authorisation consistent with the existing Rain Cheque application.

## Out of scope
- Attendee or organiser live GPS tracking.
- Turn-by-turn navigation implementation.
- Ticket checkout inside map markers.
- AI recommendations.
- Complex geofencing and multi-region deployment.
- Building a new identity service if Rain Cheque already has authentication.

## Product principles
- The map is a discovery interface, not a source of truth.
- The Event Service remains the system of record for event details and lifecycle.
- Only published, non-cancelled events within the requested date rules appear.
- Do not expose private organiser or attendee information.
- Preserve existing Rain Cheque routes, APIs, authentication, design conventions, and JSX preference.
