package com.eza.spicyex.hooks;

import static com.eza.spicyex.hooks.NativeLyricsUtils.dp;
import static com.eza.spicyex.hooks.NativeLyricsUtils.emptyFallback;
import static com.eza.spicyex.hooks.NativeLyricsUtils.firstNonBlank;
import static com.eza.spicyex.hooks.NativeLyricsUtils.formatMs;
import static com.eza.spicyex.hooks.NativeLyricsUtils.hasJapaneseReading;
import static com.eza.spicyex.hooks.NativeLyricsUtils.isBlank;
import static com.eza.spicyex.hooks.NativeLyricsUtils.isChineseLine;
import static com.eza.spicyex.hooks.NativeLyricsUtils.progress01;
import static com.eza.spicyex.hooks.NativeLyricsUtils.safe;
import static com.eza.spicyex.hooks.NativeLyricsUtils.setTextIfChanged;
import static com.eza.spicyex.hooks.NativeLyricsUtils.shortTrackId;
import static com.eza.spicyex.hooks.NativeLyricsUtils.sideSystemPadding;
import static com.eza.spicyex.hooks.NativeLyricsUtils.sourceProviderLabel;
import static com.eza.spicyex.hooks.NativeLyricsUtils.spToPx;
import static com.eza.spicyex.hooks.NativeLyricsUtils.topSystemPadding;
import static com.eza.spicyex.hooks.NativeLyricsUtils.trackIdFromUri;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.eza.spicyex.BuildStamp;
import com.eza.spicyex.CurrentLyricState;
import com.eza.spicyex.R;
import com.eza.spicyex.References;
import com.eza.spicyex.Settings;
import com.eza.spicyex.SpotifyPlusConfig;
import com.eza.spicyex.SettingsPanel;
import com.eza.spicyex.SettingsStore;
import android.app.Dialog;
import android.view.Window;
import android.graphics.drawable.ColorDrawable;
import com.eza.spicyex.SpotifyTrack;
import com.eza.spicyex.beautifullyrics.entities.LyricsResponseCache;
import com.eza.spicyex.beautifullyrics.entities.LyricsTranslator;
import com.eza.spicyex.beautifullyrics.entities.VsyncFrameScheduler;
import com.eza.spicyex.lyrics.AnimatedLetterState;
import com.eza.spicyex.lyrics.AppliedLine;
import com.eza.spicyex.lyrics.BoundedLyricWindow;
import com.eza.spicyex.lyrics.ChipSpinnerDrawable;
import com.eza.spicyex.lyrics.FrameStyleBatcher;
import com.eza.spicyex.lyrics.GlyphIconDrawable;
import com.eza.spicyex.lyrics.LyricsSkeletonView;
import com.eza.spicyex.lyrics.LyricsAmbientController;
import com.eza.spicyex.lyrics.LyricCaches;
import com.eza.spicyex.lyrics.LyricTimeline;
import com.eza.spicyex.lyrics.LyricVisuals;
import com.eza.spicyex.lyrics.LyricsAnimationApplier;
import com.eza.spicyex.lyrics.LyricsDocumentProcessor;
import com.eza.spicyex.lyrics.LyricsLocalRomanizer;
import com.eza.spicyex.lyrics.LyricsMountedRowWindow;
import com.eza.spicyex.lyrics.LyricsPlaybackClock;
import com.eza.spicyex.lyrics.LyricsRowViewFactory;
import com.eza.spicyex.lyrics.LyricsRowVirtualizer;
import com.eza.spicyex.lyrics.LyricsSecondaryProcessor;
import com.eza.spicyex.lyrics.LyricsShellLifecycle;
import com.eza.spicyex.lyrics.LyricsShellSettings;
import com.eza.spicyex.lyrics.LyricsSpaceView;
import com.eza.spicyex.lyrics.LyricsTapSeekHandler;
import com.eza.spicyex.lyrics.LyricsTextFactory;
import com.eza.spicyex.lyrics.LyricsRepository;
import com.eza.spicyex.lyrics.LyricsParser;
import com.eza.spicyex.lyrics.NativeLyricsSource;
import com.eza.spicyex.lyrics.LyricsDocument;
import com.eza.spicyex.lyrics.LyricsLine;
import com.eza.spicyex.lyrics.SpicyAnimatedTextView;
import com.eza.spicyex.lyrics.SpicyProcessing;
import com.eza.spicyex.lyrics.SpicyTextDetection;
import com.eza.spicyex.lyrics.SyllableSegment;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import okhttp3.OkHttpClient;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.FieldsMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.TAG;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.HTTP;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.PROCESSOR;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.GOOGLE_WORKERS;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.GOOGLE_PROCESSING_VERSION;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.LYRIC_FULL_RENDER_THRESHOLD;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.LYRIC_WINDOW_BEFORE_ACTIVE;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.LYRIC_WINDOW_AFTER_ACTIVE;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.LYRIC_WINDOW_EDGE_BUFFER;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.LYRIC_ESTIMATED_ROW_HEIGHT_DP;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.SCROLL_SETTLE_REMEASURE_DELAY_MS;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.dbg;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.dbgEnter;
import static com.eza.spicyex.hooks.NativeSpicyLyricsHook.createRoundIconButton;
import com.eza.spicyex.hooks.NativeSpicyLyricsHook.LyricsResultCallback;

final class NativeSpicyShellViewImpl extends FrameLayout {
    private final LyricsHost host;
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView title;
    private final TextView subtitle;
    private final TextView progress;
    private final TextView status;
    private final ImageButton romanToggle;
    private final ImageButton translationToggle;
    private final TextView jumpToCurrentButton;
    private final LyricsAmbientController ambientController;
    private final ScrollView lyricsScroll;
    private final FrameLayout lyricsFrame;
    private final LinearLayout lyricsColumn;
    private final LinearLayout mountedRowsHost;
    private final LyricsSpaceView topStaticSpacer;
    private final LyricsSpaceView topVirtualSpacer;
    private final LyricsSpaceView bottomVirtualSpacer;
    private final TextView sourceFooter;
    private final SpotifyPlusConfig config;
    private final FrameStyleBatcher styleBatcher;
    private final LyricsTextFactory textFactory;
    private final LyricsRowViewFactory rowViewFactory;
    private final LyricsShellSettings shellSettings;
    private final LyricsSecondaryProcessor secondaryProcessor;
    private final LyricsShellLifecycle shellLifecycle;
    private final LyricsAnimationApplier.StyleSink animationStyleSink = new LyricsAnimationApplier.StyleSink() {
        @Override
        public void applyAlpha(View view, float alpha) {
            applyAlphaIfChanged(view, alpha);
        }

        @Override
        public void applyScale(View view, float scaleX, float scaleY) {
            applyScaleIfChanged(view, scaleX, scaleY);
        }

        @Override
        public void applyTranslationY(View view, float translationY) {
            applyTranslationYIfChanged(view, translationY);
        }
    };
    private LinearLayout contentColumn;
    private final LyricsMountedRowWindow mountedRows = new LyricsMountedRowWindow();
    private final Runnable scrollSettleRunnable = () -> {
        remeasureMountedRows();
        renderWindowForActive(currentWindowAnchor());
    };
    private final BoundedLyricWindow lyricWindow = new BoundedLyricWindow(
            LYRIC_FULL_RENDER_THRESHOLD,
            LYRIC_WINDOW_BEFORE_ACTIVE,
            LYRIC_WINDOW_AFTER_ACTIVE);
    private String lastUri = "";
    private String loadingTrackId = "";
    private LyricsDocument document;
    private int activeIndex = -2;
    private int[] rowHeightPrefix;
    private boolean running;
    private boolean scrollWindowRenderScheduled;
    private boolean showRomanization;
    private boolean showTranslation;
    private String japaneseReadingMode; // effective (cycle resolved to a concrete mode)
    private String chineseMode;         // effective
    private String jpModeConfig;        // configured JP mode ("cycle" or a fixed value) — drives chip behavior
    private String cnModeConfig;        // configured CN mode
    private boolean localModeProcessing;
    private boolean localModeProcessingPending;
    private boolean lastAttachTransliterationToWords;
    private String lastLineSpacingMode;
    private String lastLineSyncFillMode;
    private long lastTransliterationCheckMs;
    private long userScrollHoldUntilMs;
    private final LyricsPlaybackClock playbackClock;
    private SpotifyTrack throttledTrack;
    private long throttledTrackAtMs;
    private boolean cachedLineGradientEnabled;
    private boolean cachedLineBlurEnabled;
    private float cachedBlurQuality;
    private boolean cachedInterludeNoteIcon;
    private boolean cachedToggleSpinnerEnabled;
    private final ChipSpinnerDrawable romanSpinner;
    private final ChipSpinnerDrawable translationSpinner;
    // Spinner shows only after work has been pending this long, so taps that resolve within a frame
    // never flash a partial spinner. 0 = not currently pending.
    private static final long SPINNER_SHOW_DELAY_MS = 180L;
    private long romanPendingSinceMs;
    private long translationPendingSinceMs;
    private final GlyphIconDrawable romanGlyph = new GlyphIconDrawable("A", android.graphics.Typeface.DEFAULT_BOLD);
    private int lyricsTopInsetPx;
    private boolean cachedBoldLyrics;
    private String cachedTextSizeMode;
    private float cachedTextSizeMultiplier = 1f;

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    private int chromeButtonDp() {
        return isLandscape() ? 36 : 40;
    }

    private int lyricsTopPaddingDp() {
        return isLandscape() ? 10 : 22;
    }

    private int lyricsBottomPaddingDp() {
        return isLandscape() ? 86 : 118;
    }

    // Start the first lyric line near screen center (the active line is kept centered as the song
    // plays, so the opening line should begin centered too, not pinned to the top). The top pad is
    // ~0.44 of the viewport — far larger than the status-bar/cutout inset, so the safe-area concern
    // is subsumed. Falls back to screen height before the scroll view is laid out.
    private void applyLyricsScrollPadding() {
        if (lyricsScroll == null) return;
        int safeTop = lyricsTopInsetPx + dp(lyricsTopPaddingDp());
        int viewport = lyricsScroll.getHeight();
        if (viewport <= 0) viewport = getResources().getDisplayMetrics().heightPixels;
        // ~half a viewport (minus half a row) above the first line and below the last, so the opening
        // and closing lines can sit at screen center just like every line in between (the active-line
        // scroll keeps the current line centered).
        int center = Math.max(0, viewport / 2 - dp(56));
        lyricsScroll.setPadding(0, Math.max(safeTop, center), 0, Math.max(dp(lyricsBottomPaddingDp()), center));
    }

    private int computeSafeTopInset(WindowInsets insets) {
        if (insets == null) return lyricsTopInsetPx;
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                return bars.top;
            }
            int top = insets.getSystemWindowInsetTop();
            if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                top = Math.max(top, insets.getDisplayCutout().getSafeInsetTop());
            }
            return top;
        } catch (Throwable t) {
            return lyricsTopInsetPx;
        }
    }

    private TextView createJumpToCurrentButton(Context context) {
        TextView view = textFactory.createChip(context, "↓");
        view.setTextSize(13);
        view.setAlpha(0f);
        view.setVisibility(GONE);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(22));
        bg.setColor(Color.argb(210, 36, 36, 36));
        view.setBackground(bg);
        view.setElevation(dp(8));
        view.setOnClickListener(v -> resumeFollowCurrentLine());
        return view;
    }

    private final VsyncFrameScheduler frameScheduler = new VsyncFrameScheduler(deltaTimeSeconds -> {
        if (!running) return;
        float dt = deltaTimeSeconds <= 0d ? (1f / 60f) : (float) Math.max(0.001d, Math.min(0.08d, deltaTimeSeconds));
        updateState(dt);
    });

    NativeSpicyShellViewImpl(LyricsHost host, Activity activity) {
        super(activity);
        this.host = host;
        this.activity = activity;
        this.romanSpinner = new ChipSpinnerDrawable(activity);
        this.translationSpinner = new ChipSpinnerDrawable(activity);
        this.playbackClock = new LyricsPlaybackClock(host::readBestMeasuredProgressMs);
        this.config = SpotifyPlusConfig.from(activity);
        this.styleBatcher = new FrameStyleBatcher(activity);
        this.textFactory = new LyricsTextFactory(activity, config);
        this.rowViewFactory = new LyricsRowViewFactory(activity, textFactory);
        this.shellSettings = new LyricsShellSettings(activity, config);
        this.secondaryProcessor = new LyricsSecondaryProcessor(activity, HTTP, PROCESSOR, GOOGLE_WORKERS, handler, GOOGLE_PROCESSING_VERSION);
        this.ambientController = new LyricsAmbientController(activity, HTTP, config);
        this.shellLifecycle = new LyricsShellLifecycle(activity, () -> {
            host.markExplicitLyricsExit(activity);
            activity.finish();
        });
        SharedPreferences prefs = activity.getSharedPreferences("SpotifyPlus", Context.MODE_PRIVATE);
        showRomanization = config.get(Settings.NATIVE_SPICY_ROMANIZATION);
        showTranslation = config.get(Settings.NATIVE_SPICY_TRANSLATION);
        jpModeConfig = config.get(Settings.JAPANESE_READING_MODE);
        japaneseReadingMode = "cycle".equals(jpModeConfig) ? SpotifyPlusConfig.JP_READING_FURIGANA_ROMAJI : jpModeConfig;
        cnModeConfig = config.get(Settings.CHINESE_MODE);
        chineseMode = LyricsShellSettings.normalizeChineseMode("cycle".equals(cnModeConfig) ? SpotifyPlusConfig.CHINESE_MODE_PINYIN : cnModeConfig);
        lastAttachTransliterationToWords = shellSettings.attachTransliterationToWordsEnabled();
        lastLineSpacingMode = shellSettings.lineSpacingMode();
        cachedLineGradientEnabled = config.get(Settings.ENABLE_LINE_GRADIENT);
        cachedLineBlurEnabled = config.get(Settings.ENABLE_LINE_BLUR);
        cachedBlurQuality = shellSettings.lineBlurQualityMultiplier();
        cachedInterludeNoteIcon = "note".equals(config.get(Settings.INTERLUDE_ICON));
        cachedToggleSpinnerEnabled = config.get(Settings.TOGGLE_PROGRESS_RING);
        cachedBoldLyrics = shellSettings.lyricsBoldEnabled();
        cachedTextSizeMode = shellSettings.lyricsTextSizeMode();
        cachedTextSizeMultiplier = shellSettings.lyricsTextSizeMultiplier();
        // Seed with a status-bar-height estimate; the WindowInsets listener refines it with the
        // real safe-area top (status bar + display cutout) once insets dispatch on attach.
        lyricsTopInsetPx = topSystemPadding(activity);

        setBackground(ambientController.pageBackground());
        setClickable(true);
        setFocusable(true);
        ambientController.attachAnimatedLayer(this);

        contentColumn = new LinearLayout(activity);
        contentColumn.setOrientation(LinearLayout.VERTICAL);
        contentColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        contentColumn.setClipChildren(false);
        contentColumn.setClipToPadding(false);
        contentColumn.setPadding(sideSystemPadding(activity), 0, sideSystemPadding(activity), dp(isLandscape() ? 6 : 10));
        addView(contentColumn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClipToPadding(false);
        header.setPadding(sideSystemPadding(activity), topSystemPadding(activity), sideSystemPadding(activity), 0);
        addView(header, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        int chromeButton = chromeButtonDp();
        TextView back = textFactory.createText(activity, "‹", isLandscape() ? 30 : 32, Color.WHITE, textFactory.resolveTypeface(false));
        back.setGravity(Gravity.CENTER);
        back.setAlpha(0.92f);
        back.setOnClickListener(v -> {
            host.markExplicitLyricsExit(activity);
            activity.finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(chromeButton), dp(chromeButton)));

        TextView headerTitle = textFactory.createText(activity, "", 15, Color.WHITE, textFactory.resolveTypeface(true));
        headerTitle.setAlpha(0f);
        header.addView(headerTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        romanToggle = createRoundIconButton(activity, R.drawable.ic_spicy_romanization, "Toggle transliteration", chromeButton, isLandscape() ? 11 : 12);
        // Per-script romanization glyph instead of the fixed kana drawable — signals "romanize
        // whatever's on screen", and reflects the active line's script (updateRomanizationGlyph).
        romanToggle.setImageDrawable(romanGlyph);
        romanToggle.setOnClickListener(v -> cycleTransliterationMode(prefs));
        header.addView(romanToggle, new LinearLayout.LayoutParams(dp(chromeButton), dp(chromeButton)));

        translationToggle = createRoundIconButton(activity, R.drawable.ic_spicy_translation, "Toggle translation", chromeButton, isLandscape() ? 9 : 10);
        translationToggle.setOnClickListener(v -> {
            showTranslation = !showTranslation;
            prefs.edit().putBoolean(Settings.NATIVE_SPICY_TRANSLATION.key, showTranslation).apply();
            updateToggleVisuals();
            renderDocument();
        });
        LinearLayout.LayoutParams transLp = new LinearLayout.LayoutParams(dp(chromeButton), dp(chromeButton));
        transLp.leftMargin = dp(isLandscape() ? 6 : 8);
        header.addView(translationToggle, transLp);

        // In-Spotify settings entry (⚙), rightmost in the header. Works under both LSPosed and
        // LSPatch since it writes Spotify-side prefs the hook reads directly (no standalone app).
        ImageButton settingsButton = createRoundIconButton(activity, R.drawable.ic_spicy_romanization, "Spicy EX settings", chromeButton, isLandscape() ? 11 : 12);
        settingsButton.setImageDrawable(new GlyphIconDrawable("⚙", android.graphics.Typeface.DEFAULT));
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(dp(chromeButton), dp(chromeButton));
        settingsLp.leftMargin = dp(isLandscape() ? 6 : 8);
        header.addView(settingsButton, settingsLp);
        // Progress rings drawn over the chip borders while their background work runs. They paint
        // nothing while inactive, so leaving them as foregrounds is free until setActive(true).
        romanToggle.setForeground(romanSpinner);
        translationToggle.setForeground(translationSpinner);
        updateToggleVisuals();

        title = textFactory.createText(activity, "Waiting for Spotify track…", 18, Color.WHITE, textFactory.resolveTypeface(true));
        title.setVisibility(GONE);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        title.setAlpha(0.92f);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(0);
        contentColumn.addView(title, titleLp);

        subtitle = textFactory.createText(activity, "Open playback, then fullscreen lyrics", 13, Color.rgb(190, 190, 190), textFactory.resolveTypeface(false));
        subtitle.setVisibility(GONE);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setMaxLines(1);
        subtitle.setAlpha(0.72f);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleLp.topMargin = dp(0);
        contentColumn.addView(subtitle, subtitleLp);

        lyricsScroll = new ScrollView(activity);
        lyricsScroll.setFillViewport(false);
        lyricsScroll.setClipToPadding(false);
        lyricsScroll.setClipChildren(false);
        applyLyricsScrollPadding();
        lyricsScroll.setVerticalFadingEdgeEnabled(false);
        lyricsScroll.setFadingEdgeLength(0);
        lyricsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        lyricsScroll.setVerticalScrollBarEnabled(false);
        lyricsScroll.setOnTouchListener(new LyricsTapSeekHandler(
                activity,
                config,
                untilMs -> userScrollHoldUntilMs = untilMs,
                this::seekNearestLineAt));
        lyricsFrame = new FrameLayout(activity);
        lyricsColumn = new LinearLayout(activity);
        lyricsColumn.setOrientation(LinearLayout.VERTICAL);
        lyricsColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        lyricsColumn.setClipChildren(false);
        lyricsColumn.setClipToPadding(false);

        topStaticSpacer = new LyricsSpaceView(activity, dp(96));
        topVirtualSpacer = new LyricsSpaceView(activity, 0);
        mountedRowsHost = new LinearLayout(activity);
        mountedRowsHost.setOrientation(LinearLayout.VERTICAL);
        mountedRowsHost.setGravity(Gravity.CENTER_HORIZONTAL);
        mountedRowsHost.setClipChildren(false);
        mountedRowsHost.setClipToPadding(false);
        bottomVirtualSpacer = new LyricsSpaceView(activity, 0);
        sourceFooter = textFactory.createText(activity, "", 12, Color.rgb(125, 125, 125), textFactory.resolveTypeface(false));
        sourceFooter.setGravity(Gravity.CENTER);
        sourceFooter.setAlpha(0.56f);
        sourceFooter.setPadding(dp(16), dp(38), dp(16), dp(180));

        ensureLyricsColumnScaffold();
        lyricsScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
            scheduleScrollWindowRender();
            scheduleScrollSettleRemeasure();
        });
        lyricsScroll.addView(lyricsColumn, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lyricsFrame.addView(lyricsScroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        jumpToCurrentButton = createJumpToCurrentButton(activity);
        FrameLayout.LayoutParams jumpLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44), Gravity.BOTTOM | Gravity.END);
        jumpLp.setMargins(0, 0, dp(14), dp(24));
        lyricsFrame.addView(jumpToCurrentButton, jumpLp);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = 0;
        contentColumn.addView(lyricsFrame, scrollLp);

        progress = textFactory.createText(activity, "--:--", 13, Color.rgb(210, 210, 210), textFactory.resolveTypeface(true));
        progress.setVisibility(GONE);
        progress.setGravity(Gravity.CENTER);
        progress.setAlpha(0.72f);
        contentColumn.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = textFactory.createText(activity, "Spicy native renderer", 12, Color.rgb(160, 160, 160), textFactory.resolveTypeface(false));
        status.setVisibility(GONE);
        status.setGravity(Gravity.CENTER);
        status.setMaxLines(3);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        status.setAlpha(0.62f);
        statusLp.topMargin = dp(0);
        contentColumn.addView(status, statusLp);

        // Refine the lyric top inset from real window insets (status bar + cutout) once they
        // dispatch on attach. Returned unconsumed so nothing else is starved of insets.
        setOnApplyWindowInsetsListener((v, insets) -> {
            int top = computeSafeTopInset(insets);
            if (top > 0 && top != lyricsTopInsetPx) {
                lyricsTopInsetPx = top;
                applyLyricsScrollPadding();
            }
            return insets;
        });
    }

    void start() {
        dbgEnter("NativeSpicyShellView.start");
        if (running) return;
        running = true;
        shellLifecycle.start();
        playbackClock.reset("");
        updateState(1f / 60f);
        frameScheduler.start();
    }

    void stop() {
        dbgEnter("NativeSpicyShellView.stop");
        running = false;
        romanSpinner.setActive(false);
        translationSpinner.setActive(false);
        shellLifecycle.stop();
        frameScheduler.stop();
        playbackClock.reset("");
        handler.removeCallbacks(scrollSettleRunnable);
        handler.removeCallbacksAndMessages(null);
        clearPendingStyleWrites();
    }

    // The reflective player-state walk in host.getCurrentTrackSafely() is too expensive for every
    // vsync frame; refresh at ~4Hz. Position interpolation reads live player state through
    // PlaybackClock separately, so the only cost is up to 250ms of track-change latency.
    private SpotifyTrack currentTrackThrottled() {
        long now = SystemClock.elapsedRealtime();
        if (throttledTrack == null || now - throttledTrackAtMs >= 250) {
            throttledTrack = host.getCurrentTrackSafely();
            throttledTrackAtMs = now;
        }
        return throttledTrack;
    }

    private void updateState(float deltaSeconds) {
        SpotifyTrack track = currentTrackThrottled();
        boolean playingNow = host.isPlayerActuallyPlaying();
        updateJumpToCurrentVisibility();
        updateToggleSpinners();
        if (track == null) {
            setTextIfChanged(title, "Waiting for Spotify track…");
            setTextIfChanged(subtitle, "Player state hook has not emitted yet");
            setTextIfChanged(progress, "--:--");
            setTextIfChanged(status, "Native Spicy renderer mounted. Waiting for player state.");
            return;
        }

        String uri = safe(track.uri);
        if (!uri.equals(lastUri)) {
            lastUri = uri;
            playbackClock.reset(uri);
            activeIndex = -2;
            document = null;
            String id = trackIdFromUri(uri);
            ambientController.updateForTrack(track, () -> running);
            XposedBridge.log(TAG + " active track uri=" + uri + " title=\"" + safe(track.title) + "\"");
            showLoading("Loading lyrics…");
            loadLyrics(track, id);
        }
        long pos = playbackClock.getPosition(track, playingNow);

        setTextIfChanged(title, emptyFallback(track.title, "Unknown title"));
        setTextIfChanged(subtitle, emptyFallback(track.artist, "Unknown artist") + " • " + emptyFallback(track.album, "Unknown album"));
        setTextIfChanged(progress, formatMs(pos));

        if (document != null) {
            // Throttle settings reads off the per-frame path (they do prefs/bridge lookups);
            // renderer layout toggles don't need 60fps responsiveness, ~once/sec is plenty.
            long nowMs = SystemClock.elapsedRealtime();
            if (nowMs - lastTransliterationCheckMs > 750) {
                lastTransliterationCheckMs = nowMs;
                cachedLineGradientEnabled = config.get(Settings.ENABLE_LINE_GRADIENT);
                cachedLineBlurEnabled = config.get(Settings.ENABLE_LINE_BLUR);
                cachedBlurQuality = shellSettings.lineBlurQualityMultiplier();
                cachedToggleSpinnerEnabled = config.get(Settings.TOGGLE_PROGRESS_RING);
                boolean remountRows = false;
                boolean interludeNote = "note".equals(config.get(Settings.INTERLUDE_ICON));
                if (interludeNote != cachedInterludeNoteIcon) {
                    cachedInterludeNoteIcon = interludeNote;
                    remountRows = true;
                }
                boolean bold = shellSettings.lyricsBoldEnabled();
                if (bold != cachedBoldLyrics) {
                    cachedBoldLyrics = bold;
                    remountRows = true;
                }
                String textSize = shellSettings.lyricsTextSizeMode();
                if (!safe(textSize).equals(cachedTextSizeMode)) {
                    cachedTextSizeMode = textSize;
                    cachedTextSizeMultiplier = shellSettings.lyricsTextSizeMultiplier();
                    remountRows = true;
                }
                boolean attachTransliteration = shellSettings.attachTransliterationToWordsEnabled();
                if (attachTransliteration != lastAttachTransliterationToWords) {
                    lastAttachTransliterationToWords = attachTransliteration;
                    remountRows = true;
                }
                String spacing = shellSettings.lineSpacingMode();
                if (!safe(spacing).equals(lastLineSpacingMode)) {
                    lastLineSpacingMode = spacing;
                    remountRows = true;
                }
                String fill = shellSettings.lineSyncFillMode();
                if (!safe(fill).equals(lastLineSyncFillMode)) {
                    lastLineSyncFillMode = fill;
                    remountRows = true;
                }
                if (remountRows) {
                    clearRenderedLineViews();
                    renderWindowForActive(activeIndex >= 0 ? activeIndex : currentWindowAnchor());
                }
            }
            int nextActive = LyricTimeline.findPrimaryActiveRow(document.appliedLines, pos);
            if (nextActive != activeIndex) {
                setActiveLine(nextActive, pos, track);
            }
            applySpicyRendererState(pos, nextActive, deltaSeconds);
            String processingStatus = "";
            if (document.processingPending) {
                processingStatus = document.romanizationPending && document.translationPending ? " [P:R+Tr]"
                        : document.romanizationPending ? " [P:R]"
                        : document.translationPending ? " [P:Tr]"
                        : " [P:...]";
            }
            setTextIfChanged(status, (playingNow ? "Playing" : "Paused")
                    + processingStatus
                    + " • " + document.fetchSource
                    + " • " + document.provider
                    + " • " + document.type
                    + " • " + document.appliedLines.size() + " rows");
        } else if (!loadingTrackId.isEmpty()) {
            setTextIfChanged(status, (playingNow ? "Playing" : "Paused") + " • fetching lyrics for " + shortTrackId(uri));
        }
    }

    private void loadLyrics(SpotifyTrack track, String id) {
        dbg("NativeSpicyShellView.loadLyrics", "id=" + safe(id) + " track=" + (track == null ? "null" : safe(track.uri)));
        loadingTrackId = id;
        int generation = ++NativeSpicyLyricsHook.fetchGeneration;
        host.fetchLyrics(activity, track, generation, new LyricsResultCallback() {
            @Override
            public void onSuccess(LyricsDocument doc) {
                handler.post(() -> {
                    if (!running) return;
                    if (generation < NativeSpicyLyricsHook.fetchGeneration && !id.equals(loadingTrackId)) return;
                    SpotifyTrack current = host.getCurrentTrackSafely();
                    String currentId = current == null ? "" : trackIdFromUri(current.uri);
                    if (!id.equals(currentId)) {
                        XposedBridge.log(TAG + " stale lyrics ignored id=" + id + " current=" + currentId);
                        return;
                    }
                    LyricsDocumentProcessor.applyProcessedCache(activity, doc, chineseMode, GOOGLE_PROCESSING_VERSION);
                    document = doc;
                    loadingTrackId = "";
                    renderDocument();
                    startSecondaryProcessing(id, generation);
                    XposedBridge.log(TAG + " lyrics loaded source=" + doc.fetchSource + " provider=" + doc.provider + " type=" + doc.type + " lines=" + doc.lines.size());
                });
            }

            @Override
            public void onError(String error) {
                handler.post(() -> {
                    if (!running) return;
                    SpotifyTrack current = host.getCurrentTrackSafely();
                    String currentId = current == null ? "" : trackIdFromUri(current.uri);
                    if (!id.equals(currentId)) return;
                    // A document may already be on screen (cache-first instant render) while
                    // the rest of the chain was probing for an upgrade. A terminal fallback
                    // failure must not replace rendered lyrics with an error screen.
                    if (document != null && !document.lines.isEmpty()) {
                        setTextIfChanged(status, "Lyrics refresh failed: " + safe(error));
                        return;
                    }
                    showError(error);
                });
            }
        });
    }

    private void startSecondaryProcessing(String id, int generation) {
        dbg("NativeSpicyShellView.startSecondaryProcessing", "id=" + safe(id) + " generation=" + generation);
        LyricsDocument snapshot = document;
        if (snapshot == null || snapshot.lines.isEmpty() || !snapshot.processingPending) return;

        String label = (snapshot.romanizationPending ? "readings" : "")
                + (snapshot.romanizationPending && snapshot.translationPending ? " + " : "")
                + (snapshot.translationPending ? "translation" : "");
        status.setText("Enhancing " + label + "…");

        final String targetLang = config.get(Settings.TRANSLATION_TARGET);
        final String sourceLangOverride = "manual".equalsIgnoreCase(config.get(Settings.SOURCE_LANGUAGE_MODE))
                ? config.get(Settings.SOURCE_LANGUAGE) : null;
        final String effectiveSourceLang = sourceLangOverride != null ? sourceLangOverride : snapshot.language;

        XposedBridge.log(TAG + " secondary processing start target=" + targetLang + " source=" + effectiveSourceLang);
        secondaryProcessor.start(id, generation, snapshot, showRomanization, chineseMode, targetLang, effectiveSourceLang,
                this::isCurrentProcessingResult,
                new LyricsSecondaryProcessor.Callback() {
                    @Override
                    public void rerender(String message) {
                        if (!running || document != snapshot) return;
                        rerenderKeepingPosition(message);
                    }

                    @Override
                    public void progress(String message) {
                        if (!running || document != snapshot) return;
                        refreshSecondaryRows(message);
                    }

                    @Override
                    public void complete(String message, int changed) {
                        if (!running || document != snapshot) return;
                        refreshSecondaryRows(message);
                        LyricsDocumentProcessor.saveProcessedCache(activity, snapshot, chineseMode, GOOGLE_PROCESSING_VERSION);
                        XposedBridge.log(TAG + " secondary processing complete changed=" + changed + " lines=" + snapshot.lines.size());
                    }
                });
    }

    private boolean isCurrentProcessingResult(String id, int generation, LyricsDocument snapshot) {
        if (!running || document != snapshot) return false;
        if (generation > 0 && generation < NativeSpicyLyricsHook.fetchGeneration) return false;
        return isBlank(id) || id.equals(trackIdFromUri(lastUri));
    }

    private void populateLocalSegmentRomanization(LyricsDocument doc) {
        if (!showRomanization || doc == null || doc.lines == null || doc.lines.isEmpty()) return;
        String fullText = LyricsDocumentProcessor.collectText(doc);
        for (LyricsLine line : doc.lines) {
            if (line == null || line.interlude || isBlank(line.text)) continue;
            LyricsLocalRomanizer.populateLocalSegmentRomanization(chineseMode, doc, line, fullText);
        }
    }

    private void rerenderKeepingPosition(String message) {
        if (document == null) return;
        SpotifyTrack current = host.getCurrentTrackSafely();
        long pos = current == null ? -1 : playbackClock.getPosition(current, host.isPlayerActuallyPlaying());
        renderDocument();
        if (current != null) setActiveLine(LyricTimeline.findPrimaryActiveRow(document.appliedLines, pos), pos, current);
        if (!isBlank(message)) status.setText(message);
    }

    private void showLoading(String message) {
        lyricsColumn.removeAllViews();
        mountedRows.reset(mountedRowsHost);
        invalidateRowHeightPrefix();
        mountedRows.markDirty();
        activeIndex = -2;
        if (config.get(Settings.SHOW_SKELETON)) {
            LyricsSkeletonView skeleton = new LyricsSkeletonView(activity);
            LinearLayout.LayoutParams skeletonLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            skeletonLp.topMargin = dp(72);
            lyricsColumn.addView(skeleton, skeletonLp);
            return;
        }
        TextView loading = textFactory.createText(activity, message, 22, Color.rgb(179, 179, 179), textFactory.resolveTypeface(true));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(dp(16), dp(100), dp(16), dp(16));
        lyricsColumn.addView(loading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void showError(String error) {
        document = null;
        loadingTrackId = "";
        lyricsColumn.removeAllViews();
        mountedRows.reset(mountedRowsHost);
        invalidateRowHeightPrefix();
        mountedRows.markDirty();
        activeIndex = -2;
        TextView title = textFactory.createText(activity, "No lyrics found", 24, Color.WHITE, textFactory.resolveTypeface(true));
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(16), dp(80), dp(16), dp(8));
        lyricsColumn.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView message = textFactory.createText(activity, safe(error), 14, Color.rgb(179, 179, 179), textFactory.resolveTypeface(false));
        message.setGravity(Gravity.CENTER);
        message.setPadding(dp(16), dp(4), dp(16), dp(16));
        lyricsColumn.addView(message, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        status.setText("Lyrics error: " + safe(error));
    }

    private void renderDocument() {
        dbg("NativeSpicyShellView.renderDocument", "doc=" + (document == null ? "null" : document.fetchSource + "/" + document.type + "/" + document.lines.size()));
        updateToggleVisuals();
        ensureLyricsColumnScaffold();
        clearRenderedLineViews();
        activeIndex = -2;
        if (document == null || document.lines.isEmpty()) {
            showError("Empty lyrics response");
            return;
        }
        populateLocalSegmentRomanization(document);
        LyricTimeline.applySyncedRows(document);
        lastAttachTransliterationToWords = shellSettings.attachTransliterationToWordsEnabled();
        lastLineSpacingMode = shellSettings.lineSpacingMode();
        lastLineSyncFillMode = shellSettings.lineSyncFillMode();
        if (document.appliedLines.isEmpty()) {
            showError("Empty applied lyrics rows");
            return;
        }
        sourceFooter.setText("lyrics provided by " + sourceProviderLabel(document.provider));
        mountedRows.markDirty();
        renderWindowForActive(0);
    }

    private void ensureLyricsColumnScaffold() {
        if (topStaticSpacer.getParent() == lyricsColumn) return;
        lyricsColumn.removeAllViews();
        lyricsColumn.addView(topStaticSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lyricsColumn.addView(topVirtualSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lyricsColumn.addView(mountedRowsHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lyricsColumn.addView(bottomVirtualSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lyricsColumn.addView(sourceFooter, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void renderWindowForActive(int active) {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
        ensureLyricsColumnScaffold();
        int anchor = currentWindowAnchor();
        if (active >= 0 && SystemClock.elapsedRealtime() >= userScrollHoldUntilMs) anchor = active;
        BoundedLyricWindow.Range range = lyricWindow.rangeFor(document.appliedLines.size(), anchor);
        if (mountedRows.matches(range)) {
            updateVirtualSpacerHeights();
            return;
        }
        mountedRows.render(
                range,
                document.appliedLines,
                mountedRowsHost,
                activeIndex,
                this::ensureRowView,
                this::onNewRowMounted,
                this::invalidateLineVisualCaches,
                this::styleLine);
        updateVirtualSpacerHeights();
        flushStyleBatch();
    }

    private View ensureRowView(AppliedLine line) {
        return line.rowView != null ? line.rowView : buildLyricRow(line);
    }

    private void onNewRowMounted(AppliedLine line) {
        invalidateLineVisualCaches(line);
        remeasureLine(line);
    }

    private int currentWindowAnchor() {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return 0;
        if (SystemClock.elapsedRealtime() < userScrollHoldUntilMs) return currentViewportAnchor();
        return activeIndex >= 0 ? activeIndex : currentViewportAnchor();
    }

    private int currentViewportAnchor() {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return 0;
        int center = lyricsScroll.getScrollY() + Math.max(1, lyricsScroll.getHeight()) / 2;
        int offset = Math.max(0, center - topStaticSpacer.getHeight());
        return findLineIndexForOffset(offset);
    }

    private int findLineIndexForOffset(int offsetPx) {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return 0;
        return LyricsRowVirtualizer.findLineIndexForOffset(rowHeightPrefix(), offsetPx, document.appliedLines.size());
    }

    private void updateVirtualSpacerHeights() {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) {
            topVirtualSpacer.setHeightPx(0);
            bottomVirtualSpacer.setHeightPx(0);
            return;
        }
        int count = document.appliedLines.size();
        LyricsRowVirtualizer.SpacerHeights heights = LyricsRowVirtualizer.spacerHeights(
                rowHeightPrefix(), count, mountedRows.start(), mountedRows.end());
        topVirtualSpacer.setHeightPx(heights.top);
        bottomVirtualSpacer.setHeightPx(heights.bottom);
    }

    private int[] rowHeightPrefix() {
        if (rowHeightPrefix != null) return rowHeightPrefix;
        rowHeightPrefix = LyricsRowVirtualizer.buildRowHeightPrefix(
                document == null ? null : document.appliedLines,
                this::rowHeightForIndex);
        return rowHeightPrefix;
    }

    private void invalidateRowHeightPrefix() {
        rowHeightPrefix = null;
    }

    private int rowHeightForIndex(int index) {
        if (document == null || document.appliedLines == null || index < 0 || index >= document.appliedLines.size()) return 0;
        AppliedLine line = document.appliedLines.get(index);
        if (line == null) return 0;
        if (line.measuredHeightPx > 0) return line.measuredHeightPx;
        int estimate = (int) (dp(LYRIC_ESTIMATED_ROW_HEIGHT_DP) * shellSettings.lineSpacingMultiplier() * cachedTextSizeMultiplier);
        if (!line.bgLine && showRomanization && !isBlank(line.romanizedText)) estimate += dp(18);
        if (!line.bgLine && showTranslation && !isBlank(line.translatedText)) estimate += dp(18);
        return estimate;
    }

    private void remeasureMountedRows() {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
        boolean changed = false;
        for (int index : mountedRows.mountedIndices()) {
            AppliedLine line = document.appliedLines.get(index);
            if (remeasureLine(line)) changed = true;
        }
        if (changed) updateVirtualSpacerHeights();
    }

    private boolean remeasureLine(AppliedLine line) {
        if (line == null || line.rowView == null || !line.rowView.isAttachedToWindow()) return false;
        int height = line.rowView.getHeight();
        if (height <= 0 || Math.abs(line.measuredHeightPx - height) < 1) return false;
        line.measuredHeightPx = height;
        invalidateRowHeightPrefix();
        return true;
    }

    private void scheduleScrollWindowRender() {
        if (scrollWindowRenderScheduled) return;
        scrollWindowRenderScheduled = true;
        Runnable work = () -> {
            scrollWindowRenderScheduled = false;
            if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
            int anchor = currentViewportAnchor();
            if (shouldRemountWindowForViewport(anchor)) renderWindowForActive(anchor);
        };
        if (Build.VERSION.SDK_INT >= 16) lyricsScroll.postOnAnimation(work);
        else lyricsScroll.post(work);
    }

    private boolean shouldRemountWindowForViewport(int anchor) {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return false;
        return LyricsRowVirtualizer.shouldRemountWindowForViewport(
                document.appliedLines.size(),
                mountedRows.dirty(),
                mountedRows.start(),
                mountedRows.end(),
                LYRIC_FULL_RENDER_THRESHOLD,
                LYRIC_WINDOW_EDGE_BUFFER,
                anchor);
    }

    private void scheduleScrollSettleRemeasure() {
        handler.removeCallbacks(scrollSettleRunnable);
        handler.postDelayed(scrollSettleRunnable, SCROLL_SETTLE_REMEASURE_DELAY_MS);
    }

    // Sentence-synced lines have no word timing, so per-word transliteration ("attach to words")
    // normally falls back to a single line-level romaji. When the setting is on, split the line into
    // words (space-delimited scripts only — CJK keeps line-level), distribute the line's timing across
    // them for a left-to-right sweep, and let segmentRomanizedText romanize each. Lets aligned romaji
    // work without word-level timing. Synthesized words are flagged so they're dropped if toggled off.
    private void ensureAlignedWordsForSentenceSync(AppliedLine line) {
        boolean attach = shellSettings.attachTransliterationToWordsEnabled();
        if (line == null || line.dotLine || line.bgLine) return;
        if (line.syntheticWords && (!attach || !showRomanization)) {
            line.words.clear();
            line.syntheticWords = false;
            return;
        }
        if (!attach || !showRomanization) return;
        if (!line.words.isEmpty()) return;           // real word-level timing — leave it
        if (hasJapaneseReading(line)) return;        // JP furigana path handles per-char readings
        String text = line.text == null ? "" : line.text.trim();
        if (text.isEmpty() || isBlank(line.romanizedText)) return; // need a romanizable line
        if (!text.contains(" ")) return;             // only space-delimited scripts
        String[] parts = text.split("\\s+");
        if (parts.length < 2) return;
        int totalChars = 0;
        for (String p : parts) totalChars += Math.max(1, p.length());
        long span = Math.max(1, line.endMs - line.startMs);
        long cursor = line.startMs;
        int acc = 0;
        for (int i = 0; i < parts.length; i++) {
            acc += Math.max(1, parts[i].length());
            long end = (i == parts.length - 1) ? line.endMs : line.startMs + span * acc / totalChars;
            SyllableSegment seg = new SyllableSegment();
            seg.text = parts[i];
            seg.startMs = cursor;
            seg.endMs = Math.max(cursor + 1, end);
            seg.totalMs = seg.endMs - seg.startMs;
            seg.partOfWord = false; // gets the inter-word spacing margin
            line.words.add(seg);
            cursor = seg.endMs;
        }
        line.syntheticWords = true;
    }

    private LinearLayout buildLyricRow(AppliedLine line) {
        ensureAlignedWordsForSentenceSync(line);
        LyricsRowViewFactory.Options options = new LyricsRowViewFactory.Options();
        options.lineSpacingMultiplier = shellSettings.lineSpacingMultiplier();
        options.showRomanization = showRomanization;
        options.showTranslation = showTranslation;
        options.showJapaneseFurigana = LyricsShellSettings.showJapaneseFurigana(japaneseReadingMode);
        options.showJapaneseRomaji = LyricsShellSettings.showJapaneseRomaji(japaneseReadingMode);
        options.attachTransliterationToWords = shellSettings.attachTransliterationToWordsEnabled();
        options.lineLevelFillTopDown = shellSettings.lineSyncFillTopDown();
        options.interludeNoteIcon = cachedInterludeNoteIcon;
        options.boldLyrics = cachedBoldLyrics;
        options.textSizeMultiplier = cachedTextSizeMultiplier;
        boolean useSyllableWords = line != null && line.words != null && !line.words.isEmpty();
        boolean japaneseLine = hasJapaneseReading(line);
        boolean showJapaneseFurigana = japaneseLine && showRomanization && options.showJapaneseFurigana;
        boolean showAlignedRomaji = useSyllableWords
                && !showJapaneseFurigana
                && options.attachTransliterationToWords
                && showRomanization
                && !isBlank(line.romanizedText);
        options.documentText = showAlignedRomaji ? LyricsDocumentProcessor.collectText(document) : "";
        return rowViewFactory.build(line, options, this::segmentRomanizedText, () -> {
            invalidateRowHeightPrefix();
            updateVirtualSpacerHeights();
        });
    }

    // Google enhancement only touches LINE-LEVEL romanized/translated text, so mounted rows
    // can be refreshed in place: update existing secondary TextViews, and rebuild only the
    // individual rows that need a secondary view they don't have yet. The previous full
    // rerenderKeepingPosition() every 12 translated lines tore down every mounted row, reset
    // all springs (visible re-fade flicker) and snapped the scroll. The LOCAL pass keeps its
    // single full rerender — per-word segment romanization changes row structure wholesale.
    private void refreshSecondaryRows(String message) {
        LyricsDocument snapshot = document;
        if (snapshot == null || snapshot.appliedLines == null || snapshot.appliedLines.isEmpty()) {
            if (!isBlank(message)) setTextIfChanged(status, message);
            return;
        }
        boolean structureChanged = false;
        for (AppliedLine row : snapshot.appliedLines) {
            // bg rows carry the background line's own text; the lead sourceLine must not
            // overwrite it. Dot rows have no secondary text at all.
            if (row == null || row.dotLine || row.bgLine || row.sourceLine == null) continue;
            String roman = safe(row.sourceLine.romanizedText);
            String translated = safe(row.sourceLine.translatedText);
            boolean romanChanged = !roman.equals(row.romanizedText);
            boolean translatedChanged = !translated.equals(row.translatedText);
            if (!romanChanged && !translatedChanged) continue;
            row.romanizedText = roman;
            row.translatedText = translated;
            if (row.rowView == null) continue; // unbuilt; picks the new text up when built
            boolean needsNewViews =
                    (romanChanged && showRomanization && !roman.isEmpty() && row.romanView == null)
                            || (translatedChanged && showTranslation && !translated.isEmpty() && row.translationView == null);
            if (needsNewViews) {
                clearRowViewState(row);
                structureChanged = true;
            } else {
                if (romanChanged && row.romanView != null) row.romanView.setText(roman);
                if (translatedChanged && row.translationView != null) row.translationView.setText(translated);
            }
        }
        if (structureChanged) {
            invalidateRowHeightPrefix();
            mountedRows.markDirty();
            renderWindowForActive(currentWindowAnchor());
        }
        if (!isBlank(message)) setTextIfChanged(status, message);
    }

    private void clearRowViewState(AppliedLine line) {
        if (line == null) return;
        if (line.rowView != null && line.rowView.getParent() == mountedRowsHost) {
            mountedRowsHost.removeView(line.rowView);
        }
        invalidateLineVisualCaches(line);
        line.rowView = null;
        line.mainView = null;
        line.romanView = null;
        line.translationView = null;
        line.dotViews = null;
        line.opacitySpring = null;
        line.lineScaleSpring = null;
        line.measuredHeightPx = 0;
        if (line.words == null) return;
        for (SyllableSegment seg : line.words) {
            if (seg == null) continue;
            seg.view = null;
            seg.textView = null;
            if (seg.letters != null) {
                for (AnimatedLetterState letter : seg.letters) {
                    if (letter == null) continue;
                    letter.view = null;
                    letter.scaleSpring = null;
                    letter.ySpring = null;
                    letter.glowSpring = null;
                }
            }
            seg.romanizedTextView = null;
        }
    }

    private void clearRenderedLineViews() {
        if (document != null && document.appliedLines != null) {
            for (AppliedLine line : document.appliedLines) {
                clearRowViewState(line);
            }
        }
        invalidateRowHeightPrefix();
        mountedRows.reset(mountedRowsHost);
        topVirtualSpacer.setHeightPx(0);
        bottomVirtualSpacer.setHeightPx(0);
    }

    private void applyAlphaIfChanged(View view, float alpha) {
        styleBatcher.applyAlphaIfChanged(view, alpha);
    }

    private void applyScaleIfChanged(View view, float scaleX, float scaleY) {
        styleBatcher.applyScaleIfChanged(view, scaleX, scaleY);
    }

    private void applyTranslationYIfChanged(View view, float translationY) {
        styleBatcher.applyTranslationYIfChanged(view, translationY);
    }

    private void queueBlurIfChanged(View view, float blurPx, float epsilon) {
        styleBatcher.queueBlurIfChanged(view, blurPx, epsilon);
    }

    private void flushStyleBatch() {
        styleBatcher.flush();
    }

    private void clearPendingStyleWrites() {
        styleBatcher.clearPendingWrites();
    }

    private void invalidateStyleCacheRecursive(View view) {
        styleBatcher.invalidateRecursive(view);
    }

    private void invalidateLineVisualCaches(AppliedLine line) {
        if (line == null) return;
        invalidateStyleCacheRecursive(line.rowView);
        invalidateStyleCacheRecursive(line.mainView);
        invalidateStyleCacheRecursive(line.romanView);
        invalidateStyleCacheRecursive(line.translationView);
        if (line.dotViews != null) {
            for (SpicyAnimatedTextView dot : line.dotViews) invalidateStyleCacheRecursive(dot);
        }
        if (line.words == null) return;
        for (SyllableSegment seg : line.words) {
            if (seg == null) continue;
            invalidateStyleCacheRecursive(seg.view);
            invalidateStyleCacheRecursive(seg.textView);
            invalidateStyleCacheRecursive(seg.romanizedTextView);
            if (seg.letters == null) continue;
            for (AnimatedLetterState letter : seg.letters) {
                if (letter != null) invalidateStyleCacheRecursive(letter.view);
            }
        }
    }

    private void seekNearestLineAt(float yInScroll) {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
        int contentY = lyricsScroll.getScrollY() + Math.round(yInScroll);
        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        // Compare in scroll-content (lyricsColumn) coordinates, mirroring the auto-scroll fix
        // in setActiveLine: row.getTop() is relative to mountedRowsHost, which sits below the
        // static + virtual spacers. Only mounted rows have valid coordinates — unmounted rows
        // keep a cached rowView with stale layout from an earlier window placement.
        android.graphics.Rect r = new android.graphics.Rect();
        for (int i : mountedRows.mountedIndices()) {
            if (i < 0 || i >= document.appliedLines.size()) continue;
            AppliedLine line = document.appliedLines.get(i);
            if (line == null || line.rowView == null || line.dotLine) continue;
            if (line.rowView.getParent() != mountedRowsHost) continue;
            r.set(0, 0, line.rowView.getWidth(), line.rowView.getHeight());
            lyricsColumn.offsetDescendantRectToMyCoords(line.rowView, r);
            int center = r.top + Math.max(1, r.height()) / 2;
            int distance = Math.abs(contentY - center);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) seekToLine(document.appliedLines.get(bestIndex), bestIndex);
    }

    private void seekToLine(AppliedLine line, int index) {
        if (line == null || line.startMs < 0) return;
        long target = Math.max(0, line.startMs);
        userScrollHoldUntilMs = 0;
        boolean ok = host.seekSpotifyTo(target);
        if (ok) {
            NativeSpicyLyricsHook.mediaPositionMs = target;
            NativeSpicyLyricsHook.mediaPositionUpdatedAtElapsedMs = SystemClock.elapsedRealtime();
            NativeSpicyLyricsHook.seekOverrideUntilElapsedMs = SystemClock.elapsedRealtime() + 1800;
            playbackClock.forcePosition(target, host.isPlayerActuallyPlaying());
            setActiveLine(index, target, host.getCurrentTrackSafely());
            applySpicyRendererState(target, index, 1f / 60f);
            XposedBridge.log(TAG + " seek line index=" + index + " ms=" + target);
        } else {
            userScrollHoldUntilMs = SystemClock.elapsedRealtime() + 2500;
            XposedBridge.log(TAG + " seek line failed index=" + index + " ms=" + target);
        }
    }

    private void setActiveLine(int index, long positionMs, SpotifyTrack track) {
        int old = activeIndex;
        if (document != null && index >= 0) {
            boolean activeVisible = mountedRows.containsIndex(index);
            if (!activeVisible || SystemClock.elapsedRealtime() < userScrollHoldUntilMs) {
                renderWindowForActive(index);
                if (!activeVisible) old = -1;
            }
        }
        activeIndex = index;
        updateRomanizationGlyph();
        styleLine(old, false);
        styleLine(index, true);
        flushStyleBatch();
        if (document == null || index < 0 || index >= document.appliedLines.size()) return;

        AppliedLine line = document.appliedLines.get(index);
        CurrentLyricState.updateLine(track, document.provider, document.language, line.dotLine ? "" : line.text, line.dotLine ? "" : line.romanizedText, line.dotLine ? "" : line.translatedText, positionMs, index, host.isPlayerActuallyPlaying(), "active");

        if (SystemClock.elapsedRealtime() < userScrollHoldUntilMs) return;
        View row = line.rowView;
        if (row == null || row.getParent() != mountedRowsHost) return;
        lyricsScroll.post(() -> {
            if (row.getParent() != mountedRowsHost) return;
            // The scroll target must be in scroll-content (lyricsColumn) coordinates. row.getTop()
            // is only relative to mountedRowsHost, which sits below topStaticSpacer + topVirtualSpacer;
            // as the bounded window slides down the track, topVirtualSpacer grows large, so scrolling
            // to the raw row.getTop() lands inside the empty spacer -> blank screen, active line off-view.
            android.graphics.Rect r = new android.graphics.Rect(0, 0, row.getWidth(), row.getHeight());
            lyricsColumn.offsetDescendantRectToMyCoords(row, r);
            // r.top is in lyricsColumn content coords; the scroll content is offset by the scroll
            // view's top padding (clipToPadding=false), so the scroll position that centers the row
            // must include paddingTop. Omitting it left the active line paddingTop below center
            // (~0.44 screen → bottom) once the center-start padding got large.
            int target = lyricsScroll.getPaddingTop() + r.top - (lyricsScroll.getHeight() / 2) + (row.getHeight() / 2);
            lyricsScroll.smoothScrollTo(0, Math.max(0, target));
        });
    }

    private void styleLine(int index, boolean active) {
        if (document == null || index < 0 || index >= document.appliedLines.size()) return;
        AppliedLine line = document.appliedLines.get(index);
        int base = line.baseTextSp > 0 ? line.baseTextSp : LyricVisuals.lyricTextSizeSp(line.text);
        int color = line.bgLine ? Color.rgb(170, 170, 170) : Color.WHITE;
        // Float properties (alpha/scale/translation) MUST go through the style batch:
        // direct setters desync the batch's last-applied cache and make it skip later
        // legitimate updates. Text color/size/shadow are not batched fields and stay direct.
        if (line.mainView != null) {
            line.mainView.setTextColor(color);
            line.mainView.setTextSize(base);
            applyAlphaIfChanged(line.mainView, 1.0f);
            line.mainView.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }
        if (line.words != null) {
            for (SyllableSegment seg : line.words) {
                if (seg == null || seg.view == null) continue;
                applyAlphaIfChanged(seg.view, 1.0f);
                applyScaleIfChanged(seg.view, 0.95f, 0.95f);
                applyTranslationYIfChanged(seg.view, 0f);
                if (seg.textView != null) {
                    seg.textView.setTextColor(color);
                    seg.textView.setTextSize(base);
                    seg.textView.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                }
                if (seg.letters != null) {
                    for (AnimatedLetterState letter : seg.letters) {
                        if (letter == null || letter.view == null) continue;
                        letter.view.setTextColor(color);
                        letter.view.setTextSize(base);
                        applyScaleIfChanged(letter.view, 1.0f, 1.0f);
                        applyTranslationYIfChanged(letter.view, 0f);
                        applyAlphaIfChanged(letter.view, 1.0f);
                        letter.view.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                        letter.view.setGradientPosition(-20f, 0f);
                    }
                }
            }
        }
    }

    private void applySpicyRendererState(long positionMs, int active, float deltaSeconds) {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
        for (int i : mountedRows.mountedIndices()) {
            if (i < 0 || i >= document.appliedLines.size()) continue;
            AppliedLine line = document.appliedLines.get(i);
            if (line == null || line.rowView == null || line.rowView.getParent() != mountedRowsHost) continue;
            // Time-based: several rows can be active at once (lead + background vocal, a
            // line extended across a short gap). Keying animation off the single primary
            // index left background vocals permanently un-animated.
            boolean lineActive = LyricTimeline.isRowActiveAt(line, positionMs);
            boolean sung = positionMs >= line.endMs;
            float opacity = LyricsAnimationApplier.stepLineOpacity(line, lineActive, sung, deltaSeconds);
            applyAlphaIfChanged(line.rowView, opacity);
            applyBlurEffect(line.rowView, mobileLineBlurPx(line, i, active, positionMs));

            if (line.mainView != null) {
                // Line-level (no per-word) row: spring the zoom so it eases instead of snapping
                // ("blink"), and flow the configured gradient axis over the line duration.
                float targetScale = lineActive ? 1.0f : 0.95f;
                float s = LyricsAnimationApplier.stepLineScale(line, targetScale, deltaSeconds);
                updateLineScalePivot(line);
                applyScaleIfChanged(line.mainView, s, s);
                // The karaoke fill runs to the line's REAL end (fillEndMs); line.endMs may
                // be extended into a short gap purely for active-window purposes.
                float lineGradient = lineActive
                        ? (-20f + 120f * progress01(positionMs, line.startMs, LyricTimeline.fillEndMs(line)))
                        : (sung ? 100f : -20f);
                line.mainView.setGradientPosition(lineGradient, lineActive ? 0.22f : 0f);
            }
            if (line.dotLine) {
                if (lineActive) {
                    LyricsAnimationApplier.animateInterludeDots(line, positionMs, deltaSeconds, spToPx(31), animationStyleSink);
                } else {
                    LyricsAnimationApplier.resetInterludeDots(line, animationStyleSink);
                }
            } else {
                applySecondaryGradient(line, positionMs);
                if (lineActive) {
                    LyricsAnimationApplier.animateSyllables(
                            line,
                            positionMs,
                            deltaSeconds,
                            spToPx(line.baseTextSp > 0 ? line.baseTextSp : LyricVisuals.lyricTextSizeSp(line.text)),
                            animationStyleSink);
                } else {
                    LyricsAnimationApplier.resetSyllables(line, animationStyleSink);
                }
            }
        }
        flushStyleBatch();
    }

    private void updateLineScalePivot(AppliedLine line) {
        if (line == null || line.mainView == null) return;
        int width = line.mainView.getWidth();
        int height = line.mainView.getHeight();
        if (width <= 0 || height <= 0) return;
        float pivotX = line.oppositeAligned ? width : 0f;
        float pivotY = height * 0.5f;
        if (Math.abs(line.mainView.getPivotX() - pivotX) > 0.5f) line.mainView.setPivotX(pivotX);
        if (Math.abs(line.mainView.getPivotY() - pivotY) > 0.5f) line.mainView.setPivotY(pivotY);
    }

    private void resumeFollowCurrentLine() {
        if (document == null || document.appliedLines == null || document.appliedLines.isEmpty()) return;
        SpotifyTrack track = host.getCurrentTrackSafely();
        long pos = track == null ? -1 : playbackClock.getPosition(track, host.isPlayerActuallyPlaying());
        int index = pos >= 0 ? LyricTimeline.findPrimaryActiveRow(document.appliedLines, pos) : activeIndex;
        if (index < 0 || index >= document.appliedLines.size()) return;
        userScrollHoldUntilMs = 0;
        renderWindowForActive(index);
        setActiveLine(index, Math.max(0, pos), track);
        applySpicyRendererState(Math.max(0, pos), index, 1f / 60f);
        updateJumpToCurrentVisibility();
    }

    // Open the in-Spotify settings panel as a centered FLOATING CARD (not edge-to-edge, so there's
    // no status-bar gap to fight). Pause the lyric render loop + animated background while it's open
    // so the modal is smooth; resume on dismiss. The panel writes Spotify-side prefs; the resumed
    // per-second refresh applies the changes after closing.
    private void showSettingsDialog() {
        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(new SettingsPanel(activity, new SettingsStore(activity), dialog::dismiss).build());
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
                int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.84f);
                window.setLayout(w, h); // centered by default → floats with margins, no system-bar gap
                window.setDimAmount(0.6f);
            }
            // Quiet the live render behind the modal (the main source of sluggishness).
            frameScheduler.stop();
            ambientController.pauseAnimation();
            dialog.setOnDismissListener(d -> {
                frameScheduler.start();
                onSettingsClosed();
            });
            dialog.show();
        } catch (Throwable t) {
            XposedBridge.log(TAG + " settings dialog failed: " + t);
        }
    }

    // Re-read the settings that the in-Spotify panel can change but the per-frame refresh doesn't
    // track (the JP/CN mode configs are captured at mount), and apply the animated-background toggle.
    private void onSettingsClosed() {
        try {
            String jpCfg = safe(config.get(Settings.JAPANESE_READING_MODE));
            if (!jpCfg.equals(jpModeConfig)) {
                jpModeConfig = jpCfg;
                japaneseReadingMode = "cycle".equals(jpCfg) ? SpotifyPlusConfig.JP_READING_FURIGANA_ROMAJI : jpCfg;
                updateToggleVisuals();
                renderDocument();
            }
            String cnCfg = safe(config.get(Settings.CHINESE_MODE));
            if (!cnCfg.equals(cnModeConfig)) {
                cnModeConfig = cnCfg;
                chineseMode = LyricsShellSettings.normalizeChineseMode(
                        "cycle".equals(cnCfg) ? SpotifyPlusConfig.CHINESE_MODE_PINYIN : cnCfg);
                reprocessLocalModeOnly("cn mode setting changed");
            }
            ambientController.applyEnabled(config.get(Settings.ENABLE_BACKGROUND));
        } catch (Throwable t) {
            XposedBridge.log(TAG + " onSettingsClosed failed: " + t);
        }
    }

    // Spin the chip rings while their work is outstanding: initial fetch, or background
    // romanization/translation enhancement. Reads only in-memory state, so it's cheap per frame.
    private void updateToggleSpinners() {
        if (!cachedToggleSpinnerEnabled) {
            romanPendingSinceMs = 0L;
            translationPendingSinceMs = 0L;
            romanSpinner.setActive(false);
            translationSpinner.setActive(false);
            return;
        }
        long now = SystemClock.elapsedRealtime();
        boolean loading = !loadingTrackId.isEmpty();
        boolean romanPending = showRomanization
                && romanToggle.getVisibility() == View.VISIBLE
                && (loading || localModeProcessing || (document != null && document.romanizationPending));
        boolean translationPending = showTranslation
                && translationToggle.getVisibility() == View.VISIBLE
                && (loading || (document != null && document.translationPending));

        // Defer showing the spinner until work has been pending past a short threshold, so a tap
        // that resolves within a frame or two never flashes a partial spinner.
        if (romanPending) {
            if (romanPendingSinceMs == 0L) romanPendingSinceMs = now;
        } else {
            romanPendingSinceMs = 0L;
        }
        if (translationPending) {
            if (translationPendingSinceMs == 0L) translationPendingSinceMs = now;
        } else {
            translationPendingSinceMs = 0L;
        }
        romanSpinner.setActive(romanPendingSinceMs != 0L && now - romanPendingSinceMs >= SPINNER_SHOW_DELAY_MS);
        translationSpinner.setActive(translationPendingSinceMs != 0L && now - translationPendingSinceMs >= SPINNER_SHOW_DELAY_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        romanSpinner.setActive(false);
        translationSpinner.setActive(false);
        super.onDetachedFromWindow();
    }

    private void updateJumpToCurrentVisibility() {
        if (jumpToCurrentButton == null) return;
        boolean show = document != null && activeIndex >= 0 && SystemClock.elapsedRealtime() < userScrollHoldUntilMs;
        int targetVisibility = show ? View.VISIBLE : View.GONE;
        if (jumpToCurrentButton.getVisibility() != targetVisibility) {
            jumpToCurrentButton.setVisibility(targetVisibility);
        }
        jumpToCurrentButton.setAlpha(show ? 0.92f : 0f);
    }

    private void applySecondaryGradient(AppliedLine line, long positionMs) {
        if (line == null) return;
        boolean animate = cachedLineGradientEnabled; // refreshed on the 750ms settings tick
        // Fork rule: the TRANSLITERATION row MAY karaoke-animate (it tracks the reading of the sung
        // line), but the TRANSLATION row is ALWAYS static — its phrasing doesn't line up with the
        // sung timing, so sweeping it looks wrong.
        if (line.romanView != null) {
            float g = animate ? secondaryLineGradientPosition(line, positionMs) : 100f;
            float glow = animate && positionMs >= line.startMs && positionMs < line.endMs ? 0.10f : 0f;
            line.romanView.setGradientPosition(g, glow);
        }
        if (line.translationView != null) line.translationView.setGradientPosition(100f, 0f);
        // Per-syllable aligned romaji also matches word timing, so it may sweep with the words.
        applyAlignedRomanizedGradient(line, positionMs, animate);
    }

    private float secondaryLineGradientPosition(AppliedLine line, long positionMs) {
        if (line == null || positionMs < line.startMs) return -20f;
        long fillEnd = LyricTimeline.fillEndMs(line);
        if (positionMs >= fillEnd) return 100f;
        return -20f + 120f * progress01(positionMs, line.startMs, fillEnd);
    }

    private void applyAlignedRomanizedGradient(AppliedLine line, long positionMs, boolean animated) {
        if (line == null || line.words == null) return;
        for (SyllableSegment seg : line.words) {
            if (seg == null || seg.romanizedTextView == null) continue;
            if (!animated) {
                seg.romanizedTextView.setGradientPosition(100f, 0f);
                continue;
            }
            float gradient = secondarySegmentGradientPosition(seg, positionMs);
            float glow = positionMs >= seg.startMs && positionMs < seg.endMs ? 0.18f : 0f;
            seg.romanizedTextView.setGradientPosition(gradient, glow);
        }
    }

    private String segmentRomanizedText(AppliedLine line, SyllableSegment seg, String fullText) {
        if (seg == null || isBlank(seg.text)) return "";
        if (!isBlank(seg.romanizedText)) return seg.romanizedText;
        LyricsLine source = line == null ? null : line.sourceLine;
        String local = LyricsLocalRomanizer.romanizeText(chineseMode, document, seg.text, fullText, source == null ? "" : source.chineseMode);
        if (!isBlank(local) && !local.equals(seg.text) && !SpicyTextDetection.hasRomanizableScript(local)) {
            seg.romanizedText = local;
            return local;
        }
        return "";
    }

    private float secondarySegmentGradientPosition(SyllableSegment seg, long positionMs) {
        if (seg == null || positionMs < seg.startMs) return -20f;
        if (positionMs >= seg.endMs) return 100f;
        return -20f + 120f * progress01(positionMs, seg.startMs, seg.endMs);
    }

    private float mobileLineBlurPx(AppliedLine line, int index, int active, long positionMs) {
        if (line == null || Build.VERSION.SDK_INT < 31) return 0f;
        if (!cachedLineBlurEnabled) return 0f;
        if (SystemClock.elapsedRealtime() < userScrollHoldUntilMs) return 0f;
        float quality = cachedBlurQuality;
        if (quality <= 0f) return 0f;
        // Rows stay active across small gaps now (LyricTimeline extends their window), so
        // active is only -1 in genuinely rowless moments (e.g. a sub-3s intro) — no blur then.
        if (active < 0) return 0f;
        int distance = Math.abs(index - active);
        if (distance <= 1) return 0f;
        boolean emphasized = safe(line.text).codePointCount(0, safe(line.text).length()) <= 12 || line.dotLine;
        float max = emphasized ? 1.0f : 1.8f;
        float weighted = max * Math.min(1f, distance / 4f);
        if (distance == 2) weighted *= 0.55f;
        return weighted * quality;
    }

    private void applyBlurEffect(View view, float blurPx) {
        queueBlurIfChanged(view, blurPx, 0.25f);
    }

    private void cycleTransliterationMode(SharedPreferences prefs) {
        boolean jp = documentHasJapanese();
        boolean cn = documentHasChinese();
        if (jp) {
            if ("cycle".equals(jpModeConfig)) {
                // Cycle: off -> furigana -> romaji -> both -> off. In-screen only; never overwrites
                // the configured "cycle" preference.
                if (!showRomanization) {
                    showRomanization = true;
                    japaneseReadingMode = SpotifyPlusConfig.JP_READING_FURIGANA_ONLY;
                } else if (SpotifyPlusConfig.JP_READING_FURIGANA_ONLY.equals(japaneseReadingMode)) {
                    japaneseReadingMode = SpotifyPlusConfig.JP_READING_ROMAJI_ONLY;
                } else if (SpotifyPlusConfig.JP_READING_ROMAJI_ONLY.equals(japaneseReadingMode)) {
                    japaneseReadingMode = SpotifyPlusConfig.JP_READING_FURIGANA_ROMAJI;
                } else {
                    showRomanization = false;
                }
            } else {
                // Fixed mode: the chip only toggles the (locked) reading on/off.
                showRomanization = !showRomanization;
            }
            prefs.edit().putBoolean(Settings.NATIVE_SPICY_ROMANIZATION.key, showRomanization).apply();
            reprocessLocalModeOnly("jp transliteration mode");
            return;
        }
        if (cn) {
            if ("cycle".equals(cnModeConfig)) {
                if (!showRomanization) {
                    showRomanization = true;
                    chineseMode = SpotifyPlusConfig.CHINESE_MODE_PINYIN;
                } else if (SpotifyPlusConfig.CHINESE_MODE_PINYIN.equals(LyricsShellSettings.normalizeChineseMode(chineseMode))) {
                    chineseMode = SpotifyPlusConfig.CHINESE_MODE_JYUTPING;
                } else {
                    showRomanization = false;
                }
            } else {
                showRomanization = !showRomanization;
            }
            prefs.edit().putBoolean(Settings.NATIVE_SPICY_ROMANIZATION.key, showRomanization).apply();
            reprocessLocalModeOnly("cn transliteration mode");
            return;
        }
        showRomanization = !showRomanization;
        prefs.edit().putBoolean(Settings.NATIVE_SPICY_ROMANIZATION.key, showRomanization).apply();
        reprocessLocalModeOnly("transliteration mode");
    }

    private void reprocessLocalModeOnly(String reason) {
        LyricsDocument snapshot = document;
        if (snapshot == null || snapshot.lines == null || snapshot.lines.isEmpty()) {
            updateToggleVisuals();
            renderDocument();
            return;
        }
        if (localModeProcessing) {
            localModeProcessingPending = true;
            return;
        }
        localModeProcessing = true;
        secondaryProcessor.reprocessLocal(snapshot, showRomanization, chineseMode, reason,
                this::isCurrentProcessingResult,
                (completedReason, changed, current) -> {
                    localModeProcessing = false;
                    if (!current) return;
                    rerenderKeepingPosition(completedReason + " ready");
                    XposedBridge.log(TAG + " local mode reprocess complete changed=" + changed + " reason=" + completedReason);
                    if (localModeProcessingPending) {
                        localModeProcessingPending = false;
                        reprocessLocalModeOnly(completedReason + " pending");
                    }
                });
    }

    private boolean documentHasJapanese() {
        return document != null && SpicyTextDetection.detectPresentScripts(LyricsDocumentProcessor.collectText(document), document.language, "")
                .contains(SpicyTextDetection.Script.JAPANESE);
    }

    private boolean documentHasChinese() {
        return document != null && SpicyTextDetection.detectPresentScripts(LyricsDocumentProcessor.collectText(document), document.language, "")
                .contains(SpicyTextDetection.Script.CHINESE);
    }

    private boolean documentHasRomanizableScript() {
        return document != null && !SpicyTextDetection.detectPresentScripts(LyricsDocumentProcessor.collectText(document), document.language, "").isEmpty();
    }

    private boolean documentHasTranslationCandidate() {
        if (document == null || document.lines == null) return false;
        for (LyricsLine line : document.lines) {
            if (line == null || isBlank(line.text) || line.interlude) continue;
            if (!isBlank(line.translatedText)) return true;
            if (SpicyProcessing.shouldTranslateLine(line.text, document.language, "en")) return true;
        }
        return false;
    }

    // Off  -> neutral "A" (chip rendered in the dim/disabled state by styleIconChip).
    // On   -> the glyph for whatever script is on screen (あ / 拼·粤 / 가 / Я / Ω), shown in the
    //         bright "enabled" state. Prefers the active line's script, falling back to the
    //         document's dominant present script.
    private void updateRomanizationGlyph() {
        romanGlyph.setGlowing(showRomanization);
        if (!showRomanization) {
            romanGlyph.setGlyph("A");
            return;
        }
        String source = "";
        if (document != null && activeIndex >= 0 && activeIndex < document.appliedLines.size()) {
            AppliedLine line = document.appliedLines.get(activeIndex);
            if (line != null && !line.dotLine && !isBlank(line.text)) source = line.text;
        }
        if (isBlank(source) && document != null) source = LyricsDocumentProcessor.collectText(document);
        romanGlyph.setGlyph(romanizationGlyphFor(source));
    }

    private String romanizationGlyphFor(String text) {
        String language = document == null ? "" : document.language;
        List<SpicyTextDetection.Script> scripts = SpicyTextDetection.detectPresentScripts(text, language, "");
        if (!scripts.isEmpty()) {
            switch (scripts.get(0)) {
                case JAPANESE: return "あ";
                case CHINESE:
                    return SpotifyPlusConfig.CHINESE_MODE_JYUTPING.equals(LyricsShellSettings.normalizeChineseMode(chineseMode)) ? "粤" : "拼";
                case KOREAN: return "한";
                case CYRILLIC: return "Я";
                case GREEK: return "Ω";
            }
        }
        return "A";
    }

    private void updateToggleVisuals() {
        boolean jp = documentHasJapanese();
        boolean cn = documentHasChinese();
        boolean romanizable = documentHasRomanizableScript();
        romanToggle.setVisibility(romanizable ? View.VISIBLE : View.GONE);
        updateRomanizationGlyph();
        romanToggle.setContentDescription(jp ? "Toggle Japanese reading" : cn ? "Toggle Chinese transliteration" : "Toggle transliteration");
        textFactory.styleIconChip(romanToggle, showRomanization);
        translationToggle.setVisibility(documentHasTranslationCandidate() ? View.VISIBLE : View.GONE);
        textFactory.styleIconChip(translationToggle, showTranslation);
    }
}
