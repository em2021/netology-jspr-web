package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Request {

    private final String method;
    private final String path;
    private final List<NameValuePair> params;
    private final String headers;
    private final String body;

    public Request(String method, String path, String params, String headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
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

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}