import { auth } from "@clerk/nextjs/server";
import { SignIn } from "@clerk/nextjs";
import { redirect } from "next/navigation";

export default async function SignInPage() {
  const { userId } = await auth();
  if (userId) redirect("/");

  return (
    <div className="flex justify-center">
      <SignIn
        signUpUrl="/sign-up"
        forceRedirectUrl="/"
        fallbackRedirectUrl="/"
        appearance={{
          elements: {
            rootBox: "w-full",
            card: "shadow-none w-full",
          },
        }}
      />
    </div>
  );
}
