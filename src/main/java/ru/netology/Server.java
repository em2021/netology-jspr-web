package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.*;

public class Server {

    private final Path resourcesRoot = Path.of(".", "public");
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                threadPool.submit(new ConnectionProcessor(resourcesRoot, serverSocket.accept()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}