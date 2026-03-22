import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebServer {
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final FileAccessManager fileAccessManager;

    public WebServer(AuthService authService, SessionManager sessionManager, FileAccessManager fileAccessManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
        this.fileAccessManager = fileAccessManager;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);

        server.createContext("/", exchange -> {
            System.out.println("Request path: " + exchange.getRequestURI().getPath());
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                sendFile(exchange, "src/main/resources/public/index.html", "text/html");
            } else if (path.equals("/style.css")) {
                sendFile(exchange, "src/main/resources/public/style.css", "text/css");
            } else if (path.equals("/script.js")) {
                sendFile(exchange, "src/main/resources/public/script.js", "application/javascript");
            } else if (path.equals("/state")) {
                handleState(exchange);
            } else if (path.equals("/login")) {
                handleLogin(exchange);
            } else if (path.equals("/file-action")) {
                handleFileAction(exchange);
            } else if (path.equals("/logout")) {
                handleLogout(exchange);
            } else if (path.equals("/user-status")) {
                handleUserStatus(exchange);
            } else if (path.equals("/request-write")) {
                handleRequestWrite(exchange);
            } else if (path.equals("/release-write")) {
                handleReleaseWrite(exchange);
            }else if (path.equals("/request-read")) {
                handleRequestRead(exchange);
            } else if (path.equals("/release-read")) {
                handleReleaseRead(exchange);
            }else {
                sendResponse(exchange, "Not found", "text/plain", 404);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Web server started on port 5000");
    }

    private void handleState(HttpExchange exchange) throws IOException {
        String json = "{"
                + "\"activeUsers\":" + sessionManager.getActiveUsersJson() + ","
                + "\"waitingUsers\":" + sessionManager.getWaitingUsersJson() + ","
                + "\"fileStatus\":\"" + escapeJson(fileAccessManager.getFileStatus()) + "\","
                + "\"readerStatus\":\"" + escapeJson(fileAccessManager.getReaderStatus()) + "\","
                + "\"writerStatus\":\"" + escapeJson(fileAccessManager.getWriterStatus()) + "\","
                + "\"writeReservationStatus\":\"" + escapeJson(fileAccessManager.getWriteReservationStatus()) + "\","
                + "\"fileContent\":\"" + escapeJson(fileAccessManager.getFileContent()) + "\","
                + "\"files\":[{"
                + "\"name\":\"ProductSpecification.txt\","
                + "\"status\":\"" + escapeJson(fileAccessManager.getFileStatus()) + "\","
                + "\"readerStatus\":\"" + escapeJson(fileAccessManager.getReaderStatus()) + "\","
                + "\"writerStatus\":\"" + escapeJson(fileAccessManager.getWriterStatus()) + "\","
                + "\"writeReservationStatus\":\"" + escapeJson(fileAccessManager.getWriteReservationStatus()) + "\""
                + "}]"
                + "}";

        sendResponse(exchange, json, "application/json");
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        int id = extractInt(body, "id");
        String username = extractString(body, "username");

        User user = authService.authenticate(id, username);

        if (user == null) {
            sendResponse(exchange, "Authentication failed.", "text/plain", 403);
            return;
        }

        boolean loggedIn = sessionManager.requestLogin(user);

        if (loggedIn) {
            sendResponse(exchange, "LOGIN_SUCCESS", "text/plain");
        } else {
            int position = sessionManager.getWaitingPosition(user.getId());
            sendResponse(exchange, "WAITING:" + position, "text/plain");
        }
    }

        private void handleFileAction(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            int id = extractInt(body, "id");
            String username = extractString(body, "username");
            String fileName = extractString(body, "fileName");
            String action = extractString(body, "action");
            String content = extractOptionalString(body, "content");

            User user = authService.authenticate(id, username);

            if (user == null) {
                sendResponse(exchange, "Authentication failed.", "text/plain", 403);
                return;
            }

            if (!sessionManager.isUserLoggedIn(user.getId())) {
                sendResponse(exchange, "User is not logged in.", "text/plain", 403);
                return;
            }


            if ("write".equalsIgnoreCase(action)) {
                if (!fileAccessManager.hasWriteReservation(user.getId())) {
                    sendResponse(exchange, "Write access not reserved for this user.", "text/plain", 403);
                    return;
                }

                new FileOperationTask(user, fileAccessManager, action, content).start();
                sendResponse(exchange, "Write request started for " + fileName, "text/plain");
                return;
            }

            sendResponse(exchange, "Invalid file action.", "text/plain", 400);
        }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        int id = extractInt(body, "id");
        String username = extractString(body, "username");

        User user = authService.authenticate(id, username);

        if (user == null) {
            sendResponse(exchange, "Authentication failed.", "text/plain", 403);
            return;
        }

        if (!sessionManager.isUserLoggedIn(user.getId())) {
            sendResponse(exchange, "User is not logged in.", "text/plain", 403);
            return;
        }

        sessionManager.logout(user);
        sendResponse(exchange, "Logged out: " + username, "text/plain");
    }

    private void sendFile(HttpExchange exchange, String fileName, String contentType) throws IOException {
        Path path = Path.of(fileName);
        System.out.println("Serving file: " + path.toAbsolutePath());

        if (!Files.exists(path)) {
            String msg = "File not found: " + path.toAbsolutePath();
            System.out.println(msg);
            sendResponse(exchange, msg, "text/plain", 404);
            return;
        }

        byte[] bytes = Files.readAllBytes(path);
        System.out.println("File size: " + bytes.length + " bytes");

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        sendResponse(exchange, response, contentType, 200);
    }

    private void sendResponse(HttpExchange exchange, String response, String contentType, int statusCode)
            throws IOException {
        byte[] bytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private int extractInt(String json, String key) {
        String value = extractString(json, key);
        return Integer.parseInt(value);
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) {
            return "";
        }

        int start = keyIndex + search.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        boolean quoted = json.charAt(start - 1) == '"';

        if (quoted) {
            end = json.indexOf("\"", start);
        } else {
            while (end < json.length() && Character.isDigit(json.charAt(end))) {
                end++;
            }
        }

        return json.substring(start, end);
    }
    private String extractOptionalString(String json, String key) {
        String search = "\"" + key + "\":";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) {
            return "";
        }

        int start = keyIndex + search.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && json.charAt(end - 1) != '\\') {
                break;
            }
            end++;
        }

        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private void handleUserStatus(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        int id = extractInt(body, "id");
        String username = extractString(body, "username");

        User user = authService.authenticate(id, username);

        if (user == null) {
            sendResponse(exchange, "INVALID", "text/plain", 403);
            return;
        }

        if (sessionManager.isUserLoggedIn(user.getId())) {
            sendResponse(exchange, "ACTIVE", "text/plain");
            return;
        }

        int position = sessionManager.getWaitingPosition(user.getId());
        if (position != -1) {
            sendResponse(exchange, "WAITING:" + position, "text/plain");
            return;
        }

        sendResponse(exchange, "NONE", "text/plain");
    }

    private void handleRequestWrite(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        int id = extractInt(body, "id");
        String username = extractString(body, "username");

        User user = authService.authenticate(id, username);

        if (user == null) {
            sendResponse(exchange, "Authentication failed.", "text/plain", 403);
            return;
        }

        if (!sessionManager.isUserLoggedIn(user.getId())) {
            sendResponse(exchange, "User is not logged in.", "text/plain", 403);
            return;
        }

        String result = fileAccessManager.requestWriteReservation(user);
        sendResponse(exchange, result, "text/plain");
    }

    private void handleReleaseWrite(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        int id = extractInt(body, "id");
        String username = extractString(body, "username");

        User user = authService.authenticate(id, username);

        if (user == null) {
            sendResponse(exchange, "Authentication failed.", "text/plain", 403);
            return;
        }

        fileAccessManager.releaseWriteReservation(user);
        sendResponse(exchange, "WRITE_RELEASED", "text/plain");
    }
        private void handleRequestRead(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            int id = extractInt(body, "id");
            String username = extractString(body, "username");

            User user = authService.authenticate(id, username);

            if (user == null) {
                sendResponse(exchange, "Authentication failed.", "text/plain", 403);
                return;
            }

            if (!sessionManager.isUserLoggedIn(user.getId())) {
                sendResponse(exchange, "User is not logged in.", "text/plain", 403);
                return;
            }

            String result = fileAccessManager.requestReadAccess(user);
            sendResponse(exchange, result, "text/plain");
        }

        private void handleReleaseRead(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, "Method Not Allowed", "text/plain", 405);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

            int id = extractInt(body, "id");
            String username = extractString(body, "username");

            User user = authService.authenticate(id, username);

            if (user == null) {
                sendResponse(exchange, "Authentication failed.", "text/plain", 403);
                return;
            }

            fileAccessManager.releaseReadAccess(user);
            sendResponse(exchange, "READ_RELEASED", "text/plain");
        }
}