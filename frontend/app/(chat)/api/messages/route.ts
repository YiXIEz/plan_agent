import { NextResponse, NextRequest } from "next/server";

const BACKEND = "http://localhost:8080";

export async function GET(request: NextRequest) {
  try {
    const chatId = request.nextUrl.searchParams.get("chatId");
    if (!chatId) return NextResponse.json({ messages: [] });
    const res = await fetch(`${BACKEND}/api/sessions/${chatId}/messages`);
    if (!res.ok) return NextResponse.json({ messages: [] });
    const rows = await res.json();
    const messages = rows.map((r: Record<string, unknown>) => ({
      id: `msg-${r.seq || 0}`,
      chatId,
      role: r.role,
      parts: r.content ? [{ type: "text", text: r.content }] : [],
      createdAt: r.created_at,
    }));
    return NextResponse.json({ messages });
  } catch {
    return NextResponse.json({ messages: [] });
  }
}
