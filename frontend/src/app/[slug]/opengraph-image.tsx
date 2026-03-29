import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "DraftURL Document";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function OgImage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let title = "Shared Document";
  let docType = "HTML";

  try {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    const res = await fetch(`${apiUrl}/api/v1/documents/${slug}/view`, {
      next: { revalidate: 60 },
    });
    if (res.ok) {
      const doc = await res.json();
      title = doc.title || "Shared Document";
      docType = doc.docType === "markdown" ? "Markdown" : "HTML";
    }
  } catch {
    // fallback to defaults
  }

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          background: "linear-gradient(135deg, #0f0f17 0%, #1a1a2e 50%, #16213e 100%)",
          fontFamily: "sans-serif",
        }}
      >
        {/* Glow */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            width: "600px",
            height: "400px",
            borderRadius: "50%",
            background: "radial-gradient(ellipse, rgba(99, 102, 241, 0.12), transparent 70%)",
          }}
        />
        {/* Doc type badge */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            marginBottom: "24px",
          }}
        >
          <div
            style={{
              padding: "6px 16px",
              borderRadius: "20px",
              background: "rgba(99, 102, 241, 0.2)",
              border: "1px solid rgba(99, 102, 241, 0.3)",
              color: "#818cf8",
              fontSize: "14px",
              fontWeight: 600,
            }}
          >
            {docType}
          </div>
        </div>
        {/* Title */}
        <div
          style={{
            fontSize: "40px",
            fontWeight: 700,
            color: "#f1f5f9",
            textAlign: "center",
            maxWidth: "800px",
            lineHeight: 1.3,
            overflow: "hidden",
            textOverflow: "ellipsis",
          }}
        >
          {title.length > 60 ? title.slice(0, 57) + "..." : title}
        </div>
        {/* Footer */}
        <div
          style={{
            position: "absolute",
            bottom: "40px",
            display: "flex",
            alignItems: "center",
            gap: "10px",
          }}
        >
          <div
            style={{
              width: "28px",
              height: "28px",
              borderRadius: "8px",
              background: "linear-gradient(135deg, #6366f1, #8b5cf6)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "white",
              fontSize: "14px",
              fontWeight: 800,
            }}
          >
            D
          </div>
          <span style={{ fontSize: "16px", color: "#94a3b8" }}>
            Shared via DraftURL
          </span>
        </div>
      </div>
    ),
    { ...size },
  );
}
