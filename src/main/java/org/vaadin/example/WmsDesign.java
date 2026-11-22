package org.vaadin.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shared look and navigation for all Java Vaadin views. No extra CSS files required. */
public final class WmsDesign {
    public static final String INK = "#242326";
    public static final String ORANGE = "#ff4b20";
    public static final String MUTED = "#79777c";
    public static final String BG = "#f4f4f6";

    private WmsDesign() {}

    public static boolean admin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
    }

    public static void decorate(VerticalLayout view, String section, String caption) {
        List<Component> existing = new ArrayList<>(view.getChildren().toList());
        view.removeAll();
        view.setWidthFull();
        view.setMaxWidth("100%");
        view.setMinHeight("100vh");
        view.setPadding(false);
        view.setSpacing(false);
        view.getStyle().set("background", BG).set("margin", "0")
                .set("font-family", "Inter, Arial, sans-serif");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        body.setMinWidth("0");
        body.getStyle().set("padding", "28px clamp(16px, 3vw, 42px)")
                .set("box-sizing", "border-box");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setAlignItems(HorizontalLayout.Alignment.CENTER);
        top.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        Span crumb = new Span("WORKSPACE  /  " + section.toUpperCase());
        crumb.getStyle().set("font-size", "11px").set("letter-spacing", "1.2px")
                .set("font-weight", "700").set("color", MUTED);
        Span role = new Span(admin() ? "● ADMIN" : "● STAFF");
        role.getStyle().set("background", "white").set("color", INK)
                .set("font-size", "12px").set("font-weight", "700")
                .set("border-radius", "20px").set("padding", "10px 15px");
        top.add(crumb, role);
        body.add(top);

        H1 title = new H1(section.equals("Home") ? "Warehouse workspace" : section + " Management");
        title.getStyle().set("margin", "18px 0 0").set("font-size", "clamp(25px, 3vw, 35px)")
                .set("font-weight", "800").set("color", INK);
        Paragraph description = new Paragraph(caption);
        description.getStyle().set("margin", "0 0 16px").set("font-size", "13px")
                .set("color", MUTED);
        body.add(title, description);

        for (Component c : existing) {
            if (c instanceof H1) continue; // Standardized title above.
            if (c instanceof HorizontalLayout row) {
                row.getStyle().set("flex-wrap", "wrap").set("row-gap", "12px");
            }
            if (c instanceof Grid<?> grid) {
                grid.getStyle().set("border", "0").set("border-radius", "13px")
                        .set("overflow", "hidden");
            }
            if (c instanceof VerticalLayout vl) {
                vl.getStyle().set("background", "#ffffff")
                        .set("border", "1px solid #eeeeef")
                        .set("border-radius", "16px")
                        .set("padding", "22px")
                        .set("box-shadow", "0 6px 25px rgba(25,25,30,0.035)");
            }
            styleButtons(c);
            body.add(c);
        }
        // Keep large management tables inside their own white workspace.
        HorizontalLayout outer = new HorizontalLayout(nav(section), body);
        outer.setPadding(false);
        outer.setSpacing(false);
        outer.setWidthFull();
        outer.setAlignItems(HorizontalLayout.Alignment.STRETCH);
        outer.getStyle().set("min-height", "100vh");
        outer.expand(body);
        view.add(outer);
    }

    private static void styleButtons(Component component) {
        if (component instanceof Button b) {
            String text = b.getText().toLowerCase();
            boolean dangerous = text.contains("delete") || text.contains("cancel");
            boolean primary = text.contains("add ") || text.contains("create ")
                    || text.contains("update ") || text.contains("mark ");
            b.getStyle().set("border-radius", "10px").set("font-weight", "700")
                    .set("cursor", "pointer");
            if (dangerous) {
                b.getStyle().set("background", "#fff0eb").set("color", "#ba3b1d");
            } else if (primary) {
                b.getStyle().set("background", ORANGE).set("color", "white");
            } else {
                b.getStyle().set("background", "#f0f0f2").set("color", INK);
            }
        }
        component.getChildren().forEach(WmsDesign::styleButtons);
    }

    public static VerticalLayout nav(String active) {
        VerticalLayout nav = new VerticalLayout();
        nav.setWidth("198px");
        nav.setMinWidth("198px");
        nav.setPadding(false);
        nav.setSpacing(false);
        nav.getStyle().set("background", INK).set("padding", "22px 12px")
                .set("box-sizing", "border-box").set("min-height", "100vh");
        Span mark = new Span("W");
        mark.getStyle().set("background", ORANGE).set("color", "white")
                .set("padding", "9px 15px").set("font-size", "22px")
                .set("font-weight", "900").set("border-radius", "12px");
        Span brand = new Span("WAREHOUSE OS");
        brand.getStyle().set("color", "white").set("font-size", "12px")
                .set("font-weight", "800").set("letter-spacing", "1px")
                .set("margin", "13px 0 35px");
        nav.add(mark, brand);
        nav.add(link("◫", "Dashboard", "dashboard", active));
        nav.add(link("▤", "Inventory", "inventory", active));
        nav.add(link("▣", "Orders", "orders", active));
        if (admin()) {
            nav.add(link("◈", "Products", "products", active));
            nav.add(link("♙", "Customers", "customers", active));
        }
        nav.add(link("⌂", "Home", "", active));
        Paragraph note = new Paragraph("WAREHOUSE OPERATIONS");
        note.getStyle().set("color", "#8d8990").set("font-size", "9px")
                .set("letter-spacing", "1px").set("margin-top", "32px");
        nav.add(note);
        return nav;
    }

    private static Anchor link(String symbol, String name, String route, String active) {
        Anchor a = new Anchor("/" + route, symbol + "   " + name);
        a.setWidthFull();
        a.getStyle().set("display", "block").set("box-sizing", "border-box")
                .set("padding", "13px 12px").set("margin", "3px 0")
                .set("text-decoration", "none").set("font-size", "13px")
                .set("font-weight", "700").set("border-radius", "10px")
                .set("background", name.equalsIgnoreCase(active) ? ORANGE : "transparent")
                .set("color", name.equalsIgnoreCase(active) ? "#fff" : "#c9c6ce");
        return a;
    }

    public static Div metric(String label, String number, String variant) {
        Div d = new Div();
        String bg = "orange".equals(variant) ? ORANGE : "dark".equals(variant) ? INK : "#fff";
        String fg = "plain".equals(variant) ? INK : "#fff";
        d.getStyle().set("background", bg).set("border-radius", "19px")
                .set("padding", "22px").set("min-height", "120px")
                .set("box-sizing", "border-box");
        Paragraph caption = new Paragraph(label);
        caption.getStyle().set("margin", "0 0 18px").set("font-size", "12px")
                .set("color", fg);
        H2 value = new H2(number);
        value.getStyle().set("margin", "0").set("font-weight", "800")
                .set("font-size", "32px").set("color", fg);
        d.add(caption, value);
        return d;
    }

    public static VerticalLayout panel(String title, String subtitle) {
        VerticalLayout v = new VerticalLayout();
        v.setWidthFull();
        v.setPadding(false);
        v.setSpacing(true);
        v.getStyle().set("background", "#fff").set("padding", "24px")
                .set("border-radius", "19px").set("box-sizing", "border-box");
        H2 h = new H2(title);
        h.getStyle().set("color", INK).set("font-size", "18px").set("margin", "0");
        Paragraph sub = new Paragraph(subtitle);
        sub.getStyle().set("font-size", "12px").set("color", MUTED)
                .set("margin", "0 0 12px");
        v.add(h, sub);
        return v;
    }
}
