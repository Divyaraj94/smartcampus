// State
let geminiApiKey = localStorage.getItem("smartcampus_gemini_key") || "";
let activeInterviewQuestion = "";
let currentAtsKeywords = [];

// Initialize PDF.js worker
if (window.pdfjsLib) {
  pdfjsLib.GlobalWorkerOptions.workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js";
}

document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  initStudyHub();
  initCareerLab();
  initResumeFileUpload();
  initApiKeyModal();
  checkBackendHealth();

  // Clear notes button
  document.getElementById("btnClearNotes")?.addEventListener("click", () => {
    document.getElementById("studyNotesInput").value = "";
    updateWordCount();
  });

  // Word count on notes input
  document.getElementById("studyNotesInput")?.addEventListener("input", updateWordCount);

  // Word count on resume input
  document.getElementById("careerResumeInput")?.addEventListener("input", updateResumeWordCount);
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
// 2. Word Count Helpers
// -------------------------------------------------------------
function updateWordCount() {
  const text = document.getElementById("studyNotesInput").value.trim();
  const words = text ? text.split(/\s+/).length : 0;
  const chars = text.length;
  document.getElementById("studyWordCount").textContent = `${words} words · ${chars} characters`;
}

function updateResumeWordCount() {
  const text = document.getElementById("careerResumeInput")?.value.trim() || "";
  const words = text ? text.split(/\s+/).length : 0;
  const countEl = document.getElementById("resumeWordCount");
  if (countEl) countEl.textContent = `${words} words`;
}

// -------------------------------------------------------------
// 3. Resume File Upload (PDF.js + Text Reader)
// -------------------------------------------------------------
function initResumeFileUpload() {
  const dropzone = document.getElementById("resumeDropzone");
  const fileInput = document.getElementById("resumeFileInput");
  const resumeInput = document.getElementById("careerResumeInput");
  const uploadStatus = document.getElementById("fileUploadStatus");
  const fileNameEl = document.getElementById("uploadedFileName");
  const filePagesEl = document.getElementById("uploadedFilePages");
  const btnRemove = document.getElementById("btnRemoveFile");

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener("click", (e) => {
    if (e.target !== btnRemove && !btnRemove.contains(e.target)) {
      fileInput.click();
    }
  });

  dropzone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropzone.classList.add("dragover");
  });

  dropzone.addEventListener("dragleave", () => {
    dropzone.classList.remove("dragover");
  });

  dropzone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropzone.classList.remove("dragover");
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleResumeFile(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener("change", (e) => {
    if (e.target.files && e.target.files.length > 0) {
      handleResumeFile(e.target.files[0]);
    }
  });

  btnRemove?.addEventListener("click", (e) => {
    e.stopPropagation();
    fileInput.value = "";
    uploadStatus.style.display = "none";
    resumeInput.value = "";
    updateResumeWordCount();
    showToast("Uploaded resume removed.");
  });

  async function handleResumeFile(file) {
    fileNameEl.textContent = file.name;
    uploadStatus.style.display = "inline-flex";

    if (file.type === "application/pdf" || file.name.endsWith(".pdf")) {
      if (window.pdfjsLib) {
        filePagesEl.textContent = "(Extracting PDF...)";
        try {
          const arrayBuffer = await file.arrayBuffer();
          const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
          let extractedText = "";
          for (let i = 1; i <= pdf.numPages; i++) {
            const page = await pdf.getPage(i);
            const content = await page.getTextContent();
            extractedText += content.items.map(item => item.str).join(" ") + "\n";
          }
          resumeInput.value = extractedText.trim();
          filePagesEl.textContent = `(${pdf.numPages} ${pdf.numPages === 1 ? 'page' : 'pages'})`;
          updateResumeWordCount();
          showToast(`Parsed ${pdf.numPages} pages from ${file.name}`);
        } catch (err) {
          alert("Failed to parse PDF file. Make sure it contains readable text.");
          filePagesEl.textContent = "(Error parsing)";
        }
      } else {
        const reader = new FileReader();
        reader.onload = (event) => {
          const raw = event.target.result;
          const clean = raw.replace(/[^\x20-\x7E\n\r\t]/g, " ").replace(/\s+/g, " ");
          resumeInput.value = clean.trim();
          filePagesEl.textContent = "(Parsed Text)";
          updateResumeWordCount();
          showToast(`Loaded ${file.name}`);
        };
        reader.readAsText(file);
      }
    } else {
      // Plain text
      const reader = new FileReader();
      reader.onload = (event) => {
        resumeInput.value = event.target.result;
        filePagesEl.textContent = `(${(file.size / 1024).toFixed(1)} KB)`;
        updateResumeWordCount();
        showToast(`Loaded ${file.name}`);
      };
      reader.readAsText(file);
    }
  }

  // Copy ATS keywords button
  document.getElementById("btnCopyAtsKeywords")?.addEventListener("click", () => {
    if (currentAtsKeywords.length === 0) return;
    navigator.clipboard.writeText(currentAtsKeywords.join(", "));
    showToast("Copied all ATS keywords to clipboard");
  });
}

// -------------------------------------------------------------
// 4. Study Hub
// -------------------------------------------------------------
function initStudyHub() {
  const askForm = document.getElementById("studyAskForm");
  const questionInput = document.getElementById("studyQuestionInput");
  const chatDisplay = document.getElementById("studyChatDisplay");

  askForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const question = questionInput.value.trim();
    if (!question) return;

    const notes = document.getElementById("studyNotesInput").value.trim();

    // Append user bubble
    appendChatBubble(chatDisplay, "user", question);
    questionInput.value = "";

    // Append loading AI bubble
    const aiBubble = appendChatBubble(chatDisplay, "ai", "Analyzing and synthesizing answer...");

    try {
      const response = await fetch("/api/study/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question, notes, apiKey: geminiApiKey })
      });

      const data = await response.json();
      typewriterEffect(aiBubble.querySelector(".bubble-text"), data.answer || "No response received.");
    } catch (err) {
      aiBubble.querySelector(".bubble-text").textContent = "Network error connecting to backend. Ensure server is running on port 8080.";
    }
  });

  // Quiz Generator
  document.getElementById("btnGenerateQuiz")?.addEventListener("click", async () => {
    const notes = document.getElementById("studyNotesInput").value.trim();
    const quizContainer = document.getElementById("quizContainer");

    quizContainer.innerHTML = `
      <div class="empty-state">
        <div class="pulsing-dot" style="margin: 0 auto 12px; width: 14px; height: 14px;"></div>
        <h4 class="font-display">Generating Quiz...</h4>
        <p class="text-muted text-sm">Synthesizing multiple-choice questions from your notes.</p>
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
    <span>${type === "user" ? "YOU" : "AI ASSISTANT"}</span>
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
            b.querySelector(".opt-status").textContent = "CORRECT";
          } else if (idx === selected) {
            b.classList.add("wrong");
            b.querySelector(".opt-status").textContent = "INCORRECT";
          }
        });
      });
    });

    quizContainer.appendChild(card);
  });
}

// -------------------------------------------------------------
// 5. Career & Placement (ATS Suite + Study Plan)
// -------------------------------------------------------------
function initCareerLab() {
  const btnAnalyze = document.getElementById("btnAnalyzeResume");
  const resultsContainer = document.getElementById("careerResultsContainer");

  btnAnalyze.addEventListener("click", async () => {
    const resume = document.getElementById("careerResumeInput").value.trim();
    const targetRole = document.getElementById("careerTargetRole").value.trim();
    const jobDescription = document.getElementById("careerJdInput").value.trim();

    if (!resume) {
      alert("Please upload your resume (PDF or TXT) or paste the text first.");
      return;
    }

    btnAnalyze.disabled = true;
    btnAnalyze.querySelector("span").textContent = "Analyzing...";

    try {
      const response = await fetch("/api/career/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resume, targetRole, jobDescription, apiKey: geminiApiKey })
      });

      const data = await response.json();
      renderCareerResults(data);
      resultsContainer.style.display = "block";
      resultsContainer.scrollIntoView({ behavior: "smooth" });
    } catch (err) {
      alert("Error analyzing resume. Please ensure the server is running on port 8080.");
    } finally {
      btnAnalyze.disabled = false;
      btnAnalyze.querySelector("span").textContent = "Run ATS Analysis";
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
      currentQText.textContent = "Describe a challenging project you worked on and how you handled the technical decisions.";
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
  const scoreVal = data.atsScore || data.score || 85;
  document.getElementById("scoreNumber").textContent = `${scoreVal}%`;
  document.getElementById("scoreHeadline").textContent = scoreVal >= 80 ? "High ATS Pass Likelihood" : scoreVal >= 60 ? "Moderate Alignment" : "Needs Keyword Optimization";
  document.getElementById("scoreSubtitle").textContent = data.summary || "Profile evaluation complete.";

  const circle = document.getElementById("scoreProgressCircle");
  const circumference = 364.4;
  const offset = circumference - (scoreVal / 100) * circumference;
  circle.style.strokeDashoffset = offset;

  // Sub-Meters
  const breakdown = data.breakdown || { keywords: 85, impact: 75, formatting: 90 };
  document.getElementById("subKeywordsScore").textContent = `${breakdown.keywords}%`;
  document.getElementById("meterKeywordsBar").style.width = `${breakdown.keywords}%`;

  document.getElementById("subImpactScore").textContent = `${breakdown.impact}%`;
  document.getElementById("meterImpactBar").style.width = `${breakdown.impact}%`;

  document.getElementById("subFormatScore").textContent = `${breakdown.formatting}%`;
  document.getElementById("meterFormatBar").style.width = `${breakdown.formatting}%`;

  // Matched Skills
  const matchedList = document.getElementById("matchedSkillsList");
  matchedList.innerHTML = (data.matchedSkills || []).map(s => `
    <span class="skill-tag skill-tag-emerald">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
      ${s}
    </span>
  `).join("");

  // Missing Skills
  const missingList = document.getElementById("missingSkillsList");
  missingList.innerHTML = (data.missingSkills || []).map(s => `
    <span class="skill-tag skill-tag-amber">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      ${s}
    </span>
  `).join("");

  // ATS Missing Keywords
  currentAtsKeywords = data.atsKeywords || [];
  const atsKeywordsList = document.getElementById("atsKeywordsList");
  atsKeywordsList.innerHTML = currentAtsKeywords.map(kw => `
    <span class="skill-tag" style="background: rgba(0, 82, 255, 0.08); border: 1px solid var(--accent-border); color: var(--accent);">
      <span style="font-weight: 700;">+</span> ${kw}
    </span>
  `).join("");

  // Bullet Point Rewrites
  const bulletBox = document.getElementById("bulletRewritesContainer");
  const rewrites = data.bulletRewrites || [];
  bulletBox.innerHTML = rewrites.map(b => `
    <div class="bullet-card">
      <div class="bullet-orig">
        <span class="font-mono text-xs font-semibold" style="display: block; margin-bottom: 2px;">BEFORE:</span>
        "${b.original}"
      </div>
      <div class="bullet-improved">
        <span class="font-mono text-xs font-semibold" style="display: block; margin-bottom: 2px;">AFTER (OPTIMIZED):</span>
        "${b.improved}"
      </div>
      <div class="bullet-rationale"><strong>Why this works:</strong> ${b.rationale}</div>
    </div>
  `).join("");

  // Pre-Interview Study Guide
  const studyGuide = data.studyGuide || {};
  const coreTopicsList = document.getElementById("studyCoreTopicsList");
  coreTopicsList.innerHTML = (studyGuide.coreTopics || []).map(t => `
    <li class="study-item">
      <span class="study-bullet-dot"></span>
      <span>${t}</span>
    </li>
  `).join("");

  const systemDesignList = document.getElementById("studySystemDesignList");
  systemDesignList.innerHTML = (studyGuide.systemDesign || []).map(t => `
    <li class="study-item">
      <span class="study-bullet-dot" style="background: var(--emerald);"></span>
      <span>${t}</span>
    </li>
  `).join("");

  const questionsList = document.getElementById("studyQuestionsList");
  questionsList.innerHTML = (studyGuide.interviewQuestions || []).map((q, idx) => `
    <div class="interview-qa-card">
      <div class="qa-question">Q${idx + 1}: ${q.question}</div>
      <div class="qa-tip font-mono"><strong>Interviewer Evaluates:</strong> ${q.tip}</div>
    </div>
  `).join("");

  // Roadmap
  const roadmapBox = document.getElementById("careerRoadmapContent");
  roadmapBox.innerHTML = (data.roadmap || []).map((step, idx) => `
    <div class="roadmap-item">
      <div class="roadmap-step-num font-mono">${idx + 1}</div>
      <div>
        <p class="text-sm font-semibold">${step}</p>
      </div>
    </div>
  `).join("");
}

// -------------------------------------------------------------
// 6. API Key Modal & Persistence
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
    showToast(geminiApiKey ? "Gemini API Key Saved" : "Reverted to Offline Mode");
  });
}

// -------------------------------------------------------------
// 7. Backend Health & Connectivity
// -------------------------------------------------------------
async function checkBackendHealth() {
  try {
    const res = await fetch("/api/health");
    if (res.ok) {
      const data = await res.json();
      document.getElementById("systemStatusText").textContent = `ONLINE`;
    }
  } catch (err) {
    document.getElementById("systemStatusText").textContent = "OFFLINE";
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
