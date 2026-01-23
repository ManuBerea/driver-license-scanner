"use client";

import {useRef, useState, type ChangeEvent} from "react";

import {CameraPanel} from "@/components/CameraPanel";
import {UploadPanel} from "@/components/UploadPanel";
import {useCameraCapture} from "@/hooks/useCameraCapture";
import {validateImageFile} from "@/lib/imageUtils";

export default function CaptureScreen() {
    const [selectedImage, setSelectedImage] = useState<File | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [hasContinued, setHasContinued] = useState(false);

    const uploadInputRef = useRef<HTMLInputElement | null>(null);

    const {
        videoRef,
        canvasRef,
        isCameraActive,
        isVideoReady,
        isStartingCamera,
        startCamera,
        stopCamera,
        captureFrame,
        markVideoReady,
    } = useCameraCapture();

    const resetSelection = () => {
        setSelectedImage(null);
        setHasContinued(false);
        setError(null);

        // allows re-selecting the same file
        if (uploadInputRef.current) {
            uploadInputRef.current.value = "";
        }
    };

    const selectImage = (file: File) => {
        setSelectedImage(file);
        setHasContinued(false);
        setError(null);

        // If we captured from camera, close it to avoid leaving it running
        if (isCameraActive) {
            stopCamera();
        }
        if (uploadInputRef.current) {
            uploadInputRef.current.value = "";
        }
    };

    const handleFileSelected = (file: File) => {
        const validationError = validateImageFile(file);
        if (validationError) {
            setSelectedImage(null);
            setHasContinued(false);
            setError(validationError);
            if (uploadInputRef.current) {
                uploadInputRef.current.value = "";
            }
            return;
        }

        selectImage(file);
    };

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;
        handleFileSelected(file);
    };

    const handleCapture = async () => {
        setError(null);

        const result = await captureFrame();
        if (!result.ok) {
            setError(result.error);
            return;
        }

        handleFileSelected(result.file);
    };

    const handleContinue = () => {
        if (!selectedImage) {
            setError("Please upload or capture an image before continuing.");
            return;
        }
        setHasContinued(true);
    };

    const handleStartStop = async () => {
        setError(null);

        if (isCameraActive) {
            stopCamera();
            return;
        }

        const result = await startCamera();
        if (!result.ok && result.error !== "Camera start was canceled.") {
            setError(result.error);
        }
    };

    return (
        <div
            className="relative min-h-screen overflow-hidden bg-gradient-to-br from-amber-50 via-white to-sky-50 text-slate-900">
            <div
                className="pointer-events-none absolute -top-24 right-0 h-64 w-64 rounded-full bg-amber-200/40 blur-3xl"/>
            <div
                className="pointer-events-none absolute -bottom-24 left-0 h-64 w-64 rounded-full bg-sky-200/40 blur-3xl"/>

            <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-10 md:px-10">
                <header className="flex flex-col gap-2">
                    <h1 className="text-3xl font-bold tracking-tight text-slate-900">
                        Capture your UK driving license
                    </h1>
                    <p className="max-w-2xl text-sm text-slate-600">
                        Upload a photo or capture a frame using your camera.
                    </p>
                </header>

                {error && (
                    <div className="rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-900">
                        {error}
                    </div>
                )}

                <div className="grid gap-6 md:grid-cols-2">
                    <UploadPanel
                        selectedImage={selectedImage}
                        onFileChange={handleFileChange}
                        inputRef={uploadInputRef}
                        onClearSelection={resetSelection}
                        onFileSelect={handleFileSelected}
                    />

                    <CameraPanel
                        isCameraActive={isCameraActive}
                        isVideoReady={isVideoReady}
                        isStartingCamera={isStartingCamera}
                        onStartStop={handleStartStop}
                        onCapture={handleCapture}
                        videoRef={videoRef}
                        canvasRef={canvasRef}
                        onVideoReady={markVideoReady}
                    />
                </div>

                <div
                    className="flex flex-col items-start gap-3 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
                    <button
                        type="button"
                        className="rounded-full bg-emerald-600 px-6 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-emerald-300"
                        onClick={handleContinue}
                        disabled={!selectedImage}
                    >
                        Continue
                    </button>

                    {hasContinued && selectedImage && (
                        <div className="text-sm text-slate-700">
                            Ready to continue with:{" "}
                            <span className="font-semibold">{selectedImage.name}</span>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}
