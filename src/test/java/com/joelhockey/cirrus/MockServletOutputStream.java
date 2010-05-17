package com.joelhockey.cirrus;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

public class MockServletOutputStream extends ServletOutputStream {
    private OutputStream outs;
    public MockServletOutputStream(OutputStream outs) {
        this.outs = outs;
    }

    @Override
    public void write(int b) throws IOException { outs.write(b); }
    @Override
    public void write(byte[] buf) throws IOException { outs.write(buf); }
    @Override
    public void write(byte[] buf, int start, int len) throws IOException { outs.write(buf, start, len); }
}
