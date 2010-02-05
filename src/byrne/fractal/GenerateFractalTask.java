/*
This file is part of Fractoid
Copyright (C) 2010 David Byrne
david.r.byrne@gmail.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package byrne.fractal;

import android.os.AsyncTask;
import android.graphics.*;

public class GenerateFractalTask extends AsyncTask<Void, Bitmap, Bitmap> {
  
  FractalParameters params;
  FractalView fractalView;
  long startTime;
  NativeLib mNativeLib;
  
  public GenerateFractalTask(FractalParameters p, FractalView fv) {
    params = p;
    fractalView = fv;
    mNativeLib = new NativeLib();
  }
  
  private int[] calculateColors() {
    
    double red, green, blue;    
    final int numberOfColors = params.getMaxIterations()*10;
    int[] colorIntegers = new int[numberOfColors];   
    double shiftFactor = params.getShiftFactor();
    
    switch (params.getColorSet()) {
      case RAINBOW:
        for (int x = 0; x < numberOfColors; x++) {
          double data = x;
          data = 2*Math.PI*(data/500);
          red = Math.sin(data + Math.PI*shiftFactor);
          green = Math.cos(data + Math.PI*shiftFactor);
          blue = -((red + green)*.707);
          red = (red*127.0) + 127.0;
          green = (green*127.0) + 127.0;
          blue = (blue*127.0) + 127.0;
          colorIntegers[x] = Color.rgb((int)red, (int)green, (int)blue);
        }    
        break;
      
      case WINTER:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(255-color,255-color,255-color/3);
        }
        break;
      
      case SUMMER:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(255-color/3,255-color,128-color/2);
        }
        break;
      
      case NIGHT_SKY:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(color/2,color,127+color/2);
        }
        break;
      
      case ORANGE:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(color,color/2,0);
        }
        break;
      
      case RED:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(color,0,0);
        }
        break;
      
      case GREEN:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(0,color,0);
        }
        break;
      
      case YELLOW:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color;
          if (value <= 255)
            color = Math.abs(value);
          else color = Math.abs(255-(value-255));
          colorIntegers[x] = Color.rgb(color,color,0);
        }
        break;
        
      case BLACK_AND_WHITE:
        for (int x = 0; x < numberOfColors; x++) {
          int value = x%510;
          int color = Math.abs(255-value);
          colorIntegers[x] = Color.rgb(color,color,color);
        }     
    }
    return colorIntegers;   
  }
  
  private Bitmap createBitmap() {
    
    
    
    ComplexEquation equation = params.getEquation();
    int power = equation.getPower();
    
    double realmin = params.getRealMin();
    double realmax = params.getRealMax();
    double imagmin = params.getImagMin();
    double imagmax = params.getImagMax();
    
    double P = params.getP();
    double Q = params.getQ();
    
    double xtmp = 0;
    
    int xres = params.getXRes();
    int yres = params.getYRes();
    FractalType type = params.getType();
    
    Bitmap b = Bitmap.createBitmap(xres, yres, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    
    Paint paint = new Paint();
    paint.setStyle(Paint.Style.FILL_AND_STROKE);
    paint.setStrokeWidth(1);
    
    int[] colorIntegers = calculateColors();
    int[] rowColors;

    double x=-1, y=-1, prev_x = -1, prev_y =-1,tmp_prev_x,tmp_prev_y, mu = 1;
    int index;
    boolean lessThanMax;

    double deltaP = (realmax - realmin)/xres;
    double deltaQ = (imagmax - imagmin)/yres;
    
    final int max = params.getMaxIterations();
    final int PASSES = 5;
    int updateCount=0;
    
    for (int rpass = 0; rpass < PASSES; rpass++) {
      for (int row=rpass; row < yres; row += PASSES) {
        
        updateCount++;
        if (updateCount % 8 == 0) {
          if (isCancelled())
            return b;
          this.publishProgress(b);
        }        
        rowColors = mNativeLib.getFractalRow(row,xres,yres,
                                             power,max,equation.getInt(),type.getInt(),P,Q,
                                             realmin,realmax,imagmin,imagmax);  
        for (int col=0; col < xres; col++) {
          
          if (rowColors[col] >= 0) {
            paint.setColor(colorIntegers[rowColors[col]]);
          } else {
            paint.setColor(Color.BLACK);
          }
          //TODO Store results so color changes don't require recalculation
          c.drawPoint(col,row,paint);
        }
      }
    }
    return b;
  }
  
  @Override protected void onPreExecute() {
    startTime = System.currentTimeMillis();
  }
  @Override protected Bitmap doInBackground(Void... unused) {   
    return createBitmap();
  }
  @Override protected void onProgressUpdate(Bitmap... b) {
    fractalView.setFractal(b[0]);
    fractalView.invalidate();
  }
  @Override protected void onPostExecute(Bitmap bitmap) {
    fractalView.setFractal(bitmap);
    fractalView.setTime(System.currentTimeMillis()-startTime);
    fractalView.invalidate();
  }  
}
class NativeLib {
  public native int[] getFractalRow(int row,
                                    int xres,
                                    int yres,
                                    int power,
                                    int max,
                                    int equation,
                                    int type,
                                    double P,
                                    double Q,
                                    double realmin,
                                    double realmax,
                                    double imagmin,
                                    double imagmax);
  static {
    System.loadLibrary("FractalMath");
  }
}
