# Copilot Documenation of API

## 📘 Student Assessment Platform —

## Complete API Documentation

```
Version: 1. 0
Database: MongoDB (users_db + exams_db)
Auth: JWT Bearer Token
Password Hashing: bcrypt
```
## Table of Contents

```
Auth — Student Authentication
Content — Articles and Practice Exams
Test — Scheduled Tests Student-Facing)
Admin — College Admin Endpoints
Super Admin — Platform-Wide Management
Database Architecture
Endpoint Summary Table
```
## 1. Auth — Student Authentication

#### POST /auth/signup

Description: Registers a new student account. After signup,
approvalStatus.status is set to PENDING — the student cannot log in until a College
Admin approves them.

Request Body:

```
{
"name": "Priya Sharma",
"email": "priya.sharma@student.jntuh.ac.in",
```

```
"password": "SecurePass@123",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026
}
```
Internal Operations:

```
Creates a new document in users_db.users.
password → hashed via bcrypt → stored as passwordHash (plain text never
saved).
role^ →^ STUDENT.
approvalStatus → { "status": "PENDING", "approvedBy": null, "approvedAt": null,
"rejectionReason": null }.
aptitudeHistory → [] (empty array).
createdAt → current timestamp.
```
Response **201 Created** :

```
{
"message": "Account created successfully. Awaiting admin
approval.",
"data": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"email": "priya.sharma@student.jntuh.ac.in",
"role": "STUDENT",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": { "status": "PENDING" },
"createdAt": "2025-04-01T09:00:00Z"
}
}
```
Errors:


```
Status Reason
```
(^400) Missing or invalid fields
(^409) Email already registered

#### POST /auth/login

Description: Authenticates a student with email and password. Returns a JWT

token. Login is blocked if (^) approvalStatus.status is not (^) APPROVED.
Request Body:
{
"email": "priya.sharma@student.jntuh.ac.in",
"password": "SecurePass@123"
}
Internal Operations:
Looks up student in users_db.users by email.
Compares password against passwordHash using bcrypt.
Checks approvalStatus.status === "APPROVED". If PENDING or REJECTED, login
denied.
Updates lastLoginAt to current timestamp.
Returns signed JWT containing (^) _id, (^) role, (^) college, (^) branch.
Response **200 OK** :
{
"message": "Login successful.",
"data": {
"token": "eyJhbGciOiJIUzI1NiIs...",
"user": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"email": "priya.sharma@student.jntuh.ac.in",
"role": "STUDENT",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",


```
"approvalStatus": { "status": "APPROVED" }
}
}
}
```
Errors:

```
Status Reason
```
(^400) Missing email or password
(^401) Wrong password
(^403) Account is PENDING or REJECTED
(^404) No account with that email

#### PUT /auth/update-profile

Description: Allows the logged-in student to update profile fields (name, state,
college, branch, yearOfPassing). Email and password cannot be changed here.

Headers: Authorization: Bearer <token>

Request Body (partial update allowed):

```
{
"name": "Priya S. Kumar",
"state": "Andhra Pradesh",
"college": "SVUCE",
"branch": "ECE",
"yearOfPassing": 2027
}
```
Internal Operations:

```
Extracts _id from JWT.
```
Updates only supplied fields in (^) users_db.users.
If college or branch changes, approvalStatus may be reset to PENDING
(requires re-approval).
Response **200 OK** :


##### {

```
"message": "Profile updated successfully.",
"data": {
"_id": "64bf2d3e0c4a...",
"name": "Priya S. Kumar",
"email": "priya.sharma@student.jntuh.ac.in",
"state": "Andhra Pradesh",
"college": "SVUCE",
"branch": "ECE",
"yearOfPassing": 2027,
"approvalStatus": { "status": "PENDING" }
}
}
```
Errors:

```
Status Reason
```
(^400) Invalid field values
(^401) Missing or expired token

#### GET /profile

Description: Returns the full profile of the currently logged-in student including
approval status and complete aptitude history.

Headers: Authorization: Bearer <token>

Response **200 OK** :

```
{
"data": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"email": "priya.sharma@student.jntuh.ac.in",
"role": "STUDENT",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
```

```
"approvalStatus": {
"status": "APPROVED",
"approvedBy": "64af1c2e9b3f...",
"approvedAt": "2024-06-05T10:15:00Z",
"rejectionReason": null
},
"aptitudeHistory": [
{ "topic": "Number Series", "score": 8, "timeTaken":
420, "type": "PRACTICE", "attemptedAt": "2025-03-20T14:00:
0Z" },
{ "topic": "Logical Reasoning", "score": 7, "timeTake
n": 510, "type": "SCHEDULED", "attemptedAt": "2025-03-25T
0:00:00Z" }
],
"createdAt": "2024-06-01T09:00:00Z"
}
}
```
Errors:

```
Status Reason
```
(^401) Missing or expired token

#### POST /auth/update-password

Description: Initiates a password change. Student sends their current
password; the system sends a one-time password OTP to their registered
email.

Headers: Authorization: Bearer <token>

Request Body:

```
{ "currentPassword": "SecurePass@123" }
```
Internal Operations:

```
Verifies currentPassword against stored passwordHash.
Generates a time-limited OTP  6 digits, valid 5 minutes).
Sends OTP to student's email.
```

```
Stores OTP hash temporarily server-side.
```
Response **200 OK** :

```
{ "message": "OTP sent to your registered email. Valid for
5 minutes." }
```
Errors:

```
Status Reason
```
(^400) Missing password field
(^401) Current password incorrect or token missing/expired

#### POST /auth/update-pass/otp

Description: Completes the password change. Student sends OTP + new
password.

Headers: Authorization: Bearer <token>

Request Body:

```
{ "otp": "482917", "newPassword": "NewSecure@456" }
```
Internal Operations:

```
Validates OTP against stored hash + checks expiry.
Hashes newPassword with bcrypt.
Updates passwordHash in users_db.users.
Invalidates OTP (no reuse).
```
Response (^) **200 OK** :
{ "message": "Password updated successfully." }
Errors:
Status Reason
(^400) Missing OTP or new password
(^401) Token missing/expired


```
Status Reason
```
(^410) OTP expired
(^422) Invalid or incorrect OTP

## 2. Content — Articles and Practice Exams

#### GET /article/{topic}

Description: Returns an educational article for the given topic. Available to any
approved student.

Headers: Authorization: Bearer <token>

Path Params: topic (string) — e.g. number-series

Response **200 OK** :

```
{
"data": {
"topic": "Number Series",
"slug": "number-series",
"content": "A number series is a sequence of numbers th
at follows a specific pattern...",
"examples": [
{ "question": "Find the next number: 2, 6, 12, 20,
?", "answer": "30", "explanation": "Differences are 4, 6, 8
→ next difference is 10 → 20 + 10 = 30." }
],
"relatedTopics": ["Logical Reasoning", "Pattern Recogni
tion"]
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Student not approved
(^404) No article for this topic


#### GET /exam/{topic}

Description: Fetches a practice exam from exams_db.exams where type =
"PRACTICE". Questions sent one at a time (anti-cheat). If shuffleQuestions: true,
order is randomised per attempt.

Headers: Authorization: Bearer <token>

Path Params: topic (string)

Query Params:

```
Param Type Default Description
```
```
questionIndex int 0 Zero-based^ index^ of^ question^ to
fetch
```
Response **200 OK** :

```
{
"data": {
"examId": "66df4f5a2e6c...",
"title": "Quantitative Aptitude — Practice Set 3",
"type": "PRACTICE",
"totalQuestions": 30,
"totalMarks": 30,
"negativeMarking": 0,
"durationMinutes": 45,
"currentQuestion": {
"index": 0,
"text": "A train 150m long passes a pole in 15 sec. F
ind speed.",
"options": ["36 km/h", "40 km/h", "45 km/h", "54 km/
h"],
"topic": "Speed & Distance",
"difficulty": "EASY"
},
"hasNext": true
}
}
```
```
⚠ correctIndex is never sent to the client. Answer revealed only after
submission.
```

Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Student not approved
(^404) No practice exam for this topic

#### POST /exam/submit/{topic}

Description: Submits answers for a practice exam. Scores the attempt and
appends a new entry to the student's aptitudeHistory[] in users_db.users.

Headers: Authorization: Bearer <token>

Path Params: topic (string)

Request Body:

```
{
"examId": "66df4f5a2e6c...",
"answers": [
{ "questionIndex": 0, "selectedOption": 0 },
{ "questionIndex": 1, "selectedOption": 2 },
{ "questionIndex": 2, "selectedOption": null }
]
}
```
Internal Operations:

```
Fetches exam from exams_db.exams.
Compares each selectedOption against correctIndex.
```
Scoring: (^) +1 correct, 0 wrong (practice has (^) negativeMarking: 0), skipped
(null) = 0.
Calculates timeTaken from session start.
Appends to aptitudeHistory[]: { "topic": "...", "score": 24, "timeTaken": 1860,
"type": "PRACTICE", "attemptedAt": "..." }.
If aptitudeHistory exceeds 50 entries, oldest is archived/removed.
Response **200 OK** :


##### {

```
"message": "Exam submitted successfully.",
"data": {
"examId": "66df4f5a2e6c...",
"topic": "Time & Work",
"type": "PRACTICE",
"totalQuestions": 30,
"attempted": 28,
"correct": 24,
"wrong": 4,
"skipped": 2,
"score": 24,
"totalMarks": 30,
"timeTaken": 1860,
"percentage": 80.
}
}
```
Errors:

```
Status Reason
```
(^400) Invalid answers format or count mismatch
(^401) Token missing/expired
(^403) Student not approved
(^404) No practice exam for this topic

#### GET /result/exam/{topic}

Description: Retrieves the student's result(s) for the specified practice exam

topic from (^) aptitudeHistory[] where (^) type = "PRACTICE".
Headers: Authorization: Bearer <token>
Path Params: topic (string)
Query Params (pagination):
Param Type Default Description
page int (^1) Page number


```
Param Type Default Description
```
limit int (^10) Results per page
all bool false If^ true,^ returns^ all^ attempts
(paginated)
Response **200 OK** (latest only, default):
{
"data": {
"topic": "Number Series",
"score": 8,
"totalMarks": 10,
"timeTaken": 420,
"type": "PRACTICE",
"attemptedAt": "2025-03-20T14:00:00Z",
"percentage": 80.
}
}
Response **200 OK** (with **?all=true&page=1&limit=10** ):
{
"data": {
"topic": "Number Series",
"attempts": [
{ "score": 8, "timeTaken": 420, "attemptedAt": "2025-
03-20T14:00:00Z" },
{ "score": 6, "timeTaken": 500, "attemptedAt": "2025-
03-18T11:00:00Z" }
],
"pagination": { "currentPage": 1, "totalPages": 1, "tot
alAttempts": 2, "limit": 10 }
}
}
Errors:


```
Status Reason
```
(^401) Token missing/expired
(^404) No attempts found for this topic

## 3. Test — Scheduled Tests (Student-Facing)

#### GET /test/{testId}

Description: Fetches a scheduled test by _id from exams_db.exams where type =
"SCHEDULED". Enforces access control (state/college/branch/year must match)
and time window (must be within scheduledWindow.start and scheduledWindow.end).
Questions sent one at a time.

Headers: Authorization: Bearer <token>

Path Params: testId (string)

Query Params:

```
Param Type Default Description
questionIndex int 0 Which^ question^ to^ fetch^ (zero-
based)
```
Internal Operations:

```
Fetches exam from exams_db.exams.
Checks isPublished === true.
Access control: Student's state, college, branch, yearOfPassing must ALL
match exam's access fields.
Time window: scheduledWindow.start <= now <= scheduledWindow.end.
Returns only the requested question (never full list).
```
Response **200 OK** :

```
{
"data": {
"testId": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",
"type": "SCHEDULED",
"totalQuestions": 30,
```

```
"totalMarks": 30,
"negativeMarking": 0.25,
"durationMinutes": 45,
"scheduledWindow": { "start": "2025-04-05T09:00:00Z",
"end": "2025-04-05T11:00:00Z" },
"currentQuestion": {
"index": 0,
"text": "If 6 men do a task in 12 days, how many days
for 9 men?",
"options": ["8", "9", "10", "12"],
"topic": "Time & Work",
"difficulty": "MEDIUM"
},
"hasNext": true
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Scope mismatch or not published
(^404) No exam with this testId
(^410) Time window passed
(^425) Exam hasn't started yet

#### POST /test/{testId}/submit

Description: Submits answers for a scheduled test. Applies negative marking.
Appends entry to aptitudeHistory[] with type: "SCHEDULED".

Headers: Authorization: Bearer <token>

Path Params: testId (string)

Request Body:

```
{
"answers": [
{ "questionIndex": 0, "selectedOption": 1 },
```

```
{ "questionIndex": 1, "selectedOption": 3 },
{ "questionIndex": 2, "selectedOption": null }
]
}
```
Internal Operations:

```
Re-validates access control + time window.
Checks no duplicate submission.
```
Scoring: correct (^) +1, wrong (^) 0.25 (from (^) negativeMarking), skipped 0.
Appends to aptitudeHistory[]: { "topic": "...", "score": 22.5, "timeTaken": 2400,
"type": "SCHEDULED", "attemptedAt": "..." }.
Response **200 OK** :
{
"message": "Test submitted successfully.",
"data": {
"testId": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",
"type": "SCHEDULED",
"totalQuestions": 30,
"attempted": 27,
"correct": 24,
"wrong": 3,
"skipped": 3,
"score": 23.25,
"totalMarks": 30,
"negativeMarking": 0.25,
"timeTaken": 2400,
"percentage": 77.
}
}
Errors:
Status Reason
(^400) Invalid answer format


```
Status Reason
```
(^401) Token missing/expired
(^403) Access denied (scope mismatch)
(^409) Already submitted this test
(^410) Time window expired

#### GET /test/{testId}/results

Description: Retrieves the logged-in student's result for a specific scheduled
test from their aptitudeHistory[].

Headers: Authorization: Bearer <token>

Path Params: testId (string)

Response **200 OK** :

```
{
"data": {
"testId": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",
"type": "SCHEDULED",
"score": 23.25,
"totalMarks": 30,
"negativeMarking": 0.25,
"timeTaken": 2400,
"percentage": 77.5,
"attemptedAt": "2025-04-05T09:42:00Z",
"breakdown": { "attempted": 27, "correct": 24, "wrong":
3, "skipped": 3 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) No access to this test
(^404) Student hasn't attempted this test


## 4. Admin — College Admin Endpoints

All admin endpoints require (^) role: "COLLEGE_ADMIN" or (^) SUPER_ADMIN in JWT. Every
query is scoped — admin can only see/manage students whose (^) state,
college,^ branch^ match^ one^ of^ the^ admin's^ scopes[].

#### POST /admin/login

Description: Authenticates a college admin. Looks up users_db.collegeAdmins by
email, verifies bcrypt password, checks isActive === true, returns JWT.

Request Body:

```
{ "email": "admin.cse@jntuh.ac.in", "password": "AdminPass@
789" }
```
Internal Operations:

```
Finds admin in users_db.collegeAdmins by email.
```
Compares password against (^) passwordHash.
Checks isActive === true (deactivated admins blocked).
Updates lastLoginAt.
Returns JWT containing _id, role, scopes[].
Response **200 OK** :
{
"message": "Login successful.",
"data": {
"token": "eyJhbGciOiJIUzI1NiIs...",
"admin": {
"_id": "64af1c2e9b3f...",
"name": "Dr. Ramesh Kumar",
"email": "admin.cse@jntuh.ac.in",
"role": "COLLEGE_ADMIN",
"isActive": true,
"scopes": [
{ "state": "Telangana", "college": "JNTUH", "branc
h": "CSE" },


```
{ "state": "Telangana", "college": "JNTUH", "branc
h": "IT" }
],
"lastLoginAt": "2025-04-01T08:00:00Z"
}
}
}
```
Errors:

```
Status Reason
```
(^400) Missing credentials
(^401) Wrong password
(^403) Admin deactivated (isActive: false)
(^404) No admin with this email

#### GET /admin/approvals

Description: Returns students with approvalStatus.status === "PENDING" whose
state/college/branch match the admin's scopes[]. These students are waiting for
approval.

Headers: Authorization: Bearer <token> (admin)

Query Params (pagination):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Students per page
branch string all scoped Filter by branch
search string — Search by name/email
Response **200 OK** :
{
"data": {
"students": [
{
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",


```
"email": "priya.sharma@student.jntuh.ac.in",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": { "status": "PENDING" },
"createdAt": "2025-04-01T09:00:00Z"
}
],
"pagination": { "currentPage": 1, "totalPages": 3, "tot
alStudents": 47, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not an admin role

#### GET /admin/rejections

Description: Returns students this admin has previously rejected —
approvalStatus.status === "REJECTED" AND approvalStatus.approvedBy matches this
admin's _id, scoped by scopes[].

Headers: Authorization: Bearer <token> (admin)

Query Params (pagination):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Students per page
branch string all scoped Filter by branch
search string — Search by name/email
Response **200 OK** :
{
"data": {


```
"students": [
{
"_id": "64cf1a2b3c4d...",
"name": "Amit Patel",
"email": "amit.p@student.jntuh.ac.in",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": {
"status": "REJECTED",
"approvedBy": "64af1c2e9b3f...",
"approvedAt": "2025-04-02T11:00:00Z",
"rejectionReason": "Duplicate registration."
},
"createdAt": "2025-04-01T08:00:00Z"
}
],
"pagination": { "currentPage": 1, "totalPages": 1, "tot
alStudents": 5, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not an admin role

#### POST /admin/approve/{id}

Description: Approves a single student. Changes approvalStatus.status from
PENDING → APPROVED. Student is removed from the pending list and can now log
in.

Headers: Authorization: Bearer <token> (admin)

Path Params: id (string) — student's _id

Internal Operations:


```
Verifies student's state/college/branch falls within admin's scopes[].
Verifies approvalStatus.status === "PENDING".
Updates student document in users_db.users:
approvalStatus.status^ →^ "APPROVED"
approvalStatus.approvedBy → admin's _id
approvalStatus.approvedAt → current timestamp
approvalStatus.rejectionReason → null
Student disappears from /admin/approvals and can now log in.
```
Response (^) **200 OK** :
{
"message": "Student approved successfully.",
"data": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"approvalStatus": { "status": "APPROVED", "approvedBy":
"64af1c2e9b3f...", "approvedAt": "2025-04-02T10:00:00Z" }
}
}
Errors:
Status Reason
(^401) Token missing/expired
(^403) Student not in admin's scope
(^404) No student with this ID
(^409) Student not in (^) PENDING status

#### POST /admin/approve/all

Description: Bulk-approves all PENDING students within the admin's scopes[].
Every matching student's status → APPROVED. All removed from pending queue
and can now log in.

Headers: Authorization: Bearer <token> (admin)


Request Body (optional):

```
{ "branch": "CSE" }
```
```
If omitted, approves all pending across all scoped branches.
```
Internal Operations:

```
Queries users_db.users: approvalStatus.status === "PENDING" AND scope match.
Bulk-updates: status → "APPROVED", approvedBy → admin._id, approvedAt → now.
```
Response (^) **200 OK** :
{
"message": "All pending students approved.",
"data": {
"approvedCount": 47,
"scope": [
{ "state": "Telangana", "college": "JNTUH", "branch":
"CSE" },
{ "state": "Telangana", "college": "JNTUH", "branch":
"IT" }
]
}
}
Errors:
Status Reason
(^401) Token missing/expired
(^403) Not an admin role
(^404) No pending students

#### POST /admin/reject/{id}

Description: Rejects a single student. Changes (^) approvalStatus.status from
PENDING^ →^ REJECTED^ with^ a^ mandatory^ rejection^ reason.^ Student^ moves^ from
pending list → rejection list. Student cannot log in.
Headers: Authorization: Bearer <token> (admin)


Path Params: id (string) — student's _id

Request Body:

```
{ "rejectionReason": "Duplicate registration. Student alrea
dy has an approved account." }
```
Internal Operations:

```
Verifies student within admin's scopes[].
Verifies approvalStatus.status === "PENDING".
Updates student document:
approvalStatus.status → "REJECTED"
approvalStatus.approvedBy → admin's _id
approvalStatus.approvedAt → current timestamp
approvalStatus.rejectionReason → supplied reason
```
Student removed from (^) /admin/approvals, appears in (^) /admin/rejections.
Student login blocked.
Response **200 OK** :
{
"message": "Student rejected.",
"data": {
"_id": "64cf1a2b3c4d...",
"name": "Amit Patel",
"approvalStatus": {
"status": "REJECTED",
"approvedBy": "64af1c2e9b3f...",
"approvedAt": "2025-04-02T11:00:00Z",
"rejectionReason": "Duplicate registration."
}
}
}
Errors:


```
Status Reason
```
(^400) Missing rejectionReason
(^401) Token missing/expired
(^403) Student not in admin's scope
(^404) No student with this ID
(^409) Student not in PENDING status

#### POST /admin/reject/all

Description: Bulk-rejects all pending students within admin's scopes with a
mandatory rejection reason applied to every student.

Headers: Authorization: Bearer <token> (admin)

Request Body:

```
{ "rejectionReason": "Registration period ended. Re-apply n
ext semester.", "branch": "CSE" }
```
```
branch optional — if omitted, rejects all pending across all scoped branches.
```
Internal Operations:

```
Queries all PENDING students matching scopes.
```
Bulk-updates: (^) status → "REJECTED", (^) approvedBy → admin._id, (^) approvedAt → now,
rejectionReason → supplied.
All move from pending → rejection list.
Response **200 OK** :
{
"message": "All pending students rejected.",
"data": { "rejectedCount": 12, "rejectionReason": "Regist
ration period ended. Re-apply next semester." }
}
Errors:


```
Status Reason
```
(^400) Missing rejectionReason
(^401) Token missing/expired
(^403) Not an admin role
(^404) No pending students

#### GET /admin/student/{id}

Description: Returns the complete profile of a specific student including all
fields, approval status, and full aptitudeHistory[]. Only if student falls within
admin's scopes[].

Headers: Authorization: Bearer <token> (admin)

Path Params: id (string)

Response **200 OK** :

```
{
"data": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"email": "priya.sharma@student.jntuh.ac.in",
"role": "STUDENT",
"state": "Telangana",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": {
"status": "APPROVED",
"approvedBy": "64af1c2e9b3f...",
"approvedAt": "2024-06-05T10:15:00Z",
"rejectionReason": null
},
"aptitudeHistory": [
{ "topic": "Number Series", "score": 8, "timeTaken":
420, "type": "PRACTICE", "attemptedAt": "2025-03-20T14:00:0
0Z" },
{ "topic": "Logical Reasoning", "score": 7, "timeTake
n": 510, "type": "SCHEDULED", "attemptedAt": "2025-03-25T1
```

##### 0:00:00Z" }

##### ],

```
"createdAt": "2024-06-01T09:00:00Z"
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Student not within admin's scopes
(^404) No student with this ID

#### GET /admin/history

Description: Returns the admin's complete action history — every approval
and rejection performed. Queries users_db.users for students where
approvalStatus.approvedBy === admin._id. Sorted by approvedAt descending.

Headers: Authorization: Bearer <token> (admin)

Query Params (pagination and filtering):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Records per page
action string all Filter: APPROVED or REJECTED
branch string all scoped Filter by branch
from ISO date — Start date
to ISO date — End date
Response **200 OK** :
{
"data": {
"history": [
{
"studentId": "64bf2d3e0c4a...",
"studentName": "Priya Sharma",


```
"email": "priya.sharma@student.jntuh.ac.in",
"college": "JNTUH",
"branch": "CSE",
"action": "APPROVED",
"actionAt": "2025-04-02T10:00:00Z",
"rejectionReason": null
},
{
"studentId": "64cf1a2b3c4d...",
"studentName": "Amit Patel",
"email": "amit.p@student.jntuh.ac.in",
"college": "JNTUH",
"branch": "CSE",
"action": "REJECTED",
"actionAt": "2025-04-02T11:00:00Z",
"rejectionReason": "Duplicate registration."
}
],
"pagination": { "currentPage": 1, "totalPages": 5, "tot
alRecords": 92, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not an admin role

#### GET /admin/history/{id}

Description: Returns the action history for a specific student — all
approval/rejection actions by this admin on this student. Useful to see if a
student was previously rejected then re-approved.

Headers: Authorization: Bearer <token> (admin)

Path Params: id (string) — student's _id

Response **200 OK** :


##### {

```
"data": {
"studentId": "64bf2d3e0c4a...",
"studentName": "Priya Sharma",
"college": "JNTUH",
"branch": "CSE",
"actions": [
{ "action": "REJECTED", "actionAt": "2025-03-28T09:0
0:00Z", "rejectionReason": "Incomplete profile." },
{ "action": "APPROVED", "actionAt": "2025-04-02T10:0
0:00Z", "rejectionReason": null }
]
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Student not within admin's scopes
(^404) No student or no history found

#### POST /admin/compare

Description: Compares two students side-by-side. Admin provides two student
IDs; response returns full profiles + computed summary statistics for both.

Headers: Authorization: Bearer <token> (admin)

Request Body:

```
{ "studentId1": "64bf2d3e0c4a...", "studentId2": "64bf3e4f1
d5b..." }
```
Internal Operations:

```
Fetches both from users_db.users.
Verifies both within admin's scopes[].
```
Computes summary stats from each student's (^) aptitudeHistory[].


Response **200 OK** :

```
{
"data": {
"student1": {
"_id": "64bf2d3e0c4a...",
"name": "Priya Sharma",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": { "status": "APPROVED" },
"stats": { "totalAttempts": 12, "averageScore": 7.8,
"averageTimeTaken": 465, "practiceAttempts": 8, "scheduledA
ttempts": 4 },
"aptitudeHistory": ["..."]
},
"student2": {
"_id": "64bf3e4f1d5b...",
"name": "Rahul Verma",
"college": "JNTUH",
"branch": "CSE",
"yearOfPassing": 2026,
"approvalStatus": { "status": "APPROVED" },
"stats": { "totalAttempts": 9, "averageScore": 6.5,
"averageTimeTaken": 520, "practiceAttempts": 6, "scheduledA
ttempts": 3 },
"aptitudeHistory": ["..."]
}
}
}
```
Errors:

```
Status Reason
```
(^400) Missing or same student IDs
(^401) Token missing/expired
(^403) One or both not in scope
(^404) One or both not found


#### GET /students/testStatus/{testId}

Description: Returns test results of all students who attempted a specific
scheduled test. Scoped to admin's colleges/branches. For class-wide
performance view.

Headers: Authorization: Bearer <token> (admin)

Path Params: testId (string)

Query Params (pagination and sorting):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Students per page
sortBy string score score, (^) timeTaken, (^) name
order string desc asc or desc
Internal Operations:
Fetches exam from (^) exams_db.exams for metadata.
Queries users_db.users for students within scope who have matching
aptitudeHistory[] entry.
Response **200 OK** :
{
"data": {
"testId": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",
"totalStudentsAttempted": 85,
"results": [
{ "studentId": "64bf2d3e0c4a...", "name": "Priya Shar
ma", "branch": "CSE", "score": 23.25, "timeTaken": 2400, "p
ercentage": 77.5, "attemptedAt": "2025-04-05T09:42:00Z" },
{ "studentId": "64bf3e4f1d5b...", "name": "Rahul Verm
a", "branch": "CSE", "score": 21.0, "timeTaken": 2650, "per
centage": 70.0, "attemptedAt": "2025-04-05T09:50:00Z" }
],
"pagination": { "currentPage": 1, "totalPages": 5, "tot
alStudents": 85, "limit": 20 }


##### }

##### }

Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Test not in admin's scope
(^404) No test with this ID

## 5. Super Admin — Platform-Wide Management

Requires (^) role: "SUPER_ADMIN" in JWT. No scope restrictions — full visibility
across the entire platform.

#### GET /admin/tests

Description: Returns all tests SCHEDULED  PRACTICE) created by this Super
Admin from exams_db.exams where createdBy matches Super Admin's _id.

Headers: Authorization: Bearer <token> (super admin)

Query Params (pagination and filtering):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Tests per page
type string all SCHEDULED or PRACTICE
isPublished bool all Filter by published
search string — Search by title
Response **200 OK** :
{
"data": {
"tests": [
{
"_id": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",


```
"type": "SCHEDULED",
"state": "Telangana", "college": "JNTUH", "branch":
"CSE", "yearOfPassing": 2026,
"scheduledWindow": { "start": "2025-04-05T09:00:00
Z", "end": "2025-04-05T11:00:00Z" },
"durationMinutes": 45, "totalQuestions": 30, "total
Marks": 30, "negativeMarking": 0.25, "isPublished": true
},
{
"_id": "66df4f5a2e6c...",
"title": "Quantitative Aptitude — Practice Set 3",
"type": "PRACTICE",
"state": null, "college": null, "branch": null,
"scheduledWindow": null,
"durationMinutes": 45, "totalQuestions": 30, "negat
iveMarking": 0, "shuffleQuestions": true, "shuffleOptions":
true, "isPublished": true
}
],
"pagination": { "currentPage": 1, "totalPages": 3, "tot
alTests": 48, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not a super admin

#### POST /admin/test/create

Description: Creates a new test (scheduled or practice) in exams_db.exams. For
scheduled: access fields + time window required. For practice: access fields
set to null.

Headers: Authorization: Bearer <token> (super admin)

Request Body (scheduled):


##### {

```
"title": "JNTUH CSE 2026 — Mock Aptitude Test 2",
"type": "SCHEDULED",
"state": "Telangana", "college": "JNTUH", "branch": "CS
E", "yearOfPassing": 2026,
"scheduledWindow": { "start": "2025-05-10T09:00:00Z", "en
d": "2025-05-10T11:00:00Z" },
"durationMinutes": 45, "totalQuestions": 30, "totalMark
s": 30, "negativeMarking": 0.25, "isPublished": false,
"questions": [
{ "text": "If 6 men do a task in 12 days, how many days
for 9 men?", "options": ["8","9","10","12"], "correctInde
x": 1, "topic": "Time & Work", "difficulty": "MEDIUM" }
]
}
```
Request Body (practice):

```
{
"title": "Quantitative Aptitude — Practice Set 4",
"type": "PRACTICE",
"state": null, "college": null, "branch": null, "yearOfPa
ssing": null, "scheduledWindow": null,
"durationMinutes": 45, "totalQuestions": 30, "totalMark
s": 30, "negativeMarking": 0,
"shuffleQuestions": true, "shuffleOptions": true, "isPubl
ished": true,
"questions": ["...30 question objects..."]
}
```
Internal Operations:

```
Validates fields based on type.
SCHEDULED scheduledWindow must be future, access fields required.
```
PRACTICE: access fields must be (^) null.
Sets createdBy → Super Admin's _id.
Inserts into exams_db.exams.


Response **201 Created** :

```
{
"message": "Test created successfully.",
"data": { "_id": "67ef5g6h3i7j...", "title": "JNTUH CSE 2
026 — Mock Aptitude Test 2", "type": "SCHEDULED", "isPublis
hed": false, "totalQuestions": 30, "createdBy": "64af1c2e9b
3f..." }
}
```
Errors:

```
Status Reason
```
(^400) Missing fields, question count mismatch
(^401) Token missing/expired
(^403) Not a super admin
(^422) Scheduled window in the past

#### DELETE /admin/test/{testId}

Description: Soft-deletes a test from `exams_db.exams` by setting `isDeleted: true`, `deletedAt: current timestamp`, and `deletedBy: Super Admin's _id`. Only tests created
by this Super Admin (createdBy must match). Students' aptitudeHistory entries
are preserved (history references topic, not test _id).
Headers: Authorization: Bearer <token> (super admin)
Path Params: testId (string)
Response (^) **200 OK** :
{
"message": "Test deleted successfully.",
"data": { "deletedTestId": "67ef5g6h3i7j...", "title": "J
NTUH CSE 2026 — Mock Aptitude Test 2" }
}
Errors:
Status Reason
(^401) Token missing/expired


```
Status Reason
```
(^403) Not super admin or not created by this admin
(^404) No test with this ID

#### GET /admin/users

Description: Returns all students on the platform (basic fields only, no aptitude
history). Super Admin overview dashboard.

Headers: Authorization: Bearer <token> (super admin)

Query Params (pagination and filtering):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^50) Users per page
state string all Filter by state
college string all Filter by college
branch string all Filter by branch
status string all PENDING, APPROVED, REJECTED
search string — Name or email search
Response (^) **200 OK** :
{
"data": {
"students": [
{ "_id": "64bf2d3e0c4a...", "name": "Priya Sharma",
"state": "Telangana", "college": "JNTUH", "branch": "CSE",
"yearOfPassing": 2026, "approvalStatus": { "status": "APPRO
VED" } },
{ "_id": "64bf3e4f1d5b...", "name": "Rahul Verma", "s
tate": "Telangana", "college": "JNTUH", "branch": "CSE", "y
earOfPassing": 2026, "approvalStatus": { "status": "PENDIN
G" } }
],
"pagination": { "currentPage": 1, "totalPages": 20, "to
talStudents": 982, "limit": 50 }


##### }

##### }

Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not a super admin

#### GET /admin/admins

Description: Returns all college admins from users_db.collegeAdmins — name,
scopes, active status.

Headers: Authorization: Bearer <token> (super admin)

Query Params (pagination):

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Admins per page
state string all Filter by state
college string all Filter by college
isActive bool all Filter by status
Response **200 OK** :
{
"data": {
"admins": [
{
"_id": "64af1c2e9b3f...",
"name": "Dr. Ramesh Kumar",
"email": "admin.cse@jntuh.ac.in",
"role": "COLLEGE_ADMIN",
"isActive": true,
"scopes": [
{ "state": "Telangana", "college": "JNTUH", "bran
ch": "CSE" },
{ "state": "Telangana", "college": "JNTUH", "bran


```
ch": "IT" }
],
"createdAt": "2024-06-01T09:00:00Z",
"lastLoginAt": "2025-04-01T08:00:00Z"
}
],
"pagination": { "currentPage": 1, "totalPages": 2, "tot
alAdmins": 34, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not a super admin

#### GET /admin/results

Description: Aggregated results across all scheduled tests created by this
Super Admin. High-level performance metrics per test.

Headers: Authorization: Bearer <token> (super admin)

Query Params:

```
Param Type Default Description
```
page int (^1) Page number
limit int (^20) Tests per page
testId string — Filter specific test
Response **200 OK** :
{
"data": {
"testResults": [
{
"testId": "65cf3e4f1d5b...",
"title": "JNTUH CSE 2026 — Mock Aptitude Test 1",
"type": "SCHEDULED", "college": "JNTUH", "branch":


##### "CSE",

```
"totalStudentsAttempted": 85, "averageScore": 21.3,
"highestScore": 28.75, "lowestScore": 8.5, "averageTimeTake
n": 2350, "averagePercentage": 71.0
},
{
"testId": "66df4f5a2e6c...",
"title": "Quantitative Aptitude — Practice Set 3",
"type": "PRACTICE", "college": null, "branch": nul
l,
"totalStudentsAttempted": 342, "averageScore": 22.
1, "highestScore": 30, "lowestScore": 5, "averageTimeTake
n": 1900, "averagePercentage": 73.7
}
],
"pagination": { "currentPage": 1, "totalPages": 3, "tot
alTests": 48, "limit": 20 }
}
}
```
Errors:

```
Status Reason
```
(^401) Token missing/expired
(^403) Not a super admin

#### POST /admin/add-admin

Description: Creates a new College Admin in users_db.collegeAdmins.

Headers: Authorization: Bearer <token> (super admin)

Request Body:

```
{
"name": "Dr. Lakshmi Devi",
"email": "admin.ece@jntuh.ac.in",
"password": "TempPass@001",
"scopes": [ { "state": "Telangana", "college": "JNTUH",
```

```
"branch": "ECE" } ]
}
```
Internal Operations:

```
Validates no duplicate email.
Hashes password with bcrypt.
```
Creates document: (^) role: "COLLEGE_ADMIN", (^) isActive: true, (^) createdAt: now,
lastLoginAt: null.
Response **201 Created** :
{
"message": "Admin account created successfully.",
"data": {
"_id": "68gf2h4i5j6k...",
"name": "Dr. Lakshmi Devi",
"email": "admin.ece@jntuh.ac.in",
"role": "COLLEGE_ADMIN",
"isActive": true,
"scopes": [ { "state": "Telangana", "college": "JNTUH",
"branch": "ECE" } ],
"createdAt": "2025-04-01T12:00:00Z"
}
}
Errors:
Status Reason
(^400) Missing fields or empty scopes
(^401) Token missing/expired
(^403) Not a super admin
(^409) Email already registered

#### GET /admincred/

Description: Retrieves admin credentials for a specific college + branch. Used
when Super Admin needs to look up login details.


Headers: Authorization: Bearer <token> (super admin)

Query Params:

```
Param Type Required Description
college string Yes e.g. JNTUH
branch string Yes e.g. CSE
```
Response **200 OK** :

```
{
"data": {
"adminId": "64af1c2e9b3f...",
"name": "Dr. Ramesh Kumar",
"email": "admin.cse@jntuh.ac.in",
"college": "JNTUH",
"branch": "CSE",
"isActive": true,
"temporaryPassword": "Auto-generated or last-set temp p
assword"
}
}
```
Errors:

```
Status Reason
```
(^400) Missing college or branch
(^401) Token missing/expired
(^403) Not a super admin
(^404) No admin for this college + branch

#### PUT /admin/del-admin

Description: Deactivates and soft-deletes a College Admin by setting `isActive: false`, `isDeleted: true`, `deletedAt: current timestamp`, and `deletedBy: Super Admin's _id`. Admin cannot log in anymore. Record preserved for audit. Identified by college + branch.
Headers: Authorization: Bearer <token> (super admin)
Request Body:


```
{ "college": "JNTUH", "branch": "ECE" }
```
Internal Operations:

```
Finds admin in users_db.collegeAdmins matching college + branch in scopes[].
Sets isActive → false, isDeleted → true, deletedAt → current timestamp, deletedBy → Super Admin's _id.
```
Admin login blocked (login checks `isActive` and `isDeleted`).
Students previously managed are unchanged.
Pending students need a new admin assigned.
Response **200 OK** :
{
"message": "Admin account deactivated successfully.",
"data": { "adminId": "68gf2h4i5j6k...", "name": "Dr. Laks
hmi Devi", "email": "admin.ece@jntuh.ac.in", "isActive": fa
lse, "deactivatedAt": "2025-04-01T15:00:00Z" }
}
Errors:
Status Reason
(^400) Missing college or branch
(^401) Token missing/expired
(^403) Not a super admin
(^404) No admin for this college + branch
(^409) Admin already deactivated

## 6. Database Architecture

The platform uses two databases (`users_db` and `exams_db`), with collections and schemas that support soft-delete functionality.

### Database: `users_db`

#### Collection: `collegeAdmins`
Stores admin users (both Super Admin and College Admins).

| Field | Type | Required / Constraints | Description |
|---|---|---|---|
| `name` | String | Required | Full name of the admin |
| `email` | String | Required, Unique | Email address |
| `passwordHash` | String | Required | bcrypt hash of the password |
| `role` | Enum | Default: `'COLLEGE_ADMIN'` | `'COLLEGE_ADMIN'` or `'SUPER_ADMIN'` |
| `isActive` | Boolean | Default: `true` | False blocks login |
| `scopes` | Array | Default: `[]` | List of `{ state, college, branch }` entries. Determines which students/exams a College Admin can access |
| `lastLoginAt` | Date | Default: `null` | Tracks last login time |
| `isDeleted` | Boolean | Default: `false` | Soft-delete flag |
| `deletedAt` | Date | Default: `null` | Timestamp of deletion |
| `deletedBy` | ObjectId | Default: `null` | ID of the admin who performed the soft-delete |

#### Collection: `users`
Stores student accounts.

| Field | Type | Required / Constraints | Description |
|---|---|---|---|
| `name` | String | Required | Full name of the student |
| `email` | String | Required, Unique | Email address |
| `passwordHash` | String | Required | bcrypt hash of the password |
| `role` | String | Default: `'STUDENT'` | Immutable role field |
| `state` | String | Required | Student's state |
| `college` | String | Required | Student's college |
| `branch` | String | Required | Student's branch (e.g., `'CSE'`) |
| `yearOfPassing` | Number | Required | Year of graduation |
| `approvalStatus` | Object | Default: `PENDING` | `{ status: Enum('PENDING', 'APPROVED', 'REJECTED'), approvedBy: ObjectId, approvedAt: Date, rejectionReason: String }` |
| `aptitudeHistory` | Array | Default: `[]` | List of `{ topic, score, timeTaken, type, examId, attemptedAt }`. Capped at latest 50 attempts using mongodb $slice |
| `otpHash` | String | Default: `null` | Temporary bcrypt hash for password reset |
| `otpExpiry` | Date | Default: `null` | Expiry time for the OTP |
| `lastLoginAt` | Date | Default: `null` | Tracks last login time |
| `isDeleted` | Boolean | Default: `false` | Soft-delete flag |
| `deletedAt` | Date | Default: `null` | Timestamp of deletion |
| `deletedBy` | ObjectId | Default: `null` | ID of the admin who performed the soft-delete |

### Database: `exams_db`

#### Collection: `exams`
Stores both SCHEDULED and PRACTICE exams.

| Field | Type | Required / Constraints | Description |
|---|---|---|---|
| `title` | String | Required | Exam title |
| `type` | Enum | Required | `'SCHEDULED'` or `'PRACTICE'` |
| `createdBy` | ObjectId | Required | ID of the Super Admin who created the exam |
| `state` | String | Default: `null` | Access control (null for Practice) |
| `college` | String | Default: `null` | Access control (null for Practice) |
| `branch` | String | Default: `null` | Access control (null for Practice) |
| `yearOfPassing` | Number | Default: `null` | Access control (null for Practice) |
| `scheduledWindow` | Object | Default: `null` | `{ start: Date, end: Date }` (null for Practice) |
| `durationMinutes` | Number | Required, Default: `45` | Exam duration in minutes |
| `totalQuestions` | Number | Required, Default: `30` | Number of questions |
| `totalMarks` | Number | Required, Default: `30` | Total marks available |
| `negativeMarking` | Number | Default: `0` | `0.25` for Scheduled, `0` for Practice |
| `shuffleQuestions`| Boolean | Default: `false` | True for Practice exams |
| `shuffleOptions` | Boolean | Default: `false` | True for Practice exams |
| `isPublished` | Boolean | Default: `false` | False until admin publishes |
| `questions` | Array | Required | Array of Question objects (see below) |
| `isDeleted` | Boolean | Default: `false` | Soft-delete flag |
| `deletedAt` | Date | Default: `null` | Timestamp of deletion |
| `deletedBy` | ObjectId | Default: `null` | ID of the admin who performed the soft-delete |

#### Sub-Collection details `exams.questions`

| Field | Type | Required / Constraints | Description |
|---|---|---|---|
| `text` | String | Required | Question text |
| `options` | Array | Required (length: 4)| Exactly 4 string options |
| `correctIndex`| Number | Min: 0, Max: 3 | 0-3. **NEVER POSTED to client API** |
| `explanation` | String | Default: `''` | Explanation shown after submission |
| `topic` | String | Required | E.g. `'Time & Work'` |
| `difficulty` | Enum | Default: `'MEDIUM'` | `'EASY'`, `'MEDIUM'`, or `'HARD'` |

### Key Security Rules

```
Passwords are never stored as plain text — Every password is hashed
using bcrypt before storing.
Students cannot log in until approved — approvalStatus starts as PENDING.
Login blocked until APPROVED.
Exam questions are sent one at a time — The backend never sends all 30
questions at once. Prevents cheating.
Scheduled exams enforce the time window — Access denied before start
or after end.
Admin scope controls everything they see — A college admin at JNTUH
CSE cannot see students from another college.
Test history uses one unified list — All attempts (practice + scheduled)
stored in a single aptitudeHistory[] with a type label.
```
## 7. Endpoint Summary Table

```
Method Endpoint Role Description
```
```
POST /auth/signup Public
```
```
Register student
(status →
PENDING
```
```
POST /auth/login Public Login^ (blocked^ if
not APPROVED
```

Method Endpoint Role Description

```
PUT /auth/update-profile Student Update^ profile
fields
```
```
GET /profile Student Full^ profile^ +
aptitudeHistory
```
```
POST /auth/update-password Student
```
```
Request OTP for
password
change
POST /auth/update-pass/otp Student Verify^ OTP^ +^ set
new password
GET /article/{topic} Student Read^ study
article
```
```
GET /exam/{topic} Student
```
```
Get practice
exam  1 question
at a time)
```
```
POST /exam/submit/{topic} Student
```
```
Submit practice
exam →
aptitudeHistory
GET /result/exam/{topic} Student Get^ practice
result(s)
```
```
GET /test/{testId} Student
```
```
Get scheduled
test (access +
time enforced)
```
```
POST /test/{testId}/submit Student
```
```
Submit
scheduled test
→
aptitudeHistory
```
```
GET /test/{testId}/results Student Get^ scheduled
test result
POST /admin/login Admin Admin login
```
```
GET /admin/approvals Admin
```
```
Pending
students
(paginated)
```
```
GET /admin/rejections Admin
```
```
Rejected
students
(paginated)
POST /admin/approve/{id} Admin Approve →
APPROVED,
```

Method Endpoint Role Description
removed from
pending

```
POST /admin/approve/all Admin Bulk^ approve^ all
pending
```
```
POST /admin/reject/{id} Admin
```
```
Reject →
REJECTED +
reason, moved to
rejections
```
```
POST /admin/reject/all Admin
```
```
Bulk reject all
pending +
reason
GET /admin/student/{id} Admin Full^ student
profile + history
```
```
GET /admin/history Admin
```
```
Admin's action
history
(paginated)
GET /admin/history/{id} Admin History^ for
specific student
```
```
POST /admin/compare Admin
```
```
Compare two
students side-
by-side
```
```
GET /students/testStatus/{testId} Admin
```
```
All students'
results for a test
(paginated)
GET /admin/tests Super Admin All^ tests^ created
(paginated)
```
```
POST /admin/test/create Super Admin
```
```
Create
scheduled or
practice test
DELETE /admin/test/{testId} Super Admin Soft-delete a
test
```
```
GET /admin/users Super Admin
```
```
All students
(paginated,
filterable)
```
```
GET /admin/admins Super Admin
```
```
All college
admins
(paginated)
```

```
Method Endpoint Role Description
GET /admin/results Super Admin Aggregated^ test
results
```
```
POST /admin/add-admin Super Admin Create^ college
admin
```
```
GET /admincred/ Super Admin
```
```
Get admin
credentials by
college+branch
PUT /admin/del-admin Super Admin Deactivate^ admin
(soft delete)
```
```
Pagination Standard: All list endpoints support page + limit query params
with response format: { currentPage, totalPages, total*, limit }
Database Architecture: users_db → collegeAdmins + users | exams_db → exams
Security: bcrypt passwords · JWT auth · approval gate · scoped queries ·
one-question-at-a-time · time window enforcement
```
user (includes test and exams )
admin
super admin
diff jwt

→frontend react for all
→backend java
→


