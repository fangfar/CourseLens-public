package cn.courselens;

import android.content.Context;
import android.net.Uri;
import cn.courselens.ocr.OcrEngine;

final class LocalOcr {
 static final boolean AVAILABLE=true;
 static void read(Context context,Uri uri,ParcelOcr.Callback callback){OcrEngine.read(context,uri,callback::result);}
}
