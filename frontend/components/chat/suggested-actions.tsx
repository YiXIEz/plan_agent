"use client";

import type { UseChatHelpers } from "@ai-sdk/react";
import { motion } from "framer-motion";
import { memo } from "react";
import { suggestions } from "@/lib/constants";
import type { ChatMessage } from "@/lib/types";
import { Suggestion } from "../ai-elements/suggestion";
import type { VisibilityType } from "./visibility-selector";

type SuggestedActionsProps = {
  chatId: string;
  sendMessage: UseChatHelpers<ChatMessage>["sendMessage"];
  selectedVisibilityType: VisibilityType;
};

function PureSuggestedActions({ chatId, sendMessage }: SuggestedActionsProps) {
  const suggestedActions = suggestions;

  return (
    <div className="flex flex-wrap justify-center gap-2" data-testid="suggested-actions">
      {suggestedActions.map((suggestedAction, index) => (
        <motion.div
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 16 }}
          initial={{ opacity: 0, y: 16 }}
          key={suggestedAction}
          transition={{
            delay: 0.05 * index,
            duration: 0.4,
            ease: [0.16, 1, 0.3, 1],
          }}
        >
          <Suggestion
            className="inline-flex h-10 items-center rounded-full border border-[rgba(255,255,255,0.08)] bg-[rgba(255,255,255,0.04)] px-5 text-[14px] text-[rgba(255,255,255,0.7)] transition-all duration-[180ms] ease-out hover:border-[rgba(255,255,255,0.15)] hover:bg-[rgba(255,255,255,0.07)] hover:text-foreground cursor-pointer"
            onClick={(suggestion) => {
              window.history.pushState(
                {},
                "",
                `${process.env.NEXT_PUBLIC_BASE_PATH ?? ""}/chat/${chatId}`
              );
              sendMessage({
                role: "user",
                parts: [{ type: "text", text: suggestion }],
              });
            }}
            suggestion={suggestedAction}
          >
            {suggestedAction}
          </Suggestion>
        </motion.div>
      ))}
    </div>
  );
}

export const SuggestedActions = memo(
  PureSuggestedActions,
  (prevProps, nextProps) => {
    if (prevProps.chatId !== nextProps.chatId) return false;
    if (prevProps.selectedVisibilityType !== nextProps.selectedVisibilityType) return false;
    return true;
  }
);
