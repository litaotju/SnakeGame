package com.tju.snakegame;

import java.io.IOException;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameActivity extends Activity {

	Canvas canvas;
	SnakeView snakeView;
	Bitmap headBitmap;
	Bitmap midBitmap;
	Bitmap tailBitmap;
	Bitmap appleBitmap;
	
	//竖直方向的bitmap
	Bitmap headBitmapV;
	Bitmap midBitmapV;
	Bitmap tailBitmapV;
	
	Bitmap headBitmapVR;
	Bitmap headBitmapHR;
	
	private SoundPool soundPool;
	int soundID1;
	int soundID2;
	int soundID3;
	int soundID4;
	
	int screenWidth;
	int screenHight;
	int topGap;

	final int numBlockWide = 40;
	int blockSize;
	int numBlockHight;
		
	long lastFrameTime;
	int fps;
	
	int score;
	int hi;
	
	int [] snakeX;
	int [] snakeY;
	
	//控制身体中各个部分的显示方向
	int [] snakeBodyDir;
	final int bodyHorizon = 1;
	final int bodyVertical = 0;
	
	//主要用来控制头的显示方向
	final int bodyHorizonR = 2;
	final int bodyVerticalR = 3;
	
	int appleX;
	int appleY;
	int snakeLength;
	int directionOfTravel;
	// 0 = up, 1= right, 2= down, 3= left

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		//loadSound();
		configureDisplay();
		snakeView = new SnakeView(this, SnakeView.easyLevel);
		setContentView( snakeView);
		Log.i("Info", "GameActivity.OnCreated()");
	}

	class SnakeView extends SurfaceView implements Runnable, Callback{
		
		int diffcultLevel;
		public static final int easyLevel = 1;
		public static final int mediumLevel = 2;
		public static final int hardLevel = 3;
		
		public static final int timeToSleepAfterDead = 1000;
		public static final int frameInterStopTime = 100;
		
		Thread ourThread = null;
		SurfaceHolder ourHolder;
		volatile boolean playingState = true; 
		Paint paint;
		Context context;
		
		public SnakeView(Context context, int level) {
			super(context);
			this.context = context;
			ourHolder = getHolder();
			ourHolder.addCallback(this);
			paint = new Paint();
			snakeX = new int[200];
			snakeY = new int[200];
			snakeBodyDir = new int[200];
			diffcultLevel = level;
			initSnake();
			getApple();
		}
	  
		@Override  
	    public void surfaceCreated(SurfaceHolder holder) {  
	        ourThread= new Thread(this); // 创建一个线程对象  
	        playingState = true; // 把线程运行的标识设置成true  
	        ourThread.start(); // 启动线程  
	    }  
	  
		@Override
		public void run() {
			while(playingState){
				updateGame();
				drawGame();
				controlFPS();
			}
		}	
		
		private void initSnake(){
			//初始方向。初始长度，初始蛇的位置
			directionOfTravel = new Random().nextInt(3);
			snakeLength = 3;
			snakeX[0] = numBlockWide /2;
			snakeY[0] = numBlockHight /2;
			snakeX[1] = snakeX[0]-1;
			snakeY[1] = snakeY[0];
			snakeX[2] = snakeX[1] -1;
			snakeY[2] = snakeY[0];
			snakeBodyDir[0] = bodyHorizon;
			snakeBodyDir[1] = bodyHorizon;
			snakeBodyDir[2] = bodyHorizon;
		}
		
		private void getApple(){
			//随意产生一个苹果的位置。
			Random random = new Random();
			appleX = random.nextInt( numBlockWide  -1) +1 ;
			appleY = random.nextInt( numBlockHight -1)  +1 ;
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent motionEvent){
			//根据触摸的位置来决定调整的方向。
			// directionOfTravel++是顺时针， direactionOfTravel--是逆时针
			switch( motionEvent.getAction() & MotionEvent.ACTION_MASK ){
			case MotionEvent.ACTION_UP:
				if( motionEvent.getX() > screenWidth/2) { //
					//向右旋转
					directionOfTravel ++;
					if( directionOfTravel == 4){
						directionOfTravel = 0;
					}
				}else{
					//向左转
					directionOfTravel --;
					if(directionOfTravel == -1){
						directionOfTravel = 3;
					}
				}
			}
			return true;
		}
		
		private void updateGame(){
			if(snakeX[0] == appleX && snakeY[0] == appleY ){
				snakeLength++;
				getApple();
				computeScore();
				//soundPool.play(soundID1, 1, 1, 0, 0, 1);
			}
			//更新尾部的时候是确定的。
			for(int i = snakeLength -1; i>0; i--){
				if( snakeX[i-1] != snakeX[i] ){
					//水平绘制这个块
					snakeBodyDir[i] = bodyHorizon;
				} else if( snakeY[ i-1] != snakeY[i]) {
					//竖直绘制这个块
					snakeBodyDir[i] = bodyVertical;
				}
				snakeX[i] = snakeX[ i-1];
				snakeY[i] = snakeY[ i-1];
			}
			//更新头的时候一次
			switch( directionOfTravel){
				case 0:{ //UP
					snakeY[0] --;
					snakeBodyDir[0] = bodyVerticalR;
					break;
				}
				case 1: {//RIGHT
					snakeX[0] ++;
					snakeBodyDir[0] = bodyHorizon;
					break;
				}
				case 2: {//down
					snakeY[0] ++;
					snakeBodyDir[0] = bodyVertical;
					break;
				}
				case 3:{//left
					snakeX[0] --;
					snakeBodyDir[0] = bodyHorizonR;
					break;
				}
			}
			if( checkDead()){
				//重新开始
				score = 0;
				if( ourHolder.getSurface().isValid()){
					canvas = ourHolder.lockCanvas();
					canvas.drawText("Oh. It's dead.", screenWidth/2, screenHight/2, paint);
					try{
						Thread.sleep(timeToSleepAfterDead);
					}catch( InterruptedException e){
					}
					ourHolder.unlockCanvasAndPost(canvas);
				}
				initSnake();
				getApple();
			}
		}
		
		private boolean checkDead(){
			if( diffcultLevel > mediumLevel){
				if( snakeX[0] == -1 || snakeY[0] == -1 || snakeX[0] > numBlockWide || 
						snakeY[0] > numBlockHight){
					return true;
				}
			} else{
				//撞墙继续返回来
				if( snakeX[0] == -1){
					snakeX[0] = numBlockWide;
				}
				if( snakeX[0] > numBlockWide){
					snakeX[0] = 0;
				}
				if( snakeY[0] == -1){
					snakeY[0] = numBlockHight;
				}
				if( snakeY[0] == numBlockHight){
					snakeY[0] = 0;
				}			
			}
			//撞自己
			for( int i = snakeLength -1; i >= 0; i--){
				if( i > 4 && (snakeX[0] == snakeX[i]) && snakeY[0] == snakeY[i]){
					return true;
				}
			}
			return false;
		}
		private void computeScore(){
			score += snakeLength;
		};
		
		private void drawGame(){
			if( ourHolder.getSurface().isValid()){
				canvas = ourHolder.lockCanvas();
				canvas.drawColor(Color.BLACK);
				paint.setColor(Color.rgb(255, 255, 255));
				paint.setTextSize(topGap/2);
				//当前局面和最高分
				canvas.drawText("Socore: " + score + " Hi: " + hi, 10, topGap-6, paint);
				
				//画四条线
				paint.setStrokeWidth(3);
				canvas.drawLine(1, topGap, screenWidth-1, topGap, paint);
				canvas.drawLine(screenWidth-1, topGap,    screenWidth-1, topGap+(numBlockHight *blockSize),paint);
				canvas.drawLine(screenWidth-1,topGap+(numBlockHight*blockSize),
						         1, topGap+(numBlockHight*blockSize), paint );
				canvas.drawLine(1,topGap, 1, topGap+(numBlockHight *blockSize), paint);
				


				//Snake
				switch (snakeBodyDir[0]){
					case bodyHorizon:
						canvas.drawBitmap(headBitmap, snakeX[0] * blockSize, snakeY[0] *blockSize+topGap, paint);
						break;
					case bodyHorizonR:
						canvas.drawBitmap(headBitmapHR, snakeX[0] * blockSize, snakeY[0] *blockSize+topGap, paint);
						break;
					case bodyVertical:
						canvas.drawBitmap(headBitmapV, snakeX[0] * blockSize, snakeY[0] *blockSize+topGap, paint);
						break;
					case bodyVerticalR:
					canvas.drawBitmap(headBitmapVR, snakeX[0] * blockSize, snakeY[0] *blockSize+topGap, paint);
						break;
				}
				//Body
				for(int i =1; i < snakeLength-1; i++){
					if (snakeBodyDir[i] == bodyHorizon)
						canvas.drawBitmap(midBitmap, snakeX[i] * blockSize, snakeY[i] *blockSize+ topGap, paint);
					else
						canvas.drawBitmap(midBitmapV, snakeX[i] * blockSize, snakeY[i] *blockSize+ topGap, paint);
				}
				//tail
				if (snakeBodyDir[snakeLength-1] == bodyHorizon)
					canvas.drawBitmap(tailBitmap, snakeX[snakeLength-1] * blockSize,
							snakeY[snakeLength-1] *blockSize + topGap, paint);
				else
					canvas.drawBitmap(tailBitmapV, snakeX[snakeLength-1] * blockSize,
							snakeY[snakeLength-1] *blockSize + topGap, paint);
				//apple
				canvas.drawBitmap(appleBitmap, appleX * blockSize, appleY *blockSize + topGap, paint);
				
				ourHolder.unlockCanvasAndPost(canvas);
			}
		}
		
		private void controlFPS(){
			long thisFrameTime = System.currentTimeMillis() - lastFrameTime;
			long timeToSleep = frameInterStopTime - thisFrameTime;
			if( thisFrameTime > 0)
				fps = (int) (1000 / thisFrameTime);
			if( timeToSleep > 0){
				try{
					Thread.sleep(timeToSleep);
				}catch( InterruptedException e){
				}
			}
			lastFrameTime = System.currentTimeMillis();
		}
		
		public void pause(){
			playingState = false;
			try {
				ourThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public void resume(){
			Log.i("Debug:", "SnakeView resumed()");
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {
		}
		
	    @Override  
	    public void surfaceDestroyed(SurfaceHolder holder) {  
	        playingState = false; // 把线程运行的标识设置成false  
	        ourHolder.removeCallback(this); 
	        Log.i("Info", "SnakeView.surfaceDestroyed()");
	    }  
	  
	}
	
	//根据屏幕的大小来设定网格的数量
	private void configureDisplay(){
		
		Display display = this.getWindowManager().getDefaultDisplay();
		Point point = new Point();
		
		display.getSize(point);
		screenWidth = point.x;
		screenHight = point.y;
		topGap = screenHight / 14;
		
		blockSize = screenWidth /numBlockWide;
		numBlockHight = (screenHight - topGap) / blockSize;
		headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head);
		midBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid);
		tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail);
		appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);
		
		headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
		midBitmap = Bitmap.createScaledBitmap(midBitmap, blockSize, blockSize, false);
		tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
		appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);
		
		Matrix m = new Matrix();
        m.setRotate(90, (float)blockSize / 2, (float) blockSize / 2);
        
        try {
        	headBitmapV = Bitmap.createBitmap(headBitmap, 0, 0, blockSize, blockSize, m, true);
        	midBitmapV = Bitmap.createBitmap(midBitmap, 0, 0, blockSize, blockSize, m, true);
        	tailBitmapV = Bitmap.createBitmap(tailBitmap, 0, 0, blockSize, blockSize, m, true);
        	
            m.setRotate(180, (float)blockSize / 2, (float) blockSize / 2);
        	headBitmapHR = Bitmap.createBitmap(headBitmap, 0, 0, blockSize, blockSize, m, true);
            m.setRotate(270, (float)blockSize / 2, (float) blockSize / 2);
        	headBitmapVR = Bitmap.createBitmap(headBitmap, 0, 0, blockSize, blockSize, m, true);
        	
		} catch (OutOfMemoryError ex) {
		}
	}
	
	public void loadSound(){
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		AssetManager assetManager = getAssets();
		AssetFileDescriptor descriptor;
		try {
			descriptor = assetManager.openFd("sample1.ogg");
			soundID1 = soundPool.load(descriptor, 0);
			
			descriptor = assetManager.openFd("sample2.ogg");
			soundID2 = soundPool.load(descriptor, 0);
			
			descriptor = assetManager.openFd("sample3.ogg");
			soundID3 = soundPool.load(descriptor, 0);
			
			descriptor = assetManager.openFd("sample4.ogg");
			soundID4 = soundPool.load(descriptor, 0);
			
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Error", "failed to load sound asset");
		}
		
	}
	
	@Override
	public void onStop(){
		super.onStop();
		while(true){
			snakeView.pause();
			break;
		}
		finish();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		snakeView.resume();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		snakeView.pause();
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			snakeView.pause();
			finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
