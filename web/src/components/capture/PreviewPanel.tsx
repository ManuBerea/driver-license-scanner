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

  const scanFields = useMemo(() => {
    const fields = scanResult?.fields;
    const addressLine = fields?.addressLine ?? "";
    const postcode = fields?.postcode ?? "";
    const addressValue =
      addressLine || postcode
        ? [addressLine, postcode].filter(Boolean).join(", ")
        : "--";
    const categoriesValue =
      fields?.categories && fields.categories.length > 0
        ? fields.categories.join(", ")
        : "--";

    return [
      ["First name", fields?.firstName ?? "--"],
      ["Last name", fields?.lastName ?? "--"],
      ["Date of birth", fields?.dateOfBirth ?? "--"],
      ["Address", addressValue],
      ["License number", fields?.licenceNumber ?? "--"],
      ["Expiry date", fields?.expiryDate ?? "--"],
      ["License categories", categoriesValue],
    ];
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
            <dl className={styles.fieldsGrid}>
              {scanFields.map(([label, value]) => (
                <div key={label} className={styles.fieldItem}>
                  <dt className={styles.fieldLabel}>{label}</dt>
                  <dd className={styles.fieldValue}>{value}</dd>
                </div>
              ))}
            </dl>
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
