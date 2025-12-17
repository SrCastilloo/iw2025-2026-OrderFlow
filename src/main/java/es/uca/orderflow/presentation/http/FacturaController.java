// src/main/java/es/uca/orderflow/presentation/http/FacturaController.java
package es.uca.orderflow.presentation.http;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Detalle_Pedido;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.persistence.data.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@AnonymousAllowed
public class FacturaController {

    private final PedidoRepository pedidoRepository;

    @GetMapping("/{id}/factura.pdf")
    public ResponseEntity<byte[]> factura(@PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + id));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Documento
            Document doc = new Document(PageSize.A4, 40, 40, 46, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Formatos
            var dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            var euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

            Font H1 = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font H2 = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font TXT = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font TXTB = new Font(Font.HELVETICA, 10, Font.BOLD);

            // Header
            Paragraph head = new Paragraph("FACTURA", H1);
            head.setSpacingAfter(10);
            doc.add(head);

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.setWidths(new float[]{60, 40});

            PdfPCell left = cell("", TXT); left.setBorder(Rectangle.NO_BORDER);
            left.addElement(new Paragraph("Pedido #" + pedido.getId(), H2));
            left.addElement(new Paragraph("Fecha: " +
                    (pedido.getFechaRealizacion()==null?"-":dateFmt.format(pedido.getFechaRealizacion())), TXT));
            left.addElement(new Paragraph("Cliente: " +
                    (pedido.getCliente()==null?"-":pedido.getCliente().getNombre()), TXT));
            left.addElement(new Paragraph("Dirección envío: " +
                    (pedido.getCliente()!=null ? String.valueOf(pedido.getCliente().getDireccion()) : "-"), TXT));

            PdfPCell right = cell("", TXT); right.setBorder(Rectangle.NO_BORDER);
            right.addElement(new Paragraph("Estado: " + safe(pedido.getEstado()==null?null:pedido.getEstado().name()), TXTB));
            right.addElement(new Paragraph("Método de pago: " + safe(pedido.getPaymentMethod()==null?null:pedido.getPaymentMethod().name()), TXT));
            right.addElement(new Paragraph("Txn: " + safe(pedido.getPaymentTxnId()), TXT));

            meta.addCell(left); meta.addCell(right);
            doc.add(meta);

            doc.add(Chunk.NEWLINE);

            // Tabla líneas
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{54, 12, 17, 17});

            headerCell(table, "Producto", TXTB);
            headerCell(table, "Cant.", TXTB);
            headerCell(table, "Precio unit.", TXTB);
            headerCell(table, "Importe", TXTB);

            BigDecimal subtotal = BigDecimal.ZERO;

            for (Detalle_Pedido dp : pedido.getDetallespedido()) {
                table.addCell(cell(safe(dp.getProducto()==null?null:dp.getProducto().getNombre()), TXT));
                table.addCell(cell(dp.getCantidad()==null?"-":String.valueOf(dp.getCantidad()), TXT));
                table.addCell(cell(dp.getPrecioUnitario()==null?"-":euro.format(dp.getPrecioUnitario()), TXT));
                table.addCell(cell(dp.getImporte()==null?"-":euro.format(dp.getImporte()), TXT));
                if (dp.getImporte()!=null) subtotal = subtotal.add(dp.getImporte());
            }

            // Totales
            BigDecimal iva = subtotal.multiply(new BigDecimal("0.21")).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal total = subtotal.add(iva);

            PdfPCell empty = cell("", TXT); empty.setColspan(2); empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);
            table.addCell(cellRight("Subtotal", TXTB));
            table.addCell(cellRight(euro.format(subtotal), TXTB));

            table.addCell(empty);
            table.addCell(cellRight("IVA (21%)", TXTB));
            table.addCell(cellRight(euro.format(iva), TXTB));

            table.addCell(empty);
            PdfPCell ttl = cellRight("TOTAL", new Font(Font.HELVETICA, 12, Font.BOLD));
            PdfPCell ttlV = cellRight(euro.format(total), new Font(Font.HELVETICA, 12, Font.BOLD));
            table.addCell(ttl); table.addCell(ttlV);

            doc.add(table);

            doc.add(Chunk.NEWLINE);
            Paragraph foot = new Paragraph(
                    "Gracias por su compra. Si necesita una factura simplificada o con CIF/NIF, póngase en contacto con atención al cliente.",
                    new Font(Font.HELVETICA, 9, Font.ITALIC));
            foot.setSpacingBefore(6);
            doc.add(foot);

            doc.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=factura-" + pedido.getId() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar la factura: " + e.getMessage(), e);
        }
    }

    /* ===== helpers ===== */
    private static void headerCell(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBackgroundColor(new Color(245, 247, 250));
        t.addCell(c);
    }
    private static PdfPCell cell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text==null?"":text, f));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        return c;
    }
    private static PdfPCell cellRight(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text==null?"":text, f));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
    private static String safe(Object o){ return o==null?"-":String.valueOf(o); }
}
