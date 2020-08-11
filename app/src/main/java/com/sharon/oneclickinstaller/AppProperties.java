package com.sharon.oneclickinstaller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.commons.utils.FastAdapterUIUtils;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;


public class AppProperties extends AbstractItem<AppProperties, AppProperties.ViewHolder> {

    private String appname, pname, versionname, apkpath, apksize;
    private int versionCode;
    int apkbytesize;
    private boolean already_installedorbackedup = false, isInstaller = false, updated = false;
    private Drawable icon;
    private Context context;

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getVersionname() {
        return versionname;
    }

    public void setVersionname(String versionname) {
        this.versionname = versionname;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getApkpath() {
        return apkpath;
    }

    public void setApkpath(String apkpath) {
        this.apkpath = apkpath;
    }

    public String getApksize() {
        return apksize;
    }

    public void setApksize(String apksize) {
        this.apksize = apksize;
    }

    public int getApkbytesize() {
        return apkbytesize;
    }

    public void setApkbytesize(int apkbytesize) {
        this.apkbytesize = apkbytesize;
    }

    public boolean isAlready_installedorbackedup() {
        return already_installedorbackedup;
    }

    public void setAlready_installedorbackedup(boolean already_installedorbackedup) {
        this.already_installedorbackedup = already_installedorbackedup;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isInstaller() {
        return isInstaller;
    }

    public void setInstaller(boolean installer) {
        isInstaller = installer;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item_adapter;
    }

    @Override
    public void bindView(@NonNull ViewHolder holder, @NonNull List<Object> payloads) {
        super.bindView(holder, payloads);
        context = holder.itemView.getContext();
        holder.appname.setText(getAppname());
        holder.packagename.setText(getPname());
        holder.version.setText("Ver: " + getVersionname());
        if (isInstaller()) {
            if (isAlready_installedorbackedup()) {
                holder.installedbackedup.setText("installed");
                holder.installedbackedup.setTextColor(Color.parseColor("#FF689F38"));
            } else {
                holder.installedbackedup.setText("not installed");
                holder.installedbackedup.setTextColor(Color.RED);
            }
        } else {
            if (isAlready_installedorbackedup()) {
                if (isUpdated()) {
                    holder.installedbackedup.setText("updated");
                    holder.installedbackedup.setTextColor(Color.BLUE);
                } else {
                    holder.installedbackedup.setText("backed up");
                    holder.installedbackedup.setTextColor(Color.parseColor("#FF689F38"));
                }
            } else {
                holder.installedbackedup.setText("no backup");
                holder.installedbackedup.setTextColor(Color.RED);
            }
        }
        holder.icon.setImageDrawable(getIcon());
        holder.constraintLayout.setBackground(FastAdapterUIUtils.getSelectableBackground(context, context.getResources().getColor(R.color.colorAccent), false));
    }

    @Override
    public void unbindView(@NonNull ViewHolder holder) {
        super.unbindView(holder);
        holder.icon.setImageDrawable(null);
        holder.appname.setText(null);
        holder.packagename.setText(null);
        holder.version.setText(null);
        holder.itemView.setBackground(null);
        holder.constraintLayout.setBackground(null);
        holder.installedbackedup.setText(null);
        holder.constraintLayout.setConstraintSet(null);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.row_view_list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView appname, packagename, version, installedbackedup;
        ImageView icon;
        ConstraintLayout constraintLayout;

        ViewHolder(View view) {
            super(view);
            appname = view.findViewById(R.id.app_name);
            packagename = view.findViewById(R.id.app_package_name);
            version = view.findViewById(R.id.app_version);
            icon = view.findViewById(R.id.app_icon);
            installedbackedup = view.findViewById(R.id.installedbackedup);
            constraintLayout = view.findViewById(R.id.mainConstraintLayout);
        }
    }

}
