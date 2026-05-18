package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArtworkController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Artwork> artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> statusColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService  artistService  = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null
                        ? cellData.getValue().getArtist().getName() : "Unknown"));

        statusFilter.setItems(FXCollections.observableArrayList(
                "FOR_SALE", "SOLD", "EXHIBITION", "PRIVATE"));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String status = statusFilter.getValue();
        List<Artwork> filtered = artworkService.getAllArtworks().stream()
                .filter(a -> query.isEmpty() ||
                        (a.getTitle() != null && a.getTitle().toLowerCase().contains(query)) ||
                        (a.getType()  != null && a.getType().toLowerCase().contains(query)) ||
                        (a.getArtist() != null && a.getArtist().getName() != null &&
                                a.getArtist().getName().toLowerCase().contains(query)))
                .filter(a -> status == null || status.isEmpty() ||
                        (a.getStatus() != null && a.getStatus().toString().equals(status)))
                .collect(Collectors.toList());
        artworkTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        statusFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showArtworkDialog(null).ifPresent(artwork -> {
            artworkService.createArtwork(artwork);
            refreshTable();
        });
    }

    @FXML
    private void handleEdit() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une oeuvre à modifier."); return; }
        showArtworkDialog(selected).ifPresent(updated -> {
            artworkService.updateArtwork(updated);
            refreshTable();
        });
    }

    @FXML
    private void handleDelete() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une oeuvre à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer « " + selected.getTitle() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            artworkService.deleteArtwork(selected.getTitle());
            refreshTable();
        });
    }

    private Optional<Artwork> showArtworkDialog(Artwork existing) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle oeuvre" : "Modifier l'oeuvre");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField  = new TextField(); titleField.setPromptText("Titre");
        TextField typeField   = new TextField(); typeField.setPromptText("Type (painting, sculpture…)");
        TextField mediumField = new TextField(); mediumField.setPromptText("Médium");
        TextField priceField  = new TextField(); priceField.setPromptText("Prix (€)");
        TextField yearField   = new TextField(); yearField.setPromptText("Année de création");

        ComboBox<Artist> artistCombo = new ComboBox<>(
                FXCollections.observableArrayList(artistService.getAllArtists()));
        ComboBox<Artwork.Status> statusCombo = new ComboBox<>(
                FXCollections.observableArrayList(Artwork.Status.values()));

        if (existing != null) {
            if (existing.getTitle()  != null) titleField.setText(existing.getTitle());
            if (existing.getType()   != null) typeField.setText(existing.getType());
            if (existing.getMedium() != null) mediumField.setText(existing.getMedium());
            priceField.setText(String.valueOf(existing.getPrice()));
            if (existing.getCreationYear() != null) yearField.setText(existing.getCreationYear().toString());
            artistCombo.setValue(existing.getArtist());
            statusCombo.setValue(existing.getStatus());
        } else {
            statusCombo.setValue(Artwork.Status.FOR_SALE);
        }

        grid.add(new Label("Titre :"),   0, 0); grid.add(titleField,  1, 0);
        grid.add(new Label("Artiste :"), 0, 1); grid.add(artistCombo, 1, 1);
        grid.add(new Label("Type :"),    0, 2); grid.add(typeField,   1, 2);
        grid.add(new Label("Médium :"),  0, 3); grid.add(mediumField, 1, 3);
        grid.add(new Label("Prix (€) :"),0, 4); grid.add(priceField,  1, 4);
        grid.add(new Label("Année :"),   0, 5); grid.add(yearField,   1, 5);
        grid.add(new Label("Statut :"),  0, 6); grid.add(statusCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        titleField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Artwork a = existing != null ? existing : new Artwork();
            a.setTitle(titleField.getText().trim());
            a.setType(typeField.getText().trim());
            a.setMedium(mediumField.getText().trim());
            a.setArtist(artistCombo.getValue());
            a.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : Artwork.Status.FOR_SALE);
            try { a.setPrice(Double.parseDouble(priceField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            try { a.setCreationYear(Integer.parseInt(yearField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            return a;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
