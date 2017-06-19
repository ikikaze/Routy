package ro.upt.edu.mapzenver;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mapzen.android.graphics.model.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlannerListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<Map.Entry<Marker, String>>> _listDataChild;
    private boolean listOrderChanged = false;

    public PlannerListAdapter(Context context, List<String> listDataHeader,
                              HashMap<String, List<Map.Entry<Marker, String>>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final Map.Entry<Marker, String> child = (Map.Entry<Marker, String>) getChild(groupPosition, childPosition);

        final String childText = (childPosition + 1) + ". " + (child.getValue());

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView tView = (TextView) convertView.findViewById(R.id.pointText);
        tView.setText(childText);

        ImageButton arrowUp = (ImageButton) convertView.findViewById(R.id.upArrow);
        if (childPosition == 0)
            arrowUp.setVisibility(View.INVISIBLE);
        else
            arrowUp.setVisibility(View.VISIBLE);


        arrowUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveItemUp(groupPosition, childPosition);

            }
        });

        ImageButton arrowDown = (ImageButton) convertView.findViewById(R.id.downArrow);

        if (childPosition == getChildrenCount(groupPosition) - 1)
            arrowDown.setVisibility(View.INVISIBLE);
        else
            arrowDown.setVisibility(View.VISIBLE);

        arrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveItemDown(groupPosition, childPosition);
            }
        });


        ImageButton switchButton = (ImageButton) convertView.findViewById(R.id.btnSwitch);
        if (groupPosition == 1) {
            switchButton.setImageResource(android.R.drawable.ic_input_add);
        } else
            switchButton.setImageResource(android.R.drawable.ic_delete);

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchGroup(groupPosition, childPosition);
            }
        });


        //txtListChild.setText(childText);
        return convertView;
    }

    private void switchGroup(int groupPosition, int childPosition) {
        Map.Entry<Marker, String> name = (Map.Entry<Marker, String>) getChild(groupPosition, childPosition);
        String groupName = (String) getGroup(groupPosition);
        List<Map.Entry<Marker, String>> children = _listDataChild.get(groupName);

        children.remove(name);

        _listDataChild.put(groupName, children);
        String otherGroup;
        if (groupPosition == 0)
            otherGroup = (String) getGroup(1);
        else
            otherGroup = (String) getGroup(0);

        children = _listDataChild.get(otherGroup);
        children.add(name);
        _listDataChild.put(otherGroup, children);

        notifyDataSetChanged();
    }

    private void MoveItemDown(int group, int child) {
        Map.Entry<Marker, String> belowChildName = (Map.Entry<Marker, String>) getChild(group, child + 1);
        String groupName = (String) getGroup(group);
        List<Map.Entry<Marker, String>> children = _listDataChild.get(groupName);

        children.remove(belowChildName);
        children.add(child, belowChildName);
        _listDataChild.put(groupName, children);
        notifyDataSetChanged();
        listOrderChanged = true;
    }


    private void MoveItemUp(int group, int child) {
        Map.Entry<Marker, String> childName = (Map.Entry<Marker, String>) getChild(group, child);
        Map.Entry<Marker, String> aboveChildName = (Map.Entry<Marker, String>) getChild(group, child - 1);
        String groupName = (String) getGroup(group);
        List<Map.Entry<Marker, String>> children = _listDataChild.get(groupName);

        children.remove(aboveChildName);
        children.add(child, aboveChildName);
        _listDataChild.put(groupName, children);
        notifyDataSetChanged();
        listOrderChanged = true;


    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    public HashMap<String, List<Map.Entry<Marker, String>>> getItems() {
        return _listDataChild;
    }


    public void setListReordered() {
        listOrderChanged = false;
    }

    public boolean isListOrderChanged() {
        return listOrderChanged;
    }
}
