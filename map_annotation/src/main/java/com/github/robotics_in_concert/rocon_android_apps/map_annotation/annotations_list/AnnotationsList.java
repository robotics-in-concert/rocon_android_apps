
package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.R;
import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.*;

public class AnnotationsList extends BaseExpandableListAdapter {

    @Override
    public boolean areAllItemsEnabled()
    {
        return true;
    }

    private Context                           context;
    private ExpandableListView                listView;
    private ArrayList<String>                 groups;
    private ArrayList<ArrayList<Annotation>>  children;

    public AnnotationsList(Context context, ExpandableListView listView) {
        this.context  = context;
        this.listView = listView;
        this.groups   = new ArrayList<String>();
        this.children = new ArrayList<ArrayList<Annotation>>();
    }

    public void addGroup(Annotation annotation) {
        if (!groups.contains(annotation.getGroup())) {
            groups.add(annotation.getGroup());
            children.add(new ArrayList<Annotation>());
            notifyDataSetChanged();
        }
    }

    /**
     * A general add method, that allows you to add a Annotation to this list
     * 
     * Depending on if the category opf the annotation is present or not,
     * the corresponding item will either be added to an existing group if it 
     * exists, else the group will be created and then the item will be added
     * @param annotation
     */
    public void addItem(Annotation annotation) {
        if (!groups.contains(annotation.getGroup())) {
            addGroup(annotation);
        }
        int index = groups.indexOf(annotation.getGroup());
        if (index < 0 || index >= children.size()) {
            Log.e("MapAnn", "Group index out of bounds: " + index);
        }
        else {
            children.get(index).add(annotation);
            notifyDataSetChanged();

            listView.expandGroup(index);
        }
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return children.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    
    // Return a child view. You can load your custom layout here.
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        Annotation annotation = (Annotation) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.child_layout, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.tvChild);
        tv.setText("   " + annotation.getName());

        // Depending upon the child type, set the imageTextView01
        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        if (annotation instanceof Column) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.column, 0, 0, 0);
        } else if (annotation instanceof Wall) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.wall, 0, 0, 0);
        } else if (annotation instanceof Marker) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.marker, 0, 0, 0);
        } else if (annotation instanceof Pickup) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pickup, 0, 0, 0);
        } else if (annotation instanceof Table) {
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.table, 0, 0, 0);
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return children.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    // Return a group view. You can load your custom layout here.
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        String group = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.group_layout, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.tvGroup);
        tv.setText(group);
//        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//        if (group.equalsIgnoreCase("Virtual Columns")) {
//            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.column, 0, 0, 0);
//            tv.setHeight(120);
//        } else if (group.equalsIgnoreCase("Virtual Walls")) {
//            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.wall, 0, 0, 0);
//            tv.setHeight(80);
//        } else if (group.equalsIgnoreCase("AR Markers")) {
//            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.marker, 0, 0, 0);
//            tv.setHeight(50);
//        } else if (group.equalsIgnoreCase("Pickup Points")) {
//            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pickup, 0, 0, 0);
//        } else if (group.equalsIgnoreCase("Tables")) {
//            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.table, 0, 0, 0);
//        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }

    // Extra candy methods
    public long getGroupId(Annotation annotation) {
        return getGroupId(groups.indexOf(annotation.getGroup()));
    }

    public ArrayList<Annotation> getChildren(String groupName) {
        int index = groups.indexOf(groupName);
        if (index == -1) {
            Log.e("MapAnn", "Invalid group name: " + groupName);
            return null;
        }

        return (ArrayList<Annotation>)children.get(index);
    }

    public ArrayList<Annotation> listFullContent() {
        ArrayList<Annotation> list = new ArrayList<Annotation>();
        for (ArrayList<Annotation> child : children) {
            list.addAll(child);
        }
        return list;
    }
}
