package de.georgrichter.vibrationdemoapp.audio;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipAudioResource {
    private static int idIterator;

    private ArrayList<byte[]> chunks;
    private int chunkIterator;
    private int resourceId;
    private Context context;
    private final int id;

    public ZipAudioResource(Context context, int resourceId){
        chunkIterator = 0;
        this.resourceId = resourceId;
        this.context = context;
        chunks = new ArrayList<>();
        id = idIterator++;
        OpenZip();
    }

    public byte[] getNextChunk(){
        return chunks.get(chunkIterator++);
    }

    public int getCurrentChunkId() {
        return chunkIterator - 1;
    }

    public int getSoundID(){
        return id;
    }

    private void OpenZip(){
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            ZipInputStream zipStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null){
                if(entry.getSize() > Integer.MAX_VALUE) throw new IllegalStateException("Chunk size is too large.");
                byte[] chunk = new byte[(int)entry.getCompressedSize()];
                zipStream.read(chunk);
                zipStream.closeEntry();
                chunks.add(chunk);
            }
            zipStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
