package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.*;

public class Request {

    private final String method;
    private final String path;
    private final List<NameValuePair> params;
    private final String headers;
    private final Map<String, List<String>> body = new HashMap<>();

    public Request(String method, String path, String params, String headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        if (body != null) {
            setBody(body);
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

    private void setBody(String body) {
        List<NameValuePair> postParams = URLEncodedUtils.parse(body, Charset.defaultCharset());
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

    public Map<String, List<String>> getPostParams() {
        return body;
    }

    public List<String> getPostParam(String name) {
        return body.get(name);
    }

    public String getHeaders() {
        return headers;
    }
}