# Logging Implementation Summary

This document summarizes all the logging that has been added to the backend application.

## Files Created/Modified

### New Files
1. **extensions.py** - Firebase initialization and comprehensive logging setup
2. **LOGGING_GUIDE.md** - Complete guide to all logging in the application
3. **LOGGING_IMPLEMENTATION.md** - This file

### Modified Files

#### Core Application
- **app.py** - Added logging to Flask application initialization and error handlers

#### Services (Database Operations)
- **services/user_service.py** - Logging for user registration, retrieval, and deletion
- **services/health_service.py** - Logging for health profile CRUD operations
- **services/nutrition_service.py** - Logging for nutrition profile CRUD operations
- **services/plan_service.py** - Logging for plan creation, updates, and deletion
- **services/tracking_service.py** - Logging for meal and workout tracking

#### Routes (API Endpoints & AI Usage)
- **routes/plan_ai.py** - Comprehensive AI inference and usage logging
- **routes/user_ai.py** - Health and nutrition AI analysis logging

#### AI Components
- **ai/inference/gemini_engine.py** - Enhanced inference logging with performance metrics
- **ai/inference/cache_manager.py** - Detailed cache hit/miss tracking

## Logging Categories

### 1. AI Inference Logging (`AI_INFERENCE:`)
Tracks all LLM inference operations:
- Start of inference with parameters
- Cache hits/misses
- Token generation progress
- Inference completion with elapsed time
- Errors during inference
- Simulation mode operations

**Files affected:**
- routes/plan_ai.py
- routes/user_ai.py
- ai/inference/gemini_engine.py

### 2. AI Usage Tracking (`AI_USAGE:` and `AI_NON_USAGE:`)
Tracks when AI is invoked vs. when fallbacks are used:
- AI plan generation initiation
- Fallback to mock data
- AI insights and recommendations requests
- Meal suggestion requests

**Files affected:**
- routes/plan_ai.py
- routes/user_ai.py

### 3. Database Operations (`DB_READ:` and `DB_WRITE:`)
Logs all Firestore database operations:
- User registration, retrieval, deletion
- Health profile CRUD operations
- Nutrition profile CRUD operations
- Plan creation, updates, deletion
- Meal and workout tracking

**Files affected:**
- services/user_service.py
- services/health_service.py
- services/nutrition_service.py
- services/plan_service.py
- services/tracking_service.py

### 4. Cache Management (`AI_CACHE:`)
Tracks KV cache performance:
- Cache initialization
- Cache hits with key information
- Cache misses
- Cache entries stored with size metrics
- Cache eviction (FIFO policy)
- Cache statistics

**Files affected:**
- ai/inference/cache_manager.py

### 5. Application Lifecycle
Tracks startup, shutdown, and error handling:
- Logging system initialization
- Firebase initialization
- Flask application startup
- HTTP error handling

**Files affected:**
- extensions.py
- app.py

## Logging Infrastructure

### Log Files
Three separate log files are created:
1. **logs/backend.log** - All application logs
2. **logs/ai_inference.log** - AI-specific operations
3. **logs/database.log** - Database operations

### Log Configuration
```python
# Setup in extensions.py
- Log level: DEBUG (file), INFO (console)
- Rotating file handlers: 10MB max, 5 backups
- Timestamp format: YYYY-MM-DD HH:MM:SS
- Full module path tracking: module:function:line
```

### Logger Instances
```python
# Main logger
logger = logging.getLogger(__name__)

# AI-specific logger
ai_logger = logging.getLogger('ai')

# Database-specific logger
db_logger = logging.getLogger('database')
```

## Key Features

### 1. Performance Metrics
All AI inference operations log elapsed time:
```
AI_INFERENCE: Inference completed successfully for userId=user123, elapsed_time=2.34s
```

### 2. User Tracking
All operations include userId for easy filtering:
```
grep "userId=user123" logs/*.log  # Find all operations for a user
```

### 3. Request Parameters
Parameters are logged at INFO level:
```
AI_INFERENCE: Starting plan adjustment for userId=user123, request=Make workouts less intense
```

### 4. Error Details
Full exception stack traces are logged for debugging:
```
logger.error(f"Error message: {e}", exc_info=True)
```

### 5. Cache Statistics
Cache performance is tracked:
```
AI_CACHE: Stored cache entry: key... (1.25MB), total_cache=3.45MB
```

## Implementation Details

### Database Service Functions

Each service function logs:
- **START**: Operation type with user/entity ID
- **CHECKS**: Uniqueness checks, existence validation
- **OPERATIONS**: Field updates, data modifications
- **SUCCESS**: Successful completion
- **ERROR**: Exception with context

Example:
```python
def get_user(user_id):
    logger.info(f"DB_READ: Fetching user with userId={user_id}")
    try:
        # ... operation ...
        logger.debug(f"DB_READ: Successfully retrieved user data for userId={user_id}")
        return {"user": user_data}, 200
    except Exception as e:
        logger.error(f"DB_READ: Failed to retrieve user - userId={user_id}, error={str(e)}", exc_info=True)
        return {"error": str(e)}, 500
```

### AI Route Functions

Each AI endpoint logs:
- **START**: Inference initiation with operation type
- **PROGRESS**: Data fetching, prompt generation
- **TIMING**: Start and end times with elapsed duration
- **COMPLETION**: Successful inference result
- **FALLBACK**: Mock data usage indication
- **ERROR**: Inference failures with details

Example:
```python
def create_user_plan_ai(user_id):
    if use_ai:
        ai_logger.info(f"AI_USAGE: Initiating AI plan generation - userId={user_id}, plan_type={plan_type}")
        start_time = time.time()
        try:
            # ... AI inference ...
            elapsed_time = time.time() - start_time
            ai_logger.info(f"AI_INFERENCE: Inference completed successfully for userId={user_id}, elapsed_time={elapsed_time:.2f}s")
        except Exception as e:
            ai_logger.error(f"AI_INFERENCE: AI generation error for userId={user_id}: {e}", exc_info=True)
    else:
        ai_logger.info(f"AI_NON_USAGE: Mock data used instead of AI for userId={user_id}, plan_type={plan_type}")
```

## Usage Examples

### Monitor Real-time Logs
```bash
# Watch all logs
tail -f logs/backend.log

# Watch specific category
tail -f logs/ai_inference.log
tail -f logs/database.log
```

### Search Logs
```bash
# Find AI operations for a user
grep "userId=user123" logs/ai_inference.log

# Find cache performance
grep "AI_CACHE" logs/ai_inference.log

# Find database errors
grep "ERROR.*DB_" logs/database.log

# Find inference times
grep "elapsed_time" logs/ai_inference.log
```

### Performance Analysis
```bash
# Cache hit rate
grep "Cache HIT" logs/ai_inference.log | wc -l
grep "Cache MISS" logs/ai_inference.log | wc -l

# Average inference time
grep "elapsed_time" logs/ai_inference.log | awk '{print $(NF-1)}'

# Database operation count
wc -l logs/database.log
```

## Testing the Logging

To verify logging is working:

1. **Start the application**
   ```bash
   python app.py
   ```

2. **Check console output**
   - Should see startup messages with initialization details

3. **Make API requests**
   ```bash
   curl http://localhost:5000/health
   ```

4. **Verify log files**
   ```bash
   ls -lh logs/
   tail logs/backend.log
   ```

5. **Test AI operations**
   ```bash
   # Create plan with AI
   curl -X POST http://localhost:5000/users/test/plan \
     -H "Content-Type: application/json" \
     -d '{"use_ai": true}'

   # Check logs
   grep "AI_INFERENCE" logs/ai_inference.log
   ```

## Future Enhancements

Potential logging improvements:
1. Add request ID for tracing across service calls
2. Add performance monitoring for database operations
3. Add structured logging (JSON format) for log aggregation
4. Add metrics collection (Prometheus format)
5. Add request/response logging for API endpoints
6. Add audit logging for sensitive operations

## Notes

- All logging uses the `logging` standard library
- Logs are thread-safe and production-ready
- Performance impact is minimal (asynchronous file writing)
- Log rotation prevents disk space issues
- Structured format allows easy parsing and analysis
