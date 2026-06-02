package mod.hey.studios.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.R;

public class CompileLogHelper {
    private static final String TAG = "CompileLogHelper";

    private static final Pattern ERROR_PATTERN   = Pattern.compile("----------\\n([0-9]+\\. ERROR)", Pattern.MULTILINE);
    private static final Pattern WARNING_PATTERN = Pattern.compile("----------\\n([0-9]+\\. WARNING)", Pattern.MULTILINE);
    private static final Pattern XML_PATTERN     = Pattern.compile("error:", Pattern.MULTILINE);

    // Patterns for human-readable summaries
    private static final Pattern CANNOT_FIND_SYMBOL  = Pattern.compile("cannot find symbol.*?symbol:\\s*(\\S+.+?)\\n.*?location:\\s*(.+?)\\n", Pattern.DOTALL);
    private static final Pattern PKG_NOT_EXIST       = Pattern.compile("package ([\\w.]+) does not exist");
    private static final Pattern CLASS_NOT_FOUND     = Pattern.compile("cannot find symbol\\s+symbol\\s*:\\s*class (\\w+)");
    private static final Pattern METHOD_NOT_FOUND    = Pattern.compile("cannot find symbol\\s+symbol\\s*:\\s*method (\\w+)");
    private static final Pattern MISSING_IMPORT      = Pattern.compile("import ([\\w.]+) cannot be resolved");
    private static final Pattern FORMAT_EXCEPTION    = Pattern.compile("MissingFormatArgumentException.*?'(%\\d+\\$s)'");
    private static final Pattern NullPointer         = Pattern.compile("NullPointerException");
    private static final Pattern LINE_ERROR          = Pattern.compile("at line (\\d+)");

    public static SpannableStringBuilder getColoredLogs(Context context, String logs) {
        int errorColor   = MaterialColors.getColor(context, R.attr.colorError, TAG);
        int warningColor = MaterialColors.getColor(context, R.attr.colorAmber, TAG);
        int summaryColor = MaterialColors.getColor(context, R.attr.colorPrimary, TAG);

        // Build summary first
        List<String> summaries = buildSummary(logs);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (!summaries.isEmpty()) {
            String header = "━━━ WHAT WENT WRONG ━━━\n";
            sb.append(header);
            sb.setSpan(new StyleSpan(Typeface.BOLD),
                    0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(summaryColor),
                    0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            for (String s : summaries) {
                int start = sb.length();
                sb.append("• ").append(s).append("\n");
                sb.setSpan(new ForegroundColorSpan(errorColor),
                        start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append("\n━━━ FULL LOG ━━━\n\n");
        }

        // Append colored full log
        SpannableStringBuilder full = new SpannableStringBuilder(logs);
        applyStyle(full, ERROR_PATTERN.matcher(logs), errorColor);
        applyStyle(full, WARNING_PATTERN.matcher(logs), warningColor);
        applyStyleForXml(full, XML_PATTERN.matcher(logs), errorColor);
        sb.append(full);

        return sb;
    }

    private static List<String> buildSummary(String logs) {
        List<String> out = new ArrayList<>();

        // Package does not exist
        Matcher m = PKG_NOT_EXIST.matcher(logs);
        while (m.find()) {
            out.add("Missing package: " + m.group(1)
                    + " — did you forget to add the library or import?");
        }

        // Cannot find class
        m = CLASS_NOT_FOUND.matcher(logs);
        while (m.find()) {
            out.add("Unknown class: " + m.group(1)
                    + " — check spelling or add the import in your custom block.");
        }

        // Cannot find method
        m = METHOD_NOT_FOUND.matcher(logs);
        while (m.find()) {
            out.add("Unknown method: " + m.group(1)
                    + "() — wrong name or wrong variable type.");
        }

        // MissingFormatArgumentException (custom block code issue)
        m = FORMAT_EXCEPTION.matcher(logs);
        while (m.find()) {
            out.add("Custom block slot mismatch: format specifier "
                    + m.group(1) + " has no matching input — "
                    + "check your block's code template slot count.");
        }

        // NullPointerException at line
        if (NullPointer.matcher(logs).find()) {
            Matcher lm = LINE_ERROR.matcher(logs);
            String line = lm.find() ? " at line " + lm.group(1) : "";
            out.add("NullPointerException" + line
                    + " — a variable is null where a value was expected.");
        }

        // Generic "cannot find symbol" fallback
        if (out.isEmpty() && logs.contains("cannot find symbol")) {
            out.add("One or more symbols (variable, method, or class) could not be found. "
                    + "Check your custom block imports and variable names.");
        }

        return out;
    }

    private static void applyStyle(SpannableStringBuilder sb, Matcher matcher, int color) {
        while (matcher.find()) {
            sb.setSpan(new StyleSpan(Typeface.BOLD),
                    matcher.start(1), matcher.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(color),
                    matcher.start(1), matcher.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static void applyStyleForXml(SpannableStringBuilder sb, Matcher matcher, int color) {
        while (matcher.find()) {
            sb.setSpan(new StyleSpan(Typeface.BOLD),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(color),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}