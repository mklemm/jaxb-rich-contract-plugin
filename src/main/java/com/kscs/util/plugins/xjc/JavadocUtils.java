/*
 * MIT License
 *
 * Copyright 2019 Crown Copyright
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.kscs.util.plugins.xjc;

import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JDocCommentable;
import org.davidmoten.text.utils.WordWrap;

import java.util.ArrayList;
import java.util.List;

public class JavadocUtils {

    /**
     * Adds a paragraph and optionally a p tag to the head of the passed {@link JDocComment}.
     * Also hard wraps the text.
     */
    static JDocComment appendJavadocParagraph(final JDocCommentable jDocCommentable, final String paragraphText) {
        final JDocComment jDocComment = jDocCommentable.javadoc();
        if (paragraphText != null && !paragraphText.isEmpty()) {
            List<Object> jdocItems = new ArrayList<>(jDocComment);
            jDocComment.clear();

            // Add hard line breaks so we get readable javadoc
            String wrappedText = hardWrapTextForJavadoc(paragraphText);
            jDocComment.append(wrappedText + "\n\n");

            if (!jdocItems.isEmpty()) {
                Object firstItem = jdocItems.get(0);
                if (!(firstItem instanceof String) || !((String) firstItem).startsWith("<p>") ) {
                    // We already had text in the comment section so add line break and p tag
                    jDocComment.append("<P>\n");
                }
            }
            // add the removed items back in so we have our para at the head
            jDocComment.addAll(jdocItems);
        }
        return jDocComment;
    }

    /**
     * Adds each paragraph to the tail of the javadoc comment section of {@link JDocComment}. Also adds a p tag between
     * each paragraph and hard wraps the text.
     */
    static JDocComment appendJavadocCommentParagraphs(final JDocComment jDocComment, final String... paragraphs) {
        if (paragraphs != null) {
            for (int i = 0; i < paragraphs.length; i++) {
                if (paragraphs[i] != null && !paragraphs[i].isEmpty()) {
                    // Add hard line breaks so we get readable javadoc
                    String wrappedText = hardWrapTextForJavadoc(paragraphs[i]);
                    jDocComment.append(wrappedText);
                    if (i != paragraphs.length - 1) {
                        jDocComment.append("\n<P>\n");
                    }
                }
            }
        }
        return jDocComment;
    }

    static void hardWrapCommentText(final JDocComment jDocComment) {
        for (int i = 0; i < jDocComment.size(); i++) {
            Object item = jDocComment.get(i);
            if (item instanceof String) {
                jDocComment.set(i, hardWrapTextForJavadoc((String) item));
            }
        }
    }


    /**
     * Hard wraps text to 80 chars for use in javadoc comments.
     * @param text The text to wrap
     * @return The wrapped text
     */
    static String hardWrapTextForJavadoc(final String text) {
        if (text != null && !text.isEmpty()) {
            return WordWrap.from(text)
                    .maxWidth(80)
                    // Add extra word chars to stop it breaking where it shouldn't, e.g. in links
                    .includeExtraWordChars(".#0123456789()_-{}/=\"&")
                    .breakWords(false)
                    .wrap();
        } else {
            return text;
        }
    }
}
