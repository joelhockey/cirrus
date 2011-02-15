// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletInputStream;

public class MockServletInputStream extends ServletInputStream {
    ByteArrayInputStream bais;
    public MockServletInputStream(String content) {
        this.bais = new ByteArrayInputStream(content != null ? content.getBytes() : new byte[0]);
    }

    @Override
    public int read() throws IOException { return bais.read(); }
    @Override
    public int read(byte[] buf, int start, int len) { return bais.read(buf, start, len); }
}
