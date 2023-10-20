package com.fmsys.snapdrop;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebView;

import androidx.annotation.StringRes;

public class WebsiteLocalizer {

    private enum TranslationElement {

        NO_PEERS_INFO("x-no-peers>h2", R.string.website_no_peers_info),
        INSTRUCTION_DESKTOP("x-instructions", "setAttribute('desktop', %s)", R.string.website_instruction),
        INSTRUCTION_MOBILE("x-instructions", "setAttribute('mobile', %s)", R.string.website_instruction),
        FILE_RECEIVED("#receiveDialog>x-background>x-paper>h3", R.string.website_file_received),
        FILE_ASK_EACH_TIME("label[for='autoDownload']", R.string.website_file_ask_before_download),
        FILE_DOWNLOAD("#download", R.string.website_file_download),
        FOOTER_DISCOVERY("footer>div.font-body2", R.string.website_footer_discovery),
        ABOUT_SUBHEADING("x-about>section>div.font-subheading", R.string.website_about_subheading),
        ;

        final String selector;
        final String modifier;
        final @StringRes int translationRes;

        TranslationElement(final String selector, final String modifier, final @StringRes int translationRes) {
            this.selector = selector;
            this.modifier = modifier;
            this.translationRes = translationRes;
        }

        TranslationElement(final String selector, final @StringRes int translationRes) {
            this.selector = selector;
            this.modifier = "innerHTML=%s";
            this.translationRes = translationRes;
        }
    }

    private WebsiteLocalizer() {
        // no instances
    }

    public static void localizeIfNotBuiltIn(final WebView webView) {
        webView.evaluateJavascript("(function() { return !!document.getElementById('language-selector'); })();", localizationBuiltIn -> {
            if (localizationBuiltIn.equals("false")) {
                //Localization is not built into instance. Localize via snapdrop-android.
                localize(webView);
            }
        });
    }

    public static void localize(final WebView webView) {
        for (TranslationElement element : TranslationElement.values()) {
            webView.evaluateJavascript(getTranslationJS(element, webView.getContext()), null);
        }
    }

    private static String getTranslationJS(final TranslationElement element, final Context context) {
        return "javascript: " +
                "var x = document.querySelector(\"" + element.selector + "\")." + String.format(element.modifier, "\"" + TextUtils.htmlEncode(context.getString(element.translationRes)) + "\"") + ";";
    }
}
