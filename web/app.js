/**
 * SmartCampus AI — Client Controller
 * Minimalist Modern Frontend interactions & Java Backend REST connector
 */

// State
let geminiApiKey = localStorage.getItem("smartcampus_gemini_key") || "";
let activeInterviewQuestion = "";

// Sample Data for Instant Evaluation & Demo
const SAMPLE_JAVA_NOTES = `MODULE 1: OBJECT-ORIENTED PROGRAMMING IN JAVA
1. Encapsulation: Wrapping code and data together into a single unit. Achieved via private fields and public getter/setter methods to protect internal state.
2. Inheritance: Mechanism where one class acquires properties of another class using the 'extends' keyword. Java supports single class inheritance and multiple interface implementation.
3. Polymorphism: Ability of an object to take many forms. Includes:
   - Compile-time (Static): Method Overloading (same method name, different parameter lists).
   - Runtime (Dynamic): Method Overriding (subclass provides specific implementation of a superclass method).
4. Abstraction: Hiding implementation details and showing only essential features. Implemented via abstract classes and interfaces.

MODULE 2: JVM ARCHITECTURE & MEMORY MANAGEMENT
- ClassLoader Subsystem: Loads, links, and initializes .class bytecode.
- JVM Memory Areas:
  * Heap Area: Stores all objects and instances (managed by Garbage Collector).
  * Method Area: Stores class-level data, bytecode, and static variables.
  * JVM Stack: Stores local variables and partial results per thread execution frame.
  * Program Counter (PC) Registers: Tracks next instruction address.
  * Native Method Stack: Handles native C/C++ methods via JNI.
- Garbage Collection (GC): Automated memory daemon freeing unreferenced heap memory using Mark-Sweep-Compact algorithms.`;

const SAMPLE_STUDENT_RESUME = `NAME: Divyanshu Kumar
DEGREE: B.Tech in Computer Science & Engineering (2022 - 2026) | CGPA: 8.6/10
TARGET: Java Software Engineer / Backend Developer

TECHNICAL SKILLS:
- Core Java, Collections Framework, Multithreading, OOP, Java 17/21
- Backend: Spring Boot, RESTful APIs, Hibernate, JPA
- Database: MySQL, PostgreSQL, H2 Database
- Web: HTML5, CSS3, JavaScript (ES6+), Fetch API
- Developer Tools: Git, GitHub, Maven, Docker Basics, VS Code

PROJECTS:
1. Distributed E-Commerce Microservices (Spring Boot, Kafka, Docker)
   - Built checkout and order processing services with JWT authentication.
   - Designed relational database schemas in MySQL handling 5,000+ orders.
2. College Library Management System (Java, JavaFX, SQLite)
   - Built a desktop management dashboard with search and book reservation indexing.

ACHIEVEMENTS:
- Solved 300+ LeetCode problems (Data Structures, Binary Trees, Dynamic Programming)
- Finalist in Smart India Hackathon 2025`;

document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  initSampleData();
  initStudyHub();
  initCareerLab();
  initApiKeyModal();
  checkBackendHealth();
});

// -------------------------------------------------------------
// 1. Navigation Tabs
// -------------------------------------------------------------
function initTabs() {
  const tabStudyBtn = document.getElementById("tabStudyBtn");
  const tabCareerBtn = document.getElementById("tabCareerBtn");
  const studyPanel = document.getElementById("studyHubPanel");
  const careerPanel = document.getElementById("careerLabPanel");

  tabStudyBtn.addEventListener("click", () => {
    tabStudyBtn.classList.add("active");
    tabStudyBtn.setAttribute("aria-selected", "true");
    tabCareerBtn.classList.remove("active");
    tabCareerBtn.setAttribute("aria-selected", "false");

    studyPanel.classList.add("active");
    careerPanel.classList.remove("active");
  });

  tabCareerBtn.addEventListener("click", () => {
    tabCareerBtn.classList.add("active");
    tabCareerBtn.setAttribute("aria-selected", "true");
    tabStudyBtn.classList.remove("active");
    tabStudyBtn.setAttribute("aria-selected", "false");

    careerPanel.classList.add("active");
    studyPanel.classList.remove("active");
  });
}

// -------------------------------------------------------------
// 2. Sample Data Loaders
// -------------------------------------------------------------
function initSampleData() {
  const notesInput = document.getElementById("studyNotesInput");
  const resumeInput = document.getElementById("careerResumeInput");

  document.getElementById("btnLoadSampleNotes")?.addEventListener("click", () => {
    notesInput.value = SAMPLE_JAVA_NOTES;
    updateWordCount();
    showToast("Sample Java Course Notes Loaded!");
  });

  document.getElementById("btnClearNotes")?.addEventListener("click", () => {
    notesInput.value = "";
    updateWordCount();
  });

  notesInput.addEventListener("input", updateWordCount);

  document.getElementById("btnLoadSampleResume")?.addEventListener("click", () => {
    resumeInput.value = SAMPLE_STUDENT_RESUME;
    showToast("Sample Student Resume Loaded!");
  });
}

function updateWordCount() {
  const text = document.getElementById("studyNotesInput").value.trim();
  const words = text ? text.split(/\s+/).length : 0;
  const chars = text.length;
  document.getElementById("studyWordCount").textContent = `${words} words · ${chars} characters`;
}

// -------------------------------------------------------------
// 3. Academic Study Hub
// -------------------------------------------------------------
function initStudyHub() {
  const askForm = document.getElementById("studyAskForm");
  const questionInput = document.getElementById("studyQuestionInput");
  const chatDisplay = document.getElementById("studyChatDisplay");

  // Chips click handler
  document.querySelectorAll(".chip").forEach(chip => {
    chip.addEventListener("click", () => {
      questionInput.value = chip.getAttribute("data-question");
      askForm.requestSubmit();
    });
  });

  askForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const question = questionInput.value.trim();
    if (!question) return;

    const notes = document.getElementById("studyNotesInput").value.trim();

    // Append user bubble
    appendChatBubble(chatDisplay, "user", question);
    questionInput.value = "";

    // Append loading AI bubble
    const aiBubble = appendChatBubble(chatDisplay, "ai", "Analyzing notes and synthesizing answer...");

    try {
      const response = await fetch("/api/study/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question, notes, apiKey: geminiApiKey })
      });

      const data = await response.json();
      typewriterEffect(aiBubble.querySelector(".bubble-text"), data.answer || "No response received.");
    } catch (err) {
      aiBubble.querySelector(".bubble-text").textContent = "Network error connecting to Java backend. Ensure server is running on port 8080.";
    }
  });

  // Quiz Generator
  document.getElementById("btnGenerateQuiz")?.addEventListener("click", async () => {
    const notes = document.getElementById("studyNotesInput").value.trim();
    const quizContainer = document.getElementById("quizContainer");

    quizContainer.innerHTML = `
      <div class="empty-state">
        <div class="pulsing-dot" style="margin: 0 auto 12px; width: 14px; height: 14px;"></div>
        <h4 class="font-display">Generating Exam Flashcards...</h4>
        <p class="text-muted text-sm">Synthesizing multiple-choice questions from your course text.</p>
      </div>
    `;

    try {
      const response = await fetch("/api/study/quiz", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ notes, apiKey: geminiApiKey })
      });

      const data = await response.json();
      renderQuiz(data.questions || []);
    } catch (err) {
      quizContainer.innerHTML = `<p class="text-amber text-sm text-center">Failed to generate quiz. Make sure notes are provided.</p>`;
    }
  });
}

function appendChatBubble(container, type, text) {
  const bubble = document.createElement("div");
  bubble.className = `chat-bubble ${type === "user" ? "user-bubble" : "ai-bubble"}`;
  
  const header = document.createElement("div");
  header.className = "bubble-header font-mono";
  header.innerHTML = `
    <span>${type === "user" ? "STUDENT" : "CAMPUS-AI"}</span>
    <span>${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
  `;

  const content = document.createElement("p");
  content.className = "bubble-text";
  content.textContent = text;

  bubble.appendChild(header);
  bubble.appendChild(content);
  container.appendChild(bubble);
  container.scrollTop = container.scrollHeight;
  return bubble;
}

function typewriterEffect(element, text) {
  element.textContent = "";
  let i = 0;
  const speed = 12;
  function type() {
    if (i < text.length) {
      element.textContent += text.charAt(i);
      i++;
      setTimeout(type, speed);
    }
  }
  type();
}

function renderQuiz(questions) {
  const quizContainer = document.getElementById("quizContainer");
  if (!questions || questions.length === 0) {
    quizContainer.innerHTML = `<div class="empty-state"><p class="text-muted">No questions could be generated. Try adding more detailed notes.</p></div>`;
    return;
  }

  quizContainer.innerHTML = "";

  questions.forEach((q, qIndex) => {
    const card = document.createElement("div");
    card.className = "quiz-card";

    let optionsHtml = "";
    q.options.forEach((opt, optIndex) => {
      optionsHtml += `
        <button class="quiz-option-btn" data-q="${qIndex}" data-opt="${optIndex}">
          <span>${String.fromCharCode(65 + optIndex)}. ${opt}</span>
          <span class="opt-status font-mono text-xs"></span>
        </button>
      `;
    });

    card.innerHTML = `
      <div class="quiz-qnum font-mono">QUESTION ${qIndex + 1} OF ${questions.length}</div>
      <div class="quiz-question">${q.question}</div>
      <div class="quiz-options">${optionsHtml}</div>
      <div class="quiz-explanation" id="exp-${qIndex}" style="display: none;">
        <strong>Explanation:</strong> ${q.explanation}
      </div>
    `;

    // Add option click events
    card.querySelectorAll(".quiz-option-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const selected = parseInt(btn.getAttribute("data-opt"));
        const expBox = card.querySelector(`#exp-${qIndex}`);
        expBox.style.display = "block";

        card.querySelectorAll(".quiz-option-btn").forEach((b, idx) => {
          b.disabled = true;
          if (idx === q.correctAnswerIndex) {
            b.classList.add("correct");
            b.querySelector(".opt-status").textContent = "✓ CORRECT";
          } else if (idx === selected) {
            b.classList.add("wrong");
            b.querySelector(".opt-status").textContent = "✗ INCORRECT";
          }
        });
      });
    });

    quizContainer.appendChild(card);
  });
}

// -------------------------------------------------------------
// 4. Career & Placement Lab
// -------------------------------------------------------------
function initCareerLab() {
  const btnAnalyze = document.getElementById("btnAnalyzeResume");
  const resultsContainer = document.getElementById("careerResultsContainer");

  btnAnalyze.addEventListener("click", async () => {
    const resume = document.getElementById("careerResumeInput").value.trim();
    const targetRole = document.getElementById("careerTargetRole").value;
    const degree = document.getElementById("careerStudentDegree").value;

    if (!resume) {
      alert("Please paste your resume or click 'Load Sample Student Resume' first!");
      return;
    }

    btnAnalyze.disabled = true;
    btnAnalyze.querySelector("span").textContent = "Analyzing Skill Match...";

    try {
      const response = await fetch("/api/career/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resume, targetRole, degree, apiKey: geminiApiKey })
      });

      const data = await response.json();
      renderCareerResults(data);
      resultsContainer.style.display = "block";
      resultsContainer.scrollIntoView({ behavior: "smooth" });
    } catch (err) {
      alert("Error analyzing resume. Please ensure Java server is running.");
    } finally {
      btnAnalyze.disabled = false;
      btnAnalyze.querySelector("span").textContent = "Run Career & Gap Analysis";
    }
  });

  // Mock Interview
  const btnStartInterview = document.getElementById("btnStartInterview");
  const interviewBox = document.getElementById("interviewSessionBox");
  const currentQText = document.getElementById("currentInterviewQuestion");
  const btnSubmitAnswer = document.getElementById("btnSubmitAnswer");
  const feedbackBox = document.getElementById("interviewFeedbackBox");
  const feedbackText = document.getElementById("interviewFeedbackText");
  const scoreBadge = document.getElementById("interviewScoreBadge");

  btnStartInterview.addEventListener("click", async () => {
    const resume = document.getElementById("careerResumeInput").value.trim();
    const targetRole = document.getElementById("careerTargetRole").value;

    btnStartInterview.disabled = true;
    currentQText.textContent = "Generating personalized interview question based on your profile...";
    interviewBox.style.display = "block";
    feedbackBox.style.display = "none";

    try {
      const response = await fetch("/api/career/interview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "start", resume, targetRole, apiKey: geminiApiKey })
      });

      const data = await response.json();
      activeInterviewQuestion = data.question;
      currentQText.textContent = activeInterviewQuestion;
    } catch (err) {
      currentQText.textContent = "Tell me about a challenging Java backend project you developed and how you handled database transactions.";
      activeInterviewQuestion = currentQText.textContent;
    } finally {
      btnStartInterview.disabled = false;
    }
  });

  btnSubmitAnswer.addEventListener("click", async () => {
    const answer = document.getElementById("interviewUserAnswer").value.trim();
    const targetRole = document.getElementById("careerTargetRole").value;

    if (!answer) {
      alert("Please enter your answer before submitting.");
      return;
    }

    btnSubmitAnswer.disabled = true;
    btnSubmitAnswer.querySelector("span").textContent = "Grading Answer...";

    try {
      const response = await fetch("/api/career/interview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "evaluate",
          question: activeInterviewQuestion,
          answer,
          targetRole,
          apiKey: geminiApiKey
        })
      });

      const data = await response.json();
      feedbackBox.style.display = "block";
      scoreBadge.textContent = `GRADE: ${data.score}/10`;
      feedbackText.textContent = data.feedback;
      feedbackBox.scrollIntoView({ behavior: "smooth" });
    } catch (err) {
      alert("Error grading interview answer.");
    } finally {
      btnSubmitAnswer.disabled = false;
      btnSubmitAnswer.querySelector("span").textContent = "Submit Answer for AI Grading";
    }
  });
}

function renderCareerResults(data) {
  // Score Radial Animation
  const scoreVal = data.score || 85;
  document.getElementById("scoreNumber").textContent = `${scoreVal}%`;
  document.getElementById("scoreHeadline").textContent = scoreVal >= 80 ? "Top Tier Candidate" : scoreVal >= 60 ? "Solid Alignment" : "Needs Upskilling";
  document.getElementById("scoreSubtitle").textContent = data.summary || "Profile evaluation complete.";

  const circle = document.getElementById("scoreProgressCircle");
  const circumference = 364.4;
  const offset = circumference - (scoreVal / 100) * circumference;
  circle.style.strokeDashoffset = offset;

  // Matched Skills
  const matchedList = document.getElementById("matchedSkillsList");
  matchedList.innerHTML = (data.matchedSkills || ["Core Java", "Spring Boot", "REST APIs", "SQL", "OOP"]).map(s => `
    <span class="skill-tag skill-tag-emerald">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
      ${s}
    </span>
  `).join("");

  // Missing Skills
  const missingList = document.getElementById("missingSkillsList");
  missingList.innerHTML = (data.missingSkills || ["Microservices Resiliency", "Docker CI/CD", "Redis Caching"]).map(s => `
    <span class="skill-tag skill-tag-amber">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      ${s}
    </span>
  `).join("");

  // Roadmap
  const roadmapBox = document.getElementById("careerRoadmapContent");
  roadmapBox.innerHTML = (data.roadmap || [
    "Build a production-grade distributed microservice implementing Kafka event streaming.",
    "Containerize your Java Spring Boot applications using multi-stage Docker builds.",
    "Master System Design trade-offs: Caching (Redis), Load Balancing, and Sharding."
  ]).map((step, idx) => `
    <div class="roadmap-item">
      <div class="roadmap-step-num font-mono">${idx + 1}</div>
      <div>
        <p class="text-sm font-semibold">${step}</p>
      </div>
    </div>
  `).join("");
}

// -------------------------------------------------------------
// 5. API Key Modal & Persistence
// -------------------------------------------------------------
function initApiKeyModal() {
  const modal = document.getElementById("keyModal");
  const btnOpen = document.getElementById("btnOpenKeyModal");
  const btnClose = document.getElementById("btnCloseKeyModal");
  const btnSave = document.getElementById("btnSaveApiKey");
  const input = document.getElementById("geminiApiKeyInput");
  const keyLabel = document.getElementById("keyLabel");

  if (geminiApiKey) {
    input.value = geminiApiKey;
    keyLabel.textContent = "KEY ACTIVE";
  }

  btnOpen.addEventListener("click", () => {
    modal.classList.add("open");
  });

  btnClose.addEventListener("click", () => {
    modal.classList.remove("open");
  });

  modal.addEventListener("click", (e) => {
    if (e.target === modal) modal.classList.remove("open");
  });

  btnSave.addEventListener("click", () => {
    geminiApiKey = input.value.trim();
    localStorage.setItem("smartcampus_gemini_key", geminiApiKey);
    keyLabel.textContent = geminiApiKey ? "KEY ACTIVE" : "API KEY";
    modal.classList.remove("open");
    showToast(geminiApiKey ? "Gemini API Key Saved!" : "Reverted to Offline Heuristic Mode.");
  });
}

// -------------------------------------------------------------
// 6. Backend Health & Connectivity
// -------------------------------------------------------------
async function checkBackendHealth() {
  try {
    const res = await fetch("/api/health");
    if (res.ok) {
      const data = await res.json();
      document.getElementById("systemStatusText").textContent = `ONLINE · ${data.mode || "JAVA 21"}`;
    }
  } catch (err) {
    document.getElementById("systemStatusText").textContent = "STANDBY · RUN MAIN.JAVA";
  }
}

function showToast(msg) {
  const toast = document.createElement("div");
  toast.className = "status-pill";
  toast.style.position = "fixed";
  toast.style.bottom = "24px";
  toast.style.right = "24px";
  toast.style.zIndex = "9999";
  toast.style.boxShadow = "var(--shadow-lg)";
  toast.innerHTML = `<span class="pulsing-dot"></span><span>${msg}</span>`;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2800);
}
