import { motion } from "framer-motion";

export const Greeting = () => {
  return (
    <div className="flex flex-col items-center px-8 -mt-20" key="overview">
      <motion.h1
        animate={{ opacity: 1, y: 0 }}
        className="text-center font-semibold text-foreground leading-tight tracking-tight"
        initial={{ opacity: 0, y: 32 }}
        transition={{ delay: 0.3, duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        style={{ fontSize: "56px", letterSpacing: "-0.03em", lineHeight: "1.1" }}
      >
        今天想安排什么活动？
      </motion.h1>
      <motion.p
        animate={{ opacity: 1, y: 0 }}
        className="mt-4 text-center leading-relaxed"
        initial={{ opacity: 0, y: 16 }}
        transition={{ delay: 0.45, duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
        style={{ fontSize: "17px", color: "rgba(255,255,255,0.55)", letterSpacing: "-0.01em" }}
      >
        告诉我你的需求，AI 助手为你规划完美的周末出行方案
      </motion.p>
    </div>
  );
};
