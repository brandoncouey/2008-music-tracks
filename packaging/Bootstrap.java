package com.brandon.hdos.launcher;

import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.JOptionPane;

/** Portable entry point; never copies user settings, login files, or sessions. */
public final class Bootstrap {
    public static void main(String[] args) throws Exception {
        try {
            Path root=Path.of(System.getProperty("hdos.bundle")).toAbsolutePath().normalize();
            Path plugin=root.resolve("plugin/hdos-music.jar");
            Path music=root.resolve("assets/music");
            Path cache=root.resolve("assets/cache");
            require(plugin);require(cache.resolve("main_file_cache.dat2"));
            for(int i:new int[]{4,14,15,255})require(cache.resolve("main_file_cache.idx"+i));
            long count;
            try(var paths=Files.list(music)){count=paths.filter(p->p.toString().endsWith(".mid")).count();}
            if(count!=826)throw new IOException("Expected 826 music files, found "+count+". Extract the entire ZIP first.");
            System.out.println("HDOS Music: "+count+" music files; portable cache and Java runtime found.");
            if(Arrays.asList(args).contains("--check")) {
                checkSynth(plugin,cache,music);
                System.out.println("CHECK PASSED. No RuneLite settings or login data were modified.");
                return;
            }
            Path target=Path.of(System.getProperty("user.home"),".runelite","sideloaded-plugins","hdos-music.jar");
            Files.createDirectories(target.getParent());
            if(Files.exists(target) && Files.mismatch(plugin,target)!=-1) {
                Path backup=target.resolveSibling("hdos-music.jar.backup-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
                Files.copy(target,backup);
                System.out.println("Previous HDOS plugin backed up to "+backup);
            }
            Files.copy(plugin,target,StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Starting official RuneLite 1.13.8 with the HDOS-Music-Portable configuration profile.");
            System.out.println("Sign in with your own account. Disable any other music replacement plugin.");
            Class.forName("net.runelite.client.RuneLite").getMethod("main",String[].class)
                .invoke(null,(Object)new String[]{"--developer-mode","--profile","HDOS-Music-Portable"});
        } catch(Throwable e) {
            Throwable cause=e instanceof InvocationTargetException && e.getCause()!=null ? e.getCause() : e;
            cause.printStackTrace();
            if(!Arrays.asList(args).contains("--check"))JOptionPane.showMessageDialog(null,"HDOS Music could not start:\n"+cause+"\n\nExtract the entire ZIP to a writable folder and read START HERE.txt.","HDOS Music",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    private static void require(Path path) throws IOException {if(!Files.isRegularFile(path))throw new IOException("Missing bundle file: "+path);}
    private static void checkSynth(Path plugin,Path cache,Path music) throws Exception {
        Path midi;
        try(var paths=Files.list(music)){midi=paths.filter(p->p.getFileName().toString().startsWith("00002")).findFirst().orElseThrow();}
        try(URLClassLoader loader=new URLClassLoader(new URL[]{plugin.toUri().toURL()},Bootstrap.class.getClassLoader())) {
            Class<?> type=loader.loadClass("main.HdosSynth");
            Object synth=type.getConstructor(Path.class,byte[].class).newInstance(cache,Files.readAllBytes(midi));
            try {
                byte[] bytes=new byte[1024];long nonzero=0;
                Method read=type.getMethod("read",byte[].class,double.class);
                for(int i=0;i<172;i++) {
                    read.invoke(synth,bytes,1d);
                    for(byte b:bytes)if(b!=0)nonzero++;
                }
                if(nonzero==0)throw new IOException("Native HDOS synthesis returned no audio for Autumn Voyage");
                System.out.println("Native HDOS instruments decoded successfully: "+nonzero+" nonzero PCM bytes in one second.");
            } finally {((AutoCloseable)synth).close();}
        }
    }
}
