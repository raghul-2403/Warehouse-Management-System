package org.vaadin.example;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Route("")
@PageTitle("WMS | Home")
public class MainView extends VerticalLayout {
    public MainView(AuthenticationContext authenticationContext) {
        boolean isAdmin = authenticationContext.getGrantedRoles()
                .stream().anyMatch("ADMIN"::equals);
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        getStyle().set("background", WmsDesign.BG);

        Div hero = new Div();
        hero.getStyle().set("background", WmsDesign.INK)
                .set("border-radius", "22px").set("padding", "34px")
                .set("margin-bottom", "16px");
        Span kicker = new Span("WAREHOUSE OPERATIONS • " + (isAdmin ? "ADMIN" : "STAFF"));
        kicker.getStyle().set("color", "#ff937a").set("font-size", "11px")
                .set("font-weight", "800").set("letter-spacing", "1.5px");
        H1 title = new H1("Warehouse Management System");
        title.getStyle().set("font-size", "clamp(26px, 3vw, 39px)")
                .set("color", "white").set("margin", "14px 0");
        Paragraph info = new Paragraph("Your central workspace for stock, orders and daily warehouse tasks.");
        info.getStyle().set("color", "#c7c4c9").set("font-size", "14px");
        hero.add(kicker, title, info);
        add(hero);

        Div cards = new Div();
        cards.setWidthFull();
        cards.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(230px, 1fr))")
                .set("gap", "16px");
        cards.add(card("▦", "Dashboard", "Live warehouse analytics and alerts", "dashboard"));
        cards.add(card("▤", "Inventory", "Stock by product and storage location", "inventory"));
        cards.add(card("▣", "Orders", "Create and process warehouse orders", "orders"));
        if (isAdmin) {
            cards.add(card("◈", "Products", "Manage SKUs and catalog records", "products"));
            cards.add(card("♙", "Customers", "Customer master records", "customers"));
        }
        add(cards);
        Button logout = new Button("Sign out", e -> authenticationContext.logout());
        logout.getStyle().set("background", "#fff0ec").set("color", "#bd4324")
                .set("font-weight", "700").set("border-radius", "10px");
        add(logout);
        WmsDesign.decorate(this, "Home", "Choose a module to get started");
    }

    private Anchor card(String symbol, String title, String subtitle, String route) {
        Anchor a = new Anchor("/" + route);
        a.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "18px").set("padding", "26px")
                .set("text-decoration", "none").set("border-radius", "18px")
                .set("background", "white").set("border", "1px solid #eeedf0")
                .set("min-height", "170px").set("box-sizing", "border-box");
        Span icon = new Span(symbol);
        icon.getStyle().set("font-size", "28px").set("color", WmsDesign.ORANGE);
        H2 h = new H2(title);
        h.getStyle().set("font-size", "19px").set("color", WmsDesign.INK).set("margin", "0");
        Paragraph p = new Paragraph(subtitle);
        p.getStyle().set("font-size", "12px").set("color", WmsDesign.MUTED).set("margin", "0");
        Span open = new Span("OPEN MODULE  ↗");
        open.getStyle().set("margin-top", "auto").set("color", WmsDesign.ORANGE)
                .set("font-weight", "800").set("font-size", "11px");
        a.add(icon, h, p, open);
        return a;
    }
}
