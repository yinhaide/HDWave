package com.yhd.hdwave;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class WaveView extends View {

    /************************** 以下是内部使用的参数 ***************************/
    private static final String TAG = "WaveView";
    /**
     * 默认峰值取到最高,为了保证没有锯齿的出现,规则是只能缩小不能拉伸。
     * 理论上峰值为0.5f为最佳,但是因为数值保留精度的差异,在高度不大的情况下
     * 精度达不到,出现空白区域,所以要根据高度去调整这个峰值,一下是测试数据:(控件高度相对整个屏幕的百分比 --> 振幅临界值)
     * 0.01 --> 0.45 ; 0.02 --> 0.475 ; 0.05 --> 0.49 ; 0.1 --> 0.495 ; 0.15 --> 0.495 ; 0.2 --> 0.497
     * 所以这里去最小值0.45 ,兼容1%屏幕高度,小于1%已经快看不到效果了
     */
    private static final float DEFAULT_AMPLI_RATIO = 0.45f;
    //默认水平高度的比例,作为基准进行操作
    private static final float DEFAULT_HEIGHT_RATIO = 0.5f;
    //默认正弦的水平距离,作为基准进行操作
    private static final float DEFAULT_LENGTH_RATIO = 1.0f;
    //默认两个正弦波浪的水平距离比例（0f-1.0f）
    private static final float DEFAULT_DISTANCE_RATIO = 0.125f;
    //最小值
    private static final float DEFAULT_MIN_VALUE = 0.00000001f;
    //最大振幅
    private static final float DEFAULT_MAX_AMPLI_VALUE = 0.5f;
    //默认的水平高度,需要在getHeight()>0的时候才能缓存下来
    private float mDefaultHeight;
    //波浪偏移:正弦平移距离比例（0f-1.0f）
    private float mShiftRatio = 1.0f;
    //振幅临时的数值,用来自动调整振幅高度
    private float mLastAmplitudeRatio = 0.1f;//默认值
    /************************** 以下是外部设置的参数 ***************************/
    //波浪幅度:正弦的高度比例（amplitudeRatio + heightRatio <= 1 体验最佳）
    private float amplitudeRatio = 0.1f;
    //波浪高度:正弦起点高度比例（amplitudeRatio + heightRatio <= 1 体验最佳）
    private float heightRatio = DEFAULT_HEIGHT_RATIO;
    //双波浪距离:两个正弦波浪的水平距离比例（0f-1.0f）
    private float distanceRatio = DEFAULT_DISTANCE_RATIO;
    //波浪频率:正弦的水平距离比例（0f-1.0f）
    private float frequency = 1.0f;
    //波浪升高到指定高度需要的动画时间
    private int heightTime = 1000;
    //水平偏移一个画布长度需要的时间
    private int shiftTime = 2000;
    //外圆边距
    private int borderWidth = 0;
    //外圆边颜色
    private int borderColor = Color.parseColor("#44FFFFFF");
    //后面正弦的默认颜色,需要一定的不透明度
    private int behindColor = Color.parseColor("#28FFFFFF");
    //前面正弦的默认颜色,需要一定的不透明度
    private int frontColor = Color.parseColor("#3CFFFFFF");
    //背景色
    private int backgroundColor = Color.parseColor("#00FFFFFF");
    //默认形状为原型
    private Shape shape = Shape.CIRCLE;
    /************************** 以下是内部属性 ***************************/
    // 重复波浪的容器
    private BitmapShader mWaveShader;
    // 重复波浪的容器
    private BitmapShader mWaveShader2;
    // shader matrix
    private Matrix mShaderMatrix;
    private Matrix mShaderMatrix2;
    // 画波浪的画笔
    private Paint mViewPaint;
    // 画波浪的画笔2
    private Paint mViewPaint2;
    // 画边界的画笔
    private Paint mBorderPaint;
    // 集合动画
    private AnimatorSet mAnimatorSet;

    /**
     * 这里不能调用super(context)
     * 因为早期的项目可能用的AppCompatActivity,getContex()得到的是ContextWrapper代理,并不是Context实例
     * super(context, attrs, defStyle)方法在内部自动转换为Context,比较安全
     * 自定义View统一走的时候super(context, attrs, defStyle)方法
     */
    public WaveView(Context context) {
        this(context, null, 0);
    }

    public WaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //View被窗体移除的时候释放动画资源
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
            mAnimatorSet.cancel();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //View被加载到环境中
    }

    @Override
    public void onWindowFocusChanged(boolean hasFoucus) {
        super.onWindowFocusChanged(hasFoucus);
        //View焦点变化
        if (mAnimatorSet != null) {
            if (hasFoucus) {
                if (mAnimatorSet.isStarted()) {
                    mAnimatorSet.resume();
                } else {
                    mAnimatorSet.start();
                }
            } else {
                mAnimatorSet.pause();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //用作布局中显示绘制第一帧静态图像
        createShader();
        //绘制完第一帧之后就可以启动动画了
        if (mAnimatorSet != null) mAnimatorSet.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveShader != null && mWaveShader2 != null && getWidth() > 0 && getHeight() > 0) {
            //shader为空则设置shader
            if (mViewPaint.getShader() == null) {
                mViewPaint.setShader(mWaveShader);
            }
            if (mViewPaint2.getShader() == null) {
                mViewPaint2.setShader(mWaveShader2);
            }
            // 根据 frequency 和 amplitudeRatio 拉伸高度与长度的倍数
            mShaderMatrix.setScale(frequency, amplitudeRatio / DEFAULT_AMPLI_RATIO, 0, mDefaultHeight);
            // 根据 mShiftRatio 和 heightRatio 进行水平平移指定长度
            mShaderMatrix.postTranslate(mShiftRatio * getWidth() * 1f, (DEFAULT_HEIGHT_RATIO - heightRatio) * getHeight());
            // 根据 frequency 和 amplitudeRatio 拉伸高度与长度的倍数
            mShaderMatrix2.setScale(frequency, amplitudeRatio / DEFAULT_AMPLI_RATIO, 0, mDefaultHeight);
            // 根据 mShiftRatio 和 heightRatio 进行水平平移指定长度,第二个曲线的频率设置为两倍,达到生动的波浪效果
            mShaderMatrix2.postTranslate(mShiftRatio * getWidth() * 2f, (DEFAULT_HEIGHT_RATIO - heightRatio) * getHeight());
            // 将属性Matrix应用到shader中生效
            mWaveShader.setLocalMatrix(mShaderMatrix);
            mWaveShader2.setLocalMatrix(mShaderMatrix2);
            //设置边界以及绘制背景的形状
            float borderWidth = mBorderPaint == null ? 0f : mBorderPaint.getStrokeWidth();
            switch (shape) {
                case CIRCLE:
                    //绘制外边界圆
                    if (borderWidth > 0) {
                        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, (getWidth() - borderWidth) / 2f - 1f, mBorderPaint);
                    }
                    float radius = getWidth() / 2f - borderWidth;
                    Paint paint = new Paint();
                    paint.setColor(backgroundColor);
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
                    //绘制内边界圆
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mViewPaint);
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mViewPaint2);
                    break;
                case SQUARE:
                    setBackgroundColor(backgroundColor);
                    //绘制外边界方形
                    if (borderWidth > 0) {
                        canvas.drawRect(borderWidth / 2f, borderWidth / 2f, getWidth() - borderWidth / 2f - 0.5f, getHeight() - borderWidth / 2f - 0.5f, mBorderPaint);
                    }
                    //绘制内边界方形
                    canvas.drawRect(borderWidth, borderWidth, getWidth() - borderWidth, getHeight() - borderWidth, mViewPaint);
                    canvas.drawRect(borderWidth, borderWidth, getWidth() - borderWidth, getHeight() - borderWidth, mViewPaint2);
                    break;
            }
        } else {
            mViewPaint.setShader(null);
            mViewPaint2.setShader(null);
        }
    }

    /**
     * 绘制默认两个静态波浪线
     */
    private void createShader() {
        double mDefaultAngularFrequency = 2.0f * Math.PI / DEFAULT_LENGTH_RATIO / getWidth();
        float mDefaultAmplitude = getHeight() * DEFAULT_AMPLI_RATIO;
        mDefaultHeight = getHeight() * DEFAULT_HEIGHT_RATIO;
        //用bitmap作画布,方便裁剪出圆形
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Bitmap bitmap2 = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap2);
        //初始化波浪画笔
        Paint wavePaint = new Paint();
        wavePaint.setStrokeWidth(2);
        wavePaint.setAntiAlias(true);
        wavePaint.setFilterBitmap(true);
        // 将默认的波浪绘制到bitmap,按照公式:y=Asin(ωx+φ)+h 进行绘制正弦
        final int endX = getWidth() + 1;
        final int endY = getHeight() + 1;
        float[] waveY = new float[endX];
        wavePaint.setColor(behindColor);
        //绘制前正弦
        for (int beginX = 0; beginX < endX; beginX++) {
            double wx = beginX * mDefaultAngularFrequency;
            float beginY = (float) (mDefaultHeight + mDefaultAmplitude * Math.sin(wx));
            canvas.drawLine(beginX, beginY, beginX, endY, wavePaint);
            waveY[beginX] = beginY;
        }
        //绘制后正弦
        wavePaint.setColor(frontColor);
        //颜色重叠模式:上下层都显示（另外两种:PorterDuff.Mode.DARKEN、PorterDuff.Mode.LIGHTEN）
        wavePaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN));
        final int wave2Shift = (int) (getWidth() * DEFAULT_DISTANCE_RATIO);
        for (int beginX = 0; beginX < endX; beginX++) {
            canvas2.drawLine(beginX, waveY[(beginX + wave2Shift) % endX], beginX, endY, wavePaint);
            /*double wx = beginX * mDefaultAngularFrequency;
            float beginY = (float) (mDefaultHeight + mDefaultAmplitude * Math.sin(wx));
            canvas2.drawLine(beginX, beginY, beginX, endY, wavePaint);*/
        }
        // 利用 bitamp 创建 shader
        mWaveShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
        mViewPaint.setShader(mWaveShader);
        mWaveShader2 = new BitmapShader(bitmap2, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
        mViewPaint2.setShader(mWaveShader2);
    }

    /**
     * 初始化
     *
     * @param attrs 布局属性
     */
    private void init(AttributeSet attrs) {
        if (attrs != null) {
            //初始化布局属性
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.WaveView, 0, 0);
            //高度只能大于等于0以及小于等于1
            float heightRatio = typedArray.getFloat(R.styleable.WaveView_wa_heightRatio, this.heightRatio);
            if (heightRatio >= 0 && heightRatio <= 1.0f) {
                this.heightRatio = heightRatio;
            } else {
                Log.e(TAG, "invalid heightRatio");
            }
            //振幅只能大于0以及小于等于0.5
            float amplitudeRatio = typedArray.getFloat(R.styleable.WaveView_wa_amplitudeRatio, this.amplitudeRatio);
            if (amplitudeRatio > DEFAULT_MIN_VALUE && amplitudeRatio <= DEFAULT_MAX_AMPLI_VALUE) {
                this.amplitudeRatio = amplitudeRatio;
                this.mLastAmplitudeRatio = this.amplitudeRatio;
            } else {
                Log.e(TAG, "invalid amplitudeRatio");
            }
            //根据合法性调整振幅
            this.amplitudeRatio = adjustAmplitudeRatio(this.heightRatio, this.amplitudeRatio, this.mLastAmplitudeRatio);
            //波形密度检查
            int frequency = typedArray.getInteger(R.styleable.WaveView_wa_frequency, 1);
            if (frequency > 0) {
                this.frequency = 2.0f / ((int) Math.pow(2, frequency - 1));
            } else {
                Log.e(TAG, "invalid frequency");
            }
            //双波形间距检查
            /*float distanceRatio = typedArray.getFloat(R.styleable.WaveView_distanceRatio, this.distanceRatio);
            if(distanceRatio >= 0 && distanceRatio <= 1.0f){
                this.distanceRatio = distanceRatio;
            }else{
                Log.e(TAG,"invalid distanceRatio");
            }*/
            //边界宽度
            int borderWidth = typedArray.getColor(R.styleable.WaveView_wa_borderWidth, this.borderWidth);
            if (borderWidth >= 0) {
                this.borderWidth = borderWidth;
            } else {
                Log.e(TAG, "invalid borderWidth");
            }
            //其他参数
            heightTime = typedArray.getInteger(R.styleable.WaveView_wa_heightTime, heightTime);
            shiftTime = typedArray.getInteger(R.styleable.WaveView_wa_shiftTime, shiftTime);
            frontColor = typedArray.getColor(R.styleable.WaveView_wa_frontColor, frontColor);
            behindColor = typedArray.getColor(R.styleable.WaveView_wa_behindColor, behindColor);
            shape = typedArray.getInt(R.styleable.WaveView_wa_shape, 0) == 0 ? shape : Shape.SQUARE;
            borderColor = typedArray.getColor(R.styleable.WaveView_wa_borderColor, borderColor);
            backgroundColor = typedArray.getColor(R.styleable.WaveView_wa_background, backgroundColor);
            typedArray.recycle();
        }
        //初始化画笔
        mShaderMatrix = new Matrix();
        mShaderMatrix2 = new Matrix();
        mViewPaint = new Paint();
        mViewPaint.setAntiAlias(true);
        mViewPaint.setFilterBitmap(true);
        mViewPaint2 = new Paint();
        mViewPaint2.setAntiAlias(true);
        mViewPaint2.setFilterBitmap(true);
        mAnimatorSet = new AnimatorSet();
        //边界画笔
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setFilterBitmap(true);
        mBorderPaint.setStyle(Style.STROKE);
        mBorderPaint.setColor(borderColor);
        mBorderPaint.setStrokeWidth(borderWidth);
        //初始化动画
        List<Animator> animators = new ArrayList<>();
        //水平平移动画,指定时间偏移一个画布长度
        animators.add(getShiftAnimator(this.shiftTime));
        //竖直平移动画,指定时间偏移到默认高度
        animators.add(getHeightAnimator(this.heightTime));
        //集合动画播放
        mAnimatorSet.playTogether(animators);
    }

    /**
     * 得到屏幕尺寸
     *
     * @return 尺寸
     */
    private DisplayMetrics getScreenMetrics() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * 获取水平偏移相对整个画布长度比例（范围:0 ~ 1）
     *
     * @return float 高度比例
     */
    private float getShiftRatio() {
        return mShiftRatio;
    }

    /**
     * 设置水平偏移的相对整个画布长度比例（范围:0 ~ 1）
     *
     * @param waveShiftRatio 水平偏移的长度比例
     */
    private void setShiftRatio(@FloatRange(from = 0.0f, to = 1.0f) float waveShiftRatio) {
        if (mShiftRatio != waveShiftRatio) {
            mShiftRatio = waveShiftRatio;
            invalidate();
        }
    }

    /**
     * 获取水平偏移动画
     *
     * @param shiftTime 动画时间
     */
    private ObjectAnimator getShiftAnimator(int shiftTime) {
        //水平平移动画,指定时间偏移一个画布长度
        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(this, "shiftRatio", 0f, 2f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(shiftTime);
        waveShiftAnim.setRepeatMode(ValueAnimator.RESTART);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        return waveShiftAnim;
    }

    /**
     * 获取竖直偏移动画
     *
     * @param heightTime 动画时间
     */
    private ObjectAnimator getHeightAnimator(int heightTime) {
        //竖直平移动画,指定时间偏移到默认高度
        ObjectAnimator waveLevelAnim = ObjectAnimator.ofFloat(this, "heightRatio", 0f, getHeightRatio());
        waveLevelAnim.setDuration(heightTime);
        waveLevelAnim.setInterpolator(new DecelerateInterpolator());
        return waveLevelAnim;
    }

    /**
     * 获取波浪高度比例（范围:0 ~ 1）
     *
     * @return float 高度比例
     */
    private float getHeightRatio() {
        return heightRatio;
    }

    /**
     * 设置波浪相对整个画布高度比例（范围:0 ~ 1）,amplitudeRatio + heightRatio <= 1 体验最佳
     *
     * @param heightRatio 高度比例
     */
    public void setHeightRatio(@FloatRange(from = 0.0f, to = 1.0f) float heightRatio) {
        if (heightRatio > 1f) {
            heightRatio = 1f;
        }
        if (this.heightRatio != heightRatio && heightRatio >= 0) {
            this.heightRatio = heightRatio;
            //得到合理的振幅
            this.amplitudeRatio = adjustAmplitudeRatio(this.heightRatio, this.amplitudeRatio, this.mLastAmplitudeRatio);
            //invalidate();
        }
    }

    /**
     * 调整振幅,保证平滑处理
     *
     * @return float 最后输出的振幅
     */
    private float adjustAmplitudeRatio(float heightRatio, float amplitudeRatio, float lastAmplitudeRatio) {
        float ratio = amplitudeRatio;
        //恢复上次修改的值
        if (ratio != lastAmplitudeRatio) {
            ratio = lastAmplitudeRatio;
        }
        //微调
        if ((ratio + heightRatio > 1)) {
            //最高不能超过整个画布,且振幅不为0
            ratio = 1f - heightRatio + DEFAULT_MIN_VALUE;
        } else {
            //振幅不能大于高度
            if (heightRatio < lastAmplitudeRatio) {
                ratio = heightRatio;
            }
        }
        return ratio;
    }

    /**
     * 设置两个正弦波浪的距离相对一个波浪长度的比例（范围:0 ~ 1）
     *
     * @param distanceRatio 距离比例
     */
    private void setDistanceRatio(@FloatRange(from = 0.0f, to = 1.0f) float distanceRatio) {
        if (this.distanceRatio != distanceRatio && getWidth() > 0 && getHeight() > 0) {
            this.distanceRatio = distanceRatio;
            //需要重新创建shader
            mWaveShader = null;
            createShader();
            //invalidate();
        }
    }
    /* -------------------------------------------- public -------------------------------------------- */

    /**
     * 设置画布背景的形状:圆形或者方形
     *
     * @param Shape 形状的枚举类
     */
    public void setShape(Shape Shape) {
        if (this.shape != Shape) {
            this.shape = Shape;
            //invalidate();
        }
    }

    /**
     * 设置振幅相对整个画布高度比例（范围:0 ~ 0.5）,amplitudeRatio + heightRatio <= 1 体验最佳
     *
     * @param amplitudeRatio 振幅比例
     */
    public void setAmplitudeRatio(@FloatRange(from = DEFAULT_MIN_VALUE, to = DEFAULT_MAX_AMPLI_VALUE) float amplitudeRatio) {
        if (this.amplitudeRatio != amplitudeRatio && amplitudeRatio <= DEFAULT_MAX_AMPLI_VALUE && amplitudeRatio >= DEFAULT_MIN_VALUE) {
            this.mLastAmplitudeRatio = amplitudeRatio;
            //得到合理的振幅
            this.amplitudeRatio = adjustAmplitudeRatio(heightRatio, amplitudeRatio, mLastAmplitudeRatio);
            //invalidate();
        }
    }

    /**
     * 波浪密度也即是频率:求水平容纳波浪个数,一个上下波峰表示一个波浪数
     *
     * @param frequency 水平波浪数目
     */
    public void setFrequency(int frequency) {
        if (frequency <= 0) {
            frequency = 1;
        }
        //求2的次方
        int powNumber = (int) Math.pow(2, frequency - 1);
        //求倒数
        float backNumber = 2.0f / powNumber;
        if (backNumber != frequency) {
            this.frequency = backNumber;
            //invalidate();
        }
    }

    /**
     * 设置前波浪的颜色
     *
     * @param frontColor 前波浪颜色
     */
    public void setFrontColor(int frontColor) {
        if (getWidth() > 0 && getHeight() > 0 && this.frontColor != frontColor) {
            this.frontColor = frontColor;
            //需要重新创建shader
            mWaveShader = null;
            createShader();
            //invalidate();
        }
    }

    /**
     * 设置后波浪的颜色
     *
     * @param behindColor 后波浪颜色
     */
    public void setBehindColor(int behindColor) {
        if (getWidth() > 0 && getHeight() > 0 && this.behindColor != behindColor) {
            this.behindColor = behindColor;
            //需要重新创建shader
            mWaveShader = null;
            createShader();
            //invalidate();
        }
    }

    /**
     * 设置偏移动画时间
     *
     * @param shiftTime 动画时间
     */
    public void setShiftTime(int shiftTime) {
        this.shiftTime = shiftTime;
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.play(getShiftAnimator(this.shiftTime));
        mAnimatorSet.start();
    }

    /**
     * 设置边界的宽度(像素单位)
     *
     * @param width 宽度
     */
    public void setBorderWidth(int width) {
        mBorderPaint.setStrokeWidth(width);
        //invalidate();
    }

    /**
     * 设置边界的颜色
     *
     * @param color 颜色
     */
    public void setBorderColor(int color) {
        mBorderPaint.setColor(color);
        //invalidate();
    }

    //波浪背景娿形状,圆形与方形
    public enum Shape {
        CIRCLE, SQUARE
    }
}
