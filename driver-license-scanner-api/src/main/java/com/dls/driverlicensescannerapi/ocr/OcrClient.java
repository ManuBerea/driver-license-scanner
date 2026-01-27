package com.dls.driverlicensescannerapi.ocr;

import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    private static final Logger logger = LoggerFactory.getLogger(OcrClient.class);

    private final RestTemplate restTemplate;
    private final String workerUrl;
    private final String internalKey;

    public OcrClient(
            RestTemplateBuilder builder,
            @Value("${OCR_WORKER_URL:}") String workerUrl,
            @Value("${X_INTERNAL_KEY:}") String internalKey
    ) {
        this.restTemplate = builder
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .build();
        this.workerUrl = workerUrl;
        this.internalKey = internalKey;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!StringUtils.hasText(workerUrl)) {
            throw new IllegalStateException("OCR_WORKER_URL must be configured");
        }
        if (!StringUtils.hasText(internalKey)) {
            throw new IllegalStateException("X_INTERNAL_KEY must be configured");
        }
    }

    public OcrResult scan(MultipartFile image, String requestId) {
        if (image == null || image.isEmpty()) {
            throw new OcrClientException(ErrorCatalog.INVALID_IMAGE_CODE, ErrorCatalog.MISSING_IMAGE_MESSAGE);
        }

        String filename = Objects.requireNonNullElse(image.getOriginalFilename(), "image");

        ByteArrayResource resource;
        try {
            resource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException ex) {
            logger.warn("Failed to read image bytes requestId={}", requestId, ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE, ex);
        }

        HttpHeaders partHeaders = new HttpHeaders();
        if (StringUtils.hasText(image.getContentType())) {
            partHeaders.setContentType(MediaType.parseMediaType(image.getContentType()));
        }
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, partHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-INTERNAL-KEY", internalKey);
        if (StringUtils.hasText(requestId)) {
            headers.set("X-Request-Id", requestId);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String ocrEndpoint = workerUrl.endsWith("/") ? workerUrl + "ocr" : workerUrl + "/ocr";

        try {
            ResponseEntity<OcrResult> response = restTemplate.postForEntity(
                    ocrEndpoint,
                    requestEntity,
                    OcrResult.class
            );
            if (response.getBody() == null) {
                logger.warn("OCR response empty requestId={} status={}", requestId, response.getStatusCode());
                throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
            }
            return response.getBody();
        } catch (ResourceAccessException ex) {
            logger.warn("OCR request timeout requestId={}", requestId, ex);
            throw new OcrClientException(ErrorCatalog.OCR_TIMEOUT_CODE, ErrorCatalog.OCR_TIMEOUT_MESSAGE);
        } catch (HttpStatusCodeException ex) {
            logger.warn("OCR request failed requestId={} status={}", requestId, ex.getStatusCode(), ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
        } catch (RestClientException ex) {
            logger.warn("OCR request error requestId={}", requestId, ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
        }
    }
}
