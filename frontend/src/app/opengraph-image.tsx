import { ImageResponse } from "next/og";

export const runtime = "edge";
export const alt = "DraftURL — Share HTML & Markdown Instantly via URL";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OgImage() {
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
        {/* Glow effect */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            width: "600px",
            height: "400px",
            borderRadius: "50%",
            background: "radial-gradient(ellipse, rgba(99, 102, 241, 0.15), transparent 70%)",
          }}
        />
        {/* Logo / Brand */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "12px",
            marginBottom: "24px",
          }}
        >
          <div
            style={{
              width: "48px",
              height: "48px",
              borderRadius: "12px",
              background: "linear-gradient(135deg, #6366f1, #8b5cf6)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "white",
              fontSize: "24px",
              fontWeight: 800,
            }}
          >
            D
          </div>
          <span
            style={{
              fontSize: "36px",
              fontWeight: 700,
              color: "#e2e8f0",
              letterSpacing: "-0.02em",
            }}
          >
            DraftURL
          </span>
        </div>
        {/* Tagline */}
        <div
          style={{
            fontSize: "28px",
            fontWeight: 600,
            color: "#f1f5f9",
            textAlign: "center",
            maxWidth: "700px",
            lineHeight: 1.4,
          }}
        >
          Share HTML & Markdown
        </div>
        <div
          style={{
            fontSize: "28px",
            fontWeight: 600,
            color: "#818cf8",
            textAlign: "center",
            marginTop: "4px",
          }}
        >
          Instantly via URL
        </div>
        {/* Subtitle */}
        <div
          style={{
            fontSize: "16px",
            color: "#94a3b8",
            marginTop: "20px",
            textAlign: "center",
          }}
        >
          No signup required · Paste and share in 3 seconds
        </div>
      </div>
    ),
    { ...size },
  );
}
