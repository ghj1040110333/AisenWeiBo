package org.aisen.weibo.sina.support.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.m.common.context.GlobalContext;
import com.m.common.setting.SettingUtility;
import com.m.common.utils.DateUtils;
import com.m.common.utils.FileUtils;
import com.m.common.utils.Logger;
import com.m.common.utils.SystemUtils;
import com.m.common.utils.Utils;
import com.m.component.bitmaploader.core.BitmapDecoder;
import com.m.ui.fragment.ABaseFragment;

import org.aisen.weibo.sina.R;
import org.aisen.weibo.sina.base.AppSettings;
import org.sina.android.bean.StatusContent;
import org.sina.android.bean.WeiBoUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by wangdan on 15/4/12.
 */
public class AisenUtils {

    public static int getThemeColor(Context context) {
        final int materialBlue = Color.parseColor("#ff0000");
        int themeColor = Utils.resolveColor(context, R.attr.theme_color, materialBlue);
        return themeColor;
    }

    public static String getUserScreenName(WeiBoUser user) {
        if (AppSettings.isShowRemark() && !TextUtils.isEmpty(user.getRemark()))
            return user.getRemark();

        return user.getScreen_name();
    }

    public static String getUserKey(String key, WeiBoUser user) {
        return key + "-" + user.getIdstr();
    }

    public static File getUploadFile(File source) {
        if (source.getName().toLowerCase().endsWith(".gif")) {
            Logger.w("上传图片是GIF图片，上传原图");
            return source;
        }

        File file = null;

        String imagePath = GlobalContext.getInstance().getAppPath() + SettingUtility.getStringSetting("draft") + File.separator;

        int sample = 1;
        int maxSize = 0;

        int type = AppSettings.getUploadSetting();
        // 自动，WIFI时原图，移动网络时高
        if (type == 0) {
            if (SystemUtils.getNetworkType() == SystemUtils.NetWorkType.wifi)
                type = 1;
            else
                type = 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), opts);
        switch (type) {
            // 原图
            case 1:
                Logger.w("原图上传");
                file = source;
                break;
            // 高
            case 2:
                sample = BitmapDecoder.calculateInSampleSize(opts, 1920, 1080);
                Logger.w("高质量上传");
                maxSize = 700 * 1024;
                imagePath = imagePath + "高" + File.separator + source.getName();
                file = new File(imagePath);
                break;
            // 中
            case 3:
                Logger.w("中质量上传");
                sample = BitmapDecoder.calculateInSampleSize(opts, 1280, 720);
                maxSize = 300 * 1024;
                imagePath = imagePath + "中" + File.separator + source.getName();
                file = new File(imagePath);
                break;
            // 低
            case 4:
                Logger.w("低质量上传");
                sample = BitmapDecoder.calculateInSampleSize(opts, 1280, 720);
                maxSize = 100 * 1024;
                imagePath = imagePath + "低" + File.separator + source.getName();
                file = new File(imagePath);
                break;
            default:
                break;
        }

        // 压缩图片
        if (type != 1 && !file.exists()) {
            Logger.w(String.format("压缩图片，原图片 path = %s", source.getAbsolutePath()));
            byte[] imageBytes = FileUtils.readFileToBytes(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                out.write(imageBytes);
            } catch (Exception e) {
            }

            Logger.w(String.format("原图片大小%sK", String.valueOf(imageBytes.length / 1024)));
            if (imageBytes.length > maxSize) {
                // 尺寸做压缩
                BitmapFactory.Options options = new BitmapFactory.Options();

                if (sample > 1) {
                    options.inSampleSize = sample;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                    Logger.w(String.format("压缩图片至大小：%d*%d", bitmap.getWidth(), bitmap.getHeight()));
                    out.reset();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    imageBytes = out.toByteArray();
                }

                options.inSampleSize = 1;
                if (imageBytes.length > maxSize) {
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

                    int quality = 90;
                    out.reset();
                    Logger.w(String.format("压缩图片至原来的百分之%d大小", quality));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    while (out.toByteArray().length > maxSize) {
                        out.reset();
                        quality -= 10;
                        Logger.w(String.format("压缩图片至原来的百分之%d大小", quality));
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    }
                }

            }

            try {
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();

                Logger.w(String.format("最终图片大小%sK", String.valueOf(out.toByteArray().length / 1024)));
                FileOutputStream fo = new FileOutputStream(file);
                fo.write(out.toByteArray());
                fo.flush();
                fo.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public static void showMenuDialog(ABaseFragment fragment, final View targetView,
                                      String[] menuArr, DialogInterface.OnClickListener onItemClickListener) {
        new AlertDialogWrapper.Builder(fragment.getActivity())
                .setItems(menuArr, onItemClickListener)
                .show();
    }

    public static String getFirstId(@SuppressWarnings("rawtypes") List datas) {
        int size = datas.size();
        if (size > 0)
            return getId(datas.get(0));
        return null;
    }

    public static String getLastId(@SuppressWarnings("rawtypes") List datas) {
        int size = datas.size();
        if (size > 0)
            return getId(datas.get(size - 1));
        return null;
    }

    public static String getId(Object t) {
        try {
            Field idField = t.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return idField.get(t).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static String convDate(String time) {
        Context context = GlobalContext.getInstance();
        Resources res = context.getResources();

        StringBuffer buffer = new StringBuffer();

        Calendar createCal = Calendar.getInstance();
        createCal.setTimeInMillis(Date.parse(time));
        Calendar currentcal = Calendar.getInstance();
        currentcal.setTimeInMillis(System.currentTimeMillis());

        long diffTime = (currentcal.getTimeInMillis() - createCal.getTimeInMillis()) / 1000;

        // 同一月
        if (currentcal.get(Calendar.MONTH) == createCal.get(Calendar.MONTH)) {
            // 同一天
            if (currentcal.get(Calendar.DAY_OF_MONTH) == createCal.get(Calendar.DAY_OF_MONTH)) {
                if (diffTime < 3600 && diffTime >= 60) {
                    buffer.append((diffTime / 60) + res.getString(R.string.msg_few_minutes_ago));
                } else if (diffTime < 60) {
                    buffer.append(res.getString(R.string.msg_now));
                } else {
                    buffer.append(res.getString(R.string.msg_today)).append(" ").append(DateUtils.formatDate(createCal.getTimeInMillis(), "HH:mm"));
                }
            }
            // 前一天
            else if (currentcal.get(Calendar.DAY_OF_MONTH) - createCal.get(Calendar.DAY_OF_MONTH) == 1) {
                buffer.append(res.getString(R.string.msg_yesterday)).append(" ").append(DateUtils.formatDate(createCal.getTimeInMillis(), "HH:mm"));
            }
        }

        if (buffer.length() == 0) {
            buffer.append(DateUtils.formatDate(createCal.getTimeInMillis(), "MM-dd HH:mm"));
        }

        String timeStr = buffer.toString();
        if (currentcal.get(Calendar.YEAR) != createCal.get(Calendar.YEAR)) {
            timeStr = createCal.get(Calendar.YEAR) + " " + timeStr;
        }
        return timeStr;
    }


    public static String getGender(WeiBoUser user) {
        Resources res = GlobalContext.getInstance().getResources();
        if (user != null) {
            if ("m".equals(user.getGender())) {
                return res.getString(R.string.msg_male);
            } else if ("f".equals(user.getGender())) {
                return res.getString(R.string.msg_female);
            } else if ("n".equals(user.getGender())) {
                return res.getString(R.string.msg_gender_unknow);
            }
        }
        return "";
    }

    public static String convCount(int count) {
        if (count < 10000) {
            return count + "";
        } else {
            Resources res = GlobalContext.getInstance().getResources();
            String result = new DecimalFormat("#.0").format(count * 1.0f / 10000) + res.getString(R.string.msg_ten_thousand);
            return result;
        }
    }

    public static String getCounter(int count) {
        Resources res = GlobalContext.getInstance().getResources();

        if (count < 10000)
            return String.valueOf(count);
        else if (count < 100 * 10000)
            return new DecimalFormat("#.0" + res.getString(R.string.msg_ten_thousand)).format(count * 1.0f / 10000);
        else
            return new DecimalFormat("#" + res.getString(R.string.msg_ten_thousand)).format(count * 1.0f / 10000);
    }

    /**
     * 显示高清头像
     *
     * @param user
     * @return
     */
    public static String getUserPhoto(WeiBoUser user) {
        if (user == null)
            return "";

        if (AppSettings.isLargePhoto()) {
            return user.getAvatar_large();
        }

        return user.getProfile_image_url();
    }

    public static void setImageVerified(ImageView imgVerified, WeiBoUser user) {
        // 2014-08-27 新增判断，VerifiedType存在为null的情况
        if (user == null || user.getVerified_type() == null) {
            imgVerified.setVisibility(View.GONE);
            return;
        }

        // 黄V
        if (user.getVerified_type() == 0) {
            imgVerified.setImageResource(R.drawable.avatar_vip);
        }
        // 200:初级达人 220:高级达人
        else if (user.getVerified_type() == 200 || user.getVerified_type() == 220) {
            imgVerified.setImageResource(R.drawable.avatar_grassroot);
        }
        // 蓝V
        else if (user.getVerified_type() > 0) {
            imgVerified.setImageResource(R.drawable.avatar_enterprise_vip);
        }
        if (user.getVerified_type() >= 0)
            imgVerified.setVisibility(View.VISIBLE);
        else
            imgVerified.setVisibility(View.GONE);
    }

    public static void timelineMenuSelected(final ABaseFragment fragment, String selectedItem, final StatusContent status) {

    }

}