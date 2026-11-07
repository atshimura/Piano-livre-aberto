package br.org.pianolivre;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Exposes only exported score files (PDF/PNG inside the app's private
 * external "partituras" directory) to apps chosen by the user through the
 * share sheet. Path traversal is rejected; nothing else is readable.
 */
public final class ScoreExportProvider extends ContentProvider {
    private File directory;
    @Override public boolean onCreate(){
        File root=getContext().getExternalFilesDir(null);
        directory=root==null?null:new File(root,"partituras");
        return true;
    }
    private File resolve(Uri uri)throws FileNotFoundException{
        String name=uri==null?null:uri.getLastPathSegment();
        if(name==null||name.contains("/")||name.contains("\\")||name.contains("..")
            ||(!name.endsWith(".pdf")&&!name.endsWith(".png")))
            throw new FileNotFoundException("Arquivo de exportação inválido");
        if(directory==null)throw new FileNotFoundException("Pasta de exportação indisponível");
        File file=new File(directory,name);
        try{
            if(!file.getCanonicalPath().startsWith(directory.getCanonicalPath()+File.separator))
                throw new FileNotFoundException("Caminho fora da pasta de exportação");
        }catch(IOException ex){throw new FileNotFoundException("Arquivo de exportação inválido");}
        if(!file.isFile())throw new FileNotFoundException("Exportação não encontrada");
        return file;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.parseMode("r"));
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String order){
        return null;
    }
    @Override public String getType(Uri uri){
        return uri.getLastPathSegment()!=null&&uri.getLastPathSegment().endsWith(".pdf")
            ?"application/pdf":"image/png";
    }
    @Override public Uri insert(Uri uri,ContentValues values){return null;}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs){return 0;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs){return 0;}
}
