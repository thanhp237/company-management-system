package com.group3.company_management.core.controller;
import com.group3.company_management.core.dto.QuotationDetailResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/quotation")
public class QuotationController {

    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @GetMapping("/create/{customerId}")
    public String createPage(@PathVariable Long customerId, Model model) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        QuotationRequest quotationRequest = new QuotationRequest();
        quotationRequest.setCustomerId(customerId);

        model.addAttribute("customer", customer);
        model.addAttribute("products", productRepository.findByActiveTrue());
        model.addAttribute("quotationRequest", quotationRequest);

        return "quotation/create";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("quotationRequest") QuotationRequest request) {

        Long quotationId = quotationService.createQuotation(request);

        return "redirect:/quotation/detail/" + quotationId;
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {

        QuotationResponse quotation = quotationService.getQuotationDetail(id);

        model.addAttribute("quotation", quotation);

        return "quotation/detail";
    }
    @GetMapping("/export-pdf/{id}")
    public void exportPdf(@PathVariable Long id,
                          HttpServletResponse response) throws Exception {

        QuotationResponse quotation = quotationService.getQuotationDetail(id);

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=quotation-" + quotation.getQuotationCode() + ".pdf"
        );

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        document.add(new Paragraph("QUOTATION"));
        document.add(new Paragraph("Code: " + quotation.getQuotationCode()));
        document.add(new Paragraph("Customer: " + quotation.getCustomerName()));
        document.add(new Paragraph("Email: " + quotation.getCustomerEmail()));
        document.add(new Paragraph("Phone: " + quotation.getCustomerPhone()));
        document.add(new Paragraph("Address: " + quotation.getCustomerAddress()));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);

        table.addCell("Product");
        table.addCell("Description");
        table.addCell("Quantity");
        table.addCell("Unit Price");
        table.addCell("Total");

        for (QuotationDetailResponse item : quotation.getDetails()) {
            table.addCell(item.getProductName());
            table.addCell(item.getDescription());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(String.valueOf(item.getUnitPrice()));
            table.addCell(String.valueOf(item.getTotalPrice()));
        }

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Sub Total: " + quotation.getSubTotal()));
        document.add(new Paragraph("Discount: " + quotation.getDiscountAmount()));
        document.add(new Paragraph("Final Amount: " + quotation.getFinalAmount()));
        document.add(new Paragraph("Note: " + quotation.getNote()));

        document.close();
    }
}
