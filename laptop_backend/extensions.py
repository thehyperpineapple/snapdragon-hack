"""
Firebase and logging extensions initialization.
"""

import os
import logging
import logging.handlers
from datetime import datetime
from pathlib import Path

# Create logs directory
LOGS_DIR = Path("logs")
LOGS_DIR.mkdir(exist_ok=True)

# Configure logging
def setup_logging():
    """
    Configure comprehensive logging with console and file handlers.
    Logs include timestamps, level, module name, and detailed messages.
    """
    # Create logger
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    # Remove existing handlers
    for handler in logger.handlers[:]:
        logger.removeHandler(handler)

    # Log format with detailed information
    log_format = logging.Formatter(
        '[%(asctime)s] %(levelname)-8s [%(name)s:%(funcName)s:%(lineno)d] %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    # Console handler (INFO level)
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(log_format)
    logger.addHandler(console_handler)

    # File handler for all logs (DEBUG level)
    file_handler = logging.handlers.RotatingFileHandler(
        LOGS_DIR / 'backend.log',
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(log_format)
    logger.addHandler(file_handler)

    # Separate AI inference log file
    ai_handler = logging.handlers.RotatingFileHandler(
        LOGS_DIR / 'ai_inference.log',
        maxBytes=10*1024*1024,
        backupCount=5
    )
    ai_handler.setLevel(logging.DEBUG)
    ai_handler.setFormatter(log_format)

    # Separate database operations log file
    db_handler = logging.handlers.RotatingFileHandler(
        LOGS_DIR / 'database.log',
        maxBytes=10*1024*1024,
        backupCount=5
    )
    db_handler.setLevel(logging.DEBUG)
    db_handler.setFormatter(log_format)

    return logger, ai_handler, db_handler


# Initialize logging
logger, ai_handler, db_handler = setup_logging()

# Add AI-specific logger
ai_logger = logging.getLogger('ai')
ai_logger.addHandler(ai_handler)

# Add database-specific logger
db_logger = logging.getLogger('database')
db_logger.addHandler(db_handler)

logger.info("="*80)
logger.info("Backend Application Started")
logger.info(f"Logging initialized: {datetime.now().isoformat()}")
logger.info("="*80)

# Firebase initialization
try:
    import firebase_admin
    from firebase_admin import credentials, firestore

    # Initialize Firebase
    if not firebase_admin.get_app():
        creds_path = os.getenv('FIREBASE_CREDENTIALS_PATH', './serviceAccountKey.json')
        if os.path.exists(creds_path):
            cred = credentials.Certificate(creds_path)
            firebase_admin.initialize_app(cred)
            logger.info(f"Firebase initialized with credentials from: {creds_path}")
        else:
            firebase_admin.initialize_app()
            logger.info("Firebase initialized with default credentials")

    db = firestore.client()
    logger.info("Firestore client initialized successfully")

except ImportError:
    logger.warning("Firebase admin SDK not installed")
    db = None
except Exception as e:
    logger.error(f"Failed to initialize Firebase: {e}", exc_info=True)
    db = None
