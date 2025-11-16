package com.clooney.agent.inspect;

import java.util.Map;

public class APICall {
    public String method;
    public String url;
    public String path;
    public Map<String, Object> query;
    public Object requestBody;
    public int status;
    public Object responseBody;
}
