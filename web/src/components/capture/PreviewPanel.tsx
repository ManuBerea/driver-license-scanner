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
}: PreviewPanelProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [editableFields, setEditableFields] = useState<EditableFields>({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    addressLine: "",
    licenceNumber: "",
    expiryDate: "",
    categories: "",
  });

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
    const fields = scanResult?.fields;
    setEditableFields({
      firstName: fields?.firstName ?? "",
      lastName: fields?.lastName ?? "",
      dateOfBirth: fields?.dateOfBirth ?? "",
      addressLine: fields?.addressLine ?? "",
      licenceNumber: fields?.licenceNumber ?? "",
      expiryDate: fields?.expiryDate ?? "",
      categories: fields?.categories?.join(", ") ?? "",
    });
  }, [scanResult]);

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
                return (
                  <label key={key} className={styles.formField}>
                    <span className={styles.fieldLabel}>{label}</span>
                    <textarea
                      rows={1}
                      value={value}
                      onChange={(event) => {
                        setEditableFields((prev) => ({
                          ...prev,
                          [key]: event.target.value,
                        }));
                      }}
                      onInput={(event) => {
                        const target = event.currentTarget;
                        target.style.height = "auto";
                        target.style.height = `${target.scrollHeight}px`;
                      }}
                      className={`${styles.fieldInput} ${
                        isMissing ? styles.inputError : ""
                      }`}
                    />
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
          onClick={onScan}
          disabled={!file || isScanning}
        >
          {scanError ? "Retry scan" : "Scan"}
        </button>
      </div>
    </section>
  );
}
