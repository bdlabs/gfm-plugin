package com.github.shyykoserhiy.gfm.markdown.offline;

import com.github.shyykoserhiy.gfm.markdown.AbstractMarkdownParser;
import com.github.shyykoserhiy.gfm.markdown.GfmRequestDoneListener;
import com.github.shyykoserhiy.gfm.resource.ResourceUnzip;
import com.intellij.openapi.util.io.FileUtil;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class JnaMarkdownParser extends AbstractMarkdownParser {
    private static boolean jnaInitialized;
    private static MarkdownJna markdownJna;

    public JnaMarkdownParser(GfmRequestDoneListener requestDoneListener) {
        super(requestDoneListener);
    }

    public static String getNativeLibraryUrl() {
        if (Platform.isMac()) {
            return FileUtil.join("osx64", "libmarkdown.dylib");
        }
        if (Platform.isLinux()) {
            if (Platform.is64Bit()) {
                return FileUtil.join("linux64", "libmarkdown.so");
            } else {
                return FileUtil.join("linux32", "libmarkdown.so");
            }
        }
        if (Platform.isWindows()) {
            if (Platform.is64Bit()) {
                return FileUtil.join("win64", "libmarkdown.dll");
            } else {
                return FileUtil.join("win32", "libmarkdown.dll");
            }
        }
        return null;
    }

    public synchronized static void initializeJna() throws PlatformNotSupported {
        if (!isSupported()) {
            throw new PlatformNotSupported();
        }
        if (jnaInitialized) {
            return;
        }
        File nativeDirectory = new ResourceUnzip().unzipResources("/com/github/shyykoserhiy/gfm/native/native.zip");
        File nativeLibrary = new File(nativeDirectory, getNativeLibraryUrl());
        markdownJna = (MarkdownJna) Native.synchronizedLibrary((MarkdownJna)
                Native.loadLibrary(nativeLibrary.getPath(), MarkdownJna.class));
        jnaInitialized = true;
    }

    public static boolean isSupported() {
        return getNativeLibraryUrl() != null;
    }

    @Override
    public GfmWorker getWorker(String parentFolder, String filename, String markdown, boolean useFileAsSuccessResponse) {
        return new GfmWorker(parentFolder, filename, markdown, useFileAsSuccessResponse);
    }

    private MarkdownJna.Buffer markdownToHtml(String markdown) throws PlatformNotSupported {
        initializeJna();
        return markdownJna.markdownToHtml(markdown, markdown.getBytes(Charset.forName("UTF-8")).length);
    }

    private class GfmWorker extends AbstractMarkdownParser.GfmWorker {
        public GfmWorker(String parentFolder, String filename, String markdown, boolean useFileAsSuccessResponse) {
            super(parentFolder, filename, markdown, useFileAsSuccessResponse);
        }

        @Override
        public void run() {
            try {
                try {
                    fireSuccess(markdownToHtml(markdown).toString());
                } catch (IOException e) {
                    fireFail(e.getMessage(), ExceptionUtils.getStackTrace(e));
                }
            } catch (PlatformNotSupported platformNotSupported) {
                fireFail(platformNotSupported.getMessage(), ExceptionUtils.getStackTrace(platformNotSupported));
            }
        }
    }
}
