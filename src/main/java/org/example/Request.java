package org.example;

import org.apache.http.client.utils.URLEncodedUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final String protocol;

    URLEncodedUtils utils = new URLEncodedUtils();

    public Request(String method, String path, String protocol) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getQueryParam(String name) {
        return getQueryParams().get(name);
    }

    public Map<String, String> getQueryParams() {
        String[] params = getPath().substring(getPath().indexOf("?") + 1).split("&");
        Map<String, String> paramsMap = new HashMap<>();
        Arrays.stream(params)
                .forEach(
                        param -> {
                            String[] paramPair = param.split("=");
                            paramsMap.put(paramPair[0], paramPair[1]);
                        }
                );
        return paramsMap;
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", protocol='" + protocol + '\'' +
                '}';
    }

    //public
}
