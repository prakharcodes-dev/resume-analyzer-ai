# resume-analyzer-ai

AI-powered resume analysis platform that parses resumes, extracts candidate information, evaluates profile completeness, and provides ATS-ready insights with a scalable Spring Boot architecture.

# AI Resume Analyzer

AI Resume Analyzer is a web application that helps users upload resumes, automatically extract important information, and build a structured career profile.

The system processes PDF and DOCX resumes, identifies contact information, skills, education, experience, and projects, then organizes the data into an easy-to-manage dashboard. The application is built using Spring Boot, Java, HTML, CSS, and JavaScript, laying the foundation for advanced ATS scoring and AI-powered career assistance.

---

# Features

## Resume Upload

- Upload PDF and DOCX resumes
- Drag-and-drop file support
- Real-time upload feedback
- File validation
- File size restrictions

## Resume Parsing

- Extracts text from resumes
- Detects candidate contact details
- Identifies education history
- Extracts work experience
- Detects technical skills
- Captures project information

## Career Profile Dashboard

- Automatically populates profile information
- Displays profile completeness score
- Allows manual editing of extracted details
- Stores user information persistently

## ATS Readiness

- Evaluates profile completeness
- Provides a profile score
- Highlights missing information

---

# Current Features (Phase 1)

- Resume Upload (PDF & DOCX)
- Drag-and-drop Upload
- Resume Text Extraction
- Contact Information Extraction
- Education Extraction
- Experience Extraction
- Skills Extraction
- Projects Extraction
- User Dashboard
- Profile Management
- Persistent Database Storage

---

# Technology Stack

## Frontend

- HTML5
- CSS3
- JavaScript

## Backend

- Java
- Spring Boot
- Spring Data JPA

## Resume Parsing

- Apache PDFBox
- Apache POI

## Database

- H2 Database

## Build Tool

- Maven

## Version Control

- Git
- GitHub

---

# Project Structure

```text
src/
├── controller/
├── service/
├── repository/
├── model/
└── resources/
    ├── static/
    ├── application.properties
    └── schema.sql
```

---

# Current Implementation

- Resume Upload System
- PDF & DOCX Parsing
- Contact Information Extraction
- Skills Detection
- Education Extraction
- Experience Extraction
- Dashboard Statistics
- Profile Management

---

# Future Roadmap

The project is being developed incrementally.

Upcoming features include:

- AI-powered Resume Feedback
- ATS Score Generation
- Resume Improvement Suggestions
- Resume-Job Matching
- AI Resume Analysis
- Job Description Matching
- AI Interview Coach
- Resume Comparison
- Skill Gap Analysis
- Career Recommendations
- Career Roadmap
- AI Cover Letter Generator
- Recruiter Dashboard
- Admin Dashboard
- Portfolio Analyzer
- GitHub Profile Analyzer
- LinkedIn Profile Analyzer
- Resume Optimization Insights

---

# Getting Started

## Prerequisites

- Java 17+
- Maven
- IntelliJ IDEA or VS Code

## Run the Project

```bash
mvn spring-boot:run
```

Open your browser:

```
http://localhost:8080
```

---

# Project Status

**Current Version:** Phase 1 Completed

Implemented Modules:

- Dashboard
- Resume Upload
- Resume Parsing
- Profile Management

Future AI-powered modules will be added in upcoming releases.