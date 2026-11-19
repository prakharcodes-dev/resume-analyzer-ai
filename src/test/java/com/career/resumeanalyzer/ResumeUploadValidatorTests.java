package com.career.resumeanalyzer;

import com.career.resumeanalyzer.service.ResumeUploadValidator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResumeUploadValidatorTests {
    private final ResumeUploadValidator validator = new ResumeUploadValidator();

    @Test
    void acceptsStructurallyValidPdfAndDocxEvenWhenBrowserMimeIsUnreliable() throws Exception {
        assertEquals(ResumeUploadValidator.PDF_MIME, validator.validate(new MockMultipartFile(
                "file", "resume.pdf", "text/plain", createPdf())));
        assertEquals(ResumeUploadValidator.DOCX_MIME, validator.validate(new MockMultipartFile(
                "file", "resume.docx", "application/octet-stream", createDocx())));
    }

    @Test
    void rejectsUnsupportedExtensionEmptyCorruptAndOversizedFiles() throws Exception {
        byte[] pdf = createPdf();
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "resume.doc", "application/msword", pdf)));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0])));
        assertThrows(Exception.class, () -> validator.validate(new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile(
                "file", "large.pdf", "application/pdf", new byte[(int) ResumeUploadValidator.MAX_FILE_SIZE + 1])));
    }

    @Test
    void acceptsPathLikeOriginalNameOnlyAsAnInputLabelAndDoesNotTrustMimeType() throws Exception {
        assertEquals(ResumeUploadValidator.PDF_MIME, validator.validate(new MockMultipartFile(
                "file", "../../private.pdf", "application/octet-stream", createPdf())));
    }

    private byte[] createPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Candidate resume content");
            document.write(output);
            return output.toByteArray();
        }
    }
}
