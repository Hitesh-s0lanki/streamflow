"use client";

import { motion } from "framer-motion";
import { Clapperboard } from "lucide-react";
import { useContentGeneration } from "./use-movie-generation";
import { ContentSearchSection } from "./content-search-section";
import { ContentSelectionSection } from "./content-selection-section";
import { GeneratedContentSection } from "./generated-content-section";
import { VideoUploadSection } from "./video-upload-section";

const UploadForm = () => {
  const {
    phase,
    contentType,
    season,
    setContentType,
    setSeason,
    contentId,
    selectedMatch,
    contentDetails,
    contentImages,
    imageGenFailed,
    searchResults,
    isSearching,
    isGeneratingDetails,
    isGeneratingImages,
    isSaving,
    isCreatingContent,
    isUploadingAssets,
    handleSearch,
    handleSelectMatch,
    handleManualImages,
    handleProceedToUpload,
    handleBackToReview,
    handleReset,
  } = useContentGeneration();

  const isSeries = contentType === "SERIES";
  const headerTitle = isSeries ? "Upload a Series" : "Upload a Movie";
  const headerSubtitle = isSeries
    ? "Enter the series name and we'll handle the rest."
    : "Enter the movie name and we'll handle the rest.";

  const handleContentTypeChange = (type: "MOVIE" | "SERIES") => {
    setContentType(type);
    if (type === "MOVIE") setSeason(null);
  };

  return (
    <div className="flex w-full px-4 py-6 lg:py-12 justify-center">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="w-full max-w-5xl"
      >
        {/* Header */}
        <div className="mb-8 w-full flex flex-row items-center gap-4">
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{
              type: "spring",
              stiffness: 200,
              damping: 15,
              delay: 0.1,
            }}
            className="bg-primary/10 p-3 flex size-16 items-center justify-center rounded-2xl"
          >
            <Clapperboard className="text-primary size-8" />
          </motion.div>
          <div className="space-y-1">
            <motion.h1
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-foreground text-lg font-bold tracking-tight sm:text-3xl"
            >
              {headerTitle}
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="text-muted-foreground text-sm pr-5"
            >
              {headerSubtitle}
            </motion.p>
          </div>
        </div>

        {/* Section: Search */}
        {phase === "search" && (
          <ContentSearchSection
            contentType={contentType}
            season={season}
            onContentTypeChange={handleContentTypeChange}
            onSeasonChange={setSeason}
            onSearch={handleSearch}
            isSearching={isSearching}
          />
        )}

        {/* Section: Select (only when multiple matches) */}
        {phase === "select" && searchResults && (
          <ContentSelectionSection
            contentType={contentType}
            searchResults={searchResults}
            onSelect={handleSelectMatch}
            onBack={handleReset}
          />
        )}

        {/* Section: Generating + Review + Saving + Upload (stays visible) */}
        {(phase === "generating" ||
          phase === "review" ||
          phase === "saving" ||
          phase === "upload") &&
          selectedMatch && (
            <GeneratedContentSection
              selectedMovie={selectedMatch}
              movieDetails={contentDetails}
              movieImages={contentImages}
              isGeneratingDetails={isGeneratingDetails}
              isGeneratingImages={isGeneratingImages}
              isSaving={isSaving}
              isCreatingContent={isCreatingContent}
              isUploadingAssets={isUploadingAssets}
              onProceed={handleProceedToUpload}
              onBack={handleReset}
              readOnly={phase === "upload"}
              imageGenFailed={imageGenFailed}
              onManualImagesUpload={handleManualImages}
            />
          )}

        {/* Section: Upload + Processing (appears below generated content) */}
        {phase === "upload" && contentId && contentDetails && (
          <VideoUploadSection
            contentId={contentId}
            contentDetails={contentDetails}
          />
        )}
      </motion.div>
    </div>
  );
};

export default UploadForm;
