package com.brandon.music2008;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class TrackCatalogTest {
    @Test public void recognizesDifferentArchiveIdsByName() {
        TrackCatalog c=new TrackCatalog();c.add(0,1120933843);
        assertEquals(Integer.valueOf(0),c.archiveFor("Scape Main"));
        assertEquals(Integer.valueOf(0),c.archiveFor("<col=ff0000>SCAPE MAIN</col>"));
        assertNull(c.archiveFor("New song without replacement"));
    }
    @Test public void rejectsAmbiguousHashesInsteadOfChoosingWrongSong() {
        TrackCatalog c=new TrackCatalog();int h=TrackCatalog.nameHash("Harmony");
        c.add(12,h);c.add(12,h);assertEquals(Integer.valueOf(12),c.archiveFor("Harmony"));
        c.add(99,h);c.add(100,h);assertNull(c.archiveFor("Harmony"));
    }
    @Test public void loadsRenamedFilesAndLeavesMissingFilesUnmapped() throws Exception {
        Path dir=Files.createTempDirectory("music2008-catalog-test");
        try {
            Path midi=dir.resolve("00000 - Scape Main.mid");Files.write(midi,new byte[]{1});
            TrackCatalog c=TrackCatalog.load(dir);
            assertEquals(midi,c.midiFor("Scape Main"));assertNull(c.midiFor("Autumn Voyage"));
            Files.delete(midi);
        } finally {Files.delete(dir);}
    }
}
