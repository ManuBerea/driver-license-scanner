package com.dls.driverlicensescannerapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dls.driverlicensescannerapi.config.ScanProperties;
import com.dls.driverlicensescannerapi.service.ImageValidationService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScanControllerTest {

    private MockMvc mockMvc;
    private ScanProperties scanProperties;

    @BeforeEach
    void setUp() {
        scanProperties = new ScanProperties();
        mockMvc = MockMvcBuilders.standaloneSetup(new ScanController(new ImageValidationService(scanProperties))).build();
    }

    @Test
    void scan_returnsStubResponseOnValidImage() throws Exception {
        byte[] payload = sampleJpeg();
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, payload);

        MvcResult result = mockMvc.perform(multipart("/license/scan").file(image)
                        .header("X-Request-Id", "test-req"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.requestId").value("test-req"))
                .andExpect(jsonPath("$.selectedEngine").value("stub"))
                .andExpect(jsonPath("$.attemptedEngines[0]").value("stub"))
                .andExpect(jsonPath("$.fields").exists())
                .andExpect(jsonPath("$.validation.blockingErrors").isArray())
                .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void scan_acceptsContentTypeWithParameters() throws Exception {
        byte[] payload = sampleJpeg();
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg;charset=UTF-8", payload);

        mockMvc.perform(multipart("/license/scan").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedEngine").value("stub"));
    }

    @Test
    void scan_returnsInvalidImageWhenMissingFile() throws Exception {
        mockMvc.perform(multipart("/license/scan"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void scan_returnsInvalidImageWhenWrongType() throws Exception {
        byte[] payload = "not-an-image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("image", "file.txt", MediaType.TEXT_PLAIN_VALUE, payload);

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void scan_rejectsContentTypeWhenExtensionIsAllowed() throws Exception {
        byte[] payload = "still-not-image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("image", "file.jpg", MediaType.TEXT_PLAIN_VALUE, payload);

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void scan_returnsInvalidImageWhenTooLarge() throws Exception {
        scanProperties.setMaxFileBytes(10);
        byte[] payload = sampleJpeg();
        MockMultipartFile file = new MockMultipartFile("image", "large.jpg", MediaType.IMAGE_JPEG_VALUE, payload);

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void scan_returnsStubResponseWhenContentTypeMissingButExtensionAllowed() throws Exception {
        byte[] payload = sampleJpeg();
        MockMultipartFile file = new MockMultipartFile("image", "fallback.jpg", null, payload);

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedEngine").value("stub"));
    }

    @Test
    void scan_rejectsInvalidMagicBytesEvenWhenTypeAllowed() throws Exception {
        byte[] payload = new byte[12];
        MockMultipartFile file = new MockMultipartFile("image", "fake.jpg", MediaType.IMAGE_JPEG_VALUE, payload);

        mockMvc.perform(multipart("/license/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    private byte[] sampleJpeg() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46,
                0x49, 0x46, 0x00, 0x01
        };
    }
}
