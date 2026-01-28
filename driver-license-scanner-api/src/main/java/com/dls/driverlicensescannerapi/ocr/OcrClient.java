package com.dls.driverlicensescannerapi.ocr;

import com.dls.driverlicensescannerapi.error.ErrorCatalog;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.net.ConnectException;
import java.net.UnknownHostException;

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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(CONNECT_TIMEOUT.toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(READ_TIMEOUT.toMillis()));
        this.restTemplate.setRequestFactory(requestFactory);
        ensureMultipartConverter();
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

    private void ensureMultipartConverter() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());
        boolean hasFormConverter = converters.stream()
                .anyMatch(converter -> converter instanceof FormHttpMessageConverter);
        if (!hasFormConverter) {
            converters.add(0, new FormHttpMessageConverter());
            restTemplate.setMessageConverters(converters);
        }
    }

    public OcrResult scan(MultipartFile image, String requestId) {
        return scan(image, requestId, null);
    }

    public OcrResult scan(MultipartFile image, String requestId, String engine) {
        if (image == null || image.isEmpty()) {
            throw new OcrClientException(ErrorCatalog.INVALID_IMAGE_CODE, ErrorCatalog.MISSING_IMAGE_MESSAGE);
        }

        String filename = Objects.requireNonNullElse(image.getOriginalFilename(), "image");

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException ex) {
            logger.warn("Failed to read image bytes requestId={}", requestId, ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE, ex);
        }

        HttpHeaders partHeaders = new HttpHeaders();
        if (StringUtils.hasText(image.getContentType())) {
            partHeaders.setContentType(MediaType.parseMediaType(image.getContentType()));
        } else {
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        partHeaders.setContentDispositionFormData("image", filename);
        HttpEntity<byte[]> filePart = new HttpEntity<>(imageBytes, partHeaders);

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-INTERNAL-KEY", internalKey);
        if (StringUtils.hasText(engine)) {
            headers.set("X-OCR-ENGINE", engine);
        }
        if (StringUtils.hasText(requestId)) {
            headers.set("X-Request-Id", requestId);
        }

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
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
            if (isConnectFailure(ex)) {
                throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
            }
            throw new OcrClientException(ErrorCatalog.OCR_TIMEOUT_CODE, ErrorCatalog.OCR_TIMEOUT_MESSAGE);
        } catch (HttpStatusCodeException ex) {
            logger.warn("OCR request failed requestId={} status={}", requestId, ex.getStatusCode(), ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
        } catch (RestClientException ex) {
            logger.warn("OCR request error requestId={}", requestId, ex);
            throw new OcrClientException(ErrorCatalog.OCR_FAILED_CODE, ErrorCatalog.OCR_FAILED_MESSAGE);
        }
    }

    private boolean isConnectFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
