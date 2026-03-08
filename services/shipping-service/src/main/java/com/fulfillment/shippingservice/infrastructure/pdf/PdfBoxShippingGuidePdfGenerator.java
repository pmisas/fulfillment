package com.fulfillment.shippingservice.infrastructure.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.ports.ShippingGuidePdfGenerator;

@Component
public class PdfBoxShippingGuidePdfGenerator implements ShippingGuidePdfGenerator {

    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 18f;
    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    @Override
    public byte[] generate(Shipment shipment) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float contentWidth = pageWidth - 2 * MARGIN;

            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = pageHeight - MARGIN;

                // Header block
                cs.setNonStrokingColor(new Color(40, 40, 40));
                cs.addRect(MARGIN, y - 32, contentWidth, 38);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                writeText(cs, bold, 16, MARGIN + 10, y - 22, "FULFILLMENT CO.  -  GUIA DE ENVIO");
                y -= 45;

                separator(cs, pageWidth, y);
                y -= 15;

                cs.setNonStrokingColor(Color.BLACK);
                writeText(cs, bold, 14, MARGIN, y, "TRACKING:  " + safe(shipment.getTrackingId()));
                y -= 10;
                separator(cs, pageWidth, y);
                y -= 20;

                cs.setNonStrokingColor(Color.BLACK);
                y = field(cs, bold, regular, "Shipment ID:", shipment.getShipmentId(), y);
                y = field(cs, bold, regular, "Order ID:", shipment.getOrderId(), y);
                y = field(cs, bold, regular, "Warehouse:", shipment.getWarehouseId(), y);
                y = field(cs, bold, regular, "Carrier:", shipment.getCarrier().name(), y);
                if (shipment.getShippedAt() != null) {
                    y = field(cs, bold, regular, "Enviado:", FMT.format(shipment.getShippedAt()), y);
                }
                y = field(cs, bold, regular, "Est. Entrega:", FMT.format(shipment.getEstimatedDeliveryAt()), y);

                y -= 8;
                separator(cs, pageWidth, y);
                y -= 15;

                // Items section
                writeText(cs, bold, 11, MARGIN, y, "ARTICULOS");
                y -= 6;

                cs.setNonStrokingColor(new Color(220, 220, 220));
                cs.addRect(MARGIN, y - 4, contentWidth, 16);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
                writeText(cs, bold, 9, MARGIN + 5, y, "SKU");
                writeText(cs, bold, 9, MARGIN + 250, y, "CANTIDAD");
                y -= 16;

                int totalQty = 0;
                for (ShipmentItem item : shipment.getItems()) {
                    cs.setNonStrokingColor(Color.BLACK);
                    writeText(cs, regular, 9, MARGIN + 5, y, item.getSku());
                    writeText(cs, regular, 9, MARGIN + 250, y, String.valueOf(item.getQuantity()));
                    y -= 14;
                    totalQty += item.getQuantity();
                }

                y -= 5;
                separator(cs, pageWidth, y);
                y -= 14;

                writeText(cs, bold, 10, MARGIN, y,
                        "TOTAL: " + totalQty + " unidades en " + shipment.getItems().size() + " referencia(s)");

                float footerY = MARGIN + 8;
                separator(cs, pageWidth, footerY + 12);
                cs.setNonStrokingColor(new Color(130, 130, 130));
                writeText(cs, regular, 7, MARGIN, footerY,
                        "Generado: " + FMT.format(Instant.now()) + "  |  Fulfillment Co. - Documento de uso interno");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to generate shipping guide PDF for shipment " + shipment.getShipmentId(), e);
        }
    }

    private void writeText(PDPageContentStream cs, PDFont font, float size,
            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private float field(PDPageContentStream cs, PDFont bold, PDFont regular,
            String label, String value, float y) throws IOException {
        cs.beginText();
        cs.setFont(bold, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label);
        cs.setFont(regular, 10);
        cs.newLineAtOffset(130, 0);
        cs.showText(safe(value));
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private void separator(PDPageContentStream cs, float pageWidth, float y) throws IOException {
        cs.setStrokingColor(new Color(180, 180, 180));
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(pageWidth - MARGIN, y);
        cs.stroke();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
