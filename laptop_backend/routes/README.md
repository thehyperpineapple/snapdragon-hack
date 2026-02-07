# Routes Documentation

## Auth (`auth.py`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/register` | Register user with `email`, `username`, `password` |
| GET | `/users/<user_id>` | Get user by ID |
| DELETE | `/users/<user_id>` | Delete user and all data |

## Health Profile (`user.py`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/<user_id>/health` | Create health profile (`weight`, `height`, `age` required) |
| GET | `/users/<user_id>/health` | Get health profile |
| PUT | `/users/<user_id>/health` | Update health profile |
| DELETE | `/users/<user_id>/health` | Remove health profile |

## Nutrition Profile (`user.py`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/<user_id>/nutrition` | Create nutrition preferences |
| GET | `/users/<user_id>/nutrition` | Get nutrition profile |
| PUT | `/users/<user_id>/nutrition` | Update nutrition profile |
| DELETE | `/users/<user_id>/nutrition` | Remove nutrition profile |

## Plans (`plan.py`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/<user_id>/plan` | Create plan (`plan_type`: diet/workout/combined) |
| GET | `/users/<user_id>/plan` | Get active plan |
| PUT | `/users/<user_id>/plan` | Update plan |
| DELETE | `/users/<user_id>/plan` | Delete plan |

## Tracking (`tracking.py`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/<user_id>/tracking/meals` | Log meal |
| POST | `/users/<user_id>/tracking/workout` | Log workout |
| GET | `/users/<user_id>/tracking/daily` | Get daily log (`?date=YYYY-MM-DD`) |
| GET | `/users/<user_id>/tracking/history` | Get tracking history |
| POST | `/users/<user_id>/tracking/water` | Log water intake |
| POST | `/users/<user_id>/tracking/wellness` | Log wellness metrics |

## Response Codes

- `200` - Success
- `201` - Created
- `400` - Bad request
- `404` - Not found
- `409` - Conflict (duplicate)
- `500` - Server error
