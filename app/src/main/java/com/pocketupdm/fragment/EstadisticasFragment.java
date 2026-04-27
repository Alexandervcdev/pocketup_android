package com.pocketupdm.fragment;

import static android.graphics.Typeface.BOLD;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstadisticasFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private RadarChart radarChart; // NUEVO
    private TextView tvMesActual, tvIngresos, tvGastos, tvBalance;
    private SessionManager sessionManager;

    private MaterialButton btnFiltroDatos, btnTipoGrafica;
    private List<MovimientoResponse> listaMovimientosGlobal = new ArrayList<>();

    private String filtroActual = "Gastos";
    private String tipoGraficaActual = "Pastel";

    private MaterialButton btnExportarPdf, btnVerInformes;

    public EstadisticasFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_estadisticas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        radarChart = view.findViewById(R.id.radarChart); // NUEVO

        tvMesActual = view.findViewById(R.id.tv_mes_actual);
        tvIngresos = view.findViewById(R.id.tv_stats_ingresos);
        tvGastos = view.findViewById(R.id.tv_stats_gastos);
        tvBalance = view.findViewById(R.id.tv_stats_balance);
        btnFiltroDatos = view.findViewById(R.id.btn_filtro_datos);
        btnTipoGrafica = view.findViewById(R.id.btn_tipo_grafica);
        btnExportarPdf = view.findViewById(R.id.btn_exportar_pdf);
        btnVerInformes = view.findViewById(R.id.btn_ver_informes);

        btnFiltroDatos.setOnClickListener(v -> mostrarMenuFiltros());
        btnTipoGrafica.setOnClickListener(v -> mostrarMenuTipoGrafica());

        configurarMesActual();
        configurarGraficasVacias();
        cargarDatosReales();
        btnExportarPdf.setOnClickListener(v -> generarInformePDF());
        btnVerInformes.setOnClickListener(v -> mostrarHistorialPDFs());
        com.github.mikephil.charting.listener.OnChartValueSelectedListener chartListener = new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e.getData() != null) {
                    String categoriaSeleccionada = e.getData().toString();
                    mostrarDetallesCategoria(categoriaSeleccionada);
                }
            }
            @Override
            public void onNothingSelected() {}
        };

        pieChart.setOnChartValueSelectedListener(chartListener);
        barChart.setOnChartValueSelectedListener(chartListener);
        radarChart.setOnChartValueSelectedListener(chartListener);
    }

    private void generarInformePDF() {
        android.graphics.pdf.PdfDocument pdfDocument = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();

        // Iniciamos la primera página
        android.graphics.pdf.PdfDocument.Page[] currentPage = {pdfDocument.startPage(pageInfo)};
        android.graphics.Canvas[] canvas = {currentPage[0].getCanvas()};

        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.Paint titlePaint = new android.graphics.Paint();

        int xPos = 50;
        final int[] yPos = {60}; // Usamos un array para poder modificarlo dentro de lambdas o bucles
        final int yLimit = 780; // Límite de seguridad para saltar de página

        // --- 1. CABECERA (Página 1) ---
        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, BOLD));
        titlePaint.setTextSize(24f);
        titlePaint.setColor(getResources().getColor(R.color.turquesa_oscuro, null));
        canvas[0].drawText("POCKETUP", xPos, yPos[0], titlePaint);

        paint.setTextSize(12f);
        paint.setColor(android.graphics.Color.GRAY);
        canvas[0].drawText("Informe Mensual: " + tvMesActual.getText().toString(), xPos, yPos[0] + 25, paint);
        yPos[0] += 60;

        // --- 2. CAMBIO 1: RESUMEN DE CUENTA (ARRIBA) ---
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F5F5F5"));
        canvas[0].drawRoundRect(xPos - 10, yPos[0], 545, yPos[0] + 70, 15, 15, paint);

        paint.setColor(Color.BLACK);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, BOLD));
        paint.setTextSize(14f);
        canvas[0].drawText("RESUMEN DE BALANCE", xPos, yPos[0] + 25, paint);

        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        canvas[0].drawText("Ingresos: " + tvIngresos.getText().toString(), xPos + 10, yPos[0] + 50, paint);
        canvas[0].drawText("Gastos: " + tvGastos.getText().toString(), xPos + 180, yPos[0] + 50, paint);
        canvas[0].drawText("Disponible: " + tvBalance.getText().toString(), xPos + 350, yPos[0] + 50, paint);

        yPos[0] += 100;

        // --- 3. CAMBIO 2: GRÁFICA CON MEJOR CALIDAD ---
        android.graphics.Bitmap bitmapGrafica;
        if (tipoGraficaActual.equals("Pastel")) bitmapGrafica = pieChart.getChartBitmap();
        else if (tipoGraficaActual.equals("Barras")) bitmapGrafica = barChart.getChartBitmap();
        else bitmapGrafica = radarChart.getChartBitmap();

        // Para evitar el pixelado, usamos un escalado con filtro bilineal activo
        int targetWidth = 450;
        int targetHeight = (bitmapGrafica.getHeight() * targetWidth) / bitmapGrafica.getWidth();
        android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmapGrafica, targetWidth, targetHeight, true);

        // Centramos la imagen
        canvas[0].drawBitmap(scaledBitmap, (595 - targetWidth) / 2f, yPos[0], null);
        yPos[0] += targetHeight + 50;

        // --- 4. CAMBIO 3: MOVIMIENTOS CON PAGINACIÓN ---
        paint.setColor(Color.BLACK);
        paint.setTextSize(16f);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, BOLD));
        canvas[0].drawText("DETALLE DE MOVIMIENTOS", xPos, yPos[0], paint);
        canvas[0].drawLine(xPos, yPos[0] + 5, 545, yPos[0] + 5, paint);
        yPos[0] += 40;

        // Agrupamos movimientos por categoría
        Map<String, List<MovimientoResponse>> grupos = new HashMap<>();
        for (MovimientoResponse m : listaMovimientosGlobal) {
            String cat = (m.getCategoria() != null) ? m.getCategoria().getNombre() : "Otros";
            if (!grupos.containsKey(cat)) grupos.put(cat, new ArrayList<>());
            grupos.get(cat).add(m);
        }

        paint.setTextSize(12f);
        for (Map.Entry<String, List<MovimientoResponse>> entrada : grupos.entrySet()) {

            // ¿CABEMOS? Si no cabe el título de la categoría, saltamos de página
            if (yPos[0] > yLimit - 40) {
                pdfDocument.finishPage(currentPage[0]);
                currentPage[0] = pdfDocument.startPage(pageInfo);
                canvas[0] = currentPage[0].getCanvas();
                yPos[0] = 50; // Reiniciamos arriba en la nueva hoja
            }

            // Título Categoría
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, BOLD));
            paint.setColor(getResources().getColor(R.color.turquesa_oscuro, null));
            canvas[0].drawText("■ " + entrada.getKey().toUpperCase(), xPos, yPos[0], paint);
            yPos[0] += 25;

            // Lista de movimientos
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
            paint.setColor(Color.BLACK);

            for (MovimientoResponse mov : entrada.getValue()) {

                // ¿CABEMOS? Si la línea del movimiento se va a salir, saltamos de página
                if (yPos[0] > yLimit) {
                    pdfDocument.finishPage(currentPage[0]);
                    currentPage[0] = pdfDocument.startPage(pageInfo);
                    canvas[0] = currentPage[0].getCanvas();
                    yPos[0] = 50;

                    // Repetimos el título de la categoría en la nueva página para que no se pierda el contexto
                    paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, BOLD));
                    paint.setColor(getResources().getColor(R.color.turquesa_oscuro, null));
                    canvas[0].drawText("■ " + entrada.getKey().toUpperCase() + " (continuación)", xPos, yPos[0], paint);
                    yPos[0] += 25;
                    paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
                    paint.setColor(Color.BLACK);
                }

                String fecha = mov.getFecha();
                String nota = (mov.getNota() != null) ? mov.getNota() : "Sin nota";
                String monto = (mov.getTipo() == MovementType.INGRESO ? "+" : "-") + mov.getImporte() + "€";

                canvas[0].drawText(fecha, xPos + 10, yPos[0], paint);
                canvas[0].drawText(nota, xPos + 100, yPos[0], paint);

                paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                canvas[0].drawText(monto, 545, yPos[0], paint);
                paint.setTextAlign(android.graphics.Paint.Align.LEFT);

                yPos[0] += 20;
            }
            yPos[0] += 15; // Espacio entre bloques de categorías
        }

        // Cerramos la última página activa
        pdfDocument.finishPage(currentPage[0]);

        // 5. GUARDAR Y LANZAR NOTIFICACIÓN
        try {
            java.io.File directorio = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            String nombreArchivo = "PocketUp_Full_Report_" + System.currentTimeMillis() + ".pdf";
            java.io.File archivoPDF = new java.io.File(directorio, nombreArchivo);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(archivoPDF);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            mostrarNotificacionPDF(archivoPDF);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarNotificacionPDF(java.io.File archivo) {
        String channelId = "informes_pdf";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        // En móviles modernos hay que crear un "Canal de Notificación"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Informes PDF", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Usamos el FileProvider para crear una ruta segura (content://) que el lector de PDF pueda entender
        android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                archivo);

        // Preparamos el "paquete" (Intent) que le dirá a Android: "Abre este PDF"
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); // Damos permiso de lectura temporal

        // Esto es lo que se ejecutará cuando el usuario toque la notificación
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                requireContext(), 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        // Diseñamos la notificación
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_chart) // Usamos tu propio icono de las gráficas
                .setContentTitle("¡Informe Generado!")
                .setContentText("Toca aquí para abrir " + archivo.getName())
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH) // Sale como "Pop-up" emergente
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Se borra de la lista al tocarla

        notificationManager.notify(1001, builder.build());
    }

    private void configurarMesActual() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String mesActual = sdfMes.format(calendar.getTime());
        tvMesActual.setText(mesActual.substring(0, 1).toUpperCase() + mesActual.substring(1));
    }

    private void configurarGraficasVacias() {
        // --- DONA ---
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setCenterTextColor(getResources().getColor(R.color.turquesa_oscuro, null));
        pieChart.getLegend().setEnabled(false);
        pieChart.setNoDataText("Aún no hay movimientos este mes");
        pieChart.setNoDataTextColor(Color.GRAY);

        // --- BARRAS ---
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.setNoDataText("Aún no hay movimientos este mes");
        barChart.setNoDataTextColor(Color.GRAY);

        // --- RADAR (NUEVO) ---
        radarChart.getDescription().setEnabled(false);
        radarChart.getLegend().setEnabled(false);
        radarChart.setWebLineWidth(1f); // Grosor telaraña principal
        radarChart.setWebColor(Color.LTGRAY);
        radarChart.setWebLineWidthInner(1f); // Grosor telaraña interior
        radarChart.setWebColorInner(Color.LTGRAY);
        radarChart.setWebAlpha(100);
        radarChart.setNoDataText("Aún no hay movimientos este mes");
        radarChart.setNoDataTextColor(Color.GRAY);
    }

    private void mostrarMenuFiltros() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), btnFiltroDatos);
        popup.getMenu().add("Resumen (Ingresos vs Gastos)");
        popup.getMenu().add("Gastos");
        popup.getMenu().add("Ingresos");

        popup.setOnMenuItemClickListener(item -> {
            filtroActual = item.getTitle().toString();
            btnFiltroDatos.setText(filtroActual.split(" ")[0]);
            procesarEstadisticas();
            return true;
        });
        popup.show();
    }

    private void mostrarMenuTipoGrafica() {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), btnTipoGrafica);
        popup.getMenu().add("Pastel");
        popup.getMenu().add("Barras");
        popup.getMenu().add("Radar"); // NUEVO EN EL MENÚ

        popup.setOnMenuItemClickListener(item -> {
            tipoGraficaActual = item.getTitle().toString();
            btnTipoGrafica.setText(tipoGraficaActual);
            procesarEstadisticas();
            return true;
        });
        popup.show();
    }

    private void cargarDatosReales() {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        RetrofitClient.getApiService().obtenerMovimientos(usuarioId).enqueue(new Callback<List<MovimientoResponse>>() {
            @Override
            public void onResponse(Call<List<MovimientoResponse>> call, Response<List<MovimientoResponse>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    listaMovimientosGlobal = response.body();
                    procesarEstadisticas();
                } else {
                    Toast.makeText(getContext(), "Error al cargar estadísticas", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<MovimientoResponse>> call, Throwable t) {
                Log.e("ESTADISTICAS", "Fallo de red: ", t);
            }
        });
    }

    private void procesarEstadisticas() {
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;
        Map<String, Float> datosParaGrafica = new HashMap<>();

        for (MovimientoResponse mov : listaMovimientosGlobal) {
            float importe = mov.getImporte().floatValue();
            String nombreCat = (mov.getCategoria() != null) ? mov.getCategoria().getNombre() : "Otros";

            if (mov.getTipo() == MovementType.INGRESO) {
                totalIngresos = totalIngresos.add(mov.getImporte());
                if (filtroActual.equals("Ingresos")) {
                    datosParaGrafica.put(nombreCat, datosParaGrafica.getOrDefault(nombreCat, 0f) + importe);
                }
            } else {
                totalGastos = totalGastos.add(mov.getImporte());
                if (filtroActual.equals("Gastos")) {
                    datosParaGrafica.put(nombreCat, datosParaGrafica.getOrDefault(nombreCat, 0f) + importe);
                }
            }
        }

        if (filtroActual.startsWith("Resumen")) {
            if (totalIngresos.floatValue() > 0) datosParaGrafica.put("Ingresos", totalIngresos.floatValue());
            if (totalGastos.floatValue() > 0) datosParaGrafica.put("Gastos", totalGastos.floatValue());
        }

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        tvIngresos.setText(formatoMoneda.format(totalIngresos));
        tvGastos.setText(formatoMoneda.format(totalGastos));
        tvBalance.setText(formatoMoneda.format(totalIngresos.subtract(totalGastos)));

        // COLOR DINÁMICO PARA LOS TEXTOS DE LAS GRÁFICAS (Soporte Modo Oscuro)
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int colorTextoTema = typedValue.data;


        // RENDERIZADO VISUAL
        if (tipoGraficaActual.equals("Pastel")) {
            barChart.setVisibility(View.GONE);
            radarChart.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);

            ArrayList<PieEntry> entradasGrafica = new ArrayList<>();
            // Para Pastel:
            for (Map.Entry<String, Float> entry : datosParaGrafica.entrySet()) {
                entradasGrafica.add(new PieEntry(entry.getValue(), entry.getKey(), entry.getKey())); // <-- Añadido el entry.getKey() al final
            }

            if (entradasGrafica.isEmpty()) { pieChart.clear(); return; }

            PieDataSet dataSet = new PieDataSet(entradasGrafica, "");
            if (filtroActual.startsWith("Resumen")) {
                dataSet.setColors(getResources().getColor(R.color.turquesa_oscuro, null), getResources().getColor(R.color.red, null));
            } else {
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            }
            dataSet.setSliceSpace(4f);
            dataSet.setValueTextSize(14f);
            dataSet.setValueTextColor(Color.WHITE);

            pieChart.setData(new PieData(dataSet));
            pieChart.setCenterText(filtroActual.split(" ")[0]);
            pieChart.animateY(1000);
            pieChart.invalidate();

        } else if (tipoGraficaActual.equals("Barras")) {
            pieChart.setVisibility(View.GONE);
            radarChart.setVisibility(View.GONE);
            barChart.setVisibility(View.VISIBLE);

            ArrayList<BarEntry> entradasBarras = new ArrayList<>();
            ArrayList<String> etiquetasEjeX = new ArrayList<>();
            int index = 0;

            // Para Barras:
            for (Map.Entry<String, Float> entry : datosParaGrafica.entrySet()) {
                entradasBarras.add(new BarEntry(index, entry.getValue(), entry.getKey())); // <-- Añadido el entry.getKey() al final
                etiquetasEjeX.add(entry.getKey());
                index++;
            }

            if (entradasBarras.isEmpty()) { barChart.clear(); return; }

            BarDataSet dataSet = new BarDataSet(entradasBarras, "");
            if (filtroActual.startsWith("Resumen")) {
                dataSet.setColors(getResources().getColor(R.color.turquesa_oscuro, null), getResources().getColor(R.color.red, null));
            } else {
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            }
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(colorTextoTema);

            BarData data = new BarData(dataSet);
            data.setBarWidth(0.6f);
            barChart.setData(data);

            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetasEjeX));
            barChart.getXAxis().setTextColor(colorTextoTema);
            barChart.getAxisLeft().setTextColor(colorTextoTema);

            barChart.animateY(1000);
            barChart.invalidate();

        } else if (tipoGraficaActual.equals("Radar")) {
            // NUEVO BLOQUE: LÓGICA DE LA GRÁFICA DE RADAR
            pieChart.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            radarChart.setVisibility(View.VISIBLE);

            ArrayList<RadarEntry> entradasRadar = new ArrayList<>();
            ArrayList<String> etiquetasRadar = new ArrayList<>();

            // Para Radar:
            for (Map.Entry<String, Float> entry : datosParaGrafica.entrySet()) {
                entradasRadar.add(new RadarEntry(entry.getValue(), entry.getKey())); // <-- Añadido el entry.getKey() al final
                etiquetasRadar.add(entry.getKey());
            }

            if (entradasRadar.isEmpty()) { radarChart.clear(); return; }

            RadarDataSet dataSet = new RadarDataSet(entradasRadar, "");
            int colorFuerte = getResources().getColor(R.color.turquesa_oscuro, null);

            // Le damos estilo "Cyberpunk/Financiero"
            dataSet.setColor(colorFuerte);
            dataSet.setFillColor(colorFuerte);
            dataSet.setDrawFilled(true); // Hace que el interior se coloree
            dataSet.setFillAlpha(100); // 100 de 255 (semitransparente)
            dataSet.setLineWidth(2f);
            dataSet.setDrawHighlightCircleEnabled(true);
            dataSet.setDrawHighlightIndicators(false);

            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(colorTextoTema);

            RadarData data = new RadarData(dataSet);
            radarChart.setData(data);

            // Configuramos los textos de las "puntas" de la telaraña
            radarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetasRadar));
            radarChart.getXAxis().setTextColor(colorTextoTema);
            radarChart.getXAxis().setTextSize(12f);

            radarChart.getYAxis().setDrawLabels(false); // Oculta los números internos para que quede limpio

            radarChart.animateXY(1000, 1000);
            radarChart.invalidate();
        }
    }

    private void mostrarDetallesCategoria(String categoria) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        // Creamos la vista directamente por código para no saturar tu carpeta de layouts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);

        TextView titulo = new TextView(requireContext());
        titulo.setText("Desglose: " + categoria);
        titulo.setTextSize(22f);
        titulo.setTypeface(null, BOLD);
        layout.addView(titulo);

        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(requireContext());
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));

        // 1. Filtramos los movimientos que pertenecen a esa categoría y a ese tipo (Ingreso/Gasto)
        List<MovimientoResponse> filtrados = new ArrayList<>();
        for (MovimientoResponse m : listaMovimientosGlobal) {
            String cat = m.getCategoria() != null ? m.getCategoria().getNombre() : "Otros";

            boolean coincideTipo = false;
            if (filtroActual.startsWith("Resumen")) coincideTipo = true;
            else if (filtroActual.equals("Ingresos") && m.getTipo() == MovementType.INGRESO) coincideTipo = true;
            else if (filtroActual.equals("Gastos") && m.getTipo() == MovementType.GASTO) coincideTipo = true;

            if (cat.equals(categoria) && coincideTipo) {
                filtrados.add(m);
            }
        }

        // 2. Usamos tu espectacular MovimientoAdapter
        com.pocketupdm.adapter.MovimientoAdapter adapter = new com.pocketupdm.adapter.MovimientoAdapter(requireContext(), filtrados, null);
        rv.setAdapter(adapter);

        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 40;
        layout.addView(rv, params);

        dialog.setContentView(layout);
        dialog.show();
    }

    private void mostrarHistorialPDFs() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        android.widget.LinearLayout layoutMain = new android.widget.LinearLayout(requireContext());
        layoutMain.setOrientation(android.widget.LinearLayout.VERTICAL);
        layoutMain.setPadding(60, 60, 60, 60);

        TextView titulo = new TextView(requireContext());
        titulo.setText("Historial de Informes");
        titulo.setTextSize(22f);
        titulo.setTypeface(null, BOLD);
        layoutMain.addView(titulo);

        // Añadimos un ScrollView por si el usuario tiene muchísimos informes
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout layoutArchivos = new android.widget.LinearLayout(requireContext());
        layoutArchivos.setOrientation(android.widget.LinearLayout.VERTICAL);
        layoutArchivos.setPadding(0, 30, 0, 0);

        // Leemos la carpeta segura
        java.io.File directorio = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
        java.io.File[] archivos = directorio != null ? directorio.listFiles() : new java.io.File[0];

        if (archivos == null || archivos.length == 0) {
            TextView empty = new TextView(requireContext());
            empty.setText("No hay informes guardados.");
            layoutArchivos.addView(empty);
        } else {
            java.util.Arrays.sort(archivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            for (java.io.File file : archivos) {
                if (file.getName().endsWith(".pdf")) {
                    // CONTENEDOR DE LA FILA
                    android.widget.LinearLayout fila = new android.widget.LinearLayout(requireContext());
                    fila.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    fila.setPadding(0, 20, 0, 20);
                    fila.setClickable(true);
                    fila.setFocusable(true);

                    // NOMBRE DEL ARCHIVO (Click para abrir)
                    TextView tvNombre = new TextView(requireContext());
                    tvNombre.setText(file.getName().replace(".pdf", "").replace("_", " "));
                    tvNombre.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    tvNombre.setTextSize(16f);
                    tvNombre.setOnClickListener(v -> { dialog.dismiss(); abrirPDFLocal(file); });
                    fila.addView(tvNombre);

                    // BOTÓN DE LOS 3 PUNTOS
                    android.widget.ImageButton btnOpciones = new android.widget.ImageButton(requireContext());
                    btnOpciones.setImageResource(R.drawable.ic_open_in_new_24px); // Puedes usar ic_more_vert si lo tienes
                    btnOpciones.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    btnOpciones.setOnClickListener(v -> mostrarMenuOpcionesArchivo(v, file, dialog));
                    fila.addView(btnOpciones);

                    layoutArchivos.addView(fila);

                    // Línea divisoria suave
                    View divisor = new View(requireContext());
                    divisor.setLayoutParams(new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                    divisor.setBackgroundColor(android.graphics.Color.LTGRAY);
                    layoutArchivos.addView(divisor);
                }
            }
        }

        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(layoutArchivos);
        layoutMain.addView(scroll);

        dialog.setContentView(layoutMain);
        dialog.show();
    }

    private void mostrarMenuOpcionesArchivo(View anchor, java.io.File archivo, com.google.android.material.bottomsheet.BottomSheetDialog parentDialog) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add("Compartir");
        popup.getMenu().add("Eliminar");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Compartir")) {
                compartirPDF(archivo);
            } else if (item.getTitle().equals("Eliminar")) {
                confirmarEliminacionPDF(archivo, parentDialog);
            }
            return true;
        });
        popup.show();
    }
    private void compartirPDF(java.io.File archivo) {
        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), requireContext().getPackageName() + ".provider", archivo);

        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(android.content.Intent.createChooser(intent, "Compartir Informe via..."));
    }

    private void confirmarEliminacionPDF(java.io.File archivo, com.google.android.material.bottomsheet.BottomSheetDialog parentDialog) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar informe")
                .setMessage("¿Estás seguro de que quieres borrar '" + archivo.getName() + "'?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, which) -> {
                    if (archivo.delete()) {
                        Toast.makeText(requireContext(), "Informe eliminado", Toast.LENGTH_SHORT).show();
                        parentDialog.dismiss();
                        mostrarHistorialPDFs(); // Recargamos la lista para que desaparezca
                    }
                })
                .show();
    }

    private void abrirPDFLocal(java.io.File archivo) {
        try {
            // Reutilizamos el puente de seguridad (FileProvider)
            android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    archivo);

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); // Permiso de lectura temporal

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(requireContext(), "No tienes ninguna aplicación instalada para leer PDFs.", Toast.LENGTH_LONG).show();
        }
    }
}