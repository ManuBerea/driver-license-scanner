import {useCallback, useEffect, useRef, useState, type RefObject} from "react";

import {MAX_CAPTURE_DIMENSION} from "@/lib/imageUtils";

type CameraStartResult = { ok: true } | { ok: false; error: string };
type CaptureResult = { ok: true; file: File } | { ok: false; error: string };

type CameraCaptureState = {
    videoRef: RefObject<HTMLVideoElement | null>;
    canvasRef: RefObject<HTMLCanvasElement | null>;
    isCameraActive: boolean;
    isVideoReady: boolean;
    isStartingCamera: boolean;
    startCamera: () => Promise<CameraStartResult>;
    stopCamera: () => void;
    captureFrame: () => Promise<CaptureResult>;
    markVideoReady: () => void;
};

export function useCameraCapture(): CameraCaptureState {
    const videoRef = useRef<HTMLVideoElement | null>(null);
    const canvasRef = useRef<HTMLCanvasElement | null>(null);

    const streamRef = useRef<MediaStream | null>(null);
    const mountedRef = useRef(true);
    const requestIdRef = useRef(0);

    const [isCameraActive, setIsCameraActive] = useState(false);
    const [isVideoReady, setIsVideoReady] = useState(false);
    const [isStartingCamera, setIsStartingCamera] = useState(false);

    const stopCamera = useCallback(() => {
        requestIdRef.current += 1;

        const stream = streamRef.current;
        if (stream) {
            stream.getTracks().forEach((track) => track.stop());
            streamRef.current = null;
        }

        const video = videoRef.current;
        if (video) {
            video.pause();
            video.srcObject = null;
        }

        if (mountedRef.current) {
            setIsCameraActive(false);
            setIsVideoReady(false);
            setIsStartingCamera(false);
        }
    }, []);

    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
            stopCamera();
        };
    }, [stopCamera]);

    const startCamera = useCallback(async (): Promise<CameraStartResult> => {
        if (isStartingCamera) {
            return {ok: false, error: "Camera is already starting."};
        }

        const requestId = requestIdRef.current + 1;
        requestIdRef.current = requestId;

        setIsStartingCamera(true);
        setIsVideoReady(false);

        try {
            if (!navigator.mediaDevices?.getUserMedia) {
                return {ok: false, error: "This browser does not support camera access."};
            }

            const stream = await navigator.mediaDevices.getUserMedia({
                video: {facingMode: "environment"},
                audio: false,
            });

            if (!mountedRef.current || requestId !== requestIdRef.current) {
                stream.getTracks().forEach((track) => track.stop());
                return {ok: false, error: "Camera start was canceled."};
            }

            streamRef.current = stream;

            const video = videoRef.current;
            if (!video) {
                stopCamera();
                return {ok: false, error: "Unable to attach the camera stream."};
            }

            video.srcObject = stream;
            await video.play();

            if (mountedRef.current && requestId === requestIdRef.current) {
                setIsCameraActive(true);
            }

            return {ok: true};
        } catch (err) {
            stopCamera();
            return {
                ok: false,
                error: err instanceof Error ? err.message : "Unable to access the camera.",
            };
        } finally {
            if (mountedRef.current) {
                setIsStartingCamera(false);
            }
        }
    }, [isStartingCamera, stopCamera]);

    const markVideoReady = useCallback(() => {
        const video = videoRef.current;
        if (
            streamRef.current &&
            mountedRef.current &&
            video &&
            video.videoWidth &&
            video.videoHeight
        ) {
            setIsVideoReady(true);
        }
    }, []);

    const captureFrame = useCallback(async (): Promise<CaptureResult> => {
        const video = videoRef.current;
        const canvas = canvasRef.current;

        if (!video || !canvas) {
            return {ok: false, error: "Camera is not ready yet. Please try again."};
        }

        if (!video.videoWidth || !video.videoHeight) {
            return {ok: false, error: "Camera is still starting. Please wait a moment."};
        }

        const sourceWidth = video.videoWidth;
        const sourceHeight = video.videoHeight;

        const maxDimension = Math.max(sourceWidth, sourceHeight);
        const scale = Math.min(1, MAX_CAPTURE_DIMENSION / maxDimension);

        const targetWidth = Math.max(1, Math.round(sourceWidth * scale));
        const targetHeight = Math.max(1, Math.round(sourceHeight * scale));

        canvas.width = targetWidth;
        canvas.height = targetHeight;

        const ctx = canvas.getContext("2d");
        if (!ctx) {
            return {ok: false, error: "Unable to capture the frame."};
        }

        ctx.drawImage(video, 0, 0, targetWidth, targetHeight);

        const blob = await new Promise<Blob | null>((resolve) =>
            canvas.toBlob(resolve, "image/jpeg", 0.9),
        );

        if (!blob) {
            return {ok: false, error: "Failed to encode the captured image."};
        }

        const file = new File([blob], `capture-${Date.now()}.jpg`, {type: "image/jpeg"});
        return {ok: true, file};
    }, []);

    return {
        videoRef,
        canvasRef,
        isCameraActive,
        isVideoReady,
        isStartingCamera,
        startCamera,
        stopCamera,
        captureFrame,
        markVideoReady,
    };
}
