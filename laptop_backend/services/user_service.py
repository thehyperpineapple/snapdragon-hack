"""
User service - handles user registration and management.
"""

from extensions import db


def register_user_entry(email, username, password):
    """Create a new user document with basic credentials."""
    try:
        users_ref = db.collection("users")

        # Check for existing user with the same email
        existing = users_ref.where("email", "==", email).limit(1).get()
        if len(list(existing)) > 0:
            return {"error": "A user with this email already exists"}, 409

        # Check for existing username
        existing_username = users_ref.where("username", "==", username).limit(1).get()
        if len(list(existing_username)) > 0:
            return {"error": "Username already taken"}, 409

        doc_ref = users_ref.document()
        doc_ref.set({
            "email": email,
            "username": username,
            "password": password,  # TODO: Hash in production
        })
        return {"message": "User registered successfully", "userId": doc_ref.id}, 201
    except Exception as e:
        return {"error": str(e)}, 500


def get_user(user_id):
    """Retrieve user data by ID."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
        
        user_data = doc.to_dict()
        user_data.pop("password", None)  # Remove password from response
        user_data["userId"] = user_id
        return {"user": user_data}, 200
    except Exception as e:
        return {"error": str(e)}, 500


def delete_user(user_id):
    """Delete a user and all associated data."""
    try:
        doc_ref = db.collection("users").document(user_id)
        doc = doc_ref.get()
        if not doc.exists:
            return {"error": "User not found"}, 404
        
        doc_ref.delete()
        return {"message": "User deleted successfully"}, 200
    except Exception as e:
        return {"error": str(e)}, 500
