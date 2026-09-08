# SmartCampus AI Platform: Design Specification

**Date**: 2026-09-08  
**Topic**: Hybrid College Project — Academic Study & Career Placement Intelligence  
**Principles**: Ponytail Philosophy (Standard library first, zero bloat, fewest files, YAGNI) + Pro-Max UI/UX (Glassmorphism, dark theme, micro-animations, fluid reactivity)

---

## 1. Overview & Objectives

SmartCampus AI is an integrated academic and career platform engineered in Java. It solves two critical college needs in a single unified dashboard:
1. **Academic Study Hub**: Allows students to ingest course materials/notes and query them using an AI assistant, or generate flashcards and exam revision quizzes.
2. **Career & Placement Lab**: Allows students to upload/paste their resume, compare skills against industry job descriptions (Software Engineer, AI Engineer, Cloud Architect, etc.), compute a match score, and take interactive AI mock interviews.

---

## 2. Architectural Design (Ponytail Standard-Library Approach)

Following the **Ponytail** rule (*stdlib does it? Use it. No unrequested abstractions, no bloated dependencies, fewest files*):

```
+-------------------------------------------------------------+
|               Browser Frontend (Pro-Max UI/UX)              |
|   - Glassmorphic Dark UI, CSS Variables, SVG Icons          |
|   - Tab 1: Academic Study & Document Intelligence           |
|   - Tab 2: Career Lab (Resume & Mock Interview)             |
+------------------------------+------------------------------+
                               | REST JSON (Fetch API)
                               v
+-------------------------------------------------------------+
|                 Java 21 Backend (Single Process)            |
|   - Built-in com.sun.net.httpserver.HttpServer (port 8080)   |
|   - Built-in java.net.http.HttpClient (Gemini / Ollama)     |
|   - In-memory Knowledge & Session Store                     |
|   - Zero-external-dependency runner: java SmartCampusApp.java|
+-------------------------------------------------------------+
```

### Why this beats bloated frameworks for College Evaluation:
- **Instant startup (< 0.5s)**: Evaluators can run `java SmartCampusApp.java` immediately without waiting for Maven downloads or dependency resolution.
- **Zero fragile dependencies**: No broken classpath or version mismatch on the professor's computer.
- **Dual AI Engine**:
  1. Real AI mode: Integrates directly with Google Gemini REST API or local Ollama.
  2. Offline Evaluation mode: Built-in intelligent semantic analyzer that parses keywords, sections, and generates realistic answers even if no internet or API key is provided during live campus viva/grading.

---

## 3. Component Breakdown

### Backend Files
1. `src/SmartCampusApp.java`:
   - Single-file (or minimal paired files) containing:
     - `HttpServer` serving static UI assets and REST endpoints.
     - Endpoints:
       - `POST /api/study/ask`: Answers questions based on indexed notes.
       - `POST /api/study/quiz`: Generates structured 4-option quiz questions.
       - `POST /api/career/analyze`: Computes skill match score and gap recommendations.
       - `POST /api/career/interview`: Generates interview questions and evaluates user answers.
       - `GET /api/health`: System status and AI connection mode.
     - Document Text Extractor: Standard UTF-8 / plain-text / PDF stream parser.
     - AI Connector: `HttpClient` making standard HTTPS JSON calls.

### Frontend Files (Pro-Max UI/UX)
1. `web/index.html`: Semantic layout featuring a sidebar navigation, header with system status pill, and tabbed view containers.
2. `web/style.css`: Pro-Max design system:
   - Deep midnight dark theme (`#0a0d14`, `#121826`) with ambient radial glow gradients.
   - Glassmorphic card styling (`backdrop-filter: blur(12px)`).
   - High-contrast typography (Inter font family), glowing status badges, smooth transitions.
   - Animated radial score indicators, micro-interactions, responsive mobile/desktop layout.
3. `web/app.js`: Reactive frontend controller handling state, asynchronous fetch calls, typewriter response animations, and clipboard copy.

---

## 4. UI/UX Feature Specifications

### Study Companion View
- **Knowledge Input**: Drag-and-drop or paste lecture notes/syllabus.
- **Context Badge**: Shows currently loaded document word count and topic keywords.
- **AI Chat Room**: Clean message bubbles with Markdown-like rendering, instant copy button, and confidence indicator.
- **Quiz Generator**: Interactive flashcards with revealable answers and interactive multiple-choice quiz questions with instant feedback on correct/incorrect choices.

### Career & Placement Lab View
- **Resume Input**: Paste or upload resume text.
- **Job Role Selector**: Dropdown of target roles (Java Backend Developer, Full Stack Engineer, DevOps, Data Analyst, etc.) + custom job description input.
- **Match Score Gauge**: Animated circular progress percentage showing role readiness.
- **Skill Matrix**: Badges for "Matched Skills" (green), "Missing Skills" (amber), and "Recommended Actions".
- **Mock Interview Simulator**: Step-by-step interview questions with text/voice input simulation and constructive critique.

---

## 5. Verification & Self-Review

- **Spec Self-Review**:
  - Placeholder scan: No TODOs or unresolved mocks.
  - Consistency: Architecture directly uses Java 21 stdlib (`com.sun.net.httpserver`, `java.net.http`).
  - Ponytail check: Zero third-party Maven/Gradle bloat required; stdlib is fully leveraged.
  - Pro-Max UI check: Glassmorphism, dark theme, micro-animations, and complete responsive layout specified.
