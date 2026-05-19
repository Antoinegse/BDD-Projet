package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.stream.Collectors;

public class CommunityController {

    // table
    @FXML private TableView<CommunityMember>              memberTable;
    @FXML private TableColumn<CommunityMember, String>    nameColumn;
    @FXML private TableColumn<CommunityMember, String>    emailColumn;
    @FXML private TableColumn<CommunityMember, String>    cityColumn;
    @FXML private TableColumn<CommunityMember, String>    membershipColumn;
    @FXML private TableColumn<CommunityMember, String>    disciplinesColumn;

    // form
    @FXML private TextField fieldName;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldCity;
    @FXML private TextField fieldPhone;
    @FXML private ComboBox<String> fieldMembership;
    @FXML private TextField fieldYear;
    @FXML private ListView<Discipline> disciplineList;

    private final CommunityService communityService = ServiceProvider.getCommunityService();
    private final ArtistService    artistService    = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        membershipColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nvl(data.getValue().getMembershipType())));
        disciplinesColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFavoriteDisciplines().stream()
                        .map(Discipline::getName).collect(Collectors.joining(", "))));

        fieldMembership.setItems(FXCollections.observableArrayList("FREE", "PREMIUM"));
        fieldMembership.setValue("FREE");

        // load disciplines from DB (reuse ArtistService which already has getAllDisciplines)
        List<Discipline> allDisciplines = artistService.getAllDisciplines();
        disciplineList.setItems(FXCollections.observableArrayList(allDisciplines));
        disciplineList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        memberTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> { if (sel != null) fillForm(sel); });
        refreshTable();
    }

    @FXML private void handleAdd() {
        CommunityMember m = buildFromForm();
        if (m == null) return;
        try { communityService.createMember(m); refreshTable(); handleClear(); info("Member added."); }
        catch (Exception e) { error("Failed to add member", e); }
    }

    @FXML private void handleSave() {
        CommunityMember sel = memberTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select a member to update."); return; }
        CommunityMember m = buildFromForm();
        if (m == null) return;
        m.setName(sel.getName());
        try { communityService.updateMember(m); refreshTable(); handleClear(); info("Member updated."); }
        catch (Exception e) { error("Failed to update member", e); }
    }

    @FXML private void handleDelete() {
        CommunityMember sel = memberTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("Select a member to delete."); return; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        c.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { communityService.deleteMember(sel.getName()); refreshTable(); handleClear(); info("Member deleted."); }
                catch (Exception e) { error("Failed to delete member", e); }
            }
        });
    }

    @FXML private void handleClear() {
        fieldName.clear(); fieldEmail.clear(); fieldCity.clear();
        fieldPhone.clear(); fieldYear.clear();
        fieldMembership.setValue("FREE");
        disciplineList.getSelectionModel().clearSelection();
        memberTable.getSelectionModel().clearSelection();
    }

    private void fillForm(CommunityMember m) {
        fieldName.setText(nvl(m.getName()));
        fieldEmail.setText(nvl(m.getEmail()));
        fieldCity.setText(nvl(m.getCity()));
        fieldPhone.setText(nvl(m.getPhone()));
        fieldMembership.setValue(m.getMembershipType() != null ? m.getMembershipType() : "FREE");
        fieldYear.setText(m.getBirthYear() != null ? String.valueOf(m.getBirthYear()) : "");

        // pre-select matching favourite disciplines
        disciplineList.getSelectionModel().clearSelection();
        List<String> favNames = m.getFavoriteDisciplines().stream()
                .map(Discipline::getName).collect(Collectors.toList());
        for (int i = 0; i < disciplineList.getItems().size(); i++) {
            if (favNames.contains(disciplineList.getItems().get(i).getName())) {
                disciplineList.getSelectionModel().select(i);
            }
        }
    }

    private CommunityMember buildFromForm() {
        String name  = fieldName.getText().trim();
        String email = fieldEmail.getText().trim();
        if (name.isEmpty())  { warn("Name is required."); return null; }
        if (email.isEmpty()) { warn("Email is required."); return null; }

        CommunityMember m = new CommunityMember(name, email);
        m.setCity(fieldCity.getText().trim());
        m.setPhone(fieldPhone.getText().trim());
        m.setMembershipType(fieldMembership.getValue() != null ? fieldMembership.getValue() : "FREE");

        String yr = fieldYear.getText().trim();
        if (!yr.isEmpty()) {
            try { m.setBirthYear(Integer.parseInt(yr)); }
            catch (NumberFormatException ex) { warn("Birth year must be a number."); return null; }
        }

        m.setFavoriteDisciplines(disciplineList.getSelectionModel().getSelectedItems()
                .stream().collect(Collectors.toList()));

        return m;
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private void info(String m)  { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void warn(String m)  { new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait(); }
    private void error(String m, Exception e) {
        new Alert(Alert.AlertType.ERROR, m + "\n" + e.getMessage(), ButtonType.OK).showAndWait();
    }
}