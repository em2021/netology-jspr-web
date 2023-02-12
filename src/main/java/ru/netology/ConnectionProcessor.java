package ru.netology;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ConnectionProcessor implements Runnable {

    private final Path resourcesRoot;
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedOutputStream out;
    private final StringBuilder sb = new StringBuilder();

    public ConnectionProcessor(Path resourcesRoot, Socket socket) throws IOException {
        this.resourcesRoot = resourcesRoot;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try (socket; in; out) {
            String path = validatePath(extractPath(extractRequestLine()));
            if (path == null) {
                responseWriter(null, null, null);
            } else {
                final var filePath = Path.of(resourcesRoot.toString(), path);
                final var mimeType = Files.probeContentType(filePath);
                final var content = contentHandler(filePath);
                responseWriter(mimeType, path, content);
            }
        } catch (
                IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private String[] extractRequestLine() throws IOException {
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            return null;
        }
        return parts;
    }

    private String extractPath(String[] requestLineParts) {
        return requestLineParts[1];
    }

    private String validatePath(String path) throws IOException {
        if (path.equals("/") || !Files.exists(Path.of(resourcesRoot.toString(), path))) {
            return null;
        }
        return path;
    }

    private byte[] contentHandler(Path filePath) throws IOException {
        if (filePath.endsWith("classic.html")) {
            final var template = Files.readString(filePath);
            return template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
        }
        return Files.readAllBytes(filePath);
    }

    private void responseWriter(String mimeType,
                                String path,
                                byte[] content) throws IOException {
        if (path == null) {
            sb.append("HTTP/1.1 404 Not Found\r\n")
                    .append("Content-Length: 0\r\n")
                    .append("Connection: close\r\n")
                    .append("\r\n");
        } else {
            int contentLength = 0;
            if (content != null) {
                contentLength = content.length;
            }
            sb.append("HTTP/1.1 200 OK\r\n")
                    .append("Content-Type: ").append(mimeType).append("\r\n")
                    .append("Content-Length: ").append(contentLength).append("\r\n")
                    .append("\r\n");
        }
        out.write((
                sb.toString()
        ).getBytes());
        if (content != null) {
            out.write(content);
        }
        out.flush();
    }
}