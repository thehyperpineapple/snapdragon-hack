# Postman tests for AI endpoints

## Import the collection

1. Open Postman.
2. **Import** → **Upload Files** → select `AI_Endpoints_Collection.postman_collection.json`.
3. Set collection variables:
   - **base_url**: `http://localhost:5000` (or your server URL).
   - **user_id**: A valid user ID (e.g. from **Register user** response).

## Recommended test order

1. **Prerequisites**
   - **Register user** → copy `user_id` from the response and set it in the collection variable.
   - **Create health profile** (required for health AI and plan AI).
   - **Create nutrition profile** (required for nutrition AI and plan AI).

2. **Health AI**
   - **Update health profile with AI insights** (PUT with `generate_insights: true`).
   - **Analyze health metrics (AI)** (GET).

3. **Nutrition AI**
   - **Update nutrition profile with AI recommendations** (PUT with `generate_recommendations: true`).
   - **Analyze nutrition profile (AI)** (GET).
   - **Get meal suggestions (AI)** (POST with `meal_type` and `remaining_macros`).

4. **Plan AI**
   - **Create plan (AI)** (POST with `use_ai: true`) — can take several seconds.
   - **Get plan** (GET).
   - **Validate plan (AI)** (POST).
   - **Adjust plan (AI)** (PUT with `adjustment_request`).
   - **Adjust workout plan (AI)** (PUT with `week_name`, `skipped_workouts`).
   - **Adjust nutrition plan (AI)** (PUT with `week_name`, `extra_calories`, `day_of_week`).
   - **Delete plan** (DELETE) — optional cleanup.

## Endpoints summary

| Method | Path | Purpose |
|--------|------|--------|
| PUT | `/users/:user_id/health` | Update health + optional AI insights |
| GET | `/users/:user_id/health/analyze` | AI health analysis |
| PUT | `/users/:user_id/nutrition` | Update nutrition + optional AI recommendations |
| GET | `/users/:user_id/nutrition/analyze` | AI nutrition analysis |
| POST | `/users/:user_id/nutrition/meal-suggestions` | AI meal suggestions |
| POST | `/users/:user_id/plan` | Create plan (AI or mock) |
| GET | `/users/:user_id/plan` | Get plan |
| DELETE | `/users/:user_id/plan` | Delete plan |
| POST | `/users/:user_id/plan/validate` | AI plan validation |
| PUT | `/users/:user_id/plan/adjust` | AI plan adjustment |
| PUT | `/users/:user_id/plan/workout/adjust` | AI workout week adjustment |
| PUT | `/users/:user_id/plan/nutrition/adjust` | AI nutrition week adjustment |

## Notes

- Health and nutrition profiles must exist before calling analyze or meal-suggestions; otherwise you get 404.
- A plan must exist before validate, adjust, workout/adjust, or nutrition/adjust.
- AI calls may take a few seconds; ensure `GEMINI_API_KEY` (or your AI config) is set in `.env` for plan/health/nutrition AI.
