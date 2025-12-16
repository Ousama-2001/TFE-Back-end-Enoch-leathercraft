package com.enoch.leathercraft.services;

import com.enoch.leathercraft.entities.Order;
import com.enoch.leathercraft.entities.OrderItem;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final String SHOP_VAT = "BE0999.999.999 (démo)";
    private static final String SHOP_NAME = "Enoch Leathercraft Shop";
    private static final String SHOP_ADDRESS = "Rue de la Maroquinerie 42, 1000 Bruxelles, Belgique";
    private static final String SHOP_EMAIL = "saidenoch@gmail.com";
    private static final String LOGO_PATH = "uploads/products/logo.jpg";

    public byte[] generate(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD);
            Font bold = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font small = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font redFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.RED);

            // HEADER
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 2.8f});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);

            File logoFile = new File(LOGO_PATH);
            if (logoFile.exists()) {
                Image logo = Image.getInstance(logoFile.getAbsolutePath());
                logo.scaleToFit(120, 60);
                logoCell.addElement(logo);
            }
            header.addCell(logoCell);

            PdfPCell titleCell = new PdfPCell(new Phrase("FACTURE", titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.addCell(titleCell);

            document.add(header);
            document.add(Chunk.NEWLINE);

            // INFOS
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{3f, 2f});

            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.NO_BORDER);
            left.addElement(new Phrase(SHOP_NAME, bold));
            left.addElement(new Phrase(SHOP_ADDRESS, normal));
            left.addElement(new Phrase("TVA : " + SHOP_VAT, normal));
            left.addElement(new Phrase("Email : " + SHOP_EMAIL, normal));

            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            String invoiceNumber = "INV-" + order.getReference();
            String dateStr = order.getCreatedAt() == null ? "" :
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(order.getCreatedAt());
            right.addElement(new Phrase("Facture : " + invoiceNumber, normal));
            right.addElement(new Phrase("Commande : " + order.getReference(), normal));
            right.addElement(new Phrase("Date : " + dateStr, normal));

            info.addCell(left);
            info.addCell(right);
            document.add(info);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // ADDRESSES
            String fullName = safe(order.getFirstName()) + " " + safe(order.getLastName());
            if (fullName.trim().isEmpty()) fullName = order.getCustomerEmail();

            Paragraph billed = new Paragraph("Facturé à :", bold);
            document.add(billed);
            document.add(new Paragraph(fullName, normal));
            document.add(new Paragraph(safe(order.getCustomerEmail()), normal));
            document.add(Chunk.NEWLINE);

            Paragraph ship = new Paragraph("Adresse de livraison :", bold);
            document.add(ship);
            document.add(new Paragraph(safe(order.getStreet()), normal));
            String cityLine = (safe(order.getPostalCode()) + " " + safe(order.getCity())).trim();
            if (!cityLine.isBlank()) document.add(new Paragraph(cityLine, normal));
            document.add(new Paragraph(safe(order.getCountry()), normal));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // ITEMS TABLE
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.2f, 1.2f, 0.7f, 1.3f});

            addHeaderCell(table, "Produit");
            addHeaderCell(table, "Prix Unit.");
            addHeaderCell(table, "Qté");
            addHeaderCell(table, "Total Ligne");

            // Pour l'affichage des lignes, on affiche le prix de base (sans réduc)
            // car la réduc est globale en bas de facture.
            for (OrderItem item : order.getItems()) {
                BigDecimal unit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));

                table.addCell(cell(item.getProductName(), normal));
                table.addCell(cell(formatMoney(unit) + " €", normal));
                table.addCell(cell(String.valueOf(qty), normal));
                table.addCell(cell(formatMoney(lineTotal) + " €", normal));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // TOTALS (AVEC PROMO)
            PdfPTable totals = new PdfPTable(1);
            totals.setWidthPercentage(100);
            PdfPCell totalCell = new PdfPCell();
            totalCell.setBorder(Rectangle.NO_BORDER);
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // 1. Sous-total (si différent du total)
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                Paragraph subP = new Paragraph("Sous-total : " + formatMoney(order.getSubtotalAmount()) + " €", normal);
                subP.setAlignment(Element.ALIGN_RIGHT);
                totalCell.addElement(subP);

                String label = "Réduction";
                if(order.getCouponCode() != null) label += " (" + order.getCouponCode() + ")";

                Paragraph discP = new Paragraph(label + " : -" + formatMoney(order.getDiscountAmount()) + " €", redFont);
                discP.setAlignment(Element.ALIGN_RIGHT);
                totalCell.addElement(discP);
            }

            // 2. Total Final
            Paragraph totalP = new Paragraph("TOTAL PAYÉ : " + formatMoney(order.getTotalAmount()) + " €", new Font(Font.HELVETICA, 14, Font.BOLD));
            totalP.setAlignment(Element.ALIGN_RIGHT);

            Paragraph vatP = new Paragraph("TVA incluse (régime particulier)", small);
            vatP.setAlignment(Element.ALIGN_RIGHT);

            totalCell.addElement(totalP);
            totalCell.addElement(vatP);
            totals.addCell(totalCell);
            document.add(totals);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Merci pour votre confiance.", normal));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF facture", e);
        }
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        PdfPCell c = new PdfPCell(new Phrase(text, headerFont));
        c.setPadding(8);
        c.setBackgroundColor(new Color(240, 240, 240));
        table.addCell(c);
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setPadding(8);
        return c;
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String formatMoney(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(v == null ? BigDecimal.ZERO : v);
    }
}