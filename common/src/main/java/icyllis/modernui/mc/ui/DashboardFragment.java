/*
 * Modern UI.
 * Copyright (C) 2023-2026 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc.ui;

import icyllis.modernui.R;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.LinearGradient;
import icyllis.modernui.graphics.Shader;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.markflow.Markflow;
import icyllis.modernui.markflow.MarkflowPlugin;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.mc.ModernUIMod;
import icyllis.modernui.mc.StillAlive;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.text.method.LinkMovementMethod;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.resources.language.I18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

public class DashboardFragment extends Fragment {

    private FrameLayout mLayout;
    private Markflow mMarkflow;
    private int mClickCount;
    private TextView mLyricView;
    private TextView mCreditView;
    private TextView mArtView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable DataSet savedInstanceState) {
        var context = requireContext();
        var layout = new FrameLayout(context);
        var value = new TypedValue();
        Markflow markflow; {
            var builder = Markflow.builder(context);
            Typeface monoFont = Typeface.getSystemFont("JetBrains Mono Medium");
            if (monoFont != Typeface.SANS_SERIF) {
                builder.usePlugin(new MarkflowPlugin() {
                    @Override
                    public void configureTheme(@NonNull MarkflowTheme.Builder builder) {
                        builder.codeTypeface(monoFont);
                    }
                });
            }
            markflow = builder.build();
        }
        mMarkflow = markflow;

        {
            // two panel layout
            var content = new WrappingLinearLayout(context);
            content.setWrapWidth(content.dp(864));
            content.setChildMaxWidth(content.dp(746));
            int dp4 = content.dp(4);

            {
                var panel = new LinearLayout(context);
                panel.setOrientation(LinearLayout.VERTICAL);
                panel.setClipToPadding(false);
                panel.setGravity(Gravity.CENTER_VERTICAL);
                // TITLE
                {
                    var title = new TextView(context);
                    title.setText(I18n.get("modernui.center.title"));
                    title.setTextSize(32);
                    title.setTextStyle(Typeface.BOLD);
                    title.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        int height = bottom - top;
                        int oldHeight = oldBottom - oldTop;
                        if (height != oldHeight) {
                            var tv = (TextView) v;
                            tv.getPaint().setShader(new LinearGradient(0, 0, height * 2, height,
                                    // Minato Aqua
                                    new int[]{
                                            0xFFB8DFF4,
                                            0xFFF8C5CE,
                                            0xFFFEFDF0
                                    },
                                    null,
                                    Shader.TileMode.MIRROR,
                                    null));
                        }
                    });
                    context.getTheme().resolveAttribute(R.ns, R.attr.colorControlHighlight, value, true);
                    title.setBackground(new RippleDrawable(ColorStateList.valueOf(value.data), null,
                            new ColorDrawable(~0)));
                    title.setOnClickListener(this::prepare);
                    mClickCount = 0;

                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    params.bottomMargin = content.dp(40);
                    panel.addView(title, params);
                }
                // 撑开面板，把下面的链接推到底部
                // Spacer that pushes the links below down to the bottom of the card.
                var spacer = new View(context);
                panel.addView(spacer, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
                {
                    // 本分支在 CurseForge 与 Modrinth 的发布页
                    // Release pages of this fork on CurseForge and Modrinth.
                    var download = new TextView(context);
                    download.setMovementMethod(LinkMovementMethod.getInstance());
                    markflow.setMarkdown(download,
                            I18n.get("modernui.center.home.download_ss",
                                    "[CURSEFORGE](https://www.curseforge.com/minecraft/mc-mods/modernui-v)",
                                    "[MODRINTH](https://modrinth.com/project/modernui-v)"));
                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    params.bottomMargin = content.dp(8);
                    panel.addView(download, params);
                }
                {
                    // 本分支的源代码仓库，链接文案走语言文件
                    // Source repository of this fork; the label comes from the language files.
                    var source = new TextView(context);
                    source.setMovementMethod(LinkMovementMethod.getInstance());
                    markflow.setMarkdown(source,
                            I18n.get("modernui.center.home.source",
                                    "[GITHUB](https://github.com/ShaoZhige/ModernUI-V)"));
                    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                    params.bottomMargin = content.dp(8);
                    panel.addView(source, params);
                }
                {
                    var info = new TextView(context);
                    info.setMovementMethod(LinkMovementMethod.getInstance());
                    markflow.setMarkdown(info,
                            I18n.get("modernui.center.home.upstream_ss",
                                    "[CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui)",
                                    "[Modrinth](https://modrinth.com/mod/modern-ui)"));
                    panel.addView(info, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
                }

                ThemeControl.makeElevatedCard(context, panel, value);
                var params = new LinearLayout.LayoutParams(MATCH_PARENT, content.dp(420), 1);
                params.setMargins(dp4, dp4, dp4, dp4);
                content.addView(panel, params);
            }

            {
                // 分支说明与注意事项，内容走语言文件，方便翻译
                // Fork intro and notes, both come from the language files so they can be translated.
                var panel = new NestedScrollView(context);
                panel.setClipToPadding(false);
                panel.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

                var inner = new LinearLayout(context);
                inner.setOrientation(LinearLayout.VERTICAL);
                {
                    var tv = new TextView(context);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    markflow.setMarkdown(tv, I18n.get("modernui.center.home.intro"));
                    var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                    params.bottomMargin = content.dp(20);
                    inner.addView(tv, params);
                }
                {
                    var tv = new TextView(context);
                    tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    markflow.setMarkdown(tv, I18n.get("modernui.center.home.notice"));
                    inner.addView(tv, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                }
                panel.addView(inner, MATCH_PARENT, WRAP_CONTENT);

                ThemeControl.makeElevatedCard(context, panel, value);
                var params = new LinearLayout.LayoutParams(MATCH_PARENT, content.dp(420), 1);
                params.setMargins(dp4, dp4, dp4, dp4);
                content.addView(panel, params);
            }

            // 让内容在屏幕上垂直居中，避免大屏幕下贴在顶部。
            // fillViewport 只在内容不满一屏时把 wrapper 撑到整屏高，此时下面的 gravity 才生效；
            // 内容超过一屏（窄窗口下两卡改为竖排）时保持自然高度，仍可从头滚动，顶部不会被裁掉。
            // Center the content vertically instead of pinning it to the top.
            // fillViewport stretches the wrapper to the viewport height only when the content is
            // shorter, which is what lets the gravity below take effect; taller content keeps its
            // natural height and stays scrollable from the top.
            var sv = new ScrollView(context);
            sv.setFillViewport(true);
            var wrapper = new FrameLayout(context);
            wrapper.addView(content, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER));
            sv.addView(wrapper, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
            layout.addView(sv);
        }

        var transition = new LayoutTransition();
        layout.setLayoutTransition(transition);
        return mLayout = layout;
    }

    private Runnable mUpdateText;

    // all events
    private StillAlive.Event[] mEvents;
    private int mEventIndex;
    private long mEventStartTime;
    // current line
    private String mLyricLine;
    private int mLyricIndex;
    private int mLyricStartTime;
    private int mLyricInterval;
    private boolean mLyricNeedsWrap;

    private int mCreditIndex;
    private int mCreditStartTime;
    private int mCreditInterval;

    private void prepare(View button) {
        if (++mClickCount == 7) {
            mLayout.removeAllViews();

            mLayout.postDelayed(this::play, 2000);
        }
    }

    private void play() {
        Context context = requireContext();
        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setRowCount(2);
        gridLayout.setColumnCount(2);
        gridLayout.setUseDefaultMargins(true);

        Typeface monoFont = Typeface.getSystemFont("JetBrains Mono Medium");
        if (monoFont == Typeface.SANS_SERIF) {
            monoFont = Typeface.MONOSPACED;
        }
        TypedValue value = new TypedValue();
        {
            var tv = new TextView(context);
            tv.setTypeface(monoFont);
            tv.setTextSize(14);
            tv.setLineSpacing(0, 1.5f / 1.1f);
            tv.setText("", TextView.BufferType.EDITABLE);
            tv.setTextDirection(View.TEXT_DIRECTION_LTR);
            ThemeControl.makeOutlinedCard(context, tv, value);

            var params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(0, 2, 1F);
            params.columnSpec = GridLayout.spec(0, 1, 1F);
            params.width = 0;
            params.height = 0;
            gridLayout.addView(tv, params);
            mLyricView = tv;
        }

        {
            var tv = new TextView(context);
            tv.setTypeface(monoFont);
            tv.setTextSize(14);
            tv.setLineSpacing(0, 1.5f / 1.1f);
            tv.setText("", TextView.BufferType.EDITABLE);
            tv.setTextDirection(View.TEXT_DIRECTION_LTR);
            tv.setGravity(Gravity.BOTTOM);
            ThemeControl.makeOutlinedCard(context, tv, value);

            var params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(0, 1F);
            params.columnSpec = GridLayout.spec(1, 1F);
            params.width = 0;
            params.height = 0;
            gridLayout.addView(tv, params);
            mCreditView = tv;
        }

        {
            var tv = new TextView(context);
            tv.setTypeface(monoFont);
            tv.setTextSize(14);
            tv.setLineSpacing(0, 1f / 1.1f);
            tv.setTextDirection(View.TEXT_DIRECTION_LTR);
            tv.setGravity(Gravity.CENTER);
            ThemeControl.makeOutlinedCard(context, tv, value);

            var params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(1, 1.1F);
            params.columnSpec = GridLayout.spec(1, 1F);
            params.width = 0;
            params.height = 0;
            gridLayout.addView(tv, params);
            mArtView = tv;
        }

        mLayout.addView(gridLayout);

        mEvents = StillAlive.Event.getEvents();
        mEventIndex = 0;
        mEventStartTime = System.nanoTime();
        mCreditInterval = 0;

        mUpdateText = this::tick;
        tick();
    }

    private void tick() {
        if (mLayout == null || !mLayout.isAttachedToWindow()) {
            return;
        }
        int time = (int) ((System.nanoTime() - mEventStartTime) / 1000000L);
        while (mEventIndex < mEvents.length) {
            var e = mEvents[mEventIndex];
            if (time < e.time()) {
                break;
            }
            tickLyric(time);
            switch (e.kind()) {
                case StillAlive.Event.WORDS_WRAP, StillAlive.Event.WORDS_NOWRAP -> {
                    mLyricLine = e.payload();
                    mLyricIndex = 0;
                    mLyricStartTime = e.time();
                    int interval = e.arg();
                    if (interval < 0) {
                        mLyricInterval = mEvents[mEventIndex + 1].time() - mLyricStartTime;
                    } else {
                        mLyricInterval = interval;
                    }
                    mLyricNeedsWrap = e.kind() == StillAlive.Event.WORDS_WRAP;
                }
                case StillAlive.Event.ASCII_ART -> mArtView.setText(StillAlive.ASCII_ARTS[e.arg()]);
                case StillAlive.Event.CLEAR_SCREEN -> mLyricView.getEditableText().clear();
                case StillAlive.Event.PLAY_MUSIC -> StillAlive.getInstance().start();
                case StillAlive.Event.SHOW_CREDITS -> {
                    mCreditIndex = 0;
                    mCreditStartTime = e.time();
                    mCreditInterval = mEvents[mEvents.length - 1].time() - mCreditStartTime;
                }
            }
            mEventIndex++;
        }
        if (mEventIndex == mEvents.length) {
            mEvents = null;
            mLyricLine = null;
            return;
        }
        tickLyric(time);
        mLayout.postDelayed(mUpdateText, 50);
    }

    private void tickLyric(int time) {
        if (mLyricLine == null) {
            return;
        }
        int count = mLyricLine.length();
        if (mLyricIndex < count) {
            int end = Math.min((time - mLyricStartTime) * count / mLyricInterval + 1, count);
            if (mLyricIndex < end) {
                mLyricView.getEditableText().append(mLyricLine, mLyricIndex, end);
                mLyricIndex = end;
            }
        }
        if (mLyricNeedsWrap && mLyricIndex == count) {
            mLyricView.getEditableText().append('\n');
            mLyricNeedsWrap = false;
        }
        if (mCreditInterval > 0) {
            count = StillAlive.CREDITS.length();
            int end = Math.min((time - mCreditStartTime) * count / mCreditInterval + 1, count);
            if (mCreditIndex < end) {
                mCreditView.getEditableText().append(StillAlive.CREDITS, mCreditIndex, end);
                mCreditIndex = end;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLayout = null;
        StillAlive.stop();
    }

    static class WrappingLinearLayout extends LinearLayout {

        private int mWrapWidth;
        private int mChildMaxWidth;

        public WrappingLinearLayout(Context context) {
            super(context);
        }

        public void setWrapWidth(int wrapWidth) {
            mWrapWidth = wrapWidth;
        }

        public int getWrapWidth() {
            return mWrapWidth;
        }

        public void setChildMaxWidth(int childMaxWidth) {
            mChildMaxWidth = childMaxWidth;
        }

        public int getChildMaxWidth() {
            return mChildMaxWidth;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            var orientation = width >= mWrapWidth
                    ? HORIZONTAL : VERTICAL;
            setOrientation(orientation);
            if (mChildMaxWidth > 0) {
                int limit = orientation == HORIZONTAL
                        ? getChildCount() * mChildMaxWidth : mChildMaxWidth;
                if (width > limit) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(limit, MeasureSpec.EXACTLY);
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
