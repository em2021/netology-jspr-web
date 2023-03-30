package ru.netology;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Request {

    private final static String CONTENT_DISPOSITION = "Content-Disposition:";
    private final static String CONTENT_TYPE = "Content-Type:";
    private final String method;
    private final String path;
    private final List<NameValuePair> params;
    private final List<String> headers;
    private final Map<String, List<String>> body = new HashMap<>();
    private final Map<String, List<Part>> bodyParts = new HashMap<>();
    private final String contentType;
    private byte[] boundary = null;
    private boolean isMultipart = false;

    public Request(String method, String path, String params, List<String> headers, byte[] body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.contentType = extractContentType(headers);
        if (contentType != null) {
            if (contentType.contains("multipart/form-data")) {
                isMultipart = true;
                this.boundary = extractBoundary(contentType);
                parseMultipartBody(body);
            } else if (contentType.contains("x-www-form-url-encoded")) {
                if (body != null) {
                    setBody(body);
                }
            }
        }
        if (params != null) {
            this.params = URLEncodedUtils.parse(params, Charset.defaultCharset());
        } else {
            this.params = null;
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }

    public String getQueryParam(String name) {
        String value = null;
        List<NameValuePair> queryParams = getQueryParams();
        Optional<NameValuePair> match = null;
        if (queryParams != null) {
            value = queryParams.stream()
                    .filter(s -> name.equals(s.getName()))
                    .findFirst()
                    .get()
                    .getValue();
        }
        return value;
    }

    private void setBody(byte[] body) {
        List<NameValuePair> postParams = URLEncodedUtils.parse(new String(body, Charset.defaultCharset()), Charset.defaultCharset());
        postParams.forEach(pair -> {
            String name = pair.getName();
            if (this.body.containsKey(name)) {
                this.body.get(name).add(pair.getValue());
            } else {
                List<String> paramsList = new ArrayList<>();
                paramsList.add(pair.getValue());
                this.body.put(name, paramsList);
            }
        });
    }

    private void addBodyParts(Part part) {
        String name = part.getFieldName();
        if (this.bodyParts.containsKey(name)) {
            this.bodyParts.get(name).add(part);
        } else {
            List<Part> partsList = new ArrayList<>();
            partsList.add(part);
            this.bodyParts.put(name, partsList);
        }
    }

    private String extractContentType(List<String> headers) {
        String ctLine = null;
        if (headers != null) {
            Optional<String> ct = headers.stream()
                    .filter(s -> s.startsWith("Content-Type:"))
                    .findFirst();
            if (ct.isPresent()) {
                ctLine = ct.get().trim();
                return ctLine.substring(ctLine.indexOf(" ") + 1);
            }
        }
        return ctLine;
    }

    private byte[] extractBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        Map<String, String> params = parser.parse(contentType, new char[]{';', ','});
        String boundaryStr = (String) params.get("boundary");
        if (boundaryStr == null) {
            return null;
        } else {
            byte[] boundary;
            try {
                boundary = boundaryStr.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException var7) {
                boundary = boundaryStr.getBytes();
            }

            return boundary;
        }
    }

    private String getFileName(String contentDisposition) {
        String fileName = null;
        if (contentDisposition != null) {
            String cdl = contentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                Map<String, String> params = parser.parse(contentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = (String) params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

    private String getContentDisposition(String header) {
        if (header.contains(CONTENT_DISPOSITION)) {
            return header.substring(CONTENT_DISPOSITION.length() + 1, header.indexOf("\r\n"));
        }
        return null;
    }

    private String getContentType(String header) {
        if (header.contains(CONTENT_TYPE)) {
            return header.substring(header.indexOf(CONTENT_TYPE) + CONTENT_TYPE.length() + 1, header.indexOf("\r\n\r\n"));
        }
        return null;
    }

    private void parseMultipartBody(byte[] body) {
        if (body != null) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(body)) {
                MultipartStream multipartStream = new MultipartStream(bais, boundary);
                boolean nextPart = multipartStream.skipPreamble();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ParameterParser pp = new ParameterParser();
                while (nextPart) {
                    String header = multipartStream.readHeaders();
                    String contentDisposition = getContentDisposition(header);
                    Map<String, String> nvPairs = pp.parse(contentDisposition, ';');
                    String fieldName = nvPairs.get("name");
                    String contentType = getContentType(header);
                    String fileName = getFileName(contentDisposition);
                    multipartStream.readBodyData(output);
                    output.flush();
                    boolean isFormField = true;
                    if (fileName != null) {
                        isFormField = false;
                    }
                    addBodyParts(new Part(fieldName, contentType, isFormField, fileName, output.toByteArray()));
                    nextPart = multipartStream.readBoundary();
                    output.reset();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public List<Part> getPart(String name) {
        return bodyParts.get(name);
    }

    public Map<String, List<Part>> getParts() {
        return bodyParts;
    }

    public Map<String, List<String>> getPostParams() {
        return body;
    }

    public List<String> getPostParam(String name) {
        return body.get(name);
    }

    public List<String> getHeaders() {
        return headers;
    }

    public boolean isMultipart() {
        return isMultipart;
    }
}