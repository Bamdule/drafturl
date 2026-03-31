import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "DraftURL — Share HTML & Markdown Instantly via URL";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function Image() {
  return new ImageResponse(
    (
      <div
        style={{
          background: "#0f1117",
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: "system-ui, sans-serif",
          position: "relative",
        }}
      >
        {/* Background glow */}
        <div
          style={{
            position: "absolute",
            top: 0,
            left: "50%",
            transform: "translateX(-50%)",
            width: 800,
            height: 400,
            background:
              "radial-gradient(ellipse, rgba(99,102,241,0.15) 0%, transparent 70%)",
          }}
        />
        {/* Logo */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 16,
            marginBottom: 32,
          }}
        >
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 14,
              background: "linear-gradient(135deg, #7c5cfc, #a78bfa)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 32,
              fontWeight: 700,
              color: "white",
            }}
          >
            D
          </div>
          <span
            style={{
              fontSize: 36,
              fontWeight: 700,
              color: "#e2e8f0",
              letterSpacing: -1,
            }}
          >
            DraftURL
          </span>
        </div>
        {/* Headline */}
        <div
          style={{
            fontSize: 56,
            fontWeight: 800,
            color: "#ffffff",
            textAlign: "center",
            lineHeight: 1.15,
            letterSpacing: -2,
            maxWidth: 900,
            marginBottom: 24,
            display: "flex",
            flexWrap: "wrap",
            justifyContent: "center",
          }}
        >
          Share HTML & Markdown{" "}
          <span style={{ color: "#a78bfa" }}>Instantly via URL</span>
        </div>
        {/* Subtitle */}
        <div
          style={{
            fontSize: 24,
            color: "#94a3b8",
            textAlign: "center",
            maxWidth: 700,
            lineHeight: 1.5,
          }}
        >
          Paste your document · Get a shareable link · No signup required
        </div>
        {/* URL badge */}
        <div
          style={{
            marginTop: 40,
            background: "#1e293b",
            border: "1px solid #334155",
            borderRadius: 10,
            padding: "10px 24px",
            fontSize: 20,
            color: "#7c5cfc",
            fontFamily: "monospace",
          }}
        >
          drafturl.com
        </div>
      </div>
    ),
    { ...size },
  );
}
