# Comprehensive Logging Guide

This document describes all the logging that has been added to the backend application, covering AI inference, database operations, caching, and more.

## Overview

The application now includes comprehensive logging across all major components:

- **AI Inference**: Tracks all LLM inference calls, generation times, and cache hits/misses
- **AI Usage**: Tracks when AI is used vs. when fallbacks are used
- **Database Operations**: Logs all reads and writes to Firestore
- **Caching**: Detailed KV cache statistics and management
- **Error Handling**: All exceptions are logged with full stack traces

## Log Files

Logs are stored in the `logs/` directory with the following structure:

```
logs/
├── backend.log           # Main application log (rotated, 10MB max, 5 backups)
├── ai_inference.log      # AI-specific operations
└── database.log          # Database operations
```

### Log Rotation

All logs use rotating file handlers with the following configuration:
- **Max file size**: 10MB per file
- **Backup count**: 5 older files retained
- **Total potential log size**: ~60MB (6 files × 10MB)

## Log Levels

- **DEBUG**: Detailed information for debugging (file only)
- **INFO**: General operational messages (file and console)
- **WARNING**: Warning messages (file and console)
- **ERROR**: Error messages with stack traces (file and console)

## AI Inference Logging

All AI inference operations are logged with the prefix `AI_INFERENCE:` and include:

### Cache Operations
```
AI_INFERENCE: Cache HIT - returning cached response (prompt_len=245, max_tokens=1500)
AI_CACHE: Cache HIT for key: prompt_hash...
AI_CACHE: Cache MISS for key: prompt_hash...
```

### Inference Start/Complete
```
AI_INFERENCE: Starting inference for userId=user123, plan_type=diet
AI_INFERENCE: Inference completed successfully for userId=user123, elapsed_time=2.34s
```

### Simulation Mode
```
AI_INFERENCE: Running in SIMULATION mode (no QNN backend) - prompt_len=245
AI_INFERENCE: Simulation mode response generated (response_len=1250)
```

### Token Generation
```
AI_INFERENCE: Generation complete. Total tokens generated: 125, response_length: 2048 chars
AI_INFERENCE: Generated 50 tokens so far...
```

### Errors
```
AI_INFERENCE: Failed to parse AI response for userId=user123: JSON parsing error
AI_INFERENCE: Inference failed at step 45: CUDA out of memory
```

## AI Usage Tracking

The application tracks when AI is used and when fallbacks are used:

### AI Usage
```
AI_USAGE: Initiating AI plan generation - userId=user123, plan_type=combined
AI_USAGE: Complete health insights for userId=user123, elapsed_time=1.45s
```

### AI Non-Usage (Fallback)
```
AI_NON_USAGE: Mock data used instead of AI for userId=user123, plan_type=diet
AI_NON_USAGE: Health insights not requested for userId=user123
```

## Database Logging

All database operations are logged with prefixes `DB_READ:` or `DB_WRITE:`:

### Database Reads
```
DB_READ: Fetching user with userId=user123
DB_READ: User not found - userId=user456
DB_READ: Health profile retrieved successfully for userId=user123
DB_READ: Checking for existing user with email=test@example.com
```

### Database Writes
```
DB_WRITE: Creating health profile for userId=user123
DB_WRITE: Health profile created successfully for userId=user123
DB_WRITE: User registration failed - email already exists: test@example.com
DB_WRITE: Updating plan for userId=user123 with fields: ['diet', 'workouts']
DB_WRITE: Deleting health profile for userId=user123
```

### Database Operations by Service

#### User Service
- `register_user_entry()`: User registration, email/username uniqueness checks
- `get_user()`: User retrieval
- `delete_user()`: User deletion

#### Health Service
- `post_health_data()`: Health profile creation
- `get_health_data()`: Health profile retrieval
- `patch_health_data()`: Health profile updates
- `delete_health_data()`: Health profile deletion

#### Nutrition Service
- `post_nutrition_data()`: Nutrition profile creation
- `get_nutrition_data()`: Nutrition profile retrieval
- `update_nutrition_data()`: Nutrition profile updates
- `delete_nutrition_data()`: Nutrition profile deletion
- `add_diet_entry()`: Diet entry addition

#### Plan Service
- `create_plan()`: Plan creation (tracks if AI-generated)
- `get_plan()`: Plan retrieval
- `update_plan()`: Plan updates
- `delete_plan()`: Plan deletion

#### Tracking Service
- `update_meal_completion()`: Meal completion updates
- `toggle_workout_status()`: Workout status updates
- `log_daily_meal()`: Daily meal logging
- `log_daily_workout()`: Daily workout logging

## Cache Management Logging

Cache operations are logged with the prefix `AI_CACHE:`:

```
AI_CACHE: KV Cache initialized with 512MB limit
AI_CACHE: Cache HIT for key: prompt_hash...
AI_CACHE: Cache MISS for key: prompt_hash...
AI_CACHE: Stored cache entry: prompt_hash... (1.25MB), total_cache=3.45MB
AI_CACHE: Evicted cache entry: old_prompt_hash... (2.10MB)
AI_CACHE: KV cache cleared
```

## Application Startup Logging

```
================================================================================
Backend Application Started
Logging initialized: 2024-02-07T10:30:45.123456
================================================================================

Firebase initialized with credentials from: ./serviceAccountKey.json
Firestore client initialized successfully

================================================================================
Starting Nutrition & Workout API
Host: 0.0.0.0, Port: 5000, Debug: true
================================================================================
```

## Error Handling Logging

All errors are logged with full stack traces for debugging:

```
ERROR: DB_WRITE: Failed to create health profile - userId=user123, error=Connection timeout
ERROR: AI_INFERENCE: Plan validation error for userId=user123: JSON decode error
ERROR: AI_INFERENCE: Inference failed at step 45: CUDA out of memory (traceback follows)
```

## HTTP Error Logging

Flask error handlers log all HTTP errors:

```
WARNING: Bad request: Invalid JSON in request body
WARNING: Resource not found: /api/unknown-endpoint
ERROR: Internal server error: NoneType has no attribute 'get'
```

## Viewing Logs

### Real-time Monitoring
```bash
# Watch all logs
tail -f logs/backend.log

# Watch AI operations only
tail -f logs/ai_inference.log

# Watch database operations only
tail -f logs/database.log
```

### Search and Filter
```bash
# Find all AI inference operations
grep "AI_INFERENCE:" logs/ai_inference.log

# Find all cache hits
grep "Cache HIT" logs/ai_inference.log

# Find all database writes for a specific user
grep "userId=user123" logs/database.log | grep "DB_WRITE"

# Find all errors
grep "ERROR" logs/backend.log

# Check performance metrics
grep "elapsed_time" logs/ai_inference.log
```

## Performance Monitoring

Use the logs to monitor performance:

```bash
# Average inference time
grep "elapsed_time" logs/ai_inference.log | awk '{print $(NF-1)}'

# Cache hit rate
cache_hits=$(grep "Cache HIT" logs/ai_inference.log | wc -l)
cache_misses=$(grep "Cache MISS" logs/ai_inference.log | wc -l)
echo "Hit rate: $(($cache_hits * 100 / ($cache_hits + $cache_misses)))%"

# Database operations per user
grep "userId=" logs/database.log | cut -d'=' -f3 | cut -d',' -f1 | sort | uniq -c
```

## Log Message Format

All log messages follow this format:

```
[YYYY-MM-DD HH:MM:SS] LEVEL [module:function:line] message
```

Example:
```
[2024-02-07 10:30:45] INFO [routes/plan_ai:create_user_plan_ai:73] AI_INFERENCE: Starting inference for userId=user123, plan_type=diet
```

## Debugging Tips

1. **AI Performance Issues**: Check `logs/ai_inference.log` for elapsed_time metrics
2. **Database Errors**: Check `logs/database.log` for DB_READ/DB_WRITE errors
3. **Cache Issues**: Look for `AI_CACHE` messages to understand cache behavior
4. **User-Specific Debugging**: Filter logs by userId to trace all operations for a user
5. **Error Investigation**: Check ERROR level messages with full stack traces

## Configuration

Logging is configured in `extensions.py`. To modify logging settings:

1. **Change log level**: Modify `logger.setLevel()` in `setup_logging()`
2. **Add new log files**: Create new handlers in `setup_logging()`
3. **Change file size limits**: Modify `maxBytes` in `RotatingFileHandler`
4. **Change log format**: Modify the `log_format` string

## Integration with Monitoring Tools

The structured log format makes it easy to integrate with monitoring tools:

- **ELK Stack**: Parse logs using the structured format
- **Datadog**: Forward logs to Datadog agent
- **CloudWatch**: Send logs to AWS CloudWatch
- **Splunk**: Index logs in Splunk

Example log parsing pattern:
```
%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \[%{DATA:module}:%{DATA:function}:%{INT:line}\] %{DATA:prefix}: %{GREEDYDATA:message}
```
