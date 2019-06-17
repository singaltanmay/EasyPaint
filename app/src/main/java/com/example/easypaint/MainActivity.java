package com.example.easypaint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PaintView.onViewTouchedListener {

    private PaintView paintView;
    private BottomNavigationView navigationView;
    RecyclerView colorPickerRecyclerView;
    Paint paint;
    ConstraintLayout strokeStylePanel;
    private boolean eraserMode = false;

    private ArrayList<Integer> allColors = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paintView = findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);
        paint = paintView.getPaint();

        initBottomNavigationBar();

        initColorsArraylist();

        strokeStylePanel = findViewById(R.id.stroke_style_config_panel);
        colorPickerRecyclerView = findViewById(R.id.colorPickerRecyclerView);
        colorPickerRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        colorPickerRecyclerView.setLayoutManager(linearLayoutManager);

    }

    private void initColorsArraylist() {
        allColors.add(Color.RED);
        allColors.add(Color.BLUE);
        allColors.add(Color.GREEN);
        allColors.add(Color.YELLOW);
        allColors.add(Color.WHITE);
        allColors.add(Color.MAGENTA);
        allColors.add(Color.CYAN);
        allColors.add(Color.LTGRAY);
        allColors.add(Color.GRAY);
        allColors.add(Color.DKGRAY);
        allColors.add(Color.BLACK);
    }


    @Override
    public void onPaintWindowTouch() {
        if (!eraserMode) {
            managePanels(false, false);
        }
    }

    private void managePanels(boolean strokePanel, boolean colorPanel) {
        if (eraserMode) {
            eraserMode = false;
        }
        paintView.setPaint(paint);
        paintView.setStrokeColor(paint.getColor());
        if (!strokePanel) {
            strokeStylePanel.setVisibility(View.GONE);
        }
        if (!colorPanel) {
            colorPickerRecyclerView.setVisibility(View.GONE);
        }
        if (strokePanel) {
            strokeStylePanel.setVisibility(View.VISIBLE);
        }
        if (colorPanel) {
            colorPickerRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    private void initBottomNavigationBar() {
        navigationView = findViewById(R.id.bottom_navigation_view);

        navigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        int backgroundColor = paintView.getBackgroundColor();
                        switch (menuItem.getItemId()) {

                            case R.id.action_draw_mode:
                                managePanels(false, false);
                                break;
                            case R.id.action_stroke_style:
                                managePanels(true, false);
                                break;
                            case R.id.action_stroke_color:
                                managePanels(false, true);
                                List<Integer> colorOptions = new ArrayList<>(allColors);
                                int i = colorOptions.indexOf(backgroundColor);
                                if (i > 0) {
                                    colorOptions.remove(i);
                                }
                                colorPickerRecyclerView.setAdapter(new ColorPickerAdapter(colorOptions, ColorPickerAdapter.STROKE_COLOR_MODE));
                                break;
                            case R.id.action_background_color:
                                managePanels(false, true);
                                colorPickerRecyclerView.setAdapter(new ColorPickerAdapter(allColors, ColorPickerAdapter.BACKGROUND_PICKER_MODE));
                                break;
                            case R.id.action_eraser:
                                if (eraserMode) {
                                    managePanels(false, false);
                                    break;
                                }
                                managePanels(false, false);
                                eraserMode = true;

                                paint = paintView.getPaint();

                                paintView.setPaint(new Paint());
                                paintView.initPaint();
                                paintView.setStrokeColor(backgroundColor);
                                break;

                            default:
                                if (eraserMode) {
                                    eraserMode = false;
                                    paintView.setPaint(paint);
                                    paintView.setStrokeColor(paint.getColor());
                                }
                                colorPickerRecyclerView.setVisibility(View.GONE);
                                break;
                        }
                        return true;
                    }
                }
        );

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.normal:
                paintView.normal();
                return true;
            case R.id.emboss:
                paintView.emboss();
                return true;
            case R.id.blur:
                paintView.blur();
                return true;
            case R.id.clear:
                paintView.clear();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.MyViewHolder> {

        public static final int STROKE_COLOR_MODE = 0;
        public static final int BACKGROUND_PICKER_MODE = 1;


        private List<Integer> mDataset;
        private int currentMode;


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        private class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            private View view;

            private MyViewHolder(View v) {
                super(v);
                view = v;

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int adapterPosition = getAdapterPosition();
                        Toast.makeText(MainActivity.this, adapterPosition, Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

        // Provide a suitable constructor (depends on the kind of dataset)
        private ColorPickerAdapter(List<Integer> myDataset, int currentMode) {
            this.mDataset = myDataset;
            this.currentMode = currentMode;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ColorPickerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.color_picker_item, parent, false);

            ColorPickerAdapter.MyViewHolder vh = new ColorPickerAdapter.MyViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ColorPickerAdapter.MyViewHolder holder, int position) {
            final FloatingActionButton colorButton = (FloatingActionButton) holder.view;
            colorButton.setBackgroundTintList(ColorStateList.valueOf(mDataset.get(position)));
            colorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FloatingActionButton button = ((FloatingActionButton) view);

                    int selectedColor = button.getBackgroundTintList().getDefaultColor();
                    if (currentMode == STROKE_COLOR_MODE) {
                        paint.setColor(selectedColor);
                        paintView.setStrokeColor(selectedColor);
                    } else if (currentMode == BACKGROUND_PICKER_MODE) {
                        paintView.setBackgroundColor(selectedColor);
                    }


                }
            });
        }


        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }


}
