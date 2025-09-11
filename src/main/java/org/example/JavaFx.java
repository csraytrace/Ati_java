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

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import javafx.css.PseudoClass;

import javafx.util.converter.DoubleStringConverter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.util.Objects;

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
                buildFunctionFiltersTab()
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
        var url = Objects.requireNonNull(getClass().getResource("/dino.png"), "Icon nicht gefunden!");
        stage.getIcons().add(new Image(url.toExternalForm()));
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
        //s = s.trim();
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
        final java.util.Map<Verbindung, Boolean> useMap = new java.util.IdentityHashMap<>();

        // Startwerte (nur beim ersten Aufruf)
        if (listData.isEmpty()) {
            Verbindung v1 = buildVerbindungFromSpec("Al", 2.70, 50);
            Verbindung v2 = buildVerbindungFromSpec("Rh", 12.41, 25);
            listData.addAll(v1, v2);
            textMap.put(v1, "Al");
            textMap.put(v2, "Rh");
            useMap.put(v1, true);   // <— NEU
            useMap.put(v2, true);
        }

        ListView<Verbindung> list = new ListView<>(listData);
        list.setCellFactory(lv -> new ListCell<>() {
            // --- UI pro Karte ---
            private final CheckBox chkUse = new CheckBox("Use");   // <— NEU
            private final TextField tfComp = new TextField();
            private final TextField tfRho  = new TextField();
            private final TextField tfTh   = new TextField();
            private final Button btnDel    = new Button("Delete");
            private final VBox card;

            // Hält die aktuelle Verbindung dieser Zelle (damit Handler wissen, auf welches Item sie arbeiten)
            private final javafx.beans.property.ObjectProperty<Verbindung> itemRef =
                    new javafx.beans.property.SimpleObjectProperty<>();

            // Parser/Helper (einmal pro Zelle)
            private final Funktionen fk = new FunktionenImpl();

            private void paintCard(boolean enabled) {
                String base = """
            -fx-background-radius: 10;
            -fx-border-color: -fx-box-border;
            -fx-border-radius: 10;""";
                String bg = enabled
                        ? "-fx-background-color: rgba(40,160,80,0.25);"   // grünlich
                        : "-fx-background-color: rgba(200,60,60,0.25);";  // rötlich
                card.setStyle(bg + base);
            }
            /*



            // Helper: Karte einfärben (grün = aktiv, rot = inaktiv)
            private void paintCard(boolean enabled) {
                String base = """
        -fx-background-insets: 0, 0;
        -fx-background-radius: 10, 10;
        -fx-border-color: -fx-box-border;
        -fx-border-radius: 10;
        """;

                String stripesEnabled = """
        -fx-background-color:
            linear-gradient(from 0% 0% to 100% 100%,
                rgba(60,180,95,0.35) 25%,
                transparent 25%,
                transparent 50%,
                rgba(60,180,95,0.35) 50%,
                rgba(60,180,95,0.35) 75%,
                transparent 75%),
            rgba(255,255,255,0.75);
        """;

                String stripesDisabled = """
        -fx-background-color:
            linear-gradient(from 0% 0% to 100% 100%,
                rgba(200,60,60,0.35) 25%,
                transparent 25%,
                transparent 50%,
                rgba(200,60,60,0.35) 50%,
                rgba(200,60,60,0.35) 75%,
                transparent 75%),
            rgba(255,255,255,0.80);
        """;

                card.setStyle((enabled ? stripesEnabled : stripesDisabled) + base);
            }
            */




            {
                // --------- UI bauen (einmalig) ----------
                Label lMat = new Label("Material:");
                Label lRho = new Label("ρ [g/cm³]:");
                Label lD   = new Label("d [cm]:");

                tfComp.setPrefColumnCount(14);
                tfRho.setPrefColumnCount(6);
                tfTh.setPrefColumnCount(6);

                // <— HIER Checkbox in die erste Zeile aufnehmen
                HBox row1 = new HBox(10, chkUse, lMat, tfComp);
                HBox row2 = new HBox(10, lRho, tfRho, lD, tfTh, btnDel);
                row1.setAlignment(Pos.CENTER_LEFT);
                row2.setAlignment(Pos.CENTER_LEFT);

                card = new VBox(6, row1, row2);
                card.setPadding(new Insets(10));
                // Anfangsfarbe neutral; wir setzen später per paintCard(...)
                card.setStyle("""
            -fx-background-radius: 10;
            -fx-border-color: -fx-box-border;
            -fx-border-radius: 10;""");

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                // --- Checkbox-Listener (nur einmal pro Zelle) ---
                chkUse.selectedProperty().addListener((obs, oldSel, sel) -> {
                    Verbindung v = itemRef.get();
                    if (v == null) return;
                    useMap.put(v, sel);   // Zustand merken
                    paintCard(sel);       // Karte einfärben
                });

                // --------- Handler NUR EINMAL registrieren ----------

                // ENTER im Material: neue Verbindung aus Name + Auto-Dichte (parsed.getDichte()), Dicke beibehalten
                tfComp.setOnAction(e -> {
                    Verbindung v = itemRef.get();
                    if (v == null) return;

                    String oldComp = textMap.getOrDefault(v, "");
                    String comp    = parseFormula(tfComp, oldComp.isBlank() ? "Al" : oldComp);
                    double thCm    = parseDouble(tfTh, v.getFensterDickeCm());
                    double useEmin = (globalEmin <= 0 ? globalStep() : globalEmin);

                    try {
                        Verbindung parsed = fk.parseVerbindung(comp, useEmin, globalEmax(), globalStep(), DATA_FILE);
                        double rhoAuto    = parsed.getDichte();

                        Verbindung neu = new Verbindung(
                                parsed.getSymbole(), parsed.getKonzentrationen(),
                                useEmin, globalEmax(), globalStep(), DATA_FILE,
                                rhoAuto
                        );
                        neu.setFensterDickeCm(thCm);
                        neu.setModulationIdentitaet();

                        // Validierung (vor dem Einsetzen)
                        if (neu.getKonzentrationen() == null || neu.getKonzentrationen().length == 0) {
                            throw new IllegalArgumentException("Keine Konzentrationen aus Formel \"" + comp + "\" ermittelt.");
                        }

                        int idx = getIndex();
                        if (idx >= 0) {
                            // bisherigen Use-Status übernehmen
                            boolean wasOn = useMap.getOrDefault(v, Boolean.TRUE);
                            useMap.put(neu, wasOn);
                            useMap.remove(v);

                            // zuerst Map aktualisieren, dann ersetzen
                            textMap.put(neu, comp);
                            textMap.remove(v);
                            getListView().getItems().set(idx, neu);

                            // Auto-Dichte in Feld schreiben
                            tfRho.setText(String.valueOf(rhoAuto));

                            // Karte entsprechend einfärben
                            paintCard(wasOn);
                        }
                        clearFieldError(tfComp);

                    } catch (IllegalArgumentException ex) {
                        showError(tfComp, "Ungültige Verbindung:\n" + comp + "\n\n" + ex.getMessage());
                        tfComp.setText(oldComp);
                        markFieldError(tfComp);
                    }
                });

                // Optional: bei Fokus-Verlust so behandeln wie ENTER
                tfComp.focusedProperty().addListener((o, was, is) -> {
                    if (!is) tfComp.fireEvent(new javafx.event.ActionEvent());
                });

                // ENTER in ρ: neue Verbindung mit exakt dieser Dichte bauen (Formel + Dicke beibehalten)
                tfRho.setOnAction(e -> {
                    Verbindung v = itemRef.get();
                    if (v == null) return;

                    String comp = parseFormula(tfComp, textMap.getOrDefault(v, "Al"));
                    double thCm = parseDouble(tfTh,  v.getFensterDickeCm());
                    double rho  = parseDouble(tfRho, v.getDichte());   // Eingabe oder Fallback
                    double useEmin = (globalEmin <= 0 ? globalStep() : globalEmin);

                    try {
                        Verbindung parsed = fk.parseVerbindung(comp, useEmin, globalEmax(), globalStep(), DATA_FILE);
                        Verbindung neu = new Verbindung(
                                parsed.getSymbole(), parsed.getKonzentrationen(),
                                useEmin, globalEmax(), globalStep(), DATA_FILE,
                                rho
                        );
                        neu.setFensterDickeCm(thCm);
                        neu.setModulationIdentitaet();

                        // Validierung (vor dem Einsetzen)
                        if (neu.getKonzentrationen() == null || neu.getKonzentrationen().length == 0) {
                            throw new IllegalArgumentException("Keine Konzentrationen.");
                        }

                        int idx = getIndex();
                        if (idx >= 0) {
                            boolean wasOn = useMap.getOrDefault(v, Boolean.TRUE);
                            useMap.put(neu, wasOn);
                            useMap.remove(v);

                            textMap.put(neu, comp);
                            textMap.remove(v);
                            getListView().getItems().set(idx, neu);

                            paintCard(wasOn);
                        }
                        clearFieldError(tfRho);

                    } catch (IllegalArgumentException ex) {
                        showError(tfRho, "Fehlerhafte Formel/Dichte:\n" + ex.getMessage());
                        markFieldError(tfRho);
                    }
                });

                // ENTER in d: Dicke direkt am bestehenden Objekt setzen (kein Neuaufbau)
                tfTh.setOnAction(e -> {
                    Verbindung v = itemRef.get();
                    if (v != null) {
                        double thCm = parseDouble(tfTh, v.getFensterDickeCm());
                        v.setFensterDickeCm(thCm);
                    }
                });

                // Delete
                btnDel.setOnAction(e -> {
                    Verbindung v = itemRef.get();
                    if (v != null) {
                        getListView().getItems().remove(v);
                        textMap.remove(v);
                        useMap.remove(v);    // <— NEU: Status entsorgen
                    }
                });
            }

            @Override protected void updateItem(Verbindung v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    itemRef.set(null);
                    setGraphic(null);
                    return;
                }
                itemRef.set(v);

                String compShown = textMap.getOrDefault(v, "");
                tfComp.setText(compShown);
                tfRho.setText(String.valueOf(v.getDichte()));
                tfTh.setText(String.valueOf(v.getFensterDickeCm()));

                // Use-Status herstellen + Farbe setzen
                boolean use = useMap.getOrDefault(v, Boolean.TRUE);
                chkUse.setSelected(use);
                paintCard(use);

                setGraphic(card);
            }
        });



        // Spalten-Controls
        Button btnAdd = new Button("Add");
        btnAdd.setOnAction(e -> {
            Verbindung neu = buildVerbindungFromSpec("Al", 2.70, 0.0);
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
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
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








    // --- Fehler-Helfer ---
    private static final PseudoClass PC_ERROR = PseudoClass.getPseudoClass("error");

    /** Zeigt einen Error-Dialog (modal zum Fenster, in dem das Feld steht). */
    private static void showError(Node owner, String message) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle("Fehler");
        a.setHeaderText("Formel kann nicht geparst werden");
        a.setContentText(message);

        Window w = owner != null && owner.getScene() != null ? owner.getScene().getWindow() : null;
        if (w != null) a.initOwner(w);
        a.showAndWait();
    }

    /** Markiert ein Feld visuell als fehlerhaft (per Pseudo-Class). */
    private static void markFieldError(Node node) {
        if (node != null) node.pseudoClassStateChanged(PC_ERROR, true);
    }

    /** Entfernt die Fehler-Markierung. */
    private static void clearFieldError(Node node) {
        if (node != null) node.pseudoClassStateChanged(PC_ERROR, false);
    }


    // --- Function filters (GUI) ---
    private final javafx.collections.ObservableList<PwSegment> tubeFuncSegs = FXCollections.observableArrayList();
    private final javafx.collections.ObservableList<PwSegment> detFuncSegs  = FXCollections.observableArrayList();

    // Default-Werte (außerhalb aller Segmente)
    private final DoubleProperty tubeFuncDefault = new SimpleDoubleProperty(1.0); // neutral
    private final DoubleProperty detFuncDefault  = new SimpleDoubleProperty(1.0); // neutral


    private Tab buildFunctionFiltersTab() {
        // Segments & Defaults initialisieren (wie bisher)
        if (tubeFuncSegs.isEmpty()) tubeFuncSegs.add(constExprOne());
        if (detFuncSegs.isEmpty())  detFuncSegs.add(constExprOne());

        // linke/rechte Spalte (deine bestehende buildSegmentColumn-Version weiterverwenden!)
        VBox tubeCol = buildSegmentColumn(
                "Röhren: Funktions-Filter",
                tubeFuncSegs,
                tubeFuncDefault,
                () -> applySegmentsToVerbindungen(tubeFilterVerbindungen, tubeFuncSegs, tubeFuncDefault.get())
        );

        VBox detCol  = buildSegmentColumn(
                "Detektor: Funktions-Filter",
                detFuncSegs,
                detFuncDefault,
                () -> applySegmentsToVerbindungen(detFilterVerbindungen, detFuncSegs, detFuncDefault.get())
        );

        // Grid wie gehabt
        GridPane grid = new GridPane();
        var c1 = new ColumnConstraints(); c1.setPercentWidth(50); c1.setHgrow(Priority.ALWAYS);
        var c2 = new ColumnConstraints(); c2.setPercentWidth(50); c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(tubeCol, 0, 0);
        grid.add(detCol,  1, 0);
        grid.setHgap(12);

        // Untere Tab-Buttons: "Show all"
        Button btnShowAllTube = new Button("Show all (Tube)");
        Button btnShowAllDet  = new Button("Show all (Detector)");
        HBox globalControls = new HBox(10, btnShowAllTube, btnShowAllDet);
        globalControls.setAlignment(Pos.CENTER);
        globalControls.setPadding(new Insets(6, 0, 6, 0));

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);


        VBox content = new VBox(10, grid, globalControls, vSpacer);

        //VBox content = new VBox(grid, globalControls);
        VBox.setVgrow(grid, Priority.ALWAYS);
        content.setPadding(new Insets(0));

        // === Full-screen Overlay auf Tab-Ebene =================================
        StackPane stack = new StackPane(content);

        // Overlay: BorderPane mit Header (Titel + Close) und großem Chart
        BorderPane overlay = new BorderPane();
        overlay.setStyle("""
        -fx-background-color: rgba(0,0,0,0.35);
    """);
        overlay.setVisible(false);
        overlay.setManaged(false);

        // Innenrahmen für Karte
        BorderPane card = new BorderPane();
        card.setStyle("""
        -fx-background-color: -fx-control-inner-background;
        -fx-background-radius: 12;
        -fx-border-color: -fx-box-border;
        -fx-border-radius: 12;
    """);
        card.setPadding(new Insets(10));
        overlay.setCenter(new StackPane(card) {{ setPadding(new Insets(12)); }});

        // Header
        Label title = new Label("Function Filters – Combined");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("×");
        btnClose.setOnAction(e -> { overlay.setVisible(false); overlay.setManaged(false); });
        HBox header = new HBox(8, title, spacer, btnClose);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 8, 6, 8));
        card.setTop(header);

        // Chart
        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Energy [keV]");
        NumberAxis yAxis = new NumberAxis(); yAxis.setLabel("h(x)");
        LineChart<Number,Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        card.setCenter(chart);

        stack.getChildren().add(overlay);

        // Plot-Helfer: alle Segmente einer Seite + Default über [globalEmin, globalEmax()]
        Runnable plotTube = () -> {
            plotCombined(chart, tubeFuncSegs, tubeFuncDefault.get(),
                    (globalEmin <= 0 ? globalStep() : globalEmin), globalEmax());
            title.setText(String.format(java.util.Locale.ROOT,
                    "Tube – default=%.6g   range=[%s, %s]",
                    tubeFuncDefault.get(), Double.toString((globalEmin <= 0 ? globalStep() : globalEmin)), Double.toString(globalEmax())));
            overlay.setManaged(true); overlay.setVisible(true);
        };
        Runnable plotDet = () -> {
            plotCombined(chart, detFuncSegs, detFuncDefault.get(),
                    (globalEmin <= 0 ? globalStep() : globalEmin), globalEmax());
            title.setText(String.format(java.util.Locale.ROOT,
                    "Detector – default=%.6g   range=[%s, %s]",
                    detFuncDefault.get(), Double.toString((globalEmin <= 0 ? globalStep() : globalEmin)), Double.toString(globalEmax())));
            overlay.setManaged(true); overlay.setVisible(true);
        };

        btnShowAllTube.setOnAction(e -> plotTube.run());
        btnShowAllDet.setOnAction(e -> plotDet.run());

        return new Tab("Function Filters", stack);
    }

    // neutrales Startsegment (expr ≡ 1 auf großem Bereich)
    private static PwSegment constExprOne() {
        PwSegment s = new PwSegment();
        s.enabled = true;
        s.type = SegmentType.EXPR;
        s.expr = "1";
        s.a = 0; s.b = 1e9; s.inclA = true; s.inclB = true;
        return s;
    }

    private void plotCombined(LineChart<Number,Number> chart,
                              java.util.List<PwSegment> segs,
                              double defaultValue,
                              double xMin, double xMax) {

        if (!(xMax > xMin)) return;

        // Parser mit Default aufbauen; die Segment-Expr kommen aus deiner PwSegment-Logik
        MathParser mp = MathParser.withDefault(defaultValue);
        for (PwSegment s : segs) {
            if (!s.enabled) continue;
            String expr = s.toExpressionString(null); // enthält Clamp, falls gesetzt
            mp = mp.addExprSegment(expr, s.a, s.inclA, s.b, s.inclB);
        }

        // Chart füllen
        chart.getData().clear();
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        double eMin = (globalEmin <= 0 ? globalStep() : globalEmin); // dein Konventionswert
        double eMax = globalEmax();
        double h    = globalStep();
        if (h <= 0) throw new IllegalArgumentException("step must be > 0");

// Anzahl Intervalle
        int N = (int) Math.max(0, Math.floor((eMax - eMin) / h));
        for (int i = 0; i <= N; i++) {
            double x = xMin + (xMax - xMin) * i / N;
            double y = mp.evaluate(x);               // Default außerhalb; Segmente innen
            if (Double.isFinite(y)) series.getData().add(new XYChart.Data<>(x, y));
        }
        chart.getData().add(series);
    }




    private VBox buildSegmentColumn(String title,
                                    ObservableList<PwSegment> segs,
                                    DoubleProperty defaultProp,
                                    Runnable applyAll) {

        // --- Fester Default-Block (unlöschbar) ---
        Label lDef = new Label("Default outside segments:");
        TextField tfDefault = new TextField(Double.toString(defaultProp.get()));
        tfDefault.setPrefColumnCount(8);
        Runnable saveDefault = () -> {
            try {
                double v = Double.parseDouble(tfDefault.getText().trim().replace(',', '.'));
                defaultProp.set(v);
                applyAll.run();
            } catch (Exception ignore) {
                tfDefault.setText(Double.toString(defaultProp.get()));
            }
        };
        tfDefault.setOnAction(e -> saveDefault.run());
        tfDefault.focusedProperty().addListener((o, was, is) -> { if (!is) saveDefault.run(); });

        HBox defaultRow = new HBox(10, lDef, tfDefault, new Label("(used when no segment matches)"));
        defaultRow.setAlignment(Pos.CENTER_LEFT);
        defaultRow.setPadding(new Insets(10));
        defaultRow.setStyle("""
        -fx-background-color: -fx-control-inner-background;
        -fx-background-radius: 10;
        -fx-border-color: -fx-box-border;
        -fx-border-radius: 10;
    """);



        // --- ListView der Segmente (nur EXPR) ---
        ListView<PwSegment> list = new ListView<>(segs);
        list.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox chkUse = new CheckBox("Use");
            private final TextField tfA = new TextField(), tfB = new TextField();
            private final Button btnLeft  = new Button("[");  // [ oder (
            private final Button btnRight = new Button("]");  // ] oder )
            private final TextField tfExpr = new TextField();
            private final CheckBox cbClamp = new CheckBox("Clamp");
            private final TextField tfYmin = new TextField(), tfYmax = new TextField();

            private final Button btnShow = new Button("Show");
            private final Button btnDel  = new Button("Delete");

            // Mini-Plot-Bereich innerhalb der Karte (ein-/ausblendbar)
            private final BorderPane plotPane = new BorderPane();
            private final Label plotTitle = new Label("Plot");
            private final Button btnClosePlot = new Button("×");

            private LineChart<Number,Number> chart; // wird lazy erstellt

            private final VBox card = new VBox(8);
            private PwSegment item;

            private void paintCard(boolean enabled) {
                String base =
                        "-fx-background-radius: 10; " +
                                "-fx-border-color: -fx-box-border; " +
                                "-fx-border-radius: 10;";
                String bg = enabled
                        ? "-fx-background-color: rgba(40,160,80,0.18);"  // grün
                        : "-fx-background-color: rgba(200,60,60,0.18);"; // rot
                card.setStyle(bg + base);
            }


            {
                // Prompts & Größen
                tfA.setPromptText("from"); tfB.setPromptText("to");
                tfA.setPrefColumnCount(6); tfB.setPrefColumnCount(6);

                tfExpr.setPromptText("example: sin(x)+0.3*x");
                tfExpr.setPrefColumnCount(24);

                tfYmin.setPromptText("yMin"); tfYmax.setPromptText("yMax");
                tfYmin.setPrefColumnCount(6); tfYmax.setPrefColumnCount(6);

                // Klammer-Buttons
                btnLeft.setMinWidth(28);  btnLeft.setFocusTraversable(false);
                btnRight.setMinWidth(28); btnRight.setFocusTraversable(false);
                Tooltip.install(btnLeft,  new Tooltip("Left bound: [ inclusive, ( exclusive"));
                Tooltip.install(btnRight, new Tooltip("Right bound: ] inclusive, ) exclusive"));

                // Zeilen
                HBox row2    = new HBox(10, chkUse, new Label("Range:"), btnLeft, tfA, new Label(","), tfB, btnRight);
                HBox rowExpr = new HBox(10, new Label("expr:"), tfExpr);
                HBox rowClamp= new HBox(10, cbClamp, new Label("y ∈ ["), tfYmin, new Label(","), tfYmax, new Label("]"), btnShow, btnDel);

                row2.setAlignment(Pos.CENTER_LEFT);
                rowExpr.setAlignment(Pos.CENTER_LEFT);
                rowClamp.setAlignment(Pos.CENTER_LEFT);

                // Plot-Header mit Close-Button
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox plotHeader = new HBox(8, plotTitle, spacer, btnClosePlot);
                plotHeader.setPadding(new Insets(6, 8, 6, 8));
                plotHeader.setStyle("-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-box-border;");
                plotPane.setTop(plotHeader);

                // Plot anfangs verborgen
                plotPane.setVisible(false);
                plotPane.setManaged(false);

                // Karte zusammenbauen
                card.getChildren().addAll(row2, rowExpr, rowClamp, plotPane);
                card.setPadding(new Insets(10));
                card.setStyle("""
            -fx-background-color: -fx-control-inner-background;
            -fx-background-radius: 10;
            -fx-border-color: -fx-box-border;
            -fx-border-radius: 10;
        """);

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                // Helper: Double lesen (oder null)
                java.util.function.Function<TextField, Double> rd = tf -> {
                    try {
                        String s = tf.getText();
                        if (s == null || s.isBlank()) return null;
                        return Double.parseDouble(s.trim().replace(',', '.'));
                    } catch (Exception ex) { return null; }
                };

                // Änderungen → Model + applyAll
                Runnable pushToModel = () -> {
                    if (item == null) return;

                    item.enabled = chkUse.isSelected();
                    item.type = SegmentType.EXPR; // fix
                    item.expr = (tfExpr.getText() == null) ? "" : tfExpr.getText().trim();

                    var a = rd.apply(tfA);
                    var b = rd.apply(tfB);
                    if (a != null) item.a = a;
                    if (b != null) item.b = b;
                    item.inclA = "[".equals(btnLeft.getText());
                    item.inclB = "]".equals(btnRight.getText());

                    item.clamp = cbClamp.isSelected();
                    var ymin = rd.apply(tfYmin);
                    var ymax = rd.apply(tfYmax);
                    if (ymin != null) item.yMin = ymin;
                    if (ymax != null) item.yMax = ymax;

                    applyAll.run();
                };

                // Listener
                chkUse.selectedProperty().addListener((obs, was, ist) -> {
                    if (item == null) return;
                    item.enabled = ist;     // Model-Spiegel
                    paintCard(ist);         // Farbe sofort umschalten
                    applyAll.run();         // optional: sofort übernehmen
                });


                btnLeft.setOnAction(e -> { // [ <-> (
                    btnLeft.setText("[".equals(btnLeft.getText()) ? "(" : "[");
                    pushToModel.run();
                });
                btnRight.setOnAction(e -> { // ] <-> )
                    btnRight.setText("]".equals(btnRight.getText()) ? ")" : "]");
                    pushToModel.run();
                });

                cbClamp.setOnAction(e -> pushToModel.run());

                tfA.setOnAction(e -> pushToModel.run());
                tfB.setOnAction(e -> pushToModel.run());
                tfExpr.setOnAction(e -> pushToModel.run());
                tfExpr.focusedProperty().addListener((o, was, is) -> { if (!is) pushToModel.run(); });
                tfYmin.setOnAction(e -> pushToModel.run());
                tfYmax.setOnAction(e -> pushToModel.run());

                tfYmin.focusedProperty().addListener((o, was, is) -> { if (!is) pushToModel.run(); });
                tfYmax.focusedProperty().addListener((o, was, is) -> { if (!is) pushToModel.run(); });

// (optional auch für tfA/tfB)
                tfA.focusedProperty().addListener((o, was, is) -> { if (!is) pushToModel.run(); });
                tfB.focusedProperty().addListener((o, was, is) -> { if (!is) pushToModel.run(); });

                btnDel.setOnAction(e -> {
                    if (item != null) {
                        segs.remove(item);
                        applyAll.run();
                    }
                });

                // SHOW: Segment in eingebettetem Chart plotten
                btnShow.setOnAction(e -> {
                    if (item == null) return;
                    ensureChart();
                    plotCurrentSegment(item);
                    showPlot(true);
                });

                // Plot schließen
                btnClosePlot.setOnAction(e -> showPlot(false));
            }

            private void ensureChart() {
                if (chart != null) return;
                NumberAxis x = new NumberAxis(); x.setLabel("x");
                NumberAxis y = new NumberAxis(); y.setLabel("f(x)");
                chart = new LineChart<>(x, y);
                chart.setCreateSymbols(false);
                chart.setAnimated(false);
                chart.setLegendVisible(false);
                plotPane.setCenter(chart);
            }

            private void showPlot(boolean on) {
                plotPane.setVisible(on);
                plotPane.setManaged(on);
            }

            private void plotCurrentSegment(PwSegment s) {
                if (chart == null) return;
                chart.getData().clear();

                // Ausdruck inkl. optionalem Clamp generieren
                String expr = s.toExpressionString(null);

                // mXparser-Funktion bauen
                org.mariuszgromada.math.mxparser.Function f =
                        new org.mariuszgromada.math.mxparser.Function("f(x) = " + expr);
                if (!f.checkSyntax()) {
                    // Syntaxfehler => nichts plotten
                    return;
                }

                // Bereich beachten, Exklusivität per kleinem Epsilon
                double eps = 1e-9 * Math.max(1.0, Math.max(Math.abs(s.a), Math.abs(s.b)));
                double left  = s.inclA ? s.a : s.a + eps;
                double right = s.inclB ? s.b : s.b - eps;
                if (!(right > left)) return;

                XYChart.Series<Number,Number> series = new XYChart.Series<>();

                int N = 600;
                for (int i = 0; i <= N; i++) {
                    double x = left + (right - left) * i / N;
                    double y = f.calculate(x);
                    if (Double.isFinite(y)) {
                        series.getData().add(new XYChart.Data<>(x, y));
                    }
                }
                chart.getData().add(series);
                plotTitle.setText("Plot: " + expr);
            }

            @Override protected void updateItem(PwSegment s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    item = null;
                    return;
                }
                item = s;

                chkUse.setSelected(s.enabled);
                paintCard(s.enabled);

                tfA.setText(Double.toString(s.a));
                tfB.setText(Double.toString(s.b));
                btnLeft.setText(s.inclA ? "[" : "(");
                btnRight.setText(s.inclB ? "]" : ")");

                tfExpr.setText(s.expr == null ? "" : s.expr);

                cbClamp.setSelected(s.clamp);
                tfYmin.setText(s.clamp ? Double.toString(s.yMin) : "");
                tfYmax.setText(s.clamp ? Double.toString(s.yMax) : "");

                // Plot bei Zellenwechsel standardmäßig zu
                showPlot(false);

                setGraphic(card);
            }
        });



        // Spalten-Controls (Add / Up / Down)
        Button btnAdd = new Button("Add segment");
        btnAdd.setOnAction(e -> {
            PwSegment s = new PwSegment();
            s.enabled = true;
            s.type = SegmentType.EXPR;
            s.expr = "1";
            s.a = 0; s.b = globalEmax();
            s.inclA = true; s.inclB = true;
            segs.add(s);
            applyAll.run();
        });

        Button btnUp = new Button("↑");
        btnUp.setOnAction(e -> {
            int i = list.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                PwSegment s = segs.remove(i);
                segs.add(i - 1, s);
                list.getSelectionModel().select(i - 1);
                applyAll.run();
            }
        });

        Button btnDown = new Button("↓");
        btnDown.setOnAction(e -> {
            int i = list.getSelectionModel().getSelectedIndex();
            if (i >= 0 && i < segs.size() - 1) {
                PwSegment s = segs.remove(i);
                segs.add(i + 1, s);
                list.getSelectionModel().select(i + 1);
                applyAll.run();
            }
        });

        HBox controls = new HBox(8, btnAdd, new Separator(), btnUp, btnDown);
        controls.setPadding(new Insets(6, 0, 6, 0));

        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 6 0;");

        VBox col = new VBox(8, header, defaultRow, list, controls);
        VBox.setVgrow(list, Priority.ALWAYS);
        col.setPadding(new Insets(12));
        return col;
    }


    private void applySegmentsToVerbindungen(java.util.List<Verbindung> verbindungen,
                                             java.util.List<PwSegment> segs,
                                             double defaultValue) {
        for (Verbindung v : verbindungen) {
            // Default setzen (Wert außerhalb aller Segmente / bei Fehlern)
            v.clearModulation(defaultValue);

            for (PwSegment s : segs) {
                if (!s.enabled) continue;

                // Ausdruck aus Segment erzeugen
                String expr = (s.expr == null || s.expr.isBlank()) ? "1" : s.expr.trim();

                // Optional clamp
                if (s.clamp) {
                    expr = String.format(java.util.Locale.US,
                            "min(max((%s), %.17g), %.17g)", expr, s.yMin, s.yMax);
                }

                // Intervall hinzufügen
                v.addModulationSegment(expr, s.a, s.inclA, s.b, s.inclB);
            }
        }
    }













    public static void main(String[] args) { launch(args); }
}
