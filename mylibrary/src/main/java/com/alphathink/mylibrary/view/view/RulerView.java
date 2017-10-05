package com.alphathink.mylibrary.view.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.alphathink.mylibrary.R;

import java.math.BigDecimal;

/**
 * Created by Panda on 2017/9/28.
 */

public class RulerView extends View {
    private int background;//背景颜色
    private Paint paintBackground,paintLine,paintText,paintUnit,paintUnitText;//各个画笔
    private int scale_margin;//刻度间距
    private int scale_count;//每个大刻度间小刻度数目
    private int max,min;//最大最小值
    private String unit;//单位
    private VelocityTracker velocityTracker;//速度追踪器
    private boolean continueScroll;//滑动flag
    private float currentPosition,originPosition;//当前位置，初始位置
    private float origin_x,current_x;//初始x坐标，当前x坐标
    private float moved_x;//x轴移动距离
    private float speed;//移动速度
    private float start_x;//开始画的x坐标
    private int distance_long;//每个大刻度间px距离
    private float border_left,border_right;//左右边距的x位置
    private int width_screen;//屏幕宽

    private OnPositionChangedListener listener;//自定义接口回调获取position值
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (null != listener) {
                float position = (float) (Math.round(currentPosition * 10)) / 10;//保留一位小数
                listener.PositionChanged(position);
            }
        }
    };

    public RulerView(Context context){
        super(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //处理xml布局传递过来的属性
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.RulerView,0,0);
        try {
            background = array.getColor(R.styleable.RulerView_back_color, 0xFBDE00);
            scale_margin = array.getInt(R.styleable.RulerView_scale_margin,20);
            scale_count = array.getInt(R.styleable.RulerView_scale_count,10);
            min = array.getInt(R.styleable.RulerView_value_min,0);
            max = array.getInt(R.styleable.RulerView_value_max,100);
            if (null == array.getString(R.styleable.RulerView_unit)){
                unit = "cm";
            }else{
                unit = array.getString(R.styleable.RulerView_unit);
            }
        }finally {
            //TypedArray共享属性 需要回收
            array.recycle();
        }

        //获取屏幕显示器宽
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        width_screen = wm.getDefaultDisplay().getWidth();
        //初始化x位置
        originPosition = (max - min)/2;
        currentPosition = originPosition;
        distance_long =  scale_count * scale_margin;

        border_left = width_screen/2 - ((min + max)/2 - min)*distance_long;

        border_right = width_screen/2 + ((min + max)/2 - min)*distance_long;
        origin_x = (border_left + border_right)/2;
        start_x = border_left;
        current_x = width_screen/2;

        //初始化画笔等资源
        initResource();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //画矩形底框
        super.onDraw(canvas);//回调父类前后处理
        drawRect(canvas);

        //画倒三角指示器
        drawPointer(canvas);

        //画刻度 区分长短
        drawScale(canvas);
    }

    /**
     * 初始化画笔资源
     */
    public void initResource(){
        //画笔--背景图黄色矩形
        paintBackground = new Paint();
        paintBackground.setColor(background);
        paintBackground.setStyle(Paint.Style.FILL);

        //画笔--三角行、长短竖线  因为颜色样式一样  节省资源用一个画笔就可以了
        paintLine = new Paint();
        paintLine.setColor(getResources().getColor(R.color.withe));
        paintLine.setStyle(Paint.Style.FILL);
        paintLine.setStrokeWidth(5);

        //画笔--画字
        paintUnitText = new Paint();
        paintUnitText.setTextSize(100);
        paintUnitText.setColor(Color.RED);
        paintUnitText.setTextAlign(Paint.Align.CENTER);
        paintLine.setStyle(Paint.Style.FILL);

        //画笔--画小一点的单位
        paintUnit = new Paint();
        paintUnit.setTextSize(50);
        paintUnit.setColor(Color.BLACK);
        paintUnit.setStyle(Paint.Style.FILL);

        //画笔--画刻度字体
        paintText = new Paint();
        paintText.setTextSize(50);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setStyle(Paint.Style.FILL);

    }

    /**
     * 刻度计算 主要是位置坐标的计算
     * @param canvas
     */
    public void drawScale(Canvas canvas){
        float start_y = getMeasuredHeight() / 5 * 2;
        for (int i = min;i <= max;i++){
            //画位置数字
            canvas.drawText(String.valueOf(i),
                    start_x + (i - min)*scale_count*scale_margin,
                    start_y +200,
                    paintText);
            //画长刻度线
            canvas.drawLine(start_x + (i - min)*scale_count*scale_margin,
                    start_y,
                    start_x + (i - min)*scale_count*scale_margin,
                    start_y + 120,
                    paintLine);

            //画短刻度线
            for (int j = 1; j < scale_count;j++){
                if (i == max)continue;
                canvas.drawLine(start_x + (i - min)*scale_count*scale_margin+j*scale_margin,
                        start_y,
                        start_x + (i - min)*scale_count*scale_margin+j*scale_margin,
                        start_y + 60,
                        paintLine);
            }
        }
    }

    /**
     * 画三角指示器
     * @param canvas
     */
    public void drawPointer(Canvas canvas){
        //画文字
        float text_x = getMeasuredWidth()/2;
        float text_y = getMeasuredHeight()/5 * 2 - 30;
        float v = (float) (Math.round(currentPosition * 10)) / 10;
        canvas.drawText(String.valueOf(v),text_x,text_y,paintUnitText);
        canvas.drawText(unit,text_x + 100,text_y-40,paintUnit);

        //画倒三角
        float center_x = getMeasuredWidth()/2;
        float center_y = getMeasuredHeight()/5 * 2;
        Path path = new Path();
        path.moveTo(center_x - 50,center_y);
        path.lineTo(center_x ,center_y + 50);
        path.lineTo(center_x + 50,center_y);
        path.close();
        canvas.drawPath(path,paintLine);
    }

    /**
     * 画背景矩形框
     * @param canvas
     */
    public void drawRect(Canvas canvas){
        float start_x = getX();
        float start_y = getMeasuredHeight()/5 * 2;
        canvas.drawRect(start_x,start_y,getMeasuredWidth(),getMeasuredHeight()- 2,paintBackground);
    }


    /**
     * 重写触摸事件，实现自己的滑动策略，这里主要是处理了x坐标方向的位置计算 ，同理要实现y轴的移动 或是自由xy轴移动可
     * 相应处理
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pos = event.getX();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                moved_x = pos;
                //初始化速度追踪器
                continueScroll = false;
                if(velocityTracker == null){
                    velocityTracker = VelocityTracker.obtain();
                }else{
                    velocityTracker.clear();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);            //速度追踪器事件添加
                float moved_distance = (int) (moved_x - pos);  //手指移动的距离
                start_x -= moved_distance;                     //计算移动后的x位置
                current_x -= moved_distance;
                calculateCurrentPosition();                   //计算当前位置
                invalidate();                                   //界面重绘
                moved_x = pos;                                  //替换当前x坐标为滑动到的位置
                break;
            case MotionEvent.ACTION_UP:
                confirmBorder();                              //确定边界
                velocityTracker.computeCurrentVelocity(1000);
                speed = velocityTracker.getXVelocity();      //获取x方向速度值
                float min_speed = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();//获取允许手势操作最小速度值
                if (Math.abs(speed) > min_speed){//如果有速度继续滑动，否则回收
                    continueScroll = true;
                    continueScroll();
                }else{
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }
        return true;
    }

    /**
     * 计算当前位置
     */
    public void calculateCurrentPosition(){
        Log.v("sss","daole");
        //计算当前刻度位置
        float offset_x_total = current_x - origin_x;   //移动的x方向总距离
        int offset_big = (int) (offset_x_total/distance_long);//移动的大刻度个数
        float offset_scale = offset_x_total % distance_long; //移动的小刻度

        //保留移动位置小数1位
        int offset_Small = (new BigDecimal(offset_scale / scale_margin).setScale(0,
                BigDecimal.ROUND_HALF_UP)).intValue();
        float offset = offset_big + offset_Small * 0.1f; //移动的总位置

        if (originPosition - offset > max){             //滑动到最大位置
            currentPosition = max;
        }else if (originPosition - offset < min){       //滑动到最小位置
            currentPosition = min;
        }else{                                          //中间情况
            currentPosition = originPosition - offset;
        }
        handler.sendEmptyMessage(0);               //回调通知
    }

    /**
     * 确认是否到边界
     */
    public void confirmBorder(){
        if (current_x < border_left){
            current_x = border_left;
            start_x =border_left - (border_right - border_left)/2;
            postInvalidate();
        }else if (current_x > border_right){
            current_x = border_right;
            start_x = border_left + (border_right - border_left)/2;
            postInvalidate();
        }
    }

    /**
     * 手指抬起后继续惯性滑动
     */
    private void continueScroll() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                float speedAbs = 0;//速度绝对值
                if (speed > 0 && continueScroll) {//根据速度的正负判断方向，做相应位置调动
                    speed -= 50;
                    start_x += speed * speed / 1000000;
                    current_x += speed * speed / 1000000;
                    speedAbs = speed;
                } else if (speed < 0 && continueScroll) {
                    speed += 50;
                    start_x -= speed * speed / 1000000;
                    current_x -= speed * speed / 1000000;
                    speedAbs = -speed;
                }
                calculateCurrentPosition();
                confirmBorder();
                postInvalidate();
                if (continueScroll && speedAbs > 0) {
                    post(this);
                } else {
                    continueScroll = false;
                }
            }
        }).start();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width_screen,measureHeight(heightMeasureSpec));

    }


    /**
     * 测量高
     * @param measureSpec
     * @return
     */
    public int measureHeight(int measureSpec){
        int contentHeight = 200;
        int result = contentHeight * 2;
        int measureMode = MeasureSpec.getMode(measureSpec);
        int measureSize = MeasureSpec.getSize(measureSpec);

        if (measureMode == MeasureSpec.EXACTLY){
            result = Math.max(result,measureSize);
        }else{
            result = Math.min(result,measureSize);
        }
        return result;
    }

    /**
     * 自定义接口实现位置改变响应回调
     */
    public interface OnPositionChangedListener{
        void PositionChanged(float position);
    }

    /**
     * 设置监听
     * @param listener
     */
    public void setOnPositionChangerListener(OnPositionChangedListener listener){
        this.listener = listener;
    }

}
