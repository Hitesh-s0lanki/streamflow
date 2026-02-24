/**
 * Shared base and enums for Streamflow entities.
 * BaseEntity is the mapped superclass for all entities.
 * Enums align with com.streamflow.entity.enums.
 */

// --- Base type ---

/**
 * Mapped superclass for all entities. Provides common fields.
 */
export interface BaseEntity {
  /** Primary key (generated) */
  id: string;
  /** Creation timestamp (ISO-8601) */
  createdAt: string;
  /** Last update timestamp (ISO-8601) */
  updatedAt: string;
  /** Optional status (max 32 chars) */
  status?: string | null;
}

// --- Enums ---

/**
 * Type of content: Movie (single video) or Series (seasons/episodes).
 */
export type ContentType = "MOVIE" | "SERIES";

/**
 * Lifecycle state of an ingestion job (upload → processing → ready).
 */
export type IngestionStatus =
  | "PENDING"
  | "UPLOADING"
  | "UPLOADED"
  | "PROCESSING"
  | "TRANSCODED"
  | "SPRITES_GENERATED"
  | "READY"
  | "FAILED";

/**
 * Status of a playback license (issued, revoked, expired).
 */
export type LicenseStatus = "ACTIVE" | "REVOKED" | "EXPIRED";

/**
 * Types of playback events for analytics (Kafka / event-driven).
 */
export type PlaybackEventType =
  | "PLAY"
  | "PAUSE"
  | "SEEK"
  | "BUFFERING"
  | "COMPLETED"
  | "ABANDONED"
  | "ERROR";

/**
 * Visibility of content in the catalog (draft vs published).
 */
export type PublishStatus = "DRAFT" | "PUBLISHED";
