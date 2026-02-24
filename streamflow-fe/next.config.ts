import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "streamflow-netflix-demo.s3.ap-south-1.amazonaws.com",
        pathname: "/**",
        search: "",
      },
      {
        protocol: "https",
        hostname: "*",
        pathname: "/**",
        search: "",
      },
    ],
  },
};

export default nextConfig;
