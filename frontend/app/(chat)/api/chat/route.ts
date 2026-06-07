import { NextResponse, NextRequest } from "next/server";

const BACKEND = "http://localhost:8080";

export async function DELETE(request: NextRequest) {
  try {
    const id = request.nextUrl.searchParams.get("id");
    if (!id) return NextResponse.json({ error: "no id" }, { status: 400 });
    await fetch(`${BACKEND}/api/sessions/${id}`, { method: "DELETE" });
    return NextResponse.json({ success: true });
  } catch {
    return NextResponse.json({ success: false }, { status: 500 });
  }
}
