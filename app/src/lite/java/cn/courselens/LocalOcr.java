package cn.courselens;

import android.content.Context;
import android.net.Uri;

final class LocalOcr {
 static final boolean AVAILABLE=false;
 static void read(Context context,Uri uri,ParcelOcr.Callback callback){callback.result(null,"轻量版请先安装图片识别插件");}
}
