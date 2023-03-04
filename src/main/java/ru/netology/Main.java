package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) throws Exception {

        Server server = new Server();
        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            Path path = Path.of(".", "/public", request.getPath());
            try {
                final var template = Files.readString(path);
                responseStream.write(template.replace(
                        "{time}",
                        LocalDateTime.now().toString()).getBytes());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
        server.listen(9999);
    }
}