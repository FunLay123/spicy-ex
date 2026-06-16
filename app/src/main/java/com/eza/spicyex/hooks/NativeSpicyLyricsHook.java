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
import com.eza.spicyex.SpotifyTrack;
import com.eza.spicyex.beautifullyrics.entities.LyricsResponseCache;
import com.eza.spicyex.beautifullyrics.entities.LyricsTranslator;
import com.eza.spicyex.beautifullyrics.entities.VsyncFrameScheduler;
import com.eza.spicyex.lyrics.AnimatedLetterState;
import com.eza.spicyex.lyrics.AppliedLine;
import com.eza.spicyex.lyrics.BoundedLyricWindow;
import com.eza.spicyex.lyrics.LiveLyricCardView;
import com.eza.spicyex.lyrics.FrameStyleBatcher;
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

/**
 * Native Spicy shell.
 *
 * Current alpha scope:
 * - mount Android-native renderer root in Spotify fullscreen lyrics activity,
 * - bridge Spotify track/progress/play state,
 * - fetch Spicy Lyrics API response with existing Spotify auth capture,
 * - render static/line/syllable payloads as native line-synced lyrics,
 * - show romanized secondary lines using Spicy fork processing ports.
 */
public class NativeSpicyLyricsHook extends SpotifyHook implements LyricsHost {
    static final String TAG = "[SpotifyPlusSpicy]";
    private static final String BUILD_CLUE = BuildStamp.CLUE;
    private static final boolean DEBUG_LOGGING = false;
    private static final String LYRICS_FULLSCREEN_ACTIVITY = "com.spotify.lyrics.fullscreenview.page.LyricsFullscreenPageActivity";
    private static final int TAG_NATIVE_SPICY_ROOT = 0x53504C53; // SPLS
    private static final int TAG_EXTRA_LYRICS_BUTTON = 0x53504C58; // SPLX
    private static final int TAG_LIVE_CARD = 0x53504C43; // SPLC — our 3-line now-playing lyric card
    private static final WeakHashMap<Activity, NowPlayingLyricController> NP_CONTROLLERS = new WeakHashMap<>();
    // W5 / D4: set our OWN client timeouts so a hung/slow Spicy upstream fails over promptly to
    // the native -> LRCLIB cascade instead of waiting indefinitely. This HTTP client is shared by
    // every lyrics call (Spicy /query, Spotify color-lyrics, LRCLIB, Google translate) — all are
    // small JSON, so a 15s read budget is ample and won't truncate any legitimate response.
    private static final int HTTP_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int HTTP_READ_TIMEOUT_SECONDS = 15;
    private static final int HTTP_WRITE_TIMEOUT_SECONDS = 10;
    static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    static final ExecutorService PROCESSOR = Executors.newSingleThreadExecutor();
    static final ExecutorService GOOGLE_WORKERS = Executors.newFixedThreadPool(4);
    private static final String PREFS_MAIN = "SpotifyPlus";
    private static final String PREFS_DEPLOY_STATE = "SpotifyPlusNativeDeployState";
    private static final String KEY_LAST_CACHE_CLEAR_VERSION = "last_cache_clear_version";
    private static final String KEY_LAST_SPOTIFY_ACCESS_TOKEN = "native_spotify_access_token";
    // Spotify's auth-token classes (kept names; fields are proto-style). The access token lives in
    // EsAccessToken$AccessToken.token_ (obfuscated getter), which the OkHttp-header capture misses.
    private static final String[] SPOTIFY_AUTH_TOKEN_CLASSES = {
            "com.spotify.authentication.login5esperanto.EsAccessToken$AccessToken",
            "com.spotify.authentication.login5esperanto.EsAccessTokenClient$AccessTokenResponse",
            "com.spotify.authentication.oauth.AccessToken",
            "com.spotify.authentication.tokenexchangeimpl.model.TokenResponse",
            "com.spotify.authentication.tokenexchangeesperanto.EsTokenExchange$TokenExchangeResponse"
    };
    private static final String[] ACCESS_TOKEN_FIELD_HINTS = {
            "accessToken", "accessToken_", "access_token",
            "token_" // EsAccessToken$AccessToken stores the access token here (proto field)
    };
    static final int GOOGLE_PROCESSING_VERSION = SpicyProcessing.PROCESSING_VERSION + 2;
    static final int LYRIC_FULL_RENDER_THRESHOLD = 72;
    static final int LYRIC_WINDOW_BEFORE_ACTIVE = 18;
    static final int LYRIC_WINDOW_AFTER_ACTIVE = 24;
    static final int LYRIC_WINDOW_EDGE_BUFFER = 7;
    static final int LYRIC_ESTIMATED_ROW_HEIGHT_DP = 74;
    static final long SCROLL_SETTLE_REMEASURE_DELAY_MS = 140;
    private static final int AUTH_REQUEST_DEBUG_LIMIT = 20;
    private static final int AUTH_HEADER_DEBUG_LIMIT = 24;
    private static final long KEEP_LYRICS_ACTIVITY_AFTER_MOUNT_MS = 3500L;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private static volatile boolean isPlaying;
    static volatile long mediaPositionMs = -1;
    static volatile long mediaPositionUpdatedAtElapsedMs = 0;
    static volatile long seekOverrideUntilElapsedMs = 0;
    static volatile int fetchGeneration;
    private static volatile int authRequestDebugCount;
    private static volatile int authHeaderDebugCount;
    private static final WeakHashMap<Activity, Long> EXPLICIT_LYRICS_EXIT_UNTIL_MS = new WeakHashMap<>();
    private static final WeakHashMap<Activity, Long> KEEP_LYRICS_ACTIVITY_UNTIL_MS = new WeakHashMap<>();
    private static final LinkedHashSet<String> NATIVE_HOOKED_CLASS_NAMES = new LinkedHashSet<>();
    private static volatile WeakReference<MediaSession> currentMediaSession = new WeakReference<>(null);

    private Method playerWrapperGetStateMethod;
    private final LyricsParser lyricsParser = new LyricsParser(NativeSpicyLyricsHook::finalizeParsedDocument);
    private final NativeLyricsSource nativeLyricsSource = new NativeLyricsSource(NativeSpicyLyricsHook::appContext, NativeSpicyLyricsHook::finalizeParsedDocument);

    static void dbg(String function, String message) {
        if (!DEBUG_LOGGING) return;
        XposedBridge.log(TAG + " [" + BUILD_CLUE + "] " + function + "() " + safe(message));
    }

    static void dbgEnter(String function) {
        dbg(function, "enter");
    }

    @Override
    protected void hook() {
        dbg("hook", "native Spicy renderer hook enabled version=" + BuildStamp.FULL);
        hookAccessTokenCapture();
        hookSpotifyAuthTokenObjects();
        hookNativeLyricsCapture();
        hookPlayerStateBridge();
        hookLyricsActivityLifecycle();
    }

    private void hookLyricsActivityLifecycle() {
        dbgEnter("hookLyricsActivityLifecycle");
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (isLyricsFullscreenActivity(activity)) {
                    if (hasNativeSpicyRoot(activity) || consumeTakeoverArmed() || nativeLyricsSessionActive) {
                        XposedBridge.log(TAG + " lyrics activity onCreate (takeover) " + activity.getClass().getName());
                        activity.getWindow().getDecorView().postDelayed(() -> mountNativeSpicyRoot(activity), 250);
                    }
                    // else: opened via Spotify's native lyric card — leave Spotify's screen untouched.
                } else {
                    scheduleExtraLyricsButtonInjection(activity);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                References.currentActivity = activity;
                if (isLyricsFullscreenActivity(activity)) {
                    if (hasNativeSpicyRoot(activity) || consumeTakeoverArmed() || nativeLyricsSessionActive) {
                        activity.getWindow().getDecorView().postDelayed(() -> mountNativeSpicyRoot(activity), 150);
                    }
                    // else: native lyric card opened Spotify's own screen — do not take over.
                } else {
                    scheduleExtraLyricsButtonInjection(activity);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                stopNowPlayingController(activity);
                if (!isLyricsFullscreenActivity(activity)) return;
                // Rotation/config change recreates the activity — keep the session so onCreate
                // re-mounts our shell. Only a real destroy ends the session.
                if (!activity.isChangingConfigurations()) nativeLyricsSessionActive = false;
                removeNativeSpicyRoot(activity);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                stopNowPlayingController((Activity) param.thisObject); // quiet the now-playing card ticker
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!isLyricsFullscreenActivity(activity)) return;
                markExplicitLyricsExit(activity);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "finish", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!shouldKeepLyricsActivityOpen(activity)) return;
                XposedBridge.log(TAG + " suppressed non-explicit lyrics activity finish to keep native renderer open");
                param.setResult(null);
            }
        });
    }

    // Replace Spotify's now-playing lyric snippet (app:id/lyrics_element) with our 3-line live card.
    // Overlays it via translationX/Y in the content FrameLayout (lyrics_element sits in a constraint
    // layout; we don't fight its LayoutParams) and hides the original. A controller drives the lines.
    private void injectLiveLyricCard(Activity activity) {
        try {
            if (activity == null || !isNativeSpicyEnabled(activity)) return;
            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null) return;
            if (content.findViewWithTag(TAG_LIVE_CARD) != null) {
                NowPlayingLyricController c;
                synchronized (NP_CONTROLLERS) { c = NP_CONTROLLERS.get(activity); }
                if (c != null) c.start(); // resume on return to now-playing
                return;
            }
            View el = findViewByResourceEntryName(content, "lyrics_element");
            if (el == null || !(el.getParent() instanceof ViewGroup)) return;
            ViewGroup parent = (ViewGroup) el.getParent();
            if (parent.findViewWithTag(TAG_LIVE_CARD) != null) return;
            // Replace lyrics_element in its OWN parent, reusing its LayoutParams so we inherit its
            // constraints/position and scroll with the player (overlaying in the static content
            // layout left it floating + mis-scrolling).
            int idx = parent.indexOfChild(el);
            ViewGroup.LayoutParams lp = el.getLayoutParams();
            LiveLyricCardView card = new LiveLyricCardView(activity);
            card.setTag(TAG_LIVE_CARD);
            parent.removeView(el);
            card.setLayoutParams(lp);
            parent.addView(card, idx);
            NowPlayingLyricController controller = new NowPlayingLyricController(this, activity, card);
            synchronized (NP_CONTROLLERS) { NP_CONTROLLERS.put(activity, controller); }
            controller.start();
            // The snippet slot indents slightly less than the content margin; nudge the card's left
            // so the lyric line is flush with the title/scrubber. Retried on a few delays because the
            // title row isn't laid out immediately and a layout-listener proved unreliable here.
            View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
            if (decor != null) {
                Runnable align = () -> alignCardLeftToContent(activity, card);
                decor.postDelayed(align, 500);
                decor.postDelayed(align, 1200);
                decor.postDelayed(align, 2600);
            }
            XposedBridge.log(TAG + " live lyric card injected in " + activity.getClass().getName());
        } catch (Throwable t) {
            XposedBridge.log(TAG + " live card inject failed: " + t);
        }
    }

    // Align the live card's text left edge with the scrubber time label (position_text). That sits at
    // the content margin on every now-playing template, whereas the title is shifted right past the
    // album thumbnail on the Canvas/video template — so the title is not a reliable anchor.
    private boolean alignCardLeftToContent(Activity activity, View card) {
        try {
            if (card == null || card.getWidth() <= 0) return false;
            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null) return false;
            View ref = findViewByResourceEntryName(content, "position_text");
            if (ref == null || ref.getWidth() <= 0) return false;
            int[] c = new int[2], t = new int[2];
            card.getLocationInWindow(c);
            ref.getLocationInWindow(t);
            int delta = t[0] - (c[0] + card.getPaddingLeft());
            if (delta > 0 && delta < dp(48)) {
                card.setPadding(card.getPaddingLeft() + delta, card.getPaddingTop(),
                        card.getPaddingRight(), card.getPaddingBottom());
            }
            return true;
        } catch (Throwable ignored) {
            return true; // give up cleanly rather than loop the listener forever
        }
    }

    private void stopNowPlayingController(Activity activity) {
        if (activity == null) return;
        NowPlayingLyricController c;
        synchronized (NP_CONTROLLERS) { c = NP_CONTROLLERS.get(activity); }
        if (c != null) c.stop();
    }

    private void scheduleExtraLyricsButtonInjection(Activity activity) {
        if (activity == null) return;
        try {
            if (!isNativeSpicyEnabled(activity)) return;
            View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
            if (decor == null) return;
            decor.postDelayed(() -> injectExtraLyricsButton(activity), 450);
            decor.postDelayed(() -> injectExtraLyricsButton(activity), 1400);
            decor.postDelayed(() -> injectExtraLyricsButton(activity), 2800);
            decor.postDelayed(() -> injectExtraLyricsButton(activity), 5200);
            decor.postDelayed(() -> injectLiveLyricCard(activity), 700);
            decor.postDelayed(() -> injectLiveLyricCard(activity), 1800);
            decor.postDelayed(() -> injectLiveLyricCard(activity), 3500);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " schedule extra lyrics injection failed: " + t);
        }
    }

    private void injectExtraLyricsButton(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || isLyricsFullscreenActivity(activity) || !isNativeSpicyEnabled(activity)) return;
            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null || content.findViewWithTag(TAG_EXTRA_LYRICS_BUTTON) != null) return;
            if (!isLikelyNowPlayingScreen(activity, content)) return;

            // The Share/Queue cluster (accessory_row) is an R8-obfuscated ConstraintLayout — we can't set
            // constraints on a new child reflectively (LayoutParams fields are minified). So instead we
            // add our ♪ to accessory_row's PARENT (the wide footer bar) and translate it into the empty
            // gap just LEFT of the cluster. translationX/Y are post-layout transforms (no constraints
            // needed), it stays within the footer's bounds (so it's visible AND tappable), and it sits in
            // free space (no overlap / tap-steal). Self-positions from the cluster's measured location.
            View rowView = findViewByResourceEntryName(content, "accessory_row");
            if (rowView == null || !rowView.isShown() || rowView.getWidth() == 0) {
                XposedBridge.log(TAG + " Extra lyrics: accessory_row not laid out yet in " + activity.getClass().getName());
                return;
            }
            ViewGroup host = rowView.getParent() instanceof ViewGroup ? (ViewGroup) rowView.getParent() : null;
            if (host == null || host.findViewWithTag(TAG_EXTRA_LYRICS_BUTTON) != null) return;
            int side = rowView.getHeight() > 0 ? rowView.getHeight() : dp(48);
            View button = createExtraLyricsRowButton(activity);
            host.addView(button, new ViewGroup.LayoutParams(side, side));
            // Dead-center horizontally in the footer (the empty space between Connect and Share/Queue),
            // vertically aligned to the action row's line.
            button.setTranslationX((host.getWidth() - side) / 2f);
            button.setTranslationY(rowView.getTop() + (rowView.getHeight() - side) / 2f);
            XposedBridge.log(TAG + " inserted Extra lyrics ♪ centered in footer in " + activity.getClass().getName());
        } catch (Throwable t) {
            XposedBridge.log(TAG + " inject extra lyrics button failed: " + t);
        }
    }

    // Flat variant for injecting into Spotify's Share/Queue action row — no circular pill background,
    // so it matches the row's plain icon buttons.
    private View createExtraLyricsRowButton(Activity activity) {
        ImageButton button = new ImageButton(activity);
        button.setTag(TAG_EXTRA_LYRICS_BUTTON);
        button.setContentDescription("Open Spicy lyrics");
        setModuleIcon(button, activity, R.drawable.ic_spicy_lyrics_page);
        button.setColorFilter(Color.rgb(232, 232, 238));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setBackgroundColor(Color.TRANSPARENT);
        // Padding tuned so the glyph renders at roughly the same visual size as Spotify's Share/Queue
        // icons (touch slot matches the row height; the glyph is inset to match their weight).
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> launchNativeLyricsFullscreen(activity));
        return button;
    }

    static ImageButton createRoundIconButton(Context context, int drawableRes, String description, int sizeDp, int paddingDp) {
        ImageButton button = new ImageButton(context);
        button.setContentDescription(description);
        setModuleIcon(button, context, drawableRes);
        button.setColorFilter(Color.rgb(232, 232, 238));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp));
        button.setMinimumWidth(dp(sizeDp));
        button.setMinimumHeight(dp(sizeDp));
        button.setClickable(true);
        button.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.argb(48, 255, 255, 255));
        bg.setStroke(dp(1), Color.argb(52, 255, 255, 255));
        button.setBackground(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) button.setElevation(dp(8));
        return button;
    }

    static void setModuleIcon(ImageButton button, Context context, int drawableRes) {
        if (button == null) return;
        try {
            Drawable drawable = References.modResources == null ? null : References.modResources.getDrawable(drawableRes);
            if (drawable == null && context != null) {
                drawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        ? context.getResources().getDrawable(drawableRes, context.getTheme())
                        : context.getResources().getDrawable(drawableRes);
            }
            button.setImageDrawable(drawable);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " failed to load module icon " + drawableRes + ": " + t);
            button.setImageDrawable(null);
        }
    }

    private View findViewByResourceEntryName(View root, String entryName) {
        if (root == null || isBlank(entryName)) return null;
        ArrayDeque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.removeFirst();
            int id = view.getId();
            if (id != View.NO_ID) {
                try {
                    String name = view.getResources().getResourceEntryName(id);
                    if (entryName.equals(name)) return view;
                } catch (Throwable ignored) {
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) queue.addLast(group.getChildAt(i));
            }
        }
        return null;
    }

    private boolean isLikelyNowPlayingScreen(Activity activity, View root) {
        String activityName = activity == null ? "" : activity.getClass().getName().toLowerCase(Locale.ROOT);
        if (activityName.contains("settings") || activityName.contains("lyrics")) return false;
        if (activityName.contains("nowplaying") || activityName.contains("now_playing")) return true;
        if (hasVisibleClassNameContaining(root, "nowplaying")
                || hasVisibleClassNameContaining(root, "now_playing")
                || hasVisibleClassNameContaining(root, "com.spotify.nowplaying")) return true;
        SpotifyTrack track = getCurrentTrackSafely();
        return track != null
                && !isBlank(trackIdFromUri(track.uri))
                && containsVisibleText(root, track.title)
                && containsVisibleText(root, track.artist);
    }

    private boolean hasVisibleClassNameContaining(View root, String needleLower) {
        if (root == null || isBlank(needleLower)) return false;
        ArrayDeque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.removeFirst();
            String name = view.getClass().getName().toLowerCase(Locale.ROOT);
            if (view.isShown() && name.contains(needleLower)) return true;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) queue.addLast(group.getChildAt(i));
            }
        }
        return false;
    }

    private boolean containsVisibleText(View root, String needle) {
        if (root == null || isBlank(needle)) return false;
        String normalizedNeedle = needle.trim().toLowerCase(Locale.ROOT);
        ArrayDeque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.removeFirst();
            if (view.isShown() && view instanceof TextView) {
                CharSequence text = ((TextView) view).getText();
                if (text != null && text.toString().trim().toLowerCase(Locale.ROOT).contains(normalizedNeedle)) {
                    return true;
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) queue.addLast(group.getChildAt(i));
            }
        }
        return false;
    }

    private void launchNativeLyricsFullscreen(Activity activity) {
        try {
            if (activity == null) return;
            // Arm takeover: only OUR launch mounts the native renderer. Tapping Spotify's own lyric
            // card opens the same activity WITHOUT arming, so it shows Spotify's native lyric screen.
            takeoverArmed = true;
            Intent intent = new Intent();
            intent.setClassName(activity.getPackageName(), LYRICS_FULLSCREEN_ACTIVITY);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            XposedBridge.log(TAG + " launched native lyrics fullscreen (takeover armed) from Extra lyrics button");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " launch native lyrics fullscreen failed: " + t);
        }
    }

    // Armed by our own entry button right before launching the lyrics activity; consumed when we mount.
    // Spotify's native lyric card launches the same activity without arming this, so it stays native.
    private static volatile boolean takeoverArmed = false;
    // True while our native lyrics screen is the active session. Survives activity recreation
    // (rotation/config change) so we re-mount instead of falling back to Spotify's native screen;
    // cleared on an explicit exit or a real (non-config-change) destroy.
    private static volatile boolean nativeLyricsSessionActive = false;

    private boolean consumeTakeoverArmed() {
        if (takeoverArmed) {
            takeoverArmed = false;
            return true;
        }
        return false;
    }

    public void markExplicitLyricsExit(Activity activity) {
        if (activity == null) return;
        nativeLyricsSessionActive = false; // user is leaving — end the session (next open stays native)
        synchronized (EXPLICIT_LYRICS_EXIT_UNTIL_MS) {
            EXPLICIT_LYRICS_EXIT_UNTIL_MS.put(activity, SystemClock.elapsedRealtime() + 1200);
        }
    }

    private void markLyricsActivityKeepWindow(Activity activity) {
        if (activity == null) return;
        synchronized (KEEP_LYRICS_ACTIVITY_UNTIL_MS) {
            KEEP_LYRICS_ACTIVITY_UNTIL_MS.put(activity, SystemClock.elapsedRealtime() + KEEP_LYRICS_ACTIVITY_AFTER_MOUNT_MS);
        }
    }

    private boolean shouldKeepLyricsActivityOpen(Activity activity) {
        if (!isLyricsFullscreenActivity(activity) || !isNativeSpicyEnabled(activity)) return false;
        if (!hasNativeSpicyRoot(activity)) return false;
        synchronized (EXPLICIT_LYRICS_EXIT_UNTIL_MS) {
            Long until = EXPLICIT_LYRICS_EXIT_UNTIL_MS.get(activity);
            if (until != null && SystemClock.elapsedRealtime() <= until) return false;
        }
        synchronized (KEEP_LYRICS_ACTIVITY_UNTIL_MS) {
            Long until = KEEP_LYRICS_ACTIVITY_UNTIL_MS.get(activity);
            return until != null && SystemClock.elapsedRealtime() <= until;
        }
    }

    private void hookAccessTokenCapture() {
        dbgEnter("hookAccessTokenCapture");
        try {
            Class<?> requestBuilder = XposedHelpers.findClass("okhttp3.Request$Builder", lpparm.classLoader);
            XC_MethodHook headerPairHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2) return;
                    Object nameObj = param.args[0];
                    Object valueObj = param.args[1];
                    if (!(nameObj instanceof String) || !(valueObj instanceof String)) return;
                    captureAuthHeader((String) nameObj, (String) valueObj);
                }
            };
            tryHookAll(requestBuilder, "header", headerPairHook);
            tryHookAll(requestBuilder, "addHeader", headerPairHook);
            tryHookAll(requestBuilder, "headers", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 1) return;
                    captureAuthorizationValue(readHeaderValue(param.args[0], "Authorization"));
                }
            });
            tryHookAll(requestBuilder, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    captureAuthorizationValue(readHeaderValue(param.getResult(), "Authorization"));
                    logBuiltRequestProbe(param.getResult());
                }
            });

            try {
                Class<?> headersBuilder = XposedHelpers.findClass("okhttp3.Headers$Builder", lpparm.classLoader);
                tryHookAll(headersBuilder, "add", headerPairHook);
                tryHookAll(headersBuilder, "set", headerPairHook);
                tryHookAll(headersBuilder, "addUnsafeNonAscii", headerPairHook);
            } catch (Throwable ignored) {
            }

            try {
                Class<?> requestClass = XposedHelpers.findClass("okhttp3.Request", lpparm.classLoader);
                XposedBridge.hookAllConstructors(requestClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        captureAuthorizationValue(readHeaderValue(param.thisObject, "Authorization"));
                        logBuiltRequestProbe(param.thisObject);
                    }
                });
            } catch (Throwable ignored) {
            }

            XposedBridge.log(TAG + " OkHttp auth capture hooks installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " auth capture hook failed: " + t);
        }
    }

    private static void tryHookAll(Class<?> clazz, String methodName, XC_MethodHook hook) {
        try {
            XposedBridge.hookAllMethods(clazz, methodName, hook);
        } catch (Throwable ignored) {
        }
    }

    private static String readHeaderValue(Object headersOrRequest, String name) {
        if (headersOrRequest == null || isBlank(name)) return null;
        for (String methodName : new String[]{"header", "get"}) {
            try {
                Object result = XposedHelpers.callMethod(headersOrRequest, methodName, name);
                if (result instanceof String) return (String) result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void captureAuthorizationValue(String headerValue) {
        captureAuthHeader("Authorization", headerValue);
    }

    private static void logBuiltRequestProbe(Object request) {
        if (request == null || authRequestDebugCount >= AUTH_REQUEST_DEBUG_LIMIT) return;
        try {
            Object rawUrl = XposedHelpers.callMethod(request, "url");
            if (rawUrl == null) return;
            String url = rawUrl.toString();
            String lower = url.toLowerCase(Locale.ROOT);
            if (!lower.contains("spotify") && !lower.contains("spclient")) return;
            String auth = readHeaderValue(request, "Authorization");
            Uri uri = Uri.parse(url);
            String host = safe(uri.getHost());
            String path = safe(uri.getPath());
            authRequestDebugCount++;
            XposedBridge.log(TAG + " okhttp request#" + authRequestDebugCount
                    + " host=" + host
                    + " path=" + path
                    + " auth=" + (!isBlank(auth)));
        } catch (Throwable ignored) {
        }
    }

    private static void captureAuthHeader(String headerName, String headerValue) {
        if (headerName == null || headerValue == null) return;
        if (!headerName.equalsIgnoreCase("authorization")) return;
        boolean bearer = headerValue.toLowerCase(Locale.ROOT).startsWith("bearer");
        if (bearer || authHeaderDebugCount < AUTH_HEADER_DEBUG_LIMIT) {
            authHeaderDebugCount++;
            dbg("captureAuthHeader", "authorization hasValue=true bearer=" + bearer + " len=" + headerValue.length());
        }
        if (!bearer) return;
        String token = headerValue.replaceFirst("(?i)^bearer", "").trim();
        if (token.isEmpty() || token.equals("0")) return;
        String old = References.accessToken;
        if (old != null && old.equals(token)) return;
        References.accessToken = token;
        persistAccessToken(token);
        XposedBridge.log(TAG + " captured Spotify access token len=" + token.length());
    }

    private static Context appContext() {
        Activity activity = References.currentActivity;
        if (activity != null) return activity.getApplicationContext();
        try {
            Object app = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentApplication");
            if (app instanceof Context) return ((Context) app).getApplicationContext();
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void persistAccessToken(String token) {
        Context context = appContext();
        if (context == null || isBlank(token)) return;
        try {
            context.getSharedPreferences(PREFS_MAIN, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_SPOTIFY_ACCESS_TOKEN, token)
                    .apply();
        } catch (Throwable t) {
            XposedBridge.log(TAG + " token persist failed: " + t);
        }
    }

    private static void restorePersistedAccessToken(Context context) {
        if (context == null || !isBlank(References.accessToken)) return;
        try {
            String stored = context.getSharedPreferences(PREFS_MAIN, Context.MODE_PRIVATE)
                    .getString(KEY_LAST_SPOTIFY_ACCESS_TOKEN, "");
            if (isBlank(stored) || "0".equals(stored)) return;
            References.accessToken = stored;
            XposedBridge.log(TAG + " restored persisted Spotify access token len=" + stored.length());
        } catch (Throwable t) {
            XposedBridge.log(TAG + " token restore failed: " + t);
        }
    }

    // Probe Spotify's auth-token classes directly — the OkHttp-header capture misses the token on
    // modern Spotify (auth doesn't flow through hooked okhttp). The token fires on cold-start refresh.
    private void hookSpotifyAuthTokenObjects() {
        int hooked = 0;
        for (String className : SPOTIFY_AUTH_TOKEN_CLASSES) {
            try {
                Class<?> clazz = XposedHelpers.findClass(className, lpparm.classLoader);
                XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        captureAccessTokenObject(param.thisObject, className + "#ctor");
                    }
                });
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getParameterTypes().length != 0) continue;
                    Class<?> rt = method.getReturnType();
                    // Hook only object getters returning a token-shaped object; extract the precise
                    // access-token field. Skip raw String getters (may return a refresh token / "Bearer").
                    if (rt == String.class || rt.isPrimitive()
                            || !rt.getName().toLowerCase(Locale.ROOT).contains("token")) continue;
                    final String name = method.getName();
                    try {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                captureAccessTokenObject(param.getResult(), className + "#" + name);
                            }
                        });
                    } catch (Throwable ignored) {
                    }
                }
                hooked++;
            } catch (Throwable ignored) {
            }
        }
        XposedBridge.log(TAG + " Spotify auth token object hooks installed classes=" + hooked);
    }

    private static void captureAccessTokenObject(Object tokenObject, String source) {
        captureAccessTokenObject(tokenObject, source, 0);
    }

    // Capture only the access-token field (token_/accessToken), recursing one level into nested
    // objects (AccessTokenResponse -> AccessToken.token_). Never grabs arbitrary long strings.
    private static void captureAccessTokenObject(Object tokenObject, String source, int depth) {
        if (tokenObject == null || depth > 2) return;
        if (tokenObject instanceof String) {
            captureAccessTokenCandidate((String) tokenObject, source);
            return;
        }
        Class<?> clazz = tokenObject.getClass();
        String cn = clazz.getName();
        if (clazz.isArray() || cn.startsWith("java.") || cn.startsWith("android.") || cn.startsWith("kotlin.")) return;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(tokenObject);
                    if (value instanceof String) {
                        if (isAccessTokenFieldName(field.getName())) {
                            captureAccessTokenCandidate((String) value, source + "#" + field.getName());
                        }
                    } else if (value != null && depth < 2) {
                        captureAccessTokenObject(value, source + "#" + field.getName(), depth + 1);
                    }
                } catch (Throwable ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static boolean isAccessTokenFieldName(String name) {
        if (isBlank(name)) return false;
        for (String hint : ACCESS_TOKEN_FIELD_HINTS) {
            if (name.equals(hint)) return true;
        }
        return false;
    }

    private static void captureAccessTokenCandidate(String candidate, String source) {
        if (isBlank(candidate)) return;
        String token = candidate.trim();
        if (token.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            token = token.substring("bearer ".length()).trim();
        }
        if (!looksLikeSpotifyAccessToken(token)) return;
        storeSpotifyAccessToken(token, source);
    }

    private static boolean looksLikeSpotifyAccessToken(String token) {
        if (isBlank(token) || "0".equals(token)) return false;
        if (token.length() < 20 || token.length() > 4096) return false;
        for (int i = 0; i < token.length(); i++) {
            if (Character.isWhitespace(token.charAt(i))) return false;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        return !lower.equals("bearer") && !lower.equals("access_token")
                && !lower.equals("token") && !lower.startsWith("spotify:");
    }

    private static void storeSpotifyAccessToken(String token, String source) {
        if (!looksLikeSpotifyAccessToken(token)) return;
        String old = References.accessToken;
        if (old != null && old.equals(token)) return;
        References.accessToken = token;
        persistAccessToken(token);
        XposedBridge.log(TAG + " captured Spotify access token source=" + source + " len=" + token.length());
    }

    private void hookNativeLyricsCapture() {
        dbgEnter("hookNativeLyricsCapture");
        String[] nativeClasses = new String[]{
                "com.spotify.lyrics.offlineimpl.database.LyricsDatabaseEntity",
                "com.spotify.lyrics.offlineimpl.database.LyricsDatabaseEntity$Line",
                "com.spotify.lyrics.offlineimpl.database.LyricsDatabaseEntity$Syllable",
                "com.spotify.lyrics.offlineimpl.database.LyricsDatabaseEntity$Provider",
                "com.spotify.lyrics.data.model.Lyrics",      // <= ~9.1.28
                "com.spotify.lyrics.data.model.ColorLyrics"  // renamed in newer Spotify (>= 9.1.56)
        };
        for (String name : nativeClasses) {
            try {
                Class<?> cls = XposedHelpers.findClass(name, lpparm.classLoader);
                hookResolvedNativeLyricsClass(cls, name);
            } catch (Throwable t) {
                XposedBridge.log(TAG + " native lyrics capture missing " + name + ": " + t.getClass().getSimpleName());
            }
        }
        hookDeferredNativeLyricsClassLoading();
        discoverNativeLyricsClasses();
    }

    private void hookDeferredNativeLyricsClassLoading() {
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.args != null && param.args.length > 0 && param.args[0] instanceof String)) return;
                    String name = (String) param.args[0];
                    if (!isNativeLyricsClassName(name)) return;
                    Object result = param.getResult();
                    if (!(result instanceof Class)) return;
                    hookResolvedNativeLyricsClass((Class<?>) result, "deferred:" + name);
                }
            });
            XposedBridge.log(TAG + " native lyrics deferred ClassLoader hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " native lyrics deferred hook failed: " + t);
        }
    }

    private void discoverNativeLyricsClasses() {
        if (bridge == null) return;
        String[][] probes = new String[][]{
                new String[]{"lyrics_entities("},
                new String[]{"SELECT * FROM lyrics_entities WHERE track_id = ?"},
                new String[]{"INSERT OR REPLACE INTO `lyrics_entities`"},
                new String[]{"syncStatus", "vocalRemovalStatus"},
                new String[]{"GeneratedJsonAdapter(LyricsDatabaseEntity.Line)"},
                new String[]{"GeneratedJsonAdapter(LyricsDatabaseEntity.Syllable)"},
                new String[]{"lyricsLines_"},
                new String[]{"LyricsLineTag"},
                new String[]{"color-lyrics/v3/track/{trackId}"},
                new String[]{"color-lyrics/v2/track/{trackId}"}
        };
        for (String[] probe : probes) {
            try {
                var found = bridge.findClass(FindClass.create().matcher(ClassMatcher.create().usingStrings(probe)));
                XposedBridge.log(TAG + " native lyrics DexKit probe strings=" + String.join(",", probe) + " matches=" + found.size());
                int count = 0;
                for (org.luckypray.dexkit.result.ClassData data : found) {
                    if (count++ >= 8) break;
                    String name = data.getName();
                    XposedBridge.log(TAG + " native lyrics DexKit candidate " + name);
                    try {
                        hookResolvedNativeLyricsClass(data.getInstance(lpparm.classLoader), "dexkit:" + String.join(",", probe));
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + " native lyrics DexKit candidate load failed " + name + ": " + t.getClass().getSimpleName());
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + " native lyrics DexKit probe failed strings=" + String.join(",", probe) + ": " + t);
            }
        }
    }

    private void hookResolvedNativeLyricsClass(Class<?> cls, String sourceTag) {
        if (cls == null) return;
        String className = cls.getName();
        synchronized (NATIVE_HOOKED_CLASS_NAMES) {
            if (NATIVE_HOOKED_CLASS_NAMES.contains(className)) return;
            if (NATIVE_HOOKED_CLASS_NAMES.size() > 40) return;
            NATIVE_HOOKED_CLASS_NAMES.add(className);
        }
        try {
            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    captureNativeLyricsCandidate(param.thisObject, param.args, sourceTag + ":ctor:" + className);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + " native lyrics constructor hook failed " + className + ": " + t.getClass().getSimpleName());
        }
        int methodHooks = 0;
        for (Method method : cls.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) continue;
            if (method.getReturnType() == Void.TYPE) continue;
            if (methodHooks >= 18) break;
            try {
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result != null) captureNativeLyricsCandidate(result, param.args, sourceTag + ":method:" + className + "#" + method.getName());
                    }
                });
                methodHooks++;
            } catch (Throwable ignored) {
            }
        }
        XposedBridge.log(TAG + " native lyrics capture hook installed " + className + " methods=" + methodHooks + " source=" + sourceTag);
    }

    private static boolean isNativeLyricsClassName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("spotify.lyrics")
                || lower.contains("lyricsdatabaseentity")
                || lower.contains("lyricsresponse")
                || lower.contains("lyricsv3response")
                || lower.contains("colorlyricsresponse");
    }

    private void captureNativeLyricsCandidate(Object candidate, Object[] ctorArgs, String sourceTag) {
        dbg("captureNativeLyricsCandidate", "source=" + safe(sourceTag) + " class=" + (candidate == null ? "null" : candidate.getClass().getName()) + " args=" + (ctorArgs == null ? 0 : ctorArgs.length));
        nativeLyricsSource.captureCandidate(getCurrentTrackSafely(), candidate, ctorArgs, sourceTag);
    }

    private void hookPlayerStateBridge() {
        dbgEnter("hookPlayerStateBridge");
        try {
            XposedHelpers.findAndHookMethod("com.spotify.player.model.AutoValue_PlayerState$Builder", lpparm.classLoader, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object state = param.getResult();
                    if (state == null) return;
                    References.playerState = new WeakReference<>(state);
                }
            });
            XposedBridge.log(TAG + " player state builder hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " player state builder hook failed: " + t);
        }

        try {
            var stateWrapperClasses = bridge.findClass(FindClass.create().matcher(ClassMatcher.create().modifiers(Modifier.PUBLIC | Modifier.FINAL).interfaceCount(1).fields(FieldsMatcher.create()
                    .add(FieldMatcher.create().modifiers(Modifier.PUBLIC | Modifier.FINAL))
                    .add(FieldMatcher.create().modifiers(Modifier.PUBLIC | Modifier.FINAL).type(String.class))
                    .add(FieldMatcher.create().modifiers(Modifier.PUBLIC | Modifier.FINAL).type(ArrayList.class))
                    .add(FieldMatcher.create().modifiers(Modifier.PUBLIC).type(Object.class))
                    .add(FieldMatcher.create().modifiers(Modifier.PUBLIC).type(Bundle.class))
            )));

            playerWrapperGetStateMethod = bridge.findMethod(FindMethod.create().searchInClass(stateWrapperClasses).matcher(MethodMatcher.create().name("getState"))).get(0).getMethodInstance(lpparm.classLoader);
            XposedBridge.hookMethod(playerWrapperGetStateMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    References.playerStateWrapper = new WeakReference<>(param.thisObject);
                }
            });
            XposedBridge.log(TAG + " player wrapper getState hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " player wrapper getState hook failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(MediaSession.class, "setPlaybackState", PlaybackState.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    currentMediaSession = new WeakReference<>((MediaSession) param.thisObject);
                    PlaybackState playbackState = (PlaybackState) param.args[0];
                    if (playbackState == null) return;
                    isPlaying = playbackState.getState() == PlaybackState.STATE_PLAYING;
                    long pos = playbackState.getPosition();
                    if (pos >= 0) {
                        mediaPositionMs = pos;
                        mediaPositionUpdatedAtElapsedMs = SystemClock.elapsedRealtime();
                    }
                }
            });
            XposedBridge.log(TAG + " MediaSession playback hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " MediaSession playback hook failed: " + t);
        }
    }

    private boolean isLyricsFullscreenActivity(Activity activity) {
        return activity != null && LYRICS_FULLSCREEN_ACTIVITY.equals(activity.getClass().getName());
    }

    private boolean isNativeSpicyEnabled(Activity activity) {
        try {
            return SpotifyPlusConfig.from(activity).get(Settings.NATIVE_SPICY_ENABLED);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void mountNativeSpicyRoot(Activity activity) {
        dbg("mountNativeSpicyRoot", "activity=" + (activity == null ? "null" : activity.getClass().getName()));
        try {
            ensureDeployCacheCleared(activity);
            restorePersistedAccessToken(activity);
            if (!isLyricsFullscreenActivity(activity)) return;
            if (!isNativeSpicyEnabled(activity)) {
                removeNativeSpicyRoot(activity);
                return;
            }
            nativeLyricsSessionActive = true; // our screen owns this lyrics session (survives rotation)

            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null) {
                XposedBridge.log(TAG + " content root missing");
                return;
            }

            View existing = content.findViewWithTag(TAG_NATIVE_SPICY_ROOT);
            if (existing instanceof NativeSpicyShellView) {
                ((NativeSpicyShellView) existing).start();
                return;
            }

            NativeSpicyShellView root = new NativeSpicyShellView(this, activity);
            root.setTag(TAG_NATIVE_SPICY_ROOT);
            content.addView(root, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            markLyricsActivityKeepWindow(activity);
            root.start();
            XposedBridge.log(TAG + " mounted native Spicy renderer shell");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " mount failed: " + t);
        }
    }

    private void removeNativeSpicyRoot(Activity activity) {
        dbg("removeNativeSpicyRoot", "activity=" + (activity == null ? "null" : activity.getClass().getName()));
        try {
            FrameLayout content = activity.findViewById(android.R.id.content);
            if (content == null) return;
            View existing = content.findViewWithTag(TAG_NATIVE_SPICY_ROOT);
            if (existing instanceof NativeSpicyShellView) {
                ((NativeSpicyShellView) existing).stop();
                content.removeView(existing);
                XposedBridge.log(TAG + " removed native Spicy shell");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " remove failed: " + t);
        }
    }

    private boolean hasNativeSpicyRoot(Activity activity) {
        try {
            if (activity == null) return false;
            FrameLayout content = activity.findViewById(android.R.id.content);
            return content != null && content.findViewWithTag(TAG_NATIVE_SPICY_ROOT) instanceof NativeSpicyShellView;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public SpotifyTrack getCurrentTrackSafely() {
        dbgEnter("getCurrentTrackSafely");
        try {
            if (References.playerState == null || References.playerState.get() == null) return null;
            return References.getTrackTitle(lpparm, bridge);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " track read failed: " + t);
            return null;
        }
    }

    public boolean seekSpotifyTo(long positionMs) {
        try {
            MediaSession session = currentMediaSession == null ? null : currentMediaSession.get();
            if (session == null) return false;
            MediaController controller = session.getController();
            if (controller == null || controller.getTransportControls() == null) return false;
            controller.getTransportControls().seekTo(positionMs);
            return true;
        } catch (Throwable t) {
            XposedBridge.log(TAG + " media seek failed: " + t);
            return false;
        }
    }

    public long readBestMeasuredProgressMs(SpotifyTrack track, boolean playing) {
        long now = SystemClock.elapsedRealtime();
        long media = mediaPositionMs;
        if (media >= 0 && now < seekOverrideUntilElapsedMs) {
            if (playing && mediaPositionUpdatedAtElapsedMs > 0) {
                return Math.max(0, media + (now - mediaPositionUpdatedAtElapsedMs));
            }
            return Math.max(0, media);
        }

        long playerStateProgress = readPlayerStateProgressMs(playing);
        if (playerStateProgress >= 0) return playerStateProgress;

        if (track != null && track.position >= 0) {
            long wallNow = System.currentTimeMillis();
            if (!playing && track.lastUpdated > 0) {
                long advancedBy = Math.max(0, wallNow - track.lastUpdated);
                return Math.max(0, track.position - advancedBy);
            }
            return Math.max(0, track.position);
        }

        if (media >= 0) {
            if (playing && mediaPositionUpdatedAtElapsedMs > 0) {
                return Math.max(0, media + (now - mediaPositionUpdatedAtElapsedMs));
            }
            return Math.max(0, media);
        }
        return -1;
    }

    private long readPlayerStateProgressMs(boolean playing) {
        try {
            Object state = References.playerState == null ? null : References.playerState.get();
            if (state == null) return -1;
            Object posOpt = XposedHelpers.callMethod(state, "positionAsOfTimestamp");
            if (posOpt == null) return -1;
            Matcher matcher = DIGITS.matcher(posOpt.toString());
            if (!matcher.find()) return -1;
            long basePos = Long.parseLong(matcher.group());
            long timestamp = 0;
            try {
                Object rawTimestamp = XposedHelpers.callMethod(state, "timestamp");
                if (rawTimestamp instanceof Long) timestamp = (Long) rawTimestamp;
            } catch (Throwable ignored) {
            }
            if (!playing || timestamp <= 0) return Math.max(0, basePos);
            return Math.max(0, basePos + (System.currentTimeMillis() - timestamp));
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public boolean isPlayerActuallyPlaying() {
        if (!isPlaying) return false;
        try {
            Object state = References.playerState == null ? null : References.playerState.get();
            if (state != null) {
                for (String method : new String[]{"isPaused", "paused"}) {
                    try {
                        Object result = XposedHelpers.callMethod(state, method);
                        if (result instanceof Boolean && (Boolean) result) return false;
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public void fetchLyrics(Activity activity, SpotifyTrack track, int generation, LyricsResultCallback callback) {
        dbg("fetchLyrics", "start generation=" + generation + " track=" + (track == null ? "null" : safe(track.uri)));
        LyricsRepository repository = new LyricsRepository(
                HTTP,
                lyricsParser,
                nativeLyricsSource
        );
        repository.fetchLyrics(
                activity,
                track,
                generation,
                SpotifyPlusConfig.from(activity).get(Settings.SEND_TOKEN),
                References.accessToken,
                new LyricsRepository.ResultCallback() {
            @Override
            public void onSuccess(LyricsDocument document) {
                callback.onSuccess(document);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Single post-parse normalization step shared by every source adapter: spread synthetic
     * static timings, fill missing end times (LyricTimeline rules), apply cached Google
     * enhancements, and compute processing flags. Adapters must not pre-fill synthetic end
     * times themselves.
     */
    private static void finalizeParsedDocument(Context context, LyricsDocument doc) {
        LyricsDocumentProcessor.finalizeParsedDocument(context, doc, GOOGLE_PROCESSING_VERSION);
    }


    interface LyricsResultCallback {
        void onSuccess(LyricsDocument document);
        void onError(String error);
    }

    private static synchronized void ensureDeployCacheCleared(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_DEPLOY_STATE, Context.MODE_PRIVATE);
            // We run inside Spotify's process, so getPackageInfo("com.eza.spicyex") throws
            // NameNotFoundException and the cache was never cleared on deploy. Use our baked-in build
            // stamp instead — it changes every build, so each deploy clears stale cached responses.
            String currentVersion = com.eza.spicyex.BuildStamp.FULL;
            String lastVersion = prefs.getString(Settings.LAST_CACHE_CLEAR_VERSION.key, "");
            if (currentVersion.equals(lastVersion)) return;
            LyricsResponseCache.clear(context);
            LyricsTranslator.clearCache(context);
            LyricCaches.clearGoogle(context);
            LyricCaches.clearProcessed(context);
            prefs.edit().putString(Settings.LAST_CACHE_CLEAR_VERSION.key, currentVersion).apply();
            XposedBridge.log(TAG + " deploy cache clear version=" + currentVersion);
        } catch (Throwable t) {
            XposedBridge.log(TAG + " deploy cache clear failed: " + t);
        }
    }


}
