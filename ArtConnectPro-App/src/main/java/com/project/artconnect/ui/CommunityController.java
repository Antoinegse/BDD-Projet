package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommunityController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> membershipFilter;
    @FXML private TableView<CommunityMember> memberTable;
    @FXML private TableColumn<CommunityMember, String> nameColumn;
    @FXML private TableColumn<CommunityMember, String> emailColumn;
    @FXML private TableColumn<CommunityMember, String> cityColumn;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        membershipFilter.setItems(FXCollections.observableArrayList("free", "premium"));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        String membership = membershipFilter.getValue();
        List<CommunityMember> filtered = communityService.getAllMembers().stream()
                .filter(m -> query.isEmpty() ||
                        (m.getName()  != null && m.getName().toLowerCase().contains(query)) ||
                        (m.getEmail() != null && m.getEmail().toLowerCase().contains(query)) ||
                        (m.getCity()  != null && m.getCity().toLowerCase().contains(query)))
                .filter(m -> membership == null || membership.isEmpty() ||
                        (m.getMembershipType() != null && m.getMembershipType().equals(membership)))
                .collect(Collectors.toList());
        memberTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        membershipFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showMemberDialog(null).ifPresent(member -> {
            communityService.createMember(member);
            refreshTable();
        });
    }

    @FXML
    private void handleEdit() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez un membre à modifier."); return; }
        showMemberDialog(selected).ifPresent(updated -> {
            communityService.updateMember(updated);
            refreshTable();
        });
    }

    @FXML
    private void handleDelete() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélection requise", "Sélectionnez un membre à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le membre « " + selected.getName() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            communityService.deleteMember(selected.getName());
            refreshTable();
        });
    }

    private Optional<CommunityMember> showMemberDialog(CommunityMember existing) {
        Dialog<CommunityMember> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouveau membre" : "Modifier le membre");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField  = new TextField(); nameField.setPromptText("Nom complet");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField cityField  = new TextField(); cityField.setPromptText("Ville");
        TextField phoneField = new TextField(); phoneField.setPromptText("Téléphone");
        TextField yearField  = new TextField(); yearField.setPromptText("Année de naissance");
        ComboBox<String> membershipCombo = new ComboBox<>(
                FXCollections.observableArrayList("free", "premium"));

        if (existing != null) {
            if (existing.getName()           != null) nameField.setText(existing.getName());
            if (existing.getEmail()          != null) emailField.setText(existing.getEmail());
            if (existing.getCity()           != null) cityField.setText(existing.getCity());
            if (existing.getPhone()          != null) phoneField.setText(existing.getPhone());
            if (existing.getBirthYear()      != null) yearField.setText(existing.getBirthYear().toString());
            if (existing.getMembershipType() != null) membershipCombo.setValue(existing.getMembershipType());
        } else {
            membershipCombo.setValue("free");
        }

        grid.add(new Label("Nom :"),         0, 0); grid.add(nameField,      1, 0);
        grid.add(new Label("Email :"),       0, 1); grid.add(emailField,     1, 1);
        grid.add(new Label("Ville :"),       0, 2); grid.add(cityField,      1, 2);
        grid.add(new Label("Tél :"),         0, 3); grid.add(phoneField,     1, 3);
        grid.add(new Label("Né(e) en :"),   0, 4); grid.add(yearField,      1, 4);
        grid.add(new Label("Abonnement :"), 0, 5); grid.add(membershipCombo,1, 5);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(existing == null);
        nameField.textProperty().addListener((obs, o, n) -> okButton.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            CommunityMember m = existing != null ? existing : new CommunityMember();
            m.setName(nameField.getText().trim());
            m.setEmail(emailField.getText().trim());
            m.setCity(cityField.getText().trim());
            m.setPhone(phoneField.getText().trim());
            m.setMembershipType(membershipCombo.getValue());
            try { m.setBirthYear(Integer.parseInt(yearField.getText().trim())); }
            catch (NumberFormatException ignored) {}
            return m;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    private void showAlert(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK) {{ setHeaderText(title); }}.showAndWait();
    }
}
