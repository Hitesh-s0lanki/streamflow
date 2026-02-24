import { QueryClient } from "@tanstack/react-query";

const defaultOptions = {
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1 minute
      gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
      retry: 1,
      refetchOnWindowFocus: true,
    },
  },
};

export function createQueryClient() {
  return new QueryClient(defaultOptions);
}
