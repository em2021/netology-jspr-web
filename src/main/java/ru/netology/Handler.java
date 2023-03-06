package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface Handler {

    abstract void handle(Request request, BufferedOutputStream responseStream) throws IOException;
}