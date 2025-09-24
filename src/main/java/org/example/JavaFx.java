package org.example;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
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

import javafx.scene.shape.Circle;
import javafx.util.Duration;

// JSON (Jackson)
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.util.StringConverter;

// Preferences
import java.util.Set;
import java.util.prefs.Preferences;


import java.util.Objects;

public class JavaFx extends Application {

    // Model
    private final BooleanProperty exampleMode = new SimpleBooleanProperty(false);
    private double globalStep() { return parseDouble(energieStep, 0.01); }
    private double globalEmax() { return parseDouble(xRayTubeVoltage, 35.0); }
    private static double globalEmin = 0;
    private static String DATA_FILE = "MCMASTER.TXT";

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
                buildPropertiesTab(),
                buildParameterTab(),
                buildDetectorTab(),
                buildFiltersTab(),
                buildFunctionFiltersTab(),
                buildConcentrationsTab()
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
                buildAccumulateBox()
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

    private enum TubePlot { CONTINUOUS, CHARACTERISTIC, TOTAL, FILTERS }
    private TubePlot lastTubePlot = null;


    private VBox buildFunctionButtons() {
        Button btnTubeCont  = new Button("Plot Tube: Cont");
        Button btnTubeChar  = new Button("Plot Tube: Char");
        Button btnTubeTotal = new Button("Plot Tube: Total");
        Button btnTubeFilt  = new Button("Plot Tube Filters");

        // Tooltip für "Plot Tube Filters" – Log hat hier keine Wirkung
        Tooltip filterTip = new Tooltip(
                "No log scaling in the plot."
        );
        filterTip.setShowDelay(Duration.millis(200));
        filterTip.setHideDelay(Duration.millis(50));
        filterTip.setShowDuration(Duration.seconds(10));
        btnTubeFilt.setTooltip(filterTip); // oder: Tooltip.install(btnTubeFilt, filterTip);


        btnTubeCont.setOnAction(e -> plotTubeContinuous());
        btnTubeChar.setOnAction(e -> plotTubeCharacteristic());
        btnTubeTotal.setOnAction(e -> plotTubeTotal());
        btnTubeFilt.setOnAction(e -> plotTubeFilters());

        HBox row1 = new HBox(10, btnTubeCont, btnTubeChar);
        HBox row2 = new HBox(10, btnTubeTotal, btnTubeFilt);

        row1.setAlignment(Pos.CENTER_LEFT);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, row1, row2);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }


    // immer erst Edits committen
    private void commitTubeEdits() {
        commitAllTextFields(parametersRoot);
        parametersRoot.requestFocus();
    }

    private LineChart<Number,Number> ensureTubeChart(String title, String yLabel) {
        LineChart<Number,Number> chart;
        if (chkAccumulate != null && chkAccumulate.isSelected()
                && plotVisible && plotPane.getCenter() instanceof LineChart<?, ?> lc) {
            chart = (LineChart<Number, Number>) lc;
        } else {
            NumberAxis x = new NumberAxis(); x.setLabel(chkLogX!=null && chkLogX.isSelected() ? "log₁₀(E) [keV]" : "E [keV]");
            NumberAxis y = new NumberAxis(); y.setLabel(chkLogY!=null && chkLogY.isSelected() ? "log₁₀("+yLabel+")" : yLabel);
            chart = new LineChart<>(x, y);
            chart.setCreateSymbols(false);
            chart.setAnimated(false);
            chart.setTitle(title);

            if (formWithImage.getChildren().contains(previewImage)) {
                formWithImage.getChildren().remove(previewImage);
            }
            plotPane.setCenter(chart);
            if (!plotVisible) {
                split = new SplitPane(inputBox, plotPane);
                split.setDividerPositions(0.25);
                parametersRoot.setCenter(split);
                plotVisible = true;
            } else chart.getData().clear();
        }
        return chart;
    }


    private void addSeriesFromArrays(
            LineChart<Number,Number> chart,
            String name,
            double[] E, double[] Y
    ) {
        XYChart.Series<Number,Number> s = new XYChart.Series<>();
        s.setName(name);
        for (int i = 0; i < E.length && i < Y.length; i++) {
            double x = transformX(E[i]);
            double y = transformY(Y[i]);
            if (Double.isFinite(x) && Double.isFinite(y)) {
                s.getData().add(new XYChart.Data<>(x, y));
            }
        }
        chart.getData().add(s);
        chart.setCreateSymbols(false); // Linien, keine Marker
    }


    private double currentStep() {
        return parseDouble(energieStep, 0.05); // oder dein globalStep()
    }

    private boolean isLogX() { return chkLogX != null && chkLogX.isSelected(); }
    private boolean isLogY() { return chkLogY != null && chkLogY.isSelected(); }

    private double transformX(double e) {
        double step = currentStep();

        // Linear: E<step -> 0
        if (!isLogX()) {
            return (e < step) ? 0.0 : e;
        }

        // LogX: nur positive Werte; E<step wird ausgelassen
        if (e <= 0 || e < step) return Double.NaN; // -> wird in addSeries... übersprungen
        return Math.log10(e);
    }

    private double transformY(double v) {
        // Linear: y<1 -> 0
        if (!isLogY()) {
            return (v < 1.0) ? 0.0 : v;
        }

        // LogY: nur positive Werte; y<1 wird ausgelassen
        if (v <= 0 || v < 1.0) return Double.NaN; // -> wird in addSeries... übersprungen
        return Math.log10(v);
    }


    private void addSeriesFromTransitionsAsLine(
            LineChart<Number,Number> chart,
            String name,
            java.util.List<Übergang> transitions
    ) {
        java.util.List<Übergang> sorted = new java.util.ArrayList<>(transitions);
        sorted.sort(java.util.Comparator.comparingDouble(Übergang::getEnergy));

        XYChart.Series<Number,Number> s = new XYChart.Series<>();
        s.setName(name);

        for (Übergang u : sorted) {
            double x = transformX(u.getEnergy());
            double y = transformY(u.getRate());
            if (Double.isFinite(x) && Double.isFinite(y)) {
                s.getData().add(new XYChart.Data<>(x, y));
            }
        }

        chart.getData().add(s);
        chart.setCreateSymbols(false); // reine Linie
    }


    private void addCharacteristicAsSticks(
            LineChart<Number,Number> chart,
            java.util.List<Übergang> transitions
    ) {
        // nur Linien, keine Marker
        chart.setCreateSymbols(false);

        // Basishöhe: in LogY ist 0 -> log10(1) = 0; in Linear ist 0 einfach 0
        final double y0 = isLogY() ? transformY(1.0) : 0.0;

        for (Übergang u : transitions) {
            double x = transformX(u.getEnergy());
            double y = transformY(u.getRate());

            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(y0)) {
                continue;
            }

            XYChart.Series<Number,Number> s = new XYChart.Series<>();
            s.setName(null); // keine Seriennamenflut in der Legende
            s.getData().add(new XYChart.Data<>(x, y0)); // Basis
            s.getData().add(new XYChart.Data<>(x, y));  // Spitze
            chart.getData().add(s);
        }
    }






    private void plotTubeContinuous() {
        commitTubeEdits();
        lastTubePlot = TubePlot.CONTINUOUS;
        RöhreBasis tube = buildTubeFromUI();
        LineChart<Number,Number> chart = ensureTubeChart("Tube – Continuous", "Counts");
        addSeriesFromArrays(chart, "Continuous", tube.getEnergieArray(), tube.getContinuousSpectrum());
    }

    private void plotTubeCharacteristic() {
        commitTubeEdits();
        lastTubePlot = TubePlot.CHARACTERISTIC;
        RöhreBasis tube = buildTubeFromUI();
        LineChart<Number,Number> chart = ensureTubeChart("Tube – Characteristic", "Counts");
        addCharacteristicAsSticks(chart, tube.getCharacteristicSpectrum());
    }


    private void plotTubeTotal() {
        commitTubeEdits();
        lastTubePlot = TubePlot.TOTAL;
        RöhreBasis tube = buildTubeFromUI();
        LineChart<Number,Number> chart = ensureTubeChart("Tube – Total", "Counts");
        addSeriesFromArrays(chart, "Total", tube.getEnergieArray(), tube.getGesamtspektrum());
    }




    private void plotTubeFilters() {
        commitTubeEdits();
        lastTubePlot = TubePlot.FILTERS;
        LineChart<Number, Number> chart = ensureTubeChart("Tube Filters – Transmission", "T(E)");

// aktive Tube-Filter einsammeln …
        java.util.List<Verbindung> active = new java.util.ArrayList<>();
        for (Verbindung v : tubeFilterVerbindungen) {
            if (tubeFilterUse.getOrDefault(v, Boolean.TRUE)) active.add(v);
        }

// gibt es aktive Function-Filter?
        boolean haveFunc = tubeFuncSegs.stream().anyMatch(s -> s.enabled);
        boolean haveMat  = !active.isEmpty();

        if (!haveMat && !haveFunc) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "No active tube filter (neither material nor function filter).");
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

// Falls keine Material-Filter, aber Function-Filter vorhanden:
// Dummy-Filter anlegen, Modulation anwenden, und den plotten.
        if (!haveMat && haveFunc) {
            Verbindung dummy = buildVerbindungFromSpec("Al", 2.70, 0.0); // Dicke 0 → neutral
            active.add(dummy);
            tubeFormulaText.put(dummy, "Function filter");
        }

// Sicherheitshalber die aktuellen Function-Filter auch auf die aktiven Verbindungen anwenden
        if (haveFunc) {
            applySegmentsToVerbindungen(active, tubeFuncSegs, tubeFuncDefault.get());
        }

        // Produktkurve + einzelne Filter
        double[] total = null;
        for (Verbindung v : active) {
            double[] E = v.getEnergieArray();
            double[] T = v.erzeuge_Filter_liste(); // inkl. Modulation

            if (total == null) total = java.util.Arrays.copyOf(T, T.length);
            else for (int i = 0; i < total.length && i < T.length; i++) total[i] *= T[i];

            XYChart.Series<Number,Number> s = new XYChart.Series<>();
            s.setName(tubeFormulaText.getOrDefault(v, "Filter"));
            for (int i = 0; i < E.length && i < T.length; i++) {
                s.getData().add(new XYChart.Data<>(E[i], T[i]));
            }
            chart.getData().add(s);
        }

        if (total != null) {
            Verbindung v0 = active.get(0);
            double[] E = v0.getEnergieArray();
            XYChart.Series<Number,Number> sTot = new XYChart.Series<>();
            sTot.setName("Total (Produkt)");
            for (int i = 0; i < E.length && i < total.length; i++) {
                sTot.getData().add(new XYChart.Data<>(E[i], total[i]));
            }
            chart.getData().add(sTot);
        }
    }


    private HBox buildAccumulateBox() {
        chkAccumulate = new CheckBox("Accumulate");
        chkAccumulate.setSelected(true); // Start: ersetzen
        chkLogY = new CheckBox("log Y");
        chkLogY.selectedProperty().addListener((o, ov, nv) -> replotLastTube());
        return new HBox(16, chkAccumulate,chkLogY);
    }
/*
    private HBox buildLogBox() {
        chkLogX = new CheckBox("log X");
        chkLogY = new CheckBox("log Y");

        chkLogX.selectedProperty().addListener((o, ov, nv) -> replotLastTube());
        chkLogY.selectedProperty().addListener((o, ov, nv) -> replotLastTube());

        //return new HBox(16, chkLogX, chkLogY);
        return new HBox(16, chkLogY);
    }*/

    private void replotLastTube() {
        if (!plotVisible || lastTubePlot == null) return;
        // kompletten Chart neu aufbauen – gleiche Aktion wie vorher
        switch (lastTubePlot) {
            case CONTINUOUS -> plotTubeContinuous();
            case CHARACTERISTIC -> plotTubeCharacteristic();
            case TOTAL -> plotTubeTotal();
            case FILTERS -> plotTubeFilters();
        }
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

        Button btnEff = new Button("Plot Effizienz");
        Button btnFilt = new Button("Plot Filters");

        final VBox inputBox2 = new VBox(10,
                new Label("Detector Input"),
                formWithImage2,
                new Separator(),
                new HBox(10, btnEff, btnFilt),
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

        btnFilt.setOnAction(e -> {
            var sc = btnFilt.getScene();
            if (sc != null) {
                var fo = sc.getFocusOwner();
                if (fo instanceof TextField tf) {
                    tf.fireEvent(new javafx.event.ActionEvent()); // löst den setOnAction-Handler aus
                }
            }

            LineChart<Number, Number> chart;
            if (chkAccumulate2.isSelected() && plotVisible2[0] && plotPane2.getCenter() instanceof LineChart<?, ?> lc) {
                chart = (LineChart<Number, Number>) lc;
            } else {
                NumberAxis x = new NumberAxis(); x.setLabel("Energie [keV]");
                NumberAxis y = new NumberAxis(); y.setLabel("Transmission T(E)");
                chart = new LineChart<>(x, y);
                chart.setCreateSymbols(false);
                chart.setAnimated(false);
                chart.setTitle("Detektor-Filter: Transmission");

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

// Aktive Detektor-Filter sammeln …
            java.util.List<Verbindung> activeDetFilters = new java.util.ArrayList<>();
            for (Verbindung v : detFilterVerbindungen) {
                if (detFilterUse.getOrDefault(v, Boolean.TRUE)) {
                    activeDetFilters.add(v);
                }
            }

// Function-Filter vorhanden?
            boolean haveFuncDet = detFuncSegs.stream().anyMatch(s -> s.enabled);
            boolean haveMatDet  = !activeDetFilters.isEmpty();

            if (!haveMatDet && !haveFuncDet) {
                Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "No active detector filter (neither material nor function filter).");
                a.setHeaderText(null);
                a.showAndWait();
                return;
            }

// Keine Material-Filter, aber Function-Filter → Dummy anlegen
            if (!haveMatDet && haveFuncDet) {
                Verbindung dummy = buildVerbindungFromSpec("Al", 2.70, 0.0);
                activeDetFilters.add(dummy);
                detFormulaText.put(dummy, "Function filter");
            }

// Function-Filter anwenden (setzt Modulation auf den Verbindungen)
            if (haveFuncDet) {
                applySegmentsToVerbindungen(activeDetFilters, detFuncSegs, detFuncDefault.get());
            }


            // Plotten: jede Verbindung als Serie; zusätzlich Produktkurve
            double[] total = null;
            for (Verbindung v : activeDetFilters) {
                double[] E = v.getEnergieArray();
                double[] T = v.erzeuge_Filter_liste(); // enthält Modulation

                if (total == null) {
                    total = java.util.Arrays.copyOf(T, T.length);
                } else {
                    for (int i = 0; i < total.length && i < T.length; i++) total[i] *= T[i];
                }

                XYChart.Series<Number, Number> s = new XYChart.Series<>();

                String label = detFormulaText.getOrDefault(v, "Filter");
                s.setName(label);
                for (int i = 0; i < E.length && i < T.length; i++) {
                    s.getData().add(new XYChart.Data<>(E[i], T[i]));
                }
                chart.getData().add(s);
            }

            // Gesamt-Transmission (Produkt)
            if (total != null) {
                Verbindung v0 = activeDetFilters.get(0);
                double[] E = v0.getEnergieArray();
                XYChart.Series<Number, Number> sTot = new XYChart.Series<>();
                sTot.setName("Total (Produkt)");
                for (int i = 0; i < E.length && i < total.length; i++) {
                    sTot.getData().add(new XYChart.Data<>(E[i], total[i]));
                }
                chart.getData().add(sTot);
            }
        });

        return new Tab("Detector", parametersRoot2);
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
        // Detector-Felder
        String fenstermaterial = parseOrDefault(windowMaterialDet,"Be" );
        double fensterdicke_um = parseOrDefault(thicknessWindowDet, 7.62);   // µm
        String kontaktmaterial = parseOrDefault(contactlayerDet,"Au" );
        double kontaktdicke_nm = parseOrDefault(contactlayerThicknessDet, 50); // nm
        String detektormaterial = parseOrDefault(detectorMaterial,"Si" );
        double totschicht_um = parseOrDefault(inactiveLayer, 0.05);           // µm
        double activeLayer_mm = parseOrDefault(activeLayer, 3);               // mm

        // Tube-Tab für Energiegitter
        double emax_kV = parseOrDefault(xRayTubeVoltage, 35);
        double step_keV = parseOrDefault(energieStep, 0.05);
        double emin_keV = 0; // 0 → deine Klassen setzen intern auf step

        // Aktive Detektor-Filter einsammeln
        java.util.List<Verbindung> activeDetFilters = new java.util.ArrayList<>();
        for (Verbindung v : detFilterVerbindungen) {
            if (detFilterUse.getOrDefault(v, Boolean.TRUE)) {
                activeDetFilters.add(v);
            }
        }

        // Wenn keine aktiven Filter, Dummy (Transmission 1) hinzufügen,
        // damit Funktionsfilter trotzdem angewendet werden können.
        if (activeDetFilters.isEmpty()) {
            //Verbindung dummy = buildVerbindungFromSpec("Al", 2.70, 0.0); // Dicke 0 → neutral
            //activeDetFilters.add(dummy);
            //detFormulaText.put(dummy, "Al");
            //detFilterUse.put(dummy, true);
        }

        // Funktionsfilter (Segmente) auf diese Verbindungen anwenden
        applySegmentsToVerbindungen(activeDetFilters, detFuncSegs, detFuncDefault.get());


        // Detektor erzeugen
        double phi_deg = 0.0;
        double bedeckungsfaktor = 1.0;
        String datei = DATA_FILE;

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
                activeDetFilters
        );
    }


    // in JavaFx:
    private RöhreBasis buildTubeFromUI() {
        // **UI lesen**
        String anodenMat     = parseOrDefault(tubeMaterial, "Rh");
        double alphaDeg      = parseOrDefault(electronIncidentAngle, 20);  // α
        double betaDeg       = parseOrDefault(electronTakeoffAngle, 70);   // β
        double fensterWinkel = 0;                                          // falls kein Feld → 0°
        String fenstMat      = parseOrDefault(windowMaterial, "Be");
        double fenstDicke_um = parseOrDefault(windowMaterialThickness, 125);
        double i_mA          = parseOrDefault(tubeCurrent, 1.0);
        double emax_kV       = parseOrDefault(xRayTubeVoltage, 35);
        double sigmaConstVal = parseOrDefault(sigmaConst, 1.0314);
        double step_keV      = parseOrDefault(energieStep, 0.01);
        double t_meas        = parseOrDefault(measurementTime, 30);
        double c2c           = parseOrDefault(charZuCont, 1.0);
        double c2cL          = parseOrDefault(charZuContL, 1.0);

        // **aktive Tube-Filter einsammeln**
        java.util.List<Verbindung> activeTubeFilters = new java.util.ArrayList<>();
        for (Verbindung v : tubeFilterVerbindungen) {
            if (tubeFilterUse.getOrDefault(v, Boolean.TRUE)) activeTubeFilters.add(v);
        }
        if (activeTubeFilters.isEmpty()) {
            // neutraler Dummy, damit Funktionsfilter angewendet werden können
            Verbindung dummy = buildVerbindungFromSpec("Al", 2.70, 0.0);
            tubeFormulaText.put(dummy, "Al");
            tubeFilterUse.put(dummy, true);
            activeTubeFilters.add(dummy);
        }

        // **Funktions-Filter auf Tube-Filter anwenden**
        applySegmentsToVerbindungen(activeTubeFilters, tubeFuncSegs, tubeFuncDefault.get());

// Mapping ComboBox -> interne Schlüssel
        String modelKey = switch (tubeModel.getValue()) {
            case "Love & Scott"   -> "lovescott";
            case "Wiederschwinger"-> "widerschwinger";
            default               -> "widerschwinger";
        };

        RöhreBasis tube;
        switch (modelKey) {
            case "lovescott":
                if (sigmaConstVal==1.0314){sigmaConstVal=1.109;}
                tube = new LoveScottRöhre(
                        /* einfallswinkelalpha */   alphaDeg,
                        /* einfallswinkelbeta  */   betaDeg,
                        /* fensterwinkel       */   fensterWinkel,
                        /* charzucont          */   c2c,
                        /* charzucontL         */   c2cL,
                        /* fensterdickeRöhre   */   fenstDicke_um,
                        /* raumwinkel          */   1.0,
                        /* röhrenstrom [A]     */   i_mA,
                        /* emin [keV]          */   0.0,
                        /* emax [keV]          */   emax_kV,
                        /* sigma               */   sigmaConstVal,
                        /* step [keV]          */   step_keV,
                        /* messzeit [s]        */   t_meas,
                        /* röhrenmaterial      */   anodenMat,
                        /* fenstermaterial     */   fenstMat,
                        /* dateipfad           */   DATA_FILE,
                        /* filter_röhre        */   activeTubeFilters
                );
                break;

            case "widerschwinger":
            default:
                tube = new WiderschwingerRöhre(
                        /* einfallswinkelalpha */   alphaDeg,
                        /* einfallswinkelbeta  */   betaDeg,
                        /* fensterwinkel       */   fensterWinkel,
                        /* charzucont          */   c2c,
                        /* charzucontL         */   c2cL,
                        /* fensterdickeRöhre   */   fenstDicke_um,
                        /* raumwinkel          */   1.0,
                        /* röhrenstrom [A]     */   i_mA,
                        /* emin [keV]          */   0.0,
                        /* emax [keV]          */   emax_kV,
                        /* sigma               */   sigmaConstVal,
                        /* step [keV]          */   step_keV,
                        /* messzeit [s]        */   t_meas,
                        /* röhrenmaterial      */   anodenMat,
                        /* fenstermaterial     */   fenstMat,
                        /* dateipfad           */   DATA_FILE,
                        /* filter_röhre        */   activeTubeFilters
                );
                break;
        }


        // Daten vorbereiten (füllt continuous / characteristic / gesamt)
        tube.prepareData();
        return tube;
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

    private final java.util.Map<Verbindung, Boolean> tubeFilterUse = new java.util.IdentityHashMap<>();
    private final java.util.Map<Verbindung, Boolean> detFilterUse  = new java.util.IdentityHashMap<>();




    private VBox buildFilterColumn(String title,
                                   ObservableList<Verbindung> listData,
                                   java.util.Map<Verbindung,String> textMap,java.util.Map<Verbindung,Boolean> useMap) {

        // Startwerte (nur beim ersten Aufruf)
        if (listData.isEmpty()) {
            //Verbindung v1 = buildVerbindungFromSpec("Al", 2.70, 50);
            //Verbindung v2 = buildVerbindungFromSpec("Rh", 12.41, 25);
            //listData.addAll(v1, v2);
            //textMap.put(v1, "Al");
            //textMap.put(v2, "Rh");
            //useMap.put(v1, true);
            //useMap.put(v2, true);
        }

        ListView<Verbindung> list = new ListView<>(listData);
        list.setCellFactory(lv -> new ListCell<>() {
            // --- UI pro Karte ---
            private final CheckBox chkUse = new CheckBox("Use");
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

                tfRho.focusedProperty().addListener((o, was, ist) -> {
                    if (!ist) tfRho.fireEvent(new javafx.event.ActionEvent());
                });



                // ENTER in d: Dicke direkt am bestehenden Objekt setzen (kein Neuaufbau)
                tfTh.setOnAction(e -> {
                    Verbindung v = itemRef.get();
                    if (v != null) {
                        double thCm = parseDouble(tfTh, v.getFensterDickeCm());
                        v.setFensterDickeCm(thCm);
                    }
                });

                // nach tfTh.setOnAction(...)
                tfTh.focusedProperty().addListener((o, was, ist) -> {
                    if (!ist) tfTh.fireEvent(new javafx.event.ActionEvent());
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
        VBox tubeCol = buildFilterColumn("Röhrenfilter", tubeFilterVerbindungen, tubeFormulaText, tubeFilterUse);
        VBox detCol  = buildFilterColumn("Detektorfilter", detFilterVerbindungen, detFormulaText, detFilterUse);


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
        //if (tubeFuncSegs.isEmpty()) tubeFuncSegs.add(constExprOne());
        //if (detFuncSegs.isEmpty())  detFuncSegs.add(constExprOne());

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
        Button btnInfo = new Button("i");
        double r = 14;
        btnInfo.setShape(new Circle(r));
        btnInfo.setMinSize(2*r, 2*r);
        btnInfo.setPrefSize(2*r, 2*r);
        btnInfo.setMaxSize(2*r, 2*r);
        btnInfo.setStyle("-fx-background-color:#1976d2; -fx-text-fill:white; -fx-font-weight:bold;");

        Tooltip infoTip = new Tooltip("""
        Info for piecewise function filters
        """);
        infoTip.setShowDelay(Duration.millis(200));     // 0.2 s
// optional:
        infoTip.setHideDelay(Duration.millis(50));      // schneller weg
        infoTip.setShowDuration(Duration.seconds(10));  // wie lange sichtbar

        Tooltip.install(btnInfo, infoTip);



        btnInfo.setOnAction(e -> {
            Window w = btnInfo.getScene() != null ? btnInfo.getScene().getWindow() : null;
            showFunctionFiltersInfo(w);
        });


        HBox globalControls = new HBox(10, btnShowAllTube, btnShowAllDet, btnInfo);
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

    private void showFunctionFiltersInfo(Window owner) {
        String info = """
        What can I do here?
        
        Important
        • The function value is always treated as non-negative: |f(x)| is used.
        
        Manage segments
        • "Add segment" creates a new interval.
        • "↑/↓" changes priority. Segments higher in the list take precedence on overlaps.
          Example: S1 on [2, 4] and S2 on [3, 5] → f(x) uses S1 on [2, 4] and S2 on (4, 5].
        • "Use" enables/disables a segment (card turns green/red).
        
        Range & bounds
        • Range: a .. b, with [ ] inclusive and ( ) exclusive.
        • The bracket buttons next to a/b toggle inclusive/exclusive.
        
        Expression (expr)
        • mXparser syntax, e.g.: 1, 0.75, sin(x), cos(x)+0.2*x, exp(-x/10).
        • Trigonometric functions use radians.
        • Syntax errors prevent the segment from being applied.
        
        Clamp
        • "Clamp" limits the segment output to [yMin, yMax] to cap spikes/outliers.
        
        Default outside segments
        • Applied wherever no segment matches or a calculation yields NaN/∞.
        • For a neutral filter, use 1.0.
        • Monoenergetic example: create a narrow segment, e.g. [20.2, 20.5] with expr = 1,
          and set the default to 0.
        
        Display
        • "Show" on a segment card plots that segment over its interval.
        • "Show all (Tube)/(Detector)" plots the combined active segments across the global energy range.
        
        Global energy range
        • Uses Emin = 0, Emax, and the step size from the Tube tab.
        
        """;


        TextArea ta = new TextArea(info);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(720, 500);

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Function Filters – Help");
        a.setHeaderText("Info Function Filters");
        a.getDialogPane().setContent(ta);
        if (owner != null) a.initOwner(owner);
        a.showAndWait();
    }


    // neutrales Startsegment (expr ≡ 1 auf großem Bereich)
    private PwSegment constExprOne() {
        PwSegment s = new PwSegment();
        s.enabled = true;
        s.type = SegmentType.EXPR;
        s.expr = "1";
        s.a = 0; s.b = globalEmax(); s.inclA = true; s.inclB = true;
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


    private void applySegmentsToVerbindungen(
            java.util.List<Verbindung> verbindungen,
            java.util.List<PwSegment> segs,
            double defaultValue
    ) {
        // Default wie im MathParser-Flow: immer nicht-negativ
        double def = Math.abs(defaultValue);

        // Linke Energieboundary wie im Plot-All (Emin=0 → step als Start)
        double eminLeft = (globalEmin <= 0 ? globalStep() : globalEmin);
        double emaxRight = globalEmax();

        for (Verbindung v : verbindungen) {
            // setzt den Default außerhalb aller Segmente
            v.clearModulation(def);

            // WICHTIG: gleiche Reihenfolge wie im Plot-All
            // => erstes passendes Segment gewinnt.
            // Falls deine Verbindung intern "zuletzt gewinnt" implementiert,
            // DANN hier segs rückwärts iterieren.
            for (PwSegment s : segs) {
                if (!s.enabled) continue;

                // 1) exakten Ausdruck wie im Plot-All generieren (inkl. Clamp)
                String expr = s.toExpressionString(null);

                // 2) wie MathParser.evaluate(): immer |f(x)|
                expr = "abs((" + expr + "))";

                // 3) Bereichsgrenzen an die Plot-Konvention anpassen
                double a = s.a;
                double b = s.b;
                boolean inclA = s.inclA;
                boolean inclB = s.inclB;

                // wenn a <= 0, benutze dieselbe Startgrenze wie Plot-All
                if (!(a > 0)) {
                    a = eminLeft;
                    inclA = true; // entspricht dem Plot-All-Sampling ab der linken Kante
                }
                // rechts sicherstellen, dass wir nicht über emax hinausgehen
                if (b > emaxRight) {
                    b = emaxRight;
                    // inclB so lassen; Verbindung kümmert sich um Exklusivität
                }

                // 4) Segment auf die Verbindung legen
                v.addModulationSegment(expr, a, inclA, b, inclB);
            }
        }
    }


    private void commitAllTextFields(Node root) {
        root.lookupAll(".text-field").forEach(n -> {
            if (n instanceof TextField tf) tf.fireEvent(new javafx.event.ActionEvent());
        });
    }



    // ====== Properties/Profile: Felder oben in der Klasse ======
    private ComboBox<String> cmbDataSource;
    private TextField txtDataFile;
    private Button btnBrowseData;
    private CheckBox chkAutoLoadLast;

    private ListView<String> lstProfiles;
    private TextField txtProfileName;
    private Button btnSaveProfile, btnLoadProfile, btnDeleteProfile;

    // Persistence
    private final Preferences prefs = Preferences.userNodeForPackage(JavaFx.class);


    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class AppSettings {
        // Datenquelle
        String  dataSource;   // "McMaster" | "Custom…"
        String  dataFile;

        // Tube  (ALLE numeric Felder: Double -> nullable!)
        String  tubeMaterial;
        Double  electronIncidentAngle, electronTakeoffAngle;
        Double  charZuCont, charZuContL;
        String  windowMaterial;
        Double  windowMaterialThickness;
        Double  tubeCurrent, xRayTubeVoltage;
        Double  sigmaConst, energieStep, measurementTime;
        String  tubeModel; // "Wiederschwinger" | "Love & Scott"

        // Detector
        String  windowMaterialDet, contactlayerDet, detectorMaterial;
        Double  thicknessWindowDet, contactlayerThicknessDet, inactiveLayer, activeLayer;

        public java.util.List<MatFilterDTO> tubeMatFilters;
        public java.util.List<MatFilterDTO> detMatFilters;


        public java.util.List<SegmentDTO> tubeFuncSegs;
        public java.util.List<SegmentDTO> detFuncSegs;

        // Function-Filter Defaults
        Double  tubeFuncDefault, detFuncDefault;

        // Flags
        Boolean autoLoadLast;


        static AppSettings fromUI(JavaFx ui) {
            AppSettings s = new AppSettings();

            // Datenquelle
            s.dataSource = emptyToNull(ui.cmbDataSource.getValue());
            s.dataFile   = emptyToNull(ui.txtDataFile.getText());

            // Tube
            s.tubeMaterial            = emptyToNull(ui.tubeMaterial.getText());
            s.electronIncidentAngle   = readDoubleOrNull(ui.electronIncidentAngle);
            s.electronTakeoffAngle    = readDoubleOrNull(ui.electronTakeoffAngle);
            s.charZuCont              = readDoubleOrNull(ui.charZuCont);
            s.charZuContL             = readDoubleOrNull(ui.charZuContL);
            s.windowMaterial          = emptyToNull(ui.windowMaterial.getText());
            s.windowMaterialThickness = readDoubleOrNull(ui.windowMaterialThickness);
            s.tubeCurrent             = readDoubleOrNull(ui.tubeCurrent);
            s.xRayTubeVoltage         = readDoubleOrNull(ui.xRayTubeVoltage);
            s.sigmaConst              = readDoubleOrNull(ui.sigmaConst);
            s.energieStep             = readDoubleOrNull(ui.energieStep);
            s.measurementTime         = readDoubleOrNull(ui.measurementTime);
            s.tubeModel               = emptyToNull(ui.tubeModel.getValue());

            // Detector
            s.windowMaterialDet        = emptyToNull(ui.windowMaterialDet.getText());
            s.thicknessWindowDet       = readDoubleOrNull(ui.thicknessWindowDet);
            s.contactlayerDet          = emptyToNull(ui.contactlayerDet.getText());
            s.contactlayerThicknessDet = readDoubleOrNull(ui.contactlayerThicknessDet);
            s.detectorMaterial         = emptyToNull(ui.detectorMaterial.getText());
            s.inactiveLayer            = readDoubleOrNull(ui.inactiveLayer);
            s.activeLayer              = readDoubleOrNull(ui.activeLayer);

            // Function-Filter Defaults
            s.tubeFuncDefault = ui.tubeFuncDefault.get(); // DoubleProperty → primitive double
            s.detFuncDefault  = ui.detFuncDefault.get();

            // Flag
            s.autoLoadLast = ui.chkAutoLoadLast.isSelected();


            // --- Material-Filter (Tube) exportieren ---
            s.tubeMatFilters = new java.util.ArrayList<>();
            for (Verbindung v : ui.tubeFilterVerbindungen) {
                MatFilterDTO d = new MatFilterDTO();
                d.formula     = ui.tubeFormulaText.getOrDefault(v, ""); // Anzeigename/Formel
                d.density     = v.getDichte();
                d.thicknessCm = v.getFensterDickeCm();
                d.use         = ui.tubeFilterUse.getOrDefault(v, Boolean.TRUE);
                s.tubeMatFilters.add(d);
            }

// --- Material-Filter (Detector) exportieren ---
            s.detMatFilters = new java.util.ArrayList<>();
            for (Verbindung v : ui.detFilterVerbindungen) {
                MatFilterDTO d = new MatFilterDTO();
                d.formula     = ui.detFormulaText.getOrDefault(v, "");
                d.density     = v.getDichte();
                d.thicknessCm = v.getFensterDickeCm();
                d.use         = ui.detFilterUse.getOrDefault(v, Boolean.TRUE);
                s.detMatFilters.add(d);
            }

// --- Funktions-Filter (Tube) exportieren ---
            s.tubeFuncSegs = new java.util.ArrayList<>();
            for (PwSegment seg : ui.tubeFuncSegs) {
                SegmentDTO d = new SegmentDTO();
                d.enabled = seg.enabled;
                d.expr    = seg.expr;
                d.a = seg.a; d.b = seg.b;
                d.inclA = seg.inclA; d.inclB = seg.inclB;
                d.clamp = seg.clamp; d.yMin = seg.yMin; d.yMax = seg.yMax;
                s.tubeFuncSegs.add(d);
            }

// --- Funktions-Filter (Detector) exportieren ---
            s.detFuncSegs = new java.util.ArrayList<>();
            for (PwSegment seg : ui.detFuncSegs) {
                SegmentDTO d = new SegmentDTO();
                d.enabled = seg.enabled;
                d.expr    = seg.expr;
                d.a = seg.a; d.b = seg.b;
                d.inclA = seg.inclA; d.inclB = seg.inclB;
                d.clamp = seg.clamp; d.yMin = seg.yMin; d.yMax = seg.yMax;
                s.detFuncSegs.add(d);
            }


            return s;
        }

        // --------- Settings -> UI: null == leer anzeigen ----------
        void applyToUI(JavaFx ui) {
            ui.cmbDataSource.setValue(nullToMcMaster(dataSource));
            ui.txtDataFile.setText(nullToEmpty(dataFile));

            ui.tubeMaterial.setText(nullToEmpty(tubeMaterial));
            ui.electronIncidentAngle.setText(doubleToStr(electronIncidentAngle));
            ui.electronTakeoffAngle.setText(doubleToStr(electronTakeoffAngle));
            ui.charZuCont.setText(doubleToStr(charZuCont));
            ui.charZuContL.setText(doubleToStr(charZuContL));
            ui.windowMaterial.setText(nullToEmpty(windowMaterial));
            ui.windowMaterialThickness.setText(doubleToStr(windowMaterialThickness));
            ui.tubeCurrent.setText(doubleToStr(tubeCurrent));
            ui.xRayTubeVoltage.setText(doubleToStr(xRayTubeVoltage));
            ui.sigmaConst.setText(doubleToStr(sigmaConst));
            ui.energieStep.setText(doubleToStr(energieStep));
            ui.measurementTime.setText(doubleToStr(measurementTime));
            if (tubeModel != null && ui.tubeModel.getItems().contains(tubeModel)) {
                ui.tubeModel.setValue(tubeModel);
            }

            ui.windowMaterialDet.setText(nullToEmpty(windowMaterialDet));
            ui.thicknessWindowDet.setText(doubleToStr(thicknessWindowDet));
            ui.contactlayerDet.setText(nullToEmpty(contactlayerDet));
            ui.contactlayerThicknessDet.setText(doubleToStr(contactlayerThicknessDet));
            ui.detectorMaterial.setText(nullToEmpty(detectorMaterial));
            ui.inactiveLayer.setText(doubleToStr(inactiveLayer));
            ui.activeLayer.setText(doubleToStr(activeLayer));

            ui.tubeFuncDefault.set(safeOr(ui.tubeFuncDefault.get(), tubeFuncDefault, 1.0));
            ui.detFuncDefault.set(safeOr(ui.detFuncDefault.get(),  detFuncDefault,  1.0));

            ui.chkAutoLoadLast.setSelected(Boolean.TRUE.equals(autoLoadLast));



            // --- Material-Filter (Tube) wiederherstellen ---
            if (tubeMatFilters != null) {
                ui.tubeFilterVerbindungen.clear();
                ui.tubeFormulaText.clear();
                ui.tubeFilterUse.clear();
                for (MatFilterDTO d : tubeMatFilters) {
                    String formula = (d.formula == null ? "Al" : d.formula.trim());
                    double rho     = d.density;
                    double thick   = d.thicknessCm;
                    Verbindung v   = ui.buildVerbindungFromSpec(formula, rho, thick);
                    ui.tubeFilterVerbindungen.add(v);
                    ui.tubeFormulaText.put(v, formula);
                    ui.tubeFilterUse.put(v, d.use);
                }
            }

// --- Material-Filter (Detector) wiederherstellen ---
            if (detMatFilters != null) {
                ui.detFilterVerbindungen.clear();
                ui.detFormulaText.clear();
                ui.detFilterUse.clear();
                for (MatFilterDTO d : detMatFilters) {
                    String formula = (d.formula == null ? "Al" : d.formula.trim());
                    double rho     = d.density;
                    double thick   = d.thicknessCm;
                    Verbindung v   = ui.buildVerbindungFromSpec(formula, rho, thick);
                    ui.detFilterVerbindungen.add(v);
                    ui.detFormulaText.put(v, formula);
                    ui.detFilterUse.put(v, d.use);
                }
            }

// --- Funktions-Filter (Tube) wiederherstellen ---
            if (tubeFuncSegs != null) {
                ui.tubeFuncSegs.clear();
                for (SegmentDTO d : tubeFuncSegs) {
                    PwSegment s = new PwSegment();
                    s.enabled = d.enabled;
                    s.type = SegmentType.EXPR; // wir verwenden EXPR
                    s.expr = d.expr;
                    s.a = d.a; s.b = d.b;
                    s.inclA = d.inclA; s.inclB = d.inclB;
                    s.clamp = d.clamp; s.yMin = d.yMin; s.yMax = d.yMax;
                    ui.tubeFuncSegs.add(s);
                }
                // Segmente auf aktuelle Verbindungen anwenden
                ui.applySegmentsToVerbindungen(ui.tubeFilterVerbindungen, ui.tubeFuncSegs, ui.tubeFuncDefault.get());
            }

// --- Funktions-Filter (Detector) wiederherstellen ---
            if (detFuncSegs != null) {
                ui.detFuncSegs.clear();
                for (SegmentDTO d : detFuncSegs) {
                    PwSegment s = new PwSegment();
                    s.enabled = d.enabled;
                    s.type = SegmentType.EXPR;
                    s.expr = d.expr;
                    s.a = d.a; s.b = d.b;
                    s.inclA = d.inclA; s.inclB = d.inclB;
                    s.clamp = d.clamp; s.yMin = d.yMin; s.yMax = d.yMax;
                    ui.detFuncSegs.add(s);
                }
                ui.applySegmentsToVerbindungen(ui.detFilterVerbindungen, ui.detFuncSegs, ui.detFuncDefault.get());
            }


            // Laufzeit: DATA_FILE aktualisieren
            ui.applyDataSourceToRuntime();
        }

        private static String nullToMcMaster(String s){ return s==null? "McMaster": s; }
        private static String nullToEmpty(String s){ return s==null? "": s; }
        private static String emptyToNull(String s){ return (s==null || s.isBlank())? null : s.trim(); }
        private static String doubleToStr(Double d){ return d==null? "" : Double.toString(d); }
        private static Double safeOr(double current, Double v, double def){ return v!=null? v : (current!=0? current : def); }
    }

    private static Double readDoubleOrNull(TextField tf) {
        String s = (tf == null) ? null : tf.getText();
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception ignored) {
            return null; // ungültig -> null
        }
    }



    private static void prefsPutStr(Preferences n, String k, String v) {
        n.put(k, v == null ? "" : v);
    }
    private static void prefsPutDouble(Preferences n, String k, Double v) {
        if (v != null) n.putDouble(k, v); else { try { n.remove(k); } catch (Exception ignore) {} }
    }
    private static void prefsPutBool(Preferences n, String k, Boolean v) {
        if (v != null) n.putBoolean(k, v); else { try { n.remove(k); } catch (Exception ignore) {} }
    }

    private void saveProfileToPrefs(String name, AppSettings s) {
        var node = prefs.node("profiles").node(name);

        // Strings
        putMaybeString(node, "dataSource", s.dataSource);
        putMaybeString(node, "dataFile",   s.dataFile);
        putMaybeString(node, "tubeMaterial", s.tubeMaterial);
        putMaybeString(node, "windowMaterial", s.windowMaterial);
        putMaybeString(node, "tubeModel", s.tubeModel);

        putMaybeString(node, "windowMaterialDet", s.windowMaterialDet);
        putMaybeString(node, "contactlayerDet",   s.contactlayerDet);
        putMaybeString(node, "detectorMaterial",  s.detectorMaterial);

        // Zahlen als String (leere bleiben leer)
        putMaybeDouble(node, "electronIncidentAngle", s.electronIncidentAngle);
        putMaybeDouble(node, "electronTakeoffAngle",  s.electronTakeoffAngle);
        putMaybeDouble(node, "charZuCont", s.charZuCont);
        putMaybeDouble(node, "charZuContL", s.charZuContL);
        putMaybeDouble(node, "windowMaterialThickness", s.windowMaterialThickness);
        putMaybeDouble(node, "tubeCurrent", s.tubeCurrent);
        putMaybeDouble(node, "xRayTubeVoltage", s.xRayTubeVoltage);
        putMaybeDouble(node, "sigmaConst", s.sigmaConst);
        putMaybeDouble(node, "energieStep", s.energieStep);
        putMaybeDouble(node, "measurementTime", s.measurementTime);

        putMaybeDouble(node, "thicknessWindowDet", s.thicknessWindowDet);
        putMaybeDouble(node, "contactlayerThicknessDet", s.contactlayerThicknessDet);
        putMaybeDouble(node, "inactiveLayer", s.inactiveLayer);
        putMaybeDouble(node, "activeLayer",   s.activeLayer);

        putMaybeDouble(node, "tubeFuncDefault", s.tubeFuncDefault);
        putMaybeDouble(node, "detFuncDefault",  s.detFuncDefault);

        node.putBoolean("autoLoadLast", s.autoLoadLast != null && s.autoLoadLast);

        // (deine Filter/Segmente speicherst du weiter wie bisher)

        prefs.put("lastProfile", name);
    }



    private static void deleteChildren(Preferences p) {
        try {
            for (String c : p.childrenNames()) {
                p.node(c).removeNode();
            }
        } catch (Exception ignore) {}
    }

    private void saveMatFiltersToPrefs(Preferences p, java.util.List<Verbindung> list,
                                       java.util.Map<Verbindung,String> textMap,
                                       java.util.Map<Verbindung,Boolean> useMap) {
        // vorherige Einträge leeren
        deleteChildren(p);
        p.putInt("count", list.size());
        for (int i = 0; i < list.size(); i++) {
            Verbindung v = list.get(i);
            Preferences c = p.node(Integer.toString(i));
            c.put("formula", textMap.getOrDefault(v, ""));
            c.putDouble("density", v.getDichte());
            c.putDouble("thicknessCm", v.getFensterDickeCm());
            c.putBoolean("use", useMap.getOrDefault(v, Boolean.TRUE));
        }
    }

    private void saveFuncSegsToPrefs(Preferences p, java.util.List<PwSegment> segs) {
        deleteChildren(p);
        p.putInt("count", segs.size());
        for (int i = 0; i < segs.size(); i++) {
            PwSegment s = segs.get(i);
            Preferences c = p.node(Integer.toString(i));
            c.putBoolean("enabled", s.enabled);
            c.put("expr", s.expr==null? "": s.expr);
            c.putDouble("a", s.a);
            c.putDouble("b", s.b);
            c.putBoolean("inclA", s.inclA);
            c.putBoolean("inclB", s.inclB);
            c.putBoolean("clamp", s.clamp);
            c.putDouble("yMin", s.yMin);
            c.putDouble("yMax", s.yMax);
        }
    }


    private AppSettings loadProfileFromPrefs(String name) {
        var node = prefs.node("profiles").node(name);
        AppSettings s = new AppSettings();

        // Strings
        s.dataSource = getMaybeString(node, "dataSource");
        s.dataFile   = getMaybeString(node, "dataFile");
        s.tubeMaterial = getMaybeString(node, "tubeMaterial");
        s.windowMaterial = getMaybeString(node, "windowMaterial");
        s.tubeModel = getMaybeString(node, "tubeModel");

        s.windowMaterialDet = getMaybeString(node, "windowMaterialDet");
        s.contactlayerDet   = getMaybeString(node, "contactlayerDet");
        s.detectorMaterial  = getMaybeString(node, "detectorMaterial");

        // Zahlen (bleiben null, wenn leer)
        s.electronIncidentAngle   = getMaybeDouble(node, "electronIncidentAngle");
        s.electronTakeoffAngle    = getMaybeDouble(node, "electronTakeoffAngle");
        s.charZuCont              = getMaybeDouble(node, "charZuCont");
        s.charZuContL             = getMaybeDouble(node, "charZuContL");
        s.windowMaterialThickness = getMaybeDouble(node, "windowMaterialThickness");
        s.tubeCurrent             = getMaybeDouble(node, "tubeCurrent");
        s.xRayTubeVoltage         = getMaybeDouble(node, "xRayTubeVoltage");
        s.sigmaConst              = getMaybeDouble(node, "sigmaConst");
        s.energieStep             = getMaybeDouble(node, "energieStep");
        s.measurementTime         = getMaybeDouble(node, "measurementTime");

        s.thicknessWindowDet       = getMaybeDouble(node, "thicknessWindowDet");
        s.contactlayerThicknessDet = getMaybeDouble(node, "contactlayerThicknessDet");
        s.inactiveLayer            = getMaybeDouble(node, "inactiveLayer");
        s.activeLayer              = getMaybeDouble(node, "activeLayer");

        s.tubeFuncDefault = getMaybeDouble(node, "tubeFuncDefault");
        s.detFuncDefault  = getMaybeDouble(node, "detFuncDefault");

        s.autoLoadLast = node.getBoolean("autoLoadLast", false);

        // (Filter/Segmente weiter wie bisher laden)
        s.tubeMatFilters = loadMatFiltersFromPrefs(node.node("tubeFilters"));
        s.detMatFilters  = loadMatFiltersFromPrefs(node.node("detFilters"));
        s.tubeFuncSegs   = loadFuncSegsFromPrefs(node.node("tubeFuncSegs"));
        s.detFuncSegs    = loadFuncSegsFromPrefs(node.node("detFuncSegs"));

        return s;
    }


    private java.util.List<MatFilterDTO> loadMatFiltersFromPrefs(Preferences p) {
        java.util.List<MatFilterDTO> out = new java.util.ArrayList<>();
        if (p == null) return out;
        int n = p.getInt("count", -1);
        try {
            if (n >= 0) {
                for (int i = 0; i < n; i++) {
                    Preferences c = p.node(Integer.toString(i));
                    MatFilterDTO d = new MatFilterDTO();
                    d.formula     = c.get("formula", "");
                    d.density     = c.getDouble("density", 0.0);
                    d.thicknessCm = c.getDouble("thicknessCm", 0.0);
                    d.use         = c.getBoolean("use", true);
                    out.add(d);
                }
            } else {
                // Fallback: falls kein "count" existiert, anhand childrenNames() laden
                for (String child : p.childrenNames()) {
                    Preferences c = p.node(child);
                    MatFilterDTO d = new MatFilterDTO();
                    d.formula     = c.get("formula", "");
                    d.density     = c.getDouble("density", 0.0);
                    d.thicknessCm = c.getDouble("thicknessCm", 0.0);
                    d.use         = c.getBoolean("use", true);
                    out.add(d);
                }
            }
        } catch (Exception ignore) {}
        return out;
    }

    private java.util.List<SegmentDTO> loadFuncSegsFromPrefs(Preferences p) {
        java.util.List<SegmentDTO> out = new java.util.ArrayList<>();
        if (p == null) return out;
        int n = p.getInt("count", -1);
        try {
            if (n >= 0) {
                for (int i = 0; i < n; i++) {
                    Preferences c = p.node(Integer.toString(i));
                    SegmentDTO d = new SegmentDTO();
                    d.enabled = c.getBoolean("enabled", true);
                    d.expr    = c.get("expr", "1");
                    d.a       = c.getDouble("a", 0.0);
                    d.b       = c.getDouble("b", Math.max(1.0, globalEmax()));
                    d.inclA   = c.getBoolean("inclA", true);
                    d.inclB   = c.getBoolean("inclB", true);
                    d.clamp   = c.getBoolean("clamp", false);
                    d.yMin    = c.getDouble("yMin", 0.0);
                    d.yMax    = c.getDouble("yMax", 0.0);
                    out.add(d);
                }
            } else {
                for (String child : p.childrenNames()) {
                    Preferences c = p.node(child);
                    SegmentDTO d = new SegmentDTO();
                    d.enabled = c.getBoolean("enabled", true);
                    d.expr    = c.get("expr", "1");
                    d.a       = c.getDouble("a", 0.0);
                    d.b       = c.getDouble("b", Math.max(1.0, globalEmax()));
                    d.inclA   = c.getBoolean("inclA", true);
                    d.inclB   = c.getBoolean("inclB", true);
                    d.clamp   = c.getBoolean("clamp", false);
                    d.yMin    = c.getDouble("yMin", 0.0);
                    d.yMax    = c.getDouble("yMax", 0.0);
                    out.add(d);
                }
            }
        } catch (Exception ignore) {}
        return out;
    }



    private void refreshProfileList() {
        try {
            // vorhandene Profilknoten
            String[] names = prefs.node("profiles").childrenNames();
            java.util.Set<String> existing = new java.util.LinkedHashSet<>();
            java.util.Collections.addAll(existing, names);

            // gewünschte Reihenfolge laden und nur existierende Namen übernehmen
            java.util.List<String> order = loadProfilesOrderFromPrefs();
            java.util.List<String> result = new java.util.ArrayList<>();

            for (String n : order) {
                if (existing.remove(n)) { // nur wenn es den Knoten gibt
                    result.add(n);
                }
            }
            // alles, was (neu) existiert, aber noch nicht in 'order' war, hinten anhängen (alphabetisch oder so)
            java.util.List<String> rest = new java.util.ArrayList<>(existing);
            java.util.Collections.sort(rest, String.CASE_INSENSITIVE_ORDER); // optional
            result.addAll(rest);

            lstProfiles.getItems().setAll(result);

            // falls Reihenfolge geändert wurde (z.B. neue Profile), gleich zurückschreiben
            saveProfilesOrderToPrefs(result);

        } catch (Exception ignore) {
            lstProfiles.getItems().clear();
        }
    }


    private void applyDataSourceToRuntime() {
        String sel = cmbDataSource.getValue();
        if ("Custom…".equals(sel)) {
            DATA_FILE = txtDataFile.getText()==null? "": txtDataFile.getText().trim();
        } else {
            DATA_FILE = "MCMASTER.TXT";
        }
    }

    private void autoLoadLastProfileIfWanted() {
        String last = prefs.get("lastProfile", null);
        boolean globalAuto = prefs.getBoolean("autoLoadGlobal", false);
        if (last != null) {
            var s = loadProfileFromPrefs(last);
            if (s.autoLoadLast || globalAuto) {
                s.applyToUI(this);
                refreshProfileList();
                lstProfiles.getSelectionModel().select(last);
            }
        } else if (globalAuto) {
            // kein Profil gespeichert, aber Global-Auto an -> nichts tun
        }
    }

    private void exportCurrentProfileToJson() {
        try {
            AppSettings s = AppSettings.fromUI(this);

            var fc = new javafx.stage.FileChooser();
            fc.setTitle("Export profile as JSON");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("JSON files", "*.json")
            );
            var last = prefs.get("jsonLastDir", null);
            if (last != null) {
                var dir = new java.io.File(last);
                if (dir.isDirectory()) fc.setInitialDirectory(dir);
            }

            var owner = (btnSaveProfile != null && btnSaveProfile.getScene()!=null)
                    ? btnSaveProfile.getScene().getWindow() : null;
            var f = fc.showSaveDialog(owner);
            if (f == null) return;

            java.io.File out = f.getName().toLowerCase().endsWith(".json")
                    ? f
                    : new java.io.File(f.getParentFile(), f.getName() + ".json");

            mapper.writeValue(out, s);

            prefs.put("jsonLastDir", out.getParentFile().getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Profile exported:\n" + out.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Export failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    private void importProfileFromJson() {
        try {
            var fc = new javafx.stage.FileChooser();
            fc.setTitle("Import profile from JSON");
            fc.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("JSON files", "*.json")
            );
            var last = prefs.get("jsonLastDir", null);
            if (last != null) {
                var dir = new java.io.File(last);
                if (dir.isDirectory()) fc.setInitialDirectory(dir);
            }

            var owner = (btnLoadProfile != null && btnLoadProfile.getScene()!=null)
                    ? btnLoadProfile.getScene().getWindow() : null;
            var f = fc.showOpenDialog(owner);
            if (f == null) return;

            AppSettings s = mapper.readValue(f, AppSettings.class);
            s.applyToUI(this);

            String base = f.getName().replaceFirst("\\.json$", "");
            prefs.put("lastProfile", base);
            saveProfileToPrefs(base, s);
            refreshProfileList();
            lstProfiles.getSelectionModel().select(base);

            prefs.put("jsonLastDir", f.getParentFile().getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Profile imported:\n" + f.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Import failed:\n" + ex.getMessage()).showAndWait();
        }
    }

    // ====== Tab-Aufbau ======
    private Tab buildPropertiesTab() {
        Label lblSource = new Label("Data source:");
        cmbDataSource = new ComboBox<>();
        cmbDataSource.getItems().addAll("McMaster", "Custom…");
        cmbDataSource.setValue("McMaster");

        txtDataFile = new TextField();
        txtDataFile.setPromptText("path/to/datafile.txt");
        txtDataFile.setPrefColumnCount(28);
        txtDataFile.setDisable(true);

        btnBrowseData = new Button("Browse…");
        btnBrowseData.setDisable(true);

        Button btnUpOrder = new Button("↑");
        Button btnDownOrder = new Button("↓");

        cmbDataSource.valueProperty().addListener((o, ov, nv) -> {
            boolean custom = "Custom…".equals(nv);
            txtDataFile.setDisable(!custom);
            btnBrowseData.setDisable(!custom);
            applyDataSourceToRuntime();
        });


        btnUpOrder.setOnAction(e -> {
            int i = lstProfiles.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                var items = lstProfiles.getItems();
                String a = items.get(i - 1);
                String b = items.get(i);
                items.set(i - 1, b);
                items.set(i, a);
                lstProfiles.getSelectionModel().select(i - 1);
                saveProfilesOrderToPrefs(items);
            }
        });

        btnDownOrder.setOnAction(e -> {
            int i = lstProfiles.getSelectionModel().getSelectedIndex();
            var items = lstProfiles.getItems();
            if (i >= 0 && i < items.size() - 1) {
                String a = items.get(i);
                String b = items.get(i + 1);
                items.set(i, b);
                items.set(i + 1, a);
                lstProfiles.getSelectionModel().select(i + 1);
                saveProfilesOrderToPrefs(items);
            }
        });


        btnBrowseData.setOnAction(e -> {
            var fc = new javafx.stage.FileChooser();
            fc.setTitle("Choose data file");
            fc.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Text files", "*.txt", "*.dat", "*.csv"),
                    new javafx.stage.FileChooser.ExtensionFilter("All files", "*.*")
            );
            var owner = (btnBrowseData.getScene()!=null) ? btnBrowseData.getScene().getWindow() : null;
            var f = fc.showOpenDialog(owner);
            if (f != null) {
                txtDataFile.setText(f.getAbsolutePath());
                applyDataSourceToRuntime();
            }
        });

        chkAutoLoadLast = new CheckBox("Auto-load last profile at startup");

        lstProfiles = new ListView<>();
        lstProfiles.setPrefHeight(180);

        txtProfileName = new TextField();
        txtProfileName.setPromptText("profile name");

        btnSaveProfile = new Button("Save");
        btnLoadProfile = new Button("Load");
        btnDeleteProfile = new Button("Delete");

        Button btnExportJson = new Button("Export JSON…");
        Button btnImportJson = new Button("Import JSON…");

        btnSaveProfile.setOnAction(e -> {
            String name = txtProfileName.getText();
            if (name == null || name.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a profile name.").showAndWait();
                return;
            }
            var s = AppSettings.fromUI(this);
            s.autoLoadLast = chkAutoLoadLast.isSelected();
            saveProfileToPrefs(name, s);
            refreshProfileList();
            lstProfiles.getSelectionModel().select(name);
            saveProfilesOrderToPrefs(lstProfiles.getItems());
        });

        btnLoadProfile.setOnAction(e -> {
            String name = lstProfiles.getSelectionModel().getSelectedItem();
            if (name == null) return;
            var s = loadProfileFromPrefs(name);
            s.applyToUI(this);
            prefs.put("lastProfile", name);
        });

        btnDeleteProfile.setOnAction(e -> {
            String name = lstProfiles.getSelectionModel().getSelectedItem();
            if (name == null) return;
            try {
                prefs.node("profiles").node(name).removeNode();
                saveProfilesOrderToPrefs(lstProfiles.getItems());
            } catch (Exception ignore) {}
            refreshProfileList();
        });

        chkAutoLoadLast.setOnAction(e -> {
            boolean on = chkAutoLoadLast.isSelected();
            String last = prefs.get("lastProfile", null);
            if (last != null) {
                var s = loadProfileFromPrefs(last);
                s.autoLoadLast = on;
                saveProfileToPrefs(last, s);
            } else {
                prefs.putBoolean("autoLoadGlobal", on);
            }
        });

        btnExportJson.setOnAction(e -> exportCurrentProfileToJson());
        btnImportJson.setOnAction(e -> importProfileFromJson());

        var rowSource = new HBox(10, lblSource, cmbDataSource, txtDataFile, btnBrowseData);
        rowSource.setAlignment(Pos.CENTER_LEFT);


        var profileControls = new HBox(8,
                txtProfileName, btnSaveProfile, btnLoadProfile, btnDeleteProfile,
                new Separator(), btnUpOrder, btnDownOrder,
                new Separator(), btnExportJson, btnImportJson
        );


        profileControls.setAlignment(Pos.CENTER_LEFT);

        var left = new VBox(10,
                new Label("Data"),
                rowSource,
                new Separator(),
                new Label("Profiles"),
                lstProfiles,
                profileControls,
                chkAutoLoadLast
        );
        left.setPadding(new Insets(14));

        refreshProfileList();
        javafx.application.Platform.runLater(this::autoLoadLastProfileIfWanted);

        return new Tab("Properties", left);
    }

    // --- DTOs für JSON (innerhalb von JavaFx, z.B. direkt über AppSettings oder darunter) ---
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class MatFilterDTO {
        public String formula;      // z.B. "Al" oder "Rh0.8Cu0.2"
        public double density;      // g/cm^3
        public double thicknessCm;  // cm
        public boolean use;         // Use-Checkbox
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class SegmentDTO {
        public boolean enabled;
        public String expr;     // Ausdruck (mXparser), wir verwenden nur EXPR-Segmente
        public double a, b;
        public boolean inclA, inclB;
        public boolean clamp;
        public double yMin, yMax;
    }


    // Speichert die aktuelle Reihenfolge der Profilnamen (eine Zeile pro Name)
    private void saveProfilesOrderToPrefs(java.util.List<String> names) {
        prefs.put("profilesOrder", String.join("\n", names));
    }

    // Lädt die gewünschte Reihenfolge; falls leer/nicht vorhanden => leere Liste
    private java.util.List<String> loadProfilesOrderFromPrefs() {
        String s = prefs.get("profilesOrder", "");
        if (s == null || s.isBlank()) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(java.util.Arrays.asList(s.split("\\R")));
    }

    private static void putMaybeDouble(Preferences p, String key, Double v) {
        if (v == null) {
            p.put(key, "");            // leer bleibt leer
        } else {
            p.put(key, v.toString());  // als String speichern
        }
    }

    private static Double getMaybeDouble(Preferences p, String key) {
        String s = p.get(key, null);   // nicht getDouble!
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (Exception ignore) {
            return null;
        }
    }

    private static void putMaybeString(Preferences p, String key, String v) {
        p.put(key, v == null ? "" : v);
    }

    private static String getMaybeString(Preferences p, String key) {
        String s = p.get(key, "");
        return (s == null || s.isBlank()) ? "" : s;
    }




    // --- Konzentrations-Tab: Modell/State ---
    private final ObservableList<java.nio.file.Path> concPaths = FXCollections.observableArrayList();
    private static final java.util.Set<String> SUPPORTED_EXTS =
            java.util.Set.of("asr", "fit");


    private Tab buildConcentrationsTab() {
        Label title = new Label("Konzentrationen – Dateien");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Liste (nur Dateinamen)
        ListView<java.nio.file.Path> list = new ListView<>(concPaths);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(java.nio.file.Path p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getFileName().toString());
            }
        });
        // Doppelklick -> Popup/Table-Viewer (falls du den schon hast)
        list.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                var sel = new java.util.ArrayList<>(list.getSelectionModel().getSelectedItems());
                if (sel.isEmpty()) {
                    var one = list.getSelectionModel().getSelectedItem();
                    if (one != null) sel.add(one);
                }
                if (!sel.isEmpty()) openConcPopup(sel, list.getScene()!=null ? list.getScene().getWindow() : null);
            }
        });
        // Drag & Drop
        list.setOnDragOver(ev -> {
            var db = ev.getDragboard();
            if (db.hasFiles() && db.getFiles().stream().anyMatch(f -> isSupported(f.toPath()))) {
                ev.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            ev.consume();
        });
        list.setOnDragDropped(ev -> {
            var db = ev.getDragboard();
            boolean ok = false;
            if (db.hasFiles()) {
                addFiles(db.getFiles().stream().map(java.io.File::toPath).toList());
                ok = true;
            }
            ev.setDropCompleted(ok);
            ev.consume();
        });

        // Buttons oben
        Button btnAdd    = new Button("Dateien hinzufügen…");
        Button btnOpen   = new Button("Öffnen (Tabelle)");
        Button btnRemove = new Button("Entfernen");
        Button btnClear  = new Button("Leeren");

        btnAdd.setOnAction(e -> chooseAndAddFiles(btnAdd.getScene()!=null ? btnAdd.getScene().getWindow() : null));
        btnOpen.setOnAction(e -> {
            var sel = new java.util.ArrayList<>(list.getSelectionModel().getSelectedItems());
            if (sel.isEmpty()) {
                var one = list.getSelectionModel().getSelectedItem();
                if (one != null) sel.add(one);
            }
            if (!sel.isEmpty()) openConcPopup(sel, list.getScene()!=null ? list.getScene().getWindow() : null);
        });
        btnRemove.setOnAction(e -> {
            var sel = new java.util.ArrayList<>(list.getSelectionModel().getSelectedItems());
            concPaths.removeAll(sel);
        });
        btnClear.setOnAction(e -> concPaths.clear());

        HBox controls = new HBox(8, btnAdd, btnOpen, new Separator(), btnRemove, btnClear);
        controls.setAlignment(Pos.CENTER_LEFT);

        // --- NUR ComboBox unter der Liste (Tabelle entfällt) ---
        Label selLbl = new Label("Datei-Auswahl:");
        ComboBox<java.nio.file.Path> cboFile = new ComboBox<>(concPaths);

        // Anzeige nur Dateiname
        javafx.util.Callback<ListView<java.nio.file.Path>, ListCell<java.nio.file.Path>> pathCellFactory = lv -> new ListCell<>() {
            @Override protected void updateItem(java.nio.file.Path p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getFileName().toString());
            }
        };
        cboFile.setCellFactory(pathCellFactory);
        cboFile.setButtonCell(pathCellFactory.call(null));
        cboFile.setPrefWidth(320);

        Button btnShowSelected = new Button("Anzeigen");
        btnShowSelected.setOnAction(e -> {
            var p = cboFile.getValue();
            if (p != null) openConcPopup(java.util.List.of(p), cboFile.getScene()!=null ? cboFile.getScene().getWindow() : null);
        });

        // deaktivieren, wenn leer/keine Auswahl
        cboFile.disableProperty().bind(Bindings.isEmpty(concPaths));
        btnShowSelected.disableProperty().bind(Bindings.isNull(cboFile.valueProperty()));

        HBox underList = new HBox(10, selLbl, cboFile, btnShowSelected);
        underList.setAlignment(Pos.CENTER_LEFT);

        // Layout: einfacher Stack
        VBox root = new VBox(10, title, controls, list, underList);
        root.setPadding(new Insets(14));
        VBox.setVgrow(list, Priority.ALWAYS); // <— wichtig: Liste wächst mit

        return new Tab("Konzentrationen", root);
    }




    private void chooseAndAddFiles(Window owner) {
        var fc = new javafx.stage.FileChooser();
        fc.setTitle("ASR/FIT-Dateien auswählen");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("ASR/FIT", "*.asr", "*.fit"),
                new javafx.stage.FileChooser.ExtensionFilter("Alle Dateien", "*.*")
        );
        var chosen = fc.showOpenMultipleDialog(owner);
        if (chosen != null && !chosen.isEmpty()) {
            addFiles(chosen.stream().map(java.io.File::toPath).toList());
        }
    }

    private void addFiles(java.util.Collection<java.nio.file.Path> paths) {
        for (var p : paths) {
            if (!isSupported(p)) continue;
            if (!concPaths.contains(p)) concPaths.add(p); // de-dupe
        }
    }

    private static boolean isSupported(java.nio.file.Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        String ext = (i >= 0 ? n.substring(i+1) : "").toLowerCase(java.util.Locale.ROOT);
        return SUPPORTED_EXTS.contains(ext);
    }

    // Platzhalter: hier später TableView öffnen
    private void openConcTable(java.nio.file.Path p) {
        if (p == null) return;
        new Alert(Alert.AlertType.INFORMATION,
                "Tabellenansicht folgt.\nDatei: " + p.getFileName()
        ).showAndWait();
    }


    // --- Konzentrations-Zeilentyp für die editierbare Tabelle ---
    public static class ConcRow {
        public final javafx.beans.property.StringProperty element    = new javafx.beans.property.SimpleStringProperty();
        public final javafx.beans.property.StringProperty transition = new javafx.beans.property.SimpleStringProperty(); // "K" | "L"
        public final javafx.beans.property.DoubleProperty intensity  = new javafx.beans.property.SimpleDoubleProperty();
        public ConcRow(String element, String transition, double intensity) {
            this.element.set(element);
            this.transition.set(transition);
            this.intensity.set(intensity);
        }
    }



    private java.util.Set<String> parseAnodeElements(String formula) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (formula == null) return out;
        String f = formula.trim();
        if (f.isEmpty()) return out;

        // 1) bevorzugt: dein Parser
        try {
            Funktionen fk = new FunktionenImpl();
            Verbindung v = fk.parseVerbindung(f, 0.0, 1.0, 0.1, DATA_FILE);
            String[] syms = v.getSymbole();
            if (syms != null) {
                java.util.Collections.addAll(out, syms);
                return out;
            }
        } catch (Throwable ignore) { }

        // 2) Fallback: Enum-Symbole, die im String vorkommen
        for (Elementsymbole e : Elementsymbole.values()) {
            String sym = e.name();
            if (f.matches(".*(?<![A-Za-z])" + java.util.regex.Pattern.quote(sym) + "(?![a-z]).*")) {
                out.add(sym);
            }
        }
        return out;
    }



    private javafx.collections.ObservableList<ConcRow> buildRowsForFiles(
            java.util.List<java.nio.file.Path> files,
            String tubeMaterialStr
    ) {
        // Anoden-Elemente
        java.util.Set<String> exclude = parseAnodeElements(tubeMaterialStr);

        // Sammeln: Element -> {K, L}
        java.util.Map<String, double[]> sums = new java.util.LinkedHashMap<>();

        for (var p : files) {
            String name = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
            try {
                if (name.endsWith(".fit")) {
                    var fitAreas = FitParserPymca.extractFitAreas(p.toString());
                    var grouped  = FitParserPymca.groupByElement(fitAreas);
                    var counts   = FitParserPymca.getCountsPerElement(grouped); // int[]{K,L}

                    for (var e : counts.entrySet()) {
                        sums.computeIfAbsent(e.getKey(), k -> new double[2]);
                        sums.get(e.getKey())[0] += e.getValue()[0];
                        sums.get(e.getKey())[1] += e.getValue()[1];
                    }

                } else if (name.endsWith(".asr")) {
                    var peaks   = AsrParser.extractPeaks(p);
                    var grouped = AsrParser.groupByElement(peaks);
                    var counts  = AsrParser.getCountsPerElement(grouped); // int[]{K,L}

                    for (var e : counts.entrySet()) {
                        sums.computeIfAbsent(e.getKey(), k -> new double[2]);
                        sums.get(e.getKey())[0] += e.getValue()[0];
                        sums.get(e.getKey())[1] += e.getValue()[1];
                    }
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Fehler beim Einlesen:\n" + p + "\n\n" + ex.getMessage()).showAndWait();
            }
        }

        // In Zeilen nach deinen Regeln
        var rows = javafx.collections.FXCollections.<ConcRow>observableArrayList();
        for (var e : sums.entrySet()) {
            String ele = e.getKey();
            if (exclude.contains(ele)) continue;

            double k = e.getValue()[0];
            double l = e.getValue()[1];

            boolean hasK = k != 0.0;
            boolean hasL = l != 0.0;

            if (!hasK && !hasL) continue;
            if (hasK && hasL) { rows.add(new ConcRow(ele, "K", k)); continue; }
            if (hasK)          { rows.add(new ConcRow(ele, "K", k)); }
            else               { rows.add(new ConcRow(ele, "L", l)); }
        }

        rows.sort(java.util.Comparator.comparing(r -> r.element.get()));
        return rows;
    }

    private javafx.collections.ObservableList<ConcRow> buildRowsForSingleFile(java.nio.file.Path file) {
        return buildRowsForFiles(java.util.List.of(file), tubeMaterial == null ? null : tubeMaterial.getText());
    }




    private void openConcPopup(java.util.List<java.nio.file.Path> files, Window owner) {
        Stage popup = new Stage();
        popup.setTitle("Datenansicht (" + files.size() + ")");
        if (owner != null) popup.initOwner(owner);
        popup.setAlwaysOnTop(true);

        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);

        // Liste aller Elementsymbole als Strings
        var allSymbols = FXCollections.observableArrayList(
                java.util.Arrays.stream(Elementsymbole.values()).map(Enum::name).toList()
        );

        for (var p : files) {
            // flüchtige (neu geparste) Zeilen nur für dieses Tab
            var rows = getOrBuildRowsForFile(p);


            TableView<ConcRow> table = new TableView<>(rows);
            table.setEditable(true);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // --- Spalten ---

            // Element (ComboBox, editiert Property direkt)
            TableColumn<ConcRow, String> cElement = new TableColumn<>("Element");
            cElement.setCellValueFactory(data -> data.getValue().element);
            cElement.setCellFactory(ComboBoxTableCell.forTableColumn(allSymbols));
            cElement.setEditable(true);

            // Übergang (K/L)
            TableColumn<ConcRow, String> cTrans = new TableColumn<>("Übergang");
            cTrans.setCellValueFactory(data -> data.getValue().transition);
            cTrans.setCellFactory(ComboBoxTableCell.forTableColumn(
                    FXCollections.observableArrayList("K", "L")
            ));
            cTrans.setEditable(true);

            // Intensität (editierbares Number-Feld)
            TableColumn<ConcRow, Number> cInt = new TableColumn<>("Intensität");
            cInt.setCellValueFactory(data -> data.getValue().intensity);
            cInt.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Number>() {
                @Override public String toString(Number n) {
                    return n == null ? "" : String.format(java.util.Locale.ROOT, "%.6g", n.doubleValue());
                }
                @Override public Number fromString(String s) {
                    if (s == null || s.isBlank()) return 0.0;
                    return Double.parseDouble(s.trim().replace(',', '.'));
                }
            }));
            cInt.setOnEditCommit(ev -> {
                ConcRow r = ev.getRowValue();
                Number nv = ev.getNewValue();
                if (nv != null && Double.isFinite(nv.doubleValue())) {
                    r.intensity.set(nv.doubleValue());
                }
            });
            cInt.setEditable(true);

            table.getColumns().setAll(cElement, cTrans, cInt);

            // --- Aktionen unter der Tabelle: + Neu / Löschen ---
            Button btnAdd = new Button("+ Neu");
            btnAdd.setOnAction(e ->
                    rows.add(new ConcRow(Elementsymbole.values()[0].name(), "K", 0.0))
            );

            Button btnDel = new Button("Löschen");
            btnDel.setOnAction(e -> {
                var sel = new java.util.ArrayList<>(table.getSelectionModel().getSelectedItems());
                rows.removeAll(sel);
            });

            HBox actions = new HBox(8, btnAdd, btnDel);
            actions.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(10, table, actions);
            content.setPadding(new Insets(12));
            VBox.setVgrow(table, Priority.ALWAYS);

            Tab tab = new Tab(p.getFileName().toString(), content);
            tab.setClosable(true);
            tp.getTabs().add(tab);
        }

        Scene sc = new Scene(new BorderPane(tp), 800, 520);
        popup.setScene(sc);
        popup.show();
    }
    // Edits nur während der Laufzeit merken (kein Persistieren auf Platte)
    private final java.util.Map<java.nio.file.Path, javafx.collections.ObservableList<ConcRow>>
            concSessionCache = new java.util.LinkedHashMap<>();

    private javafx.collections.ObservableList<ConcRow> getOrBuildRowsForFile(java.nio.file.Path file) {
        return concSessionCache.computeIfAbsent(file, f ->
                buildRowsForSingleFile(f) // deine bestehende Parser-Logik -> ObservableList<ConcRow>
        );
    }

    private void resetConcForFile(java.nio.file.Path file) { concSessionCache.remove(file); }
    private void resetAllConc() { concSessionCache.clear(); }







    public static void main(String[] args) { launch(args); }
}
