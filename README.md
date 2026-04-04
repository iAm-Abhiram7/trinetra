# Trinetra Backend - Local Setup

## Prerequisites

1. Java 17 installed.
2. Maven 3.9+ installed.
3. MongoDB available:
- Atlas URI, or
- Local MongoDB on `mongodb://localhost:27017`.

## 1) Go to backend folder

```powershell
Set-Location C:\Users\mechg\OneDrive\Desktop\trinetra\backend
```

## 2) Verify Java and Maven

```powershell
java --version
mvn -version
```

Expected: Maven should run with Java 17 runtime.

If Maven says JAVA_HOME is wrong, set it to JDK root (not `bin`):

```powershell
$env:JAVA_HOME="C:\Path\To\jdk-17"
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
```

## 3) Configure environment variables (recommended)

```powershell
$env:MONGODB_URI="<your-atlas-or-local-uri>"
$env:MONGODB_USERS_DB="users_db"
$env:MONGODB_EXAMS_DB="exams_db"

$env:JWT_SECRET="change-this-to-a-long-random-secret"
$env:JWT_EXPIRY_STUDENT="3600000"
$env:JWT_EXPIRY_ADMIN="7200000"
$env:JWT_EXPIRY_SUPERADMIN="7200000"

$env:SUPERADMIN_USERNAME="superadmin"
$env:SUPERADMIN_PASSWORD="superadmin123"
```

## 4) Run the app

Default port 8080:

```powershell
mvn spring-boot:run
```

If 8080 is busy, run on 8081:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

## 5) Check the app

If running on 8081:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## 6) Quick auth check (Swagger)

Use `POST /admin/login` with:

```json
{
	"email": "superadmin",
	"password": "superadmin123"
}
```

## 7) Run tests

```powershell
mvn test
```

## 8) Stop the app

Press `Ctrl + C` in the terminal running Spring Boot.