# AI Resume Analyzer & Career Assistant

🌐 **Live Application**: [https://resume-analyzer-ai-bvpv.onrender.com](https://resume-analyzer-ai-bvpv.onrender.com)

An AI-powered, 100% offline-first resume analysis and career platform built with **Spring Boot 3.3.1 (Java 21)** and modern **Vanilla JavaScript**. Parse PDF and DOCX resumes, extract candidate profiles, audit ATS readiness, run job description matching, check writing grammar, track resume version history, generate tailored cover letters, render 5 professional resume templates, audit LinkedIn/GitHub profiles, screen candidates in bulk with the Recruiter Dashboard, and download comprehensive career reports without requiring external API keys.

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

### 🛡️ 3. Resume Strength Report
- **Resume Strengths**: Evaluates word count suitability, skill density, complete contact headers, web presence, quantifiable metrics, and readability scanning.
- **Weaknesses**: Identifies word count anomalies, passive voice usage, weak verb phrasing (e.g. "responsible for"), missing summaries, and multi-column layout pipe clutter.
- **Missing Sections**: Audits standard resume headers (Summary, Work Experience, Skills, Education, Projects, Certifications, Achievements).
- **ATS Readiness**: Displays ATS Score, compatibility rating (`EXCELLENT`, `GOOD`, `NEEDS_IMPROVEMENT`, `POOR`), and structural readiness breakdown.
- **Resume Rating**: Calculates overall resume score out of 100 with letter grades (`A+ / Exceptional`, `A / Outstanding`, `B+ / Strong`, `B / Good`, `C / Fair`).
- **Improvement Suggestions**: Actionable recommendations categorized by section.

### ✍️ 4. Grammar & Writing Checker
- **Grammar Errors**: Checks spacing anomalies (double spaces), missing bullet point closing periods, and article misuses ("a" vs "an").
- **Spelling Mistakes**: Scans technical and dictionary terms for typos and provides correction cards.
- **Readability Level**: Computes Flesch-Kincaid index, reading complexity level, and grade level assessment.
- **Writing Style**: Audits action verbs count vs passive voice constructs and rates overall style.
- **Professional Language & Tone**: Checks casual vocabulary, formal tone grade, and replacement suggestions.
- **Sentence Structure**: Measures average sentence length, total sentences, run-on sentence warnings, and flow rating.

### 📜 5. Resume History & Version Control
- **Stored Uploaded Resumes**: Complete list of uploaded files, formats, file sizes, and parsing status.
- **Resume Versions**: Automatically tracks version badges (`v1.0`, `v2.0`, `v3.0`, etc.) based on upload sequence.
- **Upload Dates**: Displays precise upload timestamps.
- **View History**: Click "View Report" on any historical resume to launch the 8-tab Report Drawer Modal.
- **Delete Resume**: Single-click deletion that removes the database entry and deletes the local file from disk.
- **Download Reports**: Download Strength, Grammar, ATS, or AI reports directly in JSON/TXT format.

### 🎯 6. Job Description Matching & ATS Checker
- Paste or upload Job Description (`.txt`) files to compute **Resume Match Percentage**.
- Breakdown metrics: Skill Match %, Experience Match %, Education Match %, and Overall Compatibility.
- Identifies missing required skills and industry keywords.
- Evaluates ATS formatting, structure, and keyword density.

### 💡 7. AI Suggestions & Advanced Skills Analysis
- **AI Suggestions**: Recommendations across Summary, Experience, Projects, Skills, Education, Certifications, Achievements, Action Verbs, Keywords, and Industry Improvements.
- **Skills Analysis**: Categorizes skills into Languages, Frameworks, Databases, Cloud, DevOps, AI/ML, Tools, and Soft Skills. Displays skill distribution charts, strength progress graphs, missing gaps, and recommendations.

### ⚖️ 8. Resume Version Comparison (Feature 13)
- Compare two resume versions side-by-side (Resume Version 1 vs Resume Version 2).
- Generates:
  - **ATS Score Delta**: Score improvement from Version 1 to Version 2 (`+15%`).
  - **Added Skills**: Technical skills added in the newer version.
  - **Removed Skills**: Omitted or removed skills.
  - **Keyword Difference**: New keywords identified vs removed keywords.
  - **Improvement Rate**: Overall optimization score growth percentage.

### ✉️ 9. AI Cover Letter Generator (Feature 14)
- Craft tailored, high-converting professional cover letters aligned with your selected source resume, target company name, job role, and target job description.
- Instant 1-click **Copy to Clipboard** and **Download Cover Letter** functionality.

### 🎨 10. Resume Templates & Visual Builder (Feature 17)
- Generate & preview resumes in **5 distinct professional layouts**:
  1. **ATS Friendly Resume**: Simple single-column layout optimized for ATS parsing.
  2. **Professional Resume**: Corporate-style design with strong section hierarchy.
  3. **Modern Resume**: Modern visual hierarchy with subtle accents & skill badges.
  4. **Minimal Resume**: Clean whitespace layout focusing on pure content.
  5. **Creative Resume**: Vibrant dual-column layout with dark sidebar for tech & creative profiles.
- Switch between templates live without losing resume data.
- Export as standalone HTML, copy formatted text, or print to PDF.

### 💼 11. LinkedIn Profile Analyzer (Feature 18)
- Comprehensive LinkedIn audit analyzing Profile Completeness, Headline positioning, About section, Skills density, Experience metrics, and Certifications.
- Calculates **LinkedIn Score out of 100** with category breakdown.
- Actionable improvement suggestions (e.g., headline keyword additions, metric formatting).

### 🐙 12. GitHub Profile Analyzer (Feature 19)
- Integrates with live GitHub REST API v3 to retrieve repositories, languages, stars, forks, and followers.
- Evaluates Repository Quality, Programming Language diversity, Commit Activity, Contribution Impact, and Documentation Quality.
- Calculates **GitHub Score out of 100** with category breakdown and offline fallback handling.

### 👥 13. Recruiter Candidate Screening Dashboard (Enterprise Upgrade)
- **Bulk Resume Upload**: Upload multiple candidate PDF/DOC/DOCX resumes simultaneously with fault-tolerant processing and live batch progress.
- **Candidate Ranking**: Automatic candidate ranking by ATS score descending (#1, #2, #3...).
- **Multi-Criteria Filtering**: Filter candidates dynamically by Skill substring, Min/Max Experience years, Education Degree level, ATS Score range, and Shortlist status.
- **Shortlist Persistence**: Shortlist candidates with H2 database column persistence.
- **Side-by-Side Comparison**: Select multiple candidates to compare ATS scores, skills, experience, education, strengths, and weaknesses side-by-side.
- **Evaluation Report Downloads**: Download comprehensive candidate evaluation reports as text documents.

### 🎨 14. Redesigned Modern UI & Onboarding Guide (Phase 10)
- **Categorized Sidebar Menu**: Grouped into `MAIN PLATFORM`, `AI CAREER TOOLS`, `AUDIT & ANALYZERS`, and `ENTERPRISE` (`PRO`).
- **3-Step Onboarding Guide**: Easy-to-understand visual walkthrough on opening dashboard.
- **1-Click Quick Action Shortcuts**: Instant shortcut cards to Upload Resume, AI Cover Letter, Resume Templates, and Recruiter Dashboard.
- **Theme Contrast & Polish**: Dark/light mode theme toggle, card elevation, hover transitions, and responsive layout.

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
| `POST` | `/api/recruiter/upload-batch` | Upload and process multiple candidate resumes in batch |
| `GET` | `/api/recruiter/candidates` | List all processed candidates with ranking, ATS scores, experience & shortlist status |
| `POST` | `/api/recruiter/candidates/{id}/shortlist` | Toggle or set shortlisted status for candidate (persisted in DB) |
| `GET` | `/api/recruiter/candidates/{id}/report` | Download comprehensive candidate evaluation report file |
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

- **Current Status**: **Fully Featured & Production Ready**
- **Live Deployment**: [https://resume-analyzer-ai-bvpv.onrender.com](https://resume-analyzer-ai-bvpv.onrender.com)
- **Offline Engine**: 100% functional without external API key dependencies.
- **Features Included**: Document Parsing, Career Profile, ATS Checker, Job Matcher, AI Suggestions, Skills Analysis, Resume Strength Report, Grammar & Writing Checker, Theme Toggle, Real-Time Search, Resume History & Report Downloads, Full-Screen Modal Drawer, **Resume Comparison**, **AI Cover Letter Generator**, **5 Resume Templates**, **LinkedIn Profile Analyzer**, **GitHub Profile Analyzer**, **Recruiter Dashboard (Bulk Upload & Screening)**, and **Redesigned Modern UI/UX**.