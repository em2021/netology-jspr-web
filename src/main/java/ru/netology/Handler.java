package ru.netology;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handler {

    abstract void handle(Request request, BufferedOutputStream responseStream);
}