package com.dls.driverlicensescannerapi.controller;

import com.dls.driverlicensescannerapi.dto.LicenseFields;
import com.dls.driverlicensescannerapi.dto.ScanResponse;
import com.dls.driverlicensescannerapi.dto.ValidationResult;
import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import com.dls.driverlicensescannerapi.service.ScanService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScanControllerTest {

    @Mock
    private ScanService scanService;

    @Test
    void returnsBadRequestWhenImageMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScanController(scanService)).build();

        mockMvc.perform(multipart("/license/scan"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.error.code", is(ErrorCatalog.INVALID_IMAGE_CODE)))
                .andExpect(jsonPath("$.error.message", is(ErrorCatalog.MISSING_IMAGE_MESSAGE)));
    }

    @Test
    void returnsBadRequestWhenImageTooLarge() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScanController(scanService)).build();

        byte[] bytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                bytes
        );

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.error.message", is(ErrorCatalog.IMAGE_TOO_LARGE_MESSAGE)));
    }

    @Test
    void returnsBadRequestWhenFormatInvalid() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScanController(scanService)).build();

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "invalid".getBytes()
        );

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.error.message", is(ErrorCatalog.INVALID_FORMAT_MESSAGE)));
    }

    @Test
    void returnsOkForValidImage() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScanController(scanService)).build();

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "license.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        ScanResponse response = new ScanResponse(
                "req-123",
                "paddle",
                List.of("paddle"),
                1.0,
                0.70,
                123L,
                new LicenseFields("ANDREA", "CAMPBELL", "05.07.1964", null, "99999999", "30.11.2031", List.of()),
                new ValidationResult(List.of(), List.of())
        );

        when(scanService.scan(any(), anyString())).thenReturn(response);

        mockMvc.perform(multipart("/license/scan")
                        .file(file)
                        .header("X-Request-Id", "req-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.requestId", is("req-123")));

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(scanService).scan(any(), requestIdCaptor.capture());
        assertEquals("req-123", requestIdCaptor.getValue());
    }
}
