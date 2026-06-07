import { Suspense } from "react";
import { Toaster } from "sonner";
import { AppSidebar } from "@/components/chat/app-sidebar";
import { DataStreamProvider } from "@/components/chat/data-stream-provider";
import { ChatShell } from "@/components/chat/shell";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { ActiveChatProvider } from "@/hooks/use-active-chat";

export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<div className="flex h-dvh bg-sidebar" />}>
      <DataStreamProvider>
        <SidebarProvider defaultOpen={true}>
          <AppSidebar user={undefined} />
          <SidebarInset>
            <Toaster position="top-center" theme="system" />
            <Suspense fallback={<div className="flex h-dvh" />}>
              <ActiveChatProvider>
                <ChatShell />
              </ActiveChatProvider>
            </Suspense>
            {children}
          </SidebarInset>
        </SidebarProvider>
      </DataStreamProvider>
    </Suspense>
  );
}
