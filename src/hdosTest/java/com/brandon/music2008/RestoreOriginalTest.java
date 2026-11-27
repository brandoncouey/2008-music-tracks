package com.brandon.music2008;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class RestoreOriginalTest {
    @Test public void restoresVolumeWithoutRunningPacketSendingScripts() throws Exception {
        AtomicInteger volume=new AtomicInteger(-1);
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args)->{
            switch(method.getName()) {
                case "getVarpValue": return 100;
                case "getGameState": return GameState.LOGGED_IN;
                case "setMusicVolume": volume.set((Integer)args[0]);return null;
                case "runScript": throw new AssertionError("Restoration must not invoke packet-sending scripts");
                default: throw new AssertionError("Unexpected API call: "+method.getName());
            }
        });
        Music2008Plugin plugin=new Music2008Plugin();
        Field field=Music2008Plugin.class.getDeclaredField("client");field.setAccessible(true);field.set(plugin,client);
        Field muted=Music2008Plugin.class.getDeclaredField("mutedByUs");muted.setAccessible(true);muted.setBoolean(plugin,true);
        Method restore=Music2008Plugin.class.getDeclaredMethod("restoreOriginal");restore.setAccessible(true);
        restore.invoke(plugin);
        assertEquals(255,volume.get());assertFalse(muted.getBoolean(plugin));
    }
}
