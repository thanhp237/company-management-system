package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.*;
import com.group3.company_management.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class VnPayService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository transactionRepository;
    @Value("${vnpay.tmn-code:}") private String tmnCode;
    @Value("${vnpay.hash-secret:}") private String hashSecret;
    @Value("${vnpay.payment-url}") private String paymentUrl;
    @Value("${vnpay.return-url}") private String returnUrl;

    @Transactional
    public String createUrl(Long invoiceId, Long customerId, String ip) {
        if (tmnCode.isBlank() || hashSecret.isBlank()) throw new RuntimeException("VNPAY chưa được cấu hình.");
        Invoice invoice=invoiceRepository.findById(invoiceId).orElseThrow(()->new RuntimeException("Không tìm thấy hóa đơn."));
        if (invoice.getContract().getCustomer()==null || !customerId.equals(invoice.getContract().getCustomer().getId()))
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn này.");
        if (invoice.getStatus()!=Invoice.InvoiceStatus.ISSUED) throw new RuntimeException("Hóa đơn chưa sẵn sàng thanh toán.");
        String ref="INV"+invoiceId+"-"+System.currentTimeMillis();
        transactionRepository.save(PaymentTransaction.builder().invoice(invoice).txnRef(ref).amount(invoice.getOutstandingAmount()).build());
        ZonedDateTime now=ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<String,String> p=new TreeMap<>();
        p.put("vnp_Version","2.1.0"); p.put("vnp_Command","pay"); p.put("vnp_TmnCode",tmnCode);
        p.put("vnp_Amount",invoice.getOutstandingAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        p.put("vnp_CurrCode","VND"); p.put("vnp_TxnRef",ref); p.put("vnp_OrderInfo","Thanh toan hoa don "+invoice.getInvoiceCode());
        p.put("vnp_OrderType","other"); p.put("vnp_Locale","vn"); p.put("vnp_ReturnUrl",returnUrl);
        p.put("vnp_IpAddr",ip); p.put("vnp_CreateDate",now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        p.put("vnp_ExpireDate",now.plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        String data=query(p); return paymentUrl+"?"+data+"&vnp_SecureHash="+hmac(data);
    }
    public boolean valid(Map<String,String> input) {
        String received=input.get("vnp_SecureHash"); Map<String,String> copy=new TreeMap<>(input);
        copy.remove("vnp_SecureHash"); copy.remove("vnp_SecureHashType");
        return received!=null && received.equalsIgnoreCase(hmac(query(copy)));
    }
    @Transactional
    public Map<String,String> ipn(Map<String,String> p) {
        if(!valid(p)) return Map.of("RspCode","97","Message","Invalid signature");
        PaymentTransaction tx=transactionRepository.findByTxnRef(p.get("vnp_TxnRef")).orElse(null);
        if(tx==null) return Map.of("RspCode","01","Message","Order not found");
        BigDecimal callback=new BigDecimal(p.getOrDefault("vnp_Amount","0")).divide(BigDecimal.valueOf(100));
        if(tx.getAmount().compareTo(callback)!=0) return Map.of("RspCode","04","Message","Invalid amount");
        if(tx.getStatus()!=PaymentTransaction.Status.PENDING) return Map.of("RspCode","02","Message","Already confirmed");
        boolean ok="00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
        tx.setStatus(ok?PaymentTransaction.Status.SUCCESS:PaymentTransaction.Status.FAILED);
        tx.setResponseCode(p.get("vnp_ResponseCode")); tx.setVnpTransactionNo(p.get("vnp_TransactionNo")); tx.setCompletedAt(LocalDateTime.now());
        if(ok){ Invoice inv=tx.getInvoice(); inv.setPaidAmount(inv.getTotalAmount()); inv.setOutstandingAmount(BigDecimal.ZERO); inv.setStatus(Invoice.InvoiceStatus.PAID); }
        return Map.of("RspCode","00","Message","Confirm Success");
    }
    @Transactional
    public boolean confirmFromReturn(Map<String,String> p) {
        if(!valid(p)) return false;
        PaymentTransaction tx=transactionRepository.findByTxnRef(p.get("vnp_TxnRef")).orElse(null);
        if(tx==null) return false;
        BigDecimal callback=new BigDecimal(p.getOrDefault("vnp_Amount","0")).divide(BigDecimal.valueOf(100));
        if(tx.getAmount().compareTo(callback)!=0) return false;
        if(tx.getStatus()!=PaymentTransaction.Status.PENDING) return tx.getStatus()==PaymentTransaction.Status.SUCCESS;
        boolean ok="00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.getOrDefault("vnp_TransactionStatus", p.get("vnp_ResponseCode")));
        tx.setStatus(ok?PaymentTransaction.Status.SUCCESS:PaymentTransaction.Status.FAILED);
        tx.setResponseCode(p.get("vnp_ResponseCode")); tx.setVnpTransactionNo(p.get("vnp_TransactionNo")); tx.setCompletedAt(LocalDateTime.now());
        if(ok){ Invoice inv=tx.getInvoice(); inv.setPaidAmount(inv.getTotalAmount()); inv.setOutstandingAmount(BigDecimal.ZERO); inv.setStatus(Invoice.InvoiceStatus.PAID); }
        return ok;
    }
    private String query(Map<String,String> p){ return p.entrySet().stream().filter(e->e.getValue()!=null&&!e.getValue().isBlank()).map(e->enc(e.getKey())+"="+enc(e.getValue())).reduce((a,b)->a+"&"+b).orElse(""); }
    private String enc(String s){ return URLEncoder.encode(s,StandardCharsets.UTF_8); }
    private String hmac(String data){ try{ Mac m=Mac.getInstance("HmacSHA512");m.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);} }
}
