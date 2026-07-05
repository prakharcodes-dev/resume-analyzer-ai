# resume-analyzer-ai
AI-powered resume analysis platform that parses resumes, extracts candidate information, and provides ATS-ready insights with a scalable Spring Boot architecture.

# AI Resume Analyzer

An AI-powered resume analysis platform built using Spring Boot, Java, HTML, CSS, and JavaScript. The application extracts structured information from PDF and DOCX resumes, helping users organize professional profiles while laying the foundation for advanced ATS scoring and AI-powered career assistance.

---

## Current Features (Phase 1)

- Resume upload (PDF & DOCX)
- Drag-and-drop file upload
- Resume text extraction
- Contact information extraction
- Education extraction
- Experience extraction
- Skills extraction
- Projects extraction
- User dashboard
- Profile management
- Persistent database storage

---

## Tech Stack

### Frontend
- HTML5
- CSS3
- JavaScript

### Backend
- Java
- Spring Boot

### Resume Parsing
- Apache PDFBox
- Apache POI

### Database
- H2 Database

### Build Tool
- Maven

### Version Control
- Git
- GitHub

---

## Project Structure

```
src/
 ├── controller/
 ├── model/
 ├── repository/
 ├── service/
 └── resources/
      ├── static/
      ├── application.properties
      └── schema.sql
```

---

## Future Roadmap

The project is being developed incrementally.

Upcoming features include:

- ATS Score Generation
- AI Resume Analysis
- Resume Improvement Suggestions
- Job Description Matching
- AI Interview Coach
- Resume Comparison
- Career Roadmap
- AI Cover Letter Generator
- Recruiter Dashboard
- Admin Dashboard
- Portfolio Analyzer
- GitHub Profile Analyzer
- LinkedIn Profile Analyzer


## Getting Started

### Prerequisites

- Java 17+
- Maven
- IDE (IntelliJ IDEA or VS Code)

### Run the project

bash
mvn spring-boot:run

Open:
http://localhost:8080

## Project Status

Current Version:

**Phase 1 Completed**

Implemented modules:

- Dashboard
- Resume Upload
- Resume Parsing
- Profile Management

Future AI-powered modules will be added in upcoming releases.
