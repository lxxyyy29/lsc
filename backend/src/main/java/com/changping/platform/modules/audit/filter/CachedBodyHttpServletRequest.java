package com.changping.platform.modules.audit.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体的 HttpServletRequest 包装器
 * 使请求体可以被多次读取（审计日志切面需要读取 body，后续控制器也要读取）
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyInputStream(cachedBody);
    }

    public String getBody() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    private static class CachedBodyInputStream extends ServletInputStream {
        private final ByteArrayInputStream source;

        CachedBodyInputStream(byte[] body) {
            this.source = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return source.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return source.read();
        }
    }
}
