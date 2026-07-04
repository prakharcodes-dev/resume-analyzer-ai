# resume-analyzer-ai
AI-powered resume analysis platform that extracts candidate information, evaluates profile completeness, and provides structured career insights from PDF and DOCX resumes.

# AI Resume Analyzer

AI Resume Analyzer is a web application that helps users upload resumes, automatically extract important information, and build a structured career profile.

The system processes PDF and DOCX resumes, identifies contact information, skills, education, experience, and projects, then organizes the data into an easy-to-manage dashboard.

## Features

### Resume Upload
- Upload PDF and DOCX resumes
- Drag-and-drop file support
- Real-time upload feedback
- File validation and size restrictions

### Resume Parsing
- Extracts text from resumes
- Detects candidate contact details
- Identifies education history
- Extracts work experience
- Detects technical skills
- Captures project information

### Career Profile Dashboard
- Automatically populates profile information
- Displays profile completeness score
- Allows manual editing of extracted details
- Stores user information persistently

### ATS Readiness
- Evaluates profile completeness
- Provides a profile score
- Highlights missing information

## Technology Stack

### Backend
- Java
- Spring Boot
- Spring Data JPA
- H2 Database
- Apache PDFBox
- Apache POI

### Frontend
- HTML
- CSS
- JavaScript

## Project Structure

```text
src/
├── controller/
├── service/
├── repository/
├── model/
├── resources/
│   ├── static/
│   ├── application.properties
│   └── schema.sql


Current Implementation

Resume upload system
PDF and DOCX parsing
Contact information extraction
Skills detection
Education extraction
Experience extraction
Dashboard statistics
Profile management

Future Roadmap

AI-powered resume feedback
ATS score improvement suggestions
Resume-job matching
Interview preparation assistant
Skill gap analysis
Career recommendations
Resume optimization insights

Run Application
mvn spring-boot:run

Application will be available at:
http://localhost:8080
