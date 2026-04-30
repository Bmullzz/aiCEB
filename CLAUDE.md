# CLAUDE.md

# Event dashboard — project context

## Tech stack
- Backend: Java 21, Spring Boot 3.x, Spring MVC, Spring Data JPA, Spring Security, Spring Scheduler
- Frontend: React 18, fetching from REST API
- Database: PostgreSQL
- SMS: Twilio

## Architecture
The system is organized into four layers that communicate in a single top-down chain.
Physical devices are the entry point for end users. iPads run the React frontend in kiosk mode and handle two interactions: browsing the event list and submitting the SMS signup form. TVs are read-only displays that auto-refresh the upcoming events feed on a short polling interval (30–60 seconds). Neither device type requires a native app — both run the React frontend in a browser.
React frontend is a single-page application that communicates with the Spring Boot backend exclusively via REST API calls returning JSON. It serves four main views: the event list (paginated, filterable by category and date), the event detail screen, the SMS signup modal, and the admin UI for managing events and categories. The React build can either be served as static files from within the Spring Boot application under /resources/static, or deployed separately to a CDN with CORS configured on the backend.
Spring Boot backend is the application core and is responsible for all business logic, data persistence, scheduled notifications, and external service integration. It is organized into three functional slices that map to three controllers: event management (EventController, read-heavy, powers the displays), subscriptions (SubscriptionController, handles kiosk signups and opt-outs), and administration (AdminController, protected by JWT, handles CRUD operations and manual broadcasts). Each controller delegates entirely to a corresponding service layer — EventService, SubscriptionService, and a background NotificationScheduler — which in turn talk to JPA repositories. No controller accesses a repository directly. The public endpoints (/api/events/** and /api/subscriptions/**) require no authentication. All /api/admin/** routes are protected by Spring Security with JWT and require the ADMIN role.
External services are three dependencies the backend integrates with. PostgreSQL is the primary datastore, accessed via Spring Data JPA. Spring Security with JWT handles admin authentication. Twilio (or AWS SNS as an alternative) handles SMS delivery and is wrapped behind a SmsService interface so the provider can be swapped or stubbed in tests. The NotificationScheduler uses Spring's @Scheduled to query for upcoming events at regular intervals, find their active subscribers, fire reminder SMS messages at the configured offsets (e.g. 60 and 15 minutes before start), and write each delivery attempt to the notification_log table.


---

## Data model

The database has four tables. All primary keys are UUIDs. All status fields are stored as strings (not integers) using `@Enumerated(EnumType.STRING)` in JPA.

---

### `event_category`

Lookup table for event types. Referenced by `event`.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Generated via `@UuidGenerator` |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE | e.g. "Workshop", "Lecture" |
| `color` | VARCHAR(7) | NOT NULL | Hex color string e.g. `#5DCAA5` |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Soft disable without deleting |
| `created_at` | TIMESTAMP | NOT NULL | Set once on insert |

---

### `event`

Core table. One row per event.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Generated via `@UuidGenerator` |
| `category_id` | UUID | FK → event_category.id, NOT NULL | |
| `title` | VARCHAR(255) | NOT NULL | |
| `description` | TEXT | NULLABLE | |
| `location` | VARCHAR(255) | NOT NULL | Room name or building |
| `start_time` | TIMESTAMP | NOT NULL | Stored in UTC |
| `end_time` | TIMESTAMP | NOT NULL | Stored in UTC |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'UPCOMING' | Enum: `UPCOMING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `alert_offset_minutes` | INTEGER | NOT NULL, DEFAULT 60 | First reminder — minutes before start |
| `alert_offset_minutes_2` | INTEGER | NULLABLE | Second reminder — minutes before start (e.g. 15). Null means only one reminder. |
| `visible` | BOOLEAN | NOT NULL, DEFAULT true | Controls whether event appears on public displays |
| `created_at` | TIMESTAMP | NOT NULL | Set once on insert |
| `updated_at` | TIMESTAMP | NOT NULL | Updated on every write |

**Indexes:**
- `idx_event_start_time` on `(start_time)` — scheduler and display queries filter by start time constantly
- `idx_event_status` on `(status)` — scheduler filters to `UPCOMING` only
- `idx_event_visible_start` on `(visible, start_time)` — composite index for the main display query

---

### `subscription`

One row per phone number per event. Tracks who has signed up for SMS alerts.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Generated via `@UuidGenerator` |
| `event_id` | UUID | FK → event.id, NOT NULL | |
| `phone_number` | VARCHAR(20) | NOT NULL | Stored in E.164 format e.g. `+12155550123` |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | Enum: `ACTIVE`, `OPTED_OUT` |
| `subscribed_at` | TIMESTAMP | NOT NULL | Set once on insert |
| `opted_out_at` | TIMESTAMP | NULLABLE | Set when status transitions to `OPTED_OUT` |

**Constraints:**
- `UNIQUE(event_id, phone_number)` — prevents duplicate subscriptions for the same event

**Indexes:**
- `idx_subscription_event_id` on `(event_id)` — scheduler looks up all subscribers by event
- `idx_subscription_event_status` on `(event_id, status)` — scheduler filters to `ACTIVE` subscribers only

---

### `notification_log`

Audit table. One row per SMS send attempt. Used to prevent duplicate sends and track delivery status.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Generated via `@UuidGenerator` |
| `subscription_id` | UUID | FK → subscription.id, NOT NULL | |
| `offset_minutes` | INTEGER | NOT NULL | Which reminder this was (e.g. 60 or 15) |
| `message_sid` | VARCHAR(64) | NULLABLE | Twilio message SID — populated after successful API call |
| `delivery_status` | VARCHAR(20) | NOT NULL, DEFAULT 'QUEUED' | Enum: `QUEUED`, `SENT`, `DELIVERED`, `FAILED` |
| `sent_at` | TIMESTAMP | NOT NULL | When the send attempt was made |
| `updated_at` | TIMESTAMP | NOT NULL | Updated when Twilio status callback fires |

**Constraints:**
- `UNIQUE(subscription_id, offset_minutes)` — idempotency guard, prevents the scheduler from sending the same reminder twice

**Indexes:**
- `idx_notification_log_subscription_id` on `(subscription_id)` — used when checking whether a reminder has already been sent

---

### Relationships

```
event_category  ──< event ──< subscription ──< notification_log
```

- One category has many events
- One event has many subscriptions
- One subscription has many notification log entries (one per reminder offset)

---

### JPA implementation notes

- All entities extend a `BaseEntity` abstract class carrying `id`, `createdAt`, and `updatedAt`, with `@PrePersist` and `@PreUpdate` lifecycle hooks setting the timestamps automatically
- Use `@Column(nullable = false)` consistently — do not rely on the database alone to enforce nullability
- The `UNIQUE(subscription_id, offset_minutes)` constraint on `notification_log` is your scheduler's idempotency guard — catch `DataIntegrityViolationException` in `NotificationScheduler` and treat it as "already sent" rather than a hard error
- Phone numbers are stored and transmitted in E.164 format throughout — validate on the way in using libphonenumber, never transform on the way out
- All timestamps are stored in UTC. Apply `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` in `application.properties`

---


## API contract
Here's the API contract section, ready to paste directly into `CLAUDE.md`:

---

## API contract

All endpoints are prefixed with `/api`. All request and response bodies are `application/json`. All timestamps are ISO 8601 in UTC. All IDs are UUIDs. Errors follow a standard envelope shape across all controllers.

### Standard error envelope

All error responses use this shape:

```json
{
  "status": 422,
  "code":   "EVENT_CANCELLED",
  "message": "This event has been cancelled and is no longer accepting signups."
}
```

### Error codes

| Code | HTTP status | Meaning |
|---|---|---|
| `EVENT_NOT_FOUND` | 404 | No event exists with that ID |
| `EVENT_CANCELLED` | 422 | Event exists but is cancelled |
| `EVENT_IN_PAST` | 422 | Event start time has already passed |
| `CATEGORY_NOT_FOUND` | 404 | No category exists with that ID |
| `SUBSCRIPTION_NOT_FOUND` | 404 | No subscription exists with that ID |
| `ALREADY_SUBSCRIBED` | 409 | Phone number is already actively subscribed to this event |
| `ALREADY_OPTED_OUT` | 409 | Subscription is already in OPTED_OUT state |
| `INVALID_PHONE_NUMBER` | 400 | Phone number failed E.164 validation |
| `INVALID_REQUEST` | 400 | Missing required field or malformed body |
| `UNAUTHORIZED` | 401 | JWT token missing or invalid |
| `FORBIDDEN` | 403 | Valid JWT but insufficient role |

---

### EventController — `/api/events`

All routes are public. No authentication required.

---

#### `GET /api/events`

Returns a paginated list of upcoming visible events. Powers the main kiosk and TV display.

**Query parameters:**

| Parameter | Type | Required | Default | Notes |
|---|---|---|---|---|
| `page` | integer | no | 0 | Zero-based page index |
| `size` | integer | no | 20 | Results per page |
| `categoryId` | UUID | no | — | Filter by category |
| `from` | ISO 8601 date | no | today | Earliest start date |
| `to` | ISO 8601 date | no | — | Latest start date |
| `sort` | string | no | `startTime,asc` | `startTime,asc` or `startTime,desc` |

**Response 200:**

```json
{
  "content": [
    {
      "id":              "uuid",
      "title":           "string",
      "description":     "string",
      "location":        "string",
      "startTime":       "2026-05-01T14:00:00Z",
      "endTime":         "2026-05-01T15:30:00Z",
      "category": {
        "id":    "uuid",
        "name":  "string",
        "color": "#5DCAA5"
      },
      "subscriberCount": 42,
      "status":          "UPCOMING"
    }
  ],
  "page":          0,
  "size":          20,
  "totalElements": 84,
  "totalPages":    5
}
```

---

#### `GET /api/events/{id}`

Returns full detail for a single event. Shown when a user taps an event on an iPad.

**Path parameter:** `id` — UUID of the event.

**Response 200:**

```json
{
  "id":                   "uuid",
  "title":                "string",
  "description":          "string",
  "location":             "string",
  "startTime":            "2026-05-01T14:00:00Z",
  "endTime":              "2026-05-01T15:30:00Z",
  "category": {
    "id":    "uuid",
    "name":  "string",
    "color": "#5DCAA5"
  },
  "alertOffsets":         [60, 15],
  "subscriberCount":      42,
  "status":               "UPCOMING",
  "visible":              true
}
```

**Error responses:** `EVENT_NOT_FOUND` (404)

---

#### `GET /api/events/upcoming`

Returns the next N events from now. Optimized for TV auto-refresh — flat array, minimal payload, no pagination.

**Query parameters:**

| Parameter | Type | Required | Default |
|---|---|---|---|
| `limit` | integer | no | 10 |

**Response 200:**

```json
[
  {
    "id":        "uuid",
    "title":     "string",
    "location":  "string",
    "startTime": "2026-05-01T14:00:00Z",
    "category": {
      "id":    "uuid",
      "name":  "string",
      "color": "#5DCAA5"
    }
  }
]
```

---

#### `GET /api/events/categories`

Returns all active event categories. Used to populate filter controls on the display and kiosk UI.

**Response 200:**

```json
[
  { "id": "uuid", "name": "Workshop", "color": "#5DCAA5" },
  { "id": "uuid", "name": "Lecture",  "color": "#7F77DD" }
]
```

---

### SubscriptionController — `/api/subscriptions`

All routes are public. No authentication required.

---

#### `POST /api/subscriptions`

Subscribe a phone number to SMS alerts for an event. Idempotent — re-subscribing a number that previously opted out reactivates it rather than creating a duplicate row.

**Request body:**

```json
{
  "eventId":     "uuid",
  "phoneNumber": "+12155550123"
}
```

**Validation:**
- `eventId` — required, must reference an existing non-cancelled event
- `phoneNumber` — required, must pass E.164 validation via libphonenumber

**Response 201:**

```json
{
  "subscriptionId": "uuid",
  "eventTitle":     "string",
  "phoneNumber":    "+12155550123",
  "alertOffsets":   [60, 15],
  "status":         "ACTIVE"
}
```

**Error responses:**

| Code | HTTP |
|---|---|
| `INVALID_PHONE_NUMBER` | 400 |
| `INVALID_REQUEST` | 400 |
| `EVENT_NOT_FOUND` | 404 |
| `ALREADY_SUBSCRIBED` | 409 |
| `EVENT_CANCELLED` | 422 |
| `EVENT_IN_PAST` | 422 |

---

#### `POST /api/subscriptions/opt-out`

Opt a phone number out of alerts for a specific event. Called when the user taps the unsubscribe link in a confirmation or reminder SMS.

**Request body:**

```json
{
  "subscriptionId": "uuid",
  "phoneNumber":    "+12155550123"
}
```

**Response 200:**

```json
{
  "status":  "OPTED_OUT",
  "message": "You have been unsubscribed from alerts for this event."
}
```

**Error responses:**

| Code | HTTP |
|---|---|
| `SUBSCRIPTION_NOT_FOUND` | 404 |
| `ALREADY_OPTED_OUT` | 409 |

---

#### `POST /api/subscriptions/verify`

Sends a one-time SMS verification code to confirm phone number ownership before finalizing a subscription. Include this endpoint if your organization requires confirmed opt-in consent.

**Request body:**

```json
{
  "phoneNumber": "+12155550123"
}
```

**Response 200:**

```json
{
  "verificationToken": "uuid",
  "expiresAt":         "2026-04-29T15:10:00Z"
}
```

**Error responses:**

| Code | HTTP |
|---|---|
| `INVALID_PHONE_NUMBER` | 400 |

---

### AdminController — `/api/admin`

All routes require a valid JWT with the `ADMIN` role. Secure the entire `/api/admin/**` path via `SecurityFilterChain` — do not apply security annotations method by method.

---

#### `POST /api/admin/events`

Create a new event.

**Request body:**

```json
{
  "title":               "string, required",
  "description":         "string, optional",
  "location":            "string, required",
  "categoryId":          "uuid, required",
  "startTime":           "2026-05-01T14:00:00Z",
  "endTime":             "2026-05-01T15:30:00Z",
  "alertOffsetMinutes":  60,
  "alertOffsetMinutes2": 15,
  "visible":             true
}
```

**Response 201:** Full event object (same shape as `GET /api/events/{id}`).

**Error responses:**

| Code | HTTP |
|---|---|
| `CATEGORY_NOT_FOUND` | 404 |
| `INVALID_REQUEST` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |

---

#### `PUT /api/admin/events/{id}`

Full replacement update of an event. If `startTime` changes and active subscribers exist, the backend queues a rescheduled SMS to all active subscribers automatically.

**Path parameter:** `id` — UUID of the event.

**Request body:** Same shape as `POST /api/admin/events`. All fields required.

**Response 200:** Updated full event object.

**Error responses:**

| Code | HTTP |
|---|---|
| `EVENT_NOT_FOUND` | 404 |
| `CATEGORY_NOT_FOUND` | 404 |
| `INVALID_REQUEST` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |

---

#### `PATCH /api/admin/events/{id}/status`

Update the status of an event. When status is set to `CANCELLED`, the backend automatically queues a cancellation SMS to all active subscribers.

**Path parameter:** `id` — UUID of the event.

**Request body:**

```json
{
  "status": "CANCELLED",
  "reason": "string, optional — included in the cancellation SMS"
}
```

**Valid status transitions:**

| From | To (allowed) |
|---|---|
| `UPCOMING` | `CANCELLED` |
| `UPCOMING` | `IN_PROGRESS` |
| `IN_PROGRESS` | `COMPLETED` |
| `IN_PROGRESS` | `CANCELLED` |

**Response 200:**

```json
{ "id": "uuid", "status": "CANCELLED" }
```

**Error responses:**

| Code | HTTP |
|---|---|
| `EVENT_NOT_FOUND` | 404 |
| `INVALID_REQUEST` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |

---

#### `PATCH /api/admin/events/{id}/visibility`

Show or hide an event on public displays without changing its status. Used to stage events before they go live.

**Path parameter:** `id` — UUID of the event.

**Request body:**

```json
{ "visible": true }
```

**Response 200:**

```json
{ "id": "uuid", "visible": true }
```

**Error responses:** `EVENT_NOT_FOUND` (404), `UNAUTHORIZED` (401), `FORBIDDEN` (403)

---

#### `POST /api/admin/events/{id}/broadcast`

Send a manual SMS to all active subscribers of an event. Used for ad-hoc announcements such as room changes. Returns 202 immediately — delivery is async.

**Path parameter:** `id` — UUID of the event.

**Request body:**

```json
{
  "message": "string, required, max 160 characters"
}
```

**Response 202:**

```json
{
  "queued":  42,
  "message": "Broadcast queued for 42 subscribers."
}
```

**Error responses:**

| Code | HTTP |
|---|---|
| `EVENT_NOT_FOUND` | 404 |
| `INVALID_REQUEST` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |

---

#### `GET /api/admin/events/{id}/subscribers`

Returns the subscriber list for an event. Phone numbers are partially masked in the response for privacy — full numbers are never exposed via the API.

**Path parameter:** `id` — UUID of the event.

**Response 200:**

```json
{
  "eventId": "uuid",
  "total":   42,
  "subscribers": [
    {
      "subscriptionId": "uuid",
      "phoneNumber":    "+1215555****",
      "status":         "ACTIVE",
      "subscribedAt":   "2026-04-29T10:00:00Z"
    }
  ]
}
```

**Error responses:** `EVENT_NOT_FOUND` (404), `UNAUTHORIZED` (401), `FORBIDDEN` (403)

---

#### `POST /api/admin/categories`

Create a new event category.

**Request body:**

```json
{
  "name":  "string, required",
  "color": "#hex, required"
}
```

**Response 201:**

```json
{ "id": "uuid", "name": "string", "color": "#5DCAA5", "active": true }
```

**Error responses:** `INVALID_REQUEST` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403)

---

#### `PATCH /api/admin/categories/{id}`

Update a category's name or color.

**Path parameter:** `id` — UUID of the category.

**Request body:**

```json
{
  "name":   "string, optional",
  "color":  "#hex, optional",
  "active": true
}
```

**Response 200:** Updated category object.

**Error responses:** `CATEGORY_NOT_FOUND` (404), `UNAUTHORIZED` (401), `FORBIDDEN` (403)

---

### Implementation notes

- `EventController` and `SubscriptionController` are fully public — configure `permitAll()` for `/api/events/**` and `/api/subscriptions/**` in `SecurityFilterChain`
- `AdminController` uses `hasRole('ADMIN')` — configure via `SecurityFilterChain`, not per-method annotations
- All controllers return `ResponseEntity<T>` and delegate entirely to the service layer — no repository access in controllers
- Use Spring's `Pageable` with `@PageableDefault(size=20, sort="startTime")` for the list endpoint
- The broadcast endpoint returns `202 Accepted` — hand off to `@Async` service method or a queue immediately, do not block on Twilio
- A single `@ControllerAdvice` class (`GlobalExceptionHandler`) maps all custom exceptions to the standard error envelope — do not handle exceptions inside individual controllers
- DTOs are separate per use case: `EventSummaryDto` (list), `EventDetailDto` (detail), `EventUpcomingDto` (TV feed) — do not reuse entity classes as response bodies

---


## Coding conventions
Here's the coding conventions section, ready to paste directly into `CLAUDE.md`:

---

## Coding conventions

### General

- Java 21. Use records for DTOs and value objects where the class is immutable and carries no behavior.
- All new classes go in a package structure that mirrors the feature slice, not the layer. Prefer `com.yourorg.eventdashboard.event`, `com.yourorg.eventdashboard.subscription`, and `com.yourorg.eventdashboard.notification` over a flat `controllers/`, `services/`, `repositories/` layout.
- No business logic in controllers. No repository access in controllers. No business logic in repositories.
- All public service methods must have a corresponding unit test. Controller tests use `@WebMvcTest`. Service tests use `@ExtendWith(MockitoExtension.class)`. Repository tests use `@DataJpaTest`.
- Never return a JPA entity directly from a controller. Always map to a DTO before returning.
- Never expose the `notification_log` or raw `subscription` tables via public endpoints. Admin endpoints that return subscriber data must mask phone numbers before serializing.

---

### Naming

- Classes: `UpperCamelCase`. Methods and variables: `lowerCamelCase`. Constants: `UPPER_SNAKE_CASE`. Database columns and indexes: `lower_snake_case`.
- Controller classes are named `{Feature}Controller` — e.g. `EventController`, `SubscriptionController`, `AdminController`.
- Service classes are named `{Feature}Service` — e.g. `EventService`, `SubscriptionService`.
- Repository interfaces are named `{Entity}Repository` — e.g. `EventRepository`, `SubscriptionRepository`.
- DTOs are named `{Entity}{UseCase}Dto` — e.g. `EventSummaryDto`, `EventDetailDto`, `EventUpcomingDto`. Never reuse a DTO across use cases that have different field sets.
- Custom exceptions are named `{Condition}Exception` — e.g. `EventNotFoundException`, `EventCancelledException`, `AlreadySubscribedException`.
- Database indexes are named `idx_{table}_{column(s)}` — e.g. `idx_event_start_time`, `idx_subscription_event_status`.

---

### Entities

- All entities extend `BaseEntity`, an abstract class carrying `id`, `createdAt`, and `updatedAt`. `@PrePersist` sets both timestamps. `@PreUpdate` sets `updatedAt` only.
- Primary keys are UUIDs generated via `@UuidGenerator`. Never use auto-increment integer IDs.
- All status fields use `@Enumerated(EnumType.STRING)`. Never use `EnumType.ORDINAL` — ordinal breaks if enum values are reordered.
- Apply `@Column(nullable = false)` to every non-nullable field. Do not rely on the database constraint alone — Hibernate should validate before hitting the DB.
- Relationships use lazy loading by default (`fetch = FetchType.LAZY`). Never use `FetchType.EAGER` — it causes unintended N+1 queries on list endpoints.
- Never use bidirectional relationships unless there is a specific, documented reason. Prefer navigating from parent to child only.

```java
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

---

### DTOs

- Use Java records for all DTOs.
- Map from entity to DTO inside the service layer, never inside the controller or the entity itself.
- Keep a dedicated mapper class per feature slice named `{Feature}Mapper` — e.g. `EventMapper`. Do not use MapStruct unless the team has agreed to it — manual mappers are easier for Claude Code to reason about consistently.
- DTOs never reference other DTOs by inheritance. Compose by nesting where needed (e.g. `CategoryDto` nested inside `EventSummaryDto`).

```java
public record EventSummaryDto(
    UUID id,
    String title,
    String description,
    String location,
    Instant startTime,
    Instant endTime,
    CategoryDto category,
    int subscriberCount,
    String status
) {}

public record CategoryDto(
    UUID id,
    String name,
    String color
) {}
```

---

### Controllers

- Annotate with `@RestController` and `@RequestMapping("/api/{feature}")`.
- Inject the service via constructor injection. Never use `@Autowired` on a field.
- Return `ResponseEntity<T>` from every handler method. Never return a raw object.
- Use `@PageableDefault(size = 20, sort = "startTime")` on paginated endpoints.
- Do not catch exceptions inside controllers. Let `GlobalExceptionHandler` handle them.

```java
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventSummaryDto>> listEvents(
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(eventService.listEvents(pageable, categoryId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }
}
```

---

### Services

- Annotate with `@Service`. Mark the class `final` to prevent accidental subclassing.
- Annotate read methods with `@Transactional(readOnly = true)`. Annotate write methods with `@Transactional`.
- Throw custom exceptions from the service layer — never return `null` or `Optional.empty()` to signal a missing resource.
- Never call another service from within a service. If shared logic is needed, extract it to a package-private helper or a shared utility class.

```java
@Service
@RequiredArgsConstructor
public final class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public Page<EventSummaryDto> listEvents(
            Pageable pageable, UUID categoryId, LocalDate from, LocalDate to) {
        return eventRepository
                .findAllVisible(pageable, categoryId, from, to)
                .map(eventMapper::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public EventDetailDto getEvent(UUID id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDetailDto)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}
```

---

### Exception handling

- All custom exceptions extend `RuntimeException`.
- A single `@ControllerAdvice` class named `GlobalExceptionHandler` handles all exceptions and maps them to the standard error envelope.
- Never handle exceptions inside individual controllers or services — throw and let `GlobalExceptionHandler` catch.

```java
public record ErrorResponse(int status, String code, String message) {}

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(EventNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse(404, "EVENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AlreadySubscribedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadySubscribed(AlreadySubscribedException ex) {
        return ResponseEntity.status(409)
                .body(new ErrorResponse(409, "ALREADY_SUBSCRIBED", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409)
                .body(new ErrorResponse(409, "CONFLICT", "A duplicate record was detected."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
                .body(new ErrorResponse(400, "INVALID_REQUEST", message));
    }
}
```

---

### Security

- Configure all security rules in a single `SecurityConfig` class annotated `@Configuration @EnableWebSecurity`.
- Public routes (`/api/events/**`, `/api/subscriptions/**`) use `permitAll()`.
- Admin routes (`/api/admin/**`) use `hasRole('ADMIN')`.
- Do not use method-level security annotations (`@PreAuthorize`, `@Secured`) — all rules live in `SecurityFilterChain`.
- JWT is stateless — configure `SessionCreationPolicy.STATELESS` and disable CSRF for API routes.
- Never log a JWT token, phone number, or any PII at any log level.

---

### Notifications and scheduling

- The `NotificationScheduler` class is annotated `@Component` and uses `@Scheduled(fixedDelay = 60000)` to poll every 60 seconds.
- Before sending any SMS, the scheduler checks `notification_log` for an existing row matching `(subscription_id, offset_minutes)`. If a row exists, skip — do not send again.
- Catch `DataIntegrityViolationException` when inserting into `notification_log` and treat it as "already sent" — this is the idempotency guard for concurrent scheduler runs.
- All SMS sends go through `SmsService`, which is an interface. The Twilio implementation is `TwilioSmsService`. Tests inject a `MockSmsService` stub.
- Broadcast sends (`POST /api/admin/events/{id}/broadcast`) are handled by an `@Async` method in `NotificationService`. The controller returns `202 Accepted` immediately without waiting for Twilio.
- Never hardcode Twilio credentials. Read `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_PHONE_NUMBER` from environment variables via `@Value`.

---

### Database migrations

- Use Flyway for all schema changes. Never modify an existing migration file — always add a new one.
- Migration files are named `V{version}__{description}.sql` — e.g. `V1__create_event_category.sql`, `V2__create_event.sql`.
- Each migration file creates one table. Do not combine multiple table creations in a single file.
- All `ALTER TABLE` statements go in their own migration file, never mixed with a `CREATE TABLE`.
- Include `CREATE INDEX` statements at the bottom of the same migration file as the table they index.

---

### Timestamps and time zones

- All timestamps are stored in UTC using `TIMESTAMP WITH TIME ZONE` in PostgreSQL.
- Set `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` in `application.properties`.
- Use `Instant` for all timestamp fields in Java entities and DTOs. Never use `java.util.Date` or `java.sql.Timestamp`.
- The frontend is responsible for converting UTC to the user's local time zone for display. The backend never localizes timestamps.

---

### Logging

- Use SLF4J with `@Slf4j` (Lombok). Never use `System.out.println`.
- Log at `INFO` for significant business events (subscription created, SMS sent, event cancelled).
- Log at `WARN` for recoverable failures (SMS delivery failed, retry queued).
- Log at `ERROR` for unrecoverable failures only (Twilio unreachable, database connection lost).
- Never log phone numbers, JWT tokens, or any other PII at any log level.

---

### React frontend

- Components are functional. No class components.
- All API calls go through a centralized `api.js` module — never call `fetch` directly from a component.
- The kiosk display (iPad and TV views) polls `/api/events/upcoming` on a 30-second interval using `setInterval` inside a `useEffect` with proper cleanup.
- The SMS signup form validates phone number format client-side before submitting, but always defers to the server's validation response for the final error message shown to the user.
- All error responses from the API are handled by reading the `code` field from the standard error envelope and mapping it to a user-friendly message. Never display raw server error messages to a kiosk user.
- Environment-specific API base URLs are set via `.env` files using `REACT_APP_API_BASE_URL`. Never hardcode a hostname.

---

### Notes for Claude
- `AdminController` needs a `GET /api/admin/events` endpoint that returns all events regardless of `visible` or `status`, with optional query parameters for `status` and `categoryId` filtering. This is distinct from the public `GET /api/events` which filters to visible upcoming events only. Add this endpoint to the API contract section.
The cancellation SMS and the reschedule SMS are triggered server-side inside `EventService` when status transitions to `CANCELLED` or when `startTime` changes on a PUT request. These are not separate endpoints — they are side effects of the existing admin update operations. `EventService` calls `SmsService` directly after persisting the change.
The broadcast endpoint returns `202 Accepted` and hands off to an `@Async` method in `NotificationService`. The `EventService.cancelEvent()` method is synchronous with respect to the database write but calls `notificationService.sendCancellationSms()` as an `@Async` call so the admin response is not blocked on Twilio.

- `NotificationScheduler` is a `@Component` class, not a `@Service`. It owns the scheduling loop and orchestrates sends but delegates all SMS delivery to `SmsService` and all database writes to `NotificationLogRepository`. It never accesses `EventRepository` or `SubscriptionRepository` directly — it calls `NotificationService` for those queries.
- `SmsService` is an interface with two implementations: `TwilioSmsService` (production) and `MockSmsService` (test). `MockSmsService` logs the message to SLF4J at `INFO` level and returns a fake `message_sid`. The active implementation is selected via a Spring `@Profile` — `twilio` for production, `mock` for test and local development.
- `NotificationService` is a `@Service` that contains all `@Async` methods for cancellation and reschedule batches. It is separate from `SubscriptionService`. Async methods return `CompletableFuture<Void>` and have their own exception handling — they never propagate exceptions to the caller.
- The Twilio webhook controller is a new class `WebhookController` at `/api/webhooks`. It is separate from `SubscriptionController` and `AdminController`. Twilio signature validation uses `com.twilio.security.RequestValidator` — add `twilio-java` to `pom.xml`.
- The `offset_minutes` convention for non-reminder log entries: `0` = confirmation SMS, `-1` = cancellation SMS, `-2` = reschedule SMS. Document this in a `NotificationOffsets` constants class rather than using magic numbers inline.
- Never log phone numbers at any log level. When logging notification events use `subscription_id` and `event_id only`.
- Add `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`, and `APP_BASE_URL` to `application.properties` as `${TWILIO_ACCOUNT_SID}` placeholders — actual values come from environment variables, never from committed config files.

- Build and implement stories in this order: Story 1 (scaffold) → Story 2 (migrations) → Story 5 (SMS service) → Story 3 (event slice) → Story 4 (subscription slice) → Story 6 (notification slice) → Story 7 (admin slice) → Story 8 (security) → Story 9 (webhooks) → Story 10 (cross-cutting). This order ensures each story has its dependencies in place before it is started.
- Each story is a self-contained Claude Code session. Begin each session by telling Claude Code: "Refer to CLAUDE.md for architecture, data model, API contract, and coding conventions. Implement the following story:" then paste the story. Do not paste multiple stories in one session.
- After completing Story 3, paste the generated `Event`, `EventCategory`, `EventRepository`, and `EventService` code into a `## Generated artifacts` section in `CLAUDE.md` so subsequent sessions know the exact class names, field names, and method signatures already in use.
- After completing Story 4, add `Subscription`, `SubscriptionRepository`, and `SubscriptionService` to the generated artifacts section.
- After completing Story 6, add `NotificationLog`, `NotificationLogRepository`, `NotificationScheduler`, and `NotificationService` to the generated artifacts section.
- After completing Story 8, add `SecurityConfig`, `JwtService`, and `JwtFilter` to the generated artifacts section.
- The `@EnableAsync` annotation goes on the main `@SpringBootApplication` class, not on `NotificationService`. Add this note to the scaffold story when prompting Claude Code for Story 1.
Run `./mvnw test` after each story is complete before starting the next. Do not accumulate failing tests across stories.

- Implement non-functional stories after all feature stories are complete and all feature tests are passing. Do not interleave non-functional work with feature development — it creates merge conflicts and makes debugging harder.
- Story 2 (security hardening) and Story 5 (logging) should be implemented together in the same Claude Code session — `PhoneNumberMaskingConverter` is referenced by both and should only be built once. Begin the session with both stories pasted in sequence.
- Story 4 (environment configuration) must be implemented before any deployment attempt — the `EnvironmentValidator` fast-fail behavior will surface missing variables immediately rather than silently defaulting to null values that cause cryptic runtime errors.
- The `CacheNames` constants class (Story 6), the `NotificationOffsets` constants class (already in `shared`), and any future constants follow the same pattern — a dedicated class in `shared` with `public static final String` fields. Never use string literals for cache names or offset values inline.
- `@EnableCaching` and `@EnableAsync` both go on the main `@SpringBootApplication` class. After Story 6 is implemented, verify the main application class has both annotations and add a note to the generated artifacts section of `CLAUDE.md`.