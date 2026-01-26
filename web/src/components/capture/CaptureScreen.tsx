"use client";

import { useRef, useState, type ChangeEvent } from "react";

import { CameraPanel } from "@/components/capture/CameraPanel";
import { PreviewPanel } from "@/components/capture/PreviewPanel";
import { UploadPanel } from "@/components/capture/UploadPanel";
import { useCameraCapture } from "@/hooks/useCameraCapture";
import { scanLicense } from "@/lib/scanApi";
import { validateImageFile } from "@/lib/imageUtils";
import type { ScanResult } from "@/types/scan";
import styles from "@/components/capture/CaptureScreen.module.css";

type FlowStep = "capture" | "preview";

export default function CaptureScreen() {
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [flowStep, setFlowStep] = useState<FlowStep>("capture");
  const [scanResult, setScanResult] = useState<ScanResult | null>(null);
  const [scanError, setScanError] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);

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
    setError(null);
    setScanResult(null);
    setScanError(null);
    setIsScanning(false);
    setFlowStep("capture");
    stopCamera();

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

    const response = await scanLicense(selectedImage);
    if (!response.ok) {
      setScanError(response.message);
      setIsScanning(false);
      return;
    }

    setScanResult(response.data);
    setIsScanning(false);
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

  const stepLabel =
    flowStep === "capture" ? "Step 1 of 2 - Capture" : "Step 2 of 2 - Preview";

  return (
    <div className={styles.page}>
      <div className={styles.orbTop} />
      <div className={styles.orbBottom} />

      <main className={styles.main}>
        <header className={styles.header}>
          <span className={styles.stepBadge}>{stepLabel}</span>
          <h1 className={styles.title}>Capture your UK driving license</h1>
          <p className={styles.subtitle}>
            Upload a photo or capture a frame using your camera.
          </p>
        </header>

        {error && <div className={styles.error}>{error}</div>}

        {flowStep === "capture" && (
          <div className={styles.captureGrid}>
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
