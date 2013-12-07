package com.axway.ate.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.SparseArray;

import com.axway.ate.R;
import com.vordel.api.topology.model.Topology.EntityType;

public class BitmapFactory {

	private static final boolean DRAW_RECT = true;

	private static final int N_DIMENS = 2;
	private static final int DIMEN_IMGSIZE = 0;
	private static final int DIMEN_IMGFONTSIZE = DIMEN_IMGSIZE + 1;
	
	private static BitmapFactory instance = null;
//	private Map<Integer, Bitmap> bitmaps;
	private SparseArray<Bitmap> bitmaps;
	private int[] colors;
	private float[] dimens;
	private float scale;
	
	public BitmapFactory() {
		super();
		bitmaps = new SparseArray<Bitmap>();	//new HashMap<Integer, Bitmap>();
		colors = null;
		dimens = null;
	}
	
	public static BitmapFactory getInstance(Context ctx) {
		if (instance == null) {
			instance = new BitmapFactory();
			Resources res = ctx.getResources();
			instance.loadColors(res);
			instance.loadDimens(res);
		}
		return instance;
	}
	
	public Bitmap get(EntityType typ) {
		Bitmap rv = bitmaps.get(typ.ordinal());
		if (rv == null) {
			rv = drawBitmapForState(typ);
			bitmaps.put(typ.ordinal(), rv);
		}
		return rv;
	}

	private Bitmap drawBitmapForState(EntityType typ) {
		Bitmap rv = null;
		String name = null;
		if (typ == EntityType.Gateway)
			name = "Instance";
		else
			name = typ.name();
		Paint p = new Paint();
		p.setStrokeWidth(4.0f);
		Bitmap.Config config = android.graphics.Bitmap.Config.ARGB_8888;
		int h = (int)(dimens[DIMEN_IMGSIZE]);	//*scale
		int w = h;
		rv = Bitmap.createBitmap(h, w, config);
		Canvas canvas = new Canvas(rv);
		if (DRAW_RECT) {
			p.setColor(Color.BLACK);
			p.setStyle(Paint.Style.FILL);
			canvas.drawRect(0, 0, w, h, p);	//rv.getWidth(), rv.getHeight(), p);
			p.setColor(typeColor(typ));
			p.setStyle(Paint.Style.FILL);
			canvas.drawRect(4, 4, w-4, h-4, p);	//rv.getWidth(), rv.getHeight(), p);
//			canvas.drawLine(0, 0, w-1, 0, p);
//			canvas.drawLine(w-1, 0, w-1, h-1, p);
//			canvas.drawLine(w-1, h-1, 0, h-1, p);
//			canvas.drawLine(0, h-1, 0, 0, p);
		}
		else {
			float pad = (rv.getWidth()*0.01f);
			float cx = (rv.getWidth()/2) - (pad);
			float cy = (rv.getHeight()/2) - (pad) + 2;
			float r = (rv.getWidth()/2) - pad;
			canvas.drawCircle(cx, cy, r, p);
		}
		p.setColor(Color.WHITE);
		p.setTextSize(dimens[DIMEN_IMGFONTSIZE]);	// * scale);
		String text = name.substring(0,1);
		Rect bounds = new Rect();
		p.getTextBounds(text, 0, text.length(), bounds);
		p.setShadowLayer(1f, 0f, 1f, Color.BLACK);		
		int x = (rv.getWidth() - bounds.width())/2;
		int y = (rv.getHeight() + bounds.height())/2;
		canvas.drawText(text, x, y, p);
		return rv;
	}

	private int typeColor(EntityType typ) {
		int rv = -1;
		if (colors == null)
			return rv;
		int ndx = -1;
		switch (typ) {
			case Host:
				ndx = 0;
			break;
			case Group:
				ndx = 1;
			break;
			case NodeManager:
			case Gateway:
				ndx = 2;
			break;
		}
		if (ndx >= 0 && ndx < colors.length)
			rv = colors[ndx];
		return rv;
	}
	
	private void loadColors(Resources res) {
		if (colors == null) {
			colors = new int[4];
			scale = res.getDisplayMetrics().density;
			colors[0] = res.getColor(R.color.host);
			colors[1] = res.getColor(R.color.group);
			colors[2] = res.getColor(R.color.service);
			colors[3] = res.getColor(R.color.disabled);
		}
	}
	
	private void loadDimens(Resources res) {
		if (dimens == null) {
			dimens = new float[N_DIMENS];
			dimens[DIMEN_IMGSIZE] = res.getDimension(R.dimen.listitem_image_size);
			dimens[DIMEN_IMGFONTSIZE] = res.getDimension(R.dimen.listitem_image_fontsize);
		}
	}
}
