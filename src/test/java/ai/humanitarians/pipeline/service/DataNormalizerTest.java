package ai.humanitarians.pipeline.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataNormalizerTest {

    @Test
    @DisplayName("Email hygiene: trim whitespace, lowercase, and remove zero-width Unicode characters")
    void testNormalizeEmailHygiene() {
        assertEquals("jainil.shah@northeastern.edu", DataNormalizer.normalizeEmail("  Jainil.Shah@Northeastern.EDU  "));
        assertEquals("", DataNormalizer.normalizeEmail(null));
        assertEquals("user@example.com", DataNormalizer.normalizeEmail("USER@EXAMPLE.COM\u200B"));
        assertEquals("test@domain.org", DataNormalizer.normalizeEmail(" \uFEFFTEST@Domain.ORG\u200E "));
    }

    @Test
    @DisplayName("Extract YouTube ID across standard, short, embed, query, and raw formats")
    void testExtractYoutubeIdFormats() {
        String expected = "RzW17zhV4Nw";

        // Raw 11-char ID
        assertEquals(expected, DataNormalizer.extractYoutubeId("RzW17zhV4Nw"));
        assertEquals(expected, DataNormalizer.extractYoutubeId("  RzW17zhV4Nw  "));

        // Standard watch URL with query parameters
        assertEquals(expected, DataNormalizer.extractYoutubeId("https://www.youtube.com/watch?v=RzW17zhV4Nw"));
        assertEquals(expected, DataNormalizer.extractYoutubeId("https://www.youtube.com/watch?v=RzW17zhV4Nw&feature=shared&t=15"));

        // Short URL (youtu.be)
        assertEquals(expected, DataNormalizer.extractYoutubeId("https://youtu.be/RzW17zhV4Nw"));
        assertEquals(expected, DataNormalizer.extractYoutubeId("https://youtu.be/RzW17zhV4Nw?t=20"));

        // Embed URL
        assertEquals(expected, DataNormalizer.extractYoutubeId("https://www.youtube.com/embed/RzW17zhV4Nw"));

        // Empty / None
        assertNull(DataNormalizer.extractYoutubeId(null));
        assertNull(DataNormalizer.extractYoutubeId(""));
        assertNull(DataNormalizer.extractYoutubeId("   "));
    }

    @Test
    @DisplayName("Extract Drive file ID across file/d/, open?id=, and raw tokens")
    void testExtractDriveFileIdFormats() {
        String expected = "1ovAQ1N19bqlFgjhSuC8Aw4wnvSKzbTsFxt5qtOYzaY4";

        // Direct file/d/<id>/view URL
        String url1 = "https://drive.google.com/file/d/" + expected + "/view?usp=sharing";
        assertEquals(expected, DataNormalizer.extractDriveFileId(url1));

        // Open query param URL
        String url2 = "https://drive.google.com/open?id=" + expected;
        assertEquals(expected, DataNormalizer.extractDriveFileId(url2));

        // Raw token ID
        assertEquals("drive_file_101", DataNormalizer.extractDriveFileId("drive_file_101"));
        assertNull(DataNormalizer.extractDriveFileId(null));
        assertNull(DataNormalizer.extractDriveFileId(""));
    }

    @Test
    @DisplayName("Status normalization: lowercase, hyphen/space to underscore")
    void testNormalizeStatus() {
        assertEquals("in_review", DataNormalizer.normalizeStatus("In Review", "submitted"));
        assertEquals("needs_modification", DataNormalizer.normalizeStatus("Needs-Modification", "submitted"));
        assertEquals("published", DataNormalizer.normalizeStatus("  PUBLISHED  ", "submitted"));
        assertEquals("submitted", DataNormalizer.normalizeStatus(null, "submitted"));
    }

    @Test
    @DisplayName("Clean text: trim whitespace and convert empty to null")
    void testCleanText() {
        assertEquals("Hello World", DataNormalizer.cleanText("  Hello World  "));
        assertNull(DataNormalizer.cleanText("   "));
        assertNull(DataNormalizer.cleanText(null));
    }
}
