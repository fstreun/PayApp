package ch.ethz.inf.vs.fstreun.network;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fabio on 12/23/17.
 * Parses HTTP requests.
 * Body is only read if Content-Length is available
 */

public class HttpParser {

    private final String TAG = "HTTPPARSER";

    // BufferedReader given in the constructor and used by the parser.
    public final BufferedReader bufferedReader;

    private String requestLine;
    private String requestMethod;
    private String requestURI;
    private String requestHTTPVersion;

    private String head = "";
    private Map<String, String> headerFields = new HashMap<>();

    private String body = "";

    private boolean error;
    private List<Exception> exceptions = new ArrayList<>();

    public HttpParser(@NonNull BufferedReader bufferedReader){

        this.bufferedReader = bufferedReader;

        try {

            // parse first line
            String firstLine = bufferedReader.readLine();
            if (firstLine == null || firstLine.isEmpty()){
                error = true;
                exceptions.add(new IOException("HTTP Parser Error: could not read from buffered reader!"));
                return;
            }

            try {
                // might throw IOException if format is wrong
                parserRequestLine(firstLine);
            }catch (IOException e){
                // wrong format of request line.
                error = true;
                exceptions.add(new IOException("HTTP Parser Error: wrong request line format.", e));
            }


            // parse all header fields
            String headerLine;
            while ((headerLine = bufferedReader.readLine()) != null){
                if (headerLine.isEmpty()){
                    // end of header
                    break;
                }else {
                    try {

                        // might throw IOException if format is wrong
                        parseHeaderFields(headerLine);
                    }catch (IOException e){
                        error = true;
                        exceptions.add(new IOException("HTTP Parser Error: wrong header line format.", e));
                    }
                }
            }


            // get content length header field.
            String contentLength = getHeaderFieldValue("Content-Length");
            Integer maxLength = null;

            if (contentLength != null){
                // header field is available
                try{
                    maxLength = Integer.parseInt(contentLength);
                }catch (NumberFormatException e){
                    // unsuccessful parsing of integer
                    error = true;
                    exceptions.add(new IOException("HTTP Parser Error: Failed to parser Content-Length header value: " + contentLength, e));
                }
            }


            if (maxLength != null) {
                // read body char by char
                StringBuilder bodyLine = new StringBuilder(maxLength);
                int c;
                for (int i = 0; i < maxLength; i++) {
                    c = bufferedReader.read();
                    bodyLine.append((char) c);
                }

                parseBody(bodyLine.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
            error = true;
            exceptions.add(new IOException("Http Parser Error", e));
        }
    }


    /**
     * Parses the request line of a http request
     * @param line has to contain at most three parameters split by a space
     * @throws IOException if to many many parameters are there
     */
    private void parserRequestLine(@NonNull String line) throws IOException {
        String[] parts = line.split(" ", -1);

        requestLine = line;

        if (parts.length < 3){
            throw new IOException("Illegal request line format: " + line);
        }

        // TODO: distinguish between response and request...

        if (parts[0].startsWith("HTTP")){
            // it is a response

        }else{
            // it is a request
            if (parts.length != 3){
                throw new IOException("Illegal request line format: " + line);
            }

            requestMethod = parts[0];
            requestURI = parts[1];
            requestHTTPVersion = parts[2];
        }

    }

    /**
     * Parses header fields of a http request.
     * Appends line to the string head.
     * Case-insensitive and appends values of the same header field with a comma to each other.
     * @param line contains a header key ending with a ':' and followed by values.
     * @throws IOException if format is not fulfilled
     */
    private void parseHeaderFields(@NonNull String line) throws IOException {

        // check header line format
        int endOfKey = line.indexOf(':');
        if (endOfKey < 0){
            throw new IOException("Following character is missing: ':' in the line: " + line);
        }

        // append line to head string
        head += line + "\n";

        // add line to header map
        String fieldKey = line.substring(0, endOfKey);
        String fieldValue = line.substring(endOfKey + 2);
        if (headerFields.containsKey(fieldKey)){
            // field key already parsed. Append new values with an comma.
            fieldValue = headerFields.get(fieldKey) + ", " + fieldValue;
        }
        headerFields.put(fieldKey.toLowerCase(), fieldValue.toLowerCase());
    }

    /**
     * parses the body of a http request.
     * @param line to be append to the already parsed body
     */
    private void parseBody(@NonNull String line){
        body += line;
    }


    /**
     *
     * @return the parsed requestLine
     */
    public String getRequestLine(){
        return requestLine;
    }

    /**
     *
     * @return the parsed request method.
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     *
     * @return the parsed request URI
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**
     *
     * @return the parsed request HTTP Version
     */
    public String getRequestHTTPVersion() {
        return requestHTTPVersion;
    }

    /**
     *
     * @param fieldKey (case-insensitive)
     * @return header field value or null if not parsed
     */
    public String getHeaderFieldValue(String fieldKey){
        return headerFields.get(fieldKey.toLowerCase());
    }

    /**
     *
     * @return the parsed body of the http request
     */
    public String getBody(){
        return body;
    }

    /**
     *
     * @return true if an error occurred during parsing.
     */
    public boolean isError() {return error;}

    /**
     *
     * @return a copy of the exception list.
     */
    public List<Exception> getExceptions() {
        return new ArrayList<>(exceptions);
    }


    @Override
    public String toString() {
        return error ? "Error occurred during http parsing" :
                requestLine + "\n" + head + "\n" + body;
    }
}
