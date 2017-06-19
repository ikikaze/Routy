package ro.upt.edu.mapzenver;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapzen.pelias.widget.AutoCompleteAdapter;
import com.mapzen.pelias.widget.AutoCompleteItem;

/**
 * Created by Ikikaze on 09-Jun-17.
 */

public class SearchListAdapter extends AutoCompleteAdapter {
    /**
     * Constructs a new adapter given a context and layout id.
     *
     * @param context
     * @param resource
     */

    private Context context;


    public SearchListAdapter(Context context, int resource) {
        super(context, resource);
        this.context=context;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null) {
            convertView = ((Activity)context).getLayoutInflater().inflate(R.layout.list_item_double,null,false);
        }

        AutoCompleteItem item= getItem(position);
        String itemText = item.getText();
        String[] splitted = itemText.split(",",2);
        final TextView bigText = (TextView) convertView.findViewById(R.id.big_text);
        final TextView smallText = (TextView) convertView.findViewById(R.id.small_text);
        bigText.setText(splitted[0]);
        if(splitted.length>1)
            smallText.setText(splitted[1]);
        else
            smallText.setText("");

        return convertView;
    }
}
