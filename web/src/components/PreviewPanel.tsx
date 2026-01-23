"use client";

import {useEffect, useMemo, useState} from "react";

import { UK_LICENSE_ASPECT_RATIO, formatBytes } from "@/lib/imageUtils";

type PreviewPanelProps = {
  file: File;
  onChooseAnother: () => void;
  onRetake: () => void;
  onScan: () => void;
  scanRequested: boolean;
};

export function PreviewPanel({
  file,
  onChooseAnother,
  onRetake,
  onScan,
  scanRequested,
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

  return (
    <section className="flex flex-col gap-5 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
      <div className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold text-slate-900">Preview</h2>
        <p className="text-sm text-slate-600">
          Confirm the image is clear before scanning.
        </p>
      </div>

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
          disabled={!file}
        >
          Scan
        </button>
      </div>

      {scanRequested && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
          Scan requested. This is a placeholder until the API is wired.
        </div>
      )}
    </section>
  );
}
