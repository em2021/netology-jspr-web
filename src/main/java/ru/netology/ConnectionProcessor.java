package ru.netology;

import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class ConnectionProcessor implements Runnable {

    private final Path resourcesRoot;
    private final Socket socket;
    private final Server server;
    private final BufferedReader in;
    private final BufferedOutputStream out;
    private final StringBuilder sb = new StringBuilder();
    private Handler handler = null;

    public ConnectionProcessor(Server server, Path resourcesRoot, Socket socket) throws IOException {
        this.resourcesRoot = resourcesRoot;
        this.socket = socket;
        this.server = server;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try (socket; in; out) {
            String method;
            String path;
            String params;
            String headers;
            int contentLength;
            String body = null;
            String[] requestLine = extractRequestLine();
            method = validateMethod(extractMethod(requestLine));
            path = validatePath(extractPath(requestLine));
            params = extractParams(requestLine);
            headers = extract();
            contentLength = extractContentLength(headers);
            if (contentLength > 0) {
                body = extractBody(contentLength);
            }
            if (method != null && path != null) {
                Request request = new Request(method, path, params, headers, body);
                this.handler = server.getHandler(method, path);
                final var filePath = Path.of(resourcesRoot.toString(), path);
                final var mimeType = Files.probeContentType(filePath);
                final var content = contentHandler(filePath);
                responseWriter(out, mimeType, path, content, handler);
                if (handler != null) {
                    handler.handle(request, out);
                }
            } else if (method.equals("POST") && path == null) {
                path = "null";
                Request request = new Request(method, path, params, headers, body);
                final var content = request.getPostParams().toString().getBytes();
                responseWriter(out, null, path, content, handler);
            } else {
                responseWriter(out, null, null, null, handler);
            }
            out.flush();
        } catch (
                IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private String extractMethod(String[] requestLineParts) {
        return requestLineParts[0];
    }

    private String extractPath(String[] requestLineParts) {
        String path = requestLineParts[1];
        if (path.contains("?")) {
            return path.substring(0, path.indexOf("?"));
        }
        return path;
    }

    private String extractParams(String[] requestLineParts) {
        String path = requestLineParts[1];
        if (!path.contains("?")) {
            return null;
        }
        return path.substring(path.indexOf("?") + 1);
    }

    private String validatePath(String path) throws IOException {
        if (path.equals("/") || !Files.exists(Path.of(resourcesRoot.toString(), path))) {
            return null;
        }
        return path;
    }

    private String validateMethod(String method) throws IOException {
        if (!server.getAllowedMethods().contains(method)) {
            return null;
        }
        return method;
    }

    private String extract() throws IOException {
        sb.setLength(0);
        String s = null;
        if (in.ready()) {
            while (true) {
                s = in.readLine();
                if (s == null || s.equals("")) {
                    break;
                }
                sb.append(s);
                sb.append("\r\n");
            }
        }
        return sb.toString().trim();
    }

    private String extractBody(int contentLength) throws IOException {
        char[] buf = new char[contentLength];
        in.read(buf, 0, contentLength);
        return new String(buf);
    }

    private int extractContentLength(String headers) {
        if (headers != null) {
            Optional<String> cl = headers.lines().filter(s -> s.startsWith("Content-Length:")).findFirst();
            if (cl.isPresent()) {
                String clLine = cl.get().trim();
                return Integer.parseInt(clLine.substring(clLine.indexOf(" ") + 1));
            }
        }
        return 0;
    }

    private byte[] contentHandler(Path filePath) throws IOException {
        return Files.readAllBytes(filePath);
    }

    private void responseWriter(BufferedOutputStream out,
                                String mimeType,
                                String path,
                                byte[] content,
                                Handler handler) throws IOException {
        sb.setLength(0);
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
                    .append("Content-Type: " + mimeType + "\r\n")
                    .append("Content-Length: ").append(contentLength).append("\r\n")
                    .append("\r\n");
        }
        out.write((
                sb.toString()
        ).getBytes());
        if (handler == null && content != null) {
            out.write(content);
        }
    }
}