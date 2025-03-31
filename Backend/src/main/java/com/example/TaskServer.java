package com.example;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class TaskServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/plan", new PlanHandler());
        server.start();
        System.out.println("Server is running on http://localhost:8080");
    }

    static class PlanHandler implements HttpHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:5173");
            

                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equalsIgnoreCase("POST")) {
                BufferedReader buffer = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String tasks = buffer.lines().collect(Collectors.joining("\n"));

                if (tasks == null || tasks.trim().isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\": \"No tasks provided.\"}");
                    return;
                }

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted.");
                }

                String json = planTheDay(tasks);
                sendJsonResponse(exchange, 200, json);

            } else {
                sendJsonResponse(exchange, 405, "{\"error\": \"Only POST requests allowed.\"}");
            }
        }

        private String planTheDay(String taskString) throws IOException {
            List<String> tasks = Arrays.stream(taskString.split(","))
                    .map(String::trim)
                    .filter(task -> !task.isEmpty())
                    .toList();

            List<Map<String, String>> schedule = new ArrayList<>();
            int time = 11;

            for (String task : tasks) {
                Map<String, String> entry = new HashMap<>();
                entry.put("time", (time < 12 ? time + "AM" : (time - 12) + "PM"));
                entry.put("task", task);
                schedule.add(entry);
                time++;
            }

            String explanation = "Tasks are scheduled sequentially, starting at 11 AM.";

            Map<String, Object> response = new HashMap<>();
            response.put("tasks", schedule);
            response.put("explanation", explanation);

            return objectMapper.writeValueAsString(response);
        }

        private void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:3000");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream out = exchange.getResponseBody();
            out.write(response.getBytes());
            out.close();
        }
    }
}
