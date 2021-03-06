/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;

import net.gsantner.opoc.util.StringUtils;

import java.util.regex.Matcher;

public class ListHandler implements TextWatcher {
    private final boolean _reorderEnabled;
    private int reorderPosition;
    private boolean triggerReorder = false;


    public ListHandler(final boolean reorderEnabled) {
        super();
        _reorderEnabled = reorderEnabled;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        triggerReorder = triggerReorder || containsNewline(s, start, count);

        // Detects if enter pressed on empty list (correctly handles indent) and marks line for deletion.
        if (count > 0 && start > -1 && start < s.length() && s.charAt(start) == '\n') {

            int iStart = StringUtils.getLineStart(s, start);
            int iEnd = StringUtils.getNextNonWhitespace(s, iStart);

            String previousLine = s.subSequence(iEnd, start).toString();
            Spannable sSpan = (Spannable) s;

            Matcher uMatch = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(previousLine);
            if (uMatch.find() && previousLine.equals(uMatch.group() + " ")) {
                sSpan.setSpan(this, iStart, start + 1, Spanned.SPAN_COMPOSING);
            } else {
                Matcher oMatch = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(previousLine);
                if (oMatch.find()) {
                    if (previousLine.equals(oMatch.group(1) + ". ")) {
                        sSpan.setSpan(this, iStart, start + 1, Spanned.SPAN_COMPOSING);
                    } else {
                        reorderPosition = start;
                    }
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable e) {
        // Deletes spans marked for deletion
        for (Object span : e.getSpans(0, e.length(), this.getClass())) {
            if ((e.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                e.delete(e.getSpanStart(span), e.getSpanEnd(span));
            }
        }
        if (_reorderEnabled && triggerReorder && reorderPosition > 0 && reorderPosition < e.length()) {
            MarkdownAutoFormat.renumberOrderedList(e, reorderPosition);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        triggerReorder = containsNewline(s, start, count);
        reorderPosition = start;
    }

    private boolean containsNewline(CharSequence s, int start, int count) {
        final int end = start + count;
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }
}

