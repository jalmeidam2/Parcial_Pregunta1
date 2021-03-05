package com.example.parcial_2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageView Imagen;
    public Vision vision;
    TextView Datos;
    ListView opcion;
    String mensaje;
    List<String> SeleccionarOpcion;

    private void mostrar() {
        final String[] lista =
                new String[]{"LABEL_DETECTION"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, lista);
        opcion.setAdapter(arrayAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Imagen = (ImageView) findViewById(R.id.idImagen);
        opcion = (ListView) findViewById(R.id.IdObjectDetection);
        mostrar();
        //barra scroll
        Datos = findViewById(R.id.idDatos);
        Datos.setMovementMethod(new ScrollingMovementMethod());
        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs"));
        vision = visionBuilder.build();
    }

    public void SeleccionarImagen(View v) {
        final CharSequence[] opciones = {"Tomar Foto", "Buscar Imagen"};
        final AlertDialog.Builder opcions = new AlertDialog.Builder(MainActivity.this);
        opcions.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (opciones[i].toString()) {
                    case "Tomar Foto":
                        tomarFoto();
                        break;
                    case "Buscar Imagen":
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent, "Seleccione la app"), 10);
                        break;
                }
            }
        });
        opcions.show();
    }
    @SuppressLint("QueryPermissionsNeeded")
    private void tomarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public void validar(View view) {
        SeleccionarOpcion = new ArrayList<>();
        long[] idItemsCheck = opcion.getCheckItemIds();
        for (int i = 0; i < idItemsCheck.length; i++) {
            SeleccionarOpcion.add(opcion.getItemAtPosition((int) idItemsCheck[i]).toString());
        }
        if (SeleccionarOpcion.size() > 0) {
            Datos.setText("");
            mensaje = "";
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    BitmapDrawable drawable = (BitmapDrawable) Imagen.getDrawable();
                    Bitmap bitmap = drawable.getBitmap();
                    bitmap = scaleBitmapDown(bitmap, 1200);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream(); //2da de la api
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    byte[] imageInByte = stream.toByteArray();
                    Image inputImage = new Image(); //googlevision
                    inputImage.encodeContent(imageInByte);
                    List<Feature> desiredFeaturelst = new ArrayList<>();
                    Feature item;
                    for (int i = 0; i < SeleccionarOpcion.size(); i++) {
                        item = new Feature();
                        item.setType(SeleccionarOpcion.get(i));
                        desiredFeaturelst.add(item);
                    }
                    AnnotateImageRequest respuesta = new AnnotateImageRequest();
                    respuesta.setImage(inputImage);
                    respuesta.setFeatures(desiredFeaturelst);
                    BatchAnnotateImagesRequest batchRequest = new
                            BatchAnnotateImagesRequest();
                    batchRequest.setRequests(Arrays.asList(respuesta));
                    BatchAnnotateImagesResponse batchResponse = null;
                    try {
                        Vision.Images.Annotate annotateRequest =
                                vision.images().annotate(batchRequest);
                        annotateRequest.setDisableGZipContent(true);
                        batchResponse = annotateRequest.execute();
                    } catch (IOException ex) {
                        mensaje += ex.getMessage() + "\n";
                    }
                    if (batchResponse != null) {
                        for (int i = 0; i < SeleccionarOpcion.size(); i++) {
                            switch (SeleccionarOpcion.get(i)) {
                                case "LABEL_DETECTION":
                                    mensaje += "LABEL_DETECTION" + "\n";
                                    StringBuilder messagebuilder = new StringBuilder("Bandera detectada:\n\n");
                                    List<EntityAnnotation> labels =
                                            batchResponse.getResponses().get(0).getLabelAnnotations();
                                    if (labels != null) {
                                        for (EntityAnnotation label : labels) {
                                            messagebuilder.append(String.format(Locale.US, "%.3f: %s",
                                                    label.getScore(), label.getDescription()));
                                            messagebuilder.append("\n");
                                        }
                                    } else {
                                        messagebuilder.append("error, no se ha encontrado nada");
                                    }
                                    mensaje += messagebuilder.toString() + "\n";
                                    break;
                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Datos.setText(mensaje);
                        }
                    });
                }
            });
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 10:
                    Uri MIpath = data.getData();
                    Imagen.setImageURI(MIpath);
                    break;
                case 1:
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    Imagen.setImageBitmap(imageBitmap);
                    break;
                default:
                    throw new IllegalStateException("evaluar: " + requestCode);
            }
        }
    }
}
