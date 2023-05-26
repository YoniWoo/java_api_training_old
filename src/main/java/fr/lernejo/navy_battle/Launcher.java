package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length >= 1) {
            int port = Integer.parseInt(args[0]);

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(1));

            server.createContext("/ping", new PingHandler());
            server.createContext("/api/game/start", new StartGameHandler());

            server.start();

            System.out.println("Server started on port " + port);

            if (args.length >= 2) {
                String adversaryUrl = args[1];

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(adversaryUrl + "/api/game/start"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"id\":\"1\", \"url\":\"http://localhost:" + port + "\", \"message\":\"hello\"}"))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                System.out.println("Status Code: " + statusCode);

                HttpHeaders headers = response.headers();
                System.out.println("Response Headers: " + headers);

                String responseBody = response.body();
                System.out.println("Response Body: " + responseBody);
            }
        } else {
            System.out.println("Please provide a port number as an argument.");
        }
    }

    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "OK";
            createResponse(exchange, 200, response);
        }
    }

    static class StartGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method Not Allowed";
                createResponse(exchange, 405, response);
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            try {
                JSONObject requestJson = new JSONObject(requestBody);
                String id = requestJson.getString("id");
                String url = requestJson.getString("url");
                String message = requestJson.getString("message");

                JSONObject responseJson = new JSONObject();
                responseJson.put("id", id);
                responseJson.put("url", url);
                responseJson.put("message", message);

                createResponse(exchange, 202, responseJson.toString());
            } catch (Exception e) {
                String response = "Bad Request";
                createResponse(exchange, 400, response);
            }
        }
    }

    private static void createResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.sendResponseHeaders(statusCode, responseBody.length());
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBody.getBytes());
        outputStream.close();
    }
}
