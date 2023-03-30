package ru.netology;

import org.apache.hc.core5.http.impl.nio.BufferedData;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConnectionProcessor implements Runnable {

    private final Path resourcesRoot;
    private final Socket socket;
    private final Server server;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    private final StringBuilder sb = new StringBuilder();
    private Handler handler = null;

    public ConnectionProcessor(Server server, Path resourcesRoot, Socket socket) throws IOException {
        this.resourcesRoot = resourcesRoot;
        this.socket = socket;
        this.server = server;
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try (socket; in; out) {
            String method;
            String path;
            String params;
            List<String> headers;
            int contentLength;
            byte[] body = null;
            String[] requestLine;

            final var limit = 4096;
            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                requestLine = null;
            } else {
                requestLine = extractRequestLine(buffer, requestLineEnd);
            }

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                headers = null;
            } else {
                // отматываем на начало буфера
                in.reset();
                // пропускаем requestLine
                in.skip(headersStart);
                headers = extractHeaders(headersEnd - headersStart);
            }

            method = validateMethod(extractMethod(requestLine));
            path = validatePath(extractPath(requestLine));
            params = extractParams(requestLine);
            contentLength = extractContentLength(headers);

            if (contentLength > 0) {
                in.skip(headersDelimiter.length);
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
                if (request.isMultipart()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedOutputStream bos = new BufferedOutputStream(baos);
                    Map<String, List<Part>> parts = request.getParts();
                    bos.write("<p>Request contains the following parts:<br>".getBytes());
                    parts.entrySet().forEach(s -> {
                        String key = s.getKey();
                        List<Part> partsList = s.getValue();
                        try {
                            bos.write((key + ": ").getBytes());
                            partsList.forEach(k -> {
                                try {
                                    if (!k.isFormField()) {
                                        bos.write((k.getFileName() + "; ").getBytes());
                                        bos.write((k.getContentType() + "; ").getBytes());
                                        bos.write(("size: " + k.getBody().length + "<br>").getBytes());
                                    } else {
                                        bos.write(k.getBody());
                                        bos.write("<br>".getBytes());
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    bos.write("</p>".getBytes());
                    bos.flush();
                    final var content = baos.toByteArray();
                    responseWriter(out, null, path, content, handler);
                } else {
                    final var content = request.getPostParams().toString().getBytes();
                    responseWriter(out, null, path, content, handler);
                }
            } else {
                responseWriter(out, null, null, null, handler);
            }
        } catch (
                IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] extractRequestLine(byte[] buffer, int requestLineEnd) throws IOException {
        // must be in form GET /path HTTP/1.1
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }
        return requestLine;
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

    private List<String> extractHeaders(int length) throws IOException {
        final var headersBytes = in.readNBytes(length);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        return headers;
    }

    private int extractContentLength(List<String> headers) {
        if (headers != null) {
            Optional<String> cl = headers.stream()
                    .filter(s -> s.startsWith("Content-Length:"))
                    .findFirst();
            if (cl.isPresent()) {
                String clLine = cl.get().trim();
                return Integer.parseInt(clLine.substring(clLine.indexOf(" ") + 1));
            }
        }
        return 0;
    }

    private byte[] extractBody(int contentLength) throws IOException {
        final var bodyBytes = in.readNBytes(contentLength);
        final var body = new String(bodyBytes);
        return bodyBytes;
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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