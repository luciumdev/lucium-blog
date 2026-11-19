package com.ysh.post.service;

import com.ysh.domain.post.entity.ContentFormat;
import com.ysh.domain.post.service.PostPlainText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostPlainTextTest {

    @Test
    void markdown_isIndexedAsIs() {
        String md = "# 제목\n\n`code` 본문";
        assertThat(PostPlainText.of(md, ContentFormat.MARKDOWN)).isEqualTo(md);
    }

    @Test
    void html_stripsTagsScriptsAndStyles() {
        String html = """
                <!doctype html>
                <html><head><title>머리</title><style>.a{color:red}</style></head>
                <body>
                  <!-- 주석 -->
                  <h1 class="t">선형 회귀</h1>
                  <p>비용 함수 &lt;MSE&gt; &amp; 경사 하강법</p>
                  <script>console.log('x')</script>
                </body></html>
                """;

        String text = PostPlainText.of(html, ContentFormat.HTML);

        assertThat(text).isEqualTo("선형 회귀 비용 함수 <MSE> & 경사 하강법");
    }

    @Test
    void html_doesNotDoubleDecodeAmpersand() {
        assertThat(PostPlainText.of("<p>&amp;lt;</p>", ContentFormat.HTML)).isEqualTo("&lt;");
    }

    @Test
    void null_isEmpty() {
        assertThat(PostPlainText.of(null, ContentFormat.HTML)).isEmpty();
    }
}
