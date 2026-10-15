package com.llmscannerseguridad.demo.http;

public final class RequestFailure extends RuntimeException {
    private final int status;
    private final String code;

    public RequestFailure(int status, String code) {
        super(code, null, false, false);
        this.status = status;
        this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
}
