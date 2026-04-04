=== STUDENT ASSESSMENT PLATFORM — SUPER ADMIN BACKEND MASTER PROMPT ===

STACK:
- Java 17+
- Spring Boot 3.x
- MongoDB (via Spring Data MongoDB)
- JWT (io.jsonwebtoken / jjwt library)
- BCrypt (Spring Security BCryptPasswordEncoder)
- Swagger / OpenAPI 3 (springdoc-openapi-starter-webmvc-ui)
- Maven (build tool)
- Lombok (reduce boilerplate)

ARCHITECTURE: Layered MVC — Controller → Service → Repository → Model
PACKAGING STYLE: Feature-based packages (not layer-based)

ROOT PACKAGE: com.assessment.platform

=== JWT CLAIMS ARCHITECTURE (CRITICAL — READ CAREFULLY) ===

The platform has THREE distinct user types, each getting their own JWT
with different claims. The JWT is the single source of truth for identity,
role, and access scope.

JWT SECRET: Stored in application.properties as jwt.secret (min 256-bit key)
JWT EXPIRY: Configurable per role — jwt.expiry.student, jwt.expiry.admin, jwt.expiry.superadmin

--- STUDENT JWT CLAIMS ---
{
  "sub": "<student _id>",
  "role": "STUDENT",
  "college": "JNTUH",
  "branch": "CSE",
  "state": "Telangana",
  "yearOfPassing": 2026,
  "approvalStatus": "APPROVED",
  "iat": <issued at>,
  "exp": <expiry>
}

--- COLLEGE ADMIN JWT CLAIMS ---
{
  "sub": "<admin _id>",
  "role": "COLLEGE_ADMIN",
  "isActive": true,
  "scopes": [
    { "state": "Telangana", "college": "JNTUH", "branch": "CSE" },
    { "state": "Telangana", "college": "JNTUH", "branch": "IT" }
  ],
  "iat": <issued at>,
  "exp": <expiry>
}

--- SUPER ADMIN JWT CLAIMS ---
{
  "sub": "<superadmin _id>",
  "role": "SUPER_ADMIN",
  "isActive": true,
  "noScopeRestriction": true,       ← explicit flag, no scope filtering applied
  "platformAccess": "FULL",         ← signals full visibility across platform
  "iat": <issued at>,
  "exp": <expiry>
}

JWT CLAIM EXTRACTION RULES (apply in JwtUtil.java and JwtAuthFilter.java):
- Extract "role" claim first → determines which access rules apply
- If role == "SUPER_ADMIN" → check noScopeRestriction == true AND platformAccess == "FULL"
  → bypass ALL scope filtering in service layer
- If role == "COLLEGE_ADMIN" → extract scopes[] claim as List<Scope>
  → enforce scope filtering on every query
- If role == "STUDENT" → extract college, branch, state, yearOfPassing
  → used for exam access control
- Any JWT missing expected claims for its role → reject with 401

JWT CLAIM CLASSES (create these in common/security/claims/):

StudentClaims.java:
  String userId, String role, String college, String branch,
  String state, int yearOfPassing, String approvalStatus

AdminClaims.java:
  String userId, String role, Boolean isActive, List<Scope> scopes

SuperAdminClaims.java:
  String userId, String role, Boolean isActive,
  Boolean noScopeRestriction, String platformAccess

JwtUtil.java methods:
  - String generateStudentToken(Student student)
  - String generateAdminToken(CollegeAdmin admin)
  - String generateSuperAdminToken(CollegeAdmin superAdmin)  ← same model, different role
  - StudentClaims extractStudentClaims(String token)
  - AdminClaims extractAdminClaims(String token)
  - SuperAdminClaims extractSuperAdminClaims(String token)
  - String extractRole(String token)     ← always safe to call first
  - String extractUserId(String token)   ← extracts "sub"
  - boolean isTokenValid(String token)
  - boolean isSuperAdmin(String token)   ← checks role + noScopeRestriction + platformAccess
  - boolean isCollegeAdmin(String token) ← checks role + isActive

=== PACKAGE STRUCTURE ===

com.assessment.platform/
├── config/
│   ├── SecurityConfig.java
│   ├── MongoConfig.java
│   └── SwaggerConfig.java
├── common/
│   ├── response/
│   │   └── ApiResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── CustomExceptions.java
│   │       ← ResourceNotFoundException (404)
│   │       ← UnauthorizedException (401)
│   │       ← ForbiddenException (403)
│   │       ← ConflictException (409)
│   │       ← ValidationException (400)
│   │       ← GoneException (410)
│   │       ← TooEarlyException (425)
│   │       ← UnprocessableException (422)
│   └── security/
│       ├── JwtUtil.java
│       ├── JwtAuthFilter.java
│       └── claims/
│           ├── StudentClaims.java
│           ├── AdminClaims.java
│           └── SuperAdminClaims.java
├── superadmin/
│   ├── controller/
│   │   ├── SuperAdminTestController.java    ← test CRUD endpoints
│   │   ├── SuperAdminUserController.java    ← student + admin management
│   │   └── SuperAdminResultController.java  ← aggregated results
│   ├── service/
│   │   ├── SuperAdminTestService.java
│   │   ├── SuperAdminTestServiceImpl.java
│   │   ├── SuperAdminUserService.java
│   │   ├── SuperAdminUserServiceImpl.java
│   │   ├── SuperAdminResultService.java
│   │   └── SuperAdminResultServiceImpl.java
│   └── dto/
│       ├── request/
│       │   ├── CreateExamRequest.java
│       │   ├── CreateQuestionRequest.java
│       │   ├── AddAdminRequest.java
│       │   ├── DeactivateAdminRequest.java
│       │   └── AdminCredLookupRequest.java
│       └── response/
│           ├── ExamSummaryResponse.java
│           ├── ExamDetailResponse.java
│           ├── StudentSummaryResponse.java
│           ├── AdminSummaryResponse.java
│           ├── AdminCredResponse.java
│           ├── AggregatedResultResponse.java
│           ├── TestResultSummaryResponse.java
│           └── DeactivateAdminResponse.java
├── admin/
│   └── ... (from admin master prompt)
├── student/
│   ├── model/Student.java
│   ├── model/embedded/ApprovalStatus.java
│   └── model/embedded/AptitudeHistory.java
└── exam/
    ├── model/Exam.java
    ├── model/embedded/Question.java
    ├── model/embedded/ScheduledWindow.java
    └── repository/ExamRepository.java

=== DATABASE CONFIGURATION ===

Two MongoDB databases under ONE Atlas URI:
- users_db → collegeAdmins collection, users collection
- exams_db → exams collection

MongoConfig.java must define:
  @Primary @Bean MongoTemplate usersMongoTemplate()   ← for users_db
  @Bean MongoTemplate examsMongoTemplate()            ← for exams_db

Repositories annotate with:
  @EnableMongoRepositories(mongoTemplateRef = "examsMongoTemplate")
  on ExamRepository

CollegeAdmin model → @Document(collection = "collegeAdmins")
Student model      → @Document(collection = "users")
Exam model         → @Document(collection = "exams")

=== MODELS (Super Admin context) ===

CollegeAdmin.java (users_db.collegeAdmins):
  @Id String id
  @Field("name") String name
  @Field("email") String email
  @Field("passwordHash") String passwordHash
  @Field("role") String role                   ← "COLLEGE_ADMIN" or "SUPER_ADMIN"
  @Field("isActive") Boolean isActive
  @Field("scopes") List<Scope> scopes          ← empty list for SUPER_ADMIN
  @Field("lastLoginAt") Instant lastLoginAt
  @Field("isDeleted") Boolean isDeleted
  @Field("deletedAt") Instant deletedAt
  @Field("deletedBy") String deletedBy
  @Field("createdAt") Instant createdAt

Scope.java (embedded):
  @Field("state") String state
  @Field("college") String college
  @Field("branch") String branch

Exam.java (exams_db.exams):
  @Id String id
  @Field("title") String title
  @Field("type") String type                    ← "SCHEDULED" or "PRACTICE"
  @Field("createdBy") String createdBy          ← Super Admin's _id
  @Field("state") String state                  ← null for PRACTICE
  @Field("college") String college              ← null for PRACTICE
  @Field("branch") String branch                ← null for PRACTICE
  @Field("yearOfPassing") Integer yearOfPassing ← null for PRACTICE
  @Field("scheduledWindow") ScheduledWindow scheduledWindow ← null for PRACTICE
  @Field("durationMinutes") Integer durationMinutes
  @Field("totalQuestions") Integer totalQuestions
  @Field("totalMarks") Integer totalMarks
  @Field("negativeMarking") Double negativeMarking ← 0.25 SCHEDULED, 0 PRACTICE
  @Field("shuffleQuestions") Boolean shuffleQuestions ← true for PRACTICE
  @Field("shuffleOptions") Boolean shuffleOptions     ← true for PRACTICE
  @Field("isPublished") Boolean isPublished
  @Field("questions") List<Question> questions
  @Field("isDeleted") Boolean isDeleted
  @Field("deletedAt") Instant deletedAt
  @Field("deletedBy") String deletedBy

ScheduledWindow.java (embedded):
  @Field("start") Instant start
  @Field("end") Instant end

Question.java (embedded):
  @Field("text") String text
  @Field("options") List<String> options       ← exactly 4
  @Field("correctIndex") Integer correctIndex  ← NEVER sent to client
  @Field("explanation") String explanation
  @Field("topic") String topic
  @Field("difficulty") String difficulty       ← "EASY", "MEDIUM", "HARD"

Student.java (users_db.users):
  @Id String id
  @Field("name") String name
  @Field("email") String email
  @Field("passwordHash") String passwordHash
  @Field("role") String role
  @Field("state") String state
  @Field("college") String college
  @Field("branch") String branch
  @Field("yearOfPassing") Integer yearOfPassing
  @Field("approvalStatus") ApprovalStatus approvalStatus
  @Field("aptitudeHistory") List<AptitudeHistory> aptitudeHistory
  @Field("isDeleted") Boolean isDeleted
  @Field("createdAt") Instant createdAt

ApprovalStatus.java (embedded):
  @Field("status") String status              ← "PENDING","APPROVED","REJECTED"
  @Field("approvedBy") String approvedBy
  @Field("approvedAt") Instant approvedAt
  @Field("rejectionReason") String rejectionReason

AptitudeHistory.java (embedded):
  @Field("topic") String topic
  @Field("score") Double score
  @Field("timeTaken") Integer timeTaken
  @Field("type") String type                  ← "PRACTICE" or "SCHEDULED"
  @Field("examId") String examId
  @Field("attemptedAt") Instant attemptedAt

=== SECURITY — SUPER ADMIN ENFORCEMENT ===

In SecurityConfig.java, configure route protection:

  .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
  .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
  .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
  .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
  .requestMatchers("/admin/tests/**").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/test/**").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/users").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/admins").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/results").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/add-admin").hasRole("SUPER_ADMIN")
  .requestMatchers("/admincred/**").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/del-admin").hasRole("SUPER_ADMIN")
  .requestMatchers("/admin/**").hasAnyRole("COLLEGE_ADMIN","SUPER_ADMIN")
  .anyRequest().hasRole("STUDENT")

JwtAuthFilter.java:
  - Intercept every request
  - Extract Bearer token from Authorization header
  - Call JwtUtil.extractRole(token)
  - Based on role, extract appropriate claims object
  - For SUPER_ADMIN: verify noScopeRestriction == true AND platformAccess == "FULL"
    → if either missing → reject 401
  - Populate SecurityContextHolder with UsernamePasswordAuthenticationToken
    using userId as principal and role as granted authority
  - Attach full claims object to request attributes so service layer can
    access it without re-parsing JWT
    request.setAttribute("superAdminClaims", superAdminClaims)
    request.setAttribute("adminClaims", adminClaims)
    request.setAttribute("studentClaims", studentClaims)

SuperAdminGuard (utility method in service layer):
  private void verifySuperAdmin(HttpServletRequest request) {
    SuperAdminClaims claims = (SuperAdminClaims) request.getAttribute("superAdminClaims");
    if (claims == null
        || !Boolean.TRUE.equals(claims.getNoScopeRestriction())
        || !"FULL".equals(claims.getPlatformAccess())) {
      throw new ForbiddenException("Super Admin access required");
    }
  }
  Call this as the FIRST line in every Super Admin service method.

=== UNIFIED API RESPONSE FORMAT ===

ApiResponse<T>:
  boolean success
  String message
  T data
  Instant timestamp

Success:  ApiResponse.success("Test created successfully.", data)
Error:    ApiResponse.error("No test with this ID.")
Always wrap in ResponseEntity with correct HTTP status.

=== SWAGGER CONFIGURATION ===

SwaggerConfig.java:
  - OpenAPI bean with title "Student Assessment Platform API"
  - Version "1.0"
  - SecurityScheme named "bearerAuth" → type HTTP → scheme bearer → bearerFormat JWT
  - Global security requirement applying bearerAuth to all endpoints
  - Tags defined:
      "Super Admin — Tests"
      "Super Admin — Users"
      "Super Admin — Admins"
      "Super Admin — Results"

Each controller method must have:
  @Operation(summary = "...", description = "...", tags = {"Super Admin — Tests"})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "..."),
    @ApiResponse(responseCode = "401", description = "Token missing/expired"),
    @ApiResponse(responseCode = "403", description = "Not a super admin"),
    @ApiResponse(responseCode = "404", description = "...")
  })
  @SecurityRequirement(name = "bearerAuth")

=== SUPER ADMIN ENDPOINTS — DETAILED IMPLEMENTATION GUIDE ===

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE A — TEST MANAGEMENT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-1: GET /admin/tests
Controller: SuperAdminTestController
Service: SuperAdminTestService.getAllTests()

Purpose: Returns all tests (SCHEDULED + PRACTICE) created by this Super Admin.

Query: exams_db.exams where createdBy == superAdminId AND isDeleted != true

Query Params (all optional):
  page        int     default 1
  limit       int     default 20
  type        String  "SCHEDULED" or "PRACTICE" — if present, add to query
  isPublished Boolean — if present, add to query
  search      String  — if present, regex match on title field (case-insensitive)

Repository method:
  Page<Exam> findByCreatedByAndIsDeletedNot(
    String createdBy, Boolean isDeleted,
    String type, Boolean isPublished,
    String search, Pageable pageable)
  Use MongoTemplate with Criteria for dynamic query building.

Response DTO (ExamSummaryResponse):
  String id, String title, String type,
  String state, String college, String branch,
  Integer yearOfPassing,
  ScheduledWindowDto scheduledWindow,   ← { start, end }
  Integer durationMinutes, Integer totalQuestions,
  Integer totalMarks, Double negativeMarking,
  Boolean shuffleQuestions, Boolean shuffleOptions,
  Boolean isPublished, String createdBy,
  Instant createdAt

  ⚠ NEVER include questions[] array in this list response.
  ⚠ NEVER include correctIndex anywhere.

Pagination response wrapper:
  List<ExamSummaryResponse> tests
  int currentPage, int totalPages, long totalTests, int limit

HTTP 200 on success.
HTTP 401 if token missing/expired.
HTTP 403 if not super admin.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-2: POST /admin/test/create
Controller: SuperAdminTestController
Service: SuperAdminTestService.createTest()

Purpose: Creates a new SCHEDULED or PRACTICE exam in exams_db.exams.

Request Body: CreateExamRequest
  String title            ← @NotBlank
  String type             ← @NotNull, must be "SCHEDULED" or "PRACTICE"
  String state            ← required if SCHEDULED, must be null if PRACTICE
  String college          ← required if SCHEDULED, must be null if PRACTICE
  String branch           ← required if SCHEDULED, must be null if PRACTICE
  Integer yearOfPassing   ← required if SCHEDULED, must be null if PRACTICE
  ScheduledWindowDto scheduledWindow ← required if SCHEDULED, must be null if PRACTICE
    Instant start         ← must be in the future at time of creation
    Instant end           ← must be after start
  Integer durationMinutes ← @NotNull, default 45
  Integer totalQuestions  ← @NotNull, default 30
  Integer totalMarks      ← @NotNull, default 30
  Double negativeMarking  ← auto-set: 0.25 if SCHEDULED, 0 if PRACTICE
                            (accept from request but validate matches type)
  Boolean shuffleQuestions ← auto-set: false if SCHEDULED, true if PRACTICE
  Boolean shuffleOptions   ← auto-set: false if SCHEDULED, true if PRACTICE
  Boolean isPublished      ← default false
  List<CreateQuestionRequest> questions ← @NotNull, @Size(min=1)

CreateQuestionRequest:
  String text             ← @NotBlank
  List<String> options    ← @Size(min=4, max=4) exactly 4 options
  Integer correctIndex    ← @NotNull, @Min(0), @Max(3)
  String explanation      ← optional, default ""
  String topic            ← @NotBlank
  String difficulty       ← must be "EASY", "MEDIUM", or "HARD"

Validation rules (in service, throw ValidationException if violated):
  IF type == "SCHEDULED":
    - state, college, branch, yearOfPassing must NOT be null
    - scheduledWindow must NOT be null
    - scheduledWindow.start must be after Instant.now()  → else 422
    - scheduledWindow.end must be after scheduledWindow.start → else 422
    - questions.size() must equal totalQuestions → else 400
  IF type == "PRACTICE":
    - state, college, branch, yearOfPassing must all be null
    - scheduledWindow must be null
    - shuffleQuestions and shuffleOptions forced to true regardless of request
    - negativeMarking forced to 0.0 regardless of request

Internal operations:
  1. verifySuperAdmin(request)
  2. Extract superAdminId from SuperAdminClaims.userId
  3. Validate all fields per type rules above
  4. Build Exam entity:
       createdBy = superAdminId
       isDeleted = false
       deletedAt = null
       deletedBy = null
  5. Save to exams_db.exams via examsMongoTemplate
  6. Return ExamSummaryResponse (questions included but WITHOUT correctIndex)

Response HTTP 201:
  ExamDetailResponse:
    String id, String title, String type, Boolean isPublished,
    Integer totalQuestions, String createdBy,
    List<QuestionClientResponse> questions
      ← QuestionClientResponse has NO correctIndex field

HTTP 400 → missing fields or questions count mismatch
HTTP 401 → token missing/expired
HTTP 403 → not a super admin
HTTP 422 → scheduledWindow in the past or end before start

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-3: DELETE /admin/test/{testId}
Controller: SuperAdminTestController
Service: SuperAdminTestService.deleteTest()

Purpose: Soft-deletes a test. Only the Super Admin who created it can delete it.

Path Param: testId (String)

Internal operations:
  1. verifySuperAdmin(request)
  2. Extract superAdminId from claims
  3. Fetch exam from exams_db.exams by testId AND isDeleted != true
     → if not found → 404
  4. Check exam.createdBy == superAdminId
     → if mismatch → 403 ("Not created by this admin")
  5. Set isDeleted = true, deletedAt = Instant.now(), deletedBy = superAdminId
  6. Save back — do NOT physically delete the document
  7. Note: Students' aptitudeHistory entries are preserved
     (history stores topic string, not exam _id reference)

Response HTTP 200:
  { deletedTestId: "...", title: "..." }

HTTP 401 → token missing/expired
HTTP 403 → not super admin or not created by this admin
HTTP 404 → no test with this ID

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE B — PLATFORM-WIDE USER MANAGEMENT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-4: GET /admin/users
Controller: SuperAdminUserController
Service: SuperAdminUserService.getAllStudents()

Purpose: Returns all students on the platform. No scope restriction.
         Basic fields only — no aptitudeHistory in this response.

Query: users_db.users where role == "STUDENT" AND isDeleted != true

Query Params (all optional):
  page     int     default 1
  limit    int     default 50
  state    String  — filter
  college  String  — filter
  branch   String  — filter
  status   String  — "PENDING", "APPROVED", or "REJECTED"
            → filter on approvalStatus.status
  search   String  — regex on name OR email (case-insensitive, $or query)

Repository: Use MongoTemplate with dynamic Criteria building
  - Only add each filter to Criteria if the param is non-null/non-empty
  - For search: Criteria.where("name").regex(search,"i")
                  .orOperator(Criteria.where("email").regex(search,"i"))

Response DTO (StudentSummaryResponse):
  String id, String name, String email,
  String state, String college, String branch,
  Integer yearOfPassing,
  ApprovalStatusDto approvalStatus   ← { status } only, no approvedBy details
  Instant createdAt

  ⚠ No passwordHash, no aptitudeHistory, no otpHash ever in any response.

Pagination:
  List<StudentSummaryResponse> students
  int currentPage, int totalPages, long totalStudents, int limit

HTTP 200, 401, 403.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-5: GET /admin/admins
Controller: SuperAdminUserController
Service: SuperAdminUserService.getAllAdmins()

Purpose: Returns all College Admins. Super Admin overview of all admins.

Query: users_db.collegeAdmins where role == "COLLEGE_ADMIN"
       AND isDeleted != true

Query Params (all optional):
  page      int     default 1
  limit     int     default 20
  state     String  — filter on scopes[].state
  college   String  — filter on scopes[].college
  isActive  Boolean — filter

For state/college filtering use:
  Criteria.where("scopes").elemMatch(
    Criteria.where("state").is(state).and("college").is(college)
  )

Response DTO (AdminSummaryResponse):
  String id, String name, String email,
  String role, Boolean isActive,
  List<ScopeDto> scopes,
  Instant createdAt, Instant lastLoginAt

  ⚠ No passwordHash ever.

Pagination:
  List<AdminSummaryResponse> admins
  int currentPage, int totalPages, long totalAdmins, int limit

HTTP 200, 401, 403.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-6: POST /admin/add-admin
Controller: SuperAdminUserController
Service: SuperAdminUserService.createCollegeAdmin()

Purpose: Creates a new College Admin account in users_db.collegeAdmins.

Request Body: AddAdminRequest
  String name     ← @NotBlank
  String email    ← @NotBlank, @Email
  String password ← @NotBlank, @Size(min=8)
  List<ScopeDto> scopes ← @NotNull, @Size(min=1) — at least one scope required

ScopeDto:
  String state    ← @NotBlank
  String college  ← @NotBlank
  String branch   ← @NotBlank

Internal operations:
  1. verifySuperAdmin(request)
  2. Check no existing document in collegeAdmins with same email
     → if exists → 409
  3. BCrypt hash the password
  4. Build CollegeAdmin entity:
       role = "COLLEGE_ADMIN"
       isActive = true
       createdAt = Instant.now()
       lastLoginAt = null
       isDeleted = false
  5. Save to users_db.collegeAdmins via usersMongoTemplate

Response HTTP 201:
  AdminSummaryResponse (same DTO as GET /admin/admins item)
  + createdAt field

HTTP 400 → missing fields or empty scopes
HTTP 401 → token missing/expired
HTTP 403 → not a super admin
HTTP 409 → email already registered

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-7: GET /admincred/
Controller: SuperAdminUserController
Service: SuperAdminUserService.getAdminCredentials()

Purpose: Retrieve admin account details for a specific college + branch.
         Used when Super Admin needs to look up an admin's login info.

Query Params (both REQUIRED):
  college  String  ← @NotBlank
  branch   String  ← @NotBlank

Query: users_db.collegeAdmins where
  scopes contains element matching { college: college, branch: branch }
  AND isDeleted != true
  → if not found → 404

Response DTO (AdminCredResponse):
  String adminId, String name, String email,
  String college, String branch,
  Boolean isActive

  ⚠ NEVER return passwordHash.
  ⚠ The "temporaryPassword" field mentioned in raw API docs
    should NOT be returned — this is a security risk.
    Instead return only the account details.
    Document this decision in Swagger description.

HTTP 200, 400 (missing params), 401, 403, 404.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-8: PUT /admin/del-admin
Controller: SuperAdminUserController
Service: SuperAdminUserService.deactivateAdmin()

Purpose: Soft-deactivate a College Admin by setting isActive = false.
         Admin login is blocked. Record preserved for audit trail.
         Identified by college + branch combination.

Request Body: DeactivateAdminRequest
  String college  ← @NotBlank
  String branch   ← @NotBlank

Internal operations:
  1. verifySuperAdmin(request)
  2. Query collegeAdmins where scopes contains { college, branch }
     AND isDeleted != true
     → if not found → 404
  3. Check isActive == true
     → if already false → 409 ("Admin already deactivated")
  4. Set isActive = false
  5. Save — do NOT set isDeleted, record stays for audit
  6. Note: students this admin managed are UNCHANGED
     Note: pending students under this admin need manual reassignment
           (log a warning, do not auto-reassign in this endpoint)

Response HTTP 200: DeactivateAdminResponse
  String adminId, String name, String email,
  Boolean isActive (= false), Instant deactivatedAt

HTTP 400 → missing college or branch
HTTP 401 → token missing/expired
HTTP 403 → not a super admin
HTTP 404 → no admin for this college + branch
HTTP 409 → admin already deactivated

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE C — AGGREGATED RESULTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ENDPOINT SA-9: GET /admin/results
Controller: SuperAdminResultController
Service: SuperAdminResultService.getAggregatedResults()

Purpose: High-level performance metrics per test, aggregated across all
         students who attempted that test. Only tests created by this
         Super Admin.

Query Params (all optional):
  page    int     default 1
  limit   int     default 20
  testId  String  — if present, filter to just this one test

Step 1 — Fetch tests:
  Query exams_db.exams where:
    createdBy == superAdminId AND isDeleted != true
    AND type == "SCHEDULED"   ← aggregate SCHEDULED exams primarily
    (if testId provided, add AND _id == testId)

  Also include PRACTICE exams (show both types but compute separately)

Step 2 — For each exam, compute aggregated stats:
  Query users_db.users for students whose aptitudeHistory[]
  contains an entry where examId == exam._id
  (or topic matches for practice — use examId where available)

  Compute from matching aptitudeHistory entries:
    long totalStudentsAttempted  ← count of matching entries
    double averageScore          ← avg of score field
    double highestScore          ← max of score field
    double lowestScore           ← min of score field
    double averageTimeTaken      ← avg of timeTaken field
    double averagePercentage     ← avg of (score/totalMarks * 100)

  Implementation note:
    Use MongoDB aggregation pipeline via MongoTemplate:
    - $match students with aptitudeHistory entry matching examId
    - $unwind aptitudeHistory
    - $match unwound entry to examId
    - $group by null to compute avg, max, min

Response DTO (AggregatedResultResponse per test):
  String testId, String title, String type,
  String college, String branch,
  long totalStudentsAttempted,
  double averageScore, double highestScore,
  double lowestScore, double averageTimeTaken,
  double averagePercentage

Pagination wrapper:
  List<AggregatedResultResponse> testResults
  int currentPage, int totalPages, long totalTests, int limit

HTTP 200, 401, 403.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

=== CODING STANDARDS (STRICTLY FOLLOW) ===

1. Lombok on every model and DTO:
   @Data @Builder @NoArgsConstructor @AllArgsConstructor

2. MongoDB field mapping:
   @Field("fieldName") on every field that differs from Java naming

3. DTOs vs Models:
   - Services ALWAYS return DTOs, never raw model objects
   - Map inside service using a private toDto() method or MapStruct mapper
   - NEVER pass models to controller layer

4. correctIndex rule:
   - Question model has correctIndex field for internal use
   - QuestionClientResponse DTO has NO correctIndex field
   - Every response that includes questions must use QuestionClientResponse
   - Add a comment above the field in Question.java:
     // SECURITY: Never include in any client-facing DTO

5. Password rule:
   - passwordHash is only on the model
   - It is NEVER mapped into any DTO
   - Add a comment: // SECURITY: Never serialize to client

6. Dynamic queries:
   - Use MongoTemplate + Criteria for any query with optional filters
   - Use MongoRepository only for simple findById / save operations

7. Pagination:
   - Use PageRequest.of(page - 1, limit) (page is 1-based in API, 0-based in Spring)
   - Always return currentPage, totalPages, total count, limit in response

8. Timestamps:
   - All stored as Instant in UTC
   - All returned as ISO-8601 String in response DTOs

9. Service layer structure:
   - First line of every super admin service method: verifySuperAdmin(request)
   - Extract superAdminId: claims.getUserId()
   - Clear Javadoc on every public service method

10. Exception hierarchy:
    All custom exceptions extend RuntimeException.
    GlobalExceptionHandler maps each to correct HTTP status.
    Format: { success: false, message: "...", data: null, timestamp: "..." }

=== SWAGGER DOCUMENTATION RULES ===

For every endpoint produce:
  @Tag(name = "Super Admin — Tests") or appropriate group
  @Operation(
    summary = "One line summary",
    description = "Full description including business rules,
                   what gets validated, what gets returned"
  )
  @Parameter(description = "...", example = "...") on every query param
  @ApiResponses with every possible status code documented
  @SecurityRequirement(name = "bearerAuth")
  @RequestBody with @io.swagger.v3.oas.annotations.parameters.RequestBody
    including description and example JSON

=== WHEN I ASK YOU TO BUILD AN ENDPOINT ===

Always produce in this exact order:
  1. Any new Model or Embedded class (if not yet created)
  2. Request DTO (with validation annotations)
  3. Response DTO (with security notes where applicable)
  4. Repository interface / MongoTemplate query method
  5. Service interface method signature + Javadoc
  6. ServiceImpl with full business logic
  7. Controller method with all Swagger annotations
  8. Any config change needed
  9. Sample curl command AND Swagger test JSON body

=== PRIORITY ORDER ===

Phase A — Test Management:
  SA-1: GET /admin/tests
  SA-2: POST /admin/test/create
  SA-3: DELETE /admin/test/{testId}

Phase B — User Management:
  SA-4: GET /admin/users
  SA-5: GET /admin/admins
  SA-6: POST /admin/add-admin
  SA-7: GET /admincred/
  SA-8: PUT /admin/del-admin

Phase C — Results:
  SA-9: GET /admin/results

START: build everything by telling me how to create a spring boot project here and you builing everythinff

=== END OF SUPER ADMIN MASTER PROMPT ===