package com.raincheque.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeService {
    
    @Value("${app.qr.secret:defaultSecretKey123!}")
    private String qrSecretKey;
    
    private static final int QR_CODE_SIZE = 300;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    /**
     * Generate QR code as Base64 string
     * @throws IOException 
     */
    public String generateQrCodeBase64(String data) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(
                data, 
                BarcodeFormat.QR_CODE, 
                QR_CODE_SIZE, 
                QR_CODE_SIZE, 
                hints
            );
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            return "data:image/png;base64," + 
                   Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
        } catch (WriterException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    /**
     * Generate ticket verification data
     */
    public String generateTicketVerificationData(Long ticketId, String ticketCode, String qrSecret) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String data = String.format("%d|%s|%s|%s", ticketId, ticketCode, qrSecret, timestamp);
        
        try {
            String signature = generateHmacSignature(data);
            return Base64.getEncoder().encodeToString((data + "|" + signature).getBytes());
        } catch (Exception e) {
            log.error("Error generating verification data", e);
            throw new RuntimeException("Failed to generate verification data", e);
        }
    }
    
    /**
     * Verify ticket authenticity
     */
    public boolean verifyTicket(String encodedData) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encodedData));
            String[] parts = decoded.split("\\|");
            
            if (parts.length < 5) {
                return false;
            }
            
            // Last part is signature, reconstruct original data
            String data = String.join("|", 
                parts[0], parts[1], parts[2], parts[3]);
            String providedSignature = parts[4];
            
            String calculatedSignature = generateHmacSignature(data);
            
            return calculatedSignature.equals(providedSignature);
            
        } catch (Exception e) {
            log.error("Error verifying ticket", e);
            return false;
        }
    }
    
    private String generateHmacSignature(String data) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            qrSecretKey.getBytes(), 
            HMAC_ALGORITHM
        );
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}