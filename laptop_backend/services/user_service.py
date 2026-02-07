"""
User service - handles user registration and management.
"""

import logging
from extensions import db

logger = logging.getLogger('database')


def register_user_entry(email, username, password):
    """Create a new user document with basic credentials."""
    logger.info(f"DB_WRITE: Attempting to register user with email={email}, username={username}")
    try:
        users_ref = db.collection("users")

        # Check for existing user with the same email
        logger.debug(f"DB_READ: Checking for existing user with email={email}")
        existing = users_ref.where("email", "==", email).limit(1).get()
        if len(list(existing)) > 0:
            logger.warning(f"DB_READ: User registration failed - email already exists: {email}")
            return {"error": "A user with this email already exists"}, 409

        # Check for existing username
        logger.debug(f"DB_READ: Checking for existing username={username}")
        existing_username = users_ref.where("username", "==", username).limit(1).get()
        if len(list(existing_username)) > 0:
            logger.warning(f"DB_READ: User registration failed - username already taken: {username}")
            return {"error": "Username already taken"}, 409

        doc_ref = users_ref.document()
        doc_ref.set({
            "email": email,
            "username": username,
            "password": password,  # TODO: Hash in production
        })
        logger.info(f"DB_WRITE: User registered successfully with userId={doc_ref.id}, email={email}")
        return {"message": "User registered successfully", "userId": doc_ref.id}, 201
    except Exception as e:
        logger.error(f"DB_WRITE: User registration failed with error: {str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def get_user(user_id):
    """Retrieve user data by ID."""
    logger.info(f"DB_READ: Fetching user with userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_READ: User not found - userId={user_id}")
            return {"error": "User not found"}, 404

        user_data = doc.to_dict()
        user_data.pop("password", None)  # Remove password from response
        user_data["userId"] = user_id
        logger.debug(f"DB_READ: Successfully retrieved user data for userId={user_id}")
        return {"user": user_data}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to retrieve user - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500


def delete_user(user_id):
    """Delete a user and all associated data."""
    logger.info(f"DB_WRITE: Attempting to delete user - userId={user_id}")
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.warning(f"DB_WRITE: User not found for deletion - userId={user_id}")
            return {"error": "User not found"}, 404

        doc_ref.delete()
        logger.info(f"DB_WRITE: User deleted successfully - userId={user_id}")
        return {"message": "User deleted successfully"}, 200
    except Exception as e:
        logger.error(f"DB_WRITE: Failed to delete user - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500

def login_user_entry(email, password):
    """Login user with email and password."""
    logger.info(f"DB_READ: Attempting to login user with email={email}")
    try:
        users_ref = db.collection("users")
        
        # Find user by email
        docs = users_ref.where("email", "==", email).limit(1).get()
        docs_list = list(docs)
        
        if len(docs_list) == 0:
            logger.warning(f"DB_READ: User not found for login - email={email}")
            return {"error": "Invalid email or password"}, 401
        
        doc = docs_list[0]
        user_data = doc.to_dict()
        
        if user_data.get('password') != password:
            logger.warning(f"DB_READ: Incorrect password for email={email}")
            return {"error": "Invalid email or password"}, 401
        
        logger.info(f"DB_READ: User logged in successfully - userId={doc.id}, email={email}")
        return {
            "message": "User logged in successfully",
            "userId": doc.id,
            "username": user_data.get('username'),
            "email": email
        }, 200
        
    except Exception as e:
        logger.error(f"DB_READ: Failed to login user - email={email}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500