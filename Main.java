import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Optional;

public class Main extends Application {

    private TableView<product> table = new TableView<>();
    private ObservableList<product> data = FXCollections.observableArrayList();
    private FilteredList<product> filteredData;
    private BarChart<String, Number> chart;
    private Label avgSILabel, totalProductsLabel, bestProductLabel;
    private DecimalFormat df = new DecimalFormat("#.##");
    private Stage primaryStage;
    private Database dbManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.dbManager = Database.getInstance();

        VBox inputSection = createInputSection();
        HBox controlSection = createControlSection();
        VBox tableSection = createTableSection();
        HBox statsPanel = createStatsPanel();
        VBox chartSection = createChartSection();

        BorderPane mainLayout = new BorderPane();
        VBox topSection = new VBox(10, inputSection, controlSection, statsPanel);
        topSection.setPadding(new Insets(10));

        mainLayout.setTop(topSection);
        mainLayout.setCenter(tableSection);
        mainLayout.setBottom(chartSection);

        Scene scene = new Scene(mainLayout, 1200, 800);
        primaryStage.setTitle("Binovator - Sustainability Rating System");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadInitialData();
        updateStatistics();
        updateChart();
    }

    // -------------------- INPUT SECTION --------------------
    private VBox createInputSection() {
        Label titleLabel = new Label("Add New Product");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        TextField materialField = new TextField(); materialField.setPromptText("Material (0-10)");
        TextField energyField = new TextField(); energyField.setPromptText("Energy (0-10)");
        TextField wasteField = new TextField(); wasteField.setPromptText("Waste (0-10)");
        TextField longevityField = new TextField(); longevityField.setPromptText("Longevity (0-10)");
        TextField socialField = new TextField(); socialField.setPromptText("Social (0-10)");

        Button addButton = new Button("Add Product");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setOnAction(e -> handleAddProduct(nameField, materialField, energyField, wasteField, longevityField, socialField));

        HBox inputBox = new HBox(10, nameField, materialField, energyField, wasteField, longevityField, socialField, addButton);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(5, titleLabel, inputBox);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5;");
        return section;
    }

    // -------------------- ADD PRODUCT --------------------
    private void handleAddProduct(TextField nameF, TextField matF, TextField engF, TextField wasteF, TextField lonF, TextField socF) {
        String name = nameF.getText().trim();
        if(name.isEmpty()) { showAlert("Input Error", "Product name cannot be empty.", Alert.AlertType.ERROR); return; }

        try {
            double material = parseAndValidateScore(matF.getText());
            double energy = parseAndValidateScore(engF.getText());
            double waste = parseAndValidateScore(wasteF.getText());
            double longevity = parseAndValidateScore(lonF.getText());
            double social = parseAndValidateScore(socF.getText());

            double si = calculateSI(material, energy, waste, longevity, social);
            String grade = getGrade(si);

            product p = new product(0, name, material, energy, waste, longevity, social, si, grade);
            int id = dbManager.insertProduct(p);
            p.setId(id);
            data.add(p);

            nameF.clear(); matF.clear(); engF.clear(); wasteF.clear(); lonF.clear(); socF.clear();

            updateStatistics();
            updateChart();
            showAlert("Success", "Product added! ID: " + id, Alert.AlertType.INFORMATION);

        } catch (Exception e) { showAlert("Error", e.getMessage(), Alert.AlertType.ERROR); }
    }

    private double parseAndValidateScore(String scoreStr) throws Exception {
        if(scoreStr == null || scoreStr.trim().isEmpty()) throw new Exception("Score cannot be empty.");
        double value = Double.parseDouble( scoreStr);
        if(value < 0 || value > 10) throw new Exception("Score must be 0-10.");
        return value;
    }

    private double calculateSI(double m, double e, double w, double l, double s) {
        return m*0.25 + e*0.25 + w*0.25 + l*0.20 + s*0.05;
    }

    private String getGrade(double si) {
        if(si >= 9.5) return "A+*";
        if(si >= 9.0) return "A+";
        if(si >= 8.5) return "A";
        if(si >= 7.5) return "B+";
        if(si >= 6.5) return "B";
        if(si >= 5.0) return "C+";
        return "C";
    }

    // -------------------- CONTROL SECTION --------------------
    private HBox createControlSection() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search products...");
        filteredData = new FilteredList<>(data, p -> true);
        table.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(p -> {
                if(newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return p.getName().toLowerCase().contains(lower) || p.getGrade().toLowerCase().contains(lower);
            });
            updateStatistics();
        });

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("-fx-background-color:#f44336; -fx-text-fill:white;");
        deleteButton.setOnAction(e -> handleDeleteProduct());

        Button exportButton = new Button("Export to CSV");
        exportButton.setStyle("-fx-background-color:#2196F3; -fx-text-fill:white;");
        exportButton.setOnAction(e -> exportToCSV());

        HBox box = new HBox(10, new Label("Search:"), searchField, deleteButton, exportButton);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5));
        return box;
    }

    private void handleDeleteProduct() {
        product selected = table.getSelectionModel().getSelectedItem();
        if(selected == null) { showAlert("Error","Select a product to delete.", Alert.AlertType.WARNING); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Product: "+selected.getName());
        confirm.setContentText("Are you sure?");
        Optional<ButtonType> res = confirm.showAndWait();
        if(res.isPresent() && res.get() == ButtonType.OK) {
            try { dbManager.deleteProduct(selected); data.remove(selected); updateStatistics(); updateChart();
                showAlert("Success","Deleted successfully!", Alert.AlertType.INFORMATION);
            } catch (SQLException ex) { showAlert("Error", ex.getMessage(), Alert.AlertType.ERROR); }
        }
    }

    // -------------------- TABLE SECTION --------------------
    private VBox createTableSection() {
        TableColumn<product,Integer> idCol=new TableColumn<>("ID"); idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<product,String> nameCol=new TableColumn<>("Name"); nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<product,Double> matCol=new TableColumn<>("Material"); matCol.setCellValueFactory(new PropertyValueFactory<>("material"));
        TableColumn<product,Double> engCol=new TableColumn<>("Energy"); engCol.setCellValueFactory(new PropertyValueFactory<>("energy"));
        TableColumn<product,Double> wasteCol=new TableColumn<>("Waste"); wasteCol.setCellValueFactory(new PropertyValueFactory<>("waste"));
        TableColumn<product,Double> lonCol=new TableColumn<>("Longevity"); lonCol.setCellValueFactory(new PropertyValueFactory<>("longevity"));
        TableColumn<product,Double> socCol=new TableColumn<>("Social"); socCol.setCellValueFactory(new PropertyValueFactory<>("social"));
        TableColumn<product,Double> siCol=new TableColumn<>("SI"); siCol.setCellValueFactory(new PropertyValueFactory<>("si"));
        TableColumn<product,String> gradeCol=new TableColumn<>("Grade"); gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));

        table.getColumns().addAll(idCol,nameCol,matCol,engCol,wasteCol,lonCol,socCol,siCol,gradeCol);
        VBox box = new VBox(5,new Label("Products Database"),table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table,Priority.ALWAYS);
        return box;
    }

    // -------------------- STATS SECTION --------------------
    private HBox createStatsPanel() {
        avgSILabel = new Label("0.00"); totalProductsLabel = new Label("0"); bestProductLabel = new Label("N/A");

        VBox avgBox=new VBox(5,new Label("Avg SI"),avgSILabel); avgBox.setAlignment(Pos.CENTER);
        VBox totalBox=new VBox(5,new Label("Total"),totalProductsLabel); totalBox.setAlignment(Pos.CENTER);
        VBox bestBox=new VBox(5,new Label("Best Product"),bestProductLabel); bestBox.setAlignment(Pos.CENTER);

        HBox hbox = new HBox(20,avgBox,totalBox,bestBox); hbox.setAlignment(Pos.CENTER); hbox.setPadding(new Insets(10));
        return hbox;
    }

    private void updateStatistics() {
        if(filteredData.isEmpty()) { avgSILabel.setText("0.00"); totalProductsLabel.setText("0"); bestProductLabel.setText("N/A"); return; }
        double sum=0; product best=filteredData.get(0);
        for(product p:filteredData){ sum+=p.getSi(); if(p.getSi()>best.getSi()) best=p; }
        avgSILabel.setText(df.format(sum/filteredData.size()));
        totalProductsLabel.setText(String.valueOf(filteredData.size()));
        bestProductLabel.setText(best.getName()+" ("+df.format(best.getSi())+")");
    }

    // -------------------- CHART SECTION --------------------
    private VBox createChartSection() {
        CategoryAxis x=new CategoryAxis(); NumberAxis y=new NumberAxis(0,10,1);
        chart=new BarChart<>(x,y); chart.setLegendVisible(false); chart.setPrefHeight(250);
        x.setLabel("Product"); y.setLabel("SI Score");
        VBox box=new VBox(chart); box.setPadding(new Insets(10));
        return box;
    }

    private void updateChart() {
        chart.getData().clear();
        XYChart.Series<String,Number> series=new XYChart.Series<>();
        filteredData.stream().limit(Math.min(filteredData.size(),10)).forEach(p->series.getData().add(new XYChart.Data<>(p.getName(),p.getSi())));
        chart.getData().add(series);
    }

    // -------------------- CSV EXPORT --------------------
    private void exportToCSV() {
        FileChooser fc=new FileChooser(); fc.setTitle("Save CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv")); fc.setInitialFileName("binovator_export.csv");
        File file=fc.showSaveDialog(primaryStage);
        if(file!=null){
            try(FileWriter fw=new FileWriter(file)){
                fw.write("ID,Name,Material,Energy,Waste,Longevity,Social,SI,Grade\n");
                for(product p:data){
                    fw.write(String.format("%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                            p.getId(),p.getName(),p.getMaterial(),p.getEnergy(),p.getWaste(),p.getLongevity(),p.getSocial(),p.getSi(),p.getGrade()));
                }
                showAlert("Export Success","Saved as "+file.getName(), Alert.AlertType.INFORMATION);
            }catch(IOException e){ showAlert("Export Error",e.getMessage(),Alert.AlertType.ERROR);}
        }
    }
    // -------------------- LOAD INITIAL DATA --------------------
    private void loadInitialData() {
        try{ data.addAll(dbManager.loadProducts()); } catch (SQLException e){ showAlert("Error","Failed to load DB",Alert.AlertType.ERROR);}
    }
    private void showAlert(String title,String msg,Alert.AlertType type){
        Alert alert=new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }
}