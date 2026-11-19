/**
 * HTML 원본 글 → iframe srcdoc.
 *
 * iframe 은 sandbox="allow-scripts ..." 이고 allow-same-origin 이 없으므로
 * 안의 스크립트는 'null' origin 에서 돈다 → 블로그의 쿠키·localStorage·API 에 접근 불가.
 *
 * 주입하는 것 (원본 <head> 맨 앞):
 *  1. <base target="_blank"> — 글 안 링크가 iframe 안에서 열리지 않고 새 탭으로
 *  2. 높이 보고 스크립트 — 내용 높이를 부모에 postMessage → iframe 높이를 내용에 맞춤(이중 스크롤 방지)
 */

export const HEIGHT_MESSAGE = 'lucium:html-post-height'

function heightReporter(frameId: string): string {
    // 원본 글의 전역과 충돌하지 않도록 IIFE. 따옴표 이스케이프를 피하려고 JSON.stringify 사용.
    // 높이는 documentElement.scrollHeight 를 쓰지 않음 — 항상 iframe 높이 이상이라 줄어들지 못하고,
    // 100vh 를 쓰는 글이면 iframe 을 키울수록 내용도 커지는 무한 증가가 생김.
    return `<script>(function(){
var id=${JSON.stringify(frameId)},last=0;
function h(){var d=document.documentElement,b=document.body;
return Math.ceil(Math.max(d.getBoundingClientRect().height,b?b.scrollHeight:0));}
function post(){var v=h();if(v!==last){last=v;parent.postMessage({type:${JSON.stringify(HEIGHT_MESSAGE)},id:id,height:v},'*');}}
if(window.ResizeObserver){new ResizeObserver(post).observe(document.documentElement);}
document.addEventListener('DOMContentLoaded',function(){post();if(window.ResizeObserver&&document.body){new ResizeObserver(post).observe(document.body);}});
window.addEventListener('load',post);
setTimeout(post,300);setTimeout(post,1500);
})();</script>`
}

export function buildHtmlSrcDoc(html: string, frameId: string): string {
    const inject = `<base target="_blank">${heightReporter(frameId)}`

    // <head ...> 가 있으면 그 바로 뒤에, 없으면 문서 맨 앞(<!doctype> 뒤)에 넣음
    const headOpen = /<head(\s[^>]*)?>/i.exec(html)
    if (headOpen) {
        const at = headOpen.index + headOpen[0].length
        return html.slice(0, at) + inject + html.slice(at)
    }
    const doctype = /^\s*<!doctype[^>]*>/i.exec(html)
    if (doctype) {
        const at = doctype.index + doctype[0].length
        return html.slice(0, at) + inject + html.slice(at)
    }
    return inject + html
}
