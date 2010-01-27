package byrne.fractal;

import android.view.View;
import android.graphics.*;
import android.view.MotionEvent;
import android.content.Context;
import android.os.AsyncTask.Status;
import android.util.AttributeSet; 
import java.util.Map; 

public class FractalView extends View {
    
  private double minY,maxY,minX,maxX;
  private Bitmap fractalBitmap;
  private RectF selection;
  private boolean zoom;
  private double touched_x=-1, touched_y=-1;
  private GenerateFractalTask mGenerateFractalTask;
  private String calculationTime;
  private ComplexEquation equation = ComplexEquation.SECOND_ORDER;
  
  private FractalParameters params;
  
  public FractalView(Context context){
    super(context);
    zoom = true;
    params = new FractalParameters();
  }
  
  public FractalView(Context context, AttributeSet attrs){
        super(context, attrs);
        zoom = true;
        params = new FractalParameters();
  } 
  
  public void setZoom(boolean z) {
    zoom = z;
  }
  
  public void setColorSet(ColorSet cs) {
    params.setColorSet(cs);
    startFractalTask();
  }
  
  public ComplexEquation getEquation() {
    return equation;
  }
  
  public void setEquation(ComplexEquation e) {
    equation = e;
  }
  
  public int getMaxIterations() {
    return params.getMaxIterations();
  }
  
  public void setMaxIterations(int i) {
    params.setMaxIterations(i);
    startFractalTask();
  }
  
  public void zoomOut() {
    double imagmin = params.getImagMin();
    double imagmax = params.getImagMax();
    double realmin = params.getRealMin();
    double realmax = params.getRealMax();
    double realRange = Math.abs(realmax-realmin);
    double imagRange = Math.abs(imagmax-imagmin);
    
    imagmin = imagmin - (imagRange/2);
    imagmax = imagmax + (imagRange/2);
    realmin = realmin - (realRange/2);
    realmax = realmax + (realRange/2);
    
    params.setCoords(realmin,realmax,imagmin,imagmax);
    startFractalTask();
  }
  
  public FractalType getType() {
    return params.getType();
  }
  
  public void setType(FractalType t) {
    params.setType(t);
  }
  public Bitmap getFractal() {
    return fractalBitmap;
  }
  
  public void createPhoenixSet() {
    double imagmax = 1.4;
    double imagmin = -1.4;

    double r_y = Math.abs(imagmax - imagmin);
    double realmax = params.getResRatio()*r_y/2;
    double realmin = params.getResRatio()*r_y/2*-1;
    
    params.randomizeShiftFactor();
    setEquation(ComplexEquation.PHOENIX);
    setType(FractalType.JULIA);
    
    params.setEquation(ComplexEquation.PHOENIX);
    params.setType(FractalType.JULIA);
    params.setCoords(realmin,realmax,imagmin,imagmax);
    params.setP(0.56667);
    params.setQ(-0.5);
    params.setMaxIterations(100);
  
    startFractalTask();
    setZoom(true);
  }

  public void startFractalTask() {
    calculationTime = null;
    if (mGenerateFractalTask != null && mGenerateFractalTask.getStatus() == Status.RUNNING) {
      mGenerateFractalTask.cancel(true);
    }
    mGenerateFractalTask = new GenerateFractalTask(params,this);
    mGenerateFractalTask.execute();
  }

  public void setFractal(Bitmap fa) {
    fractalBitmap = fa;
  }
  
  public void setTime(long t) {
    long time = t / 1000;  
    String seconds = Integer.toString((int)(time % 60));  
    String minutes = Integer.toString((int)(time / 60));  
    for (int i = 0; i < 2; i++) {  
      if (seconds.length() < 2) {  
        seconds = "0" + seconds;  
      }  
      if (minutes.length() < 2) {  
        minutes = "0" + minutes;  
      }
    }
    calculationTime = "Time: "+minutes+":"+seconds;
  }

  protected void resetCoords() {
    double imagmax = 1.4;
    double imagmin = -1.4;

    double r_y = Math.abs(imagmax - imagmin);
    double realmax = params.getResRatio()*r_y/2;
    double realmin = params.getResRatio()*r_y/2*-1;
    
    params.randomizeShiftFactor();
    params.setEquation(equation);
    params.setCoords(realmin,realmax,imagmin,imagmax);
    if (equation == ComplexEquation.PHOENIX)
      params.setType(FractalType.JULIA);
    else
      params.setType(FractalType.MANDELBROT);
    params.resetMaxIterations();
    
    startFractalTask();
  }
      
  @Override protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    params.setXRes(width);
    params.setYRes(height);
  }
  
  @Override public boolean onTouchEvent (MotionEvent event) {

    double realmax = params.getRealMax();
    double realmin = params.getRealMin();
    double imagmin = params.getImagMin();
    double imagmax = params.getImagMax();

    if (zoom) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        touched_x = event.getX();
        touched_y = event.getY();
      } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
        if (event.getY() > touched_y) {
          maxY=event.getY();
          minY=(double)touched_y;
        } else {
          maxY=(double)touched_y;
          minY=Math.max(event.getY(),0);
        }
        if (event.getX() > touched_x) {
          maxX=event.getX();
          minX=(double)touched_x;
        } else {
          maxX=(double)touched_x;
          minX=event.getX();
        }
        double x_range = Math.abs(maxX-minX);
        double y_range = Math.abs(maxY-minY);
        double inv_ratio = (double)params.getYRes()/params.getXRes();
        double sel_ratio = x_range/y_range;

        if (params.getResRatio() > sel_ratio) {
          if (maxX == event.getX()) {
            maxX = minX+(params.getResRatio()*y_range);
          } else {
            minX = maxX-(params.getResRatio()*y_range);
          }
        } else {
          if (maxY == event.getY()) {
            maxY = minY+(inv_ratio*x_range);
          } else {
            minY = maxY-(inv_ratio*x_range);
          }
        }

        selection = new RectF(Math.max((float)minX,0),Math.min((float)maxY,params.getYRes()),Math.min((float)maxX,params.getXRes()),Math.max((float)minY,0));
        postInvalidate();
        
      } else if (event.getAction() == MotionEvent.ACTION_UP) {

        double x_range = (double)Math.abs(realmax-realmin);
        double y_range = (double)Math.abs(imagmax-imagmin);  

        realmax = realmin + (maxX/params.getXRes()) * x_range;		
        realmin = realmin + (minX/params.getXRes()) * x_range;
        imagmin = imagmax - (maxY/params.getYRes()) * y_range;
        imagmax = imagmax - (minY/params.getYRes()) * y_range;
        selection = null;
        
        params.setCoords(realmin,realmax,imagmin,imagmax);
        params.setMaxIterations(params.getMaxIterations() + 15);
        startFractalTask();
      }
    } else if (!zoom) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        
        double x_range = (double)Math.abs(realmax-realmin);
        double y_range = (double)Math.abs(imagmax-imagmin);
        touched_x = event.getX();
        touched_y = event.getY();
        params.setP(realmin + ((touched_x/params.getXRes())*x_range));
        params.setQ(imagmax - ((touched_y/params.getYRes())*y_range));

        imagmax = 1.4;
        imagmin = -1.4;
        y_range = (double)Math.abs(imagmax-imagmin);
        realmax = (params.getResRatio())*y_range/2;
        realmin = (params.getResRatio())*y_range/2*-1;
        
        params.setCoords(realmin,realmax,imagmin,imagmax);
        params.resetMaxIterations();
        startFractalTask();
      } else if (event.getAction() == MotionEvent.ACTION_UP) {
        setZoom(true);
      }
    } 
    return true;
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (fractalBitmap != null) {
      canvas.drawBitmap(fractalBitmap,0,0,null);

      Paint p = new Paint();
      if (params.getColorSet() == ColorSet.BLACK_AND_WHITE) {
        p.setColor(Color.BLACK);  
      } else {
        p.setColor(Color.WHITE);
      }
      p.setTextSize(25);

      if (selection != null) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        canvas.drawRect(selection,p);
      }
      p.setStyle(Paint.Style.FILL_AND_STROKE);
      p.setStrokeWidth(1);
      if (zoom) {
        canvas.drawText("Drag to zoom",(params.getXRes()/2)-60,params.getYRes()-5,p);
      }
      else {
        canvas.drawText("Touch Screen to Generate Julia Set",(params.getXRes()/2)-175,params.getYRes()-5,p);
      }
      String maxIterString = "MaxIter: " + params.getMaxIterations();
      canvas.drawText(maxIterString,5,params.getYRes()-5,p);
      
      if (calculationTime != null) {
        canvas.drawText(calculationTime,params.getXRes()-140,params.getYRes()-5,p);
      }
      
    } else {
      resetCoords();
    }
  }
}