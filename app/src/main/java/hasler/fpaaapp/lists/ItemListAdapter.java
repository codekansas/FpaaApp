package hasler.fpaaapp.lists;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import hasler.fpaaapp.R;

public class ItemListAdapter extends ArrayAdapter {
    private Context context;

    public ItemListAdapter(Context context, List items) {
        super(context, android.R.layout.simple_list_item_1, items);
        this.context = context;
    }

    private class ViewHolder {
        TextView titleText;
        TextView subtitleText;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ItemListItem item = (ItemListItem) getItem(position);
        ViewHolder holder;
        View viewToUse;

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            viewToUse = mInflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();
            holder.titleText = (TextView) viewToUse.findViewById(R.id.title_text_view);
            holder.subtitleText = (TextView) viewToUse.findViewById(R.id.subtitle_text_view);
            viewToUse.setTag(holder);
        } else {
            viewToUse = convertView;
            holder = (ViewHolder) viewToUse.getTag();
        }

        holder.titleText.setText(item.getTitle());
        holder.subtitleText.setText(item.getValue());
        return viewToUse;
    }
}
