import type { RefObject } from "react";

import { UK_LICENSE_ASPECT_RATIO } from "@/lib/imageUtils";

type CameraPanelProps = {
    isCameraActive: boolean;
    isVideoReady: boolean;
    isStartingCamera: boolean;
    onStartStop: () => void;
    onCapture: () => void;
    videoRef: RefObject<HTMLVideoElement | null>;
    canvasRef: RefObject<HTMLCanvasElement | null>;
    onVideoReady: () => void;
};

export function CameraPanel({
                                isCameraActive,
                                isVideoReady,
                                isStartingCamera,
                                onStartStop,
                                onCapture,
                                videoRef,
                                canvasRef,
                                onVideoReady,
                            }: CameraPanelProps) {
    return (
        <section className="flex h-full flex-col gap-5 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-2">
                <h2 className="text-lg font-semibold text-slate-900">Use your camera</h2>
                <p className="text-sm text-slate-600">
                    Capture a clear image of the license inside the frame.
                </p>
            </div>

            <div className="relative w-full overflow-hidden rounded-2xl border border-slate-200 bg-slate-900">
                <div className="relative w-full" style={{ aspectRatio: UK_LICENSE_ASPECT_RATIO }}>
                    <video
                        ref={videoRef}
                        onLoadedMetadata={onVideoReady}
                        className="h-full w-full object-cover"
                        playsInline
                        muted
                    />
                    {!isCameraActive && (
                        <div className="absolute inset-0 grid place-items-center px-6 text-center text-sm text-slate-200">
                            {isStartingCamera
                                ? "Starting camera..."
                                : "Camera is off. Press Start camera to begin."}
                        </div>
                    )}
                </div>
            </div>

            <div className="flex flex-wrap gap-3">
                <button
                    type="button"
                    className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-900 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={onStartStop}
                    disabled={isStartingCamera}
                >
                    {isCameraActive ? "Stop camera" : "Start camera"}
                </button>

                <button
                    type="button"
                    className="rounded-full bg-slate-900 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                    onClick={onCapture}
                    disabled={!isCameraActive || !isVideoReady || isStartingCamera}
                >
                    Capture frame
                </button>
            </div>

            <canvas ref={canvasRef} className="hidden" />
        </section>
    );
}
