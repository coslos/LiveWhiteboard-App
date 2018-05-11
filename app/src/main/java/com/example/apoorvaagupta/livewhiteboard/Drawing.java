package com.example.apoorvaagupta.livewhiteboard;

import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.database.sqlite.SQLiteDatabase;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Drawing extends AppCompatActivity {
    public static final String TAG = "drawing";

    ImageButton brush, ibEraser, ibSave, ibClear;

    CanvasView drawingCanvas;

    Socket socket;

    private String drawingType = "drawing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        brush = findViewById(R.id.brush_button);
        ibEraser = findViewById(R.id.ibEraser);
        ibSave = findViewById(R.id.ibSave);
        ibClear = findViewById(R.id.ibClear);

        drawingCanvas = findViewById(R.id.drawing_canvas);

        DatabaseHandler dbHelper = new DatabaseHandler(this);

        final SQLiteDatabase writeDb = dbHelper.getWritableDatabase();
        final SQLiteDatabase readDb = dbHelper.getReadableDatabase();


        Log.d(TAG, "onCreate: " + getIntent());
        Intent i = getIntent();
        Log.d(TAG, "onCreate: " + i.hasExtra("sessionId"));
        if (i.hasExtra("sessionId")) {
            this.drawingType = "session";
            socket = ((SocketHandler) getApplication()).getSocket();
            drawingCanvas.setEmitTo("drawingInSession");
            drawingCanvas.setSessionId(i.getStringExtra("sessionId"));
            Log.d(TAG, "onCreate: Before");
            socket.on("drawingInSession", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d(TAG, "onCreate: In");
                    drawingCanvas.drawFromServer((JSONObject) args[0]);
                }
            });
            Toast.makeText(this, i.getStringExtra("sessionId"), Toast.LENGTH_SHORT).show();
        }

        if (i.hasExtra("drawingId")) {
            this.drawingType = "updateDrawing";
            Log.d(TAG, "onCreate: *******" + i.getIntExtra("drawingId", 1));
            Log.d(TAG, "onCreate: " + (DrawingsTable.getDrawing(i.getIntExtra("drawingId", 1), readDb)).getId());
            Log.d(TAG, "onCreate: " + (DrawingsTable.getDrawing(i.getIntExtra("drawingId", 1), readDb)).getName());
            byte[] bitmapdata = (DrawingsTable.getDrawing(i.getIntExtra("drawingId", 1), readDb)).getDrawing();
            try {
                Bitmap dbitmap = deserialize(bitmapdata);
                Log.d(TAG, "onClick:" + dbitmap);
                drawingCanvas.drawFromBitmap(dbitmap);
//                    Log.d(TAG, "onClick: called update" );
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        ibSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap bitmap = drawingCanvas.getBitmap();

//                Log.d(TAG, "onClick: " + bitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bitmapdata = stream.toByteArray();

                DrawingsTable.insertDrawing("Drawing", bitmapdata, writeDb);

                Toast.makeText(Drawing.this, "Saved", Toast.LENGTH_SHORT).show();

//                try {
//                    Bitmap dbitmap = deserialize(bitmapdata);
////                    Log.d(TAG, "onClick:"+ dbitmap);
//                    drawingCanvas.drawFromBitmap(dbitmap);
////                    Log.d(TAG, "onClick: called update" );
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }

//                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
//                Log.d(TAG, "onClick: " + path);
//                File file = new File(path+"/image.png");
//                FileOutputStream ostream;
//                try {
//                    file.createNewFile();
//                    ostream = new FileOutputStream(file);
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
//                    ostream.flush();
//                    ostream.close();
//                    Toast.makeText(getApplicationContext(), "image saved", Toast.LENGTH_LONG).show();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
//                }


//                bitmap =  Bitmap.createBitmap (content.getWidth(), content.getHeight(), Bitmap.Config.RGB_565);;
//                Canvas canvas = new Canvas(mBitmap);
//                v.draw(canvas);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                byte[] bitmapdata = stream.toByteArray();


            }
        });

        brush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(Drawing.this,brush);

                //To display the icons in the popup menu the following try catch method is required
                try {
                    Field[] fields = popupMenu.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popupMenu);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper
                                    .getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod(
                                    "setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_brush,popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()){
                            case R.id.ibBlack:
                                drawingCanvas.changeColor(Color.BLACK);
                                break;
                            case R.id.ibBlue:
                                drawingCanvas.changeColor(Color.BLUE);
                                break;
                            case R.id.ibGreen:
                                drawingCanvas.changeColor(Color.GREEN);
                                break;
                            case R.id.ibRed:
                                drawingCanvas.changeColor(Color.RED);
                                break;
                            case R.id.ibYellow:
                                drawingCanvas.changeColor(Color.YELLOW);
                                break;
                        }
                        return true;
                    }
                });

                popupMenu.show();
            }
        });

        ibEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingCanvas.setEraser();
            }
        });

        ibClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingCanvas.clearCanvas();
            }
        });


    }

    private static Bitmap deserialize(byte[] data) throws IOException, ClassNotFoundException {
//        ByteArrayInputStream in = new ByteArrayInputStream(data);
//        ObjectInputStream is = null;
//        try {
//            is = new ObjectInputStream(in);
//            Log.d(TAG, "deserialize: "+ is);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return (Bitmap) is.readObject();

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bitmap;
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Log.d(TAG, "onBackPressed: ");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("Are You Sure");
        switch (this.drawingType) {
            case "session":
                alertDialog.setMessage("You wanna leave this session?");
                break;
            case "drawing":
                alertDialog.setMessage("You wanna discard your master piece?");
                break;
            case "updateDrawing":
                alertDialog.setMessage("You wanna discard your master piece?");
                break;

        }

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: YES");
                finish();
            }
        })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: NO");
                    }
                })
                .show();

    }
}
