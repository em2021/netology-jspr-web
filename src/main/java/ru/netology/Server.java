package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.*;

import ru.netology.Handler;

public class Server {

    private final Path resourcesRoot = Path.of(".", "public");
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final ConcurrentMap<String, ConcurrentMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                threadPool.submit(new ConnectionProcessor(this, resourcesRoot, serverSocket.accept()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addHandler(String method, String path, Handler handler) throws Exception {
        ConcurrentMap<String, Handler> pathHandlerMap = new ConcurrentHashMap<>();
        pathHandlerMap.put(path, handler);
        if (handlers.containsKey(method)) { // check if map contains key (method)
            if (handlers.get(method).containsKey(path)) { // if yes - check if value map contains key (path)
                System.out.println("A handler with method \"" + method + "\" is already associated with the path \"" + path + "\"");
            } else { // if no - add new value map
                handlers.put(method, pathHandlerMap);
            }
        } else { // if no - add new key with new map value
            handlers.put(method, pathHandlerMap);
        }
    }

    public Handler getHandler(String method, String path) throws Exception {
        Handler h = null; // returns null if there is no suitable handler
        if (handlers.containsKey(method)) { // check if map contains key (method)
            if (handlers.get(method).containsKey(path)) { // if yes - check if value map contains key (path)
                h = handlers.get(method).get(path); // if yes - get the handler
            }
        }
        return h;
    }
}