package examples.android.com.recyclerviewanimations;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

class MyViewHolder extends RecyclerView.ViewHolder {
    private TextView textView;
    LinearLayout container;

    MyViewHolder(View v) {
        super(v);
        container = (LinearLayout) v;
        textView = (TextView) v.findViewById(R.id.textview);
    }

    void update(int color, String text) {
        container.setBackgroundColor(color);
        textView.setText(text);
    }

    TextView getTextView() {
        return textView;
    }

    @Override
    public String toString() {
        return super.toString() + " \"" + textView.getText() + "\"";
    }
}