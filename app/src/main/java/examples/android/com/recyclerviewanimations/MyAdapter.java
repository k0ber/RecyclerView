package examples.android.com.recyclerviewanimations;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom adapter that supplies view holders to the RecyclerView. Our view holders
 * contain a simple LinearLayout (with a background color) and a TextView (displaying
 * the value of the container's bg color).
 */
class MyAdapter extends RecyclerView.Adapter implements ItemTouchHelperAdapter {

    private ArrayList<Integer> colors = new ArrayList<>();
    private RecyclerView recyclerView;
    private LayoutInflater layoutInflater;
    private RadioGroup radioGroup;


    MyAdapter(RecyclerView recyclerView, RadioGroup radioGroup, LayoutInflater layoutInflater) {
        this.recyclerView = recyclerView;
        this.layoutInflater = layoutInflater;
        this.radioGroup = radioGroup;
        generateData();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View container = layoutInflater.inflate(R.layout.item_layout, parent, false);
        container.setOnClickListener(mItemAction);
        return new MyViewHolder(container);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myHolder = (MyViewHolder) holder;
        int color = colors.get(position);
        myHolder.update(color, "#" + Integer.toHexString(color));
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    public void clear() {
        colors.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Integer> colors) {
        this.colors.addAll(colors);
        notifyDataSetChanged();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(colors, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(colors, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        if (position != RecyclerView.NO_POSITION) {
            colors.remove(position);
            notifyItemRemoved(position);
        }
    }


    private void deleteItem(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            colors.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void addItem(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            int color = generateColor();
            colors.add(position, color);
            notifyItemInserted(position);
        }
    }

    private void changeItem(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            int color = generateColor();
            colors.set(position, color);
            notifyItemChanged(position);
        }
    }

    private View.OnClickListener mItemAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (radioGroup.getCheckedRadioButtonId()) {
                case R.id.deleteRB:
                    deleteItem(v);
                    break;
                case R.id.addRB:
                    addItem(v);
                    break;
                case R.id.changeRB:
                    changeItem(v);
                    break;
            }
        }
    };

    private int generateColor() {
        int red = ((int) (Math.random() * 200));
        int green = ((int) (Math.random() * 200));
        int blue = ((int) (Math.random() * 200));
        return Color.rgb(red, green, blue);
    }

    private void generateData() {
        for (int i = 0; i < 100; ++i) {
            colors.add(generateColor());
        }
    }

}