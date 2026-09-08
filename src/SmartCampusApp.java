package src;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmartCampus AI -- Core Server
 *
 * Study & Career Intelligence Platform
 * - Dual Engine: Live Google Gemini AI + Offline Heuristic Analyzer
 * - REST API on port 8080
 */
public class SmartCampusApp {

    private static final int PORT = 8080;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Default or Environment API Key (fallback)
    private static volatile String globalGeminiKey = System.getenv().getOrDefault("GEMINI_API_KEY", "");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // Java 21 Virtual Threads

        // Endpoints
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/study/ask", new StudyAskHandler());
        server.createContext("/api/study/quiz", new StudyQuizHandler());
        server.createContext("/api/career/analyze", new CareerAnalyzeHandler());
        server.createContext("/api/career/interview", new CareerInterviewHandler());
        server.createContext("/api/config/key", new ConfigKeyHandler());

        server.start();
        System.out.println("==========================================================");
        System.out.println("  SmartCampus AI Platform — Server Active");
        System.out.println("  URL: http://localhost:" + PORT);
        System.out.println("==========================================================");
    }

    // =========================================================================
    // Static File Handler (Serves web/ folder)
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            Path filePath = Paths.get("web", path.startsWith("/") ? path.substring(1) : path);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendJsonResponse(exchange, 404, "{\"error\":\"File not found\"}");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    // Health Check Endpoint
    // =========================================================================
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = "{"
                    + "\"status\":\"ONLINE\","
                    + "\"hasApiKey\":" + (!globalGeminiKey.isBlank()) + ","
                    + "\"mode\":\"" + (!globalGeminiKey.isBlank() ? "LIVE AI" : "OFFLINE") + "\""
                    + "}";
            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // Academic Study Q&A Handler
    // =========================================================================
    static class StudyAskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String question = extractJsonField(body, "question");
            String notes = extractJsonField(body, "notes");
            String apiKey = extractJsonField(body, "apiKey");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            String answer;
            if (!apiKey.isBlank()) {
                try {
                    String prompt = "You are an expert technical tutor and study assistant. Answer the question clearly and accurately based on the notes below. Use clear bullet points and highlight key technical terms.\n\n"
                            + "NOTES:\n" + (notes.isBlank() ? "Key technical concepts and architecture" : notes) + "\n\n"
                            + "QUESTION:\n" + question;
                    answer = callGeminiApi(apiKey, prompt);
                } catch (Exception e) {
                    System.err.println("Gemini API call failed, using intelligent fallback: " + e.getMessage());
                    answer = generateHeuristicStudyAnswer(question, notes);
                }
            } else {
                answer = generateHeuristicStudyAnswer(question, notes);
            }

            String responseJson = "{\"answer\":\"" + escapeJson(answer) + "\"}";
            sendJsonResponse(exchange, 200, responseJson);
        }
    }

    // =========================================================================
    // Revision Quiz & Flashcard Generator Handler
    // =========================================================================
    static class StudyQuizHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String notes = extractJsonField(body, "notes");
            String apiKey = extractJsonField(body, "apiKey");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            String responseJson;
            if (!apiKey.isBlank()) {
                try {
                    String prompt = "Create 3 multiple-choice questions (MCQs) for revision based on the following notes. "
                            + "Respond strictly with valid JSON in this exact structure without markdown backticks:\n"
                            + "{\"questions\":[{\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswerIndex\":0,\"explanation\":\"...\"}]}\n\n"
                            + "NOTES:\n" + (notes.isBlank() ? "Key concepts and technical principles" : notes);
                    String raw = callGeminiApi(apiKey, prompt);
                    // Extract JSON from output
                    responseJson = cleanJsonOutput(raw);
                } catch (Exception e) {
                    responseJson = generateHeuristicQuizJson(notes);
                }
            } else {
                responseJson = generateHeuristicQuizJson(notes);
            }

            sendJsonResponse(exchange, 200, responseJson);
        }
    }

    // =========================================================================
    // Career & Placement Gap Analyzer Handler
    // =========================================================================
    static class CareerAnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String resume = extractJsonField(body, "resume");
            String targetRole = extractJsonField(body, "targetRole");
            String jobDescription = extractJsonField(body, "jobDescription");
            String apiKey = extractJsonField(body, "apiKey");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            String responseJson;
            if (!apiKey.isBlank()) {
                try {
                    String prompt = "You are an expert tech recruiter, ATS specialist, and senior hiring manager. "
                            + "Analyze this candidate resume against the Target Role: '" + targetRole + "' and Job Description: '" + jobDescription + "'.\n"
                            + "Respond strictly with valid JSON without markdown code blocks using this exact format:\n"
                            + "{\n"
                            + "  \"score\": 85,\n"
                            + "  \"atsScore\": 82,\n"
                            + "  \"breakdown\": {\"keywords\": 78, \"impact\": 74, \"formatting\": 92},\n"
                            + "  \"summary\": \"2-3 sentence executive assessment\",\n"
                            + "  \"matchedSkills\": [\"Skill1\", \"Skill2\"],\n"
                            + "  \"missingSkills\": [\"SkillA\", \"SkillB\"],\n"
                            + "  \"atsKeywords\": [\"Keyword1\", \"Keyword2\", \"Keyword3\", \"Keyword4\"],\n"
                            + "  \"bulletRewrites\": [\n"
                            + "    {\"original\": \"generic bullet from resume\", \"improved\": \"STAR formatted bullet with metrics and strong action verbs\", \"rationale\": \"explanation\"}\n"
                            + "  ],\n"
                            + "  \"studyGuide\": {\n"
                            + "    \"coreTopics\": [\"topic 1\", \"topic 2\"],\n"
                            + "    \"systemDesign\": [\"design concept 1\", \"design concept 2\"],\n"
                            + "    \"interviewQuestions\": [\n"
                            + "      {\"question\": \"question text\", \"tip\": \"what the interviewer evaluates\"}\n"
                            + "    ]\n"
                            + "  },\n"
                            + "  \"roadmap\": [\"step 1\", \"step 2\", \"step 3\"]\n"
                            + "}\n\n"
                            + "RESUME:\n" + resume + "\n\n"
                            + "JOB DESCRIPTION:\n" + (jobDescription.isBlank() ? "Standard requirements for " + targetRole : jobDescription);
                    String raw = callGeminiApi(apiKey, prompt);
                    responseJson = cleanJsonOutput(raw);
                } catch (Exception e) {
                    responseJson = generateHeuristicCareerJson(resume, targetRole, jobDescription);
                }
            } else {
                responseJson = generateHeuristicCareerJson(resume, targetRole, jobDescription);
            }

            sendJsonResponse(exchange, 200, responseJson);
        }
    }

    // =========================================================================
    // AI Mock Interview Handler
    // =========================================================================
    static class CareerInterviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String action = extractJsonField(body, "action");
            String resume = extractJsonField(body, "resume");
            String targetRole = extractJsonField(body, "targetRole");
            String question = extractJsonField(body, "question");
            String answer = extractJsonField(body, "answer");
            String apiKey = extractJsonField(body, "apiKey");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if ("start".equalsIgnoreCase(action)) {
                String questionText;
                if (!apiKey.isBlank()) {
                    try {
                        String prompt = "Generate a single challenging technical interview question for a candidate applying for: " + targetRole + " with this resume:\n" + resume;
                        questionText = callGeminiApi(apiKey, prompt);
                    } catch (Exception e) {
                        questionText = getHeuristicInterviewQuestion(targetRole);
                    }
                } else {
                    questionText = getHeuristicInterviewQuestion(targetRole);
                }
                sendJsonResponse(exchange, 200, "{\"question\":\"" + escapeJson(questionText.trim()) + "\"}");
            } else {
                // evaluate
                String feedback;
                String score;
                if (!apiKey.isBlank()) {
                    try {
                        String prompt = "You are a lead technical interviewer for " + targetRole + ".\n"
                                + "Question: " + question + "\n"
                                + "Candidate Answer: " + answer + "\n\n"
                                + "Evaluate the answer. Respond strictly with JSON: {\"score\":\"8.5\",\"feedback\":\"2-3 sentences of feedback and technical improvements\"}";
                        String raw = callGeminiApi(apiKey, prompt);
                        sendJsonResponse(exchange, 200, cleanJsonOutput(raw));
                        return;
                    } catch (Exception e) {
                        feedback = evaluateHeuristicInterview(answer);
                        score = "8.2";
                    }
                } else {
                    feedback = evaluateHeuristicInterview(answer);
                    score = "8.2";
                }
                sendJsonResponse(exchange, 200, "{\"score\":\"" + score + "\",\"feedback\":\"" + escapeJson(feedback) + "\"}");
            }
        }
    }

    // =========================================================================
    // Save API Key Configuration Handler
    // =========================================================================
    static class ConfigKeyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                String key = extractJsonField(body, "apiKey");
                globalGeminiKey = key;
                sendJsonResponse(exchange, 200, "{\"status\":\"SAVED\",\"hasKey\":" + (!globalGeminiKey.isBlank()) + "}");
            } else {
                sendJsonResponse(exchange, 200, "{\"hasKey\":" + (!globalGeminiKey.isBlank()) + "}");
            }
        }
    }

    // =========================================================================
    // Google Gemini REST API Client (Standard java.net.http.HttpClient)
    // =========================================================================
    private static String callGeminiApi(String apiKey, String prompt) throws Exception {
        // Supports current Gemini models with automatic candidate fallback
        String[] candidateModels = {
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash",
            "gemini-2.0-flash-exp"
        };
        Exception lastException = null;

        String payload = "{"
                + "\"contents\": [{"
                + "  \"parts\": [{\"text\": \"" + escapeJson(prompt) + "\"}]"
                + "}]"
                + "}";

        for (String model : candidateModels) {
            try {
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    Pattern textPattern = Pattern.compile("\"text\":\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
                    Matcher matcher = textPattern.matcher(responseBody);
                    if (matcher.find()) {
                        return unescapeJson(matcher.group(1));
                    }
                    return "Response received from Gemini.";
                } else if (response.statusCode() == 404) {
                    lastException = new RuntimeException("Gemini HTTP 404 on " + model);
                    continue;
                } else {
                    throw new RuntimeException("Gemini HTTP " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception ex) {
                lastException = ex;
                if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                    continue;
                }
                throw ex;
            }
        }

        if (lastException != null) throw lastException;
        throw new RuntimeException("All Gemini model endpoints failed.");
    }

    // =========================================================================
    // Offline Heuristic Engines (Dynamic Fallback)
    // =========================================================================
    private static String generateHeuristicStudyAnswer(String question, String notes) {
        StringBuilder sb = new StringBuilder();

        // Extract keywords from the question to search in notes
        String[] questionWords = question.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String w : questionWords) {
            if (w.length() > 3 && !List.of("what", "which", "where", "when", "that", "this", "with", "from", "have", "does", "explain", "describe", "about").contains(w)) {
                keywords.add(w);
            }
        }

        // Search provided notes for relevant sentences
        if (notes != null && !notes.isBlank()) {
            String[] sentences = notes.split("\\. |\\n");
            List<String> relevant = new ArrayList<>();
            for (String s : sentences) {
                String lower = s.trim().toLowerCase();
                if (lower.length() > 15) {
                    for (String kw : keywords) {
                        if (lower.contains(kw)) {
                            relevant.add(s.trim());
                            break;
                        }
                    }
                }
            }

            if (!relevant.isEmpty()) {
                sb.append("Based on your uploaded notes:\n\n");
                int count = 0;
                for (String r : relevant) {
                    sb.append("- ").append(r);
                    if (!r.endsWith(".")) sb.append(".");
                    sb.append("\n");
                    count++;
                    if (count >= 5) break;
                }
            } else {
                // No keyword match, return first meaningful sentences
                sb.append("From your notes:\n\n");
                int count = 0;
                for (String s : sentences) {
                    if (s.trim().length() > 20) {
                        sb.append("- ").append(s.trim());
                        if (!s.trim().endsWith(".")) sb.append(".");
                        sb.append("\n");
                        count++;
                        if (count >= 4) break;
                    }
                }
                if (count == 0) {
                    sb.append("Your notes are quite brief. Try pasting more detailed content for better answers.");
                }
            }
            sb.append("\nTip: Upload more detailed notes for deeper analysis.");
        } else {
            sb.append("No notes have been uploaded yet. Please paste your study material in the text area above, then ask your question again for a contextual answer.");
        }

        return sb.toString();
    }

    private static String generateHeuristicQuizJson(String notes) {
        // Dynamically extract concepts and definitions from whatever notes the user uploaded
        List<String[]> extractedPairs = new ArrayList<>();
        if (notes != null && !notes.isBlank()) {
            String[] lines = notes.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.contains(":") && trimmed.length() > 20) {
                    String[] parts = trimmed.split(":", 2);
                    String term = parts[0].replaceAll("^[^a-zA-Z0-9 ]+", "").trim();
                    String def = parts[1].trim();
                    if (!term.isBlank() && term.length() < 50 && def.length() > 15) {
                        extractedPairs.add(new String[]{term, def});
                    }
                } else if (trimmed.matches(".*\\b(is defined as|is|refers to|means)\\b.*") && trimmed.length() > 30) {
                    String[] parts = trimmed.split("\\b(is defined as|is|refers to|means)\\b", 2);
                    String term = parts[0].replaceAll("^[^a-zA-Z0-9 ]+", "").trim();
                    String def = parts[1].trim();
                    if (!term.isBlank() && term.length() < 50 && def.length() > 15) {
                        extractedPairs.add(new String[]{term, def});
                    }
                }
            }
        }

        // If user provided notes with extractable definitions, dynamically construct MCQs from their text!
        if (extractedPairs.size() >= 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"questions\": [");
            int count = Math.min(3, extractedPairs.size());
            for (int i = 0; i < count; i++) {
                String[] current = extractedPairs.get(i);
                String term = current[0];
                String def = current[1];

                // Gather distractor terms from other extracted concepts
                List<String> options = new ArrayList<>();
                options.add(term);
                for (int j = 0; j < extractedPairs.size(); j++) {
                    if (j != i && options.size() < 4) {
                        options.add(extractedPairs.get(j)[0]);
                    }
                }
                while (options.size() < 4) {
                    options.add("System Architecture Concept " + options.size());
                }
                Collections.shuffle(options);
                int correctIndex = options.indexOf(term);

                if (i > 0) sb.append(",");
                String shortDef = def.length() > 120 ? def.substring(0, 120) + "..." : def;
                sb.append("{")
                  .append("\"question\": \"According to your uploaded notes, which concept is defined as: \\\"")
                  .append(escapeJson(shortDef))
                  .append("\\\"?\",")
                  .append("\"options\": [\"")
                  .append(String.join("\", \"", options.stream().map(SmartCampusApp::escapeJson).toList()))
                  .append("\"],")
                  .append("\"correctAnswerIndex\": ").append(correctIndex).append(",")
                  .append("\"explanation\": \"Directly derived from your notes: ").append(escapeJson(term)).append(" refers to ").append(escapeJson(shortDef)).append("\"")
                  .append("}");
            }
            sb.append("]}");
            return sb.toString();
        }

        // Fallback: not enough extractable content — tell the user to add detailed notes
        return "{\"questions\": [{\"question\": \"Please paste more detailed notes to generate a quiz. Include definitions, key concepts, or structured content.\", \"options\": [\"Understood\", \"Will do\", \"Got it\", \"OK\"], \"correctAnswerIndex\": 0, \"explanation\": \"The quiz engine works best when your notes contain clear definitions, terms, or structured content like 'Term: Definition' patterns.\"}]}";
    }

    private static String generateHeuristicCareerJson(String resume, String targetRole, String jobDescription) {
        String lowerResume = resume.toLowerCase();
        String lowerJd = (jobDescription + " " + targetRole).toLowerCase();
        int score = 72;
        int keywordScore = 68;
        int impactScore = 70;
        int formatScore = 88;

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> atsKeywords = new ArrayList<>();

        // Generic skills matching — extract significant words from JD and check resume
        String[] jdTokens = lowerJd.replaceAll("[^a-z0-9/#+. -]", "").split("\\s+");
        Set<String> checkedWords = new java.util.HashSet<>();
        for (String token : jdTokens) {
            if (token.length() > 3 && !List.of("with", "from", "that", "this", "have", "will", "your", "must", "able", "work", "team", "role", "about", "join", "more", "should").contains(token)) {
                if (checkedWords.add(token)) {
                    if (lowerResume.contains(token)) {
                        matched.add(capitalize(token));
                        score += 2;
                        keywordScore += 3;
                    } else {
                        missing.add(capitalize(token));
                        atsKeywords.add(capitalize(token));
                    }
                }
            }
            if (matched.size() + missing.size() >= 20) break;
        }

        // Cap at reasonable limits
        if (matched.size() > 8) matched = matched.subList(0, 8);
        if (missing.size() > 8) missing = missing.subList(0, 8);
        if (atsKeywords.size() > 10) atsKeywords = atsKeywords.subList(0, 10);
        if (score > 95) score = 95;
        if (keywordScore > 98) keywordScore = 98;
        int atsOverall = (int) Math.round((keywordScore * 0.45) + (impactScore * 0.35) + (formatScore * 0.20));

        // Dynamically extract user's real resume bullet points
        List<String> actualBullets = new ArrayList<>();
        for (String line : resume.split("\\r?\\n")) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("\u2022") || trimmed.matches("^\\d+\\..*")) && trimmed.length() > 25) {
                actualBullets.add(trimmed.replaceAll("^[-*\u2022\\d.]+\\s*", ""));
            }
        }

        String bullet1Original = actualBullets.size() > 0 ? actualBullets.get(0) : "No bullet points found in resume.";
        String bullet1Improved = actualBullets.size() > 0
                ? "Spearheaded " + actualBullets.get(0) + ", delivering measurable impact with quantified results and strong action verbs."
                : "Upload your resume to get personalized bullet point rewrites.";

        String bullet2Original = actualBullets.size() > 1 ? actualBullets.get(1) : "";
        String bullet2Improved = actualBullets.size() > 1
                ? "Engineered and optimized " + actualBullets.get(1) + ", incorporating industry best practices to achieve concrete performance gains."
                : "";

        // Build study guide dynamically based on missing skills
        List<String> coreTopics = new ArrayList<>();
        List<String> designTopics = new ArrayList<>();
        for (int i = 0; i < missing.size() && i < 3; i++) {
            coreTopics.add("Study fundamentals of " + missing.get(i) + " as required by the target role.");
        }
        if (coreTopics.isEmpty()) coreTopics.add("Review the core competencies listed in the job description.");
        for (int i = 3; i < missing.size() && designTopics.size() < 3; i++) {
            designTopics.add("Learn practical applications of " + missing.get(i) + " for this role.");
        }
        if (designTopics.isEmpty()) designTopics.add("Review system architecture concepts relevant to your target role.");

        // Build interview questions from JD keywords
        List<String> interviewQs = new ArrayList<>();
        int qCount = 0;
        for (String skill : matched) {
            if (qCount >= 3) break;
            interviewQs.add("{\"question\": \"Explain your experience with " + escapeJson(skill) + " and how you applied it in a real project.\", \"tip\": \"Focus on measurable outcomes, trade-offs, and technical depth.\"}");
            qCount++;
        }
        for (String skill : missing) {
            if (qCount >= 5) break;
            interviewQs.add("{\"question\": \"How would you approach learning and applying " + escapeJson(skill) + " for this role?\", \"tip\": \"Show awareness of the technology and a concrete learning plan.\"}");
            qCount++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"score\": ").append(score).append(",")
          .append("\"atsScore\": ").append(atsOverall).append(",")
          .append("\"breakdown\": {\"keywords\": ").append(keywordScore).append(", \"impact\": ").append(impactScore).append(", \"formatting\": ").append(formatScore).append("},")
          .append("\"summary\": \"Profile analyzed for ").append(escapeJson(targetRole)).append(". ").append(matched.size()).append(" matching keywords found, ").append(missing.size()).append(" gaps identified.\",")
          .append("\"matchedSkills\": [\"").append(String.join("\", \"", matched.stream().map(SmartCampusApp::escapeJson).toList())).append("\"],")
          .append("\"missingSkills\": [\"").append(String.join("\", \"", missing.stream().map(SmartCampusApp::escapeJson).toList())).append("\"],")
          .append("\"atsKeywords\": [\"").append(String.join("\", \"", atsKeywords.stream().map(SmartCampusApp::escapeJson).toList())).append("\"],")
          .append("\"bulletRewrites\": [");

        sb.append("{\"original\": \"").append(escapeJson(bullet1Original)).append("\", \"improved\": \"").append(escapeJson(bullet1Improved)).append("\", \"rationale\": \"Adds strong action verb and quantifiable impact.\"}");
        if (!bullet2Original.isBlank()) {
            sb.append(",{\"original\": \"").append(escapeJson(bullet2Original)).append("\", \"improved\": \"").append(escapeJson(bullet2Improved)).append("\", \"rationale\": \"Incorporates industry terminology and measurable outcomes.\"}");
        }
        sb.append("],");

        sb.append("\"studyGuide\": {")
          .append("\"coreTopics\": [\"").append(String.join("\", \"", coreTopics.stream().map(SmartCampusApp::escapeJson).toList())).append("\"],")
          .append("\"systemDesign\": [\"").append(String.join("\", \"", designTopics.stream().map(SmartCampusApp::escapeJson).toList())).append("\"],")
          .append("\"interviewQuestions\": [").append(String.join(",", interviewQs)).append("]")
          .append("},");

        sb.append("\"roadmap\": [")
          .append("\"Add the missing ATS keywords to your project descriptions and skills section.\",")
          .append("\"Prepare structured STAR-format answers for the targeted interview questions.\",")
          .append("\"Build a small portfolio project showcasing the missing skills for this role.\"")
          .append("]")
          .append("}");

        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String getHeuristicInterviewQuestion(String targetRole) {
        String role = targetRole.toLowerCase();
        if (role.contains("backend") || role.contains("server") || role.contains("api")) {
            return "Describe how you would design a scalable backend service that handles high concurrency. What trade-offs would you consider?";
        } else if (role.contains("frontend") || role.contains("ui") || role.contains("react") || role.contains("web")) {
            return "How do you approach building a responsive, accessible web application? Walk me through your architecture decisions.";
        } else if (role.contains("data") || role.contains("analyst") || role.contains("machine learning")) {
            return "Describe a data pipeline or analysis project you worked on. What tools did you use and how did you validate your results?";
        } else if (role.contains("full stack")) {
            return "Walk me through how you would architect a full-stack application from database design to the frontend. What technologies would you choose and why?";
        } else if (role.contains("devops") || role.contains("cloud") || role.contains("sre")) {
            return "How would you set up a CI/CD pipeline for a production application? What monitoring and alerting would you implement?";
        } else {
            return "Tell me about a challenging technical project you worked on. What was your role, what decisions did you make, and what was the outcome?";
        }
    }

    private static String evaluateHeuristicInterview(String answer) {
        if (answer.trim().length() < 30) {
            return "Your answer is quite brief. Try structuring your response with: 1) The core concept or approach, 2) Technical trade-offs you considered, and 3) A real-world example or outcome.";
        }
        return "Good answer structure. You demonstrated understanding of the topic with practical reasoning. To strengthen further, quantify outcomes and mention specific technologies or patterns you applied.";
    }

    // =========================================================================
    // Helper Utilities
    // =========================================================================
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return unescapeJson(m.group(1));
        }
        return "";
    }

    private static String cleanJsonOutput(String raw) {
        String clean = raw.trim();
        int startFence = clean.indexOf("```json");
        if (startFence != -1) {
            int endFence = clean.lastIndexOf("```");
            if (endFence > startFence + 7) {
                return clean.substring(startFence + 7, endFence).trim();
            }
        }
        startFence = clean.indexOf("```");
        if (startFence != -1) {
            int endFence = clean.lastIndexOf("```");
            if (endFence > startFence + 3) {
                return clean.substring(startFence + 3, endFence).trim();
            }
        }
        int firstBrace = clean.indexOf("{");
        int lastBrace = clean.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return clean.substring(firstBrace, lastBrace + 1).trim();
        }
        return clean;
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static String unescapeJson(String escaped) {
        if (escaped == null) return "";
        return escaped.replace("\\\"", "\"")
                      .replace("\\n", "\n")
                      .replace("\\r", "\r")
                      .replace("\\t", "\t")
                      .replace("\\\\", "\\");
    }
}
