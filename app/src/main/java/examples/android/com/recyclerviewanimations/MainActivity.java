package examples.android.com.recyclerviewanimations;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setItemAnimator(new SlideInRightAnimator());
        recyclerView.setItemAnimator(new MyChangeAnimator());

        recyclerView.setHasFixedSize(true);

        final MyAdapter adapter = new MyAdapter(recyclerView, mRadioGroup, getLayoutInflater());
        recyclerView.setAdapter(adapter);

        RecyclerView.ItemDecoration itemDecoration = new SpacesItemDecoration(16);
        recyclerView.addItemDecoration(itemDecoration);

        ItemTouchHelper.Callback callback = new SimpleItemHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.black, android.R.color.holo_green_light);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(MainActivity.this, "REFRESH", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        adapter.clear();
//                        adapter.addAll();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 4000);
            }
        });

    }

}
