# AI Resume Analyzer & Career Assistant

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An AI-powered, 100% offline resume analysis platform built with Spring Boot and modern Vanilla JavaScript. Parse PDF and DOCX resumes, extract candidate profiles, audit ATS readiness, run job description matching, check writing grammar, track resume history with version control, and download comprehensive career reports without external API keys.

---

## 🌟 Key Features

### 📄 1. Document Parsing & Text Extraction (Offline)
- Supports **PDF** (`.pdf`) and **Word** (`.docx`) document uploads up to 10MB.
- Uses **Apache PDFBox** and **Apache POI** for complete local, privacy-focused text parsing.
- Extracts candidate name, email, phone number, LinkedIn, GitHub, and portfolio URLs.
- Parses education history, work experience, technical skills, and key projects automatically.

### 👤 2. Career Profile Dashboard
- Calculates interactive **Profile Completeness Percentage**.
- Supports inline profile management (add/remove skills, education, work experience, projects).
- Persists user career profile state locally in an embedded **H2 Database**.

### 🛡️ 3. Resume Strength Report (Phase 5)
- **Resume Strengths**: Evaluates word count suitability, skill density, complete contact headers, web presence, quantifiable metrics, and readability scanning.
- **Weaknesses**: Identifies word count anomalies, passive voice usage, weak verb phrasing (e.g. "responsible for"), missing summaries, and multi-column layout pipe clutter.
- **Missing Sections**: Audits standard resume headers (Summary, Work Experience, Skills, Education, Projects, Certifications, Achievements).
- **ATS Readiness**: Displays ATS Score, compatibility rating (`EXCELLENT`, `GOOD`, `NEEDS_IMPROVEMENT`, `POOR`), and structural readiness breakdown.
- **Resume Rating**: Calculates overall resume score out of 100 with letter grades (`A+ / Exceptional`, `A / Outstanding`, `B+ / Strong`, `B / Good`, `C / Fair`).
- **Improvement Suggestions**: Actionable recommendations categorized by section.

### ✍️ 4. Grammar & Writing Checker (Phase 5)
- **Grammar Errors**: Checks spacing anomalies (double spaces), missing bullet point closing periods, and article misuses ("a" vs "an").
- **Spelling Mistakes**: Scans technical and dictionary terms for typos and provides correction cards.
- **Readability Level**: Computes Flesch-Kincaid index, reading complexity level, and grade level assessment.
- **Writing Style**: Audits action verbs count vs passive voice constructs and rates overall style.
- **Professional Language & Tone**: Checks casual vocabulary (e.g., "cool", "stuff", "basically"), formal tone grade, and replacement suggestions.
- **Sentence Structure**: Measures average sentence length, total sentences, run-on sentence warnings, and flow rating.

### 📜 5. Resume History & Version Control (Phase 5)
- **Stored Uploaded Resumes**: Complete list of uploaded files, formats, file sizes, and parsing status.
- **Resume Versions**: Automatically tracks version badges (`v1.0`, `v2.0`, `v3.0`, etc.) based on upload sequence.
- **Upload Dates**: Displays precise upload timestamps.
- **View History**: Click "View Report" on any historical resume to launch the 8-tab Report Drawer Modal.
- **Delete Resume**: Single-click deletion that removes the database entry and deletes the local file from disk.
- **Download Previous Reports**: Download Strength, Grammar, ATS, or AI reports directly in JSON/TXT format.

### 🎯 6. Job Description Matching & ATS Checker
- Paste or upload Job Description (`.txt`) files to compute **Resume Match Percentage**.
- Breakdown metrics: Skill Match %, Experience Match %, Education Match %, and Overall Compatibility.
- Identifies missing required skills and industry keywords.
- Evaluates ATS formatting, structure, and keyword density.

### 💡 7. AI Suggestions & Advanced Skills Analysis
- **AI Suggestions**: Provides recommendations across Summary, Experience, Projects, Skills, Education, Certifications, Achievements, Action Verbs, Keywords, and Industry Improvements.
- **Skills Analysis**: Categorizes skills into Languages, Frameworks, Databases, Cloud, DevOps, AI/ML, Tools, and Soft Skills. Displays skill distribution charts, strength progress graphs, missing gaps, and next-step recommendations.

### 🔍 8. Real-Time Search & Theme Toggle
- **Top Navbar Search**: Real-time filtering across uploaded filenames, parsing status, skills, and education.
- **Light & Dark Mode**: Persistent theme switching with custom space-obsidian and deep indigo dark mode styles.

### ⚖️ 9. Resume Version Comparison (Feature 13)
- Compare two resume versions side-by-side (Resume Version 1 vs Resume Version 2).
- Generates:
  - **ATS Score Delta**: Score improvement from Version 1 to Version 2 (`+15%`).
  - **Added Skills**: Technical skills added in the newer version.
  - **Removed Skills**: Omitted or removed skills.
  - **Keyword Difference**: New keywords identified vs removed keywords.
  - **Improvement Rate**: Overall optimization score growth percentage.

### 🎨 11. Resume Templates (Feature 17)
- Generate & preview resumes in **5 distinct professional layouts**:
  1. **ATS Friendly Resume**: Simple single-column layout optimized for ATS parsing.
  2. **Professional Resume**: Corporate-style design with strong section hierarchy.
  3. **Modern Resume**: Modern visual hierarchy with subtle accents & skill badges.
  4. **Minimal Resume**: Clean whitespace layout focusing on pure content.
  5. **Creative Resume**: Vibrant dual-column layout with dark sidebar for tech & creative profiles.
- Switch between templates live without losing resume data.
- Export as standalone HTML, copy formatted text, or print to PDF.

### 💼 12. LinkedIn Profile Analyzer (Feature 18)
- Comprehensive LinkedIn audit analyzing Profile Completeness, Headline positioning, About section, Skills density, Experience metrics, and Certifications.
- Calculates **LinkedIn Score out of 100** with category breakdown.
- Provides actionable, highly specific improvement suggestions (e.g., headline keyword additions, metric formatting).

### 🐙 13. GitHub Profile Analyzer (Feature 19)
- Integrates with live GitHub REST API v3 to retrieve repositories, languages, stars, forks, and followers.
- Evaluates Repository Quality, Programming Language diversity, Commit Activity, Contribution Impact, and Documentation Quality.
- Calculates **GitHub Score out of 100** with category breakdown.
- Graceful API rate-limit and offline fallback handling.
- Provides tailored recommendations (e.g. adding READMEs, pinning top 4-6 projects, adding demo links, archiving test repos).

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Java 21, Spring Boot 3.3.1 (Spring Web, Spring Data JPA, Validation) |
| **Document Parsers** | Apache PDFBox 3.0.2, Apache POI 5.2.5 (poi-ooxml) |
| **Database** | H2 Database (File-persisted `resume_analyzer.mv.db`, MySQL mode) |
| **Frontend** | HTML5, Vanilla JavaScript (ES6+), Vanilla CSS3 (CSS Variables, Flexbox/Grid) |
| **Icons & Fonts** | FontAwesome 6.4.0, Google Fonts (Outfit) |
| **Build Tool** | Apache Maven |

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/resumes/upload` | Upload and parse a PDF/DOCX resume file |
| `GET` | `/api/resumes/history` | List all uploaded resumes with version details |
| `GET` | `/api/resumes/{id}` | Get structured details for a specific resume |
| `GET` | `/api/resumes/{id}/strength-report` | Get Resume Strength Report JSON |
| `GET` | `/api/resumes/{id}/grammar-report` | Get Grammar & Writing Checker Report JSON |
| `GET` | `/api/resumes/{id}/ats` | Get ATS compatibility score report |
| `GET` | `/api/resumes/{id}/ai-analysis` | Get linguistic and structural AI analysis |
| `GET` | `/api/resumes/{id}/suggestions` | Get section-by-section AI improvement suggestions |
| `GET` | `/api/resumes/{id}/skills-analysis` | Get skills categorizations, distribution & strength graphs |
| `GET` | `/api/resumes/{id}/download-report/{type}` | Download report file (`strength`, `grammar`, `ats`, `ai`) |
| `POST` | `/api/resumes/{id}/match` | Compare resume against a target Job Description |
| `POST` | `/api/resumes/compare` | Compare two resume versions side-by-side |
| `POST` | `/api/resumes/cover-letter` | Generate AI Cover Letter tailored to company & role |
| `GET` | `/api/templates` | Get list of available 5 resume templates |
| `POST` | `/api/templates/render` | Render resume template HTML preview |
| `POST` | `/api/analyzer/linkedin` | Audit LinkedIn profile payload & generate score + suggestions |
| `POST` | `/api/analyzer/github` | Audit GitHub profile via REST API & generate score + recommendations |
| `DELETE` | `/api/resumes/{id}` | Delete resume record and stored local file |
| `GET` | `/api/profile` | Get active user profile |
| `PUT` | `/api/profile` | Update active user profile details |

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 JDK** or higher installed.
- Maven Wrapper (`mvnw.cmd` / `mvnw`) included in the project repository.

### Run Locally
1. Clone or open the repository folder:
   ```bash
   cd "d:\AI RESUMER"
   ```

2. Compile and run using Maven Wrapper:
   - **Windows (PowerShell / CMD):**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. Open your browser and navigate to:
   ```text
   http://localhost:8080
   ```

---

## 📊 Project Status

- **Current Status**: **Phase 8 Completed**
- **Offline Engine**: 100% functional.
- **Features Included**: Document Parsing, Career Profile, ATS Checker, Job Matcher, AI Suggestions, Skills Analysis, Resume Strength Report, Grammar & Writing Checker, Theme Toggle, Real-Time Search, Resume History & Report Downloads, Full-Screen Modal Drawer, **Resume Comparison (Feature 13)**, **AI Cover Letter Generator (Feature 14)**, **Resume Templates (Feature 17)**, **LinkedIn Profile Analyzer (Feature 18)**, and **GitHub Profile Analyzer (Feature 19)**.