"use client";

import { useRef, useState, type ChangeEvent } from "react";

import { CameraPanel } from "@/components/CameraPanel";
import { PreviewPanel } from "@/components/PreviewPanel";
import { UploadPanel } from "@/components/UploadPanel";
import { useCameraCapture } from "@/hooks/useCameraCapture";
import { validateImageFile } from "@/lib/imageUtils";

type ScanErrorPayload = {
    error?: {
        code?: string;
        message?: string;
    };
};

type ScanValidation = {
    blockingErrors?: Array<{ code: string; field?: string | null; message?: string }>;
    warnings?: string[];
};

type ScanFields = {
    firstName?: string | null;
    lastName?: string | null;
    dateOfBirth?: string | null;
    addressLine?: string | null;
    postcode?: string | null;
    licenceNumber?: string | null;
    expiryDate?: string | null;
    categories?: string[];
};

type ScanResult = {
    requestId: string;
    selectedEngine?: string;
    attemptedEngines?: string[];
    ocrConfidence?: number;
    processingTimeMs?: number;
    fields?: ScanFields;
    validation?: ScanValidation;
};

export default function CaptureScreen() {
    const [selectedImage, setSelectedImage] = useState<File | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [flowStep, setFlowStep] = useState<"capture" | "preview">("capture");
    const [scanResult, setScanResult] = useState<ScanResult | null>(null);
    const [scanError, setScanError] = useState<string | null>(null);
    const [isScanning, setIsScanning] = useState(false);

    const uploadInputRef = useRef<HTMLInputElement | null>(null);
    const apiBaseUrl = (
        process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || "http://localhost:8080"
    ).replace(/\/$/, "");

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
        setError(null);
        setScanResult(null);
        setScanError(null);
        setIsScanning(false);
        setFlowStep("capture");
        stopCamera();

        // allows re-selecting the same file
        if (uploadInputRef.current) {
            uploadInputRef.current.value = "";
        }
    };

    const selectImage = (file: File) => {
        setSelectedImage(file);
        setError(null);
        setScanResult(null);
        setScanError(null);
        setIsScanning(false);
        setFlowStep("preview");

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
            setError(validationError);
            setScanResult(null);
            setScanError(null);
            setIsScanning(false);
            setFlowStep("capture");
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
        setScanError(null);

        const result = await captureFrame();
        if (!result.ok) {
            setError(result.error);
            return;
        }

        handleFileSelected(result.file);
    };

    const handleScan = async () => {
        if (!selectedImage) {
            setError("Please upload or capture an image before scanning.");
            return;
        }

        setError(null);
        setScanError(null);
        setIsScanning(true);
        setScanResult(null);

        try {
            const formData = new FormData();
            formData.append("image", selectedImage);

            const response = await fetch(`${apiBaseUrl}/license/scan`, {
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
                setScanError(message);
                return;
            }

            setScanResult(payload as ScanResult);
        } catch {
            setScanError("Scan failed. Please try again.");
        } finally {
            setIsScanning(false);
        }
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

    const handleChooseAnother = () => {
        resetSelection();
    };

    const handleRetake = async () => {
        resetSelection();
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

                {flowStep === "capture" && (
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
                )}

                {flowStep === "preview" && selectedImage && (
                    <PreviewPanel
                        file={selectedImage}
                        onChooseAnother={handleChooseAnother}
                        onRetake={handleRetake}
                        onScan={handleScan}
                        isScanning={isScanning}
                        scanError={scanError}
                        scanResult={scanResult}
                    />
                )}
            </main>
        </div>
    );
}
