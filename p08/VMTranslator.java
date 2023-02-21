import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.File;

public class VMTranslator {
    
    static class Parser {
        
        private Reader reader;
        private BufferedReader buffReader;
        private String curCmd = ""; // the current CMD that read
        
        public enum Cmd { // CMD types
            C_ARITHMETIC,
            C_PUSH,
            C_POP,
            C_LABEL,
            C_GOTO,
            C_IF,
            C_FUNCTION,
            C_RETURN,
            C_CALL,
        }

        public Parser(InputStream in) {  // init for reading
            reader = new InputStreamReader(in);
            buffReader = new BufferedReader(reader);
        }

        public String getCmd() {
            return this.curCmd;
        }

        public Boolean hasMoreLines() throws IOException { // check if there is more lines to read in the input stream
            buffReader.mark(100);
            if (buffReader.readLine() != null) {
                buffReader.reset();
                return true;
            }
            buffReader.reset();
            return false;
        }

        public void advance() throws IOException { // reads and set the curCmd to the next CMD in input stream

            if (hasMoreLines()) {
                String commandLine = buffReader.readLine().replaceAll("//.*", ""); // delete comments
                
                while((hasMoreLines()) && (commandLine.length() == 0)) { // ignores empty lines
                commandLine = buffReader.readLine().replaceAll("//.*", "");
                }
                curCmd = commandLine.replaceAll("\\s+$", "");
           }
        }
         
        public Cmd commandType() { // return the CMD type 
            String cmd = curCmd.split(" ")[0]; 
            if (cmd.equals("push")) {
                return Cmd.C_PUSH;
            }
            else if (cmd.equals("pop")) {
                return Cmd.C_POP;
            }
            else if ((cmd.equals("add") || (cmd.equals( "sub")) || (cmd.equals( "neg")) ||
                    (cmd.equals("eq")) || (cmd.equals("gt")) || (cmd.equals("lt")) ||
                    (cmd.equals("and")) || (cmd.equals("or")) || (cmd.equals("not")))) {

                    return Cmd.C_ARITHMETIC;
                    }
            else if (cmd.equals("label")) {
                    return Cmd.C_LABEL;
            }
            else if (cmd.equals("call")) {
                return Cmd.C_CALL;
            }
            else if (cmd.equals("return")) {
                return Cmd.C_RETURN;
            }
            else if (cmd.equals("goto")) {
                return Cmd.C_GOTO;
            }
            else if(cmd.equals("function")){
                return Cmd.C_FUNCTION;
            }
            return Cmd.C_IF;
                
            
        }

        public String arg1() { // return the first argument in the CMD according to its type
            if(commandType().equals(Cmd.C_ARITHMETIC)) {
              return curCmd;
            }
            else {
                return curCmd.split(" ")[1];
            }
        }

        public int arg2() { // return the second argument for C_PUSH/POP
            return Integer.parseInt(curCmd.split(" ")[2]);
        }
    }


    static class CodeWriter {

        private Writer writer;
        private BufferedWriter buffWriter;
        private HashMap<String, String> segmentConverter = new HashMap<String, String>(){{
            put("local", "LCL");
            put("argument", "ARG");
            put("this", "THIS");
            put("that", "THAT");
        }}; // map for converting segment names to asm symbols
        private String fileName = "";
        private int id = 0; // id for loop symbols and functions
        
        public CodeWriter(OutputStream out) { // init for writing to output stream
            writer = new OutputStreamWriter(out);
            buffWriter = new BufferedWriter(writer);
        }

        public void setFileName (String fileName) { // set the fileName
            this.fileName = fileName;
        }
        

        public void bootstrap() throws IOException{ // bootstrapping call sys main
            buffWriter.write("@256\n" +
                            "D=A\n" +
                            "@SP\n" +
                            "M=D\n");
            writeCall("Sys.init", 0);
        } 
        public void writeLabel (String label) throws IOException { // write label 
            
            buffWriter.write("(" + label + ")\n");
        }

        public void writeGoto (String label) throws IOException{  // write goto command
            String str = "@" + label + "\n" +
                         "0;JMP\n";
            buffWriter.write(str);
        }

        public void writeIf (String label) throws IOException { // write if goto command
            String str = "@SP\n" +
            "M=M-1\n" +
            "A=M\n" +
            "D=M\n" +
            "@" + label + "\n" +
            "D;JNE\n";
            buffWriter.write(str);   
        }

        public void writeFunction (String functionName, int nVars) throws IOException { // write function
            writeLabel(functionName); // first create the relevant label
            for (int i = 0; i < nVars; ++i) { // initiallize nVars 
                writePushConstant("0");
            }
        }

        public void writeCall (String functionName, int nArgs) throws IOException { // write call
            //save return
            buffWriter.write("@returnAddress" + id + "\n");
            buffWriter.write("D=A\n");
            String str1 = 
            "@SP\n" +
            "A=M\n" +
            "M=D\n" +
            "@SP\n" +
            "M=M+1\n";
            buffWriter.write(str1); 
            
            //save LCL of the caller
            writePushFromAddr("LCL"); 
            
            //save ARG of the caller
            writePushFromAddr("ARG");
            
            //save THIS of the caller
            writePushFromAddr("THIS");

            //save THAT of the caller
            writePushFromAddr("THAT");

            //saved all, now reposition ARG (SP-5-nArgs)
            String str2 = 
            "@SP\n" +
            "D=M\n" +
            "@" + nArgs + "\n" +
            "D=D-A\n" +
            "@5\n" +
            "D=D-A\n" +
            "@ARG\n" +
            "M=D\n";
            buffWriter.write(str2);
            
            //reposition LCL 
            buffWriter.write("@SP\n");
            buffWriter.write("D=M\n");
            buffWriter.write("@LCL\n");
            buffWriter.write("M=D\n");

            //tranfers control to the called function
            writeGoto(functionName);

            //declares a label for the returnAddress
            writeLabel("returnAddress" + id);

            id++;
        }

        public void writeReturn () throws IOException {
            buffWriter.write("@LCL\n" +
                        "D=M\n" +
                        "@endFrame" + id + "\n" +
                        "M=D\n" +
                        "@5\n" +
                        "A=D-A\n" +
                        "D=M\n" +
                        "@retAddr" + id + "\n" +
                        "M=D\n" +
                        "@SP\n" +
                        "M=M-1\n" +
                        "A=M\n" +
                        "D=M\n" +
                        "@ARG\n" +
                        "A=M\n" +
                        "M=D\n" +
                        "@ARG\n" +
                        "D=M+1\n" +
                        "@SP\n" +
                        "M=D\n");
            buffWriter.write(
                            "@endFrame" + id + "\n" +
                            "A=M-1\n" +
                            "D=M\n" +
                            "@THAT\n" +
                            "M=D\n");
            // return THIS, ARG, LCL to thier original values
            reinstateSegments("THIS", 2);
            reinstateSegments("ARG", 3);
            reinstateSegments("LCL", 4);
            buffWriter.write(
                        "@retAddr" + id + "\n" +
                        "A=M\n" +
                        "0;JMP\n");
            id++;
        }

        public void reinstateSegments(String Addr, int index) throws IOException{ 
            buffWriter.write(
                        "@endFrame" + id + "\n" +
                        "D=M\n" +
                        "@" + index + "\n" +
                        "A=D-A\n" +
                        "D=M\n" +
                        "@" + Addr + "\n" +
                        "M=D\n");
        }

        public void writeEq(String EqType) throws IOException { // write asm code for equailities CMD's
            String str = "@SP\n" +
            "A=M-1\n" +
            "D=M\n" +
            "A=A-1\n" +
            "D=M-D\n" +
            "@EQUAL" + id + "\n" +
            "D;" + EqType + "\n" +
            "@SP\n" +
            "A=M-1\n" +
            "A=A-1\n" +
            "M=0\n" +
            "@ENDEQ" + id + "\n" +
            "0;JMP\n" +
            "(EQUAL" + id + ")\n" +
            "@SP\n" +
            "A=M-1\n" +
            "A=A-1\n" +
            "M=-1\n" +
            "(ENDEQ" + id + ")\n" +
            "@SP\n" +
            "M=M-1\n";
            buffWriter.write(str);
        }

        public void writeOpSignTwoArg(char opSign) throws IOException { // write asm code for oprations on two arguments
            String str = "@SP\n" +
            "A=M-1\n" +
            "D=M\n" +
            "A=A-1\n" +
            "M=M" + opSign +"D\n" +
            "@SP\n" +
            "M=M-1\n"; 
            buffWriter.write(str);      
        }

        public void writeOpSignOneArg(char opSign) throws IOException { // write asm code for oprations on one arguments
            String str = "@SP\n" +
            "A=M-1\n" +
            "M=" + opSign + "M\n"; 
            buffWriter.write(str);  
        }
      
        public void writeArithmetic(String cmd) throws IOException { // handle arithmentic CMD

            switch(cmd){
                case "add":
                    writeOpSignTwoArg('+');
                    break;
                case "sub":
                    writeOpSignTwoArg('-');
                    break;
                case "neg":
                    writeOpSignOneArg('-');
                    break;
                case "eq":
                    writeEq("JEQ");
                    id++;
                    break;
                case "gt":
                    writeEq("JGT");
                    id++;
                    break;
                case "lt":
                    writeEq("JLT");
                    id++;
                    break;
                case "and":
                    writeOpSignTwoArg('&');
                    break;
                case "or":
                    writeOpSignTwoArg('|');
                    break;
                case "not":
                    writeOpSignOneArg('!');
                    break;
            }
        }
       
        public void writePushFromAddrSegment (String startingAddr, String index) throws IOException { // write asm code for CMDs that pushes data from RAM[RAM[startingAddr] + index] to stack
            String str = "@" + startingAddr + "\n" +
            "D=M\n" +
            "@" + index + "\n" +
            "A=D+A\n" +
            "D=M\n" +
            "@SP\n" +
            "A=M\n" +
            "M=D\n" +
            "@SP\n" +
            "M=M+1\n"; 
            buffWriter.write(str);   
        }

        public void writePushConstant (String value) throws IOException{ // write asm code for CMDs that pushes value to stack
            String str = "@" + value + "\n" +
                        "D=A\n" +
                        "@SP\n" +
                        "M=M+1\n" +
                        "A=M-1\n" +
                        "M=D\n";
            buffWriter.write(str);

        }
     
        public void writePushFromAddr (String Addr) throws IOException { // write asm code for CMDs that pushes data from RAM[Addr] to stack
            String str = "@" + Addr + "\n" +
            "D=M\n" +
            "@SP\n" +
            "A=M\n" +
            "M=D\n" +
            "@SP\n" +
            "M=M+1\n"; 
            buffWriter.write(str);   
        }

        public void writePopToAddrSegment (String startingAddr, String index) throws IOException { // write asm code for CMDs that pops data from stack to RAM[RAM[startingAddr] + index] 
            String str = "@SP\n" +
            "M=M-1\n" +
            "@" + startingAddr + "\n" +
            "D=M\n" +
            "@" + index + "\n" +
            "D=D+A\n" +
            "@R13\n" +
            "M=D\n" +
            "@SP\n" +
            "A=M\n" +
            "D=M\n" +
            "@R13\n" +
            "A=M\n" +
            "M=D\n";
            buffWriter.write(str);
        }

        public void writePopToAddr (String Addr) throws IOException { // write asm code for CMDs that pops data from stack to RAM[Addr] 
            String str = "@SP\n" +
            "M=M-1\n" +
            "A=M\n" +
            "D=M\n" +
            "@" + Addr + "\n" +
            "M=D\n";
            buffWriter.write(str);
        }
        
        public void writePushPop (String cmd, String segment, int index) throws IOException { // handle Push/Pop CMDs
            segment = segmentConverter.containsKey(segment) ? segmentConverter.get(segment) : segment; // convert segment name to asm symbol if needed
            if (cmd.equals("push")){
                switch(segment){
                    case "constant":
                        writePushConstant(Integer.toString(index));
                        break;
                    case "static":
                        buffWriter.write("@" + fileName + index + "\n" +
                                        "D=M\n" +
                                        "@SP\n" +
                                        "A=M\n" +
                                        "M=D\n" +
                                        "@SP\n" +
                                        "M=M+1\n");
                        break;
                    case "pointer":
                        writePushFromAddr(index == 1 ? "4" : "3"); // 4 = THAT , 3 = THIS
                        break;
                    case "temp":
                        writePushFromAddr(Integer.toString(index+5));
                        break;
                    default:
                        writePushFromAddrSegment(segment, Integer.toString(index));
                        break;
                }
            }
            else {
                switch(segment){
                    case "static":
                        buffWriter.write("@" + fileName + index + "\n" +
                                        "D=A\n" +
                                        "@R13\n" +
                                        "M=D\n" +
                                        "@SP\n" +
                                        "AM=M-1\n" +
                                        "D=M\n" +
                                        "@R13\n" +
                                        "A=M\n" +
                                        "M=D\n");
                        break;
                    case "pointer":
                        writePopToAddr(index == 1 ? "4" : "3"); // 4 = THAT , 3 = THIS
                        break;
                    case "temp":
                        writePopToAddr(Integer.toString(index+5));
                        break;
                    default:
                        writePopToAddrSegment(segment, Integer.toString(index));
                        break;
                }
            }
        }

        public void close() throws IOException { // closes writer 
            buffWriter.close();
        }
    }
    public static void main(String[] args) throws IOException{
        List<File> files = new ArrayList<File>(); 
        File input = new File(args[0]);
        String outputPath = ""; 
        if (input.isFile()) { // if the input is a single file, we create an .asm version for output
            files.add(input);
            outputPath = input.getPath().replace(".vm", ".asm");
        }
        else if (input.isDirectory()){ // create .asm file in the input directory with the name of the directory
            outputPath = input.getPath() + "/" + input.getName() + ".asm";
            for (File f : input.listFiles()){
                if (f.getName().contains(".vm")){ // get all .vm files from the directory
                    files.add(f);
                }
                
            }
        }
        OutputStream out = new FileOutputStream(outputPath); // init output stream as file.asm (if doesn't exist creates it in the same location as input)
        CodeWriter code = new CodeWriter(out);
        if (files.size() > 1){
            code.bootstrap();
        }
        
        for (File f : files){
            InputStream in = new FileInputStream(f); // init input stream from user's input (.vm file path)
            Parser parser = new Parser(in);
            code.setFileName(f.getName()); // sets fileName to current file name
            while(parser.hasMoreLines()){ // reads input file
                parser.advance();
                if(parser.commandType() == Parser.Cmd.C_ARITHMETIC){ // handle arithmetic
                    code.writeArithmetic(parser.arg1());
                }
                else if (parser.commandType() == Parser.Cmd.C_PUSH || parser.commandType() == Parser.Cmd.C_POP) { // handle Push/Pop 
                    code.writePushPop(parser.curCmd.split(" ")[0], parser.arg1(), parser.arg2());
                }
                else if (parser.commandType() == Parser.Cmd.C_CALL){
                    code.writeCall(parser.arg1(), parser.arg2());
                    
                }
                else if (parser.commandType() == Parser.Cmd.C_FUNCTION){ // handle new function
                    code.writeFunction(parser.arg1(), parser.arg2());
                }
                else if (parser.commandType() == Parser.Cmd.C_RETURN){ // handle return
                    code.writeReturn();
                }
                else if (parser.commandType() == Parser.Cmd.C_GOTO){ // handle goto commands
                    code.writeGoto(parser.arg1());
                }
                else if (parser.commandType() == Parser.Cmd.C_IF){ // handle if goto commands
                    code.writeIf(parser.arg1());
                }
                else if (parser.commandType() == Parser.Cmd.C_LABEL){ // creates labels
                    code.writeLabel(parser.arg1());
                }
            }
            
        }
        code.close();
    }
        
} 