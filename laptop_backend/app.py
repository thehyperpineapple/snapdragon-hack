"""
Nutrition & Workout Application - Flask Backend

This is the main entry point for the Flask application.
It registers blueprints and sets up the application.
"""

import os
import logging
from dotenv import load_dotenv
from flask import Flask, jsonify
from flask_cors import CORS

# Load environment variables first
load_dotenv()

# Import extensions (initializes Firebase and logging)
import extensions  # noqa: F401

logger = logging.getLogger(__name__)


def create_app():
    """
    Application factory function.
    
    Creates and configures the Flask application with all blueprints registered.
    """
    app = Flask(__name__)
    
    # Enable CORS
    CORS(app)
    
    # Configuration
    app.config['JSON_SORT_KEYS'] = False
    app.config['JSONIFY_PRETTYPRINT_REGULAR'] = True
    
    # Register blueprints
    from routes import auth_bp, user_bp, plan_bp, tracking_bp
    
    app.register_blueprint(auth_bp)
    app.register_blueprint(user_bp)
    app.register_blueprint(plan_bp)
    app.register_blueprint(tracking_bp)
    
    # Health check endpoint
    @app.route('/health', methods=['GET'])
    def health_check():
        """Basic health check endpoint."""
        logger.debug("Health check endpoint accessed")
        return jsonify({
            'status': 'ok',
            'service': 'nutrition-workout-api'
        }), 200
    
    # Root endpoint
    @app.route('/', methods=['GET'])
    def root():
        """API documentation endpoint with all routes and sample outputs."""
        routes_docs = """
# API Routes Documentation

## Health Check
### app.health_check GET /health
```json
{
  "status": "ok",
  "service": "nutrition-workout-api"
}
```

## Authentication & User Management

### auth.register_user POST /users/register
```json
{
  "user_id": "abc123",
  "email": "user@example.com",
  "username": "johndoe",
  "message": "User registered successfully"
}
```

### auth.get_user_route GET /users/<user_id>
```json
{
  "user_id": "abc123",
  "email": "user@example.com",
  "username": "johndoe",
  "created_at": "2024-01-15T10:30:00Z"
}
```

### auth.delete_user_route DELETE /users/<user_id>
```json
{
  "message": "User deleted successfully"
}
```

## Health Profile Management

### user.create_health_profile POST /users/<user_id>/health
```json
{
  "message": "Health profile created",
  "profile": {
    "weight": 75.5,
    "height": 180,
    "age": 28,
    "gender": "male",
    "activity_level": "moderate",
    "fitness_goal": "weight_loss",
    "bmi": 23.3
  }
}
```

### user.get_health_profile GET /users/<user_id>/health
```json
{
  "profile": {
    "weight": 75.5,
    "height": 180,
    "age": 28,
    "gender": "male",
    "activity_level": "moderate",
    "fitness_goal": "weight_loss",
    "bmi": 23.3
  }
}
```

### user.update_health_profile PUT /users/<user_id>/health
```json
{
  "message": "Health profile updated",
  "updated_fields": ["weight", "bmi"]
}
```

### user.remove_health_profile DELETE /users/<user_id>/health
```json
{
  "message": "Health profile deleted"
}
```

## Nutrition Profile Management

### user.create_nutrition_profile POST /users/<user_id>/nutrition
```json
{
  "message": "Nutrition profile created",
  "nutrition": {
    "allergies": ["peanuts"],
    "diet_type": "vegetarian",
    "calorie_goal": 2000,
    "meals_per_day": 4
  }
}
```

### user.get_nutrition_profile GET /users/<user_id>/nutrition
```json
{
  "nutrition": {
    "allergies": ["peanuts"],
    "diet_type": "vegetarian",
    "calorie_goal": 2000,
    "protein_goal": 150,
    "carb_goal": 250,
    "fat_goal": 65,
    "meals_per_day": 4
  }
}
```

### user.update_nutrition_profile PUT /users/<user_id>/nutrition
```json
{
  "message": "Nutrition profile updated",
  "updated_fields": ["calorie_goal"]
}
```

### user.remove_nutrition_profile DELETE /users/<user_id>/nutrition
```json
{
  "message": "Nutrition profile deleted"
}
```

## AI-Powered Plan Management

### plan.create_user_plan_ai POST /users/<user_id>/plan
```json
{
  "message": "Plan created successfully",
  "plan_id": "plan_xyz789",
  "plan": {
    "plan_type": "combined",
    "ai_generated": true,
    "generation_method": "npu_llm",
    "diet": [
      {
        "weekName": "Week 1",
        "meals": {
          "breakfast": {"name": "Oatmeal with Berries", "calories": 350},
          "lunch": {"name": "Grilled Chicken Salad", "calories": 450}
        }
      }
    ],
    "workouts": [
      {
        "weekName": "Week 1",
        "exercises": [
          {"workoutId": "w1", "name": "Push-ups", "sets": 3, "reps": 12}
        ]
      }
    ]
  }
}
```

### plan.validate_user_plan POST /users/<user_id>/plan/validate
```json
{
  "validation": {
    "overall_score": 8.5,
    "recommendations": ["Increase protein intake", "Add rest days"],
    "warnings": [],
    "alignment_with_goals": "good"
  },
  "timestamp": "now"
}
```

### plan.adjust_user_plan PUT /users/<user_id>/plan/adjust
```json
{
  "message": "Plan adjusted successfully",
  "plan": {
    "workouts": [
      {
        "weekName": "Week 1",
        "exercises": [
          {"workoutId": "w1", "name": "Modified Push-ups", "sets": 2, "reps": 10}
        ]
      }
    ]
  }
}
```

### plan.adjust_workout_plan PUT /users/<user_id>/plan/workout/adjust
```json
{
  "message": "Workout plan adjusted for Week 1",
  "week": "Week 1",
  "adjusted_count": 3,
  "adjusted_exercises": [
    {"workoutId": "w4", "name": "Light Cardio", "sets": 1, "duration": "20min"}
  ]
}
```

### plan.adjust_nutrition_plan PUT /users/<user_id>/plan/nutrition/adjust
```json
{
  "message": "Nutrition plan adjusted for Week 1",
  "week": "Week 1",
  "calorie_adjustment": -500,
  "adjusted_meals": {
    "Wednesday": {"breakfast": {"name": "Egg Whites", "calories": 200}},
    "Thursday": {"lunch": {"name": "Salad", "calories": 350}}
  }
}
```

## Daily Tracking

### tracking.update_meals POST /users/<user_id>/tracking/meals
```json
{
  "message": "Meal logged successfully",
  "date": "2024-01-15",
  "meal_type": "breakfast",
  "total_calories": 300
}
```

### tracking.update_workout POST /users/<user_id>/tracking/workout
```json
{
  "message": "Workout logged successfully",
  "date": "2024-01-15",
  "completed": true,
  "duration_minutes": 45
}
```

### tracking.get_daily_log GET /users/<user_id>/tracking/daily?date=2024-01-15
```json
{
  "date": "2024-01-15",
  "daily_log": {
    "meals": {
      "breakfast": [{"name": "Oatmeal", "calories": 300}],
      "lunch": [{"name": "Salad", "calories": 400}]
    },
    "workout": {
      "completed": true,
      "exercises": [{"name": "Push-ups", "sets": 3, "reps": 12}],
      "duration_minutes": 45
    },
    "water_ml": 2000
  }
}
```

### tracking.get_tracking_history GET /users/<user_id>/tracking/history?limit=7
```json
{
  "daily_logs": [
    {
      "date": "2024-01-15",
      "meals": {},
      "workout": {"completed": true}
    }
  ],
  "total": 7
}
```

### tracking.update_water_intake POST /users/<user_id>/tracking/water
```json
{
  "water_intake_ml": 2250
}
```

### tracking.update_wellness POST /users/<user_id>/tracking/wellness
```json
{
  "wellness": {
    "sleep_hours": 7.5,
    "mood": "good",
    "energy_level": 4
  }
}
```
"""
        return jsonify({
            'name': 'Nutrition & Workout API',
            'version': '1.0.0',
            'documentation': routes_docs
        }), 200
    
    # Error handlers
    @app.errorhandler(400)
    def bad_request(error):
        logger.warning(f"Bad request: {str(error)}")
        return jsonify({'error': 'Bad request', 'message': str(error)}), 400

    @app.errorhandler(404)
    def not_found(error):
        logger.warning(f"Resource not found: {str(error)}")
        return jsonify({'error': 'Not found', 'message': 'The requested resource was not found'}), 404

    @app.errorhandler(500)
    def internal_error(error):
        logger.error(f"Internal server error: {str(error)}", exc_info=True)
        return jsonify({'error': 'Internal server error', 'message': 'An unexpected error occurred'}), 500
    
    return app


# Create the application instance
app = create_app()


if __name__ == '__main__':
    # Get configuration from environment
    host = os.getenv('FLASK_HOST', '0.0.0.0')
    port = int(os.getenv('FLASK_PORT', 5000))
    debug = os.getenv('FLASK_DEBUG', 'true').lower() == 'true'

    logger.info("="*80)
    logger.info("Starting Nutrition & Workout API")
    logger.info(f"Host: {host}, Port: {port}, Debug: {debug}")
    logger.info("="*80)

    app.run(host=host, port=port, debug=debug)
