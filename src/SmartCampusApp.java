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
 * - Live Google Gemini AI Engine
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
        server.createContext("/api/config/models", new ConfigModelsHandler());

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
                    + "\"hasApiKey\":" + (!globalGeminiKey.isBlank())
                    + "}";
            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // Academic Study Q&A Handler (100% Live Gemini AI)
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
            String model = extractJsonField(body, "model");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"answer\":\"Please provide a valid Google Gemini API key to ask questions. Click 'Gemini API Key' above to enter your key.\"}");
                return;
            }

            try {
                String prompt = "You are an expert technical tutor and study assistant. "
                        + "Answer the user's question clearly, thoroughly, and accurately based on the study notes below.\n"
                        + "Use clear formatting, bold terms, and structured bullet points.\n\n"
                        + "STUDY NOTES:\n" + (notes.isBlank() ? "No notes provided." : notes) + "\n\n"
                        + "QUESTION:\n" + question;
                String answer = callGeminiApi(apiKey, prompt, model);
                sendJsonResponse(exchange, 200, "{\"answer\":\"" + escapeJson(answer) + "\"}");
            } catch (Exception e) {
                System.err.println("Gemini API call failed: " + e.getMessage());
                sendJsonResponse(exchange, 200, "{\"answer\":\"Gemini API Error: " + escapeJson(e.getMessage()) + ". Please verify your API key.\"}");
            }
        }
    }

    // =========================================================================
    // Revision Quiz & Flashcard Generator Handler (100% Live Gemini AI)
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
            String model = extractJsonField(body, "model");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"error\":\"Please configure your Google Gemini API key to generate quizzes with AI.\"}");
                return;
            }

            try {
                String prompt = "Create 3 high-quality multiple-choice questions (MCQs) for revision based strictly on the following study notes.\n"
                        + "Respond strictly with valid JSON without markdown code blocks, using this exact structure:\n"
                        + "{\"questions\":[{\"question\":\"question text\",\"options\":[\"option A\",\"option B\",\"option C\",\"option D\"],\"correctAnswerIndex\":0,\"explanation\":\"why this answer is correct\"}]}\n\n"
                        + "STUDY NOTES:\n" + (notes.isBlank() ? "Key concepts and technical principles" : notes);
                String raw = callGeminiApi(apiKey, prompt, model);
                String responseJson = cleanJsonOutput(raw);
                sendJsonResponse(exchange, 200, responseJson);
            } catch (Exception e) {
                System.err.println("Gemini quiz generation failed: " + e.getMessage());
                sendJsonResponse(exchange, 200, "{\"error\":\"Gemini API Error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // Career & Placement Gap Analyzer Handler (100% Live Gemini AI)
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
            String model = extractJsonField(body, "model");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"error\":\"Please configure your Google Gemini API key to analyze your resume with AI.\"}");
                return;
            }

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
                String raw = callGeminiApi(apiKey, prompt, model);
                String responseJson = cleanJsonOutput(raw);
                sendJsonResponse(exchange, 200, responseJson);
            } catch (Exception e) {
                System.err.println("Gemini career analysis failed: " + e.getMessage());
                sendJsonResponse(exchange, 200, "{\"error\":\"Gemini API Error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // AI Mock Interview Handler (100% Live Gemini AI)
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
            String model = extractJsonField(body, "model");
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"error\":\"Please configure your Google Gemini API key to practice mock interviews with AI.\"}");
                return;
            }

            if ("start".equalsIgnoreCase(action)) {
                try {
                    String prompt = "Generate a single challenging technical interview question for a candidate applying for: " + targetRole + " with this resume:\n" + resume;
                    String questionText = callGeminiApi(apiKey, prompt, model);
                    sendJsonResponse(exchange, 200, "{\"question\":\"" + escapeJson(questionText.trim()) + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 200, "{\"error\":\"Gemini API Error: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                // evaluate
                try {
                    String prompt = "You are a lead technical interviewer for " + targetRole + ".\n"
                            + "Question: " + question + "\n"
                            + "Candidate Answer: " + answer + "\n\n"
                            + "Evaluate the answer. Respond strictly with JSON: {\"score\":\"8.5\",\"feedback\":\"2-3 sentences of feedback and technical improvements\"}";
                    String raw = callGeminiApi(apiKey, prompt, model);
                    sendJsonResponse(exchange, 200, cleanJsonOutput(raw));
                } catch (Exception e) {
                    sendJsonResponse(exchange, 200, "{\"error\":\"Gemini API Error: " + escapeJson(e.getMessage()) + "\"}");
                }
            }
        }
    }

    // =========================================================================
    // Save API Key Configuration Handler
    // =========================================================================
    // Cache discovered models per API key to prevent redundant network calls
    private static final Map<String, List<String>> MODEL_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static class ConfigKeyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                String key = extractJsonField(body, "apiKey");
                globalGeminiKey = key;
                if (key.isBlank()) {
                    MODEL_CACHE.clear();
                }
                sendJsonResponse(exchange, 200, "{\"status\":\"SAVED\",\"hasKey\":" + (!globalGeminiKey.isBlank()) + "}");
            } else {
                sendJsonResponse(exchange, 200, "{\"hasKey\":" + (!globalGeminiKey.isBlank()) + "}");
            }
        }
    }

    // =========================================================================
    // List Models Endpoint (Validates Key & Discovers Available Models)
    // =========================================================================
    static class ConfigModelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String apiKey = "";
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("key=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("key=")) {
                        apiKey = param.substring(4);
                        break;
                    }
                }
            }
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"status\":\"NO_KEY\",\"models\":[]}");
                return;
            }

            try {
                List<String> allModels = getOrFetchAvailableModels(apiKey);
                List<String> textOnly = new ArrayList<>();
                String[] standardTextModels = {
                    "gemini-2.5-flash",
                    "gemini-2.0-flash",
                    "gemini-1.5-flash",
                    "gemini-2.5-pro"
                };
                for (String std : standardTextModels) {
                    if (allModels.contains(std)) {
                        textOnly.add(std);
                    }
                }
                // If standard names not present, add any clean flash/pro text model
                if (textOnly.isEmpty()) {
                    for (String m : allModels) {
                        if (isTextGenerationModel(m) && !textOnly.contains(m)) {
                            textOnly.add(m);
                        }
                    }
                }

                StringBuilder sb = new StringBuilder("{\"status\":\"OK\",\"models\":[");
                for (int i = 0; i < textOnly.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(escapeJson(textOnly.get(i))).append("\"");
                }
                sb.append("]}");
                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                System.err.println("Failed to fetch models: " + e.getMessage());
                sendJsonResponse(exchange, 200, "{\"status\":\"ERROR\",\"error\":\"" + escapeJson(e.getMessage()) + "\",\"models\":[]}");
            }
        }
    }

    // =========================================================================
    // Dynamic Model Discovery & Verification (Strictly Text Generation Models)
    // =========================================================================
    private static boolean isTextGenerationModel(String modelName) {
        String lower = modelName.toLowerCase();
        if (!lower.startsWith("gemini") && !lower.startsWith("gemma")) {
            return false;
        }
        // Strictly filter out non-text modalities
        if (lower.contains("tts") ||
            lower.contains("audio") ||
            lower.contains("image") ||
            lower.contains("vision") ||
            lower.contains("embed") ||
            lower.contains("transcribe") ||
            lower.contains("robotics") ||
            lower.contains("clip") ||
            lower.contains("customtools") ||
            lower.contains("banana") ||
            lower.contains("omni") ||
            lower.contains("research") ||
            lower.contains("preview") ||
            lower.contains("computer-use")) {
            return false;
        }
        return true;
    }

    private static List<String> getOrFetchAvailableModels(String apiKey) throws Exception {
        String cleanKey = apiKey.trim();
        if (MODEL_CACHE.containsKey(cleanKey) && !MODEL_CACHE.get(cleanKey).isEmpty()) {
            return MODEL_CACHE.get(cleanKey);
        }

        List<String> discovered = new ArrayList<>();
        String lastError = "";

        // Query Google's ModelService.ListModels across v1beta and v1
        for (String apiVer : new String[]{"v1beta", "v1"}) {
            try {
                String listUrl = "https://generativelanguage.googleapis.com/" + apiVer + "/models?key=" + cleanKey;
                HttpRequest listReq = HttpRequest.newBuilder()
                        .uri(URI.create(listUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();

                HttpResponse<String> listRes = HTTP_CLIENT.send(listReq, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Gemini ListModels " + apiVer + "] HTTP " + listRes.statusCode());

                if (listRes.statusCode() == 200) {
                    Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"models/([^\"]+)\"");
                    Matcher m = p.matcher(listRes.body());
                    while (m.find()) {
                        String mName = m.group(1);
                        if (isTextGenerationModel(mName) && !discovered.contains(mName)) {
                            discovered.add(mName);
                        }
                    }
                    if (!discovered.isEmpty()) {
                        break;
                    }
                } else {
                    String msg = extractJsonField(listRes.body(), "message");
                    if (!msg.isBlank()) {
                        lastError = msg;
                    } else {
                        lastError = "HTTP " + listRes.statusCode();
                    }
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
            }
        }

        if (discovered.isEmpty()) {
            if (!lastError.isBlank()) {
                throw new RuntimeException(lastError);
            }
            // Standard text models fallback
            discovered = Arrays.asList("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-pro");
        }

        System.out.println("[Gemini] Active text models for key: " + discovered);
        MODEL_CACHE.put(cleanKey, discovered);
        return discovered;
    }

    // =========================================================================
    // Google Gemini REST API Client (Pure Text Generation Models Only)
    // =========================================================================
    private static String callGeminiApi(String apiKey, String prompt, String preferredModel) throws Exception {
        List<String> available = getOrFetchAvailableModels(apiKey);

        // Build candidate list ordered by priority - strictly text models
        List<String> candidateModels = new ArrayList<>();
        if (preferredModel != null && !preferredModel.isBlank()) {
            String p = preferredModel.trim();
            for (String av : available) {
                if (av.equalsIgnoreCase(p)) {
                    candidateModels.add(av);
                    break;
                }
            }
        }

        // Standard text models in priority order
        String[] textPriorities = {
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-flash-latest",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-pro-latest"
        };
        for (String prio : textPriorities) {
            if (available.contains(prio) && !candidateModels.contains(prio)) {
                candidateModels.add(prio);
            }
        }

        // Add any remaining clean text models
        for (String av : available) {
            if (isTextGenerationModel(av) && !candidateModels.contains(av)) {
                candidateModels.add(av);
            }
        }

        System.out.println("[Gemini] Pure text execution order: " + candidateModels);
        Exception lastException = null;

        String payload = "{"
                + "\"contents\": [{"
                + "  \"parts\": [{\"text\": \"" + escapeJson(prompt) + "\"}]"
                + "}]"
                + "}";

        for (String model : candidateModels) {
            for (String apiVer : new String[]{"v1beta", "v1"}) {
                try {
                    String endpoint = "https://generativelanguage.googleapis.com/" + apiVer + "/models/" + model + ":generateContent?key=" + apiKey.trim();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(20))
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("[Gemini try] " + apiVer + " / " + model + " -> HTTP " + response.statusCode());

                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        Pattern textPattern = Pattern.compile("\"text\":\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
                        Matcher matcher = textPattern.matcher(responseBody);
                        if (matcher.find()) {
                            return unescapeJson(matcher.group(1));
                        }
                        return "Response received from Gemini.";
                    } else {
                        String errMsg = extractJsonField(response.body(), "message");
                        if (errMsg.isBlank()) errMsg = response.body();
                        System.out.println("[Gemini candidate fail] " + model + " (" + apiVer + "): " + errMsg);
                        lastException = new RuntimeException(model + ": " + errMsg);
                        continue; // try next apiVer / next model
                    }
                } catch (Exception ex) {
                    lastException = ex;
                    System.out.println("[Gemini exception] " + model + ": " + ex.getMessage());
                    continue; // try next candidate model
                }
            }
        }

        if (lastException != null) throw lastException;
        throw new RuntimeException("All available Gemini text models failed for this API key.");
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
