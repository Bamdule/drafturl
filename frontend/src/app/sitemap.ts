import type { MetadataRoute } from "next";

interface SitemapEntry {
  slug: string;
  updatedAt: string;
}

const baseUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
const apiUrl =
  process.env.INTERNAL_API_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080";

async function fetchPublicDocuments(): Promise<SitemapEntry[]> {
  try {
    const res = await fetch(`${apiUrl}/api/v1/documents/sitemap`, {
      next: { revalidate: 3600 },
    });
    if (!res.ok) return [];
    const json = await res.json();
    return json.data ?? [];
  } catch {
    return [];
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const documents = await fetchPublicDocuments();

  const staticPages: MetadataRoute.Sitemap = [
    {
      url: `${baseUrl}/en`,
      lastModified: new Date(),
      changeFrequency: "weekly",
      priority: 1,
    },
    {
      url: `${baseUrl}/ko`,
      lastModified: new Date(),
      changeFrequency: "weekly",
      priority: 1,
    },
  ];

  const documentPages: MetadataRoute.Sitemap = documents.map((doc) => ({
    url: `${baseUrl}/${doc.slug}`,
    lastModified: new Date(doc.updatedAt),
    changeFrequency: "monthly" as const,
    priority: 0.6,
  }));

  return [...staticPages, ...documentPages];
}
