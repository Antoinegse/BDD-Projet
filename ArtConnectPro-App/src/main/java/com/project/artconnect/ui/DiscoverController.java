package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.util.List;

/**
 * Contrôleur de l'onglet Découverte.
 * FIX #3 : données issues de la base via ExhibitionService (plus de données statiques).
 */
public class DiscoverController {

    @FXML
    private FlowPane discoverPane;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final WorkshopService   workshopService   = ServiceProvider.getWorkshopService();

    @FXML
    public void initialize() {
        discoverPane.getChildren().clear();

        addSectionHeader("🖼  Expositions à la une", "#1565C0");

        List<Exhibition> exhibitions = exhibitionService.getAllExhibitions();
        if (exhibitions.isEmpty()) {
            discoverPane.getChildren().add(emptyLabel("Aucune exposition disponible."));
        } else {
            exhibitions.stream().limit(6).forEach(this::addExhibitionCard);
        }

        addSectionHeader("🎨  Ateliers à venir", "#2E7D32");

        List<Workshop> workshops = workshopService.getAllWorkshops();
        if (workshops.isEmpty()) {
            discoverPane.getChildren().add(emptyLabel("Aucun atelier disponible."));
        } else {
            workshops.stream().limit(6).forEach(this::addWorkshopCard);
        }
    }

    private void addSectionHeader(String text, String color) {
        Label header = new Label(text);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        HBox wrapper = new HBox(header);
        wrapper.setPrefWidth(1080);
        FlowPane.setMargin(wrapper, new Insets(8, 0, 4, 0));
        discoverPane.getChildren().add(wrapper);
    }

    private Label emptyLabel(String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
        return lbl;
    }

    private void addExhibitionCard(Exhibition e) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #E3F2FD; -fx-border-color: #2196F3;" +
                "-fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 2);");

        Label badge = new Label("EXPOSITION");
        badge.setStyle("-fx-font-size: 10px; -fx-text-fill: #1565C0; -fx-font-weight: bold;");

        Label title = new Label(e.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-wrap-text: true;");
        title.setMaxWidth(230);

        String galleryName = (e.getGallery() != null) ? e.getGallery().getName() : "Galerie inconnue";
        Label gallery = new Label("📍 " + galleryName);
        gallery.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        String theme = (e.getTheme() != null && !e.getTheme().isBlank()) ? e.getTheme() : "—";
        Label themeLabel = new Label("Thème : " + theme);
        themeLabel.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(badge, title, gallery, themeLabel);

        if (e.getStartDate() != null && e.getEndDate() != null) {
            Label dates = new Label("🗓 " + e.getStartDate() + " → " + e.getEndDate());
            dates.setStyle("-fx-font-size: 10px; -fx-text-fill: #777;");
            card.getChildren().add(dates);
        }

        int nbArtworks = (e.getArtworks() != null) ? e.getArtworks().size() : 0;
        if (nbArtworks > 0) {
            Label artLabel = new Label("🎨 " + nbArtworks + " œuvre(s)");
            artLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #1976D2;");
            card.getChildren().add(artLabel);
        }

        discoverPane.getChildren().add(card);
    }

    private void addWorkshopCard(Workshop w) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: #F1F8E9; -fx-border-color: #4CAF50;" +
                "-fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 2);");

        Label badge = new Label("ATELIER");
        badge.setStyle("-fx-font-size: 10px; -fx-text-fill: #2E7D32; -fx-font-weight: bold;");

        Label title = new Label(w.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-wrap-text: true;");
        title.setMaxWidth(230);

        String instructorName = (w.getInstructor() != null) ? w.getInstructor().getName() : "Instructeur inconnu";
        Label instructor = new Label("👤 " + instructorName);
        instructor.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label price = new Label("Prix : " + w.getPrice() + " €");
        price.setStyle("-fx-font-size: 11px; -fx-text-fill: #388E3C; -fx-font-weight: bold;");

        card.getChildren().addAll(badge, title, instructor, price);
        discoverPane.getChildren().add(card);
    }
}
