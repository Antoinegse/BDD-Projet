package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GalleryController {
    @FXML private TextField searchField;
    @FXML private TableView<Gallery> galleryTable;
    @FXML private TableColumn<Gallery, String> nameColumn;
    @FXML private TableColumn<Gallery, String> addressColumn;
    @FXML private TableColumn<Gallery, Double> ratingColumn;
    @FXML private TableColumn<Gallery, String> ownerColumn;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));
        ratingColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("rating"));
        ownerColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("ownerName"));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        List<Gallery> filtered = galleryService.getAllGalleries().stream()
                .filter(g -> query.isEmpty() ||
                        (g.getName()      != null && g.getName().toLowerCase().contains(query)) ||
                        (g.getAddress()   != null && g.getAddress().toLowerCase().contains(query)) ||
                        (g.getOwnerName() != null && g.getOwnerName().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        galleryTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showGalleryDialog(null).ifPresent(gallery -> {
            galleryService.createGallery(gallery);
            refreshTable();
        });
    }

    @FXML
    private void handleEdit() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une galerie à modifier."); return; }
        showGalleryDialog(selected).ifPresent(updated -> {
            galleryService.updateGallery(updated);
            refreshTable();
        });
    }

    @FXML
    private void handleDelete() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez une galerie à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la galerie « " + selected.getName() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            galleryService.deleteGallery(selected.getName());
            refreshTable();
        });
    }

    private Optional<Gallery> showGalleryDialog(Gallery existing) {
        Dialog<Gallery> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle galerie" : "Modifier la galerie");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField    = new TextField(); nameField.setPromptText("Nom de la galerie");
        TextField addressField = new TextField(); addressField.setPromptText("Adresse");
        TextField ownerField   = new TextField(); ownerField.setPromptText("Propriétaire");
        TextField phoneField   = new TextField(); phoneField.setPromptText("Téléphone");
        TextField hoursField   = new TextField(); hoursField.setPromptText("Horaires (ex: 9h-18h)");
        TextField ratingField  = new TextField(); ratingField.setPromptText("Note (0.0 - 5.0)");
        TextField websiteField = new TextField(); websiteField.setPromptText("Site web");

        if (existing != null) {
            if (existing.getName()         != null) nameField.setText(existing.getName());
            if (existing.getAddress()      != null) addressField.setText(existing.getAddress());
            if (existing.getOwnerName()    != null) ownerField.setText(existing.getOwnerName());
            if (existing.getContactPhone() != null) phoneField.setText(existing.getContactPhone());
            if (existing.getOpeningHours() != null) hoursField.setText(existing.getOpeningHours());
            ratingField.setText(String.valueOf(existing.getRating()));
            if (existing.getWebsite()      != null) websiteField.setText(existing.getWebsite());
        }

        grid.add(new Label("Nom :"),       0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Adresse :"),   0, 1); grid.add(addressField, 1, 1);
        grid.add(new Label("Propriétaire :"), 0, 2); grid.add(ownerField,1, 2);
        grid.add(new Label("Tél :"),       0, 3); grid.add(phoneField,   1, 3);
        grid.add(new Label("Horaires :"),  0, 4); grid.add(hoursField,   1, 4);
        grid.add(new Label("Note :"),      0, 5); grid.add(ratingField,  1, 5);
        grid.add(new Label("Site web :"),  0, 6); grid.add(websiteField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        nameField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Gallery g = existing != null ? existing : new Gallery();
            g.setName(nameField.getText().trim());
            g.setAddress(addressField.getText().trim());
            g.setOwnerName(ownerField.getText().trim());
            g.setContactPhone(phoneField.getText().trim());
            g.setOpeningHours(hoursField.getText().trim());
            g.setWebsite(websiteField.getText().trim());
            try { g.setRating(Double.parseDouble(ratingField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            return g;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        galleryTable.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
