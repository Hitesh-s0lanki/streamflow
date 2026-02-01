package com.streamflow.dto.admin;

import lombok.Data;

/**
 * Optional body for upload-url: e.g. expected content-type for the file.
 */
@Data
public class PresignedUploadRequest {

    /**
     * Content-Type for the upload (e.g. video/mp4). Defaults to
     * application/octet-stream.
     */
    private String contentType = "application/octet-stream";
}
