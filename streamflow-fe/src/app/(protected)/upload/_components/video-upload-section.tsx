"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useTRPC } from "@/trpc/client";
import { getApiBaseUrl } from "@/lib/api/client";
import {
  TERMINAL_UPLOAD_STATUSES,
  TERMINAL_PROCESSING_STATUSES,
} from "@/modules/content/schema";
import type { VideoUploadResponse } from "@/modules/content/schema";
import { toast } from "sonner";
import { motion } from "framer-motion";
import {
  Upload,
  FileVideo,
  X,
  HardDrive,
  Loader2,
  CheckCircle2,
  XCircle,
  Cog,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ContentDetails } from "./use-movie-generation";

// ── Constants ────────────────────────────────────────────────────────────────

const POLL_INTERVAL_MS = 10_000;

const ACCEPTED_VIDEO_TYPES = [
  "video/mp4",
  "video/x-matroska",
  "video/quicktime",
  "video/x-msvideo",
  "video/webm",
];

// ── Types ────────────────────────────────────────────────────────────────────

interface VideoUploadSectionProps {
  contentId: string;
  contentDetails: ContentDetails;
}

type Phase = "pick" | "uploading" | "processing" | "complete" | "error";

// ── Helpers ──────────────────────────────────────────────────────────────────

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024)
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

async function uploadVideoToBackend(
  contentId: string,
  file: File,
): Promise<VideoUploadResponse> {
  const base = getApiBaseUrl();
  const formData = new FormData();
  formData.append("video", file);

  const res = await fetch(`${base}/api/content/${contentId}/video`, {
    method: "POST",
    body: formData,
  });

  if (!res.ok) {
    const text = await res.text();
    let message = `Upload failed (${res.status})`;
    try {
      const body = JSON.parse(text);
      message = body.message || body.error || message;
    } catch {
      if (text) message = text;
    }
    throw new Error(message);
  }

  return res.json();
}

// ── Component ────────────────────────────────────────────────────────────────

export function VideoUploadSection({
  contentId,
  contentDetails,
}: VideoUploadSectionProps) {
  const trpc = useTRPC();

  const [phase, setPhase] = useState<Phase>("pick");
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const hasTriggeredUpload = useRef(false);
  const hasTriggeredProcessing = useRef(false);
  const uploadTerminalHandled = useRef(false);
  const processingTerminalHandled = useRef(false);

  // ── 1. Upload mutation (direct POST to Java backend) ──────────────────────

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadVideoToBackend(contentId, file),
    onSuccess: (data) => {
      if (data.uploadStatus === "COMPLETED") {
        uploadTerminalHandled.current = true;
        setPhase("processing");
      } else if (
        data.uploadStatus === "FAILED" ||
        data.uploadStatus === "CANCELLED"
      ) {
        uploadTerminalHandled.current = true;
        const msg = data.errorMessage ?? "Upload failed";
        setPhase("error");
        setErrorMessage(msg);
        toast.error(msg);
      }
    },
    onError: (err: Error) => {
      setPhase("error");
      setErrorMessage(err.message);
      toast.error(err.message || "Failed to start video upload");
    },
  });

  // ── 2. Poll upload status every 10 s ──────────────────────────────────────

  const shouldPollUpload = phase === "uploading" && !uploadMutation.isPending;

  const uploadStatusQuery = useQuery({
    ...trpc.content.getUploadStatus.queryOptions({ contentId }),
    enabled: shouldPollUpload,
    refetchInterval: (query) => {
      const status = query.state.data?.uploadStatus;
      if (status && TERMINAL_UPLOAD_STATUSES.has(status)) return false;
      return POLL_INTERVAL_MS;
    },
  });

  const latestUploadData =
    uploadStatusQuery.data ?? uploadMutation.data ?? null;
  const uploadProgress = latestUploadData?.progressPercent ?? null;

  useEffect(() => {
    if (phase !== "uploading" || uploadTerminalHandled.current) return;
    const status = latestUploadData?.uploadStatus;
    if (!status || !TERMINAL_UPLOAD_STATUSES.has(status)) return;

    uploadTerminalHandled.current = true;

    if (status === "COMPLETED") {
      setPhase("processing");
    } else {
      const msg =
        latestUploadData?.errorMessage ??
        (status === "CANCELLED" ? "Upload was cancelled" : "Upload failed");
      setPhase("error");
      setErrorMessage(msg);
      toast.error(msg);
    }
  }, [phase, latestUploadData]);

  // ── 3. Auto-trigger video processing once upload completes ────────────────

  const triggerProcessingMutation = useMutation(
    trpc.content.triggerProcessing.mutationOptions({
      onError: (err) => {
        setPhase("error");
        setErrorMessage(err.message);
        toast.error(err.message || "Failed to start video processing");
      },
    }),
  );

  useEffect(() => {
    if (phase !== "processing" || hasTriggeredProcessing.current) return;
    hasTriggeredProcessing.current = true;
    triggerProcessingMutation.mutate({ contentId });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, contentId]);

  // ── 4. Poll processing status every 10 s ──────────────────────────────────

  const shouldPollProcessing =
    phase === "processing" && !triggerProcessingMutation.isPending;

  const processingStatusQuery = useQuery({
    ...trpc.content.getProcessingStatus.queryOptions({ contentId }),
    enabled: shouldPollProcessing,
    refetchInterval: (query) => {
      const status = query.state.data?.processingStatus;
      if (status && TERMINAL_PROCESSING_STATUSES.has(status)) return false;
      return POLL_INTERVAL_MS;
    },
  });

  const latestProcessingData = processingStatusQuery.data ?? null;

  useEffect(() => {
    if (phase !== "processing" || processingTerminalHandled.current) return;
    const status = latestProcessingData?.processingStatus;
    if (!status || !TERMINAL_PROCESSING_STATUSES.has(status)) return;

    processingTerminalHandled.current = true;

    if (status === "COMPLETED") {
      setPhase("complete");
      toast.success("Video processed successfully!");
    } else {
      const msg =
        latestProcessingData?.errorMessage ?? "Video processing failed";
      setPhase("error");
      setErrorMessage(msg);
      toast.error(msg);
    }
  }, [phase, latestProcessingData]);

  // ── File handlers ─────────────────────────────────────────────────────────

  const handleFile = useCallback((file: File) => {
    if (!ACCEPTED_VIDEO_TYPES.includes(file.type)) {
      toast.error(
        "Unsupported file format. Please use MP4, MKV, MOV, AVI, or WebM.",
      );
      return;
    }
    setVideoFile(file);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      const file = e.dataTransfer.files[0];
      if (file) handleFile(file);
    },
    [handleFile],
  );

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) handleFile(file);
    },
    [handleFile],
  );

  const removeFile = useCallback(() => {
    setVideoFile(null);
    if (inputRef.current) inputRef.current.value = "";
  }, []);

  const handleStartUpload = () => {
    if (!videoFile || hasTriggeredUpload.current) return;
    hasTriggeredUpload.current = true;
    setPhase("uploading");
    uploadMutation.mutate(videoFile);
  };

  // ── Derived state ─────────────────────────────────────────────────────────

  const isUploading = phase === "uploading";
  const isProcessing = phase === "processing";
  const isComplete = phase === "complete";
  const isError = phase === "error";
  const uploadDone = isProcessing || isComplete;

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6"
    >
      {/* ── Upload Card ───────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-md border p-6 sm:p-8"
      >
        <div className="space-y-4">
          <div>
            <div className="flex items-center gap-2">
              {uploadDone || isComplete ? (
                <CheckCircle2 className="size-4 text-green-500" />
              ) : (
                <Upload className="size-4 text-muted-foreground" />
              )}
              <h3 className="text-foreground text-sm font-semibold">
                Upload Video
              </h3>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">
              Upload the video file for &quot;{contentDetails.title}&quot;
            </p>
          </div>

          <input
            ref={inputRef}
            type="file"
            accept="video/mp4,video/x-matroska,video/quicktime,video/x-msvideo,video/webm"
            onChange={handleInputChange}
            className="hidden"
          />

          {/* Drop zone — only before a file is selected */}
          {phase === "pick" && !videoFile && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              onDragOver={(e) => {
                e.preventDefault();
                setIsDragOver(true);
              }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={handleDrop}
              onClick={() => inputRef.current?.click()}
              className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-10 text-center transition-colors ${
                isDragOver
                  ? "border-primary bg-primary/5"
                  : "border-muted-foreground/25 hover:border-primary/40 hover:bg-muted/50"
              }`}
            >
              <div
                className={`flex size-12 items-center justify-center rounded-full transition-colors ${
                  isDragOver ? "bg-primary/10" : "bg-muted"
                }`}
              >
                <Upload
                  className={`size-5 transition-colors ${
                    isDragOver ? "text-primary" : "text-muted-foreground"
                  }`}
                />
              </div>
              <p className="mt-3 text-sm font-medium text-foreground">
                {isDragOver
                  ? "Drop your video here"
                  : "Drag & drop your video file"}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                or click to browse
              </p>
              <p className="mt-3 text-xs text-muted-foreground/60">
                MP4, MKV, MOV, AVI, WebM
              </p>
            </motion.div>
          )}

          {/* File info card — stays visible once a file is selected */}
          {videoFile && (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div className="flex items-center gap-4 rounded-lg border bg-muted/30 p-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                  <FileVideo className="size-5 text-primary" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-foreground">
                    {videoFile.name}
                  </p>
                  <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                    <HardDrive className="size-3" />
                    {formatFileSize(videoFile.size)}
                  </div>
                </div>
                {phase === "pick" && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-8 shrink-0"
                    onClick={removeFile}
                  >
                    <X className="size-4" />
                  </Button>
                )}
              </div>
            </motion.div>
          )}

          {/* Upload progress bar — visible while uploading */}
          {isUploading && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="space-y-2"
            >
              <div className="flex items-center justify-between text-xs">
                <span className="flex items-center gap-1.5 font-medium text-primary">
                  <Loader2 className="size-3 animate-spin" />
                  {uploadMutation.isPending
                    ? "Starting upload..."
                    : latestUploadData?.uploadStatus === "MULTIPART_INITIATED"
                      ? "Preparing multipart upload..."
                      : `Uploading${uploadProgress !== null ? ` — ${uploadProgress}%` : "..."}`}
                </span>
                {latestUploadData?.totalParts != null &&
                  latestUploadData.uploadedParts != null && (
                    <span className="text-muted-foreground">
                      {latestUploadData.uploadedParts}/
                      {latestUploadData.totalParts} parts
                    </span>
                  )}
              </div>
              <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                <motion.div
                  className="h-full rounded-full bg-primary"
                  initial={{ width: 0 }}
                  animate={{ width: `${uploadProgress ?? 5}%` }}
                  transition={{ duration: 0.4, ease: "easeOut" }}
                />
              </div>
            </motion.div>
          )}

          {/* Upload complete banner — persists after upload finishes */}
          {uploadDone && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex items-center gap-2 rounded-lg border border-green-500/20 bg-green-500/5 p-3"
            >
              <CheckCircle2 className="size-4 text-green-500" />
              <p className="text-sm font-medium text-green-700 dark:text-green-400">
                Video uploaded successfully
              </p>
            </motion.div>
          )}

          {/* Start Upload button — only in pick phase with file selected */}
          {phase === "pick" && videoFile && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex justify-end"
            >
              <Button
                size="lg"
                onClick={handleStartUpload}
                className="gap-2 text-sm font-semibold"
              >
                <Upload className="size-4" />
                Start Upload
              </Button>
            </motion.div>
          )}
        </div>
      </motion.div>

      {/* ── Processing Card ───────────────────────────────────────────────── */}
      {(isProcessing || isComplete) && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Cog
                className={`size-4 ${isProcessing ? "animate-spin text-primary" : "text-green-500"}`}
              />
              <h3 className="text-foreground text-sm font-semibold">
                Video Processing
              </h3>
            </div>

            {isProcessing && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex items-center gap-3 rounded-lg border bg-muted/30 p-4"
              >
                <Loader2 className="size-5 shrink-0 animate-spin text-primary" />
                <div>
                  <p className="text-sm font-medium text-foreground">
                    Processing your video...
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Transcoding and generating variants. This may take several
                    minutes.
                  </p>
                </div>
              </motion.div>
            )}

            {isComplete && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex items-center gap-2 rounded-lg border border-green-500/20 bg-green-500/5 p-3"
              >
                <CheckCircle2 className="size-4 text-green-500" />
                <p className="text-sm font-medium text-green-700 dark:text-green-400">
                  Video processed successfully! Your content is ready.
                </p>
              </motion.div>
            )}
          </div>
        </motion.div>
      )}

      {/* ── Error Card ────────────────────────────────────────────────────── */}
      {isError && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-md border border-destructive/20 p-6 sm:p-8"
        >
          <div className="flex items-center gap-2">
            <XCircle className="size-4 shrink-0 text-destructive" />
            <p className="text-sm font-medium text-destructive">
              {errorMessage ?? "An unexpected error occurred."}
            </p>
          </div>
        </motion.div>
      )}
    </motion.div>
  );
}
