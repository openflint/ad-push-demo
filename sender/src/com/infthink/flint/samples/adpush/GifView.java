package com.infthink.flint.samples.adpush;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class GifView extends View {
    private final static int BUFFER_SIZE = 4096;
    private Movie mMovie;
    private long mMovieStart;
    private Executor mExecutor = Executors.newFixedThreadPool(20,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("GIF Thread");
                    return th;
                }
            });

    public GifView(Context context) {
        super(context);
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GifView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        boolean isWidMat = lp.width == LayoutParams.MATCH_PARENT;
        boolean isHeiMat = lp.height == LayoutParams.MATCH_PARENT;
        boolean isWidWra = lp.width == LayoutParams.WRAP_CONTENT;
        boolean isHeiWra = lp.height == LayoutParams.WRAP_CONTENT;
        if (isWidMat && isHeiMat) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        if (mMovie != null) {
            requestLayout();
            setMeasuredDimension(
                    isWidMat ? MeasureSpec.getSize(widthMeasureSpec)
                            : isWidWra ? mMovie.width() : lp.width,
                    isHeiMat ? MeasureSpec.getSize(heightMeasureSpec)
                            : isHeiWra ? mMovie.height() : lp.height);
        } else {
            setMeasuredDimension(
                    isWidMat ? MeasureSpec.getSize(widthMeasureSpec)
                            : isWidWra ? 0 : lp.width,
                    isHeiMat ? MeasureSpec.getSize(heightMeasureSpec)
                            : isHeiWra ? 0 : lp.height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long curTime = android.os.SystemClock.uptimeMillis();

        if (mMovie != null) {
            if (mMovieStart == 0) {
                mMovieStart = curTime;
            }

            int duration = mMovie.duration();
            if (duration == 0) {
                duration = 1000;
            }

            int relTime = (int) ((curTime - mMovieStart) % duration);
            mMovie.setTime(relTime);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            mMovie.draw(canvas, 0, 0);
            canvas.restore();
            invalidateView();
        }
    }

    @SuppressLint("NewApi")
    private void invalidateView() {
        if (getVisibility() == View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                postInvalidateOnAnimation();
            } else {
                invalidate();
            }
        }
    }

    private final Handler handler = new Handler(this.getContext()
            .getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:
                if (msg.obj == null) {
                    return;
                }
                Log.e("look", "º”‘ÿÕÍ±œ£°£°£°");
                setResource((byte[]) msg.obj);
                break;

            default:
                break;
            }
        }
    };

    private void setResource(byte[] obj) {
        mMovie = Movie.decodeByteArray(obj, 0, obj.length);
        requestLayout();
    }

    public void setResource(final String urlstr) {
        mExecutor.execute(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(urlstr);
                    HttpURLConnection connection = (HttpURLConnection) url
                            .openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestMethod("GET");
                    if (connection.getResponseCode() == 200) {
                        InputStream is = connection.getInputStream();

                        byte[] buffer = getByte(is);
                        handler.obtainMessage(1, buffer).sendToTarget();
                    } else {
                        Log.e("getResponseCode", connection.getResponseCode()
                                + ":");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static byte[] getByte(InputStream in) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return outStream.toByteArray();
    }
}
