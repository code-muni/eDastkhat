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
    @MatchPattern(
            pattern = "^(?i)(a|f|l|[1-9]\\d*)(?:-(?i)(a|f|l|[1-9]\\d*))?(?:,(?i)(a|f|l|[1-9]\\d*)(?:-(?i)(a|f|l|[1-9]\\d*))?)*$",
            message = "Invalid page format. Examples: '1', 'F', 'L', 'A', '1-5', '1,3,5', 'F,L,2-4'"
    )
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
