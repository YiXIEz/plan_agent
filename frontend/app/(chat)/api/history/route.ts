import { NextResponse } from "next/server";

const BACKEND = "http://localhost:8080";

export async function GET() {
  try {
    const res = await fetch(`${BACKEND}/api/sessions`);
    if (!res.ok) return NextResponse.json({ chats: [], hasMore: false });
    const sessions = await res.json();

    const chats = sessions.map((s: Record<string, unknown>) => ({
      id: s.session_id,
      title: s.title || "对话",
      createdAt: s.created_at,
      userId: "guest",
      visibility: "private",
    }));

    return NextResponse.json({ chats, hasMore: false });
  } catch {
    return NextResponse.json({ chats: [], hasMore: false });
  }
}
