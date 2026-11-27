package com.brandon.music2008;

import org.junit.Test;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class PortableConfigTest {
    @Test public void resolvesAssetsRelativeToBundleWithoutUserHomeDir() {
        String previous=System.getProperty("music2008.bundle");
        try {
            String root=Path.of("C:\\Example User\\Downloads\\2008 Music Soundtrack").toString();
            System.setProperty("music2008.bundle",root);
            Music2008Config config=new Music2008Config(){};
            assertEquals(Path.of(root,"assets","cache").toString(),config.cacheFolder());
            assertEquals(Path.of(root,"assets","music").toString(),config.midiFolder());
        } finally {if(previous==null)System.clearProperty("music2008.bundle");else System.setProperty("music2008.bundle",previous);}
    }
}
