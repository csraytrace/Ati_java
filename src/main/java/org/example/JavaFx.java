package org.example;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;


// Charts:
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;



import javafx.util.converter.DoubleStringConverter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.CheckBoxTableCell;

public class JavaFx extends Application {

    // Model
    private final BooleanProperty exampleMode = new SimpleBooleanProperty(false);
    private double globalStep() { return parseDouble(energieStep, 0.01); }
    private double globalEmax() { return parseDouble(xRayTubeVoltage, 35.0); }
    private static double globalEmin = 0;
    private static final String DATA_FILE = "MCMASTER.TXT";

    // Tube
    private TextField tubeMaterial;
    private TextField electronIncidentAngle;
    private TextField electronTakeoffAngle;
    private TextField charZuCont;
    private TextField charZuContL;
    private TextField windowMaterial;
    private TextField windowMaterialThickness;
    private TextField tubeCurrent;
    private TextField xRayTubeVoltage;
    private TextField sigmaConst;
    private TextField energieStep;
    private TextField measurementTime;
    private ComboBox<String> tubeModel;
    private VBox inputBox;               // linker Eingabebereich
    private BorderPane plotPane;         // rechter Bereich mit Plot
    private SplitPane split;             // nur sichtbar, wenn Plot offen
    private BorderPane parametersRoot;   // Container für den Tab-Inhalt (switcht zwischen inputBox und split)
    private boolean plotVisible = false;
    private CheckBox chkLogX;
    private CheckBox chkLogY;
    private CheckBox chkAccumulate;
    private ImageView previewImage;
    private HBox formWithImage;

    // Detector

    // Detector-Parameter (Klassenattribute)
    private TextField windowMaterialDet;
    private TextField thicknessWindowDet;         // in µm
    private TextField contactlayerDet;
    private TextField contactlayerThicknessDet; // in nm
    private TextField detectorMaterial;
    private TextField inactiveLayer;           // in µm
    private TextField activeLayer;          // in mm




    @Override
    public void start(Stage stage) {
        // ----- Menus -----
        Menu fileMenu = new Menu("File");
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());
        fileMenu.getItems().add(exit);

        Menu viewMenu = new Menu("View");
        CheckMenuItem darkModeToggle = new CheckMenuItem("Dark Mode");
        CheckMenuItem exampleToggle  = new CheckMenuItem("Example Mode"); // controls ONLY the prompt text
        exampleToggle.selectedProperty().bindBidirectional(exampleMode);
        viewMenu.getItems().addAll(darkModeToggle, new SeparatorMenuItem(), exampleToggle);

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About…");
        about.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "JavaFX Demo\n© 2025").showAndWait());
        helpMenu.getItems().add(about);

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu, helpMenu);

        // ----- Tabs -----
        TabPane tabs = new TabPane(
                buildParameterTab(),
                buildDetectorTab(),
                buildFiltersTab(),
                new Tab("Dashboard", padded(new Label("Dashboard Content"))),
                new Tab("Form",  padded(new Label("Form Content"))),
                new Tab("Table",   padded(new Label("Table Content")))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ----- Root + Scene -----
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(tabs);

        Scene scene = new Scene(root, 1200, 800);

        // Dark mode CSS (inline via data: URI)
        String darkCss = """
                .root { -fx-base:#2b2b2b; -fx-background:#2b2b2b; -fx-control-inner-background:#3c3f41; -fx-text-fill:white; }
                .label, .menu, .menu-item, .tab-label { -fx-text-fill:white; }
                """;
        darkModeToggle.selectedProperty().addListener((obs, o, on) -> {
            scene.getStylesheets().clear();
            if (on) scene.getStylesheets().add("data:text/css," + darkCss.replace("\n", "%0A").replace(" ", "%20"));
        });

        Runnable updateSigmaPrompt = () -> {
            boolean loveScott = "Love & Scott".equals(tubeModel.getValue());
            sigmaConst.setPromptText(loveScott ? "1.109" : "1.0314");
        };

// Example mode → alle Prompts setzen/entfernen
        exampleMode.addListener((obs, wasOn, isOn) -> {
            tubeMaterial.setPromptText(isOn ? "Rh"   : "");
            electronIncidentAngle.setPromptText(isOn ? "20"  : "");
            electronTakeoffAngle.setPromptText(isOn ? "70"  : "");
            charZuCont.setPromptText(isOn ? "1"     : "");
            charZuContL.setPromptText(isOn ? "1"    : "");
            windowMaterial.setPromptText(isOn ? "Be"    : "");
            windowMaterialThickness.setPromptText(isOn ? "125"  : "");
            tubeCurrent.setPromptText(isOn ? "1"     : "");
            xRayTubeVoltage.setPromptText(isOn ? "35"    : "");
            energieStep.setPromptText(isOn ? "0.01" : "");
            measurementTime.setPromptText(isOn ? "30"   : "");


            // Detector
            windowMaterialDet.setPromptText(isOn ? "Be"  : "");
            thicknessWindowDet.setPromptText(isOn ? "7.62" : "");
            contactlayerDet.setPromptText(isOn ? "Au" : "");
            contactlayerThicknessDet.setPromptText(isOn ? "50" : "");
            detectorMaterial.setPromptText(isOn ? "Si" : "");
            inactiveLayer.setPromptText(isOn ? "0.05" : "");
            activeLayer.setPromptText(isOn ? "3" : "");

            if (isOn) updateSigmaPrompt.run(); // Sigma je nach Modell
            else      sigmaConst.setPromptText("");
        });

// Wenn während aktivem Example-Mode das Modell gewechselt wird → Sigma sofort anpassen
        tubeModel.valueProperty().addListener((o, oldV, newV) -> {
            if (exampleMode.get()) updateSigmaPrompt.run();
        });

// Falls Example-Mode beim Start schon true ist (oder du den Default sofort willst)
        if (exampleMode.get()) updateSigmaPrompt.run();



        stage.setTitle("JavaFX – Parameter Tab with Prompt Toggle + Plot Button");
        stage.setScene(scene);
        stage.show();
    }


    private Tab buildParameterTab() {

        // --- TubeModel (ComboBox) ---
        Label geometryLabel = new Label("Tube Model:");
        tubeModel = new ComboBox<>();
        tubeModel.getItems().addAll("Wiederschwinger", "Love & Scott");
        tubeModel.setValue("Wiederschwinger");
        HBox TubeModelBox = new HBox(10, geometryLabel, tubeModel);

        int breite = 4;
        // --- deine Labels + Textfelder (gekürzt) ---
        Label TubeMaterialLabel = new Label("X-ray Tube Material:");
        tubeMaterial = new TextField(); tubeMaterial.setPrefColumnCount(breite);
        Label ElectronIncidentAngleLabel = new Label("α: Electron Incident Angle [°]");
        electronIncidentAngle = new TextField(); electronIncidentAngle.setPrefColumnCount(breite);
        Label lblElectronTakeoffAngle = new Label("Electron Takeoff Angle [°]:");
        electronTakeoffAngle = new TextField(); electronTakeoffAngle.setPrefColumnCount(breite);
        Label lblCharZuCont = new Label("char→cont ratio:");
        charZuCont = new TextField(); charZuCont.setPrefColumnCount(breite);
        Label lblCharZuContL = new Label("char→cont ratio (L):");
        charZuContL = new TextField(); charZuContL.setPrefColumnCount(breite);
        Label lblWindowMaterial = new Label("Window Material:");
        windowMaterial = new TextField(); windowMaterial.setPrefColumnCount(breite);
        Label lblWindowMaterialThickness = new Label("Window Thickness [µm]:");
        windowMaterialThickness = new TextField(); windowMaterialThickness.setPrefColumnCount(breite);
        Label lblTubeCurrent = new Label("Tube Current [mA]:");
        tubeCurrent = new TextField(); tubeCurrent.setPrefColumnCount(breite);
        Label lblXRayTubeVoltage = new Label("X-Ray Tube Voltage [kV]:");
        xRayTubeVoltage = new TextField(); xRayTubeVoltage.setPrefColumnCount(breite);
        Label lblSigmaConst = new Label("σ const:");
        sigmaConst = new TextField(); sigmaConst.setPrefColumnCount(breite);
        Label lblEnergieStep = new Label("Energy Step [keV]:");
        energieStep = new TextField(); energieStep.setPrefColumnCount(breite);
        Label lblMeasurementTime = new Label("Measurement Time [s]:");
        measurementTime = new TextField(); measurementTime.setPrefColumnCount(breite);

        Tooltip ttValues = new Tooltip("Just a parameter to manipulate the spectrum.\n" +
                "For default value leave the box empty or take the value from View→Example Mode");
        ttValues.setShowDelay(javafx.util.Duration.millis(300));
        lblCharZuCont.setTooltip(ttValues);
        lblCharZuContL.setTooltip(ttValues);
        lblSigmaConst.setTooltip(ttValues);







        // Grid links
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int r = 0;
        grid.addRow(r++, TubeMaterialLabel, tubeMaterial);
        grid.addRow(r++, ElectronIncidentAngleLabel, electronIncidentAngle);
        grid.addRow(r++, lblElectronTakeoffAngle, electronTakeoffAngle);
        grid.addRow(r++, lblWindowMaterial, windowMaterial);
        grid.addRow(r++, lblWindowMaterialThickness, windowMaterialThickness);
        grid.addRow(r++, lblTubeCurrent, tubeCurrent);
        grid.addRow(r++, lblXRayTubeVoltage, xRayTubeVoltage);
        grid.addRow(r++, lblEnergieStep, energieStep);
        grid.addRow(r++, lblMeasurementTime, measurementTime);
        grid.addRow(r++, lblCharZuCont, charZuCont);
        grid.addRow(r++, lblCharZuContL, charZuContL);
        grid.addRow(r++, lblSigmaConst, sigmaConst);



// --- Bild rechts neben dem Grid ---
        previewImage = new ImageView();
// lokale Datei:
        previewImage.setImage(new Image(new java.io.File(
                "C:\\Users\\julia\\OneDrive\\Dokumente\\A_Christian\\Masterarbeit\\Präsentation\\czuc0.8.png"
        ).toURI().toString()));
// ODER aus Ressourcen:
// var url = getClass().getResource("/img/czuc0.8.png");
// if (url != null) previewImage.setImage(new Image(url.toExternalForm()));

        //previewImage.setPreserveRatio(true);
        previewImage.setFitWidth(800); // Breite rechts; anpassen wie du willst
        previewImage.setFitHeight(500);

// Optional: Grid darf breit werden, Bild bleibt rechts
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

// Zeile mit Formular links und Bild rechts
        formWithImage = new HBox(20, grid, spacer, previewImage);
        formWithImage.setPadding(new Insets(0, 0, 0, 0)); // optional



        // Eingabebox (links)
        inputBox = new VBox(10,
                new Label("Input"),
                TubeModelBox,
                formWithImage,
                new Separator(),
                buildFunctionButtons(),
                buildAccumulateBox(),   // ← NEU: eigene Zeile
                buildLogBox()
        );
        inputBox.setPadding(new Insets(20,10,10,40));    //(top, right, bottom, left)

        // Rechter Plot-Bereich (zunächst leer, mit Header + Close)
        plotPane = createPlotPaneWithClose();

        // Root für den Tab: START → nur inputBox (keine Teilung)
        parametersRoot = new BorderPane();
        parametersRoot.setCenter(inputBox);

        // Tab liefern
        return new Tab("Tube", parametersRoot);
    }

    // Erstellt den Plot-Container mit Header (Titel + „×“ Close)
    private BorderPane createPlotPaneWithClose() {
        BorderPane bp = new BorderPane();

        Label title = new Label("Plot");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("×");
        btnClose.setOnAction(e -> hidePlot());

        HBox header = new HBox(8, title, spacer, btnClose);
        header.setPadding(new Insets(6, 8, 6, 8));
        header.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border;");

        bp.setTop(header);
        return bp;
    }

    private HBox buildFunctionButtons() {
        Button btnSin = new Button("Plot sin");
        btnSin.setOnAction(e -> showPlot("sin"));

        Button btnCos = new Button("Plot cos");
        btnCos.setOnAction(e -> showPlot("cos"));

        Button btnTan = new Button("Plot tan");
        btnTan.setOnAction(e -> showPlot("tan"));

        return new HBox(10, btnSin, btnCos, btnTan);
    }

    private HBox buildAccumulateBox() {
        chkAccumulate = new CheckBox("Accumulate");
        chkAccumulate.setSelected(true); // Start: ersetzen
        return new HBox(16, chkAccumulate);
    }

    private HBox buildLogBox() {
        chkLogX = new CheckBox("log X");
        chkLogY = new CheckBox("log Y");

        chkLogX.selectedProperty().addListener((o, ov, nv) -> replotAllSeries());
        chkLogY.selectedProperty().addListener((o, ov, nv) -> replotAllSeries());

        return new HBox(16, chkLogX, chkLogY);
    }


    private void showPlot(String which) {
        LineChart<Number, Number> chart;

        if (chkAccumulate != null && chkAccumulate.isSelected()
                && plotVisible && plotPane.getCenter() instanceof LineChart<?, ?> lc) {
            // Akkumulieren: vorhandenen Chart weiter nutzen
            chart = (LineChart<Number, Number>) lc;
        } else {
            // Ersetzen: neuen Chart bauen (Achsenlabel anhand Log)
            NumberAxis x = new NumberAxis(); x.setLabel(chkLogX != null && chkLogX.isSelected() ? "log₁₀(x)" : "x");
            NumberAxis y = new NumberAxis(); y.setLabel(chkLogY != null && chkLogY.isSelected() ? "log₁₀(y)" : "y");
            chart = new LineChart<>(x, y);
            chart.setCreateSymbols(false);
            chart.setAnimated(false);
            chart.setTitle("Functions");

            if (formWithImage.getChildren().contains(previewImage)) {
                formWithImage.getChildren().remove(previewImage);
            }


            plotPane.setCenter(chart);


            if (!plotVisible) {
                split = new SplitPane(inputBox, plotPane);
                split.setDividerPositions(0.25);
                parametersRoot.setCenter(split);
                plotVisible = true;
            } else {
                chart.getData().clear(); // beim Ersetzen alten Inhalt leeren
            }
        }

        chart.getData().add(buildSeries(which));
    }

    // ========== Alle vorhandenen Serien mit aktueller Log-Einstellung neu aufbauen ==========
    private void replotAllSeries() {
        if (!plotVisible) return;
        if (!(plotPane.getCenter() instanceof LineChart<?, ?> lc)) return;

        @SuppressWarnings("unchecked")
        LineChart<Number, Number> chart = (LineChart<Number, Number>) lc;

        // Achsenlabels anpassen
        if (chart.getXAxis() instanceof NumberAxis x && chart.getYAxis() instanceof NumberAxis y) {
            x.setLabel(chkLogX.isSelected() ? "log₁₀(x)" : "x");
            y.setLabel(chkLogY.isSelected() ? "log₁₀(y)" : "y");
        }

        var names = chart.getData().stream().map(XYChart.Series::getName).toList();
        chart.getData().clear();
        for (String name : names) {
            chart.getData().add(buildSeries(name));
        }
    }

    // ========== Serie generieren (berücksichtigt log X/Y) ==========
    private XYChart.Series<Number, Number> buildSeries(String which) {
        boolean logX = chkLogX.isSelected();
        boolean logY = chkLogY.isSelected();

        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(which);

        for (int i = 0; i <= 600; i++) {
            double xv = i / 10.0; // >= 0
            double yv;
            switch (which) {
                case "cos" -> yv = 2.0 + Math.cos(xv);       // > 0
                case "tan" -> yv = 2.0 + Math.tan(xv) / 3.0; // kann <0 werden; Offset hält meist >0
                default    -> yv = 2.0 + Math.sin(xv);       // > 0
            }

            if (logX && xv <= 0) continue;  // log10(x) undefiniert für x<=0
            if (logY && yv <= 0) continue;  // log10(y) undefiniert für y<=0

            double px = logX ? Math.log10(xv) : xv;
            double py = logY ? Math.log10(yv) : yv;

            s.getData().add(new XYChart.Data<>(px, py));
        }
        return s;
    }



    private void hidePlot() {
        if (!plotVisible) return;
        parametersRoot.setCenter(inputBox); // back to single-column layout
        plotVisible = false;
        // Bild wieder links anzeigen
        if (!formWithImage.getChildren().contains(previewImage)) {
            formWithImage.getChildren().add(previewImage);
        }

    }

    private BorderPane createPlotPaneWithClose(Runnable onClose) {
        BorderPane bp = new BorderPane();

        Label title = new Label("Plot");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("×");
        btnClose.setOnAction(e -> onClose.run());

        HBox header = new HBox(8, title, spacer, btnClose);
        header.setPadding(new Insets(6, 8, 6, 8));
        header.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border;");

        bp.setTop(header);
        return bp;
    }



    private Tab buildDetectorTab() {
        // --- Local state for this tab ---
        final CheckBox chkAccumulate2 = new CheckBox("Accumulate");
        chkAccumulate2.setSelected(true);

        final boolean[] plotVisible2 = { false };
        final SplitPane[] split2 = new SplitPane[1];
        final BorderPane parametersRoot2 = new BorderPane();

        int breite = 5;

// --- Detector fields ---
        Label lblWindowMaterialDet = new Label("Window Material:");
        windowMaterialDet = new TextField();
        windowMaterialDet.setPrefColumnCount(breite);

        Label lblThicknessWindowDet = new Label("Window Thickness [µm]:");
        thicknessWindowDet = new TextField();
        thicknessWindowDet.setPrefColumnCount(breite);

        Label lblContactLayerDet = new Label("Contact Layer Material:");
        contactlayerDet = new TextField();
        contactlayerDet.setPrefColumnCount(breite);

        Label lblContactLayerThicknessDet = new Label("Contact Layer Thickness [nm]:");
        contactlayerThicknessDet = new TextField();
        contactlayerThicknessDet.setPrefColumnCount(breite);

        Label lblDetectorMaterial = new Label("Detector Material:");
        detectorMaterial = new TextField();
        detectorMaterial.setPrefColumnCount(breite);

        Label lblInactiveLayer = new Label("Inactive Layer [µm]:");
        inactiveLayer = new TextField();
        inactiveLayer.setPrefColumnCount(breite);

        Label lblActiveLayer = new Label("Active Layer [mm]:");
        activeLayer = new TextField();
        activeLayer.setPrefColumnCount(breite);


// --- Detector Grid ---
        GridPane gridDet = new GridPane();
        gridDet.setHgap(10);
        gridDet.setVgap(10);

        int rDet = 0;
        gridDet.addRow(rDet++, lblWindowMaterialDet, windowMaterialDet);
        gridDet.addRow(rDet++, lblThicknessWindowDet, thicknessWindowDet);
        gridDet.addRow(rDet++, lblContactLayerDet, contactlayerDet);
        gridDet.addRow(rDet++, lblContactLayerThicknessDet, contactlayerThicknessDet);
        gridDet.addRow(rDet++, lblDetectorMaterial, detectorMaterial);
        gridDet.addRow(rDet++, lblInactiveLayer, inactiveLayer);
        gridDet.addRow(rDet++, lblActiveLayer, activeLayer);


        // --- Image on the right of the form ---
        ImageView previewImage2 = new ImageView();
        try {
            previewImage2.setImage(new Image(new java.io.File(
                    "C:\\\\Users\\\\julia\\\\OneDrive\\\\Dokumente\\\\A_Christian\\\\Masterarbeit\\\\Masterarbeit\\\\Massenstreukoeffizienten.png"
            ).toURI().toString()));
        } catch (Exception ignore) {}
        previewImage2.setPreserveRatio(true);
        previewImage2.setFitWidth(600);
        previewImage2.setFitHeight(400);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        final HBox formWithImage2 = new HBox(20, gridDet, spacer2, previewImage2);



        // --- Buttons (no log controls here) ---
        Button btnSin2 = new Button("Plot sin");
        Button btnCos2 = new Button("Plot cos");
        Button btnTan2 = new Button("Plot tan");
        Button btnEff = new Button("Plot Effizienz");

        final VBox inputBox2 = new VBox(10,
                new Label("Detector Input"),
                formWithImage2,
                new Separator(),
                new HBox(10, btnSin2, btnCos2, btnTan2, btnEff),
                new HBox(16, chkAccumulate2)  // only accumulate, no log boxes
        );
        inputBox2.setPadding(new Insets(20,10,10,40));

        // --- Right plot pane with its own close ---
        final BorderPane plotPane2 = createPlotPaneWithClose(() -> {
            if (!plotVisible2[0]) return;
            parametersRoot2.setCenter(inputBox2); // back to single-column
            plotVisible2[0] = false;
            // restore the image
            if (!formWithImage2.getChildren().contains(previewImage2)) {
                formWithImage2.getChildren().add(previewImage2);
            }
        });

        // Start: single-column layout (no split yet)
        parametersRoot2.setCenter(inputBox2);

        // --- Plot builder (no log scaling) ---
        java.util.function.Consumer<String> showPlot2 = which -> {
            LineChart<Number, Number> chart;

            if (chkAccumulate2.isSelected() && plotVisible2[0] && plotPane2.getCenter() instanceof LineChart<?, ?> lc) {
                chart = (LineChart<Number, Number>) lc;
            } else {
                NumberAxis x = new NumberAxis(); x.setLabel("x");
                NumberAxis y = new NumberAxis(); y.setLabel("y");
                chart = new LineChart<>(x, y);
                chart.setCreateSymbols(false);
                chart.setAnimated(false);
                chart.setTitle("Functions");

                // remove image from the left row
                if (formWithImage2.getChildren().contains(previewImage2)) {
                    formWithImage2.getChildren().remove(previewImage2);
                }

                plotPane2.setCenter(chart);

                if (!plotVisible2[0]) {
                    split2[0] = new SplitPane(inputBox2, plotPane2);
                    split2[0].setDividerPositions(0.30);
                    parametersRoot2.setCenter(split2[0]);
                    plotVisible2[0] = true;
                } else {
                    chart.getData().clear();
                }
            }

            chart.getData().add(buildSeriesNoLog(which));
        };

        // Button handlers
        btnSin2.setOnAction(e -> showPlot2.accept("sin"));
        btnCos2.setOnAction(e -> showPlot2.accept("cos"));
        btnTan2.setOnAction(e -> showPlot2.accept("tan"));

        btnEff.setOnAction(e -> {
            LineChart<Number, Number> chart;
            if (chkAccumulate2.isSelected() && plotVisible2[0] && plotPane2.getCenter() instanceof LineChart<?, ?> lc) {
                chart = (LineChart<Number, Number>) lc;
            } else {
                NumberAxis x = new NumberAxis(); x.setLabel("Energie [keV]");
                NumberAxis y = new NumberAxis(); y.setLabel("Detektor-Effizienz");
                chart = new LineChart<>(x, y);
                chart.setCreateSymbols(false);
                chart.setAnimated(false);
                chart.setTitle("Detektor-Effizienz");

                if (formWithImage2.getChildren().contains(previewImage2)) {
                    formWithImage2.getChildren().remove(previewImage2);
                }
                plotPane2.setCenter(chart);

                if (!plotVisible2[0]) {
                    split2[0] = new SplitPane(inputBox2, plotPane2);
                    split2[0].setDividerPositions(0.30);
                    parametersRoot2.setCenter(split2[0]);
                    plotVisible2[0] = true;
                } else {
                    chart.getData().clear();
                }
            }

            // Detektor bauen & plotten:
            Detektor det = buildDetectorFromUI();
            double[] energies = det.getEnergieArray();
            double[] eff = det.getDetektorspektrumArray();

            XYChart.Series<Number, Number> s = new XYChart.Series<>();
            s.setName("Effizienz");

            for (int i = 0; i < energies.length && i < eff.length; i++) {
                s.getData().add(new XYChart.Data<>(energies[i], eff[i]));
            }
            chart.getData().add(s);
        });


        return new Tab("Detector", parametersRoot2);
    }

    private XYChart.Series<Number, Number> buildSeriesNoLog(String which) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(which);

        for (int i = 0; i <= 600; i++) {
            double x = i / 10.0;
            double y = switch (which) {
                case "cos" -> Math.cos(x);
                case "tan" -> Math.tan(x) / 3.0; // scaled to avoid blow-up
                default    -> Math.sin(x);
            };
            s.getData().add(new XYChart.Data<>(x, y));
        }
        return s;
    }




    private static double parseOrDefault(TextField tf, double def) {
        try {
            String s = tf.getText();
            if (s == null || s.isBlank()) return def;
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return def;
        }
    }

    private static String parseOrDefault(TextField tf, String def) {
        String s = tf.getText();
        if (s == null || s.isBlank()) return def;
        s = s.trim();

        // wenn Länge ≤ 2 → Format: 1. Buchstabe groß, Rest klein
        if (s.length() <= 2) {
            s = s.substring(0, 1).toUpperCase()
                    + (s.length() > 1 ? s.substring(1).toLowerCase() : "");
        }

        return s;
    }





    private Detektor buildDetectorFromUI() {
        // Detector-Tab Felder:
        String fenstermaterial = parseOrDefault(windowMaterialDet,"Be" );
        double fensterdicke_um = parseOrDefault(thicknessWindowDet, 7.62);   // µm
        String kontaktmaterial = parseOrDefault(contactlayerDet,"Au" );
        double kontaktdicke_nm = parseOrDefault(contactlayerThicknessDet, 50); // nm
        String detektormaterial = parseOrDefault(detectorMaterial,"Si" );
        double totschicht_um = parseOrDefault(inactiveLayer, 0.05);           // µm
        double activeLayer_mm = parseOrDefault(activeLayer, 3);               // mm

        // Tube-Tab Felder für Energieskala:
        double emax_kV = parseOrDefault(xRayTubeVoltage, 35);                 // kV ~ keV hier
        double step_keV = parseOrDefault(energieStep, 0.05);
        double emin_keV = 0; // deine Detektor-Klasse setzt 0 -> step selbst

        // Defaults, weil keine UI-Felder:
        double phi_deg = 0.0;
        double bedeckungsfaktor = 1.0;
        String datei = "MCMASTER.TXT";
        java.util.List<Verbindung> filter = new java.util.ArrayList<>();

        return new Detektor(
                fenstermaterial,
                fensterdicke_um,
                phi_deg,
                kontaktmaterial,
                kontaktdicke_nm,
                bedeckungsfaktor,
                detektormaterial,
                totschicht_um,
                activeLayer_mm,
                datei,
                emin_keV,
                emax_kV,
                step_keV,
                filter
        );
    }




    // Parser
    private static double parseDouble(TextField tf, double def) {
        try {
            String s = tf.getText();
            if (s == null || s.isBlank()) return def;
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception e) { return def; }
    }
    private static String parseFormula(TextField tf, String def) {
        String s = tf.getText();
        if (s == null || s.isBlank()) return def;
        s = s.trim();
        if (s.length() <= 2) s = s.substring(0,1).toUpperCase() + (s.length()>1 ? s.substring(1).toLowerCase() : "");
        return s;
    }

    // Globale Energieparameter aus dem Tube-Tab


    // Verbindung aus UI-Spezifikation bauen
    private Verbindung buildVerbindungFromSpec(String compound, double density, double thicknessCm
                                               ) {
        Funktionen fk = new FunktionenImpl();
        double useEmin = globalEmin;
        Verbindung parsed = fk.parseVerbindung(compound, useEmin, globalEmax(), globalStep(), DATA_FILE);
        Verbindung v = new Verbindung(parsed.getSymbole(), parsed.getKonzentrationen(),
                useEmin,  globalEmax(), globalStep(), DATA_FILE, density);
        v.setFensterDickeCm(thicknessCm);
        v.setModulationIdentitaet();
        return v;
    }


    // Zwei getrennte Filter-Listen
    private final ObservableList<Verbindung> tubeFilterVerbindungen = FXCollections.observableArrayList();
    private final ObservableList<Verbindung> detFilterVerbindungen  = FXCollections.observableArrayList();

    // Anzeige-Text (Formel/Name) je Verbindung
    private final java.util.Map<Verbindung, String> tubeFormulaText = new java.util.IdentityHashMap<>();
    private final java.util.Map<Verbindung, String> detFormulaText  = new java.util.IdentityHashMap<>();




    private VBox buildFilterColumn(String title,
                                   ObservableList<Verbindung> listData,
                                   java.util.Map<Verbindung,String> textMap) {

        // Startwerte (nur beim ersten Aufruf)
        if (listData.isEmpty()) {
            Verbindung v1 = buildVerbindungFromSpec("Al", 2.70, 50);
            Verbindung v2 = buildVerbindungFromSpec("Rh", 12.41, 25);
            listData.addAll(v1, v2);
            textMap.put(v1, "Al");
            textMap.put(v2, "Rh");
        }

        ListView<Verbindung> list = new ListView<>(listData);
        list.setCellFactory(lv -> new ListCell<>() {
            private final TextField tfComp = new TextField();
            private final TextField tfRho  = new TextField();
            private final TextField tfTh   = new TextField();
            private final Button btnDel    = new Button("Delete");
            private final VBox card;

            {

                tfComp.setPrefColumnCount(14);
                tfRho.setPrefColumnCount(6);
                tfTh.setPrefColumnCount(6);

                Label lMat = new Label("Material:");
                //lMat.setTooltip(new Tooltip("Chemische Formel/Bezeichnung, z. B. Al, Be oder C22H10N2O5."));

                Label lRho = new Label("ρ [g/cm³]:");
                Tooltip ttRho = new Tooltip("Density of the material in g/cm³.");
                ttRho.setShowDelay(javafx.util.Duration.millis(300));
                lRho.setTooltip(ttRho);

                Label lD = new Label("d [cm]:");
                Tooltip ttD = new Tooltip("Thickness in cm (0.005 cm = 50 µm)");
                ttD.setShowDelay(javafx.util.Duration.millis(300));
                ttD.setWrapText(true);
                ttD.setMaxWidth(260);
                lD.setTooltip(ttD);


                HBox row1 = new HBox(10, lMat, tfComp);
                HBox row2 = new HBox(10, lRho, tfRho, lD, tfTh, btnDel);
                row1.setAlignment(Pos.CENTER_LEFT);
                row2.setAlignment(Pos.CENTER_LEFT);

                card = new VBox(6, row1, row2);
                card.setPadding(new Insets(10));
                card.setStyle("""
    -fx-background-color: -fx-control-inner-background;
    -fx-background-radius: 10;
    -fx-border-color: -fx-box-border;
    -fx-border-radius: 10;""");
            }

            @Override protected void updateItem(Verbindung v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }

                String compShown = textMap.getOrDefault(v, "");
                tfComp.setText(compShown);
                tfRho.setText(String.valueOf(v.getDichte()));
                tfTh.setText(String.valueOf(v.getFensterDickeCm()));

                // Material/Dichte ändern → Verbindung neu bauen & Item ersetzen
                Runnable rebuild = () -> {
                    int idx = getIndex();
                    if (idx < 0) return;
                    String comp = parseFormula(tfComp, compShown.isBlank() ? "Al" : compShown);
                    double rho  = parseDouble(tfRho,  v.getDichte());
                    double thUm = parseDouble(tfTh,   v.getFensterDickeCm());

                    Verbindung neu = buildVerbindungFromSpec(comp, rho, thUm);
                    listData.set(idx, neu);
                    textMap.remove(v);
                    textMap.put(neu, comp);
                };
                tfComp.setOnAction(e -> rebuild.run());
                tfRho.setOnAction(e -> rebuild.run());

                // Dicke kann live gesetzt werden (Setter vorhanden)
                tfTh.setOnAction(e -> {
                    double thUm = parseDouble(tfTh, v.getFensterDickeCm());
                    v.setFensterDickeCm(thUm );
                });

                btnDel.setOnAction(e -> listData.remove(v));
                setGraphic(card);
            }
        });

        // Spalten-Controls
        Button btnAdd = new Button("Add");
        btnAdd.setOnAction(e -> {
            Verbindung neu = buildVerbindungFromSpec("Al", 2.70, 50.0);
            listData.add(neu);
            textMap.put(neu, "Al");
            list.getSelectionModel().select(neu);
            list.scrollTo(neu);
        });

        Button btnUp = new Button("↑");
        btnUp.setOnAction(e -> {
            int i = list.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                Verbindung v = listData.remove(i);
                listData.add(i - 1, v);
                list.getSelectionModel().select(i - 1);
            }
        });

        Button btnDown = new Button("↓");
        btnDown.setOnAction(e -> {
            int i = list.getSelectionModel().getSelectedIndex();
            if (i >= 0 && i < listData.size() - 1) {
                Verbindung v = listData.remove(i);
                listData.add(i + 1, v);
                list.getSelectionModel().select(i + 1);
            }
        });

        HBox controls = new HBox(8, btnAdd, new Separator(), btnUp, btnDown);
        controls.setPadding(new Insets(6, 0, 6, 0));

        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 6 0;");

        VBox col = new VBox(8, header, list, controls);
        VBox.setVgrow(list, Priority.ALWAYS);
        col.setPadding(new Insets(12));
        return col;
    }



    private Tab buildFiltersTab() {
        // Linke Spalte: Röhrenfilter
        VBox tubeCol = buildFilterColumn("Röhrenfilter", tubeFilterVerbindungen, tubeFormulaText);
        // Rechte Spalte: Detektorfilter
        VBox detCol  = buildFilterColumn("Detektorfilter", detFilterVerbindungen, detFormulaText);

        GridPane grid = new GridPane();

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);           // ← links 45%
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);           //
        c2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(tubeCol, 0, 0);
        grid.add(detCol,  1, 0);
        grid.setHgap(12);
        grid.setVgap(0);

        VBox root = new VBox(grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        root.setPadding(new Insets(0));

        return new Tab("Filters", root);
    }





    private static VBox padded(javafx.scene.Node node) {
        VBox box = new VBox(node);
        box.setPadding(new Insets(12));
        return box;
    }

    public static void main(String[] args) { launch(args); }
}
