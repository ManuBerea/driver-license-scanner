"use client";

import { useEffect, useMemo, useState } from "react";

import { UK_LICENSE_ASPECT_RATIO, formatBytes } from "@/lib/imageUtils";
import type { ScanResult } from "@/types/scan";
import styles from "@/components/capture/PreviewPanel.module.css";

type PreviewPanelProps = {
  file: File;
  onChooseAnother: () => void;
  onRetake: () => void;
  onScan: () => void;
  isScanning: boolean;
  scanError: string | null;
  scanResult: ScanResult | null;
  editableFields: EditableFields;
  onFieldChange: (field: keyof EditableFields, value: string) => void;
};

type EditableFields = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  addressLine: string;
  licenceNumber: string;
  expiryDate: string;
  categories: string;
};

export function PreviewPanel({
  file,
  onChooseAnother,
  onRetake,
  onScan,
  isScanning,
  scanError,
  scanResult,
  editableFields,
  onFieldChange,
}: PreviewPanelProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    const url = URL.createObjectURL(file);
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPreviewUrl(url);
    return () => {
      URL.revokeObjectURL(url);
    };
  }, [file]);

  const fileDetails = useMemo(
    () => `${file.name} (${formatBytes(file.size)})`,
    [file.name, file.size],
  );

  useEffect(() => {
    const textareas = Array.from(
      document.querySelectorAll<HTMLTextAreaElement>(`.${styles.fieldInput}`),
    );
    textareas.forEach((textarea) => {
      textarea.style.height = "auto";
      textarea.style.height = `${textarea.scrollHeight}px`;
    });
  }, [editableFields]);

  const confidenceThreshold = useMemo(() => {
    return scanResult?.confidenceThreshold ?? null;
  }, [scanResult]);

  const shouldWarn =
    confidenceThreshold !== null &&
    scanResult?.ocrConfidence !== undefined &&
    scanResult.ocrConfidence < confidenceThreshold;

  const requiredFields = useMemo(
    () => [
      "firstName",
      "lastName",
      "dateOfBirth",
      "addressLine",
      "licenceNumber",
      "expiryDate",
    ],
    [],
  );

  const fieldErrors = useMemo(() => {
    const errors = new Map<string, string[]>();
    const blockingErrors = scanResult?.validation?.blockingErrors ?? [];
    blockingErrors.forEach((error) => {
      if (!error?.field || !error.message) return;
      const list = errors.get(error.field) ?? [];
      list.push(error.message);
      errors.set(error.field, list);
    });
    return errors;
  }, [scanResult]);

  const warnings = useMemo(() => {
    return scanResult?.validation?.warnings ?? [];
  }, [scanResult]);

  const hasBlockingErrors = useMemo(() => {
    return (scanResult?.validation?.blockingErrors ?? []).length > 0;
  }, [scanResult]);

  const statusLabel = useMemo(() => {
    if (isScanning) return "Scanning";
    if (scanError) return "Scan failed";
    if (scanResult) return "Scan complete";
    return "Ready to scan";
  }, [isScanning, scanError, scanResult]);

  const statusClass = useMemo(() => {
    if (isScanning) return styles.statusScanning;
    if (scanError) return styles.statusBadgeError;
    if (scanResult) return styles.statusSuccess;
    return styles.statusReady;
  }, [isScanning, scanError, scanResult]);

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2 className={styles.title}>Preview</h2>
        <p className={styles.subtitle}>
          Confirm the image is clear before scanning.
        </p>
      </div>

      <div className={styles.layout}>
        <div className={styles.column}>
          <div className={styles.photoCard}>
            <div className={styles.photoHeader}>
              <div className={styles.photoTitle}>License photo</div>
              <span className={`${styles.statusBadge} ${statusClass}`}>
                {isScanning && <span className={styles.pulseDot} />}
                {statusLabel}
              </span>
            </div>
            <div className={styles.photoBody}>
              <div
                className={styles.photoFrame}
                style={{ aspectRatio: `${UK_LICENSE_ASPECT_RATIO}` }}
              >
                {previewUrl && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={previewUrl}
                    alt="Selected driving license preview"
                    className={styles.photoImage}
                  />
                )}
              </div>
              <div className={styles.fileDetails}>{fileDetails}</div>
            </div>
          </div>
        </div>

        <div className={styles.column}>
          <div className={styles.fieldsCard}>
            <div className={styles.fieldsTitle}>Extracted fields</div>
            {shouldWarn && (
              <div className={styles.warningBanner}>
                Low confidence - please review fields
              </div>
            )}
            {warnings.length > 0 && (
              <div className={styles.warningsBox}>
                <div className={styles.warningsTitle}>Warnings</div>
                <ul className={styles.warningsList}>
                  {warnings.map((warning, index) => (
                    <li key={`${warning}-${index}`}>{warning}</li>
                  ))}
                </ul>
              </div>
            )}
            <form className={styles.formGrid}>
              {([
                ["firstName", "First name"],
                ["lastName", "Last name"],
                ["dateOfBirth", "Date of birth"],
                ["addressLine", "Address"],
                ["licenceNumber", "License number"],
                ["expiryDate", "Expiry date"],
                ["categories", "License categories"],
              ] as const).map(([key, label]) => {
                const value = editableFields[key];
                const isRequired = requiredFields.includes(key);
                const isMissing = isRequired && !value.trim();
                const errors = fieldErrors.get(key) ?? [];
                const hasError = errors.length > 0 || isMissing;
                return (
                  <label key={key} className={styles.formField}>
                    <span className={styles.fieldLabel}>{label}</span>
                    <textarea
                      rows={1}
                      value={value}
                      onChange={(event) => {
                        onFieldChange(key, event.target.value);
                      }}
                      onInput={(event) => {
                        const target = event.currentTarget;
                        target.style.height = "auto";
                        target.style.height = `${target.scrollHeight}px`;
                      }}
                      className={`${styles.fieldInput} ${
                        hasError ? styles.inputError : ""
                      }`}
                    />
                    {errors.length > 0 && (
                      <span className={styles.fieldErrorText}>{errors[0]}</span>
                    )}
                  </label>
                );
              })}
            </form>
          </div>
        </div>
      </div>

      {isScanning && (
        <div className={styles.statusInfo}>
          Scanning... Please wait.
        </div>
      )}

      {scanError && !isScanning && <div className={styles.errorBox}>{scanError}</div>}

      <div className={styles.actions}>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={onChooseAnother}
        >
          Choose another
        </button>
        <button
          type="button"
          className={styles.secondaryButton}
          onClick={onRetake}
        >
          Retake
        </button>
        <button
          type="button"
          className={styles.primaryButton}
          disabled={!scanResult || isScanning || hasBlockingErrors}
        >
          Save
        </button>
        <button
          type="button"
          className={styles.primaryButton}
          onClick={onScan}
          disabled={!file || isScanning}
        >
          {scanError ? "Retry scan" : "Scan"}
        </button>
      </div>
    </section>
  );
}
