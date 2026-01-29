package com.dls.driverlicensescannerapi.service;

import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.ocr.OcrClient;
import com.dls.driverlicensescannerapi.ocr.OcrLine;
import com.dls.driverlicensescannerapi.ocr.OcrResult;
import java.util.List;

import com.dls.driverlicensescannerapi.validator.ValidationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OcrPipelineIntegrationTest {

    @Test
    void pipelineParsesAndValidates() {
        OcrClient ocrClient = Mockito.mock(OcrClient.class);
        ResponseAssembler assembler = new ResponseAssembler(0.70, new ValidationService());
        ScanService scanService = new ScanService(ocrClient, assembler, false, 1, 0.70);

        List<OcrLine> lines = List.of(
                new OcrLine("1. CAMPBELL", 0.99),
                new OcrLine("2. ANDREA", 0.99),
                new OcrLine("3. 05.07.1964", 0.98),
                new OcrLine("4b. 30.11.2031", 0.97),
                new OcrLine("5. 99999999", 0.99),
                new OcrLine("8. 123 CASTLEROCK ROAD, COLERAINE, BT51 3TB", 0.95)
        );
        OcrResult ocrResult = new OcrResult("req-1", "paddle", 0.9, lines, 123L, null);
        Mockito.when(ocrClient.scan(Mockito.any(), Mockito.any())).thenReturn(ocrResult);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "license.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        ScanResponse response = scanService.scan(image, "req-1");

        assertEquals("ANDREA", response.fields().firstName());
        assertEquals("CAMPBELL", response.fields().lastName());
        assertEquals("05.07.1964", response.fields().dateOfBirth());
        assertEquals("30.11.2031", response.fields().expiryDate());
        assertFalse(response.validation().blockingErrors().isEmpty());
    }
}
