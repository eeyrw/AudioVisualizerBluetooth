package com.customview.graph;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BarGraphView extends SurfaceView implements
		SurfaceHolder.Callback, OnGestureListener {

	private SurfaceHolder mSfHolder;
	private SurfaceUpdateThread mSfUpdateThread;
	private boolean mDrawThreadFlag;
	private int mSurfaceHeight;
	private int mSurfaceWidth;
	private float[] mValueArray;
	private int mBackColor;
	private int mBarColor;
	private GestureDetector mGestureDetector;

	private String mExtraText;

	private Object token = null;

	public int getBackColor() {
		return mBackColor;
	}

	public void setBackColor(int color) {
		mBackColor = color;
	}

	public String getExtraText() {
		return mExtraText;
	}

	public void setExtraText(String text) {
		mExtraText = text;
	}

	private void initDefaultParam(Context context) {
		mSfHolder = getHolder();
		mSfHolder.addCallback(this);
		mDrawThreadFlag = false;
		token = new Object();
		mGestureDetector = new GestureDetector(context, this);

		mBackColor = Color.RED;
		mBarColor = Color.WHITE;
		mExtraText = "BarGraphView";
	}

	public BarGraphView(Context context, AttributeSet attrs) {

		super(context, attrs);
		initDefaultParam(context);

	}

	public synchronized void setValueArray(float[] array) {
		mValueArray = array;
		synchronized (token) {
			token.notify();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder mSfHolder, int format, int width,
			int height) {
		mSurfaceHeight = height;
		mSurfaceWidth = width;

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		mDrawThreadFlag = true;
		mSfUpdateThread = new SurfaceUpdateThread();
		mSfUpdateThread.start();

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		mDrawThreadFlag = false;

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	class SurfaceUpdateThread extends Thread {
		@Override
		public synchronized void run() {
			SurfaceHolder runholder = mSfHolder;
			Canvas canvas = null;
			
			
			Paint barPaint = new Paint();
			Paint textPaint = new Paint();
			barPaint.setColor(mBarColor);
			barPaint.setStyle(Style.FILL);

			textPaint.setAntiAlias(true);
			textPaint.setColor(mBarColor);
			textPaint.setStyle(Style.FILL);
			textPaint.setTextSize(mSurfaceHeight * 0.06f);
			
			
	
			
			FontMetrics fm = new FontMetrics();

			textPaint.getFontMetrics(fm);
			
			int textHeight=(int)(fm.bottom-fm.top);
			int textWidth=(int) textPaint.measureText(mExtraText);
			int textBaseline=(int)(-fm.ascent);
			
			Bitmap textBitmap=Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888);
			
			Canvas textCanvas=new Canvas(textBitmap);
			textCanvas.drawColor(Color.TRANSPARENT);
			textCanvas.drawText(mExtraText, 0, textBaseline, textPaint);
			
			
			while (mDrawThreadFlag) {

				synchronized (token) {
					try {
						token.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				try {
					if (mValueArray != null) {
						int sampleNum = mValueArray.length;
						float barWidth = (float) mSurfaceWidth / sampleNum;
						canvas = runholder.lockCanvas();
						if (canvas != null) {
							canvas.drawColor(mBackColor);// 这里是绘制背景
							// canvas.drawRect(left, top, right, bottom, paint)
							canvas.drawBitmap(textBitmap, mSurfaceHeight*0.05f, mSurfaceHeight*0.05f, null);
							for (int i = 0; i < sampleNum; i++) {
								float barHeight = mValueArray[i]
										* mSurfaceHeight;
								canvas.drawRect(barWidth * i, mSurfaceHeight
										- barHeight, barWidth * (i + 1),
										mSurfaceHeight, barPaint);
							}
						}
					}
				} catch (Exception e) {
				} finally {
					if (canvas != null)
						runholder.unlockCanvasAndPost(canvas);
				}

			}
		}
	}
}