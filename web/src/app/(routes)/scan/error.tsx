"use client";

type ScanErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function ScanError({ error, reset }: ScanErrorProps) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-6 text-center text-slate-700">
      <h2 className="text-lg font-semibold text-slate-900">
        Something went wrong
      </h2>
      <p className="text-sm text-slate-600">
        {error.message || "Unable to load the scan screen."}
      </p>
      <button
        type="button"
        className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-900 shadow-sm transition hover:bg-slate-50"
        onClick={reset}
      >
        Try again
      </button>
    </div>
  );
}
