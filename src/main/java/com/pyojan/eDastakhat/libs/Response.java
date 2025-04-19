package com.pyojan.eDastakhat.libs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter @Setter
public class Response<T> {
    private final static Gson jsonPrinter = new GsonBuilder().setPrettyPrinting().create();
    private String status;
    private T data;

    private Response(String status, T data) {
        this.status = status;
        this.data = data;
    }

    public static void generateSuccessResponse(LinkedHashMap<String, String> data) {
        Response<LinkedHashMap<String, String>> response = new Response<>("SUCCESS", data);
        String json = jsonPrinter.toJson(response);
        System.out.println(json);
    }



    /**
     * Generates a JSON error response from the given exception.
     * The response includes the exception message and full stack trace.
     * The JSON response is then printed to the standard error output.
     *
     * @param ex the Throwable exception to be processed into an error response
     */
    public static void generateErrorResponse(Throwable ex) {
        Map<String, String> errorResponse = new LinkedHashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("cause", getFullStackTrace(ex));

        Response<Map<String, String>> response = new Response<>("FAILED", errorResponse);
        String json = jsonPrinter.toJson(response);

        System.err.println(json);
    }


    private static String getFullStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while (throwable != null) {
            throwable.printStackTrace(pw);
            throwable = throwable.getCause();
        }
        return sw.toString();
    }
}
