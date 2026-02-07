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
        """API documentation endpoint."""
        return jsonify({
            'name': 'Nutrition & Workout API',
            'version': '1.0.0',
            'endpoints': {
                'auth': {
                    'POST /users/register': 'Register a new user',
                    'GET /users/<user_id>': 'Get user information',
                    'DELETE /users/<user_id>': 'Delete user account'
                },
                'health_profile': {
                    'POST /users/<user_id>/health': 'Create health profile',
                    'GET /users/<user_id>/health': 'Get health profile',
                    'PUT /users/<user_id>/health': 'Update health profile',
                    'DELETE /users/<user_id>/health': 'Delete health profile'
                },
                'nutrition_profile': {
                    'POST /users/<user_id>/nutrition': 'Create nutrition profile',
                    'GET /users/<user_id>/nutrition': 'Get nutrition profile',
                    'PUT /users/<user_id>/nutrition': 'Update nutrition profile',
                    'DELETE /users/<user_id>/nutrition': 'Delete nutrition profile'
                },
                'plans': {
                    'POST /users/<user_id>/plan': 'Create diet/workout plan',
                    'GET /users/<user_id>/plan': 'Get active plan',
                    'PUT /users/<user_id>/plan': 'Update plan',
                    'DELETE /users/<user_id>/plan': 'Delete plan'
                },
                'tracking': {
                    'POST /users/<user_id>/tracking/meals': 'Log meal',
                    'POST /users/<user_id>/tracking/workout': 'Log workout',
                    'GET /users/<user_id>/tracking/daily': 'Get daily log',
                    'GET /users/<user_id>/tracking/history': 'Get tracking history',
                    'POST /users/<user_id>/tracking/water': 'Log water intake',
                    'POST /users/<user_id>/tracking/wellness': 'Log wellness metrics'
                }
            }
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
