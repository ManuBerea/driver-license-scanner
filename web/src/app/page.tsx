'use client';

import Image from 'next/image';
import { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';

type CaptureMode = 'upload' | 'camera';

const MAX_FILE_BYTES = 10 * 1024 * 1024; // 10MB upper bound per guardrail
const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

interface SelectedImage {
  file: File;
  previewUrl: string;
}

export default function Home() {
  const [mode, setMode] = useState<CaptureMode>('upload');
  const [selectedImage, setSelectedImage] = useState<SelectedImage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [isCapturing, setIsCapturing] = useState(false);
  const [hasContinued, setHasContinued] = useState(false);
  const [isCameraReady, setIsCameraReady] = useState(false);

  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const modeRef = useRef<CaptureMode>(mode);
  const cameraRequestIdRef = useRef(0);

  // Cleanup preview URLs to avoid leaks
  useEffect(() => {
    return () => {
      if (selectedImage?.previewUrl) {
        URL.revokeObjectURL(selectedImage.previewUrl);
      }
    };
  }, [selectedImage]);

  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

  const validateFile = (file: File): string | null => {
    if (!ALLOWED_MIME_TYPES.has(file.type)) {
      return 'Please select a JPG, JPEG, PNG, or WEBP image.';
    }

    if (file.size > MAX_FILE_BYTES) {
      return 'Image is too large. Please keep files under 10MB.';
    }

    return null;
  };

  const resetSelection = (options?: { clearError?: boolean }) => {
    setSelectedImage(null);
    if (options?.clearError !== false) {
      setError(null);
    }
    setHasContinued(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    const validationError = validateFile(file);
    if (validationError) {
      setError(validationError);
      resetSelection({ clearError: false });
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setSelectedImage({ file, previewUrl });
    setError(null);
    setHasContinued(false);
  };

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsCameraReady(false);
  }, []);

  const startCamera = useCallback(async () => {
    try {
      setCameraError(null);
      if (streamRef.current) {
        stopCamera();
      }
      setIsCameraReady(false);
      const requestId = cameraRequestIdRef.current + 1;
      cameraRequestIdRef.current = requestId;
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      if (cameraRequestIdRef.current !== requestId || modeRef.current !== 'camera') {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      console.error(err);
      setCameraError('Unable to access camera. Please allow permissions or use file upload.');
    }
  }, [stopCamera]);

  useEffect(() => {
    if (mode === 'camera') {
      startCamera();
    } else {
      stopCamera();
    }

    return () => {
      stopCamera();
    };
  }, [mode, startCamera, stopCamera]);

  const capturePhoto = async () => {
    if (!videoRef.current) {
      setError('Camera is not ready yet.');
      return;
    }
    if (!videoRef.current.videoWidth || !videoRef.current.videoHeight) {
      setError('Camera is still loading. Please try again in a moment.');
      return;
    }

    setIsCapturing(true);
    try {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth;
      canvas.height = videoRef.current.videoHeight;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        throw new Error('Unable to capture frame.');
      }

      ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);

      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.92));
      if (!blob) {
        throw new Error('Failed to capture image.');
      }

      const file = new File([blob], `webcam-${Date.now()}.jpg`, { type: blob.type || 'image/jpeg' });
      const validationError = validateFile(file);
      if (validationError) {
        setError(validationError);
        resetSelection({ clearError: false });
        return;
      }

      const previewUrl = URL.createObjectURL(file);
      setSelectedImage({ file, previewUrl });
      setError(null);
      setHasContinued(false);
      stopCamera();
    } catch (captureError) {
      console.error(captureError);
      setError('Unable to capture photo. Please try again or upload a file.');
    } finally {
      setIsCapturing(false);
    }
  };

  const selectedFileLabel = useMemo(() => {
    if (!selectedImage) {
      return 'No image selected yet.';
    }

    const sizeInMb = (selectedImage.file.size / (1024 * 1024)).toFixed(2);
    return `${selectedImage.file.name} (${sizeInMb} MB)`;
  }, [selectedImage]);

  const handleContinue = () => {
    if (!selectedImage) {
      return;
    }

    setHasContinued(true);
  };

  return (
    <div className="min-h-screen bg-zinc-50 px-4 py-10 text-zinc-900">
      <main className="mx-auto max-w-4xl rounded-xl bg-white p-8 shadow-sm">
        <header className="space-y-3">
          <p className="text-sm font-medium text-indigo-600">Step 1 - Capture or Upload</p>
          <h1 className="text-3xl font-semibold tracking-tight">Add a UK driving licence image</h1>
          <p className="text-sm text-zinc-600">
            Choose the capture method that works best for you. Images stay in memory only and are never stored.
          </p>
        </header>

        <section className="mt-8">
          <div className="inline-flex rounded-full border border-zinc-200 bg-zinc-100 p-1 text-sm font-medium">
            {(['upload', 'camera'] as CaptureMode[]).map((captureMode) => (
              <button
                key={captureMode}
                type="button"
                onClick={() => setMode(captureMode)}
                className={`rounded-full px-4 py-1 transition ${
                  mode === captureMode ? 'bg-white text-indigo-600 shadow-sm' : 'text-zinc-600'
                }`}
              >
                {captureMode === 'upload' ? 'Upload image' : 'Use webcam'}
              </button>
            ))}
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <div className="rounded-lg border border-dashed border-zinc-300 bg-zinc-50 p-6">
              {mode === 'upload' ? (
                <div className="space-y-4">
                  <p className="text-sm text-zinc-600">
                    Supported formats: JPG, JPEG, PNG, WEBP - Max size: 10MB
                  </p>
                  <label className="flex cursor-pointer flex-col items-center justify-center rounded-lg border border-zinc-200 bg-white p-6 text-center hover:border-indigo-300">
                    <span className="text-base font-medium text-indigo-600">Choose a licence image</span>
                    <span className="text-xs text-zinc-500">Drag & drop or browse files</span>
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={handleFileChange}
                      ref={fileInputRef}
                    />
                  </label>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="aspect-video w-full overflow-hidden rounded-lg border border-zinc-200 bg-black">
                    {cameraError ? (
                      <div className="flex h-full items-center justify-center px-4 text-center text-sm text-red-500">
                        {cameraError}
                      </div>
                    ) : (
                      <video
                        ref={videoRef}
                        autoPlay
                        playsInline
                        muted
                        onLoadedMetadata={() => setIsCameraReady(true)}
                        className="h-full w-full object-cover"
                      />
                    )}
                  </div>
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={capturePhoto}
                      disabled={Boolean(cameraError) || isCapturing || !isCameraReady}
                      className="flex-1 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
                    >
                      {isCapturing ? 'Capturing...' : 'Capture photo'}
                    </button>
                    <button
                      type="button"
                      onClick={startCamera}
                      className="rounded-md bg-white px-4 py-2 text-sm font-medium text-zinc-700 shadow-sm ring-1 ring-inset ring-zinc-200 hover:bg-zinc-50"
                    >
                      Restart camera
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="rounded-lg border border-zinc-200 bg-white p-6">
              <h2 className="text-lg font-semibold">Selected image</h2>
              <p className="mt-1 text-sm text-zinc-600">{selectedFileLabel}</p>

              {selectedImage?.previewUrl && (
                <div className="mt-4 aspect-video w-full overflow-hidden rounded-lg border border-zinc-100 bg-black">
                  <Image
                    src={selectedImage.previewUrl}
                    alt="Selected preview"
                    width={640}
                    height={360}
                    className="h-full w-full object-cover"
                    sizes="(max-width: 1024px) 100vw, 640px"
                    unoptimized
                  />
                </div>
              )}

              {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

              <div className="mt-6 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={handleContinue}
                  disabled={!selectedImage}
                  className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:bg-indigo-300"
                >
                  Continue
                </button>
                <button
                  type="button"
                  onClick={() => resetSelection()}
                  className="rounded-md bg-white px-4 py-2 text-sm font-medium text-zinc-700 shadow-sm ring-1 ring-inset ring-zinc-200 hover:bg-zinc-50"
                >
                  Clear selection
                </button>
              </div>

              {hasContinued && selectedImage && (
                <div className="mt-4 rounded-md border border-green-200 bg-green-50 p-3 text-sm text-green-700">
                  Image captured successfully. Preview & scan flow will continue in the next step.
                </div>
              )}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
