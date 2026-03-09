# Logging Quick Reference

## Log Statements

```java
// INFO - Business events
log.info("Processing order for customer: {}", customerId);

// DEBUG - Technical details
log.debug("Checking cache for key: {}", cacheKey);

// WARN - Potential issues
log.warn("Response time slow: {}ms", duration);

// ERROR - Exceptions (exception object last!)
log.error("Payment failed for order {}", orderId, e);
```

## Common Commands

```bash
# View logs in real-time
tail -f logs/bi-dashboard-api.log

# Search by correlation ID
grep "abc-123" logs/bi-dashboard-api.log

# Count errors
grep "ERROR" logs/bi-dashboard-api.log | wc -l

# Find slow operations (>1000ms)
grep -E "duration=[0-9]{4,}ms" logs/bi-dashboard-api.log

# View errors from last hour
grep "$(date '+%Y-%m-%d %H')" logs/bi-dashboard-api-error.log
```

## Testing

```bash
# Start application
./mvnw spring-boot:run

# Make request
curl http://localhost:8081/api/customers/1

# Make request with custom correlation ID
curl -H "X-Correlation-Id: MY-TEST-123" http://localhost:8081/api/customers/1

# View response headers (includes correlation ID)
curl -i http://localhost:8081/api/customers/1

# Generate multiple requests
for i in {1..100}; do curl http://localhost:8081/api/customers/$i; done
```

## Log Files Location

- `logs/bi-dashboard-api.log` - All logs (INFO, DEBUG, ERROR)
- `logs/bi-dashboard-api-error.log` - Only errors
- `logs/bi-dashboard-api-sql.log` - Database queries
- `logs/archive/` - Compressed old logs

## Correlation ID Flow

```
Request → LoggingFilter (generates ID: abc-123)
        ↓
        [abc-123] Controller logs entry
        ↓
        [abc-123] Service logs operation
        ↓
        [abc-123] Repository logs query (AOP)
        ↓
        [abc-123] SQL executes
        ↓
        [abc-123] Controller logs exit
        ↓
Response ← LoggingFilter (adds X-Correlation-Id header)
```

## Best Practices

✅ **DO:**
- Use parameterized logging: `log.info("User: {}", username)`
- Log collection sizes: `log.info("Found {} items", list.size())`
- Include correlation IDs (automatic via MDC)
- Log exceptions with stack trace: `log.error("Error", e)`
- Log execution time for slow operations

❌ **DON'T:**
- Use string concatenation: `log.info("User: " + username)`
- Log full collections: `log.info("Items: {}", list)`
- Log passwords, API keys, credit cards
- Log inside tight loops
- Log without context

## Log Levels (Most to Least Verbose)

```
TRACE → DEBUG → INFO → WARN → ERROR
```

**Production:** Usually INFO or WARN
**Development:** Usually DEBUG
**Troubleshooting:** TRACE (very detailed)

## Sensitive Data Protection

```java
// Use LoggingUtil.sanitize() for sensitive data
log.info("Password: {}", LoggingUtil.sanitize(password));
// Logs: Password: Pa****rd

// Check if field is sensitive
if (LoggingUtil.isSensitiveField("apiKey")) {
    log.info("Field: ****");
}
```

## File Structure

```
src/main/
├── java/exercise/bidashboardapi/
│   ├── filter/
│   │   └── LoggingFilter.java          ← Correlation ID generation
│   ├── util/
│   │   └── LoggingUtil.java            ← Helper methods
│   ├── aspect/
│   │   └── RepositoryLoggingAspect.java ← AOP repository logging
│   ├── controller/                      ← @Slf4j on all controllers
│   ├── service/                         ← @Slf4j on all services
│   └── config/                          ← @Slf4j on all configs
└── resources/
    └── logback-spring.xml               ← Logging configuration
```

## Troubleshooting

**Problem:** Logs not appearing
- Check log level in logback-spring.xml
- Verify @Slf4j annotation on class
- Check console for errors

**Problem:** Correlation ID missing
- Check LoggingFilter is running
- Verify MDC.put() is called
- Check pattern has %X{correlationId}

**Problem:** Logs filling disk
- Check log rotation settings
- Verify maxHistory is set
- Check totalSizeCap limit

**Problem:** Application slow
- Check if using async appenders
- Verify discardingThreshold is 0
- Check queueSize is adequate (512)

## Links

- Full Guide: `LOGGING_GUIDE.md`
- Logback Docs: http://logback.qos.ch/manual/
- SLF4J Manual: http://www.slf4j.org/manual.html
- Spring Boot Logging: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging
