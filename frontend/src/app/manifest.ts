import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "DraftURL — Share HTML & Markdown Instantly",
    short_name: "DraftURL",
    description:
      "Paste HTML or Markdown and get a shareable URL in seconds. No signup required.",
    start_url: "/",
    display: "standalone",
    background_color: "#0f0f17",
    theme_color: "#6366f1",
    icons: [
      {
        src: "/favicon.svg",
        sizes: "any",
        type: "image/svg+xml",
      },
    ],
  };
}
