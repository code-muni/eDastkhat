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

/**
 * Represents a response with a status and data payload.
 *
 * @param <T> the type of the data payload
 */
@Getter @Setter
public class Response<T> {
    private final static Gson jsonPrinter = new GsonBuilder().setPrettyPrinting().create();
    private String status;
    private T data;

    /**
     * Constructs a new Response with the given status and data.
     * @param status the status of the response (SUCCESS/ERROR)
     * @param data the data payload of the response
     */
    private Response(String status, T data) {
        this.status = status;
        this.data = data;
    }

    /**
     * Generates and prints a success response in JSON format.
     * @param data the data to include in the success response
     */
    public static void generateSuccessResponse(LinkedHashMap<String, String> data) {
        Response<LinkedHashMap<String, String>> response = new Response<>("SUCCESS", data);
        String json = jsonPrinter.toJson(response);
        System.out.println(json);
        System.exit(0); // Exit with a success code
    }


    /**
     * Prints a success response with a status and generic data as a formatted JSON string to STDOUT,
     * then exits the program with a success code (0).
     *
     * @param <T>  the type of the response data
     * @param data the response data object to be included in the response
     */
    public static <T> void generateSuccessResponse(T data) {
        Response<T> response = new Response<>("SUCCESS", data);
        String json = jsonPrinter.toJson(response);
        System.out.println(json);
        System.exit(0); // Exit with a success code
    }


    /**
     * Generates and prints an error response in JSON format with the exception message and stack trace.
     * @param e the exception for which the error response is generated
     */
    public static void generateErrorResponse(Exception e) {
        LinkedHashMap<String, String> errorData = new LinkedHashMap<>();
        errorData.put("message", e.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        errorData.put("stackTrace", sw.toString());

        Response<LinkedHashMap<String, String>> response = new Response<>("ERROR", errorData);
        String json = jsonPrinter.toJson(response);
        System.err.println(json);
        System.exit(1); // Exit with an error code
    }
}
