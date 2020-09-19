package com.kapok.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
public class CodecUtil {

    public static String decodeResponseBody(byte[] buffer, ResponseEntity response) {
        try {
            String output;
            boolean isGZipEncoding = isGZipEncoding(response);
            if (isGZipEncoding) {
                output = plainTextFromGz(buffer);
            } else {
                output = new String(buffer);
            }
            return output;
        } catch (IOException e) {
            log.error("Encountered error when decoding response", e);
        }
        return new String(buffer);
    }

    public static boolean isGZipEncoding(ResponseEntity response) {
        HttpHeaders httpHeaders = response.getHeaders();
        if (httpHeaders.containsKey(HttpHeaders.CONTENT_ENCODING)) {
            List<String> contentEncodings = httpHeaders.get(HttpHeaders.CONTENT_ENCODING);
            if (null != contentEncodings && contentEncodings.size() > 0) {
                String contentEncoding = contentEncodings.get(0);
                return contentEncoding != null && contentEncoding.toLowerCase().contains("gzip");
            }
        }
        return false;
    }

    public static String plainTextFromGz(byte[] compressed) throws IOException {
        final StringBuilder outStr = new StringBuilder();
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        if (isCompressed(compressed)) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            final BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(gis, Charset.defaultCharset()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }
            gis.close();
        } else {
            outStr.append(compressed);
        }
        return outStr.toString();
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }


}
