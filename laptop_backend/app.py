"""
Nutrition & Workout Application - Flask Backend

This is the main entry point for the Flask application.
It registers blueprints and sets up the application.
"""

import os
import logging
from dotenv import load_dotenv
from flask import Flask, jsonify, Response
from flask_cors import CORS
import markdown2
import json

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

        # Route examples database with sample inputs/outputs
        route_examples = {
            'health_check': {
                'input': None,
                'output': {'status': 'ok', 'service': 'nutrition-workout-api'}
            },
            'register_user': {
                'input': {'email': 'user@example.com', 'username': 'johndoe', 'password': 'securepass123'},
                'output': {'user_id': 'abc123', 'email': 'user@example.com', 'username': 'johndoe', 'message': 'User registered successfully'}
            },
            'get_user_route': {
                'input': None,
                'output': {'user_id': 'abc123', 'email': 'user@example.com', 'username': 'johndoe', 'created_at': '2024-01-15T10:30:00Z'}
            },
            'delete_user_route': {
                'input': None,
                'output': {'message': 'User deleted successfully'}
            },
            'create_health_profile': {
                'input': {'weight': 75.5, 'height': 180, 'age': 28, 'gender': 'male', 'activity_level': 'moderate', 'fitness_goal': 'weight_loss'},
                'output': {'message': 'Health profile created', 'profile': {'weight': 75.5, 'height': 180, 'age': 28, 'gender': 'male', 'activity_level': 'moderate', 'fitness_goal': 'weight_loss', 'bmi': 23.3}}
            },
            'get_health_profile': {
                'input': None,
                'output': {'profile': {'weight': 75.5, 'height': 180, 'age': 28, 'gender': 'male', 'activity_level': 'moderate', 'fitness_goal': 'weight_loss', 'bmi': 23.3}}
            },
            'update_health_profile': {
                'input': {'weight': 77.0},
                'output': {'message': 'Health profile updated', 'updated_fields': ['weight', 'bmi']}
            },
            'remove_health_profile': {
                'input': None,
                'output': {'message': 'Health profile deleted'}
            },
            'create_nutrition_profile': {
                'input': {'allergies': ['peanuts'], 'diet_type': 'vegetarian', 'calorie_goal': 2000, 'meals_per_day': 4},
                'output': {'message': 'Nutrition profile created', 'nutrition': {'allergies': ['peanuts'], 'diet_type': 'vegetarian', 'calorie_goal': 2000, 'meals_per_day': 4}}
            },
            'get_nutrition_profile': {
                'input': None,
                'output': {'nutrition': {'allergies': ['peanuts'], 'diet_type': 'vegetarian', 'calorie_goal': 2000, 'protein_goal': 150, 'carb_goal': 250, 'fat_goal': 65, 'meals_per_day': 4}}
            },
            'update_nutrition_profile': {
                'input': {'calorie_goal': 2200},
                'output': {'message': 'Nutrition profile updated', 'updated_fields': ['calorie_goal']}
            },
            'remove_nutrition_profile': {
                'input': None,
                'output': {'message': 'Nutrition profile deleted'}
            },
            'create_user_plan_ai': {
                'input': {'plan_type': 'combined', 'duration_weeks': 4, 'intensity': 'moderate', 'specific_goals': ['weight_loss'], 'use_ai': True},
                'output': {'message': 'Plan created successfully', 'plan_id': 'plan_xyz789', 'plan': {'plan_type': 'combined', 'ai_generated': True, 'generation_method': 'npu_llm', 'diet': [{'weekName': 'Week 1', 'meals': {'breakfast': {'name': 'Oatmeal with Berries', 'calories': 350}}}], 'workouts': [{'weekName': 'Week 1', 'exercises': [{'workoutId': 'w1', 'name': 'Push-ups', 'sets': 3, 'reps': 12}]}]}}
            },
            'get_user_plan': {
                'input': None,
                'output': {'plan': {'plan_type': 'combined', 'ai_generated': True, 'diet': [{'weekName': 'Week 1', 'meals': {'breakfast': {'name': 'Oatmeal', 'calories': 350}}}], 'workouts': [{'weekName': 'Week 1', 'exercises': [{'workoutId': 'w1', 'name': 'Push-ups', 'sets': 3, 'reps': 12}]}]}}
            },
            'delete_user_plan': {
                'input': None,
                'output': {'message': 'Plan deleted successfully'}
            },
            'validate_user_plan': {
                'input': None,
                'output': {'validation': {'overall_score': 8.5, 'recommendations': ['Increase protein intake', 'Add rest days'], 'warnings': [], 'alignment_with_goals': 'good'}, 'timestamp': 'now'}
            },
            'adjust_user_plan': {
                'input': {'adjustment_request': 'Make workouts less intense', 'user_feedback': 'The current plan is too hard'},
                'output': {'message': 'Plan adjusted successfully', 'plan': {'workouts': [{'weekName': 'Week 1', 'exercises': [{'workoutId': 'w1', 'name': 'Modified Push-ups', 'sets': 2, 'reps': 10}]}]}}
            },
            'adjust_workout_plan': {
                'input': {'week_name': 'Week 1', 'skipped_workouts': ['w1', 'w2'], 'reason': 'Too intense'},
                'output': {'message': 'Workout plan adjusted for Week 1', 'week': 'Week 1', 'adjusted_count': 3, 'adjusted_exercises': [{'workoutId': 'w4', 'name': 'Light Cardio', 'sets': 1, 'duration': '20min'}]}
            },
            'adjust_nutrition_plan': {
                'input': {'week_name': 'Week 1', 'extra_calories': 500, 'day_of_week': 2, 'notes': 'Had extra snacks'},
                'output': {'message': 'Nutrition plan adjusted for Week 1', 'week': 'Week 1', 'calorie_adjustment': -500, 'adjusted_meals': {'Wednesday': {'breakfast': {'name': 'Egg Whites', 'calories': 200}}}}
            },
            'update_meals': {
                'input': {'date': '2024-01-15', 'meal_type': 'breakfast', 'items': [{'name': 'Oatmeal', 'calories': 300}]},
                'output': {'message': 'Meal logged successfully', 'date': '2024-01-15', 'meal_type': 'breakfast', 'total_calories': 300}
            },
            'update_workout': {
                'input': {'date': '2024-01-15', 'completed': True, 'exercises': [{'name': 'Push-ups', 'sets': 3, 'reps': 12}], 'duration_minutes': 45},
                'output': {'message': 'Workout logged successfully', 'date': '2024-01-15', 'completed': True, 'duration_minutes': 45}
            },
            'get_daily_log': {
                'input': None,
                'output': {'date': '2024-01-15', 'daily_log': {'meals': {'breakfast': [{'name': 'Oatmeal', 'calories': 300}]}, 'workout': {'completed': True, 'exercises': [{'name': 'Push-ups', 'sets': 3, 'reps': 12}], 'duration_minutes': 45}, 'water_ml': 2000}}
            },
            'get_tracking_history': {
                'input': None,
                'output': {'daily_logs': [{'date': '2024-01-15', 'meals': {}, 'workout': {'completed': True}}], 'total': 7}
            },
            'update_water_intake': {
                'input': {'amount_ml': 250},
                'output': {'water_intake_ml': 2250}
            },
            'update_wellness': {
                'input': {'sleep_hours': 7.5, 'mood': 'good', 'energy_level': 4},
                'output': {'wellness': {'sleep_hours': 7.5, 'mood': 'good', 'energy_level': 4}}
            }
        }

        # Build markdown documentation
        markdown_content = "# API Routes Documentation\n\n"

        # Collect all routes from the app
        routes = []
        for rule in app.url_map.iter_rules():
            if rule.endpoint not in ['static']:  # Skip static files
                routes.append({
                    'endpoint': rule.endpoint,
                    'methods': sorted([m for m in rule.methods if m not in ['HEAD', 'OPTIONS']]),
                    'path': str(rule)
                })

        # Sort routes by path
        routes.sort(key=lambda x: x['path'])

        # Generate markdown for each route
        for route in routes:
            endpoint_name = route['endpoint']
            methods_str = ', '.join(route['methods'])
            path = route['path']

            # Get sample data
            examples = route_examples.get(endpoint_name.split('.')[-1], {
                'input': None,
                'output': {'message': 'No example available'}
            })

            # Add route header
            markdown_content += f"## {endpoint_name} {methods_str} {path}\n\n"

            # Add sample input
            if examples['input']:
                markdown_content += "### Sample Input\n```json\n"
                markdown_content += json.dumps(examples['input'], indent=2)
                markdown_content += "\n```\n\n"
            else:
                markdown_content += "### Sample Input\n*No request body required*\n\n"

            # Add sample output
            markdown_content += "### Sample Output\n```json\n"
            markdown_content += json.dumps(examples['output'], indent=2)
            markdown_content += "\n```\n\n"
            markdown_content += "---\n\n"

        # Convert markdown to HTML
        html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>API Documentation - Nutrition & Workout API</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            line-height: 1.6;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        h1 {{
            color: #333;
            border-bottom: 3px solid #007bff;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #007bff;
            margin-top: 30px;
            background-color: #e7f3ff;
            padding: 10px;
            border-radius: 5px;
        }}
        h3 {{
            color: #555;
            margin-top: 15px;
        }}
        pre {{
            background-color: #2d2d2d;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }}
        code {{
            font-family: 'Courier New', Courier, monospace;
        }}
        hr {{
            border: none;
            border-top: 1px solid #ddd;
            margin: 30px 0;
        }}
        .header {{
            background-color: #007bff;
            color: white;
            padding: 20px;
            border-radius: 5px;
            margin-bottom: 30px;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1 style="color: white; border: none; margin: 0;">Nutrition & Workout API</h1>
        <p style="margin: 5px 0 0 0;">Version 1.0.0 - AI-Powered Health & Fitness Platform</p>
    </div>
    {markdown2.markdown(markdown_content, extras=['fenced-code-blocks'])}
</body>
</html>
"""

        return Response(html_content, mimetype='text/html')
    
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
