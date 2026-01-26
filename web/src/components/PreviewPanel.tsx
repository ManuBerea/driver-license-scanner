"use client";

import {useEffect, useMemo, useState} from "react";

import { UK_LICENSE_ASPECT_RATIO, formatBytes } from "@/lib/imageUtils";

type PreviewPanelProps = {
  file: File;
  onChooseAnother: () => void;
  onRetake: () => void;
  onScan: () => void;
  isScanning: boolean;
  scanError: string | null;
  scanResult: {
    requestId: string;
    selectedEngine?: string;
    attemptedEngines?: string[];
    ocrConfidence?: number;
    processingTimeMs?: number;
    fields?: {
      firstName?: string | null;
      lastName?: string | null;
      dateOfBirth?: string | null;
      addressLine?: string | null;
      postcode?: string | null;
      licenceNumber?: string | null;
      expiryDate?: string | null;
      categories?: string[];
    };
    validation?: {
      blockingErrors?: Array<{ code: string; field?: string | null; message?: string }>;
      warnings?: string[];
    };
  } | null;
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
    if (!scanResult) return null;
    const fields = scanResult.fields;
    const addressLine = fields?.addressLine ?? "";
    const postcode = fields?.postcode ?? "";
    const addressValue =
      addressLine || postcode
        ? [addressLine, postcode].filter(Boolean).join(", ")
        : "—";
    const categoriesValue =
      fields?.categories && fields.categories.length > 0
        ? fields.categories.join(", ")
        : "—";

    return [
      ["First name", fields?.firstName ?? "—"],
      ["Last name", fields?.lastName ?? "—"],
      ["Date of birth", fields?.dateOfBirth ?? "—"],
      ["Address", addressValue],
      ["Licence number", fields?.licenceNumber ?? "—"],
      ["Expiry date", fields?.expiryDate ?? "—"],
      ["Licence categories", categoriesValue],
    ];
  }, [scanResult]);

  return (
    <section className="flex flex-col gap-6 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
      <div className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold text-slate-900">Preview</h2>
        <p className="text-sm text-slate-600">
          Confirm the image is clear before scanning.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <div className="flex flex-col gap-4">
          <div
            className="relative w-full overflow-hidden rounded-2xl border border-slate-200 bg-slate-50"
            style={{ aspectRatio: `${UK_LICENSE_ASPECT_RATIO}` }}
          >
            {previewUrl && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={previewUrl}
                alt="Selected driving licence preview"
                className="absolute inset-0 h-full w-full object-contain"
              />
            )}
          </div>
          <div className="text-sm text-slate-600">{fileDetails}</div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
            <div className="mb-3 text-sm font-semibold">Scan results</div>
            <dl className="grid gap-2 sm:grid-cols-2">
              {scanFields?.map(([label, value]) => (
                <div key={label} className="rounded-lg bg-white/80 px-3 py-2">
                  <dt className="text-xs uppercase tracking-wide text-emerald-900/70">
                    {label}
                  </dt>
                  <dd className="text-sm font-medium text-emerald-950">
                    {value}
                  </dd>
                </div>
              ))}
            </dl>
            {!scanFields && !isScanning && !scanError && (
              <div className="text-sm text-emerald-900/80">
                Run a scan to see extracted fields here.
              </div>
            )}
          </div>

          {isScanning && (
            <div className="rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">
              Scanning… Please wait.
            </div>
          )}

          {scanError && !isScanning && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-900">
              {scanError}
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-900 shadow-sm transition hover:bg-slate-50"
          onClick={onChooseAnother}
        >
          Choose another
        </button>
        <button
          type="button"
          className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-900 shadow-sm transition hover:bg-slate-50"
          onClick={onRetake}
        >
          Retake
        </button>
        <button
          type="button"
          className="rounded-full bg-emerald-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-emerald-300"
          onClick={onScan}
          disabled={!file || isScanning}
        >
          {scanError ? "Retry scan" : "Scan"}
        </button>
      </div>
    </section>
  );
}
