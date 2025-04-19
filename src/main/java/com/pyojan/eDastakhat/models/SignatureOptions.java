package com.pyojan.eDastakhat.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.oval.constraint.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureOptions {

    @NotEmpty(message = "page cannot be empty")
    @MatchPattern(pattern = "(?!0)(?i)(f|l|a|\\d+)", message = "Invalid page value. Valid values are: F, L, A, <number>")
    private String page = "L";

    @Size(min = 0, max = 4, message = "coord must have between 0 and 4 elements")
    private int[] coord = {};

    @Length(max = 25, message = "reason cannot be longer than 25 characters")
    private String reason = "";

    @Length(max = 40, message = "location cannot be longer than 40 characters")
    private String location = "";

    @Length(max = 60, message = "customText cannot be longer than 60 characters")
    private String customText = "";

    private boolean greenTick;
    private boolean changesAllowed;
    private boolean enableLtv;

    @AssertValid
    @NotNull(message = "timestamp cannot be null")
    private Timestamp timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timestamp {
        private boolean enabled;
        private String url;
        private String username;
        private String password;
    }
}
