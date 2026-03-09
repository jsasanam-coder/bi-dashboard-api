# Complete Beginner's Guide to Logging in Spring Boot

**Project:** bi-dashboard-api
**Date:** March 2, 2026
**Author:** Learning Exercise - Day 1

---

## Table of Contents
1. [What is Logging?](#what-is-logging)
2. [Why Do We Need Logging?](#why-do-we-need-logging)
3. [What We Built Today](#what-we-built-today)
4. [The Components Explained](#the-components-explained)
5. [Logging Levels](#logging-levels)
6. [How Logging Flow Works](#how-logging-flow-works)
7. [Testing Your Implementation](#testing-your-implementation)
8. [Learning Outcomes](#learning-outcomes)
9. [Practice Exercises](#practice-exercises)
10. [Quick Reference](#quick-reference)
11. [Common Questions](#common-questions)

---

## What is Logging?

**Logging** is like keeping a diary for your application. Just like you might write in a diary:
- "Woke up at 7am"
- "Had breakfast"
- "Went to work"

Your application writes down what it's doing:
- "Received a request from user to get customer data"
- "Found customer with ID 123"
- "Sending response back"

### Real-World Analogy

Think of logging like a **black box recorder in an airplane**:
- Records everything that happens during the flight
- When something goes wrong, investigators use the black box to understand what happened
- Your application logs are the same - they help you understand what happened when things go wrong

---

## Why Do We Need Logging?

### 1. **Debugging** 🐛
When something breaks, logs tell you exactly what happened:
```
ERROR: Database connection failed at 10:30:15
ERROR: Attempted to connect to: jdbc:sqlserver://10.9.182.73:1433
ERROR: Reason: Connection timeout after 30 seconds
```
Now you know: "The database server wasn't responding!"

### 2. **Monitoring** 📊
See if your application is running smoothly:
```
INFO: Average response time: 150ms
INFO: Processed 1,000 requests in last hour
INFO: Memory usage: 45%
```

### 3. **Tracking** 🔍
Follow a specific user's request through the entire system:
```
[abc-123] User logged in
[abc-123] Viewed product page
[abc-123] Added item to cart
[abc-123] Completed payment
```

### 4. **Compliance** 📋
Keep records for audits and legal requirements:
- Who accessed what data?
- When was sensitive information modified?
- What actions were performed?

---

## What We Built Today

Think of your application as a **restaurant**. We built a comprehensive system to track everything that happens.

### 1. The Diary Books (Log Files) 📚

We created **3 separate notebooks**:

#### **application.log** - The Main Journal
Records everything that happens in your restaurant:
```
2026-03-02 10:30:15 INFO [abc-123] Customer entered
2026-03-02 10:30:20 INFO [abc-123] Ordered burger and fries
2026-03-02 10:45:00 INFO [abc-123] Food delivered
2026-03-02 11:00:00 INFO [abc-123] Customer left
```

**Contains:**
- All INFO, DEBUG, ERROR level logs
- Normal operations
- Business logic execution
- Request/response information

#### **error.log** - The Problem Log
Only records when things go wrong:
```
2026-03-02 10:35:00 ERROR [xyz-789] Kitchen ran out of tomatoes
2026-03-02 10:40:00 ERROR [def-456] Payment system crashed
```

**Contains:**
- Only ERROR level logs
- Exceptions with full stack traces
- System failures
- Critical issues

#### **sql.log** - The Database Activity Log
Records all database queries (like checking inventory):
```
2026-03-02 10:30:16 DEBUG Hibernate: SELECT * FROM customers WHERE customer_id = ?
2026-03-02 10:30:16 TRACE Binding parameter [1] as INTEGER - 123
2026-03-02 10:30:17 DEBUG Hibernate: SELECT * FROM orders WHERE customer_id = ?
```

**Contains:**
- SQL queries executed by Hibernate
- JDBC template queries
- Query parameters
- Database operations

### 2. The Order Tracking System (Correlation ID) 🎫

#### The Problem
Imagine a restaurant where 100 customers order at the same time. How do you track which order belongs to which customer?

```
10:30:01 - Received order
10:30:02 - Received order
10:30:03 - Checking inventory
10:30:04 - Checking inventory
```
😕 **Which order is which? No idea!**

#### The Solution: Give Each Order a Unique Ticket Number!

```
Customer A's order → Ticket #abc-123
Customer B's order → Ticket #def-456
```

This is exactly what **Correlation ID** does:
- Every request gets a unique ID (UUID like `550e8400-e29b-41d4-a716-446655440000`)
- This ID is attached to EVERY log message for that request
- Now you can follow one customer's journey through all the logs

#### Example WITH Correlation ID
```
10:30:01 [abc-123] - Received order from Customer A
10:30:02 [def-456] - Received order from Customer B
10:30:03 [abc-123] - Checking inventory for Customer A
10:30:04 [def-456] - Checking inventory for Customer B
10:30:05 [abc-123] - Order confirmed for Customer A
10:30:06 [def-456] - Order confirmed for Customer B
```
✅ **Now you can track each order separately!**

---

## The Components Explained

Let's understand each file we created:

### 1. LoggingFilter.java - The Entry Gate Guard 🚪

**Location:** `src/main/java/exercise/bidashboardapi/filter/LoggingFilter.java`

Think of this as a security guard at the restaurant entrance who:

1. **Gives each customer a ticket number** (Correlation ID)
2. **Notes when they entered** ("Customer arrived at 10:30")
3. **Notes when they left** ("Customer left at 11:00, spent 30 minutes")

#### What It Does (Simplified)
```java
public void doFilter(request, response) {
    // Step 1: Generate or extract correlation ID
    String correlationId = UUID.randomUUID().toString();  // Like: abc-123

    // Step 2: Store it in MDC (thread-local storage)
    MDC.put("correlationId", correlationId);

    // Step 3: Add it to response headers
    response.setHeader("X-Correlation-Id", correlationId);

    // Step 4: Log request arrival
    log.info("Incoming request: method={}, uri={}, correlationId={}");

    long startTime = System.currentTimeMillis();

    // Step 5: Let the request proceed through the application
    chain.doFilter(request, response);

    // Step 6: Calculate how long it took
    long duration = System.currentTimeMillis() - startTime;

    // Step 7: Log request completion
    log.info("Outgoing response: status={}, duration={}ms", status, duration);

    // Step 8: Clean up MDC to prevent memory leaks
    MDC.clear();
}
```

#### Why Is This Important?
- **Automatic** - You don't have to manually add correlation IDs to every log
- **Consistent** - Every log for the same request has the same ID
- **Traceable** - You can follow a request from entry to exit

---

### 2. LoggingUtil.java - The Helper Tools 🛠️

**Location:** `src/main/java/exercise/bidashboardapi/util/LoggingUtil.java`

This is like having utility tools in your restaurant.

#### Feature 1: Sanitize Function - The Privacy Protector 🔒

Never write sensitive information in logs (like passwords or credit cards):

**❌ BAD - Logging a password:**
```java
log.info("User password: MySecret123");  // NEVER DO THIS!
```

**✅ GOOD - Masking sensitive data:**
```java
String maskedPassword = LoggingUtil.sanitize("MySecret123");
log.info("User password: {}", maskedPassword);  // Logs: My****23
```

**Examples:**
```java
sanitize("MySecret123")           → "My****23"
sanitize("1234-5678-9012-3456")   → "12**********56"
sanitize("abc")                   → "****" (too short, mask everything)
```

**How It Works:**
```java
public static String sanitize(String value) {
    if (value == null) return null;
    if (value.length() <= 4) return "****";

    // Show first 2 and last 2 characters
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
}
```

#### Feature 2: isSensitiveField - The Smart Detector 🔍

Automatically detects if a field name contains sensitive keywords:

```java
isSensitiveField("password")     → true  (don't log it!)
isSensitiveField("username")     → false (safe to log)
isSensitiveField("apiKey")       → true  (don't log it!)
isSensitiveField("ssn")          → true  (don't log it!)
isSensitiveField("creditCard")   → true  (don't log it!)
```

**Usage in Code:**
```java
String fieldName = "password";
String fieldValue = "MySecret123";

if (LoggingUtil.isSensitiveField(fieldName)) {
    log.info("Field {}: {}", fieldName, LoggingUtil.sanitize(fieldValue));
    // Logs: Field password: My****23
} else {
    log.info("Field {}: {}", fieldName, fieldValue);
    // Logs: Field username: JohnDoe
}
```

---

### 3. RepositoryLoggingAspect.java - The Database Monitor 📊

**Location:** `src/main/java/exercise/bidashboardapi/aspect/RepositoryLoggingAspect.java`

#### What is AOP (Aspect-Oriented Programming)?

**Analogy:** Imagine you have 10 waiters in your restaurant. Instead of teaching each waiter individually to write down what they're doing, you hire a **supervisor** who automatically watches everyone and writes it down.

**That's AOP!**

#### Without AOP (Manual Logging)
You have to add logging to EVERY repository method:

```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    default Customer findByIdWithLogging(Integer id) {
        log.debug("Finding customer with id: {}", id);
        long startTime = System.currentTimeMillis();

        Optional<Customer> result = findById(id);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Found customer in {}ms", duration);

        return result.orElse(null);
    }
}
```

❌ **Problems:**
- Repetitive code in every method
- Easy to forget
- Hard to maintain

#### With AOP (Automatic Logging)

```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    // That's it! No logging code needed!
    // AOP automatically logs for you
}
```

✅ **Benefits:**
- No repetitive code
- Consistent logging everywhere
- Easy to maintain (change one place, affects all repositories)

#### How RepositoryLoggingAspect Works

```java
@Around("execution(* exercise.bidashboardapi.repository.*.*(..))")
public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    // Before method execution
    String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    Object[] args = joinPoint.getArgs();

    log.debug("Executing {}.{}({})", className, methodName, formatArgs(args));

    long startTime = System.currentTimeMillis();

    try {
        // Execute the actual method
        Object result = joinPoint.proceed();

        // After successful execution
        long duration = System.currentTimeMillis() - startTime;
        log.debug("Completed {}.{} in {}ms, result: {}",
                  className, methodName, duration, formatResult(result));

        return result;

    } catch (Exception e) {
        // If method throws exception
        long duration = System.currentTimeMillis() - startTime;
        log.error("Failed {}.{} after {}ms: {}",
                  className, methodName, duration, e.getMessage(), e);
        throw e;
    }
}
```

**Example Output:**
```
DEBUG [abc-123] Executing CustomerRepository.findById(123)
DEBUG [abc-123] Completed CustomerRepository.findById in 45ms, result: Optional[present]
```

---

### 4. logback-spring.xml - The Configuration File ⚙️

**Location:** `src/main/resources/logback-spring.xml`

This is like the restaurant's **operations manual**. It defines all the rules for logging.

#### Section 1: Properties (Variables)
```xml
<property name="LOG_PATH" value="logs"/>
<property name="APP_NAME" value="bi-dashboard-api"/>
```
**Translation:** "Store log files in the 'logs' folder and name them 'bi-dashboard-api'"

#### Section 2: Console Appender (Screen Output)
```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%X{correlationId}] [%thread] %cyan(%logger{36}) - %msg%n</pattern>
    </encoder>
</appender>
```

**Translation:** "Write logs to the console (terminal) with this format:"

**Pattern Breakdown:**
- `%d{yyyy-MM-dd HH:mm:ss.SSS}` → Date and time: `2026-03-02 10:30:15.123`
- `%highlight(%-5level)` → Log level in color: `INFO`, `ERROR`, etc.
- `[%X{correlationId}]` → Correlation ID from MDC: `[abc-123]`
- `[%thread]` → Thread name: `[http-nio-8081-exec-1]`
- `%cyan(%logger{36})` → Logger name in cyan: `e.b.controller.DashboardController`
- `%msg` → The actual log message
- `%n` → New line

**Example Output:**
```
2026-03-02 10:30:15.123 INFO  [abc-123] [http-nio-8081-exec-1] e.b.controller.DashboardController - Fetching KPIs
```

#### Section 3: File Appender (Write to Files)
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}.log</file>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{correlationId}] %logger{36} - %msg%n</pattern>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_PATH}/archive/${APP_NAME}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>1GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

**Translation:**
- Write to file: `logs/bi-dashboard-api.log`
- When file reaches 10MB, create a new file
- Move old files to: `logs/archive/bi-dashboard-api-2026-03-02.0.log.gz`
- Keep files for 30 days, then delete
- Maximum total size: 1GB

**Why Log Rotation?**
Without rotation, log files would grow infinitely and fill your disk!

**Example:**
```
Day 1: bi-dashboard-api.log (10MB)
Day 2: bi-dashboard-api.log (10MB) → Day 1 moved to archive/bi-dashboard-api-2026-03-01.0.log.gz
Day 3: bi-dashboard-api.log (10MB) → Day 2 moved to archive/bi-dashboard-api-2026-03-02.0.log.gz
...
Day 31: Day 1 archive deleted (older than 30 days)
```

#### Section 4: Async Appender (Performance Boost) ⚡
```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
```

**Translation:**
- Don't block the application while writing logs
- Use a queue to buffer 512 log messages
- Write to disk in the background

**Why Async?**
- **Synchronous (blocking):** App waits for disk write (slow) → 100ms delay
- **Asynchronous (non-blocking):** App adds to queue (fast) → 1ms delay

#### Section 5: Logger Levels Per Package
```xml
<logger name="exercise.bidashboardapi.controller" level="INFO" />
<logger name="exercise.bidashboardapi.service" level="DEBUG" />
<logger name="exercise.bidashboardapi.repository" level="DEBUG" />
<logger name="org.hibernate.SQL" level="DEBUG" />
```

**Translation:**
- Controllers: Log INFO and above (INFO, WARN, ERROR)
- Services: Log DEBUG and above (DEBUG, INFO, WARN, ERROR)
- Repositories: Log DEBUG and above
- Hibernate SQL: Log DEBUG to capture SQL queries

---

## Logging Levels

Think of logging levels like **alert levels in a hospital**:

### **TRACE** - Extremely Detailed (Doctor's Detailed Notes) 🔬
Every tiny detail, used for deep debugging:

```java
log.trace("Entering method processOrder with orderId={}", orderId);
log.trace("Binding SQL parameter [1] as VARCHAR - 'John Doe'");
log.trace("Cache lookup for key: customer_123");
```

**When to use:** Almost never in application code. Hibernate uses this internally for SQL parameter binding.

---

### **DEBUG** - Detailed Information (Nurse's Notes) 📝
Helpful for developers to understand flow:

```java
log.debug("Executing query: SELECT * FROM customers WHERE id = ?");
log.debug("Cache hit for key: customer_123");
log.debug("Validating customer data: {}", customer);
```

**When to use:** Development and troubleshooting. Turn off in production for performance.

**Example:**
```
DEBUG [abc-123] Executing CustomerRepository.findById(123)
DEBUG [abc-123] Hibernate: SELECT * FROM customers WHERE customer_id = ?
DEBUG [abc-123] Query completed in 45ms
```

---

### **INFO** - General Information (Patient Chart) 📋
Normal operations, business events:

```java
log.info("Customer order received, orderId={}", orderId);
log.info("Payment processed successfully for amount ${}", amount);
log.info("Email sent to customer: {}", email);
```

**When to use:** Important business events, milestones, successful operations.

**Example:**
```
INFO [abc-123] Incoming request: GET /api/customers/123
INFO [abc-123] Customer found: name=John Doe
INFO [abc-123] Outgoing response: status=200, duration=150ms
```

---

### **WARN** - Warning (Elevated Concern) ⚠️
Something unusual but not critical:

```java
log.warn("Customer data cache expired, refreshing from database");
log.warn("Response time slow: {} seconds", duration);
log.warn("Deprecated method used: getOldCustomerData()");
```

**When to use:** Recoverable issues, performance degradation, deprecated functionality.

**Example:**
```
WARN [abc-123] No analytics found for customer 123, using default values
WARN [abc-123] Database query took 5 seconds (threshold: 1 second)
```

---

### **ERROR** - Error (Emergency) 🚨
Something went wrong, needs attention:

```java
try {
    processPayment();
} catch (PaymentException e) {
    log.error("Payment processing failed for orderId={}", orderId, e);
    throw e;
}
```

**When to use:** Exceptions, failures, critical issues.

**Example:**
```
ERROR [abc-123] ResourceNotFoundException: Customer with id 999 not found
    at exercise.bidashboardapi.service.TransactionalService.getCustomerById(TransactionalService.java:95)
    at exercise.bidashboardapi.controller.TransactionalController.getCustomer(TransactionalController.java:34)
    ... (full stack trace)
```

---

### Log Level Hierarchy

```
TRACE → DEBUG → INFO → WARN → ERROR
└─────────────── More Detail
                      Less Detail ─────────────┘
```

**If you set level to INFO:**
- ✅ INFO messages are logged
- ✅ WARN messages are logged
- ✅ ERROR messages are logged
- ❌ DEBUG messages are NOT logged
- ❌ TRACE messages are NOT logged

---

## How Logging Flow Works

Let's trace a **complete request** through your application step by step.

### Scenario: User Requests Customer Data

**Request:** `GET http://localhost:8081/api/customers/123`

---

#### **Step 1: Request Arrives at LoggingFilter** 🚪
```
2026-03-02 10:30:15.001 INFO  [abc-123] [http-nio-8081-exec-1] e.b.filter.LoggingFilter - Incoming request: method=GET, uri=/api/customers/123, correlationId=abc-123
```

**What happened:**
- Filter generated correlation ID: `abc-123`
- Stored it in MDC (thread-local storage)
- Started timing the request
- Logged the incoming request

---

#### **Step 2: Request Reaches Controller** 🎯
```
2026-03-02 10:30:15.005 INFO  [abc-123] [http-nio-8081-exec-1] e.b.controller.TransactionalController - Entering TransactionalController.getCustomer with customerId=123
```

**What happened:**
- Controller method `getCustomer()` was called
- Logged entry with parameter

**Code:**
```java
@GetMapping("/customers/{id}")
public Customer getCustomer(@PathVariable Integer id) {
    log.info("Entering TransactionalController.getCustomer with customerId={}", id);
    Customer customer = transactionalService.getCustomerById(id);
    log.info("Exiting TransactionalController.getCustomer with customer name={}", customer.getName());
    return customer;
}
```

---

#### **Step 3: Controller Calls Service** 🔄
```
2026-03-02 10:30:15.010 INFO  [abc-123] [http-nio-8081-exec-1] e.b.service.TransactionalService - Fetching customer with customerId=123
```

**What happened:**
- Service method `getCustomerById()` was called
- Logged the business operation

**Code:**
```java
public Customer getCustomerById(Integer customerId) {
    log.info("Fetching customer with customerId={}", customerId);

    if (customerId <= 0) {
        throw new BadRequestException("Customer ID must be positive");
    }

    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

    log.info("Fetched customer customerId={}, name={}", customer.getCustomerId(), customer.getName());
    return customer;
}
```

---

#### **Step 4: Service Calls Repository** 💾
```
2026-03-02 10:30:15.015 DEBUG [abc-123] [http-nio-8081-exec-1] e.b.aspect.RepositoryLoggingAspect - Executing CustomerRepository.findById(123)
```

**What happened:**
- AOP aspect intercepted the repository call
- Logged method name and parameters (automatically!)

---

#### **Step 5: Database Query Executes** 🗄️
```
2026-03-02 10:30:15.020 DEBUG [abc-123] [http-nio-8081-exec-1] org.hibernate.SQL -
    SELECT
        customer_id, name, email, phone, address, city, country
    FROM customers
    WHERE customer_id = ?

2026-03-02 10:30:15.022 TRACE [abc-123] [http-nio-8081-exec-1] o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as INTEGER - 123
```

**What happened:**
- Hibernate generated SQL query
- Logged the query (formatted)
- Logged parameter binding

---

#### **Step 6: Repository Returns Result** ✅
```
2026-03-02 10:30:15.060 DEBUG [abc-123] [http-nio-8081-exec-1] e.b.aspect.RepositoryLoggingAspect - Completed CustomerRepository.findById in 45ms, result: Optional[present]
```

**What happened:**
- AOP aspect logged completion
- Included execution time (45ms)
- Logged result summary

---

#### **Step 7: Service Returns to Controller** 📤
```
2026-03-02 10:30:15.065 INFO  [abc-123] [http-nio-8081-exec-1] e.b.service.TransactionalService - Fetched customer customerId=123, name=John Doe
```

**What happened:**
- Service logged successful fetch
- Included customer details (not sensitive data)

---

#### **Step 8: Controller Sends Response** 📨
```
2026-03-02 10:30:15.070 INFO  [abc-123] [http-nio-8081-exec-1] e.b.controller.TransactionalController - Exiting TransactionalController.getCustomer with customer name=John Doe
```

**What happened:**
- Controller logged exit with result summary

---

#### **Step 9: Response Leaves Through LoggingFilter** 🚀
```
2026-03-02 10:30:15.075 INFO  [abc-123] [http-nio-8081-exec-1] e.b.filter.LoggingFilter - Outgoing response: method=GET, uri=/api/customers/123, status=200, duration=74ms
```

**What happened:**
- Filter calculated total request duration (74ms)
- Logged successful response
- Cleaned up MDC (removed correlation ID from thread-local storage)

---

### **Complete Flow Visualization**

```
┌─────────────────────────────────────────────────────────────┐
│  Client Request: GET /api/customers/123                     │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
     ┌───────────────────────┐
     │  LoggingFilter        │ ← Generate correlation ID
     │  [abc-123]            │ ← Log incoming request
     └──────────┬────────────┘ ← Start timer
                │
                ▼
     ┌───────────────────────┐
     │  TransactionalController │ ← Log entry
     │  .getCustomer(123)    │
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  TransactionalService │ ← Log "Fetching customer"
     │  .getCustomerById(123)│
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  CustomerRepository   │ ← AOP logs method call
     │  .findById(123)       │
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  Hibernate/JPA        │ ← Log SQL query
     │  SQL Query            │ ← Log parameters
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  SQL Server Database  │
     └──────────┬────────────┘
                │ (Returns: Customer{id=123, name="John Doe"})
                ▼
     ┌───────────────────────┐
     │  CustomerRepository   │ ← AOP logs result + duration
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  TransactionalService │ ← Log "Fetched customer"
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  TransactionalController │ ← Log exit
     └──────────┬────────────┘
                │
                ▼
     ┌───────────────────────┐
     │  LoggingFilter        │ ← Log outgoing response
     │  [abc-123]            │ ← Calculate total duration
     └──────────┬────────────┘ ← Clean up MDC
                │
                ▼
┌────────────────────────────────────────────────────────────┐
│  Client Response: 200 OK                                    │
│  X-Correlation-Id: abc-123                                 │
│  {id: 123, name: "John Doe", ...}                         │
└────────────────────────────────────────────────────────────┘
```

### **Key Observations**

1. **Every log has `[abc-123]`** - You can search for this ID and see the entire request flow
2. **Timestamps show duration** - From 10:30:15.001 to 10:30:15.075 = 74ms total
3. **Different log levels** - INFO for business events, DEBUG for technical details
4. **Automatic logging** - Repository logging via AOP (no manual code!)
5. **Thread name included** - Shows which thread handled the request

---

## Testing Your Implementation

Now let's test what we built. I'll guide you step by step.

### **Step 1: Start Your Application** 🚀

Open a terminal:
```bash
cd "C:\Users\jsasanam\OneDrive - Global Healthcare Exchange\bi-dashboard-api"
./mvnw spring-boot:run
```

**What to look for:**

You should see colorful logs appearing in your console:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

2026-03-02 10:30:00.123 INFO  Initializing SQL Server DataSource: url=jdbc:sqlserver://10.9.182.73:1433
2026-03-02 10:30:00.456 INFO  SQL Server DataSource initialized successfully
2026-03-02 10:30:01.234 INFO  Initializing Snowflake DataSource: url=jdbc:snowflake://ILNUYKT-SH44292.snowflake...
2026-03-02 10:30:01.567 INFO  Snowflake DataSource initialized successfully
2026-03-02 10:30:02.890 INFO  Caching enabled with Spring simple cache
2026-03-02 10:30:02.891 INFO  Configured cache names: analytics, kpis, salesTrends, topProducts, customerSegments, geographicSales
2026-03-02 10:30:03.456 INFO  Started BiDashboardApiApplication in 5.234 seconds
```

✅ **If you see this, your app is running!**

❌ **If you see errors:**
- Check database connections
- Make sure ports are not in use
- Check the error.log file

---

### **Step 2: Check Log Files Were Created** 📁

Open a **second terminal** (keep the first one running):

```bash
cd "C:\Users\jsasanam\OneDrive - Global Healthcare Exchange\bi-dashboard-api"
ls -la logs/
```

**Expected output:**
```
total 32
drwxr-xr-x 1 GHX+jsasanam 4096    0 Mar  2 10:30 .
drwxr-xr-x 1 GHX+jsasanam 4096    0 Mar  2 10:30 ..
-rw-r--r-- 1 GHX+jsasanam 4096 8192 Mar  2 10:30 bi-dashboard-api.log
-rw-r--r-- 1 GHX+jsasanam 4096    0 Mar  2 10:30 bi-dashboard-api-error.log
-rw-r--r-- 1 GHX+jsasanam 4096 4096 Mar  2 10:30 bi-dashboard-api-sql.log
```

✅ **You should see 3 log files!**

**View the main log file:**
```bash
tail -f logs/bi-dashboard-api.log
```

This command means:
- `tail` - Show the end of the file
- `-f` - Keep following (show new lines as they're added)

To stop: Press `Ctrl+C`

---

### **Step 3: Make a Test Request** 🧪

Open a **third terminal** (or use Postman):

```bash
curl -X GET http://localhost:8081/api/dashboard/kpis
```

**Expected response:**
```json
{
  "totalRevenue": 125000.50,
  "totalOrders": 1234,
  "averageOrderValue": 101.30,
  "totalCustomers": 567,
  "totalProducts": 89
}
```

**Now check your first terminal (where app is running):**

You should see new log lines appearing:
```
2026-03-02 10:35:00.123 INFO  [550e8400-e29b-41d4-a716-446655440000] Incoming request: method=GET, uri=/api/dashboard/kpis
2026-03-02 10:35:00.125 INFO  [550e8400-e29b-41d4-a716-446655440000] Entering DashboardController.getKPIs
2026-03-02 10:35:00.130 INFO  [550e8400-e29b-41d4-a716-446655440000] Fetching KPIs from Snowflake
...
2026-03-02 10:35:00.380 INFO  [550e8400-e29b-41d4-a716-446655440000] Outgoing response: status=200, duration=257ms
```

✅ **If you see logs with correlation IDs, it's working!**

---

### **Step 4: Test Correlation ID in Response Headers** 🎫

Make a request and see the headers:

```bash
curl -i http://localhost:8081/api/dashboard/kpis
```

**Expected output:**
```
HTTP/1.1 200 OK
X-Correlation-Id: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 02 Mar 2026 05:05:00 GMT

{
  "totalRevenue": 125000.50,
  ...
}
```

✅ **Look for `X-Correlation-Id` header!**

**Copy that ID for the next step!**

---

### **Step 5: Search Logs by Correlation ID** 🔍

Replace `YOUR-CORRELATION-ID` with the actual ID you copied:

```bash
grep "550e8400-e29b-41d4-a716-446655440000" logs/bi-dashboard-api.log
```

**Expected output - ALL logs for that one request:**
```
2026-03-02 10:35:00.123 INFO  [550e8400-e29b-41d4-a716-446655440000] Incoming request: method=GET, uri=/api/dashboard/kpis, correlationId=550e8400-e29b-41d4-a716-446655440000
2026-03-02 10:35:00.125 INFO  [550e8400-e29b-41d4-a716-446655440000] Entering DashboardController.getKPIs
2026-03-02 10:35:00.130 INFO  [550e8400-e29b-41d4-a716-446655440000] Fetching KPIs from Snowflake
2026-03-02 10:35:00.375 INFO  [550e8400-e29b-41d4-a716-446655440000] Fetched KPIs in 245ms - revenue=125000.5, orders=1234, customers=567
2026-03-02 10:35:00.377 INFO  [550e8400-e29b-41d4-a716-446655440000] Exiting DashboardController.getKPIs with KPI data
2026-03-02 10:35:00.380 INFO  [550e8400-e29b-41d4-a716-446655440000] Outgoing response: method=GET, uri=/api/dashboard/kpis, status=200, duration=257ms
```

✅ **This is the power of correlation IDs - you can trace the entire request!**

---

### **Step 6: Test Error Logging** ⚠️

Trigger an error to test error.log:

```bash
curl http://localhost:8081/api/test-exceptions/not-found
```

**Expected response:**
```json
{
  "timestamp": "2026-03-02 10:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Customer with id 999 not found",
  "path": "/api/test-exceptions/not-found",
  "correlationId": "xyz-789-abc-123"
}
```

**Check the error log:**
```bash
tail logs/bi-dashboard-api-error.log
```

**Expected output:**
```
2026-03-02 10:40:00.123 ERROR [xyz-789-abc-123] ResourceNotFoundException [correlationId=xyz-789-abc-123]: Customer with id 999 not found
exercise.bidashboardapi.exception.ResourceNotFoundException: Customer not found with id: 999
    at exercise.bidashboardapi.controller.ExceptionTestController.testNotFound(ExceptionTestController.java:19)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
    ... (full stack trace)
```

✅ **Notice:**
- Error appears in error.log
- Includes correlation ID
- Full stack trace for debugging

---

### **Step 7: Check SQL Logs** 🗄️

Make a request that hits the database:

```bash
curl http://localhost:8081/api/customers/1
```

**Check the SQL log:**
```bash
tail logs/bi-dashboard-api-sql.log
```

**Expected output:**
```
2026-03-02 10:45:00.123 DEBUG [abc-123] Hibernate:
    SELECT
        customer_id,
        name,
        email,
        phone,
        address,
        city,
        country
    FROM customers
    WHERE customer_id = ?

2026-03-02 10:45:00.125 TRACE [abc-123] binding parameter [1] as INTEGER - 1
```

✅ **You can see:**
- Formatted SQL queries
- Parameter values
- Same correlation ID

---

### **Step 8: Use Your Own Correlation ID** 🎯

You can provide your own correlation ID:

```bash
curl -H "X-Correlation-Id: MY-TEST-12345" http://localhost:8081/api/customers/1
```

**Search for your custom ID:**
```bash
grep "MY-TEST-12345" logs/bi-dashboard-api.log
```

**Output:**
```
2026-03-02 10:50:00.123 INFO  [MY-TEST-12345] Incoming request: method=GET, uri=/api/customers/1, correlationId=MY-TEST-12345
2026-03-02 10:50:00.125 INFO  [MY-TEST-12345] Entering TransactionalController.getCustomer with customerId=1
...
2026-03-02 10:50:00.200 INFO  [MY-TEST-12345] Outgoing response: status=200, duration=77ms
```

✅ **All logs use YOUR correlation ID instead of a generated one!**

This is useful when:
- Integrating with other systems that already have request IDs
- Debugging specific issues
- Tracing requests across multiple services

---

### **Step 9: Test Log Rotation** 🔄

Log rotation happens automatically when files reach 10MB. To test it manually, you can generate lots of logs:

**Option 1: Make many requests**
```bash
for i in {1..1000}; do
  curl http://localhost:8081/api/dashboard/kpis > /dev/null 2>&1
  echo "Request $i completed"
done
```

**Option 2: Check existing archives**
```bash
ls -la logs/archive/
```

After some time (or after 10MB is reached), you'll see:
```
bi-dashboard-api-2026-03-02.0.log.gz  ← Compressed old log
bi-dashboard-api-2026-03-02.1.log.gz  ← Another old log
```

---

### **Step 10: Monitor Logs in Real-Time** 👀

Open multiple terminals side by side:

**Terminal 1 - Application logs:**
```bash
tail -f logs/bi-dashboard-api.log
```

**Terminal 2 - Error logs:**
```bash
tail -f logs/bi-dashboard-api-error.log
```

**Terminal 3 - SQL logs:**
```bash
tail -f logs/bi-dashboard-api-sql.log
```

**Terminal 4 - Make requests:**
```bash
curl http://localhost:8081/api/customers/1
curl http://localhost:8081/api/test-exceptions/internal-error
```

Watch the logs appear in real-time in different terminals!

---

## Learning Outcomes

After this implementation, you've learned:

### **1. SLF4J API** - The Logging Interface

**What is SLF4J?**
Simple Logging Facade for Java - it's an interface (contract) for logging.

**Benefits:**
- Change logging implementation without changing code
- Same API works with Logback, Log4j, JUL (Java Util Logging)

**Using Lombok @Slf4j:**
```java
@Slf4j  // Lombok generates a 'log' object for you
public class MyController {

    public void doSomething() {
        log.info("This is an info message");
        log.debug("This is a debug message");
        log.error("This is an error message");
    }
}
```

**Without Lombok:**
```java
public class MyController {
    private static final Logger log = LoggerFactory.getLogger(MyController.class);

    public void doSomething() {
        log.info("This is an info message");
    }
}
```

**Parameterized Logging (Important!):**

✅ **GOOD - Parameterized (lazy evaluation):**
```java
log.info("Processing order {} for customer {}", orderId, customerId);
```
- String formatting only happens if log level is enabled
- Better performance
- No string concatenation overhead

❌ **BAD - String concatenation:**
```java
log.info("Processing order " + orderId + " for customer " + customerId);
```
- String concatenation happens ALWAYS, even if logging is disabled
- Wastes CPU and memory

---

### **2. Logback Configuration**

You learned how to configure:

**Multiple Appenders:**
- Console (for development)
- File (for production)
- Async (for performance)

**Rolling Policies:**
- Size-based: Rotate when file reaches 10MB
- Time-based: Rotate daily
- Combined: Both size and time

**Log Levels Per Package:**
```xml
<logger name="exercise.bidashboardapi.controller" level="INFO"/>
<logger name="org.hibernate.SQL" level="DEBUG"/>
```

**Async Logging:**
```xml
<appender name="ASYNC_FILE">
    <queueSize>512</queueSize>
    <appender-ref ref="FILE"/>
</appender>
```

---

### **3. MDC (Mapped Diagnostic Context)**

**What is MDC?**
A map stored in thread-local storage that automatically adds data to all log statements in that thread.

**How it works:**

```java
// Store correlation ID in MDC
MDC.put("correlationId", "abc-123");

// All subsequent logs in this thread include it automatically
log.info("Processing request");
// Output: [abc-123] Processing request

log.info("Fetching data");
// Output: [abc-123] Fetching data

// Clean up when done
MDC.clear();
```

**Logback Pattern:**
```xml
<pattern>%d [%X{correlationId}] %msg%n</pattern>
```
`%X{correlationId}` extracts the value from MDC.

**Important:** Always call `MDC.clear()` in a `finally` block to prevent memory leaks!

---

### **4. AOP (Aspect-Oriented Programming)**

**What is AOP?**
A programming paradigm for adding behavior (like logging) to existing code without modifying it.

**Key Concepts:**

**Aspect:** The cross-cutting concern (logging)
```java
@Aspect
@Component
public class RepositoryLoggingAspect { ... }
```

**Pointcut:** Where to apply the aspect
```java
@Around("execution(* exercise.bidashboardapi.repository.*.*(..))")
```
Translation: "Intercept all methods in all repository classes"

**Advice:** What to do (before, after, around)
```java
public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) {
    // Before: log method entry
    Object result = joinPoint.proceed();  // Execute actual method
    // After: log method exit
    return result;
}
```

**Benefits:**
- No code duplication
- Consistent logging everywhere
- Easy to modify (one place)

---

### **5. 12-Factor App Logging Principles**

**What is 12-Factor App?**
A methodology for building modern, cloud-native applications.

**Logging Principle (Factor XI):**
"Treat logs as event streams"

**Key Points:**

1. **Apps write to stdout/stderr**
   - Don't manage log files in application code
   - Let the environment handle log collection

2. **Logs are unbuffered streams**
   - Write immediately, don't batch
   - Each log line is an event

3. **No log rotation in app code**
   - Environment (Kubernetes, Docker, systemd) handles rotation
   - Application just writes to stdout

4. **Structured logging**
   - Consistent format
   - Machine-parseable (JSON in production)
   - Includes context (correlation IDs)

**Our Implementation:**
- ✅ Logs go to console (stdout)
- ✅ Logs also go to files (for learning/development)
- ✅ Structured format with timestamps, levels, correlation IDs
- ✅ Async to avoid blocking

---

### **6. Sensitive Data Protection**

**Never log these:**

❌ **Passwords:**
```java
log.info("User password: {}", password);  // NEVER!
```

❌ **API Keys:**
```java
log.info("API Key: {}", apiKey);  // NEVER!
```

❌ **Credit Card Numbers:**
```java
log.info("Card: {}", cardNumber);  // NEVER!
```

❌ **Social Security Numbers:**
```java
log.info("SSN: {}", ssn);  // NEVER!
```

❌ **Personal Identifiable Information (PII) without consent:**
```java
log.info("Email: {}", email);  // Be careful!
```

**Always mask or exclude sensitive data:**

✅ **Use sanitization:**
```java
log.info("User password: {}", LoggingUtil.sanitize(password));
// Logs: User password: Pa****rd
```

✅ **Check field names:**
```java
if (LoggingUtil.isSensitiveField("password")) {
    log.info("Field {}: {}", "password", "****");
} else {
    log.info("Field {}: {}", fieldName, value);
}
```

---

## Practice Exercises

Now that you understand logging, try these exercises to reinforce your learning:

### **Exercise 1: Follow a Request End-to-End** 🔍

**Task:** Trace a complete request through all layers.

**Steps:**
1. Make a request to create an order:
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "order": {
      "customerId": 1,
      "orderDate": "2026-03-02T10:30:00",
      "totalAmount": 150.00,
      "status": "PENDING"
    },
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "price": 75.00
      }
    ]
  }'
```

2. Copy the `X-Correlation-Id` from the response

3. Search logs for that correlation ID:
```bash
grep "YOUR-CORRELATION-ID" logs/bi-dashboard-api.log
```

4. **Answer these questions:**
   - How many log lines were generated?
   - How long did the request take (check duration)?
   - Which repositories were called?
   - Were there any database queries? (check sql.log)

**Expected Answer:**
- ~15-20 log lines
- Duration: ~200-500ms
- Repositories: OrderRepository, OrderItemRepository, ProductRepository, CustomerRepository
- Multiple SQL queries (INSERT, SELECT, UPDATE)

---

### **Exercise 2: Trigger All Exception Types** ⚠️

**Task:** Test each exception handler and verify error logging.

**Steps:**

1. **ResourceNotFoundException (404):**
```bash
curl http://localhost:8081/api/test-exceptions/not-found
```

2. **BadRequestException (400):**
```bash
curl http://localhost:8081/api/test-exceptions/bad-request
```

3. **ConflictException (409):**
```bash
curl http://localhost:8081/api/test-exceptions/conflict
```

4. **InternalServerException (500):**
```bash
curl http://localhost:8081/api/test-exceptions/internal-error
```

5. **Generic Exception (500):**
```bash
curl http://localhost:8081/api/test-exceptions/generic-exception
```

6. Check error.log:
```bash
tail -50 logs/bi-dashboard-api-error.log
```

**Questions:**
- Do all exceptions appear in error.log?
- Does each error have a correlation ID?
- Do stack traces show the exact line where error occurred?

---

### **Exercise 3: Measure Performance with Caching** ⚡

**Task:** Compare cached vs non-cached request performance.

**Steps:**

1. **First request (cache miss - slow):**
```bash
time curl http://localhost:8081/api/dashboard/kpis
```
Note the time!

2. **Second request (cache hit - fast):**
```bash
time curl http://localhost:8081/api/dashboard/kpis
```
Note the time!

3. **Search logs for execution times:**
```bash
grep "Fetched KPIs in" logs/bi-dashboard-api.log | tail -2
```

**Expected Results:**
- First request: ~200-500ms (queries Snowflake database)
- Second request: ~10-50ms (returns from cache)

**Questions:**
- How much faster was the cached request?
- Can you see the cache hit in the logs?

---

### **Exercise 4: Add Your Own Logging** ✍️

**Task:** Add custom logging to an existing method.

**Steps:**

1. Open any service file, for example `TransactionalService.java`

2. Find the `updateStock()` method

3. Add additional logging:
```java
@Transactional
public Product updateStock(Integer productId, Integer newStock) {
    log.info("Updating stock for productId={} from current to new={}", productId, newStock);

    if (productId <= 0) {
        throw new BadRequestException("Product ID must be a positive number");
    }

    if (newStock < 0) {
        throw new BadRequestException("Stock cannot be negative");
    }

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

    Integer oldStock = product.getStock();

    // ADD THIS LINE:
    log.debug("Current stock for product {}: {}, changing to: {}",
              product.getName(), oldStock, newStock);

    product.setStock(newStock);
    Product updatedProduct = productRepository.save(product);

    log.info("Stock updated for productId={}, previousStock={}, newStock={}",
            productId, oldStock, updatedProduct.getStock());

    // ADD THIS LINE:
    log.debug("Stock update committed to database successfully");

    return updatedProduct;
}
```

4. Restart the application

5. Make a request:
```bash
curl -X PUT "http://localhost:8081/api/products/1/stock?stock=50"
```

6. Check logs for your custom messages:
```bash
grep "Stock update committed" logs/bi-dashboard-api.log
```

**Success:** You should see your custom log message!

---

### **Exercise 5: Test Log Rotation** 🔄

**Task:** Trigger log rotation by generating lots of logs.

**Steps:**

1. Check current log file size:
```bash
ls -lh logs/bi-dashboard-api.log
```

2. Generate 1000 requests:
```bash
for i in {1..1000}; do
  curl -s http://localhost:8081/api/dashboard/kpis > /dev/null
  if [ $((i % 100)) -eq 0 ]; then
    echo "Completed $i requests"
  fi
done
```

3. Check log file size again:
```bash
ls -lh logs/bi-dashboard-api.log
```

4. If file exceeds 10MB, check archive folder:
```bash
ls -lh logs/archive/
```

**Expected Results:**
- Original file moved to archive
- New file created
- Archive file compressed (.gz)

---

### **Exercise 6: Search for Slow Operations** 🐌

**Task:** Find all operations that took longer than 1 second.

**Steps:**

1. Search logs for slow operations:
```bash
grep -E "duration=[0-9]{4,}ms" logs/bi-dashboard-api.log
```

This regex finds durations with 4+ digits (≥1000ms)

2. Count how many slow operations occurred:
```bash
grep -E "duration=[0-9]{4,}ms" logs/bi-dashboard-api.log | wc -l
```

3. Find the slowest operation:
```bash
grep -E "duration=[0-9]{4,}ms" logs/bi-dashboard-api.log | sort -t= -k2 -n | tail -1
```

**Questions:**
- Which endpoint was slowest?
- What was the duration?
- What can you do to improve it?

---

### **Exercise 7: Test MDC with Multiple Threads** 🧵

**Task:** Make concurrent requests and verify correlation IDs don't mix.

**Steps:**

1. Make 10 concurrent requests:
```bash
for i in {1..10}; do
  curl http://localhost:8081/api/customers/$i &
done
wait
```

The `&` runs each request in the background.

2. Check logs:
```bash
tail -100 logs/bi-dashboard-api.log
```

**Verify:**
- Each request has a different correlation ID
- Logs from different requests are interleaved
- But each correlation ID is consistent throughout its request

**Example:**
```
[abc-123] Incoming request: GET /api/customers/1
[def-456] Incoming request: GET /api/customers/2
[abc-123] Entering TransactionalController.getCustomer
[def-456] Entering TransactionalController.getCustomer
[abc-123] Fetching customer with customerId=1
[def-456] Fetching customer with customerId=2
[abc-123] Outgoing response: status=200
[def-456] Outgoing response: status=200
```

Notice how logs are interleaved, but correlation IDs stay consistent!

---

## Quick Reference

### **Common Log Statements**

```java
// INFO - Normal business operations
log.info("Processing order for customer: {}", customerId);
log.info("Payment completed successfully, amount: ${}", amount);
log.info("Email sent to: {}", email);

// DEBUG - Detailed debugging information
log.debug("Checking inventory for product: {}", productId);
log.debug("Current cache size: {}", cacheSize);
log.debug("Validating customer data: {}", customer);

// WARN - Potentially problematic situations
log.warn("Cache miss for key: {}, fetching from database", cacheKey);
log.warn("Response time exceeded threshold: {}ms (max: 1000ms)", duration);
log.warn("Using deprecated method: {}", methodName);

// ERROR - Error conditions (always include exception)
try {
    processOrder(orderId);
} catch (PaymentException e) {
    log.error("Payment failed for order {}", orderId, e);
    throw e;
}

// Measuring execution time
long startTime = System.currentTimeMillis();
performOperation();
long duration = System.currentTimeMillis() - startTime;
log.info("Operation completed in {}ms", duration);
```

---

### **Log Search Commands**

```bash
# View logs in real-time (follow mode)
tail -f logs/bi-dashboard-api.log

# View last 100 lines
tail -100 logs/bi-dashboard-api.log

# View first 100 lines
head -100 logs/bi-dashboard-api.log

# Search for specific correlation ID
grep "abc-123-def-456" logs/bi-dashboard-api.log

# Search for all ERROR level logs
grep "ERROR" logs/bi-dashboard-api.log

# Count number of errors
grep "ERROR" logs/bi-dashboard-api.log | wc -l

# Find errors from specific hour
grep "2026-03-02 10:" logs/bi-dashboard-api-error.log

# Find slow operations (over 1000ms)
grep -E "duration=[0-9]{4,}ms" logs/bi-dashboard-api.log

# Find all logs for a specific customer
grep "customerId=123" logs/bi-dashboard-api.log

# Search across all log files
grep -r "PaymentException" logs/

# View logs with line numbers
grep -n "ERROR" logs/bi-dashboard-api.log

# Case-insensitive search
grep -i "exception" logs/bi-dashboard-api.log

# Search for multiple patterns
grep -E "ERROR|WARN" logs/bi-dashboard-api.log

# Show 5 lines before and after match (context)
grep -C 5 "PaymentException" logs/bi-dashboard-api.log
```

---

### **Correlation ID Commands**

```bash
# Make request with custom correlation ID
curl -H "X-Correlation-Id: MY-REQUEST-123" http://localhost:8081/api/customers/1

# Extract correlation ID from response
curl -i http://localhost:8081/api/customers/1 | grep "X-Correlation-Id"

# Search logs by correlation ID
grep "550e8400-e29b-41d4-a716-446655440000" logs/bi-dashboard-api.log

# Count requests with unique correlation IDs
grep "Incoming request" logs/bi-dashboard-api.log | wc -l

# Extract all correlation IDs from today
grep "$(date '+%Y-%m-%d')" logs/bi-dashboard-api.log | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | sort | uniq
```

---

### **Log File Management**

```bash
# Check log file sizes
ls -lh logs/

# Check total disk usage
du -sh logs/

# View compressed archive logs
zcat logs/archive/bi-dashboard-api-2026-03-01.0.log.gz | head -100

# Search in compressed logs
zgrep "ERROR" logs/archive/bi-dashboard-api-2026-03-01.0.log.gz

# Delete old logs manually (be careful!)
rm logs/archive/bi-dashboard-api-2026-02-*.log.gz

# Monitor log file growth
watch -n 1 'ls -lh logs/bi-dashboard-api.log'
```

---

### **Performance Analysis**

```bash
# Find average response time
grep "duration=" logs/bi-dashboard-api.log | grep -oE "duration=[0-9]+ms" | grep -oE "[0-9]+" | awk '{sum+=$1; count++} END {print "Average: " sum/count "ms"}'

# Find max response time
grep "duration=" logs/bi-dashboard-api.log | grep -oE "duration=[0-9]+ms" | grep -oE "[0-9]+" | sort -n | tail -1

# Count requests per endpoint
grep "Incoming request" logs/bi-dashboard-api.log | grep -oE "uri=[^ ]+" | sort | uniq -c | sort -nr

# Find most common errors
grep "ERROR" logs/bi-dashboard-api-error.log | grep -oE "[A-Za-z]+Exception" | sort | uniq -c | sort -nr
```

---

## Common Questions

### **Q: Why do we need correlation IDs?**

**A:** In a busy system with hundreds of requests per second, logs from different requests get mixed together:

```
10:30:01 - Received order
10:30:02 - Received order       ← Which customer?
10:30:03 - Processing payment   ← For which order?
10:30:04 - Order completed      ← Which one?
```

With correlation IDs, you can instantly filter logs for ONE specific request:
```bash
grep "abc-123" logs/*.log
```

This is essential for:
- **Debugging production issues** - "What happened to customer 456's order?"
- **Performance analysis** - "How long did this specific request take?"
- **Tracing across services** - In microservices, correlation IDs are passed between services

---

### **Q: What's the difference between console logs and file logs?**

**A:**

**Console Logs (stdout/stderr):**
- ✅ Appear in your terminal while app is running
- ✅ Immediately visible
- ❌ Disappear when you restart the app
- ❌ Can't search history
- **Use case:** Development, debugging

**File Logs:**
- ✅ Permanent - stored on disk
- ✅ Can search historical data
- ✅ Survive application restarts
- ❌ Need to open file to view
- **Use case:** Production, long-term analysis

**Best Practice:** Log to BOTH (which we do!)

---

### **Q: Why separate error.log and application.log?**

**A:**

**Scenario:** Your application has been running for a week. Something breaks at 3 AM. You need to find the error.

**Without separation:**
```bash
# Search through 10 million lines
grep "ERROR" logs/application.log  # Takes 5 minutes!
```

**With separation:**
```bash
# Only error logs - maybe 1000 lines
tail logs/error.log  # Instant!
```

**Benefits:**
- **Faster troubleshooting** - Only look at errors
- **Alerting** - Monitor error.log for new entries
- **Different retention** - Keep errors for 90 days, regular logs for 30 days

---

### **Q: What if logs fill up my disk?**

**A:** That's why we configured **log rotation**!

**Without rotation:**
```
Day 1: application.log (100MB)
Day 2: application.log (200MB)
Day 3: application.log (300MB)
...
Day 30: application.log (3GB)
Day 365: application.log (36GB) ← Disk full! 💥
```

**With rotation (our configuration):**
```xml
<maxFileSize>10MB</maxFileSize>
<maxHistory>30</maxHistory>
<totalSizeCap>1GB</totalSizeCap>
```

**Result:**
- Current file: ~10MB
- Archives (30 days): ~300MB
- Total: Never exceeds 1GB

Old logs are automatically:
1. Compressed (gzip) - saves 90% space
2. Moved to archive folder
3. Deleted after 30 days

---

### **Q: Should I log inside loops?**

**A:** Generally **NO!**

**❌ BAD - Logging in loop:**
```java
for (Product product : products) {
    log.info("Processing product: {}", product.getId());
    process(product);
}
```

If you have 10,000 products, you'll generate 10,000 log lines!

**✅ GOOD - Log summary:**
```java
long startTime = System.currentTimeMillis();
int successCount = 0;
int errorCount = 0;

for (Product product : products) {
    try {
        process(product);
        successCount++;
    } catch (Exception e) {
        errorCount++;
        log.error("Failed to process product: {}", product.getId(), e);
    }
}

long duration = System.currentTimeMillis() - startTime;
log.info("Processed {} products in {}ms - success: {}, errors: {}",
         products.size(), duration, successCount, errorCount);
```

**Exception:** Log errors in loops (but not every iteration)

---

### **Q: How do I log collections without printing 1000 items?**

**A:** Log the SIZE, not the contents.

**❌ BAD:**
```java
List<Customer> customers = customerRepository.findAll();
log.info("Customers: {}", customers);
```
This logs: `Customers: [Customer{id=1, name=John}, Customer{id=2, name=Jane}, ... 1000 more]`

**✅ GOOD:**
```java
List<Customer> customers = customerRepository.findAll();
log.info("Retrieved {} customers", customers.size());
```
This logs: `Retrieved 1000 customers`

**Even BETTER (with details):**
```java
log.info("Retrieved {} customers from database in {}ms", customers.size(), duration);
```

---

### **Q: When should I use DEBUG vs INFO?**

**A:**

**INFO:** Things a **business person** might care about
```java
log.info("Order placed: orderId={}, customer={}, amount=${}", orderId, customerName, amount);
log.info("Payment completed successfully");
log.info("Email sent to customer");
```

**DEBUG:** Things only a **developer** cares about
```java
log.debug("Checking cache for key: {}", cacheKey);
log.debug("Executing SQL query: {}", sql);
log.debug("Validating request parameters: {}", params);
```

**Rule of thumb:**
- **INFO:** Events that matter to the business
- **DEBUG:** Technical details for troubleshooting

---

### **Q: How do I log JSON objects?**

**A:**

**❌ BAD - toString():**
```java
Customer customer = getCustomer();
log.info("Customer: {}", customer);
// Logs: Customer: Customer@7b23ec81 (not useful!)
```

**✅ GOOD - Selective fields:**
```java
log.info("Customer: id={}, name={}, email={}",
         customer.getId(), customer.getName(), customer.getEmail());
```

**✅ BETTER - For debugging (use DEBUG level):**
```java
log.debug("Customer details: {}", objectMapper.writeValueAsString(customer));
// Logs: Customer details: {"id":123,"name":"John Doe","email":"john@example.com"}
```

---

### **Q: What's the performance impact of logging?**

**A:**

**Synchronous logging:** ~1-10ms per log statement
**Async logging (what we use):** ~0.1-1ms per log statement

**Example:**
```java
// 100 log statements
// Synchronous: 100 * 5ms = 500ms delay
// Async: 100 * 0.5ms = 50ms delay
```

**Why async is faster:**
- Logs added to queue (fast)
- Background thread writes to disk (slow, but doesn't block)

**Our configuration:**
```xml
<appender name="ASYNC_FILE">
    <queueSize>512</queueSize>  ← Can buffer 512 messages
</appender>
```

---

### **Q: How do I log exceptions properly?**

**A:** Always include the exception object as the LAST parameter:

**✅ CORRECT:**
```java
try {
    processOrder(orderId);
} catch (PaymentException e) {
    log.error("Payment failed for order {}", orderId, e);
    throw e;
}
```

The `e` at the end tells SLF4J to print the full stack trace.

**❌ WRONG:**
```java
catch (PaymentException e) {
    log.error("Payment failed: {}", e.getMessage());  // No stack trace!
}
```

**❌ WRONG:**
```java
catch (PaymentException e) {
    log.error("Payment failed: " + e);  // String concatenation (slow)
}
```

---

### **Q: Should I log before or after an operation?**

**A:** **Both!** But at different levels.

**Before (DEBUG):** "About to do something"
```java
log.debug("Executing database query for customer: {}", customerId);
Customer customer = repository.findById(customerId);
```

**After (INFO):** "Successfully did something"
```java
log.info("Customer fetched: id={}, name={}", customer.getId(), customer.getName());
```

**Pattern for long operations:**
```java
long startTime = System.currentTimeMillis();
log.info("Starting batch processing of {} items", items.size());

processBatch(items);

long duration = System.currentTimeMillis() - startTime;
log.info("Batch processing completed in {}ms", duration);
```

---

### **Q: How do I test logging in unit tests?**

**A:** Use a logging appender that captures logs.

**Example with Logback:**
```java
@Test
public void testLogging() {
    // Create a list appender
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();

    // Attach to logger
    Logger logger = (Logger) LoggerFactory.getLogger(TransactionalService.class);
    logger.addAppender(listAppender);

    // Execute code that logs
    service.getCustomerById(123);

    // Verify log was written
    List<ILoggingEvent> logsList = listAppender.list;
    assertEquals("Fetching customer with customerId=123", logsList.get(0).getMessage());
}
```

---

## Conclusion

Congratulations! 🎉 You've successfully implemented enterprise-grade logging in your Spring Boot application.

### **What You've Accomplished:**

✅ Created 3 separate log files (application, error, SQL)
✅ Implemented correlation ID tracking for request tracing
✅ Added logging to all layers (controllers, services, repositories)
✅ Configured log rotation and retention
✅ Implemented sensitive data protection
✅ Used AOP for automatic repository logging
✅ Configured async logging for performance
✅ Set up proper log levels per package

### **Next Steps:**

1. **Experiment:** Try different log levels, make requests, observe output
2. **Read Documentation:**
   - [Logback Documentation](http://logback.qos.ch/manual/)
   - [SLF4J User Manual](http://www.slf4j.org/manual.html)
   - [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

3. **Advance to Day 2:** Exception handling, validation, and API documentation

### **Resources Created:**

- `LOGGING_GUIDE.md` - This comprehensive guide
- `logback-spring.xml` - Logging configuration
- `LoggingFilter.java` - Correlation ID filter
- `LoggingUtil.java` - Utility methods
- `RepositoryLoggingAspect.java` - AOP logging

### **Remember:**

> "Logs are the black box of your application. When things go wrong (and they will), logs are your best friend for understanding what happened."

Happy logging! 🚀
