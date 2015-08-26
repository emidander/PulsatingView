package se.erikmidander.pulsatingviewtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class PulsatingView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = "PulsatingView";

    private static final int NUMBER_OF_CIRCLES = 3;
    private static final float ANIMATION_TIME_IN_SECONDS = 3.0f;

    private AnimationThread thread;

    private Paint circlePaint;
    private Point center;

    private long lastFrameTime;

    private float startSize;
    private float endSize;

    private List<Float> circles;
    private int firstCircleIndex;

    public PulsatingView(Context context) {
        this(context, null);
    }

    public PulsatingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PulsatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setupDrawing();

        if (!isInEditMode()) {
            setZOrderOnTop(true);
        }
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        getHolder().addCallback(this);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        center = new Point(w / 2, h / 2);
        float smallestDimension = w < h ? w : h;
        float radius = (float)smallestDimension / 2.0f;
        startSize = radius * 0.2f;
        endSize = radius * 1.0f;
    }

    public void setupDrawing() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        circles = new ArrayList<Float>();
        for (int i = 0; i < NUMBER_OF_CIRCLES; i++) {
            circles.add(new Float(-(float) i / (float) NUMBER_OF_CIRCLES));
        }
    }

    protected void draw(Canvas canvas, long timeDelta) {

        // Clear background
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(rect, paint);

        float progress = 1.0f / ANIMATION_TIME_IN_SECONDS * timeDelta / 1000.0f;
        float sizeDelta = endSize - startSize;
        int start = firstCircleIndex;
        int end = firstCircleIndex + NUMBER_OF_CIRCLES;
        for (int i = start; i < end; i++) {
            int index = i % NUMBER_OF_CIRCLES;
            float circle = circles.get(index);
            if (circle > 0.0f) {
                int alpha = (int) (255.0f * (1.0f - 1.0f * circle));
                circlePaint.setARGB(alpha, 255, 255, 255);
                canvas.drawCircle(center.x, center.y, startSize + sizeDelta * circle, circlePaint);
            }
            circle += progress;
            if (circle > 1.0) {
                circle -= 1.0;
                firstCircleIndex++;
            }
            circles.set(index, circle);
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        draw(canvas, 0);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new AnimationThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {

            }
        }
    }

    class AnimationThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private PulsatingView pulsatingView;
        private boolean running = false;

        private long fpsCount;
        private long fpsTotal;

        public AnimationThread(SurfaceHolder surfaceHolder, PulsatingView pulsatingView) {
            this.surfaceHolder = surfaceHolder;
            this.pulsatingView = pulsatingView;
        }

        public void setRunning(boolean run) {
            this.running = run;
        }

        public SurfaceHolder getSurfaceHolder() {
            return surfaceHolder;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (running) {
                canvas = null;

                long timeStarted = System.currentTimeMillis();
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas != null && surfaceHolder.getSurface().isValid()) {
                        //synchronized (surfaceHolder) {
                            if (canvas != null) {
                                pulsatingView.draw(canvas, lastFrameTime == 0 ? 0 : timeStarted - lastFrameTime);
                            }
                        //}
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
                lastFrameTime = timeStarted;

                // Framerate
                long timeEnd = System.currentTimeMillis();
                long timeDelta = timeEnd - timeStarted;
                if (timeDelta < 16) {
                    try {
                        Thread.sleep(16 - timeDelta);
                    } catch (InterruptedException e) {

                    }
                }

                fpsCount++;
                fpsTotal += timeDelta;
                if (fpsCount > 60) {
                    Log.d(LOG_TAG, String.format("Avg drawing time = %d", fpsTotal / fpsCount));
                    fpsCount = 0;
                    fpsTotal = 0;
                }
            }
        }
    }
}
