
package master.flame.danmaku.danmaku.model.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.Log;

import master.flame.danmaku.danmaku.model.ICanvas;
import tv.cjump.gl.glrenderer.BitmapTexture;
import tv.cjump.gl.glrenderer.GLCanvas;
import tv.cjump.gl.glrenderer.GLES20Canvas;
import tv.cjump.gl.glrenderer.GLPaint;
import tv.cjump.jni.NativeBitmapFactory;

import javax.microedition.khronos.opengles.GL11;

public class GLESCanvas implements ICanvas<GL11> {

    @SuppressWarnings("unused")
    private GL11 mGL11;
    private GLCanvas mGLCanvas;
    private int mGLVersion = 2;
    private BitmapTexture mBitmapTexture;
    private float[] argb = new float[4];
    private BitmapTexture mStringTexture;
    private int mWidth;
    private int mHeight;
    private GLPaint mGLPaint;
    private BitmapHolder mStringBitmapHolder = new BitmapHolder();
    private Rect mStringBounds = new Rect();
    private Canvas mStringCanvas = new Canvas();

    public GLESCanvas(int version, GL11 gl, int width, int height) {
        mGLVersion = version;
        mGLCanvas = new GLES20Canvas();
        mGL11 = gl;
        mWidth = width;
        mHeight = height;
        mGLCanvas.setSize(mWidth, mHeight);
    }

    @Override
    public void attach(GL11 data) {
        mGL11 = data;
        if (mGLVersion == 1) {
            mGLCanvas = null; //new GLES11Canvas(data);
        }
    }

    @Override
    public int save() {
        mGLCanvas.save();
        return 0;
    }

    @Override
    public void restore() {
        mGLCanvas.restore();
    }

    @Override
    public void concat(float[] matrix) {
        mGLCanvas.multiplyMatrix(matrix, 0);
    }

    @Override
    public void setBitmap(master.flame.danmaku.danmaku.model.ICanvas.IBitmapHolder<?> bitmap) {
        mBitmapTexture = new BitmapTexture((Bitmap) bitmap.data());
        mBitmapTexture.setOpaque(false);
        mWidth = mBitmapTexture.getWidth();
        mHeight = mBitmapTexture.getHeight();
        mGLCanvas.setSize(mWidth, mHeight);
        mBitmapTexture.draw(mGLCanvas, 0, 0);
    }

    @Override
    public void drawColor(int color, master.flame.danmaku.danmaku.model.ICanvas.IMode<?> mode) {
        int a = (color >>> 24) & 0xff;
        int r = (color >>> 16) & 0xff;
        int g = (color >>> 8) & 0xff;
        int b = color & 0xff;
        argb[0] = a / 255f;
        argb[1] = r / 255f;
        argb[2] = g / 255f;
        argb[3] = b / 255f;
        mGLCanvas.clearBuffer(argb);
    }

    private synchronized GLPaint convertToGLPaint(Paint paint) {
        if (mGLPaint == null) {
            mGLPaint = new GLPaint();
        }
        final GLPaint glpaint = mGLPaint;
        glpaint.setColor(paint.getColor());
        float strokeWidth = paint.getStrokeWidth();
        if (strokeWidth > 0) {
            glpaint.setLineWidth(strokeWidth);
        }
        return glpaint;
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY,
            master.flame.danmaku.danmaku.model.ICanvas.IPaint<?> paint) {

        mGLCanvas.drawLine(startX, startY, stopX, stopY, convertToGLPaint((Paint) paint.data()));
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom,
            master.flame.danmaku.danmaku.model.ICanvas.IPaint<?> ipaint) {
        Paint paint = (Paint) ipaint.data();
        if (Style.STROKE.equals(paint.getStyle())) {
            mGLCanvas.drawRect(left, top, right, bottom, convertToGLPaint(paint));
        } else {
            mGLCanvas.fillRect(left, top, right, bottom, paint.getColor());
        }
    }

    @Override
    public synchronized void drawText(String text, float left, float top,
            master.flame.danmaku.danmaku.model.ICanvas.IPaint<?> paint) {
        if (paint == null) {
            return;
        }
        if (mStringTexture != null) {
            mStringTexture.recycle();
        }
        Paint tpaint = (Paint) paint.data();
        tpaint.getTextBounds(text, 0, text.length(), mStringBounds);
        Bitmap stringBitmap = NativeBitmapFactory.createBitmap((int) tpaint.measureText(text),
                (int) (mStringBounds.height() + tpaint.descent() - tpaint.ascent()),
                Bitmap.Config.ARGB_8888);
        mStringCanvas.setBitmap(stringBitmap);
        mStringCanvas.drawText(text, 0, -tpaint.ascent(), tpaint);
        mStringBitmapHolder.attach(stringBitmap);
        drawBitmap(mStringBitmapHolder, left, top + tpaint.ascent(), paint);
        stringBitmap.recycle();
    }

    @Override
    public synchronized void drawBitmap(
            master.flame.danmaku.danmaku.model.ICanvas.IBitmapHolder<?> bitmap, float left,
            float top, master.flame.danmaku.danmaku.model.ICanvas.IPaint<?> paint) {
        if (mBitmapTexture == null) {
            mBitmapTexture = new BitmapTexture((Bitmap) bitmap.data());
        } else {
            mBitmapTexture.setBitmap((Bitmap) bitmap.data());
        }
        mBitmapTexture.setOpaque(false);
        mBitmapTexture.draw(mGLCanvas, (int) left, (int) top);
        mGLCanvas.deleteRecycledResources();
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public boolean isHardwareAccelerated() {
        return true;
    }

    @Override
    public void setDensity(int density) {
        if (mBitmapTexture != null) {
            Bitmap bitmap = mBitmapTexture.getBitmap();
            if (bitmap != null) {
                bitmap.setDensity(density);
            }
        }
    }

    @Override
    public void clear() {
        mGLCanvas.clearBuffer();
    }

    @Override
    public void clear(float left, float top, float right, float bottom) {
        //mGLCanvas.fillRect(left, top, right, bottom, 0x00ffff00);
        clear();  //TODO: fix this
    }

}