package com.primatoos.backend.service;

import com.primatoos.backend.dto.common.UserSummaryResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestItemResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestResponse;
import com.primatoos.backend.dto.worker.WorkerSummaryResponse;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.exception.PdfGenerationException;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

    public byte[] generateWorkOrderPdf(WorkOrderResponse workOrder) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Ordem de Serviço Nº " + workOrder.orderNumber());

            addField(document, "Obra", workOrder.project().name() + " — " + workOrder.project().client());
            addField(document, "Data", format(workOrder.date()));
            addField(document, "Status", workOrder.status().name());
            addField(document, "Responsável", nameAndEmail(workOrder.responsibleUser()));
            addField(document, "Etapa", workOrder.stage());
            addField(document, "Local", valueOrDash(workOrder.location()));
            addField(document, "Horário planejado", plannedTimeRange(workOrder));

            addSectionTitle(document, "Descrição");
            addParagraph(document, valueOrDash(workOrder.description()));

            addSectionTitle(document, "Meta diária");
            addParagraph(document, valueOrDash(workOrder.dailyGoal()));

            addSectionTitle(document, "Equipe atribuída");
            addWorkersTable(document, workOrder.assignedWorkers());

            addSectionTitle(document, "Materiais necessários");
            addParagraph(document, valueOrDash(workOrder.materialsNeeded()));

            addSectionTitle(document, "Ferramentas e equipamentos");
            addParagraph(document, valueOrDash(workOrder.tools()));

            addSectionTitle(document, "Diretrizes de segurança");
            addParagraph(document, valueOrDash(workOrder.safetyGuidelines()));

            addSectionTitle(document, "Critérios de qualidade");
            addParagraph(document, valueOrDash(workOrder.qualityCriteria()));

            addSectionTitle(document, "Observações");
            addParagraph(document, valueOrDash(workOrder.notes()));

            addFooter(document);
            document.close();
        } catch (DocumentException ex) {
            throw new PdfGenerationException("Erro ao gerar PDF da ordem de serviço", ex);
        }

        return out.toByteArray();
    }

    public byte[] generateMaterialRequestPdf(MaterialRequestResponse materialRequest) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Pedido de Materiais Nº " + materialRequest.requestNumber());

            addField(document, "Obra",
                    materialRequest.project().name() + " — " + materialRequest.project().client());
            addField(document, "OS vinculada", materialRequest.workOrder() != null
                    ? "Nº " + materialRequest.workOrder().orderNumber() : "—");
            addField(document, "Data do pedido", format(materialRequest.requestDate()));
            addField(document, "Necessário até", materialRequest.neededByDate() != null
                    ? format(materialRequest.neededByDate()) : "—");
            addField(document, "Solicitante", nameAndEmail(materialRequest.requester()));
            addField(document, "Prioridade", materialRequest.priority().name());
            addField(document, "Status", materialRequest.status().name());
            addField(document, "Local de entrega", valueOrDash(materialRequest.deliveryLocation()));

            addSectionTitle(document, "Justificativa");
            addParagraph(document, valueOrDash(materialRequest.justification()));

            addSectionTitle(document, "Itens");
            addItemsTable(document, materialRequest.items());

            addSectionTitle(document, "Observações");
            addParagraph(document, valueOrDash(materialRequest.notes()));

            addFooter(document);
            document.close();
        } catch (DocumentException ex) {
            throw new PdfGenerationException("Erro ao gerar PDF do pedido de materiais", ex);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, String title) throws DocumentException {
        Paragraph appName = new Paragraph("Primato OS", HEADER_FONT);
        appName.setSpacingAfter(4);
        document.add(appName);

        Paragraph docTitle = new Paragraph(title, TITLE_FONT);
        docTitle.setSpacingAfter(12);
        document.add(docTitle);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, SECTION_FONT);
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(4);
        document.add(paragraph);
    }

    private void addField(Document document, String label, String value) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", LABEL_FONT));
        paragraph.add(new Chunk(value, VALUE_FONT));
        paragraph.setSpacingAfter(2);
        document.add(paragraph);
    }

    private void addParagraph(Document document, String text) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, VALUE_FONT);
        paragraph.setSpacingAfter(4);
        document.add(paragraph);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph paragraph = new Paragraph("Gerado em " + DATETIME_FORMAT.format(LocalDateTime.now()), FOOTER_FONT);
        paragraph.setSpacingBefore(20);
        document.add(paragraph);
    }

    private void addWorkersTable(Document document, List<WorkerSummaryResponse> workers) throws DocumentException {
        if (workers.isEmpty()) {
            addParagraph(document, "Nenhum colaborador atribuído");
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeaderCell(table, "Nome");
        addTableHeaderCell(table, "Função");

        for (WorkerSummaryResponse worker : workers) {
            addTableCell(table, worker.name());
            addTableCell(table, valueOrDash(worker.function()));
        }

        document.add(table);
    }

    private void addItemsTable(Document document, List<MaterialRequestItemResponse> items) throws DocumentException {
        if (items.isEmpty()) {
            addParagraph(document, "Nenhum item cadastrado");
            return;
        }

        PdfPTable table = new PdfPTable(new float[] {3, 3, 1.5f, 1.5f, 2, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeaderCell(table, "Item");
        addTableHeaderCell(table, "Descrição");
        addTableHeaderCell(table, "Qtd.");
        addTableHeaderCell(table, "Unid.");
        addTableHeaderCell(table, "Marca");
        addTableHeaderCell(table, "Entregue");

        for (MaterialRequestItemResponse item : items) {
            addTableCell(table, item.name());
            addTableCell(table, valueOrDash(item.description()));
            addTableCell(table, item.quantity().toPlainString());
            addTableCell(table, item.unit().name());
            addTableCell(table, valueOrDash(item.brand()));
            addTableCell(table, item.quantityDelivered().toPlainString());
        }

        document.add(table);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, LABEL_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, VALUE_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String plannedTimeRange(WorkOrderResponse workOrder) {
        if (workOrder.plannedStartTime() == null && workOrder.plannedEndTime() == null) {
            return "—";
        }

        String start = workOrder.plannedStartTime() != null ? TIME_FORMAT.format(workOrder.plannedStartTime()) : "?";
        String end = workOrder.plannedEndTime() != null ? TIME_FORMAT.format(workOrder.plannedEndTime()) : "?";
        return start + " às " + end;
    }

    private String nameAndEmail(UserSummaryResponse user) {
        return user.name() + " (" + user.email() + ")";
    }

    private String format(LocalDate date) {
        return date != null ? DATE_FORMAT.format(date) : "—";
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }
}
