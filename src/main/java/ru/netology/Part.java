package ru.netology;

import java.io.File;

public class Part {

    private final String fieldName;
    private final String contentType;
    private final boolean isFormField;
    private final String fileName;
    private final byte[] body;

    public Part(String fieldName, String contentType, boolean isFormField, String fileName, byte[] body) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.body = body;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isFormField() {
        return isFormField;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getBody() {
        return body;
    }
}
