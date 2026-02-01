"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Loader2, RefreshCw, AlertCircle } from "lucide-react";

const API_BASE = "https://jsonplaceholder.typicode.com";

type Post = {
  id: number;
  userId: number;
  title: string;
  body: string;
};

async function fetchPosts(): Promise<Post[]> {
  const res = await fetch(`${API_BASE}/posts?_limit=5`);
  if (!res.ok) throw new Error("Failed to fetch posts");
  return res.json();
}

async function createPost(payload: { title: string; body: string }) {
  const res = await fetch(`${API_BASE}/posts`, {
    method: "POST",
    headers: { "Content-type": "application/json; charset=UTF-8" },
    body: JSON.stringify({ ...payload, userId: 1 }),
  });
  if (!res.ok) throw new Error("Failed to create post");
  return res.json();
}

export function QueryDemo() {
  const queryClient = useQueryClient();

  const {
    data: posts,
    isPending,
    isFetching,
    isError,
    error,
    refetch,
    dataUpdatedAt,
    isStale,
  } = useQuery({
    queryKey: ["posts"],
    queryFn: fetchPosts,
  });

  const mutation = useMutation({
    mutationFn: createPost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });

  return (
    <div className="container max-w-3xl space-y-8 py-10">
      <div className="space-y-2">
        <h1 className="text-3xl font-bold tracking-tight">
          TanStack Query Demo
        </h1>
        <p className="text-muted-foreground">
          useQuery (posts list), loading/error states, refetch, and useMutation
          with cache invalidation. Data from{" "}
          <a
            href="https://jsonplaceholder.typicode.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary underline"
          >
            JSONPlaceholder
          </a>
          .
        </p>
        <Button variant="outline" size="sm" asChild>
          <Link href="/">← Back home</Link>
        </Button>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>useQuery — Posts</CardTitle>
            <CardDescription>
              {isStale ? "Data is stale" : "Data is fresh"} · Last updated:{" "}
              {dataUpdatedAt
                ? new Date(dataUpdatedAt).toLocaleTimeString()
                : "—"}
            </CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <RefreshCw className="size-4" />
            )}
            <span className="ml-2">Refetch</span>
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          {isPending && (
            <div className="space-y-2">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-5 w-1/2" />
              <Skeleton className="h-4 w-full" />
            </div>
          )}

          {isError && (
            <Alert variant="destructive">
              <AlertCircle className="size-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>
                {error instanceof Error ? error.message : "Something went wrong"}
              </AlertDescription>
            </Alert>
          )}

          {!isPending && !isError && posts && (
            <ul className="space-y-3">
              {posts.map((post) => (
                <li
                  key={post.id}
                  className="rounded-lg border bg-muted/30 px-4 py-3"
                >
                  <span className="font-medium text-muted-foreground">
                    #{post.id}
                  </span>{" "}
                  {post.title}
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>useMutation — Create post</CardTitle>
          <CardDescription>
            Submitting invalidates the posts query so the list refetches.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="flex flex-col gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const title = (form.elements.namedItem("title") as HTMLInputElement)
                ?.value;
              const body = (form.elements.namedItem("body") as HTMLInputElement)
                ?.value;
              if (title && body) {
                mutation.mutate({ title, body });
                form.reset();
              }
            }}
          >
            <input
              name="title"
              placeholder="Title"
              className="rounded-md border bg-background px-3 py-2 text-sm"
              required
            />
            <textarea
              name="body"
              placeholder="Body"
              rows={2}
              className="rounded-md border bg-background px-3 py-2 text-sm"
              required
            />
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? (
                <Loader2 className="size-4 animate-spin" />
              ) : null}
              Create post
            </Button>
            {mutation.isSuccess && (
              <p className="text-sm text-green-600 dark:text-green-400">
                Post created (ID: {mutation.data?.id}). List will refetch.
              </p>
            )}
            {mutation.isError && (
              <Alert variant="destructive">
                <AlertDescription>
                  {mutation.error instanceof Error
                    ? mutation.error.message
                    : "Mutation failed"}
                </AlertDescription>
              </Alert>
            )}
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
