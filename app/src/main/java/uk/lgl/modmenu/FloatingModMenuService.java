//TODO
//Text input string

package uk.lgl.modmenu;

import android.animation.ArgbEvaluator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;

public class FloatingModMenuService extends Service {
    //initialize methods from the native library
    private native String getInfo(String str);

    public static native void DrawOn(ESPView eSPView, Canvas canvas, int i, int i2);

    public static native boolean isReady();

    public static native boolean needEsp();

    native String[] getFeatureList();

    native boolean isGameLibLoaded();

    //********** Here you can easly change the menu appearance **********//
    private static FloatingModMenuService me = null;
    public static final String TAG = "Mod_Menu"; //Tag for logcat
    int TEXT_COLOR = Color.parseColor(getInfo("TXT_CL"));
    int TEXT_COLOR_2 = Color.parseColor(getInfo("TXT2_CL"));
    int TEXTHIDECLOSE_CL = Color.parseColor(getInfo("TEXTHIDECLOSE_CL"));
    int HIDECLOSE_CL = Color.parseColor(getInfo("HIDECLOSE_CL"));
    int BTN_COLOR = Color.parseColor(getInfo("BTN_CL"));
    int MENU_BG_COLOR = Color.parseColor(getInfo("MAIN_CL")); //#AARRGGBB
    int MENU_WIDTH = 290;
    int MENU_HEIGHT = 210;
    float MENU_CORNER = 1f;
    int ICON_SIZE = 50; //Change both width and height of image
    float ICON_ALPHA = 0.7f; //Transparent
    int SEEKBAR_CL = Color.parseColor(getInfo("SEEKBAR_CL"));
    int ToggleON = Color.parseColor(getInfo("SWITCHON_CL"));
    int ToggleOFF = Color.parseColor(getInfo("SWITCHOFF_CL"));
    int ToggleON2 = Color.parseColor(getInfo("SWITCHON2_CL"));
    int ToggleOFF2 = Color.parseColor(getInfo("SWITCHOFF2_CL"));
    int CategoryBG = Color.parseColor(getInfo("TXTVIEV_CL"));
    String NumberTxtColor = getInfo("NUMTXT_CL");
    int esp_H = 0;
    int esp_W = 0;
    //********************************************************************//

    //Some fields
    GradientDrawable gdMenuBody, gdAnimation = new GradientDrawable();
    RelativeLayout mCollapsed, mRootContainer;
    LinearLayout mExpanded, patches;
    LinearLayout.LayoutParams scrlLLExpanded, scrlLL;
    WindowManager mWindowManager;
    WindowManager.LayoutParams params;
    ImageView startimage;
    FrameLayout rootFrame;
    ScrollView scrollView;
    private ESPView eSPOverlayView;
    boolean stopChecking;

    static FloatingModMenuService getInstance() {
        if (me == null) {
            me = new FloatingModMenuService();
        }
        return me;
    }

    //When this Class is called the code in this function will be executed
    @Override
    public void onCreate() {
        super.onCreate();
        Preferences.context = this;

        //Create the menu
        initFloating();

        //Create a handler for this Class
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                Thread();
                handler.postDelayed(this, 1000);
            }
        });
    }

    private int getLayoutType() {
        if (Build.VERSION.SDK_INT >= 26) {
            return 2038;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            return 2002;
        }
        return Build.VERSION.SDK_INT >= 23 ? 2005 : 2003;
    }

    //Here we write the code for our Menu
    // Reference: https://www.androidhive.info/2016/11/android-floating-widget-like-facebook-chat-head/
    private void initFloating() {
        rootFrame = new FrameLayout(this); // Global markup
        rootFrame.setOnTouchListener(onTouchListener());
        mRootContainer = new RelativeLayout(this); // Markup on which two markups of the icon and the menu itself will be placed
        mCollapsed = new RelativeLayout(this); // Markup of the icon (when the menu is minimized)
        mCollapsed.setVisibility(View.VISIBLE);
        mCollapsed.setAlpha(ICON_ALPHA);

        //********** The box of the mod menu **********
        mExpanded = new LinearLayout(this); // Menu markup (when the menu is expanded)
        mExpanded.setVisibility(View.GONE);
        mExpanded.setBackgroundColor(MENU_BG_COLOR);
        mExpanded.setGravity(Gravity.CENTER);
        mExpanded.setOrientation(LinearLayout.VERTICAL);
        // mExpanded.setPadding(1, 1, 1, 1);
        mExpanded.setLayoutParams(new LinearLayout.LayoutParams(dp(MENU_WIDTH), WRAP_CONTENT));
        gdMenuBody = new GradientDrawable();
        gdMenuBody.setCornerRadius(MENU_CORNER); //Set corner
        gdMenuBody.setColor(MENU_BG_COLOR); //Set background color
        //gradientdrawable.setStroke(1, Color.parseColor("#32cb00")); //Set border
        mExpanded.setBackground(gdMenuBody); //Apply aninmation to it

        //********** The icon to open mod menu **********
        startimage = new ImageView(this);
        startimage.setLayoutParams(new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        int applyDimension = (int) TypedValue.applyDimension(1, ICON_SIZE, getResources().getDisplayMetrics()); //Icon size
        startimage.getLayoutParams().height = applyDimension;
        startimage.getLayoutParams().width = applyDimension;
        startimage.requestLayout();
        startimage.setScaleType(ImageView.ScaleType.FIT_XY);
        byte[] decode = Base64.decode(getInfo("ICON"), 0);
        startimage.setImageBitmap(BitmapFactory.decodeByteArray(decode, 0, decode.length));
        ((ViewGroup.MarginLayoutParams) startimage.getLayoutParams()).topMargin = convertDipToPixels(10);
        //Initialize event handlers for buttons, etc.
        startimage.setOnTouchListener(onTouchListener());
        startimage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.GONE);
                mExpanded.setVisibility(View.VISIBLE);
            }
        });

        //********** The icon in Webview to open mod menu **********
        WebView wView = new WebView(this); //Icon size width=\"50\" height=\"50\"
        wView.setLayoutParams(new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        int applyDimension2 = (int) TypedValue.applyDimension(1, ICON_SIZE, getResources().getDisplayMetrics()); //Icon size
        wView.getLayoutParams().height = applyDimension2;
        wView.getLayoutParams().width = applyDimension2;
        wView.loadData("<html>" +
                "<head></head>" +
                "<body style=\"margin: 0; padding: 0\">" +
                "<img src=\"" + getInfo("ICONWEBVIEW") + "\" width=\"" + ICON_SIZE + "\" height=\"" + ICON_SIZE + "\" >" +
                "</body>" +
                "</html>", "text/html", "utf-8");
        //wView.setBackgroundColor(0x00000000); //Transparent
        wView.setAlpha(ICON_ALPHA);
        wView.getSettings().setAppCachePath("/data/data/" + getPackageName() + "/cache");
        wView.getSettings().setAppCacheEnabled(true);
        wView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        wView.setOnTouchListener(onTouchListener());

        wView.requestLayout();

        //********** Title text **********
        RelativeLayout titleText = new RelativeLayout(this);
        titleText.setPadding(10, 5, 10, 5);
        titleText.setVerticalGravity(16);

        TextView title = new TextView(this);
        title.setText(Html.fromHtml(getInfo("TITLE")));
        title.setTextColor(TEXT_COLOR_2);
        title.setTextSize(18.0f);
        title.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
        title.setLayoutParams(rl);
        title.setPadding(10, 25, 10, 5);


        //********** Heading text **********
        TextView heading = new TextView(this);
        heading.setText(Html.fromHtml(getInfo("HEADING")));
        heading.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        heading.setMarqueeRepeatLimit(-1);
        heading.setSingleLine(true);
        heading.setSelected(true);
        heading.setTextColor(TEXT_COLOR);
        heading.setTextSize(10.0f);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(10, 5, 10, 10);


        //********** Mod menu feature list **********
        scrollView = new ScrollView(this);
        //Auto size. To set size manually, change the width and height example 500, 500
        scrlLL = new LinearLayout.LayoutParams(MATCH_PARENT, dp(MENU_HEIGHT));
        scrlLLExpanded = new LinearLayout.LayoutParams(mExpanded.getLayoutParams());
        scrlLLExpanded.weight = 1.0f;
        scrollView.setLayoutParams(scrlLL);
        scrollView.setBackgroundColor(Color.TRANSPARENT);

        patches = new LinearLayout(this);
        patches.setOrientation(LinearLayout.VERTICAL);
        //**********  Hide/Kill button **********
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setPadding(10, 10, 10, 10);
        relativeLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);

        Button hideBtn = new Button(this);
        hideBtn.setBackgroundColor(HIDECLOSE_CL);
        hideBtn.setText(getInfo("TEXTHIDE"));
        hideBtn.setTextColor(TEXTHIDECLOSE_CL);
        hideBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.VISIBLE);
                mCollapsed.setAlpha(0);
                mExpanded.setVisibility(View.GONE);
                Toast.makeText(view.getContext(), getInfo("TEXTHIDDEN"), Toast.LENGTH_LONG).show();
            }
        });
        hideBtn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                Toast.makeText(view.getContext(), getInfo("TEXTCLOSED"), Toast.LENGTH_LONG).show();
                FloatingModMenuService.this.stopSelf();
                return false;
            }
        });

        //********** Close button **********
        Button closeBtn = new Button(this);
        closeBtn.setBackgroundColor(HIDECLOSE_CL);
        closeBtn.setText(getInfo("TEXTCLOSE"));
        closeBtn.setTextColor(TEXTHIDECLOSE_CL);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mCollapsed.setVisibility(View.VISIBLE);
                mCollapsed.setAlpha(ICON_ALPHA);
                mExpanded.setVisibility(View.GONE);
            }
        });

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.addRule(ALIGN_PARENT_RIGHT);
        closeBtn.setLayoutParams(layoutParams);

        //********** Params **********
        //Variable to check later if the phone supports Draw over other apps permission
        params = new WindowManager.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, getLayoutType(), 8, -3);
        params.gravity = 51;
        params.x = 0;
        params.y = 100;

        //********** Adding view components **********
        rootFrame.addView(mRootContainer);
        mRootContainer.addView(mCollapsed);
        mRootContainer.addView(mExpanded);
        if (getInfo("ICONWEBVIEW") != null) {
            mCollapsed.addView(wView);
        } else {
            mCollapsed.addView(startimage);
        }
        titleText.addView(title);
        mExpanded.addView(titleText);
        mExpanded.addView(heading);
        scrollView.addView(patches);
        mExpanded.addView(scrollView);
        relativeLayout.addView(hideBtn);
        relativeLayout.addView(closeBtn);
        mExpanded.addView(relativeLayout);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(rootFrame, params);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            boolean viewLoaded = false;

            @Override
            public void run() {
                //If the save preferences is enabled, it will check if game lib is loaded before starting menu
                //Comment the if-else code out except startService if you want to run the app and test preferences
                if (Preferences.savePref && !isGameLibLoaded() && !stopChecking) {
                    if (!viewLoaded) {
                        patches.addView(addText(getInfo("TEXTSAVEPREF")));
                        patches.addView(addButton(-100, false, getInfo("TEXTFORCELOAD")));
                        viewLoaded = true;
                    }
                    handler.postDelayed(this, 600);
                } else {
                    patches.removeAllViews();
                    featureList(getFeatureList(), patches);
                }
            }
        }, 500);
    }

    private void featureList(String[] listFT, LinearLayout linearLayout) {
        int featureNum, subFeat = 0;

        for (int i = 0; i < listFT.length; i++) {
            boolean switchedOn = false;
            Log.i("featureList", listFT[i]);
            String feature = listFT[i];
            if (feature.contains("True_")) {
                switchedOn = true;
                feature = feature.replaceFirst("True_", "");
            }
            String[] str = feature.split("_");
            if (TextUtils.isDigitsOnly(str[0]) || str[0].matches("-[0-9]*")) {
                featureNum = Integer.parseInt(str[0]);
                feature = feature.replaceFirst(str[0] + "_", "");
                subFeat++;
            } else {
                //Subtract feature number. We don't want to count ButtonLink, Category, RichTextView and RichWebView
                featureNum = i - subFeat;
            }
            String[] strSplit = feature.split("_");

            if (strSplit[0].equals("Toggle")) {
                linearLayout.addView(addSwitch(featureNum, strSplit[1], switchedOn));
            } else if (strSplit[0].equals("SeekBar")) {
                linearLayout.addView(addSlider(featureNum, strSplit[1], Integer.parseInt(strSplit[2]), Integer.parseInt(strSplit[3])));
            } else if (strSplit[0].equals("Button")) {
                linearLayout.addView(addButton(featureNum, false, strSplit[1]));
            } else if (strSplit[0].equals("ButtonDialog")) {
                linearLayout.addView(addButton(featureNum, true, strSplit[1]));
            } else if (strSplit[0].equals("Category")) {
                subFeat++;
                linearLayout.addView(addText(strSplit[1]));
            } else if (strSplit[0].equals("RichTextView")) {
                subFeat++;
                linearLayout.addView(RichTextView(strSplit[1]));
            } else if (strSplit[0].equals("RichWebView")) {
                subFeat++;
                linearLayout.addView(RichWebView(strSplit[1]));
            }
        }

    }

    private View.OnTouchListener onTouchListener() {
        return new View.OnTouchListener() {
            final View collapsedView = mCollapsed;
            final View expandedView = mExpanded;
            private float initialTouchX, initialTouchY;
            private int initialX, initialY;

            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int rawX = (int) (motionEvent.getRawX() - initialTouchX);
                        int rawY = (int) (motionEvent.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (rawX < 10 && rawY < 10 && isViewCollapsed()) {
                            //When user clicks on the image view of the collapsed layout,
                            //visibility of the collapsed layout will be changed to "View.GONE"
                            //and expanded view will become visible.
                            try {
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                            } catch (NullPointerException e) {

                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + ((int) (motionEvent.getRawX() - initialTouchX));
                        params.y = initialY + ((int) (motionEvent.getRawY() - initialTouchY));
                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(rootFrame, params);
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    private void openDialog(final String featureName, final int featureNum) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, getLayoutType(), 8, -3);
        layoutParams.gravity = 17;
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(dp(300), dp(300)));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(-7829368);
        TextView textView = new TextView(this);
        textView.setText(featureName + "?");
        textView.setTypeface(null, Typeface.BOLD);
        textView.setTextColor(-1);
        textView.setTextSize(20.0f);
        textView.setPadding(20, 20, 20, 20);
        Button button = new Button(this);
        button.setText(getInfo("TEXTYES"));
        Button button2 = new Button(this);
        button2.setText(getInfo("TEXTNO"));
        linearLayout.addView(textView);
        linearLayout.addView(button);
        linearLayout.addView(button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                mWindowManager.removeView(linearLayout);
                Preferences.changeFeatureInt(featureName, featureNum, 0);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                mWindowManager.removeView(linearLayout);
            }
        });
        this.mWindowManager.addView(linearLayout, layoutParams);
    }

    private View addButton(final int featureNum, final boolean z, final String featureName) {
        final Button button = new Button(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.setMargins(2, 2, 2, 2);
        button.setLayoutParams(layoutParams);
        button.setTextSize(15.0f);
        button.setTextColor(TEXT_COLOR_2);
        button.setAllCaps(false); //Disable caps to support html
        button.setText(Html.fromHtml("<font face='monospace'><b>" + featureName + "</b></font>"));
        button.setBackgroundColor(BTN_COLOR);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (z) {
                    openDialog(featureName, featureNum);
                } else {
                    Preferences.changeFeatureInt(featureName, featureNum, 0);
                }
            }
        });

        return button;
    }

    private View addSwitch(final int featureNum, final String featureName, boolean switchedOn) {
        final Switch switchR = new Switch(this);
        ColorStateList buttonStates = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        Color.BLUE,
                        ToggleON, // ON
                        ToggleOFF // OFF
                }
        );
        ColorStateList buttonStates2 = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        Color.BLUE,
                        ToggleON2, // ON
                        ToggleOFF2 // OFF
                }
        );
        //Set colors of the switch. Comment out if you don't like it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchR.getThumbDrawable().setTintList(buttonStates);
            switchR.getTrackDrawable().setTintList(buttonStates2);
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        layoutParams.setMargins(2, 2, 2, 2);
        switchR.setLayoutParams(layoutParams);
        switchR.setText(Html.fromHtml("<font face='monospace'><b>" + featureName + "</b></font>"));
        switchR.setTextColor(TEXT_COLOR_2);
        switchR.setTypeface(null, Typeface.BOLD);
        switchR.setPadding(10, 10, 0, 10);
        switchR.setChecked(Preferences.loadPrefBoolean(featureName, featureNum, switchedOn));
        if (switchR.isChecked())
            switchR.setBackgroundColor(Color.parseColor(getInfo("SWITCHONBG_CL")));
        else
            switchR.setBackgroundColor(Color.parseColor(getInfo("SWITCHOFFBG_CL")));
        switchR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    switchR.setBackgroundColor(Color.parseColor(getInfo("SWITCHONBG_CL")));
                } else {
                    switchR.setBackgroundColor(Color.parseColor(getInfo("SWITCHOFFBG_CL")));
                }
                Preferences.changeFeatureBoolean(featureName, featureNum, isChecked);
            }
        });
        return switchR;
    }

    private View addSlider(final int featureNum, final String featureName, final int min, int max) {
        int loadedProg = Preferences.loadPrefInt(featureName, featureNum);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        layoutParams.setMargins(2, 2, 2, 2);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setPadding(10, 5, 0, 5);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setBackgroundColor(SEEKBAR_CL);
        linearLayout.setLayoutParams(layoutParams);
        final TextView textView = new TextView(this);
        textView.setText(Html.fromHtml("<font face='monospace'><b>" + featureName + ": <font color='" + NumberTxtColor + "'>" + ((loadedProg == 0) ? min : loadedProg) + "</b></font></font>"));
        textView.setTextColor(TEXT_COLOR_2);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setPadding(25, 10, 35, 10);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 21) {
            seekBar.getProgressDrawable().setTint(-1);
        }
        seekBar.setMax(max);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            seekBar.setMin(min); //setMin for Oreo and above
        seekBar.setProgress((loadedProg == 0) ? min : loadedProg);
        seekBar.setScaleY(1.0f);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                //if progress is greater than minimum, don't go below. Else, set progress
                seekBar.setProgress(i < min ? min : i);
                Preferences.changeFeatureInt(featureName, featureNum, i < min ? min : i);
                textView.setText(Html.fromHtml("<font face='monospace'><b>" + featureName + ": <font color='" + NumberTxtColor + "'>" + (i < min ? min : i) + "</b></font></font>"));
            }
        });
        linearLayout.addView(textView);
        linearLayout.addView(seekBar);

        return linearLayout;
    }

    private View addText(String text) {
        TextView textView = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        layoutParams.setMargins(2, 2, 2, 2);
        textView.setLayoutParams(layoutParams);
        textView.setBackgroundColor(CategoryBG);
        textView.setText(Html.fromHtml("<font face='monospace'><b>- : " + text + " : -</b></font>"));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(15.0f);
        textView.setTextColor(TEXT_COLOR);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(0, 5, 0, 5);
        return textView;
    }

    private View RichTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(Html.fromHtml(text));
        textView.setTextColor(TEXT_COLOR_2);
        textView.setPadding(10, 5, 10, 5);
        return textView;
    }

    private View RichWebView(String text) {
        WebView wView = new WebView(this);
        wView.loadData(text, "text/html", "utf-8");
        wView.setBackgroundColor(0x00000000); //Transparent
        wView.setPadding(0, 5, 0, 5);
        wView.getSettings().setAppCacheEnabled(false);
        wView.requestLayout();
        return wView;
    }

    private void DrawCanvas() {
        this.eSPOverlayView = new ESPView(getInstance());
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(this.esp_W, this.esp_H, getLayoutType(), 1080, -3);
        layoutParams.gravity = 8388659;
        layoutParams.x = 0;
        layoutParams.y = 100;
        this.mWindowManager.addView(this.eSPOverlayView, layoutParams);
    }

    //Override our Start Command so the Service doesnt try to recreate itself when the App is closed
    public int onStartCommand(Intent intent, int i, int i2) {
        return Service.START_NOT_STICKY;
    }

    private boolean isViewCollapsed() {
        return rootFrame == null || mCollapsed.getVisibility() == View.VISIBLE;
    }

    //For our image a little converter
    private int convertDipToPixels(int i) {
        return (int) ((((float) i) * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int dp(int i) {
        return (int) TypedValue.applyDimension(1, (float) i, getResources().getDisplayMetrics());
    }

    //Check if we are still in the game. If now our menu and menu button will dissapear
    private boolean isNotInGame() {
        RunningAppProcessInfo runningAppProcessInfo = new RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(runningAppProcessInfo);
        return runningAppProcessInfo.importance != 100;
    }

    //Destroy our View
    public void onDestroy() {
        super.onDestroy();
        if (rootFrame != null) {
            mWindowManager.removeView(rootFrame);
        }
        ESPView eSPView = this.eSPOverlayView;
        if (eSPView != null) {
            this.mWindowManager.removeView(eSPView);
            this.eSPOverlayView = null;
        }
    }

    //Same as above so it wont crash in the background and therefore use alot of Battery life
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    private void Thread() {
        if (rootFrame == null || eSPOverlayView == null) {
            return;
        }
        if (isNotInGame()) {
            rootFrame.setVisibility(View.INVISIBLE);
            eSPOverlayView.setVisibility(View.INVISIBLE);
        } else {
            rootFrame.setVisibility(View.VISIBLE);
            eSPOverlayView.setVisibility(View.VISIBLE);
        }
    }

    private class EditTextString {
        private String text;

        public void setString(String s) {
            text = s;
        }

        public String getString() {
            return text;
        }
    }

    private class EditTextNum {
        private int val;

        public void setNum(int i) {
            val = i;
        }

        public int getNum() {
            return val;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}