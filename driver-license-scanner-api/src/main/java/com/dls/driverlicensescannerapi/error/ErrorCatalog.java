package com.dls.driverlicensescannerapi.error;

public final class ErrorCatalog {

    public static final String INVALID_IMAGE_CODE = "INVALID_IMAGE";

    public static final String MISSING_IMAGE_MESSAGE =
            "No image provided. Please upload a JPG, PNG, or WEBP file under 10MB.";

    public static final String IMAGE_TOO_LARGE_MESSAGE =
            "Image is too large. Please upload a JPG, PNG, or WEBP file under 10MB.";

    public static final String INVALID_FORMAT_MESSAGE =
            "Unsupported image format. Please upload a JPG, PNG, or WEBP file.";

    public static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE =
            "Unsupported media type. Use multipart/form-data with an image field.";

    private ErrorCatalog() {}
}
