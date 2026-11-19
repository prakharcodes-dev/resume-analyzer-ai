package com.career.resumeanalyzer.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

@Service
public class ResumeUploadValidator {
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final String PDF_MIME = "application/pdf";
    public static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public String validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("The selected file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("The file exceeds the 10 MB limit.");
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.replace('\\', '/').toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        if (lowerName.endsWith(".pdf")) {
            validatePdf(bytes);
            return PDF_MIME;
        }
        if (lowerName.endsWith(".docx")) {
            validateDocx(bytes);
            return DOCX_MIME;
        }
        throw new IllegalArgumentException("Only PDF and DOCX files are supported.");
    }

    private void validatePdf(byte[] bytes) throws IOException {
        if (!startsWith(bytes, "%PDF-")) throw new IllegalArgumentException("The file content is not a PDF document.");
        try (PDDocument ignored = Loader.loadPDF(bytes)) {
            if (ignored.isEncrypted()) throw new IllegalArgumentException("Password-protected PDF files are not supported.");
        }
    }

    private void validateDocx(byte[] bytes) throws IOException {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IllegalArgumentException("The file content is not a DOCX document.");
        }
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                hasContentTypes |= entry.getName().equals("[Content_Types].xml");
                hasDocument |= entry.getName().equals("word/document.xml");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("The DOCX archive is invalid.");
        }
        if (!hasContentTypes || !hasDocument) throw new IllegalArgumentException("The DOCX document is incomplete or invalid.");
        try (XWPFDocument ignored = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            ignored.getParagraphs();
        } catch (Exception e) {
            throw new IllegalArgumentException("The DOCX document is invalid or unreadable.");
        }
    }

    private boolean startsWith(byte[] bytes, String prefix) {
        if (bytes.length < prefix.length()) return false;
        for (int index = 0; index < prefix.length(); index++) {
            if (bytes[index] != prefix.charAt(index)) return false;
        }
        return true;
    }
}
