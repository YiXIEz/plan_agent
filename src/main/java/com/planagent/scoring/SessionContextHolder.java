package com.planagent.scoring;

import com.planagent.model.SessionContext;

/**
 * ThreadLocal holder so SearchTools can access the current SessionContext
 * without changing Tool method signatures. ToolRegistry sets this before
 * each tool execution and clears it afterwards.
 */
public class SessionContextHolder {
    private static final ThreadLocal<SessionContext> holder = new ThreadLocal<>();

    public static void set(SessionContext ctx) { holder.set(ctx); }
    public static SessionContext get() { return holder.get(); }
    public static void clear() { holder.remove(); }
}
