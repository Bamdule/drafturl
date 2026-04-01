"use client";

import { useEffect } from "react";

export default function CodeBlockCopy() {
  useEffect(() => {
    const pres = document.querySelectorAll(".guide-article pre");
    pres.forEach((pre) => {
      if (pre.querySelector(".copy-btn")) return;

      const wrapper = document.createElement("div");
      wrapper.style.position = "relative";
      pre.parentNode?.insertBefore(wrapper, pre);
      wrapper.appendChild(pre);

      const btn = document.createElement("button");
      btn.className = "copy-btn";
      btn.textContent = "Copy";
      btn.style.cssText =
        "position:absolute;top:8px;right:8px;padding:4px 10px;font-size:11px;font-weight:600;border-radius:6px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.06);color:#999;cursor:pointer;transition:all 0.15s;z-index:1;";

      btn.onmouseenter = () => {
        btn.style.background = "rgba(255,255,255,0.12)";
        btn.style.color = "#ccc";
      };
      btn.onmouseleave = () => {
        btn.style.background = "rgba(255,255,255,0.06)";
        btn.style.color = "#999";
      };

      btn.onclick = async () => {
        const code = pre.querySelector("code")?.textContent ?? pre.textContent ?? "";
        try {
          await navigator.clipboard.writeText(code.trim());
          btn.textContent = "Copied!";
          btn.style.color = "#34d399";
          setTimeout(() => {
            btn.textContent = "Copy";
            btn.style.color = "#999";
          }, 2000);
        } catch {
          // fallback
          const textarea = document.createElement("textarea");
          textarea.value = code.trim();
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand("copy");
          document.body.removeChild(textarea);
          btn.textContent = "Copied!";
          setTimeout(() => { btn.textContent = "Copy"; }, 2000);
        }
      };

      wrapper.appendChild(btn);
    });
  }, []);

  return null;
}
