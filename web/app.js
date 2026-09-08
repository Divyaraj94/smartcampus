// State
let geminiApiKey = localStorage.getItem("smartcampus_gemini_key") || "";
let geminiModel = localStorage.getItem("smartcampus_gemini_model") || "gemini-2.5-flash";
if (geminiApiKey.includes("TestMockKey") || geminiApiKey === "AIzaSyTestMockKeyForVerification123") {
  localStorage.removeItem("smartcampus_gemini_key");
  geminiApiKey = "";
}
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

  // Save Notes & Connect to AI Button
  document.getElementById("btnStudySyncMaterial")?.addEventListener("click", () => {
    const notes = document.getElementById("studyNotesInput").value.trim();
    if (!notes) {
      showToast("Please paste your study material in the text area first.");
      document.getElementById("studyNotesInput").focus();
      return;
    }

    if (!geminiApiKey) {
      showToast("Notes saved! Please enter your Gemini API Key below to enable AI.");
      const keyBox = document.getElementById("studyInlineKeyBox");
      if (keyBox) {
        keyBox.style.display = "block";
        document.getElementById("studyInlineKeyInput")?.focus();
      }
    } else {
      showToast("Notes saved & connected to Gemini AI!");
    }
  });

  // Toggle Inline API Key Box
  document.getElementById("btnStudyOpenKey")?.addEventListener("click", () => {
    const keyBox = document.getElementById("studyInlineKeyBox");
    if (!keyBox) return;
    if (keyBox.style.display === "none" || !keyBox.style.display) {
      keyBox.style.display = "block";
      const inlineInput = document.getElementById("studyInlineKeyInput");
      if (inlineInput) {
        inlineInput.value = geminiApiKey;
        inlineInput.focus();
      }
    } else {
      keyBox.style.display = "none";
    }
  });

  // Save Inline Key
  document.getElementById("btnStudySaveInlineKey")?.addEventListener("click", () => {
    const val = document.getElementById("studyInlineKeyInput")?.value.trim() || "";
    const selModel = document.getElementById("studyInlineModelSelect")?.value || "gemini-2.5-flash";
    updateApiKeyUI(val, selModel);
    validateAndFetchModels(val);
    document.getElementById("studyInlineKeyBox").style.display = "none";
    showToast(val ? `Connected to ${selModel.toUpperCase()}` : "API Key cleared.");
  });

  // Clear Inline Key
  document.getElementById("btnStudyClearInlineKey")?.addEventListener("click", () => {
    updateApiKeyUI("", geminiModel);
    document.getElementById("studyInlineKeyBox").style.display = "none";
    showToast("API Key removed.");
  });

  // Close Inline Key
  document.getElementById("btnStudyCloseInlineKey")?.addEventListener("click", () => {
    document.getElementById("studyInlineKeyBox").style.display = "none";
  });

  // Ask AI Form Submit
  askForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const question = questionInput.value.trim();
    if (!question) return;

    if (!geminiApiKey) {
      const keyBox = document.getElementById("studyInlineKeyBox");
      if (keyBox) {
        keyBox.style.display = "block";
        document.getElementById("studyInlineKeyInput")?.focus();
      }
      showToast("Please enter your Gemini API Key to ask questions.");
      return;
    }

    const notes = document.getElementById("studyNotesInput").value.trim();

    // Append user bubble
    appendChatBubble(chatDisplay, "user", question);
    questionInput.value = "";

    // Append loading AI bubble
    const aiBubble = appendChatBubble(chatDisplay, "ai", `Analyzing material with ${geminiModel.toUpperCase()}...`);

    try {
      const response = await fetch("/api/study/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question, notes, apiKey: geminiApiKey, model: geminiModel })
      });

      const data = await response.json();
      if (!response.ok) {
        aiBubble.querySelector(".bubble-text").textContent = data.error || "Error from Gemini AI. Please check your API key.";
        return;
      }
      typewriterEffect(aiBubble.querySelector(".bubble-text"), data.answer || "No response received.");
    } catch (err) {
      aiBubble.querySelector(".bubble-text").textContent = "Network error connecting to backend. Ensure server is running on port 8080.";
    }
  });

  // Quiz Generator
  document.getElementById("btnGenerateQuiz")?.addEventListener("click", async () => {
    const notes = document.getElementById("studyNotesInput").value.trim();
    const quizContainer = document.getElementById("quizContainer");

    if (!notes) {
      quizContainer.innerHTML = `
        <div class="empty-state">
          <p class="text-amber text-sm text-center">Please paste your study notes in the text area above first, then click Generate Quiz.</p>
        </div>
      `;
      showToast("Please paste your study notes above first.");
      document.getElementById("studyNotesInput").focus();
      return;
    }

    if (!geminiApiKey) {
      const keyBox = document.getElementById("studyInlineKeyBox");
      if (keyBox) {
        keyBox.style.display = "block";
        document.getElementById("studyInlineKeyInput")?.focus();
      }
      showToast("Please enter your Gemini API Key to generate quizzes.");
      return;
    }

    quizContainer.innerHTML = `
      <div class="empty-state">
        <div class="pulsing-dot" style="margin: 0 auto 12px; width: 14px; height: 14px;"></div>
        <h4 class="font-display">Generating Quiz with ${geminiModel.toUpperCase()}...</h4>
        <p class="text-muted text-sm">Synthesizing multiple-choice questions from your notes.</p>
      </div>
    `;

    try {
      const response = await fetch("/api/study/quiz", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ notes, apiKey: geminiApiKey, model: geminiModel })
      });

      const data = await response.json();
      if (!response.ok) {
        quizContainer.innerHTML = `<div class="empty-state"><p class="text-amber text-sm text-center">${data.error || "Failed to generate quiz. Please check your Gemini API key."}</p></div>`;
        return;
      }
      renderQuiz(data.questions || []);
    } catch (err) {
      quizContainer.innerHTML = `<p class="text-amber text-sm text-center">Failed to generate quiz. Please check server connection.</p>`;
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

    if (!geminiApiKey) {
      document.getElementById("keyModal")?.classList.add("open");
      showToast("Please enter your Gemini API Key to run ATS Analysis.");
      return;
    }

    btnAnalyze.disabled = true;
    btnAnalyze.querySelector("span").textContent = `Analyzing with ${geminiModel.toUpperCase()}...`;

    try {
      const response = await fetch("/api/career/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resume, targetRole, jobDescription, apiKey: geminiApiKey, model: geminiModel })
      });

      const data = await response.json();
      if (!response.ok) {
        alert(data.error || "Failed to analyze resume. Please check your Gemini API key.");
        return;
      }
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

    if (!geminiApiKey) {
      document.getElementById("keyModal")?.classList.add("open");
      showToast("Please enter your Gemini API Key for Mock Interview.");
      return;
    }

    btnStartInterview.disabled = true;
    currentQText.textContent = `Generating interview question with ${geminiModel.toUpperCase()}...`;
    interviewBox.style.display = "block";
    feedbackBox.style.display = "none";

    try {
      const response = await fetch("/api/career/interview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "start", resume, targetRole, apiKey: geminiApiKey, model: geminiModel })
      });

      const data = await response.json();
      if (!response.ok) {
        currentQText.textContent = data.error || "Error generating interview question. Please check your Gemini API key.";
        activeInterviewQuestion = "";
        return;
      }
      activeInterviewQuestion = data.question;
      currentQText.textContent = activeInterviewQuestion;
    } catch (err) {
      currentQText.textContent = "Error generating interview question. Please check your Gemini API key and network connection.";
      activeInterviewQuestion = "";
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

    if (!geminiApiKey) {
      document.getElementById("keyModal")?.classList.add("open");
      showToast("Please enter your Gemini API Key to grade interview.");
      return;
    }

    btnSubmitAnswer.disabled = true;
    btnSubmitAnswer.querySelector("span").textContent = `Grading with ${geminiModel.toUpperCase()}...`;

    try {
      const response = await fetch("/api/career/interview", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "evaluate",
          question: activeInterviewQuestion,
          answer,
          targetRole,
          apiKey: geminiApiKey,
          model: geminiModel
        })
      });

      const data = await response.json();
      if (!response.ok) {
        alert(data.error || "Failed to grade interview answer. Please check your Gemini API key.");
        return;
      }
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
// 6. API Key & Model Synchronization & Persistence
// -------------------------------------------------------------
function updateApiKeyUI(key, model) {
  geminiApiKey = (key || "").trim();
  if (model) geminiModel = model.trim();

  if (geminiApiKey) {
    localStorage.setItem("smartcampus_gemini_key", geminiApiKey);
  } else {
    localStorage.removeItem("smartcampus_gemini_key");
  }
  localStorage.setItem("smartcampus_gemini_model", geminiModel);

  // Send to backend
  fetch("/api/config/key", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ apiKey: geminiApiKey })
  }).catch(() => {});

  // Update Top Navbar
  const keyLabel = document.getElementById("keyLabel");
  if (keyLabel) keyLabel.textContent = geminiApiKey ? "KEY ACTIVE" : "API KEY";

  const activeModelLabel = document.getElementById("activeModelLabel");
  if (activeModelLabel) {
    activeModelLabel.textContent = geminiModel.toUpperCase().replace(/-/g, " ");
  }

  // Update Modal Inputs
  const modalInput = document.getElementById("geminiApiKeyInput");
  if (modalInput) modalInput.value = geminiApiKey;

  const modalModelSelect = document.getElementById("geminiModelSelect");
  if (modalModelSelect) modalModelSelect.value = geminiModel;

  // Update Study Card Key elements
  const studyInlineInput = document.getElementById("studyInlineKeyInput");
  if (studyInlineInput) studyInlineInput.value = geminiApiKey;

  const studyInlineModelSelect = document.getElementById("studyInlineModelSelect");
  if (studyInlineModelSelect) studyInlineModelSelect.value = geminiModel;

  const studyKeyBtnText = document.getElementById("studyKeyBtnText");
  if (studyKeyBtnText) {
    studyKeyBtnText.textContent = geminiApiKey ? `Key: ${geminiModel.toUpperCase().replace(/-/g, " ")}` : "Gemini API Key";
  }

  const statusLabel = document.getElementById("studyAiStatusLabel");
  const statusDot = document.getElementById("studyAiStatusDot");
  if (statusLabel && statusDot) {
    if (geminiApiKey) {
      statusLabel.textContent = `Live ${geminiModel.toUpperCase().replace(/-/g, " ")}`;
      statusDot.style.backgroundColor = "var(--emerald)";
      statusDot.style.boxShadow = "0 0 8px rgba(16, 185, 129, 0.7)";
    } else {
      statusLabel.textContent = "API Key Required";
      statusDot.style.backgroundColor = "var(--amber)";
      statusDot.style.boxShadow = "none";
    }
  }
}

function initApiKeyModal() {
  const modal = document.getElementById("keyModal");
  const btnOpen = document.getElementById("btnOpenKeyModal");
  const btnModelBadge = document.getElementById("btnModelBadge");
  const btnClose = document.getElementById("btnCloseKeyModal");
  const btnSave = document.getElementById("btnSaveApiKey");
  const input = document.getElementById("geminiApiKeyInput");
  const modelSelect = document.getElementById("geminiModelSelect");

  updateApiKeyUI(geminiApiKey, geminiModel);

  const openModal = () => {
    if (input) input.value = geminiApiKey;
    if (modelSelect) modelSelect.value = geminiModel;
    modal?.classList.add("open");
  };

  btnOpen?.addEventListener("click", openModal);
  btnModelBadge?.addEventListener("click", openModal);

  btnClose?.addEventListener("click", () => {
    modal?.classList.remove("open");
  });

  modal?.addEventListener("click", (e) => {
    if (e.target === modal) modal.classList.remove("open");
  });

  btnSave?.addEventListener("click", () => {
    const key = input ? input.value.trim() : "";
    const selectedModel = modelSelect ? modelSelect.value : "gemini-2.5-flash";
    updateApiKeyUI(key, selectedModel);
    validateAndFetchModels(key);
    modal?.classList.remove("open");
    showToast(key ? `Connected to ${selectedModel.toUpperCase()}` : "Settings Saved");
  });

  if (geminiApiKey) {
    validateAndFetchModels(geminiApiKey);
  }
}

async function validateAndFetchModels(key) {
  if (!key) return;
  try {
    const res = await fetch(`/api/config/models?key=${encodeURIComponent(key)}`);
    const data = await res.json();
    if (data.status === "OK" && Array.isArray(data.models) && data.models.length > 0) {
      if (!data.models.includes(geminiModel)) {
        geminiModel = data.models[0];
        localStorage.setItem("smartcampus_gemini_model", geminiModel);
        const modalSelect = document.getElementById("geminiModelSelect");
        const inlineSelect = document.getElementById("studyInlineModelSelect");
        if (modalSelect) modalSelect.value = geminiModel;
        if (inlineSelect) inlineSelect.value = geminiModel;
        const activeModelLabel = document.getElementById("activeModelLabel");
        if (activeModelLabel) activeModelLabel.textContent = geminiModel.toUpperCase().replace(/-/g, " ");
      }
    } else if (data.status === "ERROR") {
      showToast(`Key Warning: ${data.error}`);
    }
  } catch (err) {
    // Network fallback
  }
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
    document.getElementById("systemStatusText").textContent = "DISCONNECTED";
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
