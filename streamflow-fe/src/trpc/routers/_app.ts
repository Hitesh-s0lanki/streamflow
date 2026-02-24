import { createTRPCRouter } from "../init";
import { aiRouter } from "@/modules/ai/server/procedures";
import { contentRouter } from "@/modules/content/server/procedures";

export const appRouter = createTRPCRouter({
  ai: aiRouter,
  content: contentRouter,
});

export type AppRouter = typeof appRouter;
