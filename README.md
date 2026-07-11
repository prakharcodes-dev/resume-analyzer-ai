 # resume-analyzer-ai

AI-powered resume analysis platform that parses resumes, extracts candidate information, evaluates profile completeness, and provides ATS-ready insights with a scalable Spring Boot architecture.

# AI Resume Analyzer

AI Resume Analyzer is a web application that helps users upload resumes, automatically extract important information, build a structured career profile, check ATS compatibility, and run job-description matching.

The system processes PDF and DOCX resumes, identifies contact information, skills, education, experience, and projects, and performs detailed linguistic audits to provide actionable optimization suggestions. The application is built using Spring Boot, Java, HTML, CSS, and JavaScript, running completely offline for maximum data privacy.

---

# Features

## Resume Upload
- Upload PDF and DOCX resumes
- Drag-and-drop file support
- Real-time upload feedback
- File validation (MIME-type check)
- File size restrictions (Max 10MB)

## Resume Parsing
- Extracts text from resumes
- Detects candidate contact details (Email, Phone, LinkedIn, GitHub, Portfolio)
- Identifies education history (Institution, Degree, Dates, GPA)
- Extracts work experience (Title, Company, Dates, Responsibilities)
- Detects technical skills & maps project information

## Career Profile Dashboard
- Automatically populates profile information from parsing
- Displays profile completeness score
- Allows manual editing of extracted details (Add/delete skills, experience, education, projects)
- Stores user information persistently

## ATS Resume Checker (Phase 2)
- Generates an Overall ATS Score (0–100)
- Formatting Score (Evaluates layout density, files size, and bullet utilization)
- Resume Structure Score (Verifies presence of logical standard headers)
- Keyword Score (Calculates technical skill keyword density)
- Section Completeness Score (Verifies contact and core experience details)
- Readability Score (Approximates text scanning ease using sentence structures)
- Overall ATS Compatibility Rating (Excellent, Good, Needs Improvement, Poor)

## Job Description Matching (Phase 2)
- Paste or upload Job Description files (`.txt` format)
- Computes Resume Match Percentage
- Calculates individual Skill Match, Experience Match, and Education Match percentages
- Extracts Missing Skills (skills present in the JD but not in the resume)
- Extracts Missing Keywords (industry methodologies and soft skills)
- Outputs an Overall Compatibility Score

## AI Resume Analysis (Phase 2)
- Structural Analysis (Assess logical section layouts)
- Spelling & Grammar Checks (Scans spacing anomalies and runs typos against a dictionary)
- Formatting Feedback (Flags column separators or file formatting issues)
- Professional Tone Evaluation (Identifies casual phrasing and suggests replacements)
- Readability Metrics & Resume Length Analysis (Determines word count and flags short or long layouts)
- Duplicate Content Scanner (Identifies repeated bullet points/paragraphs)
- Style Improvements:
  - Weak Sentences Auditor (Flags sentences lacking strong action verbs)
  - Passive Voice Auditor (Highlights passive verb constructs with active alternatives)

---

# Current Features (Phase 1 & Phase 2)

### Phase 1 (Core Infrastructure)
- Resume Upload (PDF & DOCX)
- Drag-and-drop Upload
- Resume Text Extraction
- Contact Information Extraction
- Education, Experience, Skills & Projects Extraction
- User Dashboard & Profile Management
- Persistent Database Storage (H2 File-based)

### Phase 2 (Analysis & Audit Engines)
- **ATS Checker**: Local rule-based score calculation and compatibility categorization.
- **Job Description Matcher**: Text matcher, `.txt` file reader, and comparative metrics calculator.
- **AI Resume Auditor**: Sentence structure parser, typo check, passive voice finder, and style optimizer.
- **Frontend Tab System**: Integrated ATS score rings, match indicators, JD file selector, and grammar check cards directly into a 5-tab Report Drawer Modal.

---

# Technology Stack

## Frontend
- HTML5
- CSS3 (Custom responsive styling, progress rings, breakdown progress bars)
- JavaScript (Asynchronous XMLHttpRequests, file readers, tab toggles)

## Backend
- Java 21
- Spring Boot 3.3
- Spring Data JPA

## Resume Parsing
- Apache PDFBox
- Apache POI (POI-OOXML)

## Database
- H2 Database (File-persisted, MySQL compatibility mode)

## Build Tool
- Maven

---

# Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/career/resumeanalyzer/
│   │       ├── controller/            # ResumeController, ProfileController
│   │       ├── service/               # ResumeParserService, ResumeAnalysisService
│   │       ├── repository/            # UploadedResumeRepository, UserRepository
│   │       ├── model/                 # UploadedResume, UserProfile, User
│   │       └── ResumeAnalyzerApplication.java
│   └── resources/
│       ├── static/                    # Frontend Web Assets
│       │   ├── css/                   # Stylesheets (styles.css)
│       │   ├── js/                    # Application client logic (app.js)
│       │   └── index.html             # Single-page layout
│       ├── application.properties      # Port & DB configurations
│       └── schema.sql                 # Database table schema
```

---

# Getting Started

## Prerequisites
- Java 21 or higher
- Maven (or use the packaged Maven Wrapper `mvnw`)

## Run the Project
1. Open a terminal in the project's root folder:
   ```bash
   cd "d:\AI RESUMER"
   ```
2. Start the Spring Boot application using the Maven Wrapper:
   - **Windows (PowerShell/CMD):**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Mac/Linux:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. Open your browser and navigate to the application port:
   ```text
   http://localhost:8080
   ```

---

# Project Status

**Current Version:** Phase 2 Completed

Implemented Modules:
- Dashboard Statistics & File Upload
- Multithreaded Text Extraction
- Interactive Profile Editing
- ATS Checker Metrics
- Job Description Matcher
- Local AI Grammatical & Style Audits

Future AI-powered modules (such as Interview Coaching, cover letter generation, and roadmaps) will be added in upcoming releases.
```