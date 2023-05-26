package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ping", new PingHandler());
        server.createContext("/api/game/start", new StartGameHandler());
        server.createContext("/api/game/fire", new FireHandler());
        int THREAD_POOL_SIZE = 1;
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        server.start();
        if (args.length >= 2) {
            String adversaryUrl = args[1];
            JSONObject requestJson = RequestHelper.createRequestJson(port);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = RequestHelper.createHttpRequest(adversaryUrl, requestJson);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ResponseHelper.handleGameStartResponse(response, port);
        }
    }
}

class RequestHelper {
    public static JSONObject createRequestJson(int port) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("id", "1");
        requestJson.put("url", "http://localhost:" + port);
        requestJson.put("message", "hello");
        return requestJson;
    }

    public static HttpRequest createHttpRequest(String adversaryUrl, JSONObject requestJson) {
        return HttpRequest.newBuilder()
            .uri(URI.create(adversaryUrl + "/api/game/start"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
            .build();
    }
}

class ResponseHelper {
    public static void handleGameStartResponse(HttpResponse<String> response, int port) {
        int statusCode = response.statusCode();
        if (statusCode == 202) {
            JSONObject responseJson = new JSONObject(response.body());
            String id = responseJson.getString("id");
            String url = responseJson.getString("url");
            String message = responseJson.getString("message");
            printGameStartResponse(id, url, message, port);
        } else {
            System.out.println("Unexpected response status: " + statusCode);
        }
    }

    private static void printGameStartResponse(String id, String url, String message, int port) {
        if (!url.equals("http://localhost:" + port)) {
            System.out.println("Unexpected server URL in response: " + url);
        }
        System.out.println("ID: " + id);
        System.out.println("Server URL: " + url);
        System.out.println("Message: " + message);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.close();
    }
}

class PingHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.sendResponse(exchange, 200, "OK");
    }
}

class StartGameHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            ResponseHelper.sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            JSONObject requestBody = new JSONObject(new JSONTokener(exchange.getRequestBody()));
            String id = requestBody.getString("id");
            String message = requestBody.getString("message");
            JSONObject responseJson = createGameStartResponseJson(exchange, id, message);
            ResponseHelper.sendResponse(exchange, 202, responseJson.toString());
        } catch (Exception e) {
            ResponseHelper.sendResponse(exchange, 400, "Bad Request");
        }
    }

    private JSONObject createGameStartResponseJson(HttpExchange exchange, String id, String message) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("id", id);
        responseJson.put("url", "http://localhost:" + exchange.getLocalAddress().getPort());
        responseJson.put("message", message);
        return responseJson;
    }
}

class FireHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            ResponseHelper.sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        String cell = exchange.getRequestURI().getQuery();
        if (cell == null || cell.isEmpty()) {
            ResponseHelper.sendResponse(exchange, 400, "Bad Request");
            return;
        }
        handleFireRequest(exchange, cell);
    }

    private void handleFireRequest(HttpExchange exchange, String cell) throws IOException {
        JSONObject responseJson = createFireResponseJson();
        ResponseHelper.sendResponse(exchange, 200, responseJson.toString());
    }

    private JSONObject createFireResponseJson() {
        JSONObject responseJson = new JSONObject();
        responseJson.put("consequence", "sunk");
        responseJson.put("shipLeft", true);
        return responseJson;
    }
}
