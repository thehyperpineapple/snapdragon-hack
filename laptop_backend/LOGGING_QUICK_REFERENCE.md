# Logging Quick Reference

## What to Look For in Logs

### When Creating a Plan with AI
```
AI_USAGE: Initiating AI plan generation - userId=user123, plan_type=diet
AI_INFERENCE: Starting inference for userId=user123, plan_type=diet
AI_INFERENCE: Cache HIT/MISS for key: ...
AI_INFERENCE: Inference completed successfully for userId=user123, elapsed_time=X.XXs
DB_WRITE: Creating plan for userId=user123, ai_generated=true
DB_WRITE: Plan created successfully for userId=user123
```

### When Creating a Plan with Mock Data
```
AI_NON_USAGE: Mock data used instead of AI for userId=user123, plan_type=diet
DB_WRITE: Creating plan for userId=user123, ai_generated=false
DB_WRITE: Plan created successfully for userId=user123
```

### When Analyzing Health Metrics
```
AI_INFERENCE: Starting comprehensive health analysis for userId=user123
DB_READ: Fetching health profile for userId=user123
AI_INFERENCE: Health analysis completed for userId=user123, elapsed_time=X.XXs
```

### When User Registers
```
DB_WRITE: Attempting to register user with email=test@example.com, username=testuser
DB_READ: Checking for existing user with email=test@example.com
DB_READ: Checking for existing username=testuser
DB_WRITE: User registered successfully with userId=abc123, email=test@example.com
```

### When User Updates Health Profile
```
DB_READ: Fetching health profile for userId=user123
DB_WRITE: Updating health profile for userId=user123 with fields: ['weight', 'height']
DB_WRITE: Health profile updated successfully for userId=user123
```

### When User Logs a Meal
```
DB_WRITE: Logging daily meal for userId=user123, date=2024-02-07, meal_type=breakfast
DB_WRITE: Daily meal logged successfully for userId=user123
```

### Cache Operations
```
AI_CACHE: KV Cache initialized with 512MB limit
AI_INFERENCE: Cache HIT - returning cached response (prompt_len=245, max_tokens=1500)
AI_CACHE: Cache MISS for key: prompt_hash...
AI_CACHE: Stored cache entry: prompt_hash... (1.25MB), total_cache=3.45MB
```

### Error Scenarios

#### User Not Found
```
DB_READ: User not found - userId=invalid123
```

#### Email Already Exists
```
DB_READ: User registration failed - email already exists: test@example.com
```

#### AI Inference Failure
```
AI_INFERENCE: Failed to parse AI response for userId=user123: JSON parsing error
AI_INFERENCE: Inference failed at step 45: CUDA out of memory
```

#### Database Connection Error
```
DB_WRITE: Failed to create user - error=Connection timeout
ERROR: DB_WRITE: User registration failed with error: Connection timeout
```

## Log Files Location

```
logs/backend.log          # Main application log
logs/ai_inference.log     # AI operations only
logs/database.log         # Database operations only
```

## Common Filtering Commands

### Find all operations for a specific user
```bash
grep "userId=user123" logs/*.log
```

### Find all AI operations
```bash
grep "AI_INFERENCE\|AI_USAGE\|AI_NON_USAGE\|AI_CACHE" logs/ai_inference.log
```

### Find all database writes
```bash
grep "DB_WRITE" logs/database.log
```

### Find all errors
```bash
grep "ERROR" logs/backend.log
```

### Find operations for a specific timestamp
```bash
grep "2024-02-07 10:" logs/backend.log
```

### Find slow operations (over 2 seconds)
```bash
grep "elapsed_time=[2-9]\." logs/ai_inference.log
grep "elapsed_time=1[0-9]" logs/ai_inference.log
```

### Find cache hits vs misses
```bash
echo "Cache hits: $(grep 'Cache HIT' logs/ai_inference.log | wc -l)"
echo "Cache misses: $(grep 'Cache MISS' logs/ai_inference.log | wc -l)"
```

## Expected Performance Metrics

### Typical Inference Times
- Simple analysis: 0.5s - 1.5s
- Plan generation: 2.0s - 4.0s
- Complex analysis: 1.5s - 3.0s

### Cache Performance
- First request (cache miss): Full inference time
- Cached request (cache hit): < 10ms
- Target cache hit rate: > 50% for repeated queries

### Database Operations
- Read: 50-200ms
- Write: 100-300ms
- Batch operations: 200-500ms

## Troubleshooting Guide

### Problem: No logs appearing
1. Check if logs directory exists: `ls -la logs/`
2. Check file permissions: `ls -la logs/backend.log`
3. Verify logging initialization in console output

### Problem: Slow inference times
1. Check cache hit rate: `grep "Cache HIT" logs/ai_inference.log | wc -l`
2. Look for errors in inference logs: `grep "ERROR" logs/ai_inference.log`
3. Check token generation progress: `grep "Generated.*tokens" logs/ai_inference.log`

### Problem: Database errors
1. Search for DB_WRITE failures: `grep "DB_WRITE.*error" logs/database.log`
2. Check for user not found errors: `grep "User not found" logs/database.log`
3. Look for connection timeouts: `grep "Connection\|timeout" logs/database.log`

### Problem: User-specific issues
1. Get all logs for user: `grep "userId=<userid>" logs/*.log`
2. Check for auth failures: `grep "userId=<userid>.*error" logs/*.log`
3. Review plan generation logs: `grep "userId=<userid>.*AI_INFERENCE" logs/*.log`

## Log Format Breakdown

Example log message:
```
[2024-02-07 10:30:45] INFO [routes/plan_ai:create_user_plan_ai:73] AI_INFERENCE: Starting inference for userId=user123, plan_type=diet
```

Breakdown:
- `[2024-02-07 10:30:45]` - Timestamp
- `INFO` - Log level
- `[routes/plan_ai:create_user_plan_ai:73]` - Module:Function:LineNumber
- `AI_INFERENCE:` - Operation category
- `Starting inference for userId=user123, plan_type=diet` - Message details

## Monitoring Dashboard Ideas

Set up monitoring for:
1. **AI Performance**
   - Average inference time
   - Cache hit rate
   - Inference error rate

2. **Database Health**
   - Read/write operation counts
   - Operation latency
   - Error counts by type

3. **User Activity**
   - Plan creations per day
   - AI vs mock data ratio
   - Most active users

4. **System Health**
   - Cache memory usage
   - Error rates over time
   - Log file size

## Real-time Monitoring Commands

```bash
# Watch AI operations in real-time
watch -n 1 'tail -n 20 logs/ai_inference.log'

# Count operations by category
watch -n 5 '
echo "=== AI Operations ===" && \
grep -c "AI_INFERENCE" logs/ai_inference.log && \
echo "=== Database Operations ===" && \
grep -c "DB_" logs/database.log
'

# Monitor inference times
watch -n 10 '
tail logs/ai_inference.log | grep "elapsed_time" | tail -5
'
```

## Exporting and Sharing Logs

```bash
# Export logs for a specific user
grep "userId=user123" logs/*.log > user123_logs.txt

# Export last 24 hours of logs
find logs -name "*.log" -newer /tmp/yesterday > recent_logs.txt

# Create compressed archive
tar -czf logs_backup_$(date +%Y%m%d_%H%M%S).tar.gz logs/

# Export only errors
grep "ERROR\|error" logs/backend.log > errors.log
```

## Integration with External Tools

### Send to external logging service
```bash
# Tail and send to remote server
tail -f logs/backend.log | nc remote-server.com 5000
```

### Parse logs with jq (after JSON conversion)
```bash
# Would require structured logging in JSON format
# Future enhancement
```

### Grep for specific patterns
```bash
# Find all AI generation requests
grep "AI_INFERENCE.*Starting" logs/ai_inference.log

# Find all slow operations
grep "elapsed_time" logs/*.log | awk '{if ($(NF-1) > 3.0) print}'
```
