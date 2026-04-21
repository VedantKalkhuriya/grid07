# Grid07 Backend Assignment

Spring Boot microservice with Redis-backed guardrails and a notification engine.

## Tech Stack

- Java 17
- Spring Boot 3.2
- PostgreSQL 15
- Redis 7
- Docker + Docker Compose

---

## How to Run

### Step 1 — Start the databases

```bash
docker-compose up -d
```

This spins up PostgreSQL on port 5432 and Redis on port 6379.

### Step 2 — Run the app

```bash
./mvnw spring-boot:run
```

Or if you don't have mvnw:

```bash
mvn spring-boot:run
```

App starts on **http://localhost:8080**

### Step 3 — Import Postman collection

Import `Grid07_Postman_Collection.json` into Postman and run the requests in order (Setup → Phase 1 → Phase 2).

---

## Project Structure

```
src/main/java/com/grid07/
├── Grid07Application.java        # entry point
├── entity/
│   ├── User.java
│   ├── Bot.java
│   ├── Post.java
│   └── Comment.java
├── repository/                   # JPA repos (Spring Data)
├── service/
│   ├── PostService.java          # core business logic
│   ├── GuardrailService.java     # Redis atomic locks
│   ├── ViralityService.java      # Redis score updates
│   └── NotificationService.java  # throttle + queue logic
├── scheduler/
│   └── NotificationScheduler.java  # CRON sweeper
├── controller/
│   ├── PostController.java
│   ├── UserController.java
│   └── Dtos.java
└── config/
    ├── RedisConfig.java
    └── GlobalExceptionHandler.java
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/users | Create a user |
| GET | /api/users | List all users |
| POST | /api/bots | Create a bot |
| GET | /api/bots | List all bots |
| POST | /api/posts | Create a post |
| GET | /api/posts | List all posts |
| POST | /api/posts/{id}/comments | Add a comment |
| POST | /api/posts/{id}/like | Like a post |
| GET | /api/posts/{id}/virality | Check virality + bot count |

---

## Redis Keys Used

| Key Pattern | Type | Purpose |
|---|---|---|
| `post:{id}:virality_score` | String (int) | Running virality score |
| `post:{id}:bot_count` | String (int) | Total bot replies on a post |
| `cooldown:bot_{id}:human_{id}` | String (TTL 10min) | Bot-human cooldown lock |
| `notif:cooldown:user_{id}` | String (TTL 15min) | Notification cooldown per user |
| `user:{id}:pending_notifs` | List | Queued notification messages |

---

## How I Guaranteed Thread Safety (Phase 2)

This was the trickiest part. The problem: if 200 bots all hit the same post at the same millisecond, a naive read-then-write approach would fail. You could read `count = 99` on 50 different threads simultaneously and let all 50 through — ending up with 149 bot replies instead of 100.

### The fix: Redis INCR

Redis `INCR` is a single atomic command. It increments the counter AND returns the new value in one operation — no other command can run between those two steps.

So the flow is:

```
newCount = INCR post:{id}:bot_count   ← atomic, returns new value
if newCount > 100:
    DECR post:{id}:bot_count          ← roll back since we went over
    return 429
else:
    proceed with DB write
```

Even with 200 concurrent threads, only one of them will see `newCount = 101` and get rejected — the others will have already gotten their slots at counts 1–100. The 101st through 200th all see counts > 100 and get rejected.

### Cooldown lock: SET NX

For the cooldown, I used `setIfAbsent` which maps to Redis `SET NX EX` under the hood. This is also atomic — it sets the key only if it doesn't exist and assigns a TTL in the same command. So two bots can't both "check" the cooldown simultaneously and both see it as unset.

### Why not just use synchronized in Java?

Because the spec says the app must be stateless — no in-memory locks. If you scale to two instances of this service, a Java `synchronized` block only locks within one JVM. Redis locks work across all instances since they share the same Redis.

---

## Notification Engine (Phase 3)

- When a bot interacts with a user's post:
  - If the user has **not** been notified in the last 15 minutes → log notification immediately, set 15-min Redis TTL key
  - If the user **has** been notified recently → push message to `user:{id}:pending_notifs` Redis List

- A `@Scheduled` CRON job runs every 5 minutes:
  - Scans for all users with pending messages
  - Pops all messages for each user
  - Logs a single summarized message: `"Bot X and N others interacted with your posts"`

---

## Testing the Race Condition

To simulate 200 concurrent bots, you can use this curl one-liner:

```bash
for i in $(seq 1 200); do
  curl -s -X POST http://localhost:8080/api/posts/1/comments \
    -H "Content-Type: application/json" \
    -d "{\"authorBotId\": $i, \"content\": \"bot $i reply\", \"depthLevel\": 0}" &
done
wait
```

After this, check Redis:
```bash
docker exec grid07_redis redis-cli GET post:1:bot_count
```

Should show exactly `100`. Check the DB — there should be exactly 100 bot comments on that post.

---

## Notes

- JPA `ddl-auto=update` creates tables automatically on startup — no SQL migration scripts needed for local dev
- All state (counters, cooldowns, queues) is in Redis — the Spring Boot app itself is completely stateless
- PostgreSQL is the source of truth for actual content; Redis is just the gatekeeper
