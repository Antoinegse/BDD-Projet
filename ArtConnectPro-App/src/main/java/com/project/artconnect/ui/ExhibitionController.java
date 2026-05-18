package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrôleur autonome pour l'onglet Expositions.
 * FIX #6 : utilise ExhibitionService (indépendant de GalleryService pour le CRUD).
 * FIX #4 : tableau enrichi — galerie, début, fin, thème, commissaire, nb œuvres.
 */
public class ExhibitionController {

    @FXML private TextField searchField;
    @FXML private TableView<Exhibition> exhibitionTable;
    @FXML private TableColumn<Exhibition, String>  titleColumn;
    @FXML private TableColumn<Exhibition, String>  galleryColumn;
    @FXML private TableColumn<Exhibition, LocalDate> startColumn;
    @FXML private TableColumn<Exhibition, LocalDate> endColumn;
    @FXML private TableColumn<Exhibition, String>  themeColumn;
    @FXML private TableColumn<Exhibition, String>  curatorColumn;
    @FXML private TableColumn<Exhibition, Number>  artworksColumn;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final GalleryService    galleryService    = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        galleryColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getGallery() != null
                        ? cell.getValue().getGallery().getName() : "—"));
        startColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        themeColumn.setCellValueFactory(new PropertyValueFactory<>("theme"));
        curatorColumn.setCellValueFactory(new PropertyValueFactory<>("curatorName"));
        artworksColumn.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getArtworks() != null
                        ? cell.getValue().getArtworks().size() : 0));
        refreshData();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        List<Exhibition> filtered = exhibitionService.getAllExhibitions().stream()
                .filter(e -> query.isEmpty()
                        || (e.getTitle()       != null && e.getTitle().toLowerCase().contains(query))
                        || (e.getTheme()       != null && e.getTheme().toLowerCase().contains(query))
                        || (e.getCuratorName() != null && e.getCuratorName().toLowerCase().contains(query))
                        || (e.getGallery()     != null && e.getGallery().getName() != null
                                && e.getGallery().getName().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        exhibitionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        refreshData();
    }

    @FXML
    private void handleAdd() {
        showExhibitionDialog(null).ifPresent(exhibition -> {
            exhibitionService.createExhibition(exhibition);
            refreshData();
        });
    }

    @FXML
    private void handleEdit() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une exposition à modifier."); return; }
        showExhibitionDialog(selected).ifPresent(updated -> {
            exhibitionService.updateExhibition(updated);
            refreshData();
        });
    }

    @FXML
    private void handleDelete() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une exposition à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'exposition « " + selected.getTitle() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            exhibitionService.deleteExhibition(selected.getTitle());
            refreshData();
        });
    }

    private Optional<Exhibition> showExhibitionDialog(Exhibition existing) {
        Dialog<Exhibition> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle exposition" : "Modifier l'exposition");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField   = new TextField(); titleField.setPromptText("Titre");
        TextField themeField   = new TextField(); themeField.setPromptText("Thème");
        TextField curatorField = new TextField(); curatorField.setPromptText("Commissaire");
        TextField descField    = new TextField(); descField.setPromptText("Description");
        TextField startField   = new TextField(); startField.setPromptText("Début (AAAA-MM-JJ)");
        TextField endField     = new TextField(); endField.setPromptText("Fin (AAAA-MM-JJ)");
        ComboBox<Gallery> galleryCombo = new ComboBox<>(
                FXCollections.observableArrayList(galleryService.getAllGalleries()));

        if (existing != null) {
            if (existing.getTitle()       != null) titleField.setText(existing.getTitle());
            if (existing.getTheme()       != null) themeField.setText(existing.getTheme());
            if (existing.getCuratorName() != null) curatorField.setText(existing.getCuratorName());
            if (existing.getDescription() != null) descField.setText(existing.getDescription());
            if (existing.getStartDate()   != null) startField.setText(existing.getStartDate().toString());
            if (existing.getEndDate()     != null) endField.setText(existing.getEndDate().toString());
            if (existing.getGallery()     != null) galleryCombo.setValue(existing.getGallery());
        }

        grid.add(new Label("Titre :"),       0, 0); grid.add(titleField,   1, 0);
        grid.add(new Label("Galerie :"),     0, 1); grid.add(galleryCombo, 1, 1);
        grid.add(new Label("Thème :"),       0, 2); grid.add(themeField,   1, 2);
        grid.add(new Label("Commissaire :"), 0, 3); grid.add(curatorField, 1, 3);
        grid.add(new Label("Description :"), 0, 4); grid.add(descField,    1, 4);
        grid.add(new Label("Date début :"),  0, 5); grid.add(startField,   1, 5);
        grid.add(new Label("Date fin :"),    0, 6); grid.add(endField,     1, 6);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        titleField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Exhibition e = existing != null ? existing : new Exhibition();
            e.setTitle(titleField.getText().trim());
            e.setTheme(themeField.getText().trim());
            e.setCuratorName(curatorField.getText().trim());
            e.setDescription(descField.getText().trim());
            e.setGallery(galleryCombo.getValue());
            try { e.setStartDate(LocalDate.parse(startField.getText().trim())); } catch (DateTimeParseException ignored) {}
            try { e.setEndDate(LocalDate.parse(endField.getText().trim()));   } catch (DateTimeParseException ignored) {}
            return e;
        });

        return dialog.showAndWait();
    }

    private void refreshData() {
        exhibitionTable.setItems(FXCollections.observableArrayList(exhibitionService.getAllExhibitions()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
