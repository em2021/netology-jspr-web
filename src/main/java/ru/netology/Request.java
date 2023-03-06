package ru.netology;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.List;

public class Request {

    private final String method;
    private final String path;
    private final String params;
    private final String headers;
    private final String body;

    public Request(String method, String path, String params, String headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.params = params;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List <NameValuePair> getQueryParams() {
        if (params == null) {
            return null;
        }
        return URLEncodedUtils.parse(params, Charset.defaultCharset());
    }

    public NameValuePair getQueryParam(String name) {
        if (params == null) {
            return null;
        }
        List<NameValuePair> queryParams = getQueryParams();
        return queryParams.get(queryParams.indexOf(name));
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}