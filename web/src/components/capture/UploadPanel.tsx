"use client";

import { useState, type ChangeEventHandler, type DragEvent, type RefObject } from "react";

import { formatBytes } from "@/lib/imageUtils";
import styles from "@/components/capture/UploadPanel.module.css";

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
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2 className={styles.heading}>Upload an image</h2>
        <p className={styles.subheading}>
          Drag and drop a clear photo of the front of the UK license.
        </p>
      </div>

      <label
        className={`${styles.dropzone} ${
          isDragging ? styles.dropzoneActive : styles.dropzoneIdle
        }`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <span className={styles.dropzoneTitle}>
          {isDragging ? "Drop the image here" : "Drag & drop or click to browse"}
        </span>
        <span className={styles.dropzoneNote}>
          Supported formats: JPG, PNG, WEBP (max 10MB)
        </span>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
          className={styles.fileInput}
          onChange={onFileChange}
        />
      </label>

      {selectedImage ? (
        <div className={styles.selectedBox}>
          <div className={styles.selectedRow}>
            <div>
              <div className={styles.selectedTitle}>Selected image</div>
              <div className={styles.selectedMeta}>
                {selectedImage.name} ({formatBytes(selectedImage.size)})
              </div>
            </div>

            <button
              type="button"
              className={styles.clearButton}
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
