package com.pyojan.eDastakhat.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pyojan.eDastakhat.utils.FileUtil;
import lombok.Getter;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ModelValidator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SignatureOptions modal;

    public ModelValidator(Path signatureOptionJsonPath) throws IOException {
        if(signatureOptionJsonPath == null) {
            throw new NullPointerException("Signature options json file path is required.");
        }

        // ensuring file is exist or not
        if (!FileUtil.fileExists(signatureOptionJsonPath)) {
            throw new NoSuchFileException("Signature JSON file does not exist at the specified path: " + signatureOptionJsonPath);
        }

        this.modal = readSignatureOptionJsonFileReturnAsModel(signatureOptionJsonPath);
    }

    private SignatureOptions readSignatureOptionJsonFileReturnAsModel(Path signatureOptionJsonPath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(signatureOptionJsonPath));
        return gson.fromJson(jsonContent, SignatureOptions.class);
    }

    public void validatePdfPayloadModel() throws IllegalArgumentException {
        Validator validator = new Validator();
        List<ConstraintViolation> violations = validator.validate(modal);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
            throw new IllegalArgumentException(String.valueOf(errors));
        }
    }
}
