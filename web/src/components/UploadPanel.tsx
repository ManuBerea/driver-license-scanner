import { useState, type ChangeEventHandler, type DragEvent, type RefObject } from "react";

import { formatBytes } from "@/lib/imageUtils";

type UploadPanelProps = {
    selectedImage: File | null;
    onFileChange: ChangeEventHandler<HTMLInputElement>;
    inputRef: RefObject<HTMLInputElement | null>;
    onClearSelection: () => void;
    onFileSelect: (file: File) => void;
};

export function UploadPanel({
    selectedImage,
    onFileChange,
    inputRef,
    onClearSelection,
    onFileSelect,
}: UploadPanelProps) {
    const [isDragging, setIsDragging] = useState(false);

    const handleDragOver = (event: DragEvent<HTMLLabelElement>) => {
        event.preventDefault();
        event.dataTransfer.dropEffect = "copy";
        setIsDragging(true);
    };

    const handleDragLeave = () => {
        setIsDragging(false);
    };

    const handleDrop = (event: DragEvent<HTMLLabelElement>) => {
        event.preventDefault();
        setIsDragging(false);
        const file = event.dataTransfer.files?.[0];
        if (file) {
            onFileSelect(file);
        }
    };

    return (
        <section className="flex h-full flex-col gap-5 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-2">
                <h2 className="text-lg font-semibold text-slate-900">Upload an image</h2>
                <p className="text-sm text-slate-600">
                    Drag and drop a clear photo of the front of the UK license.
                </p>
            </div>

            <label
                className={`flex min-h-[180px] cursor-pointer flex-col justify-center gap-3 rounded-2xl border border-dashed px-6 py-6 transition ${
                    isDragging
                        ? "border-emerald-400 bg-emerald-50"
                        : "border-slate-300 bg-slate-50 hover:border-slate-400"
                }`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
            >
                <span className="text-sm font-medium text-slate-700">
                    {isDragging ? "Drop the image here" : "Drag & drop or click to browse"}
                </span>
                <span className="text-xs text-slate-500">
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