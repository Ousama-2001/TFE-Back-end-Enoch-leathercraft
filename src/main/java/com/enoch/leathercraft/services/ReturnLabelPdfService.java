package com.enoch.leathercraft.services;

import com.enoch.leathercraft.entities.Order;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReturnLabelPdfService {

    // ✅ Ton logo
    private static final String LOGO_PATH = "uploads/products/logo.jpg";

    // ✅ Adresse retour (change si tu veux)
    private static final String RETURN_TO =
            "Enoch Leathercraft – Service Retours\n" +
                    "Rue de la Maroquinerie 42\n" +
                    "1000 Bruxelles\n" +
                    "Belgique";

    public byte[] generate(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font bold = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font small = new Font(Font.HELVETICA, 10, Font.NORMAL);

            // ---------------------------------------------------
            // HEADER : logo + titre
            // ---------------------------------------------------
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

            PdfPCell titleCell = new PdfPCell(new Phrase("BON DE RETOUR", titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.addCell(titleCell);

            document.add(header);
            document.add(Chunk.NEWLINE);

            // ---------------------------------------------------
            // Infos commande
            // ---------------------------------------------------
            String dateStr = order.getCreatedAt() == null ? "" :
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(order.getCreatedAt());

            document.add(new Paragraph("Référence commande : " + safe(order.getReference()), bold));
            if (!dateStr.isBlank()) {
                document.add(new Paragraph("Date commande : " + dateStr, normal));
            }
            document.add(Chunk.NEWLINE);

            // ---------------------------------------------------
            // Expéditeur (client) = infos livraison
            // ---------------------------------------------------
            String fullName = (safe(order.getFirstName()) + " " + safe(order.getLastName())).trim();
            if (fullName.isEmpty()) fullName = safe(order.getCustomerEmail());

            document.add(new Paragraph("Expéditeur :", bold));
            if (!fullName.isBlank()) document.add(new Paragraph(fullName, normal));
            if (!safe(order.getCustomerEmail()).isBlank()) document.add(new Paragraph(order.getCustomerEmail(), normal));
            if (!safe(order.getStreet()).isBlank()) document.add(new Paragraph(order.getStreet(), normal));

            String cityLine = (safe(order.getPostalCode()) + " " + safe(order.getCity())).trim();
            if (!cityLine.isBlank()) document.add(new Paragraph(cityLine, normal));

            if (!safe(order.getCountry()).isBlank()) document.add(new Paragraph(order.getCountry(), normal));

            document.add(Chunk.NEWLINE);

            // ---------------------------------------------------
            // Destinataire = retour
            // ---------------------------------------------------
            document.add(new Paragraph("À renvoyer à :", bold));
            document.add(new Paragraph(RETURN_TO, normal));
            document.add(Chunk.NEWLINE);

            // ---------------------------------------------------
            // Petite note
            // ---------------------------------------------------
            document.add(new Paragraph("Merci d’imprimer ce document et de le glisser dans le colis.", small));
            document.add(new Paragraph("Assurez-vous que l’article est dans son état d’origine.", small));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF bon de retour", e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
