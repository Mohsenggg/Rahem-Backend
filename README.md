# Backend Setup & Development Guide

This document summarizes the runtime environment, prerequisites, and commands used to build and run the `Rahem-Backend` application.

---

### 1. Prerequisites & Java Environment

* **Java Target Version**: Java 21 (`pom.xml` targets `<java.version>21</java.version>`).
* **Java Runtime / `JAVA_HOME`**:
  * The machine's default system `PATH` points to Java 11 (`C:\Program Files\Eclipse Adoptium\jdk-11.0.30.7-hotspot\bin`), which causes class file version mismatch errors (`class file version 65.0 vs 55.0`).
  * The working JDK (Java 21+ compatible) is located in the IntelliJ IDEA bundled runtime:
    ```powershell
    $env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\jbr"
    ```

---

### 2. Build Tool & Maven Wrapper

The project uses the Maven Wrapper (`mvnw` / `mvnw.cmd`) located at the root of `Rahem-Backend`.

#### Running Tests:
```powershell
cd d:\Software\Rahem\Rahem-Backend
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\jbr"
.\mvnw test
```

#### Running the Backend Application:
```powershell
cd d:\Software\Rahem\Rahem-Backend
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\jbr"
.\mvnw spring-boot:run
```

---

### 3. Application Configuration & Database

* **Spring Boot Version**: `3.5.4`
* **Active Spring Profile**: `local`
* **Database**:
  * PostgreSQL (PostgreSQL driver with HikariCP connection pooling `HikariDataSource`)
  * Auto-configured with Hibernate JPA ORM `6.6.22.Final`
* **CORS & Allowed Origins**:
  * `http://localhost:4200` (Angular Frontend)
  * `https://rahem-social.web.app`

---

### 4. Key Endpoints & Architecture

* **Auth Endpoints**:
  * `POST /api/auth/login` (Expects JSON body `{ username, password, rememberMe }`; returns `200 OK` with JWT on success, `401 Unauthorized` on bad credentials, `403 Forbidden` on disabled/locked accounts).
  * `POST /api/auth/register`
  * `POST /api/auth/invitation/check`
  * `POST /api/auth/invitation/generate` (Authenticated)
* **Protected Endpoints**:
  * `/api/trees/**` (Authenticated via Bearer JWT)
  * `/api/admin/**` (Requires role `ADMIN`)
