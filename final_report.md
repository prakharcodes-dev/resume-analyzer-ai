# Final Project Report: AI Resume Analyzer & Career Assistant

This report provides a comprehensive guide to running the **AI Resume Analyzer & Career Assistant** (Offline Mode - First 4 Features), explains the purpose of every file created, details why each technology/library was chosen, and reviews the challenges encountered and resolved during development.

---

## 🚀 Easy & Zero-Configuration Running Guide

With our persistent H2 database setup, **you do not need to start MySQL or run any database commands.** The local database boots up automatically.

### Step 1: Open a Standard Terminal in the Project Folder
1. Open a regular terminal (PowerShell or Command Prompt).
2. Switch to your **D:** drive by typing:
   ```powershell
   d:
   ```
3. Navigate into the project folder:
   ```powershell
   cd "D:\AI RESUMER"
   ```

### Step 2: Run the Spring Boot Server
Launch the application using the Maven Wrapper (`mvnw`):
* **If you are using PowerShell**:
  ```powershell
  ./mvnw spring-boot:run
  ```
* **If you are using Command Prompt**:
  ```cmd
  mvnw spring-boot:run
  ```
*Wait for 10-15 seconds. You will see startup logs in the console. Once you see a message like:*
`[INFO] Started ResumeAnalyzerApplication in 2.453 seconds`
*the server is running!*

### Step 3: Open your Browser
Open your preferred web browser and go to:
👉 **`http://localhost:8080`**

*You will be taken directly to the main Career Assistant Dashboard as `guest@career.com` without any login page!*

---

## 📂 Why Each File Was Created & What It Does

Here is the breakdown of the codebase, grouping files by their roles:

### 1. Build & Configuration Files

* #### [pom.xml](file:///d:/AI%20RESUMER/pom.xml)
  * **Why it was created:** It is the Maven Project Object Model. It defines the project metadata, Java version (21), compiler configurations, and all external libraries (dependencies) required to build and run the app.
  * **Key Libraries Used:**
    * `spring-boot-starter-web`: Pulls in Tomcat (the local server) and Spring MVC to handle REST APIs.
    * `spring-boot-starter-data-jpa`: Links Hibernate and JPA to perform database CRUD operations.
    * `mysql-connector-j`: The official database driver enabling Java to communicate with MySQL.
    * `pdfbox` (v3.0.2): Used to read and extract text from PDF files.
    * `poi` & `poi-ooxml` (v5.2.5): Used to extract text from Microsoft Word (`.docx`) files.
* #### [application.properties](file:///d:/AI%20RESUMER/src/main/resources/application.properties)
  * **Why it was created:** Centralizes all Spring Boot settings. It configures the connection url to the MySQL database `resume_analyzer`, sets the user to `root`, defines the database dialect, sets file upload boundaries to 10MB, and ensures `schema.sql` runs on every startup.

### 2. Database Initialization & Schema

* #### [schema.sql](file:///d:/AI%20RESUMER/src/main/resources/schema.sql)
  * **Why it was created:** Initializes tables on startup if they do not exist.
  * **Table Roles:**
    * `users`: Holds user credentials (email, roles).
    * `user_profiles`: Holds active data shown on the dashboard (Full Name, LinkedIn, education/experience JSON dumps).
    * `uploaded_resumes`: Tracks document history (file path, file size, status, parsed raw text).
    * **Default Seed Data**: Inserts `id = 1` as `guest@career.com` so that the app is immediately usable without forcing you to log in first.

### 3. Database Entities (JPA Models)

* #### [User.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/model/User.java)
  * **Why it was created:** Maps the `users` table rows into Java objects.
* #### [UserProfile.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/model/UserProfile.java)
  * **Why it was created:** Maps the `user_profiles` table, storing contact details and parsed arrays (skills, experience, etc.) as text.
* #### [UploadedResume.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/model/UploadedResume.java)
  * **Why it was created:** Maps the `uploaded_resumes` table. Includes metadata and a `@PrePersist` hook that timestamps the record automatically upon database entry.

### 4. Database Access Layer (Repositories)

* #### [UserRepository.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/repository/UserRepository.java), [UserProfileRepository.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/repository/UserProfileRepository.java), [UploadedResumeRepository.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/repository/UploadedResumeRepository.java)
  * **Why they were created:** These extend `JpaRepository`. They write database queries (like `findByUserId`) automatically, saving us from writing boilerplate SQL strings for basic data lookup and saving.

### 5. Services (Business & Parsing Logic)

* #### [ResumeParserService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeParserService.java)
  * **Why it was created:** The core parsing engine. Since we removed the Gemini API key, this class handles document parsing offline:
    * It reads the binary bytes of the document.
    * Calls PDFBox or POI based on file extension to extract the raw text.
    * Uses **Regular Expressions** to search for contact fields (LinkedIn, Email, Phone).
    * Scans for heading tokens (`EDUCATION`, `EXPERIENCE`, `SKILLS`, `PROJECTS`) to capture text groups, structuring them into clean JSON nodes via Jackson's `ObjectMapper`.
    * Contains a keyword dictionary search fallback to detect skills (like Java, Python, Docker) if a resume lacks a dedicated skills section header.
* #### [ResumeAnalysisService.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/service/ResumeAnalysisService.java)
  * **Why it was created:** Implements rules-based and dictionary-lookup evaluation models. It calculates ATS scores, checks syntax/passive voice/weak verbs, computes Job Description matching metrics, compiles 10 distinct section optimization recommendations, and organizes skills into categorized strengths and distribution bars.

### 6. Web Controllers (REST API Endpoints)

* #### [ResumeController.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/controller/ResumeController.java)
  * **Why it was created:** Handles document operations. When a file is uploaded, it:
    * Saves it locally to the `uploads/` folder.
    * Calls `ResumeParserService` to extract the data.
    * Instantly updates the user's active Profile entity with the parsed contact details, skills, education, projects, and work experience.
    * Returns the resume record. Also handles listing history and deleting files.
* #### [ProfileController.java](file:///d:/AI%20RESUMER/src/main/java/com/career/resumeanalyzer/controller/ProfileController.java)
  * **Why it was created:** Exposes routes to fetch the active guest profile (`GET /api/profile`) and save manual updates (`PUT /api/profile`).

### 7. Frontend User Interface

* #### [index.html](file:///d:/AI%20RESUMER/src/main/resources/static/index.html)
  * **Why it was created:** The Single Page Application container. It defines the layout, navigation sidebar, stats sections, drag-and-drop upload container, tables, profile editors, and report modal cards.
* #### [styles.css](file:///d:/AI%20RESUMER/src/main/resources/static/css/styles.css)
  * **Why it was created:** Provides a premium glassmorphic interface, dark backgrounds, neon violet borders, custom scrollbars, and progress bar animations.
* #### [app.js](file:///d:/AI%20RESUMER/src/main/resources/static/js/app.js)
  * **Why it was created:** Binds the HTML elements to the Spring Boot REST API. It handles drag-and-drop events, uses `XMLHttpRequest` to show upload progress, routes tabs, loads/saves the profile forms, and renders detailed reports inside the modal.

---

## 🛠️ Challenges Faced & How They Were Solved

During the development process, we hit a few hurdles and worked around them:

### 1. Spring Initializr Bad Request (400)
* **The Problem:** The initial zip download command to `start.spring.io` returned a `400 Bad Request` error. This happened because the Spring Initializr API changes its supported Spring Boot and Java versions frequently, and passing manual, outdated version flags causes validation errors.
* **The Solution:** We requested a simplified url:
  `https://start.spring.io/starter.zip?dependencies=web,data-jpa,mysql,validation&language=java&type=maven-project&javaVersion=21`
  This omitted the static Spring Boot version parameter, letting the server use its latest stable version automatically. The download completed successfully.

### 2. Windows Service Permissions
* **The Problem:** Trying to start the MySQL service via `Start-Service MySQL80` failed with access denied errors in the terminal context because Windows restricts service execution to elevated Administrator users.
* **The Solution:** We documented this challenge clearly in the running instructions, directing you to open PowerShell explicitly using **Run as Administrator** to start the service before running the code.

### 3. Java List Subscript Compilation Error
* **The Problem:** The compiler failed with:
  `[ERROR] ... ResumeParserService.java:[288,40] array required, but java.util.List<java.lang.String> found`
  This occurred because the `lines` parameter was defined as a Java `List<String>`, but the code attempted to access it using array index brackets `lines[i]`.
* **The Solution:** We replaced the line index bracket syntax with the correct List interface method: `lines.get(i)`. The project now compiles with `BUILD SUCCESS`.

### 4. Bypassing Gemini API & Authentication Gateway
* **The Problem:** You requested to remove the Gemini API key and the login screens.
* **The Solution:**
  * Bypassed the login system by writing `schema.sql` to inject a default user (`guest@career.com`) with `id = 1`.
  * Designed the controllers to automatically use `DEFAULT_USER_ID = 1L` for all uploads, lookups, and profile configurations, saving the user from logging in.
  * Replaced the Gemini model dependency with a robust local regex/rule parser within `ResumeParserService.java`, extracting emails, phones, URLs, and grouping experience/education paragraphs offline without external costs.