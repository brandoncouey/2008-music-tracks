package com.brandon.music2008;

import javax.sound.midi.SysexMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class MasterVolumeTest {
    @Test public void fullVolumeIsMaxFourteenBitValue() throws Exception {
        SysexMessage m = PlaybackEngine.masterVolumeMessage(1.0);
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x7F, 0x7F, 0x04, 0x01, 0x7F, 0x7F, (byte) 0xF7}, m.getMessage());
    }
    @Test public void quarterAmplitudeSendsHalfTheRange() throws Exception {
        byte[] m = PlaybackEngine.masterVolumeMessage(0.25).getMessage();
        int v = (m[5] & 0x7F) | ((m[6] & 0x7F) << 7);
        assertEquals(0x3FFF / 2, v, 1);
    }
    @Test public void silenceAndClampingWork() throws Exception {
        assertArrayEquals(new byte[]{(byte) 0xF0, 0x7F, 0x7F, 0x04, 0x01, 0x00, 0x00, (byte) 0xF7}, PlaybackEngine.masterVolumeMessage(-3).getMessage());
        assertArrayEquals(PlaybackEngine.masterVolumeMessage(1.0).getMessage(), PlaybackEngine.masterVolumeMessage(7).getMessage());
    }
}
