import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "streamflow-netflix-demo.s3.ap-south-1.amazonaws.com",
        pathname: "/**",
        // omit search so S3 signed URLs with ?X-Amz-* query params are allowed
      },
      {
        protocol: "https",
        hostname: "*",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
