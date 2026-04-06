# Trinetra API Endpoint Test Payloads

This file is generated from the current backend controller and DTO code, so it reflects the endpoints that are actually live now.

## Base Setup

- Base URL: `http://localhost:8080`
- Content-Type header for JSON requests:
  - `Content-Type: application/json`
- Auth header for protected routes:
  - `Authorization: Bearer <TOKEN>`

## Auth and Role Notes

- `POST /auth/signup` is public.
- `POST /auth/login` is public, but only works after student approval status becomes `APPROVED`.
- `POST /admin/login` is public.
- SUPER_ADMIN-only routes:
  - `/admin/users`, `/admin/admins`, `/admin/add-admin`, `/admincred`, `/admin/del-admin`
  - `/admin/tests`, `/admin/test/create`, `/admin/test/{testId}`, `/admin/results`
- COLLEGE_ADMIN + SUPER_ADMIN routes:
  - `/admin/approvals`, `/admin/rejections`
  - `/admin/approve/{id}`, `/admin/approve/all`
  - `/admin/reject/{id}`, `/admin/reject/all`
  - `/admin/student/{id}`, `/admin/compare`
  - `/admin/testStatus/{testId}`
  - `/admin/history`, `/admin/history/{id}`
- Use default super admin creds (unless overridden by env):

```json
{
  "email": "superadmin",
  "password": "superadmin123"
}
```

---

## Endpoint List (Current Code)

1. `POST /auth/signup`
2. `POST /auth/login`
3. `POST /admin/login`
4. `GET /admin/users`
5. `GET /admin/admins`
6. `POST /admin/add-admin`
7. `GET /admincred` (also works as `/admincred/`)
8. `PUT /admin/del-admin`
9. `GET /admin/tests`
10. `POST /admin/test/create`
11. `DELETE /admin/test/{testId}`
12. `GET /admin/results`
13. `GET /admin/approvals`
14. `GET /admin/rejections`
15. `POST /admin/approve/{id}`
16. `POST /admin/approve/all`
17. `POST /admin/reject/{id}`
18. `POST /admin/reject/all`
19. `GET /admin/student/{id}`
20. `POST /admin/compare`
21. `GET /admin/testStatus/{testId}`
22. `GET /admin/history`
23. `GET /admin/history/{id}`

---

## 1) POST /auth/signup

### Valid payload

```json
{
  "name": "Priya Sharma",
  "email": "priya.sharma.student@example.com",
  "password": "SecurePass@123",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026
}
```

### Negative payloads

Missing required field (`email`) -> expected 400

```json
{
  "name": "Priya Sharma",
  "password": "SecurePass@123",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026
}
```

Invalid email -> expected 400

```json
{
  "name": "Priya Sharma",
  "email": "not-an-email",
  "password": "SecurePass@123",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026
}
```

Short password (< 8 chars) -> expected 400

```json
{
  "name": "Priya Sharma",
  "email": "priya.shortpass@example.com",
  "password": "short",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026
}
```

Invalid year (`< 2000`) -> expected 400

```json
{
  "name": "Priya Sharma",
  "email": "priya.year@example.com",
  "password": "SecurePass@123",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 1999
}
```

Duplicate email (reuse previously signed-up email) -> expected 409

---

## 2) POST /auth/login

### Valid payload

Use an approved student account.

```json
{
  "email": "approved.student@example.com",
  "password": "SecurePass@123"
}
```

### Negative payloads

Wrong password -> expected 401

```json
{
  "email": "approved.student@example.com",
  "password": "WrongPassword@123"
}
```

Unknown email -> expected 404

```json
{
  "email": "missing.student@example.com",
  "password": "SecurePass@123"
}
```

Pending or rejected student account -> expected 403

```json
{
  "email": "priya.sharma.student@example.com",
  "password": "SecurePass@123"
}
```

---

## 3) POST /admin/login

### Valid payload (super admin)

```json
{
  "email": "superadmin",
  "password": "superadmin123"
}
```

### Valid payload (college admin)

```json
{
  "email": "admin.jntuh.cse@example.com",
  "password": "AdminPass@123"
}
```

### Negative payloads

Wrong password -> expected 401

```json
{
  "email": "superadmin",
  "password": "wrong-pass"
}
```

Unknown admin email -> expected 404

```json
{
  "email": "missing.admin@example.com",
  "password": "AdminPass@123"
}
```

Deactivated admin -> expected 403

---

## 4) GET /admin/users (SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/users?page=1&limit=50
```

Filtered list:

```text
/admin/users?page=1&limit=20&state=Telangana&college=JNTUH&branch=CSE&status=PENDING&search=priya
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-super-admin token -> expected 403

---

## 5) GET /admin/admins (SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/admins?page=1&limit=20
```

Filtered list:

```text
/admin/admins?page=1&limit=10&state=Telangana&college=JNTUH&isActive=true
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-super-admin token -> expected 403

---

## 6) POST /admin/add-admin (SUPER_ADMIN)

### Valid payload

```json
{
  "name": "JNTUH CSE Admin",
  "email": "admin.jntuh.cse@example.com",
  "password": "AdminPass@123",
  "scopes": [
    {
      "state": "Telangana",
      "college": "JNTUH",
      "branch": "CSE"
    }
  ]
}
```

### Negative payloads

Invalid email -> expected 400

```json
{
  "name": "JNTUH CSE Admin",
  "email": "not-email",
  "password": "AdminPass@123",
  "scopes": [
    {
      "state": "Telangana",
      "college": "JNTUH",
      "branch": "CSE"
    }
  ]
}
```

Short password (< 8 chars) -> expected 400

```json
{
  "name": "JNTUH CSE Admin",
  "email": "admin.shortpass@example.com",
  "password": "short",
  "scopes": [
    {
      "state": "Telangana",
      "college": "JNTUH",
      "branch": "CSE"
    }
  ]
}
```

Empty scopes -> expected 400

```json
{
  "name": "JNTUH CSE Admin",
  "email": "admin.emptyscopes@example.com",
  "password": "AdminPass@123",
  "scopes": []
}
```

Duplicate email -> expected 409

---

## 7) GET /admincred and /admincred/ (SUPER_ADMIN)

### Query test cases

```text
/admincred?college=JNTUH&branch=CSE
```

Trailing slash variant:

```text
/admincred/?college=JNTUH&branch=CSE
```

No request body.

### Negative cases

Missing required query param -> expected 400

```text
/admincred?college=JNTUH
```

Unknown college + branch mapping -> expected 404

---

## 8) PUT /admin/del-admin (SUPER_ADMIN)

### Valid payload

```json
{
  "college": "JNTUH",
  "branch": "CSE"
}
```

### Negative payloads

Missing branch -> expected 400

```json
{
  "college": "JNTUH"
}
```

Unknown college + branch mapping -> expected 404

```json
{
  "college": "UnknownCollege",
  "branch": "XYZ"
}
```

Already deactivated admin -> expected 409

---

## 9) GET /admin/tests (SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/tests?page=1&limit=20
```

Filtered list:

```text
/admin/tests?page=1&limit=10&type=SCHEDULED&isPublished=true&search=Aptitude
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-super-admin token -> expected 403

---

## 10) POST /admin/test/create (SUPER_ADMIN)

### Valid payload A: SCHEDULED exam

```json
{
  "title": "Aptitude Campus Drive - CSE 2026",
  "type": "SCHEDULED",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026,
  "scheduledWindow": {
    "start": "2099-06-01T09:00:00Z",
    "end": "2099-06-01T10:00:00Z"
  },
  "durationMinutes": 60,
  "totalQuestions": 2,
  "totalMarks": 2,
  "negativeMarking": 0.25,
  "shuffleQuestions": false,
  "shuffleOptions": false,
  "isPublished": true,
  "questions": [
    {
      "text": "2 + 2 = ?",
      "options": ["1", "2", "3", "4"],
      "correctIndex": 3,
      "explanation": "Basic arithmetic",
      "topic": "Arithmetic",
      "difficulty": "EASY"
    },
    {
      "text": "If all roses are flowers and some flowers fade quickly, which is valid?",
      "options": ["Some roses fade quickly", "No roses fade quickly", "All flowers are roses", "Cannot be determined"],
      "correctIndex": 3,
      "explanation": "No direct relation to roses fading is guaranteed",
      "topic": "Logical Reasoning",
      "difficulty": "MEDIUM"
    }
  ]
}
```

### Valid payload B: PRACTICE exam

```json
{
  "title": "Quant Practice Set 1",
  "type": "PRACTICE",
  "state": null,
  "college": null,
  "branch": null,
  "yearOfPassing": null,
  "scheduledWindow": null,
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "isPublished": false,
  "questions": [
    {
      "text": "What is 15% of 200?",
      "options": ["20", "25", "30", "35"],
      "correctIndex": 2,
      "explanation": "0.15 x 200 = 30",
      "topic": "Percentages",
      "difficulty": "EASY"
    }
  ]
}
```

### Negative payloads

Invalid type -> expected 400

```json
{
  "title": "Invalid Type Test",
  "type": "MOCK",
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "questions": [
    {
      "text": "Sample?",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "EASY"
    }
  ]
}
```

Mismatch between `totalQuestions` and array size -> expected 400

```json
{
  "title": "Question Count Mismatch",
  "type": "PRACTICE",
  "durationMinutes": 30,
  "totalQuestions": 2,
  "totalMarks": 2,
  "questions": [
    {
      "text": "Only one question present",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "EASY"
    }
  ]
}
```

Invalid difficulty value -> expected 400

```json
{
  "title": "Invalid Difficulty",
  "type": "PRACTICE",
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "questions": [
    {
      "text": "Sample",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "BEGINNER"
    }
  ]
}
```

SCHEDULED missing scheduled window -> expected 400

```json
{
  "title": "Scheduled Missing Window",
  "type": "SCHEDULED",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026,
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "questions": [
    {
      "text": "Sample",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "EASY"
    }
  ]
}
```

SCHEDULED start time in past -> expected 422

```json
{
  "title": "Scheduled Past Start",
  "type": "SCHEDULED",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026,
  "scheduledWindow": {
    "start": "2020-01-01T09:00:00Z",
    "end": "2020-01-01T10:00:00Z"
  },
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "questions": [
    {
      "text": "Sample",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "EASY"
    }
  ]
}
```

PRACTICE with non-null scope fields -> expected 400

```json
{
  "title": "Practice With Scope (Invalid)",
  "type": "PRACTICE",
  "state": "Telangana",
  "college": "JNTUH",
  "branch": "CSE",
  "yearOfPassing": 2026,
  "durationMinutes": 30,
  "totalQuestions": 1,
  "totalMarks": 1,
  "questions": [
    {
      "text": "Sample",
      "options": ["A", "B", "C", "D"],
      "correctIndex": 0,
      "topic": "General",
      "difficulty": "EASY"
    }
  ]
}
```

---

## 11) DELETE /admin/test/{testId} (SUPER_ADMIN)

### Path variable test case

```text
/admin/test/<testId>
```

Use `testId` from `POST /admin/test/create` response `data.id`.

No request body.

### Negative cases

Non-existing testId -> expected 404

```text
/admin/test/507f1f77bcf86cd799439011
```

No bearer token -> expected 401

---

## 12) GET /admin/results (SUPER_ADMIN)

### Query test cases

All results (paginated):

```text
/admin/results?page=1&limit=20
```

Single test aggregation:

```text
/admin/results?page=1&limit=20&testId=<testId>
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-super-admin token -> expected 403

---

## 13) GET /admin/approvals (COLLEGE_ADMIN + SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/approvals?page=1&limit=20
```

Filtered list:

```text
/admin/approvals?page=1&limit=20&branch=CSE&search=priya
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-admin token -> expected 403

---

## 14) GET /admin/rejections (COLLEGE_ADMIN + SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/rejections?page=1&limit=20
```

Filtered list:

```text
/admin/rejections?page=1&limit=20&branch=CSE&search=amit
```

No request body.

### Negative cases

- No bearer token -> expected 401
- Non-admin token -> expected 403

---

## 15) POST /admin/approve/{id} (COLLEGE_ADMIN + SUPER_ADMIN)

### Path variable test case

```text
/admin/approve/<studentId>
```

No request body.

### Negative cases

Student not in scope -> expected 403

Student not found -> expected 404

Student not in `PENDING` status -> expected 409

---

## 16) POST /admin/approve/all (COLLEGE_ADMIN + SUPER_ADMIN)

### Valid payload (branch filter)

```json
{
  "branch": "CSE"
}
```

### Valid payload (all scoped branches)

```json
{}
```

### Negative cases

No pending students in scope -> expected 404

---

## 17) POST /admin/reject/{id} (COLLEGE_ADMIN + SUPER_ADMIN)

### Valid payload

```json
{
  "rejectionReason": "Duplicate registration. Student already has an approved account."
}
```

### Negative payloads

Missing `rejectionReason` -> expected 400

```json
{}
```

Student not in scope -> expected 403

Student not found -> expected 404

Student not in `PENDING` status -> expected 409

---

## 18) POST /admin/reject/all (COLLEGE_ADMIN + SUPER_ADMIN)

### Valid payload (with branch)

```json
{
  "rejectionReason": "Registration period ended. Re-apply next semester.",
  "branch": "CSE"
}
```

### Valid payload (all scoped branches)

```json
{
  "rejectionReason": "Registration period ended. Re-apply next semester."
}
```

### Negative payloads

Missing `rejectionReason` -> expected 400

```json
{
  "branch": "CSE"
}
```

No pending students in scope -> expected 404

---

## 19) GET /admin/student/{id} (COLLEGE_ADMIN + SUPER_ADMIN)

### Path variable test case

```text
/admin/student/<studentId>
```

No request body.

### Negative cases

Student not in scope -> expected 403

Student not found -> expected 404

---

## 20) POST /admin/compare (COLLEGE_ADMIN + SUPER_ADMIN)

### Valid payload

```json
{
  "studentId1": "<studentId1>",
  "studentId2": "<studentId2>"
}
```

### Negative payloads

Same student IDs -> expected 400

```json
{
  "studentId1": "<studentId>",
  "studentId2": "<studentId>"
}
```

Missing student ID field -> expected 400

Student not in scope -> expected 403

Student not found -> expected 404

---

## 21) GET /admin/testStatus/{testId} (COLLEGE_ADMIN + SUPER_ADMIN)

### Query test cases

Basic:

```text
/admin/testStatus/<testId>?page=1&limit=20
```

Sorted:

```text
/admin/testStatus/<testId>?page=1&limit=20&sortBy=timeTaken&order=asc
```

### Negative cases

Test not in scope -> expected 403

Test not found -> expected 404

No bearer token -> expected 401

---

## 22) GET /admin/history (COLLEGE_ADMIN + SUPER_ADMIN)

### Query test cases

Basic pagination:

```text
/admin/history?page=1&limit=20
```

Filtered history:

```text
/admin/history?page=1&limit=20&action=REJECTED&branch=CSE&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z
```

### Negative cases

Invalid action (`PENDING`) -> expected 400

```text
/admin/history?page=1&limit=20&action=PENDING
```

No bearer token -> expected 401

---

## 23) GET /admin/history/{id} (COLLEGE_ADMIN + SUPER_ADMIN)

### Path variable test case

```text
/admin/history/<studentId>
```

### Notes

- Current implementation returns latest action only for this student by this admin (schema is unchanged).

### Negative cases

Student not in scope -> expected 403

Student not found -> expected 404

No action history for this student by this admin -> expected 404

---

## Common Error Response Shape

```json
{
  "success": false,
  "message": "<error message>",
  "data": null,
  "timestamp": "2026-04-05T12:34:56Z"
}
```

## Suggested End-to-End Test Order

1. `POST /admin/login` (super admin)
2. `POST /auth/signup`
3. `POST /admin/add-admin`
4. `POST /admin/login` (college admin)
5. `GET /admin/approvals`
6. `POST /admin/approve/{id}`
7. `GET /admin/student/{id}`
8. `POST /admin/compare`
9. `GET /admin/history`
10. `GET /admin/history/{id}`
11. `GET /admin/users`
12. `GET /admin/admins`
13. `GET /admincred`
14. `POST /admin/test/create`
15. `GET /admin/tests`
16. `GET /admin/testStatus/{testId}`
17. `GET /admin/results`
18. `DELETE /admin/test/{testId}`
19. `PUT /admin/del-admin`
20. `POST /auth/login` (student login only after student is approved)

