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
                String answer = callGeminiApi(apiKey, prompt);
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
                String raw = callGeminiApi(apiKey, prompt);
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
                String raw = callGeminiApi(apiKey, prompt);
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
            if (apiKey.isBlank()) apiKey = globalGeminiKey;

            if (apiKey.isBlank()) {
                sendJsonResponse(exchange, 200, "{\"error\":\"Please configure your Google Gemini API key to practice mock interviews with AI.\"}");
                return;
            }

            if ("start".equalsIgnoreCase(action)) {
                try {
                    String prompt = "Generate a single challenging technical interview question for a candidate applying for: " + targetRole + " with this resume:\n" + resume;
                    String questionText = callGeminiApi(apiKey, prompt);
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
                    String raw = callGeminiApi(apiKey, prompt);
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
        // Supports current production Gemini models with automatic candidate fallback
        String[] candidateModels = {
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-pro"
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
