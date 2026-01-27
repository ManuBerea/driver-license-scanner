package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import com.dls.driverlicensescannerapi.parser.LicenseFieldParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScanService {

    private final OcrClient ocrClient;
    private final ResponseAssembler responseAssembler;

    public ScanService(OcrClient ocrClient, ResponseAssembler responseAssembler) {
        this.ocrClient = ocrClient;
        this.responseAssembler = responseAssembler;
    }

    public ScanResponse scan(MultipartFile image, String requestId) {
        OcrResult ocrResult = ocrClient.scan(image, requestId);
        LicenseFields fields = LicenseFieldParser.parse(ocrResult.lines());
        return responseAssembler.assemble(requestId, ocrResult, fields);
    }
}
