import type { RefObject } from "react";

import { UK_LICENSE_ASPECT_RATIO } from "@/lib/imageUtils";
import styles from "@/components/capture/CameraPanel.module.css";

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
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2 className={styles.heading}>Use your camera</h2>
        <p className={styles.subheading}>
          Capture a clear image of the license inside the frame.
        </p>
      </div>

      <div className={styles.videoShell}>
        <div className={styles.videoAspect} style={{ aspectRatio: UK_LICENSE_ASPECT_RATIO }}>
          <video
            ref={videoRef}
            onLoadedMetadata={onVideoReady}
            onCanPlay={onVideoReady}
            className={styles.video}
            playsInline
            muted
          />
          <div className={styles.frameOverlay} />
          {!isCameraActive && (
            <div className={styles.cameraPlaceholder}>
              {isStartingCamera
                ? "Starting camera..."
                : "Camera is off. Press Start camera to begin."}
            </div>
          )}
        </div>
      </div>

      <div className={styles.buttonRow}>
        <button
          type="button"
          className={styles.toggleButton}
          onClick={onStartStop}
          disabled={isStartingCamera}
        >
          {isCameraActive ? "Stop camera" : "Start camera"}
        </button>

        <button
          type="button"
          className={styles.captureButton}
          onClick={onCapture}
          disabled={!isCameraActive || !isVideoReady || isStartingCamera}
        >
          Capture frame
        </button>
      </div>

      <canvas ref={canvasRef} className={styles.hiddenCanvas} />
    </section>
  );
}
