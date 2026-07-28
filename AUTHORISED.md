# Authorized Changes Log - AI Resume Analyzer

This log tracks all architectural and design changes authorized and implemented in the codebase.

---

## 🛠️ Phase 1 & 2: Offline Extraction & Report Infrastructure

### 1. Document Parsing & Storage
- **[ResumeParserService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeParserService.java)**: Integrated PDFBox and Apache POI libraries to parse `.pdf` and `.docx` files completely offline. Extracts email, phone, and profiles using regular expressions.
- **H2 Database Integration**: Configured Spring Boot to run an embedded H2 database (in MySQL compatibility mode) writing to local file `resume_analyzer.mv.db`. Enabled automatic schema initialization.

### 2. Metric Engines & APIs
- **[ResumeAnalysisService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeAnalysisService.java)**: Created custom heuristics checking formatting (bullet usage, layout density), section structures, grammar issues, passive voice, and reading difficulty (Flesch-Kincaid estimations). Includes text matcher comparing resume content against a job description.
- **[ResumeController.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/controller/ResumeController.java)**: Exposed REST routes:
  - `GET /api/resumes/{id}/ats`
  - `GET /api/resumes/{id}/ai-analysis`
  - `POST /api/resumes/{id}/match`

### 3. Frontend Layout Integration
- **[index.html](file:///d:/AI%20RESUMER/src/main/resources/static/index.html)**: Built a 5-tab sliding drawer modal (Structured View, ATS Checker, Job Match, AI Analysis, Raw JSON) to keep dashboard modifications minimal.
- **[app.js](file:///d:/AI%20RESUMER/src/main/resources/static/js/app.js)**: Wired tab switches, file uploads, profiles updates, and response rendering.
- **[styles.css](file:///d:/AI%20RESUMER/src/main/resources/static/css/styles.css)**: Appended styles for grid overlays, missing item tags, circular progress dials, and grammar warning boxes.

---

## 🌓 Phase 3: Theme Toggle & Advanced Auditing

### 1. Light Mode & Dark Mode
- **[index.html](file:///d:/AI%20RESUMER/src/main/resources/static/index.html)**: Added a theme switch button `<button class="theme-toggle-btn">` in the top navbar.
- **[styles.css](file:///d:/AI%20RESUMER/src/main/resources/static/css/styles.css)**: Implemented CSS variables under `body.dark-mode` mapped to space-obsidian (#050410) and deep indigo (#0d0b21 / #12102e) layouts. Maintains bright violet accents.
- **[app.js](file:///d:/AI%20RESUMER/src/main/resources/static/js/app.js)**: Configured a theme event listener that switches class states and saves the user preference in `localStorage` for cross-reload persistence.

### 2. AI Resume Suggestions (Feature 8)
- **[ResumeAnalysisService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeAnalysisService.java)**: Created `getResumeSuggestions` calculating offline suggestions for: Summary, Experience, Projects, Skills, Education, Certifications, Achievements, Action Verbs, Keywords, and Industry Improvements.
- **[ResumeController.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/controller/ResumeController.java)**: Exposes suggestions endpoint under `GET /api/resumes/{id}/suggestions`.
- **[index.html](file:///d:/AI%20RESUMER/src/main/resources/static/index.html)**: Created Suggestions Tab (`tab-suggestions`) containing a card grid layout mapping to the 10 fields.
- **[app.js](file:///d:/AI%20RESUMER/src/main/resources/static/js/app.js)**: Added logic to fetch suggestions and render them using `renderSuggestionsTab(data)`.

### 3. Skills Analysis (Feature 9)
- **[ResumeAnalysisService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeAnalysisService.java)**: Created `getSkillsAnalysis` parsing skills into Languages, Frameworks, Databases, Cloud, DevOps, AI/ML, Tools, and Soft Skills. Evaluates missing gaps, next-level recommendations, and strength percentages per category.
- **[ResumeController.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/controller/ResumeController.java)**: Exposes skills evaluation endpoint under `GET /api/resumes/{id}/skills-analysis`.
- **[index.html](file:///d:/AI%20RESUMER/src/main/resources/static/index.html)**: Configured Skills Analysis Tab (`tab-skills-analysis`) displaying distribution meters, strength graphs, and categorized tags.
- **[app.js](file:///d:/AI%20RESUMER/src/main/resources/static/js/app.js)**: Created `renderSkillsAnalysisTab(data)` rendering lists, skill bars, and strength progress scales dynamically.

### 4. Technical Documentation
- **[detailed_project_report.md](file:///d:/AI%20RESUMER/detailed_project_report.md)**: Updated module definitions and API routes catalogs.
- **[final_report.md](file:///d:/AI%20RESUMER/final_report.md)**: Documented `ResumeAnalysisService.java` additions.
