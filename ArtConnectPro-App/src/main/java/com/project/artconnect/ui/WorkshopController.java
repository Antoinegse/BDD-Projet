package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkshopController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private TableView<Workshop> workshopTable;
    @FXML private TableColumn<Workshop, String> titleColumn;
    @FXML private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML private TableColumn<Workshop, String> instructorColumn;
    @FXML private TableColumn<Workshop, Double> priceColumn;
    @FXML private TableColumn<Workshop, String> levelColumn;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService   artistService   = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null
                        ? cellData.getValue().getInstructor().getName() : "Unknown"));

        levelFilter.setItems(FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String level = levelFilter.getValue();
        List<Workshop> filtered = workshopService.getAllWorkshops().stream()
                .filter(w -> query.isEmpty() ||
                        (w.getTitle()    != null && w.getTitle().toLowerCase().contains(query)) ||
                        (w.getLocation() != null && w.getLocation().toLowerCase().contains(query)) ||
                        (w.getInstructor() != null && w.getInstructor().getName() != null &&
                                w.getInstructor().getName().toLowerCase().contains(query)))
                .filter(w -> level == null || level.isEmpty() ||
                        (w.getLevel() != null && w.getLevel().equals(level)))
                .collect(Collectors.toList());
        workshopTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        levelFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showWorkshopDialog(null).ifPresent(ws -> {
            workshopService.createWorkshop(ws);
            refreshTable();
        });
    }

    @FXML
    private void handleEdit() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez un atelier à modifier."); return; }
        showWorkshopDialog(selected).ifPresent(updated -> {
            workshopService.updateWorkshop(updated);
            refreshTable();
        });
    }

    @FXML
    private void handleDelete() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez un atelier à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'atelier « " + selected.getTitle() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            workshopService.deleteWorkshop(selected.getTitle());
            refreshTable();
        });
    }

    private Optional<Workshop> showWorkshopDialog(Workshop existing) {
        Dialog<Workshop> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel atelier" : "Modifier l'atelier");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField    = new TextField(); titleField.setPromptText("Titre");
        TextField dateField     = new TextField(); dateField.setPromptText("Date (dd/MM/yyyy HH:mm)");
        TextField priceField    = new TextField(); priceField.setPromptText("Prix (€)");
        TextField locationField = new TextField(); locationField.setPromptText("Lieu");
        TextField durationField = new TextField(); durationField.setPromptText("Durée (min)");
        TextField maxField      = new TextField(); maxField.setPromptText("Nb max participants");
        ComboBox<String> levelCombo = new ComboBox<>(
                FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        ComboBox<Artist> instructorCombo = new ComboBox<>(
                FXCollections.observableArrayList(artistService.getAllArtists()));

        if (existing != null) {
            if (existing.getTitle()    != null) titleField.setText(existing.getTitle());
            if (existing.getDate()     != null) dateField.setText(existing.getDate().format(DTF));
            priceField.setText(String.valueOf(existing.getPrice()));
            if (existing.getLocation() != null) locationField.setText(existing.getLocation());
            durationField.setText(String.valueOf(existing.getDurationMinutes()));
            maxField.setText(String.valueOf(existing.getMaxParticipants()));
            if (existing.getLevel()      != null) levelCombo.setValue(existing.getLevel());
            if (existing.getInstructor() != null) instructorCombo.setValue(existing.getInstructor());
        } else {
            levelCombo.setValue("Beginner");
        }

        grid.add(new Label("Titre :"),        0, 0); grid.add(titleField,    1, 0);
        grid.add(new Label("Instructeur :"),  0, 1); grid.add(instructorCombo,1,1);
        grid.add(new Label("Date :"),         0, 2); grid.add(dateField,     1, 2);
        grid.add(new Label("Niveau :"),       0, 3); grid.add(levelCombo,    1, 3);
        grid.add(new Label("Prix (€) :"),     0, 4); grid.add(priceField,    1, 4);
        grid.add(new Label("Lieu :"),         0, 5); grid.add(locationField, 1, 5);
        grid.add(new Label("Durée (min) :"),  0, 6); grid.add(durationField, 1, 6);
        grid.add(new Label("Max participants :"), 0, 7); grid.add(maxField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        titleField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Workshop w = existing != null ? existing : new Workshop();
            w.setTitle(titleField.getText().trim());
            w.setInstructor(instructorCombo.getValue());
            w.setLevel(levelCombo.getValue());
            w.setLocation(locationField.getText().trim());
            try { w.setPrice(Double.parseDouble(priceField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            try { w.setDurationMinutes(Integer.parseInt(durationField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            try { w.setMaxParticipants(Integer.parseInt(maxField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            try { w.setDate(LocalDateTime.parse(dateField.getText().trim(), DTF)); }
            catch (DateTimeParseException ignored) {}
            return w;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
