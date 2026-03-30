package com.drafturl.api.infra.sanitizer;

import com.drafturl.api.domain.document.port.ContentSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Jsoup 기반 HTML 새니타이저 구현체.
 *
 * <p>보안 정책 (security.md 섹션 5 참조):
 * <ul>
 *   <li>사용자 HTML은 sandbox iframe(allow-scripts, allow-same-origin 제거)에서 렌더링된다.</li>
 *   <li>서버 측 새니타이징은 다층 방어(defense in depth)를 위해 적용한다.</li>
 *   <li>script/style 태그는 허용한다 (LLM 생성 HTML의 Chart.js 등 인터랙티브 요소 지원).</li>
 *   <li>iframe/object/embed 태그와 javascript: URL 스킴은 제거한다.</li>
 *   <li>form/input 등 폼 태그는 허용한다 (sandbox iframe이 allow-forms 없이 폼 제출 차단).</li>
 * </ul>
 *
 * <p>구현 전략: head/body 분리 처리
 * <ul>
 *   <li>Jsoup.clean()은 body fragment만 반환하므로, head와 body를 분리하여 각각 새니타이징한다.</li>
 *   <li>head: meta/title/style/link 태그만 허용, script 태그 제거</li>
 *   <li>body: BODY_SAFELIST로 clean() 적용</li>
 *   <li>최종 결과를 완전한 HTML 문서로 재조립한다.</li>
 * </ul>
 */
@Component
public class JsoupContentSanitizer implements ContentSanitizer {

    /**
     * body 영역에 적용하는 Safelist.
     *
     * relaxed() 기반으로 script/style/폼 태그를 추가 허용하되,
     * iframe/object/embed는 제거한다 (relaxed에 포함되지 않으므로 별도 처리 불필요).
     * 폼 태그는 허용한다 (sandbox iframe이 allow-forms 없이 폼 제출을 차단하므로 안전).
     */
    private static final Safelist BODY_SAFELIST = createBodySafelist();

    /**
     * head 영역에서 허용하는 태그 목록.
     * meta, title, style, link만 허용하고 script는 제거한다.
     */
    private static final String HEAD_ALLOWED_TAGS = "meta, title, style, link";

    /**
     * fetch/XHR/WebSocket을 차단하는 CSP meta 태그.
     * head의 가장 앞에 주입되어 공격자가 삽입한 CSP를 덮어쓸 수 없도록 한다.
     */
    private static final String CSP_META = "<meta http-equiv=\"Content-Security-Policy\" content=\"connect-src 'none';\">";

    private static Safelist createBodySafelist() {
        return Safelist.relaxed()
                // script 태그 허용: LLM 생성 HTML의 인터랙티브 요소 지원
                // sandbox iframe에서 격리되므로 안전
                .addTags("script", "style", "canvas", "svg", "path", "circle", "rect",
                        "line", "polyline", "polygon", "text", "g", "defs", "use",
                        "nav", "header", "footer", "main", "section", "article", "aside",
                        "figure", "figcaption", "details", "summary", "mark", "time",
                        "video", "audio", "source", "picture",
                        // 폼 태그 허용: sandbox iframe이 allow-forms 없이 폼 제출을 차단하므로 안전
                        "form", "input", "textarea", "select", "option", "optgroup",
                        "button", "fieldset", "legend", "label", "datalist", "output")
                // script/style 태그의 속성 허용
                .addAttributes("script", "src", "type", "defer", "async", "crossorigin")
                .addAttributes("style", "type")
                .addAttributes("link", "rel", "href", "type", "crossorigin")
                // 전역 속성 허용 (class, id, style, data-* 등)
                .addAttributes(":all", "class", "id", "style", "title", "role",
                        "aria-label", "aria-hidden", "aria-expanded", "aria-controls",
                        "data-value", "data-type", "data-id", "data-target", "data-toggle",
                        "hidden", "lang", "dir", "tabindex")
                // SVG 속성
                .addAttributes("svg", "viewBox", "width", "height", "xmlns", "fill",
                        "stroke", "stroke-width")
                .addAttributes("path", "d", "fill", "stroke", "stroke-width", "transform")
                .addAttributes("circle", "cx", "cy", "r", "fill", "stroke")
                .addAttributes("rect", "x", "y", "width", "height", "fill", "stroke", "rx", "ry")
                .addAttributes("line", "x1", "y1", "x2", "y2", "stroke", "stroke-width")
                .addAttributes("text", "x", "y", "font-size", "text-anchor", "fill", "dy", "dx")
                .addAttributes("g", "transform", "fill", "stroke")
                .addAttributes("use", "href", "x", "y", "width", "height")
                // 폼 관련 속성
                .addAttributes("form", "action", "method", "name", "novalidate", "autocomplete")
                .addAttributes("input", "type", "name", "value", "placeholder", "disabled",
                        "readonly", "required", "checked", "min", "max", "step",
                        "minlength", "maxlength", "pattern", "autocomplete", "size")
                .addAttributes("textarea", "name", "placeholder", "rows", "cols",
                        "disabled", "readonly", "required", "maxlength")
                .addAttributes("select", "name", "disabled", "required", "multiple", "size")
                .addAttributes("option", "value", "selected", "disabled")
                .addAttributes("optgroup", "label", "disabled")
                .addAttributes("button", "type", "name", "value", "disabled")
                .addAttributes("fieldset", "disabled", "name")
                .addAttributes("label", "for")
                .addAttributes("output", "for", "name")
                // canvas 속성
                .addAttributes("canvas", "width", "height")
                // media 속성
                .addAttributes("video", "src", "controls", "width", "height", "autoplay",
                        "muted", "loop", "poster", "preload")
                .addAttributes("audio", "src", "controls", "autoplay", "muted", "loop", "preload")
                .addAttributes("source", "src", "type")
                // protocol 허용 (https, data 등)
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data")
                .addProtocols("script", "src", "http", "https")
                .addProtocols("link", "href", "http", "https")
                .addProtocols("video", "src", "http", "https")
                .addProtocols("audio", "src", "http", "https")
                .addProtocols("source", "src", "http", "https")
                // javascript: URL은 Safelist에 포함되지 않으므로 자동 차단됨
                .preserveRelativeLinks(true);
    }

    @Override
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        Document document = Jsoup.parse(html);

        // 1. head 영역 새니타이징: 허용 태그 외 제거 (특히 script 제거)
        String sanitizedHead = sanitizeHead(document);

        // 2. body 영역: Safelist 기반 clean() 적용
        Element body = document.body();
        String bodyHtml = body != null ? body.html() : "";
        String sanitizedBody = Jsoup.clean(bodyHtml, "", BODY_SAFELIST,
                new Document.OutputSettings().prettyPrint(false));

        // 3. 완전한 HTML 문서로 재조립
        return assembleDocument(document, sanitizedHead, sanitizedBody);
    }

    /**
     * head 영역에서 허용되지 않는 태그를 제거한다.
     * meta, title, style, link만 유지하고 script 등은 제거한다.
     */
    private String sanitizeHead(Document document) {
        Element head = document.head();
        if (head == null) {
            return "";
        }

        // head 내 허용되지 않는 태그 제거 (script 등)
        // 순회 중 제거 시 ConcurrentModification 방지를 위해 리스트로 수집 후 제거
        List<Element> toRemove = head.children().stream()
                .filter(child -> !HEAD_ALLOWED_TAGS.contains(child.tagName()))
                .toList();
        toRemove.forEach(Element::remove);

        // head 내 link 태그에서 javascript: URL 제거
        head.select("link[href^=javascript:]").remove();

        return head.html();
    }

    /**
     * 새니타이징된 head와 body를 완전한 HTML 문서로 조립한다.
     */
    private String assembleDocument(Document originalDocument, String headContent,
                                    String bodyContent) {
        Document result = Document.createShell("");

        // doctype은 Jsoup가 자동 생성
        // html 태그의 lang 등 속성 복원
        Element originalHtml = originalDocument.selectFirst("html");
        if (originalHtml != null) {
            originalHtml.attributes().forEach(attr ->
                    result.selectFirst("html").attr(attr.getKey(), attr.getValue()));
        }

        result.head().html(CSP_META + "\n" + headContent);
        result.body().html(bodyContent);

        return result.html();
    }
}
