import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;


public class VMWriter {

    public static enum SEGMENT {CONST,ARG,LOCAL,STATIC,THIS,THAT,POINTER,TEMP,NONE};
    public static enum COMMAND {ADD,SUB,NEG,EQ,GT,LT,AND,OR,NOT};

    private static HashMap<SEGMENT,String> segments = new HashMap<SEGMENT, String>(){{
        put(SEGMENT.CONST,"constant");
        put(SEGMENT.ARG,"argument");
        put(SEGMENT.LOCAL,"local");
        put(SEGMENT.STATIC,"static");
        put(SEGMENT.THIS,"this");
        put(SEGMENT.THAT,"that");
        put(SEGMENT.POINTER,"pointer");
        put(SEGMENT.TEMP,"temp");
    }};
    private static HashMap<COMMAND,String> commands = new HashMap<COMMAND, String>(){{
        put(COMMAND.ADD,"add");
        put(COMMAND.SUB,"sub");
        put(COMMAND.NEG,"neg");
        put(COMMAND.EQ,"eq");
        put(COMMAND.GT,"gt");
        put(COMMAND.LT,"lt");
        put(COMMAND.AND,"and");
        put(COMMAND.OR,"or");
        put(COMMAND.NOT,"not");
    }};
    private PrintWriter printWriter;


    public VMWriter(OutputStream out) {

        
            printWriter = new PrintWriter(out);
       

    }


    public void writePush(SEGMENT segment, int index){
        writeCommand("push",segments.get(segment),String.valueOf(index));
    }

    public void writePop(SEGMENT segment, int index){
        writeCommand("pop",segments.get(segment),String.valueOf(index));
    }

    
    public void writeArithmetic(COMMAND command){
        writeCommand(commands.get(command),"","");
    }


    public void writeLabel(String label){
        writeCommand("label",label,"");
    }


    public void writeGoto(String label){
        writeCommand("goto",label,"");
    }

    public void writeIf(String label){
        writeCommand("if-goto",label,"");
    }


    public void writeCall(String name, int nArgs){
        writeCommand("call",name,String.valueOf(nArgs));
    }


    public void writeFunction(String name, int nLocals){
        writeCommand("function",name,String.valueOf(nLocals));
    }

    public void writeReturn(){
        writeCommand("return","","");
    }

    public void writeCommand(String cmd, String arg1, String arg2){

        printWriter.print(cmd + " " + arg1 + " " + arg2 + "\n");

    }

    public void close(){
        printWriter.close();
    }
}