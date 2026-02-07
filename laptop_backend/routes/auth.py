"""
Authentication & User Management Blueprint.
"""

from flask import Blueprint, jsonify, request
from services.user_service import register_user_entry, get_user, delete_user, login_user_entry

auth_bp = Blueprint('auth', __name__, url_prefix='/users')


@auth_bp.route('/register', methods=['POST'])
def register_user():
    """
    Register a new user.
    
    Request Body:
        {
            "email": "user@example.com",
            "username": "johndoe",
            "password": "securepassword123"
        }
    """
    data = request.get_json(silent=True) or {}
    
    required_fields = ['email', 'username', 'password']
    missing = [f for f in required_fields if not data.get(f)]
    
    if missing:
        return jsonify({'error': 'Missing required fields', 'missing_fields': missing}), 400
    
    result, status = register_user_entry(
        data['email'].strip().lower(),
        data['username'].strip(),
        data['password']
    )
    return jsonify(result), status


@auth_bp.route('/<user_id>', methods=['GET'])
def get_user_route(user_id):
    """Get user by ID."""
    result, status = get_user(user_id)
    return jsonify(result), status


@auth_bp.route('/<user_id>', methods=['DELETE'])
def delete_user_route(user_id):
    """Delete user account."""
    result, status = delete_user(user_id)
    return jsonify(result), status

@auth_bp.route('/login', methods=['POST'])
def login_user():
    """
    Login user with email and password.
    
    Request Body:
        {
            "email": "user@example.com",
            "password": "securepassword123"
        }
    """
    data = request.get_json(silent=True) or {}
    
    if not data.get('email') or not data.get('password'):
        return jsonify({'error': 'Email and password are required'}), 400
    
    result, status = login_user_entry(
        data['email'].strip().lower(),
        data['password']
    )
    return jsonify(result), status


@auth_bp.route('/logout', methods=['POST'])
def logout_user():
    """
    Logout user.
    
    Request Body:
        {
            "userId": "user123"
        }
    
    Note: In a production app, this would invalidate tokens/sessions.
    """
    data = request.get_json(silent=True) or {}
    
    user_id = data.get('userId')
    if not user_id:
        return jsonify({'error': 'userId is required'}), 400
    
    # In production, you would invalidate JWT token or clear session here
    return jsonify({'message': 'User logged out successfully', 'userId': user_id}), 200
