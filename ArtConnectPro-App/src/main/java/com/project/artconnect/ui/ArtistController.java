package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ArtistController {

    // toolbar
    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;

    // table
    @FXML private TableView<Artist>              artistTable;
    @FXML private TableColumn<Artist, String>    nameColumn;
    @FXML private TableColumn<Artist, String>    cityColumn;
    @FXML private TableColumn<Artist, String>    emailColumn;
    @FXML private TableColumn<Artist, Integer>   yearColumn;
    @FXML private TableColumn<Artist, String>    disciplinesColumn;
    @FXML private TableColumn<Artist, Boolean>   activeColumn;

    // form
    @FXML private TextField fieldName;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldCity;
    @FXML private TextField fieldYear;
    @FXML private TextArea  fieldBio;
    @FXML private ListView<Discipline> disciplineList;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        // table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));
        disciplinesColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDisciplines().stream()
                        .map(Discipline::getName).collect(Collectors.joining(", "))));
        activeColumn.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isActive()));
        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : (v ? "Yes" : "No"));
            }
        });

        // discipline list + filter — loaded from DB
        List<Discipline> allDisciplines = artistService.getAllDisciplines();
        disciplineList.setItems(FXCollections.observableArrayList(allDisciplines));
        disciplineList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        disciplineFilter.setItems(FXCollections.observableArrayList(allDisciplines));

        // row click → fill form
        artistTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) fillForm(sel); });

        refreshTable();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @FXML private void handleAdd() {
        Artist a = buildFromForm();
        if (a == null) return;
        try { artistService.createArtist(a); refreshTable(); handleClear(); info("Artist added: " + a.getName()); }
        catch (Exception e) { error("Failed to add artist", e); }
    }

    @FXML private void handleSave() {
        Artist sel = artistTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select an artist to update."); return; }
        Artist a = buildFromForm();
        if (a == null) return;
        a.setName(sel.getName());   // name is the PK lookup key
        try { artistService.updateArtist(a); refreshTable(); handleClear(); info("Artist updated."); }
        catch (Exception e) { error("Failed to update artist", e); }
    }

    @FXML private void handleDelete() {
        Artist sel = artistTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select an artist to delete."); return; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        c.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { artistService.deleteArtist(sel.getName()); refreshTable(); handleClear(); info("Artist deleted."); }
                catch (Exception e) { error("Failed to delete artist", e); }
            }
        });
    }

    @FXML private void handleSearch() {
        Discipline d = disciplineFilter.getValue();
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(searchField.getText(),
                        d != null ? d.getName() : null, null)));
    }

    @FXML private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    @FXML private void handleClear() {
        fieldName.clear(); fieldEmail.clear(); fieldCity.clear();
        fieldYear.clear(); fieldBio.clear();
        disciplineList.getSelectionModel().clearSelection();
        artistTable.getSelectionModel().clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fillForm(Artist a) {
        fieldName.setText(nvl(a.getName()));
        fieldEmail.setText(nvl(a.getContactEmail()));
        fieldCity.setText(nvl(a.getCity()));
        fieldYear.setText(a.getBirthYear() != null ? String.valueOf(a.getBirthYear()) : "");
        fieldBio.setText(nvl(a.getBio()));

        // select matching disciplines in the list
        disciplineList.getSelectionModel().clearSelection();
        List<String> artistDisciplineNames = a.getDisciplines().stream()
                .map(Discipline::getName).collect(Collectors.toList());
        for (int i = 0; i < disciplineList.getItems().size(); i++) {
            if (artistDisciplineNames.contains(disciplineList.getItems().get(i).getName())) {
                disciplineList.getSelectionModel().select(i);
            }
        }
    }

    private Artist buildFromForm() {
        String name = fieldName.getText().trim();
        if (name.isEmpty()) { warn("Name is required."); return null; }

        Artist a = new Artist();
        a.setName(name);
        a.setContactEmail(fieldEmail.getText().trim());
        a.setCity(fieldCity.getText().trim());
        a.setBio(fieldBio.getText().trim());
        a.setActive(true);

        String yr = fieldYear.getText().trim();
        if (!yr.isEmpty()) {
            try { a.setBirthYear(Integer.parseInt(yr)); }
            catch (NumberFormatException ex) { warn("Birth year must be a number."); return null; }
        }

        // selected disciplines
        a.setDisciplines(disciplineList.getSelectionModel().getSelectedItems()
                .stream().collect(Collectors.toList()));

        return a;
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private void info(String m)  { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void warn(String m)  { new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait(); }
    private void error(String m, Exception e) {
        new Alert(Alert.AlertType.ERROR, m + "\n" + e.getMessage(), ButtonType.OK).showAndWait();
    }
}