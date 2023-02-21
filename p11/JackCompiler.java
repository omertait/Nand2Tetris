import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JackCompiler {
    public static void main(String[] args) throws IOException{
        List<File> files = new ArrayList<File>(); 
        File input = new File(args[0]);

        if (input.isFile()) { // if the input is a single file
            files.add(input);
           
        }
        else if (input.isDirectory()){ //add each file in input directory 
            for (File f : input.listFiles()){
                if (f.getName().contains(".jack")){ // get all .jack files from the directory
                    files.add(f);
                }
                
            }
        }
        
        
       
        
        for (File f : files){ // compile each file in files list
            String outputPath = f.getPath().replace(".jack", ".vm");
            OutputStream out = new FileOutputStream(outputPath); // init output stream as file.vm (if doesn't exist creates it in the same location as input)
            InputStream in = new FileInputStream(f); // init input stream from user's input (.jack file path)
            CompilationEngine engine = new CompilationEngine(in,out);
            engine.compileClass();
           
           
            
        }
    }
}