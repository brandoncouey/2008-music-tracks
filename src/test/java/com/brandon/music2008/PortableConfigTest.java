package com.brandon.music2008;

import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class PortableConfigTest {
    @Test public void resolvesAssetsRelativeToBundleWithoutUserHomeDir() {
        String previous = System.getProperty("music2008.bundle");
        try {
            String root = new File("C:\\Example User\\Downloads\\2008 Music Soundtrack").getPath();
            System.setProperty("music2008.bundle", root);
            Music2008Config config = new Music2008Config(){};
            assertEquals(new File(new File(root, "assets"), "music").getPath(), config.midiFolder());
            assertEquals(new File(new File(root, "assets"), "fanfares").getPath(), config.fanfareFolder());
        } finally {
            if (previous == null) System.clearProperty("music2008.bundle");
            else System.setProperty("music2008.bundle", previous);
        }
    }
}
