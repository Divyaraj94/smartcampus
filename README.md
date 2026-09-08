# SmartCampus AI Platform 🎓🚀
> **An Integrated Academic Study & Placement Intelligence Platform**  
> Built with **Java 21 LTS Standard Library** and designed with the **Minimalist Modern** design system.

---

## 🌟 Overview

**SmartCampus AI** bridges the gap between academic learning and career readiness for university students. Engineered adhering to the **Ponytail senior-developer philosophy** (*standard library first, zero bloat, fewest files, YAGNI*), it delivers enterprise-grade intelligence in a single-process Java server that boots in under **0.2 seconds** without requiring massive Maven/Gradle downloads or external dependencies.

### Key Capabilities:
1. **Academic Study Hub (Contextual RAG Q&A & Revision Quizzes)**:
   - Ingests lecture notes, textbooks, and syllabus outlines.
   - Grounded Q&A chatbot that synthesizes answers with clear technical definitions and exam takeaways.
   - Generates interactive 4-option multiple-choice quizzes (MCQs) with instant answer validation and explanations.

2. **Career & Placement Lab (Resume Gap Radar & AI Mock Interview)**:
   - Compares candidate resumes against target industry profiles (*Java Backend Developer*, *Full Stack Engineer*, *Cloud/DevOps*, *AI/ML*).
   - Dynamic radial readiness gauge (0-100% match score).
   - Generates verified skill badges (emerald), missing skill gaps (amber), and an actionable preparation roadmap.
   - **Inverted Contrast Section**: AI Technical Mock Interview coach that asks role-specific questions and scores candidate answers out of 10.

3. **Live Google Gemini AI Engine**:
   - Powered by Google Gemini (`gemini-1.5-flash`, `gemini-2.0-flash`, `gemini-1.5-pro`).
   - Secure client & server API key configuration with instant validation.

---

## 🎨 Minimalist Modern UI/UX Architecture

The frontend embodies the **Minimalist Modern** design system:
- **Typography Pairing**:
  - `Calistoga`: Warm, characterful serif for display headlines.
  - `Inter`: Crystal-clear sans-serif for UI, forms, and body copy.
  - `JetBrains Mono`: Monospace uppercase section labels with wide letter-spacing (`tracking-[0.15em]`).
- **Signature Gradient**: Electric Blue (`#0052FF` → `#4D7CFF`) on action buttons, headline underlines, and gauges.
- **Inverted Contrast Sections**: Deep slate `#0F172A` background with a subtle 32px radial dot grid texture for the AI Mock Interview Coach.
- **Micro-Animations**:
  - Pulsing status dots (`@keyframes pulse-dot`).
  - Tactile button presses (`scale-[0.98]`).
  - Smooth 60fps radial SVG score animations.
  - Typewriter text effect for AI responses.

---

## 🏗️ Project Structure

```
c:\Users\divya\Desktop\Java\
├── src/
│   └── SmartCampusApp.java       # Java 21 stdlib server (HttpServer, Virtual Threads, HttpClient)
├── web/
│   ├── index.html               # Semantic UI with Calistoga/Inter fonts & dual panels
│   ├── style.css                # Minimalist Modern design system, tokens, and textures
│   └── app.js                   # Reactive controller, typewriter rendering, API connector
├── docs/
│   └── superpowers/specs/       # Architecture & design specifications
├── bin/                         # Compiled Java bytecode
└── README.md                    # Project documentation
```

---

## ⚡ Quick Start & Viva Demo

### Prerequisites:
- Java 21+ (Installed via Microsoft OpenJDK)

### 1. Compile:
```powershell
& "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe" -d bin src/SmartCampusApp.java
```

### 2. Run:
```powershell
& "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" -cp bin src.SmartCampusApp
```

### 3. Open in Browser:
Visit:
```
http://localhost:8080
```

---

## 🧪 Step-by-Step Viva Presentation Walkthrough

1. **Demonstrate Academic Study Hub**:
   - Click **"Load Sample Java Notes"** to immediately populate course notes on OOP Pillars and JVM memory architecture.
   - Click any suggestion chip (e.g. **"OOP Pillars"** or **"JVM Architecture"**) and click **"Ask AI"**. Observe the typewriter animation rendering the structured answer.
   - Click **"Generate Quiz"**. Click through the 3 generated MCQs to show instant green/red validation and explanations.

2. **Demonstrate Career & Placement Lab**:
   - Switch to the **"Career & Placement Lab"** tab.
   - Click **"Load Sample Student Resume"** and select **"Java Backend Developer"**.
   - Click **"Run Career & Gap Analysis"**. Watch the radial match gauge animate, displaying matched skills, skill gaps, and the strategic roadmap.

3. **Connect Gemini API Key**:
   - Click the **"API KEY"** button in the top navbar or the inline button in Study Material.
   - Enter your free Google Gemini API key from Google AI Studio (`aistudio.google.com`) to activate live responses!
