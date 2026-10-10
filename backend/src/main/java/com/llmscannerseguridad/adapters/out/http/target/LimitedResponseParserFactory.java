package com.llmscannerseguridad.adapters.out.http.target;

import java.io.IOException;
import java.io.InputStream;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.impl.io.DefaultHttpResponseParser;
import org.apache.hc.core5.http.io.HttpMessageParser;
import org.apache.hc.core5.http.io.HttpMessageParserFactory;
import org.apache.hc.core5.http.io.HttpTransportMetrics;
import org.apache.hc.core5.http.io.SessionInputBuffer;
import org.apache.hc.core5.util.CharArrayBuffer;

final class LimitedResponseParserFactory implements HttpMessageParserFactory<ClassicHttpResponse> {
    @Override
    public HttpMessageParser<ClassicHttpResponse> create(Http1Config config) {
        var parser = new DefaultHttpResponseParser(config);
        int[] totals = {0, 0};
        return (buffer, input) -> parser.parse(new HeaderBuffer(buffer, totals), input);
    }

    static final class HeaderLimitException extends IOException {
        HeaderLimitException() {
            super("Las cabeceras de respuesta exceden el límite.");
        }
    }

    private static final class HeaderBuffer implements SessionInputBuffer {
        private final SessionInputBuffer delegate;
        private final int[] totals;
        private boolean statusLine = true;

        HeaderBuffer(SessionInputBuffer delegate, int[] totals) {
            this.delegate = delegate;
            this.totals = totals;
        }

        @Override
        public int readLine(CharArrayBuffer line, InputStream input) throws IOException {
            int read = delegate.readLine(line, input);
            if (statusLine) {
                statusLine = false;
            } else if (read >= 0) {
                totals[0] += read + 2;
                if (read > 0) totals[1]++;
                if (totals[0] > 32768 || totals[1] > 100) {
                    throw new HeaderLimitException();
                }
            }
            return read;
        }

        @Override public int length() { return delegate.length(); }
        @Override public int capacity() { return delegate.capacity(); }
        @Override public int available() { return delegate.available(); }
        @Override public int read(byte[] b, int off, int len, InputStream input) throws IOException {
            return delegate.read(b, off, len, input);
        }
        @Override public int read(byte[] b, InputStream input) throws IOException { return delegate.read(b, input); }
        @Override public int read(InputStream input) throws IOException { return delegate.read(input); }
        @Override public HttpTransportMetrics getMetrics() { return delegate.getMetrics(); }
    }
}
