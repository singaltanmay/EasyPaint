package com.example.easypaint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PaintView.onViewTouchedListener {

    private PaintView paintView;
    private BottomNavigationView navigationView;
    private final int RESULT_LOAD_IMAGE = 2893;
    private final String URI_KEY = "uri";
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    RecyclerView colorPickerRecyclerView;
    Paint paint;
    ConstraintLayout strokeStylePanel;
    private boolean eraserMode = false;

    private ArrayList<Integer> allColors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTopNavigationBar();
        initPaintView();
        initBottomNavigationBar();

        initColorsArrayList();

        strokeStylePanel = findViewById(R.id.stroke_style_config_panel);
        colorPickerRecyclerView = findViewById(R.id.colorPickerRecyclerView);
        colorPickerRecyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        colorPickerRecyclerView.setLayoutManager(linearLayoutManager);

    }

    private void initTopNavigationBar() {
        Toolbar top_action_bar = findViewById(R.id.top_action_bar);
        top_action_bar.inflateMenu(R.menu.top_navigation_main);
        top_action_bar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.action_share_canvas:
                        Bitmap bitmap1 = paintView.exportCanvas();
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                            if (!(checkIfAlreadyHavePermission())) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                            } else sendImageViaIntent(bitmap1);
                        } else sendImageViaIntent(bitmap1);
                        break;

                    case R.id.action_save_canvas:
                        Bitmap bitmap = paintView.exportCanvas();
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                            if (!(checkIfAlreadyHavePermission())) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
                            } else saveImageExternal(bitmap);
                        } else saveImageExternal(bitmap);


                        break;

                    case R.id.action_import_bitmap:
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                            if (!(checkIfAlreadyHavePermission())) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            } else importImageFromGallery();
                        } else importImageFromGallery();

                        break;

                    case R.id.action_clear_screen:
                        paintView.clear();
                        break;
                }

                return true;
            }
        });
    }

    private void saveImageExternal(final Bitmap image) {


        Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String uriString = msg.getData().getString(URI_KEY);
                Toast.makeText(MainActivity.this, "Image saved successfully at " + uriString, Toast.LENGTH_SHORT).show();
            }
        };

        saveImageThread(handler, image);

    }

    private void sendImageViaIntent(final Bitmap image) {


        Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String uriString = msg.getData().getString(URI_KEY);

                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/png");
                startActivity(intent);
            }
        };

        saveImageThread(handler, image);

    }

    private void saveImageThread(final Handler handler, final Bitmap image) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String currentDateandTime = sdf.format(new Date());
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyDrawing_" + currentDateandTime + ".png");
                    FileOutputStream stream = new FileOutputStream(file);
                    image.compress(Bitmap.CompressFormat.PNG, 90, stream);
                    stream.close();
                    uri = Uri.fromFile(file);
                    Log.d(LOG_TAG, "Image saved at " + uri.toString());
                    Message message = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString(URI_KEY, uri.toString());
                    message.setData(bundle);
                    handler.sendMessage(message);
                } catch (IOException e) {
                    Log.d(LOG_TAG, "IOException while trying to write file for sharing: " + e.getMessage());
                }
            }
        });

        thread.start();
    }

    private void importImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    private boolean checkIfAlreadyHavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importImageFromGallery();

            } else {
                Toast.makeText(this, "Permission is required to import image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageExternal(paintView.exportCanvas());

            } else {
                Toast.makeText(this, "Permission is required to save image", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == 3) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendImageViaIntent(paintView.exportCanvas());

            } else {
                Toast.makeText(this, "Permission is required to share image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            paintView.addSticker(bitmap);
            Log.d(LOG_TAG, "Path to image: " + picturePath);
        }
    }

    private void initPaintView() {
        paintView = findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //TODO fix canvas larger than view size
//        int measuredHeight = paintView.getMeasuredHeight();
//        Log.d(LOG_TAG, "Measure height of paintView is " + measuredHeight);
//        metrics.heightPixels = measuredHeight;
        paintView.init(metrics);
        paint = paintView.getPaint();
    }

    private void initColorsArrayList() {
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
            navigationView.setSelectedItemId(R.id.action_draw_mode);
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
            initStrokeStylePanel();
        }
        if (colorPanel) {
            colorPickerRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    private void initStrokeStylePanel() {

        RadioGroup group = findViewById(R.id.brushModeRadioGroup);
        if (paintView.isEmboss()) {
            group.check(R.id.radioButtonStrokeEmboss);
        } else if (paintView.isBlur()) {
            group.check(R.id.radioButtonStrokeBlur);
        } else {
            group.check(R.id.radioButtonStrokeNormal);
        }

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.radioButtonStrokeNormal:
                        paintView.normal();
                        break;
                    case R.id.radioButtonStrokeEmboss:
                        paintView.emboss();
                        break;
                    case R.id.radioButtonStrokeBlur:
                        paintView.blur();
                        break;
                    default:
                        paintView.clear();
                        break;
                }
            }
        });

        SeekBar brushSize = findViewById(R.id.brushSizeSeekBar);
        brushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int valueChanged = 20;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                valueChanged = i;
                paintView.setStrokeWidth(valueChanged);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                TextView textView = findViewById(R.id.brushSizeSeekedValue);
                textView.setText(valueChanged + "px");

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                paintView.setStrokeWidth(valueChanged);
                TextView textView = findViewById(R.id.brushSizeSeekedValue);
                textView.setText(valueChanged + "px");
            }
        });


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
                                paintView.setCapStyle(Paint.Cap.ROUND);
                                paintView.setEmboss(false);
                                paintView.setBlur(false);
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

    private class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.MyViewHolder> {

        public static final int STROKE_COLOR_MODE = 0;
        public static final int BACKGROUND_PICKER_MODE = 1;


        private List<Integer> mDataset;
        private int currentMode;

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
