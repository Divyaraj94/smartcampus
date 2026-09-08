# 📖 SmartCampus AI — Setup & Run Instructions

Welcome! This guide will walk you through running **SmartCampus AI** on **Windows** and **macOS**.  
The project is built entirely on the **Java 21 Standard Library**, meaning **NO Maven, NO Gradle, NO Node.js, and NO external dependencies** are needed!

---

## ⚡ Quick Checklist Before Starting

You only need **Java (JDK 17 or 21+)** installed on your computer.
To check if Java is installed, open your terminal (or Command Prompt) and type:
```bash
java -version
```
* If it prints `java version "17..."` or `"21..."`, you are good to go!
* If it says `command not found` or `not recognized`, install Java quickly:
  * **Windows**: Run `winget install Microsoft.OpenJDK.21` or download from [adoptium.net](https://adoptium.net/).
  * **Mac**: Run `brew install openjdk@21` or download the `.pkg` installer from [adoptium.net](https://adoptium.net/).

---

## 🪟 Instructions for Windows Users

You have two easy ways to run the project:

### Method 1: The 1-Click Way (Easiest)
1. Open the project folder.
2. Double-click the file named **`run.bat`**.
3. It will automatically compile the Java files, start the server, and open your web browser to `http://localhost:8080`!

### Method 2: Using Command Prompt or PowerShell
1. Open **Command Prompt** (`cmd`) or **PowerShell** in the project directory.
2. Compile the backend:
   ```cmd
   javac -d bin src/SmartCampusApp.java
   ```
3. Run the server:
   ```cmd
   java -cp bin src.SmartCampusApp
   ```
4. Open your browser (Chrome, Edge, Firefox) and navigate to:
   ```
   http://localhost:8080
   ```

---

## 🍎 Instructions for macOS Users

Mac users can run the project directly through the Terminal:

### Method 1: Using the Run Script
1. Open **Terminal** (`Cmd + Space`, type `Terminal`, press Enter).
2. Navigate to the project folder:
   ```bash
   cd /path/to/Java
   ```
3. Give execution permission and run the script:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```
4. The script will compile the code, launch the server, and automatically open your default browser!

### Method 2: Manual Terminal Commands
1. In Terminal, navigate to the project directory:
   ```bash
   cd /path/to/Java
   ```
2. Compile:
   ```bash
   mkdir -p bin
   javac -d bin src/SmartCampusApp.java
   ```
3. Run:
   ```bash
   java -cp bin src.SmartCampusApp
   ```
4. Open Safari or Chrome and go to:
   ```
   http://localhost:8080
   ```

---

## 🎮 How to Demo & Test the Project

Once the page opens at `http://localhost:8080`, here is how you can test all features:

### 1. 📚 Academic Study Hub Tab
1. Click **"Load Sample Java Notes"** (or paste your own textbook/lecture text).
2. Click any suggestion chip (e.g. **"OOP Pillars"** or **"JVM Architecture"**) and click **"Ask AI"**.
   * Watch the AI type out a structured, bulleted response!
3. Click **"Generate Quiz"**.
   * Test answering the multiple-choice questions—correct options highlight in green and provide instant explanations.

### 2. 💼 Career & Placement Lab Tab
1. Switch to the **"Career & Placement Lab"** tab.
2. **Upload a Resume**:
   * Drag & drop any real `.pdf` or `.txt` resume into the dashed box (or click **"Load Sample Student Resume"**).
3. **Select Target Role & Job Description**:
   * Click any role preset like **"Google SDE-1"**, **"Amazon SDE"**, or **"FinTech Backend"** (or paste any real job posting into the JD box).
4. Click **"Run ATS & Placement Intelligence"**:
   * **ATS Readiness Score**: See the overall pass likelihood and the 3 sub-meters (Keywords, Impact, Formatting).
   * **Missing Keywords**: High-priority keywords detected from the job description, with a 1-click **"Copy All Keywords"** button.
   * **Resume Bullet Optimizer**: Side-by-side comparison showing weak bullets rewritten into quantifiable STAR format achievements.
   * **Pre-Interview Masterclass**: Revision topics on Core Java/Concurrency, System Design, and 5 likely technical interview questions.
5. **AI Technical Mock Interview**:
   * Scroll down to the dark simulation section.
   * Click **"Start New Interview Session"**, type an answer, and click **"Submit Answer for AI Grading"** to receive a grade out of 10 with actionable feedback.

---

## 🔑 (Optional) Using a Live Google Gemini API Key
The application includes a built-in **Offline Heuristic AI Engine**, so it works **100% offline without any API key**.

If you want to use live Google Cloud AI:
1. Click the **"API KEY"** button in the top-right navbar.
2. Paste your free Google Gemini API key (from [Google AI Studio](https://aistudio.google.com/)).
3. Click **"Save Key"**. The server will immediately switch to live Gemini AI mode!

---

## 🛠️ Common Troubleshooting

* **Port 8080 is already in use**:
  * If another application is using port 8080, open `src/SmartCampusApp.java`, change line 33 from `private static final int PORT = 8080;` to `8081` (or any available port), recompile, and open `http://localhost:8081`.
* **How to stop the server**:
  * In the terminal running the app, press **`Ctrl + C`**.
