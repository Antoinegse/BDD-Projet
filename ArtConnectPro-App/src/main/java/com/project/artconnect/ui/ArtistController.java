package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class ArtistController {
    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist> artistTable;
    @FXML private TableColumn<Artist, String> nameColumn;
    @FXML private TableColumn<Artist, String> cityColumn;
    @FXML private TableColumn<Artist, String> emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;
    @FXML private TableColumn<Artist, String> disciplineColumn;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));
        disciplineColumn.setCellValueFactory(new PropertyValueFactory<>("disciplinesDisplay"));

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showArtistDialog(null).ifPresent(artist -> {
            artistService.createArtist(artist);
            refreshTable();
        });
    }

    @FXML
    private void handleEdit() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner un artiste à modifier.");
            return;
        }
        showArtistDialog(selected).ifPresent(updated -> {
            artistService.updateArtist(updated);
            refreshTable();
        });
    }

    @FXML
    private void handleDelete() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélection requise", "Veuillez sélectionner un artiste à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'artiste « " + selected.getName() + " » ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            artistService.deleteArtist(selected.getName());
            refreshTable();
        });
    }

    private Optional<Artist> showArtistDialog(Artist existing) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel artiste" : "Modifier l'artiste");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField  = new TextField();  nameField.setPromptText("Nom complet");
        TextField cityField  = new TextField();  cityField.setPromptText("Ville");
        TextField emailField = new TextField();  emailField.setPromptText("Email");
        TextField yearField  = new TextField();  yearField.setPromptText("Année de naissance");
        TextField bioField   = new TextField();  bioField.setPromptText("Biographie");
        TextField phoneField = new TextField();  phoneField.setPromptText("Téléphone");

        if (existing != null) {
            if (existing.getName()         != null) nameField.setText(existing.getName());
            if (existing.getCity()         != null) cityField.setText(existing.getCity());
            if (existing.getContactEmail() != null) emailField.setText(existing.getContactEmail());
            if (existing.getBirthYear()    != null) yearField.setText(existing.getBirthYear().toString());
            if (existing.getBio()          != null) bioField.setText(existing.getBio());
            if (existing.getPhone()        != null) phoneField.setText(existing.getPhone());
        }

        grid.add(new Label("Nom :"),       0, 0); grid.add(nameField,  1, 0);
        grid.add(new Label("Ville :"),     0, 1); grid.add(cityField,  1, 1);
        grid.add(new Label("Email :"),     0, 2); grid.add(emailField, 1, 2);
        grid.add(new Label("Né(e) en :"), 0, 3); grid.add(yearField,  1, 3);
        grid.add(new Label("Bio :"),       0, 4); grid.add(bioField,   1, 4);
        grid.add(new Label("Tél :"),       0, 5); grid.add(phoneField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        nameField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Artist a = existing != null ? existing : new Artist();
            a.setName(nameField.getText().trim());
            a.setCity(cityField.getText().trim());
            a.setContactEmail(emailField.getText().trim());
            a.setBio(bioField.getText().trim());
            a.setPhone(phoneField.getText().trim());
            a.setActive(true);
            try { a.setBirthYear(Integer.parseInt(yearField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            return a;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
