# InsurePro Backend — Implementation & Connection Guide

---

## Project Structure

```
insurepro-backend/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/insurepro/
│       │   ├── InsureProApplication.java              ← Entry Point
│       │   │
│       │   ├── entity/
│       │   │   ├── enums/
│       │   │   │   ├── UserRole.java
│       │   │   │   ├── KYCStatus.java
│       │   │   │   ├── PolicyType.java
│       │   │   │   ├── PolicyStatus.java
│       │   │   │   ├── ClaimType.java
│       │   │   │   ├── ClaimStatus.java
│       │   │   │   ├── SettlementStatus.java
│       │   │   │   ├── InvoiceStatus.java
│       │   │   │   ├── PaymentMethod.java
│       │   │   │   ├── PaymentStatus.java
│       │   │   │   └── NotificationCategory.java
│       │   │   ├── User.java
│       │   │   ├── AuditLog.java
│       │   │   ├── Customer.java
│       │   │   ├── Policy.java
│       │   │   ├── Claim.java
│       │   │   ├── Settlement.java
│       │   │   ├── Invoice.java
│       │   │   ├── Payment.java
│       │   │   ├── ComplianceReport.java
│       │   │   ├── KPIReport.java
│       │   │   └── Notification.java
│       │   │
│       │   ├── repository/           ← JPA Repositories (one per entity)
│       │   ├── service/              ← Business logic services
│       │   ├── controller/           ← REST API controllers
│       │   ├── dto/                  ← Request/Response DTOs
│       │   ├── security/             ← JWT provider + filters
│       │   ├── config/               ← SecurityConfig, OpenAPI config
│       │   └── exception/            ← Custom exceptions + global handler
│       │
│       └── resources/
│           ├── application.properties   ← Main config
│           └── schema.sql               ← Manual DB init script
```

---

## STEP 1 — Prerequisites

Install these tools before starting:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17 or 21 | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org |
| PostgreSQL | 15+ (recommended) | https://www.postgresql.org/download |
| IntelliJ IDEA | Latest | https://www.jetbrains.com/idea |
| Postman | Latest | For API testing |

---

## STEP 2 — Database Setup

### Option A: PostgreSQL (Recommended)

```bash
# 1. Start PostgreSQL service
# macOS
brew services start postgresql

# Ubuntu/Debian
sudo systemctl start postgresql

# Windows — start from Services or pgAdmin

# 2. Log into psql
psql -U postgres

# 3. Run these SQL commands:
CREATE DATABASE insurepro_db;
CREATE USER insurepro_user WITH ENCRYPTED PASSWORD 'insurepro_pass';
GRANT ALL PRIVILEGES ON DATABASE insurepro_db TO insurepro_user;
\q

# 4. Initialize schema (optional — Hibernate auto-creates on first run)
psql -U insurepro_user -d insurepro_db -f src/main/resources/schema.sql
```

### Option B: MySQL

```bash
# 1. Log into MySQL
mysql -u root -p

# 2. Run:
CREATE DATABASE insurepro_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'insurepro_user'@'localhost' IDENTIFIED BY 'insurepro_pass';
GRANT ALL PRIVILEGES ON insurepro_db.* TO 'insurepro_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Then update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/insurepro_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=insurepro_user
spring.datasource.password=insurepro_pass
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

---

## STEP 3 — Project Setup in IntelliJ IDEA

```
1. Open IntelliJ IDEA → File → New → Project from Existing Sources
2. Select the pom.xml file — choose "Import as Maven Project"
3. Wait for Maven to download all dependencies (~2-3 minutes first time)
4. Verify: Maven tab → insurepro-backend → Lifecycle → compile (should run without errors)
```

### Manual file creation order (paste each file's code into its path):

```
Step A:  Create all enum files in entity/enums/
Step B:  Create all entity files in entity/
Step C:  Create all repository interfaces in repository/
Step D:  Create exception classes in exception/
Step E:  Create security files in security/
Step F:  Create SecurityConfig.java in config/
Step G:  Create all DTO classes in dto/
Step H:  Create all service files in service/
Step I:  Create all controller files in controller/
Step J:  Verify InsureProApplication.java exists
```

---

## STEP 4 — Configure application.properties

Open `src/main/resources/application.properties` and update:

```properties
# ── Database ──────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5432/insurepro_db
spring.datasource.username=insurepro_user
spring.datasource.password=insurepro_pass

# ── First Run: use 'create' to auto-generate tables
spring.jpa.hibernate.ddl-auto=create
# After first run, change to 'update' to preserve data:
# spring.jpa.hibernate.ddl-auto=update

# ── JWT Secret (change this in production!) ───────
jwt.secret=InsureProSecretKey2024SuperSecureAndLongEnoughFor256Bits!!
jwt.expiration.ms=86400000

# ── Email (for notifications) ─────────────────────
spring.mail.username=your-gmail@gmail.com
spring.mail.password=your-gmail-app-password
# Gmail App Password: myaccount.google.com → Security → App passwords
```

---

## STEP 5 — Build & Run

```bash
# Navigate to project root (where pom.xml is)
cd insurepro-backend

# Clean build
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run

# OR run the JAR directly
java -jar target/insurepro-backend-1.0.0.jar
```

**Expected console output on success:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
 Started InsureProApplication in 4.321 seconds
 Tomcat started on port(s): 8080 (http)
```

---

## STEP 6 — Verify the API is Running

Open your browser or Postman:

| URL | Purpose |
|-----|---------|
| http://localhost:8080/api/v1/actuator/health | Health check (should return `{"status":"UP"}`) |
| http://localhost:8080/api/v1/swagger-ui.html | Swagger UI — all endpoints |
| http://localhost:8080/api/v1/api-docs | Raw OpenAPI JSON |

---

## STEP 7 — API Testing with Postman

### 7.1 Register a User (Admin)
```
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "name": "John Agent",
  "email": "john@insurepro.com",
  "password": "Pass@1234",
  "phone": "9876543210",
  "role": "AGENT"
}
```

### 7.2 Login and get JWT Token
```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@insurepro.com",
  "password": "Admin@123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "admin@insurepro.com",
  "role": "ADMIN"
}
```

### 7.3 Use Token for All Further Requests
```
Add to every request header:
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 7.4 Sample API Workflow

```
# 1. Register customer
POST /api/v1/customers
{ "name": "Priya Sharma", "dob": "1990-05-15",
  "email": "priya@email.com", "phone": "9123456789" }

# 2. Update KYC
PATCH /api/v1/customers/1/kyc?status=VERIFIED

# 3. Create policy
POST /api/v1/policies
{ "customerId": 1, "policyType": "HEALTH",
  "coverageAmount": 500000, "startDate": "2025-01-01", "endDate": "2026-01-01" }

# 4. Activate policy (as Underwriter)
PATCH /api/v1/policies/1/activate?underwriterId=2

# 5. Generate invoice
POST /api/v1/payments/invoices/generate/1

# 6. Pay invoice
POST /api/v1/payments/invoices/1/pay
{ "paymentMethod": "UPI", "gatewayName": "Razorpay" }

# 7. Submit claim
POST /api/v1/claims  (multipart/form-data)
claim: { "policyId": 1, "claimType": "HEALTH",
         "amountRequested": 50000, "description": "Hospitalization" }

# 8. Approve claim (as Adjuster)
PATCH /api/v1/claims/1/approve?adjusterId=3

# 9. Create settlement
POST /api/v1/claims/1/settlement?amountApproved=45000&adjusterId=3

# 10. Dashboard KPIs
GET /api/v1/analytics/dashboard
```

---

## STEP 8 — Role-Based Access Reference

| Endpoint Group | ADMIN | AGENT/BROKER | POLICYHOLDER | CLAIMS_ADJUSTER | UNDERWRITER | COMPLIANCE_OFFICER |
|---|---|---|---|---|---|---|
| Auth (login/register) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Customers (CRUD) | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| Policies (create/renew) | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Policy Activate | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Claims (submit) | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Claims (approve/reject) | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Payments | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Analytics/KPIs | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Compliance Reports | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Audit Logs | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## STEP 9 — Connect Frontend (Angular/React)

### React (Axios)
```javascript
// src/api/axiosInstance.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
});

// Attach JWT token from localStorage automatically
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;

// Usage example (login)
const response = await api.post('/auth/login', { email, password });
localStorage.setItem('token', response.data.token);
```

### Angular (HttpClient)
```typescript
// src/app/services/auth.service.ts
import { HttpClient, HttpHeaders } from '@angular/common/http';

const BASE_URL = 'http://localhost:8080/api/v1';

login(email: string, password: string) {
  return this.http.post(`${BASE_URL}/auth/login`, { email, password });
}

// Interceptor to add Bearer token (auth.interceptor.ts)
intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
  const token = localStorage.getItem('token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next.handle(req);
}
```

---

## STEP 10 — CORS Configuration

Add this bean to `SecurityConfig.java` or a separate `CorsConfig.java`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",    // React dev server
        "http://localhost:4200",    // Angular dev server
        "https://yourproductiondomain.com"
    ));
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Then add `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` inside `securityFilterChain()`.

---

## STEP 11 — Environment-Specific Profiles

Create `application-dev.properties` for local and `application-prod.properties` for production:

```bash
# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with prod profile (JAR)
java -jar target/insurepro-backend-1.0.0.jar --spring.profiles.active=prod
```

```properties
# application-prod.properties
spring.jpa.hibernate.ddl-auto=none   # Never auto-create in prod
spring.jpa.show-sql=false
logging.level.com.insurepro=WARN
```

---

## STEP 12 — Docker (Optional — Containerized Deployment)

Create `Dockerfile` in project root:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/insurepro-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: insurepro_db
      POSTGRES_USER: insurepro_user
      POSTGRES_PASSWORD: insurepro_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/insurepro_db
      SPRING_DATASOURCE_USERNAME: insurepro_user
      SPRING_DATASOURCE_PASSWORD: insurepro_pass
      JWT_SECRET: InsureProSecretKey2024!!
    depends_on:
      - db

volumes:
  postgres_data:
```

```bash
# Build and start everything
docker-compose up --build

# Stop
docker-compose down
```

---

## Common Issues & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Connection refused on 5432` | PostgreSQL not running | `brew services start postgresql` or `sudo systemctl start postgresql` |
| `Access denied for user` | Wrong DB credentials | Check `application.properties` username/password match DB |
| `Table 'users' doesn't exist` | DDL not run | Set `spring.jpa.hibernate.ddl-auto=create` for first run |
| `JWT signature does not match` | Wrong jwt.secret | Ensure same secret in config |
| `403 Forbidden` | Role mismatch | Check `@PreAuthorize` on controller vs your user's role |
| `CORS policy blocked` | Frontend origin not whitelisted | Add frontend URL to `corsConfigurationSource()` |
| `Mail sending failed` | Gmail App Password not set | Generate App Password at myaccount.google.com |
| `Port 8080 already in use` | Another process | Change `server.port=8081` or kill the process |
