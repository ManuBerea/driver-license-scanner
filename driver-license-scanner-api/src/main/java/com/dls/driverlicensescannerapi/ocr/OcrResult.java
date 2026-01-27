package com.dls.driverlicensescannerapi.ocr;

import java.util.List;

public record OcrResult(
        String requestId,
        String engine,
        double confidence,
        List<OcrLine> lines,
        long processingTimeMs,
        String rawText
) {}
