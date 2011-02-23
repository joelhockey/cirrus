// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helps to read resources
 * @author Joel Hockey
 */
public class Resource {
    private static final Log log = LogFactory.getLog(Resource.class);

    /**
     * Return set of files in given path or empty set for invalid path.
     * Uses files returned from {@link File#list()}
     * then adds any files from classloader.
     * Looks up URL of '/resource.properties' with {@link Class#getResource(String)}.
     * If found in classloader, adds file in same filesystem or jar file as
     * '/resource.properties' that match the given path.
     * @param path dir to get all files within
     * @return Set of files in given path or empty set for invalid path.
     * Should start with leading slash and  use forward slash for pathsep.
     * @throws IOException if error reading files
     */
    public static Set<String> getResourcePaths(String path) throws IOException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        // create result set
        Set<String> result = new HashSet<String>();

        // load via classpath.  Use '/resource.properties' to detect file or jar path
        String resource = Resource.class.getResource("/resource.properties").toString();

        // file is in regular file - file:/dir/dir
        if (resource.startsWith("file:")) {
            try {
                File classes = new File(new URI(resource)).getParentFile();
                File dir = new File(classes, path);
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File file : list) {
                        // add trailing slash for dirs
                        result.add(path + file.getName() +
                                (file.isDirectory() ? "/" : ""));
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected error converting URI: " + resource, e);
            }

        // file is in jar - jar:file:/.../app/WEB-INF/lib/cirrus.jar!...
        } else if (resource.startsWith("jar:")) {
            String jarFileName = resource.substring(4); // skip 'jar:'
            int bang = jarFileName.lastIndexOf('!');
            if (bang != -1) {
                jarFileName = jarFileName.substring(0, bang);
            }
            ZipFile zipFile = null;
            try {
                // read entries of zip file to find files in same dir
                zipFile = new ZipFile(new File(new URI(jarFileName)));
                for (Enumeration<? extends ZipEntry> en = zipFile.entries();
                        en.hasMoreElements(); ) {

                    ZipEntry entry = en.nextElement();
                    String entryName = entry.getName();
                    // canonicalize filename (start with slash, only forward slash)
                    entryName = entryName.replace('\\', '/');
                    if (!entryName.startsWith("/")) {
                        entryName = "/" + entryName;
                    }
                    if (entryName.startsWith(path)) {
                        // only match files in same dir, not subdirs
                        int slash = entryName.indexOf('/', path.length());
                        if (slash != -1) {
                            // keep trailing slash
                            entryName = entryName.substring(0, slash + 1);
                        }
                        result.add(entryName);
                    }
                }
            } catch (Exception e) {
                log.warn("Unexpected error converting URI: " + resource, e);
            }
            if (zipFile != null) {
                zipFile.close();
            }
        }
        return result;
    }

    /**
     * Reads file via classloader
     * @param fileName file to read - should start with slash, else
     * slash prepended
     * @return contents of file, or IOException if file not exists
     * @throws IOException if file does not exist
     */
    public static String readFile(String fileName) throws IOException {
        if (fileName == null) {
            throw new IOException("file does not exist: " + fileName);
        }
        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }

        InputStream ins = Resource.class.getResourceAsStream(fileName);
        if (ins == null) {
            throw new IOException("file does not exist: " + fileName);
        }
        byte[] buf = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int len = 0; (len = ins.read(buf)) != -1;) {
            baos.write(buf, 0, len);
        }
        ins.close();
        return baos.toString();
    }
}
