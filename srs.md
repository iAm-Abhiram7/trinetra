# SRS

## Software Requirements Specification (SRS)

## Student Assessment & Learning Platform

## 1. Introduction

### 1. 1 Purpose

This document provides a detailed description of the requirements for the Student
Assessment & Learning Platform. It outlines system functionalities, user roles, and
constraints across three development phases.

### 1. 2 Scope

The platform enables students to prepare for aptitude and technical assessments
through articles, practice exams, and performance comparison. It also supports
administrative functionalities for test creation, scheduling, and access control.

### 1. 3 Definitions

```
Student: End user who accesses learning content and tests.
Admin: User responsible for managing tests and approvals.
College Admin: Admin responsible for managing access for a specific college.
Platform: The Student Assessment & Learning System.
```
## 2. Overall Description

### 2. 1 Product Perspective

The system is a web-based platform that provides:
Learning resources (articles)
Practice tests and scheduled exams


```
Performance comparison features
Administrative controls for test and user management
```
### 2. 2 User Classes and Characteristics

```
Students
Basic technical knowledge
Access via browser
Admins
Manage tests and users
Moderate system usage
College Admins
Approve students from specific colleges
```
### 2. 3 Assumptions and Dependencies

```
Users have internet access
Valid authentication system exists
Secure environment for exams is enforced
```
## 3. System Features

## Phase 1 : Core Student Features

### 3. 1 User Authentication

```
Students can:
Sign up
Log in
Log out
Secure authentication must be implemented.
```

### 3. 2 Article Access

```
Students can read aptitude-related articles.
Articles should be categorized and searchable.
```
### 3. 3 College Dashboard

```
Each student has access to a personalized college dashboard.
Dashboard displays:
Performance metrics
Test history
```
### 3. 4 Performance Comparison

```
Students can compare performance with another valid username.
Comparison includes:
Scores
Rankings
Attempt statistics
```
### 3. 5 Practice Test

```
Students can take practice exams:
30 questions
45 - minute time limit
Auto-submit after time expires.
```
### 3. 6 Exam Security

```
Prevent:
Copy-paste actions
Screenshot attempts (as much as browser allows)
Use:
```

```
Tab switch detection
Full-screen enforcement (optional)
```
### 3. 7 Scheduled Tests

```
Ability to create scheduled tests for targeted users.
Features:
Time-bound availability
User-specific access
```
## Phase 2 : Administrative Features

### 3. 8 Admin Panel

```
Admins can:
Create tests
Assign tests to specific colleges
Manage test schedules
```
### 3. 9 College-wise Approval System

```
College admins can:
Approve or reject student registrations
Only approved students can log in.
```
## Phase 3 : Advanced Features

### 3. 10 DSA Module

```
Students can:
Learn Data Structures & Algorithms
Solve coding problems
```

### 3. 11 Online Compiler

```
Integrated coding environment:
Supports multiple programming languages
Compile and run code
Display output/errors
```
## 4. Functional Requirements

### 4. 1 Authentication

```
System must validate user credentials.
Passwords must be encrypted.
```
### 4. 2 Test Management

```
Admin can create, update, delete tests.
Tests must support:
MCQs
Timers
Scoring logic
```
### 4. 3 User Management

```
System must support:
Student registration
Admin approvals
Role-based access
```
## 5. Non-Functional Requirements

### 5. 1 Performance


```
System should handle multiple concurrent users.
Response time < 2 seconds for most operations.
```
### 5. 2 Security

```
Secure authentication and authorization
Prevent cheating during exams
Data encryption
```
### 5. 3 Usability

```
Simple and intuitive UI
Responsive design for multiple devices
```
### 5. 4 Scalability

```
System should scale with increasing users and tests.
```
### 5. 5 Reliability

```
System uptime ≥ 99 %
```
## 6. Constraints

```
Browser limitations for preventing screenshots
Dependency on internet connectivity
Security measures may not be 100 % foolproof
```
## 7. Future Enhancements

```
AI-based performance analytics
Personalized learning recommendations
Leaderboards and gamification
```
## 8. Conclusion


This SRS defines a scalable, secure, and feature-rich platform for student
assessment and preparation. The phased approach ensures incremental delivery
of features while maintaining system stability and usability.


