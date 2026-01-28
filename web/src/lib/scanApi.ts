import type { ScanErrorPayload, ScanFields, ScanResult, ScanValidation } from "@/types/scan";

const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || "http://localhost:8080"
).replace(/\/$/, "");

type ScanResponse =
  | { ok: true; data: ScanResult }
  | { ok: false; message: string };

type ValidationResponse =
  | { ok: true; data: ScanValidation }
  | { ok: false; message: string };

export async function scanLicense(image: File): Promise<ScanResponse> {
  const formData = new FormData();
  formData.append("image", image);

  try {
    const response = await fetch(`${API_BASE_URL}/license/scan`, {
      method: "POST",
      body: formData,
    });

    const payload = (await response.json().catch(() => null)) as
      | ScanResult
      | ScanErrorPayload
      | null;

    if (!response.ok) {
      const message =
        payload && "error" in payload && payload.error?.message
          ? payload.error.message
          : "Scan failed. Please try again.";
      return { ok: false, message };
    }

    return { ok: true, data: payload as ScanResult };
  } catch {
    return { ok: false, message: "Scan failed. Please try again." };
  }
}

export async function validateLicenseFields(fields: ScanFields): Promise<ValidationResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/license/validate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(fields),
    });

    const payload = (await response.json().catch(() => null)) as
      | ScanValidation
      | ScanErrorPayload
      | null;

    if (!response.ok) {
      const message =
        payload && "error" in payload && payload.error?.message
          ? payload.error.message
          : "Validation failed. Please try again.";
      return { ok: false, message };
    }

    return { ok: true, data: payload as ScanValidation };
  } catch {
    return { ok: false, message: "Validation failed. Please try again." };
  }
}
