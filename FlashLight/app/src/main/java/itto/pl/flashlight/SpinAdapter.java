package itto.pl.flashlight;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by PL_itto on 2/24/2017.
 */
public class SpinAdapter extends BaseAdapter implements SpinnerAdapter {
    Context context;
    List<String> arr;

    public SpinAdapter(Context context, List<String> arr) {
        this.context = context;
        this.arr = arr;
    }

    @Override
    public int getCount() {
        return arr.size();
    }

    @Override
    public Object getItem(int position) {
        return arr.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ItemHolder holder;
        if (convertView == null) {
            holder = new ItemHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.drop_down_item, null);
            holder.item_text = (TextView) convertView.findViewById(R.id.txt_item);
            convertView.setTag(holder);

        } else {
            holder = (ItemHolder) convertView.getTag();
        }
        holder.item_text.setText(arr.get(position));
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_spinner, null);
            holder = new Holder();
            holder.txt = (TextView) convertView.findViewById(R.id.txt_name);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        holder.txt.setText(arr.get(position));
        return convertView;
    }

    class Holder {
        TextView txt;
    }

    class ItemHolder {
        TextView item_text;
    }

}
