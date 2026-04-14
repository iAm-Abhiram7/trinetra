# Trinetra Postman Runner

This folder contains a full Postman setup that can run the API flow end-to-end with generated test data.

## Files

- `Trinetra-Full-Runner.postman_collection.json`
- `Trinetra-Local.postman_environment.json`

## What This Collection Covers

- Student auth endpoints:
  - `POST /auth/signup`
  - `POST /auth/login`
  - `PUT /auth/update-profile`
  - `GET /profile`
  - `POST /auth/update-password`
  - `POST /auth/update-pass/otp`
- Super admin endpoints:
  - `GET /admin/users`
  - `GET /admin/admins`
  - `POST /admin/add-admin`
  - `GET /admincred`
  - `GET /admincred/`
  - `PUT /admin/del-admin`
  - `GET /admin/tests`
  - `POST /admin/test/create` (practice + scheduled)
  - `DELETE /admin/test/{testId}`
  - `GET /admin/results`
- College admin endpoints:
  - `GET /admin/approvals`
  - `GET /admin/rejections`
  - `POST /admin/approve/{id}`
  - `POST /admin/approve/all`
  - `POST /admin/reject/{id}`
  - `POST /admin/reject/all`
  - `GET /admin/student/{id}`
  - `POST /admin/compare`
  - `GET /admin/history`
  - `GET /admin/history/{id}`
  - `GET /admin/testStatus/{testId}`
- Student content/practice/scheduled endpoints:
  - `GET /article/{topic}`
  - `GET /exam/{topic}`
  - `POST /exam/submit/{topic}`
  - `GET /result/exam/{topic}`
  - `GET /test/{testId}`
  - `POST /test/{testId}/submit`
  - `GET /test/{testId}/results`

## How To Run All At Once

1. Start backend on port 8081.
2. Import both files into Postman.
3. Select environment `Trinetra Local`.
4. Open collection `Trinetra API Full Runner`.
5. Click **Run collection**.
6. Run in listed order (default order is already correct).

## Test Data Strategy

- Every run auto-generates unique emails and titles using `runId`.
- Scope values are prefilled as:
  - `state = Telangana`
  - `college = JNTUH`
  - `branch = CSE`
  - `year_of_passing = 2026`
- Tokens and IDs are captured automatically into collection variables.

## OTP Step Note

- `POST /auth/update-pass/otp` is included.
- API currently logs OTP server-side and does not return it in response body.
- To make OTP step return 200, copy OTP from backend logs and set environment variable `studentOtp` before or during run.
- If OTP is not set, this request is still expected to continue with non-200 status (allowed by tests).
