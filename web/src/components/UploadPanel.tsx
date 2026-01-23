import type { ChangeEventHandler, RefObject } from "react";

import { formatBytes } from "@/lib/imageUtils";

type UploadPanelProps = {
    selectedImage: File | null;
    onFileChange: ChangeEventHandler<HTMLInputElement>;
    inputRef: RefObject<HTMLInputElement | null>;
    onClearSelection: () => void;
};

export function UploadPanel({
                                selectedImage,
                                onFileChange,
                                inputRef,
                                onClearSelection,
                            }: UploadPanelProps) {
    return (
        <section className="flex h-full flex-col gap-5 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-2">
                <h2 className="text-lg font-semibold text-slate-900">Upload an image</h2>
                <p className="text-sm text-slate-600">
                    Choose a clear, well-lit photo of the front of the UK license.
                </p>
            </div>

            <label className="flex flex-col gap-2">
        <span className="text-sm font-medium text-slate-700">
          Supported formats: JPG, PNG, WEBP (max 10MB)
        </span>
                <input
                    ref={inputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                    className="text-sm file:mr-4 file:rounded-full file:border-0 file:bg-slate-900 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-slate-800"
                    onChange={onFileChange}
                />
            </label>

            {selectedImage ? (
                <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
                    <div className="flex items-start justify-between gap-3">
                        <div>
                            <div className="font-semibold">Selected image</div>
                            <div className="text-emerald-950/80">
                                {selectedImage.name} ({formatBytes(selectedImage.size)})
                            </div>
                        </div>

                        <button
                            type="button"
                            className="rounded-full border border-emerald-300 bg-white px-3 py-1 text-xs font-semibold text-emerald-900 shadow-sm transition hover:bg-emerald-100"
                            onClick={onClearSelection}
                        >
                            Clear
                        </button>
                    </div>
                </div>
            ) : null}
        </section>
    );
}
