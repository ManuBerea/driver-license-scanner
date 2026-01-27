package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.dto.ValidationResult;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResponseAssembler {

    public ScanResponse assemble(String requestId, OcrResult ocrResult, LicenseFields fields) {
        String selectedEngine = ocrResult.engine();
        List<String> attemptedEngines = selectedEngine == null ? List.of() : List.of(selectedEngine);

        return new ScanResponse(
                requestId,
                selectedEngine,
                attemptedEngines,
                ocrResult.confidence(),
                ocrResult.processingTimeMs(),
                fields,
                new ValidationResult(List.of(), List.of())
        );
    }
}
