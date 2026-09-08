# 🤖 AGENTS.md — AI Agent Context & Repository Guide

> **To Any Future AI Agent Working on This Codebase:**  
> Read this document first before writing code, modifying files, or proposing changes. It outlines the project's purpose, design constraints, file structure, and architectural principles.

---

## 📌 Project Overview
* **Project Name**: **SmartCampus AI Platform**
* **Domain**: College Capstone / Final Year Project — Academic Study & Career Placement Intelligence.
* **Core Philosophy**: **Ponytail Senior Developer Philosophy** (Standard Library First, Zero Bloat, Minimum Code, Instant Startup) + **Minimalist Modern Design System**.
* **Current Status**: Complete, fully functional, verified, and running on `http://localhost:8080`.

---

## 🏛️ Architectural Guardrails & Constraints

### 1. Java Backend (Strictly Stdlib-First)
* **Runtime**: Java 21 LTS (`Virtual Threads` enabled).
* **Framework**: Built **100% on Java Standard Library**:
  * `com.sun.net.httpserver.HttpServer` on port `8080` (configured with `Executors.newVirtualThreadPerTaskExecutor()`).
  * `java.net.http.HttpClient` for native JSON REST communication.
  * Native in-memory state & stdlib JSON helpers.
* ⚠️ **DO NOT introduce Maven, Gradle, Spring Boot, or heavy external libraries** (like Jackson, Gson, Lombok) unless explicitly instructed by the user. The project's primary selling point for academic viva/grading is that it boots in **0.18s** with **zero broken dependencies** on any evaluator's machine.
* **Dual AI Engine**:
  * **Cloud Mode**: Google Gemini Free API (`gemini-1.5-flash`).
  * **Offline Mode**: Intelligent built-in heuristic/rule engine that provides realistic study answers, quizzes, ATS resume scoring, and mock interview feedback even if run offline with no API key. **Ensure this offline fallback is always preserved.**

### 2. Frontend (Minimalist Modern Design System)
* **Stack**: Pure Vanilla HTML5 + Vanilla CSS3 + Vanilla JavaScript (ES6+). Zero build steps (no npm, webpack, or vite).
* **Typography**:
  * Display / Headlines: `"Calistoga", Georgia, serif`
  * UI / Body: `"Inter", system-ui, sans-serif`
  * Section Badges / Monospace: `"JetBrains Mono", monospace` (tracking: `0.15em`, uppercase)
* **Color Palette & Tokens**:
  * Canvas: `--bg: #FAFAFA` (warm off-white)
  * Text / Inverted Background: `--fg: #0F172A` (deep slate)
  * Signature Gradient: `linear-gradient(135deg, #0052FF, #4D7CFF)` (Electric Blue)
  * Secondary / Cards: `--card: #FFFFFF`, `--border: #E2E8F0`, `--muted: #F1F5F9`
* **Visual Hallmarks**:
  * Section label badges with pulsing status dots (`@keyframes pulse-dot`).
  * Inverted Contrast Sections (`#0F172A` deep slate with 32px radial dot grid texture).
  * 2px gradient card stroke (`.card-featured`).
  * Radial score gauges (SVG with animated `strokeDashoffset`).
  * PDF.js integration for client-side resume PDF text extraction.

---

## 📂 Codebase File Map

```
c:\Users\divya\Desktop\Java\
├── src/
│   └── SmartCampusApp.java       # Single-process Java 21 stdlib server & API handlers
├── web/
│   ├── index.html               # Semantic UI with Calistoga/Inter fonts & dual tabs
│   ├── style.css                # Minimalist Modern design tokens, animations, & textures
│   └── app.js                   # Client controller, PDF.js parsing, typewriter effect, API connector
├── bin/                         # Compiled bytecode (.class files)
├── docs/superpowers/specs/      # Architecture & design specifications
├── run.bat                      # 1-click launcher for Windows (compiles & opens browser)
├── run.sh                       # 1-click launcher for macOS / Linux
├── INSTRUCTIONS.md              # Cross-platform guide for human teammates (Windows & Mac)
├── README.md                    # Project overview & viva demonstration guide
└── AGENTS.md                    # This file (AI context & architectural guardrails)
```

---

## 🔌 Active REST Endpoints (Port 8080)

| Endpoint | Method | Purpose | Request Body | Response Structure |
| :--- | :--- | :--- | :--- | :--- |
| `/*` | `GET` | Static file server (`web/` folder) | None | HTML, CSS, JS with correct MIME types |
| `/api/health` | `GET` | System health & active AI mode | None | `{"status":"ONLINE","runtime":"Java 21 LTS","mode":"..."}` |
| `/api/study/ask` | `POST` | Course contextual Q&A | `{"question":"...","notes":"...","apiKey":"..."}` | `{"answer":"..."}` |
| `/api/study/quiz` | `POST` | Revision quiz generator | `{"notes":"...","apiKey":"..."}` | `{"questions":[{"question":"...","options":[...],"correctAnswerIndex":0,"explanation":"..."}]}` |
| `/api/career/analyze` | `POST` | ATS resume & JD matcher | `{"resume":"...","targetRole":"...","jobDescription":"...","apiKey":"..."}` | `{"score":85,"atsScore":82,"breakdown":{...},"matchedSkills":[...],"missingSkills":[...],"atsKeywords":[...],"bulletRewrites":[...],"studyGuide":{...},"roadmap":[...]}` |
| `/api/career/interview` | `POST` | AI Mock Interview Coach | `{"action":"start\|evaluate", "resume":"...", "targetRole":"...", "question":"...", "answer":"..."}` | `{"question":"..."}` or `{"score":"8.5","feedback":"..."}` |
| `/api/config/key` | `POST` | Gemini API key persistence | `{"apiKey":"..."}` | `{"status":"SAVED","hasKey":true}` |

---

## 🛠️ Verification & Build Commands

When making modifications, verify your changes using these commands:

1. **Compile Java Backend**:
   ```bash
   javac -d bin src/SmartCampusApp.java
   ```
2. **Run Java Backend**:
   ```bash
   java -cp bin src.SmartCampusApp
   ```
3. **Verify JavaScript Syntax**:
   ```bash
   node -c web/app.js
   ```
4. **Test Endpoints**:
   ```bash
   curl -I http://localhost:8080/
   curl http://localhost:8080/api/health
   ```

---

## 💡 Key Design Rules for Future Agents

1. **Keep It Simple & Robust (YAGNI)**: Avoid over-engineering. Do not split `SmartCampusApp.java` into 20 micro-packages unless strictly necessary.
2. **Maintain Offline Fallback**: Any new AI features added MUST have a deterministic offline heuristic fallback in Java so student demonstrations never fail when evaluated without Wi-Fi.
3. **Preserve UI Aesthetics**: Always maintain the **Minimalist Modern** design system (Electric Blue gradient, Calistoga headlines, JetBrains Mono badges, and inverted contrast sections).
