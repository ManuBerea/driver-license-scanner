const MAX_FILE_BYTES = 10 * 1024 * 1024;
const MAX_CAPTURE_DIMENSION = 1920;
const UK_LICENSE_ASPECT_RATIO = 85.6 / 53.98;
const ALLOWED_MIME_TYPES = ["image/jpeg", "image/png", "image/webp"];
const ALLOWED_EXTENSIONS = [".jpg", ".jpeg", ".png", ".webp"];

function validateImageFile(file: File): string | null {
    const lowerName = file.name.toLowerCase();
    const hasAllowedExtension = ALLOWED_EXTENSIONS.some((ext) =>
        lowerName.endsWith(ext),
    );

    // If browser provides MIME type, trust it (more reliable than extension)
    if (file.type) {
        if (!ALLOWED_MIME_TYPES.includes(file.type)) {
            return "Unsupported file type. Please upload a JPG, PNG, or WEBP image.";
        }
    } else {
        // fallback for edge-case browsers
        if (!hasAllowedExtension) {
            return "Unsupported file type. Please upload a JPG, PNG, or WEBP image.";
        }
    }

    if (file.size > MAX_FILE_BYTES) {
        return "File is too large. Please upload an image smaller than 10MB.";
    }

    return null;
}

function formatBytes(bytes: number) {
    const kb = bytes / 1024;
    if (kb < 1024) return `${kb.toFixed(0)} KB`;
    const mb = kb / 1024;
    return `${mb.toFixed(2)} MB`;
}

export {
    ALLOWED_EXTENSIONS,
    ALLOWED_MIME_TYPES,
    MAX_CAPTURE_DIMENSION,
    MAX_FILE_BYTES,
    UK_LICENSE_ASPECT_RATIO,
    formatBytes,
    validateImageFile,
};
