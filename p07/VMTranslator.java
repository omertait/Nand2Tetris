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
import java.util.HashMap;



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
                curCmd = commandLine;
           }
        }
         
        public Cmd commandType() { // return the CMD type 
            String cmd = curCmd.split(" ")[0]; 
            if (cmd.equals("push")) {
                return Cmd.C_PUSH;
            }
            if (cmd.equals("pop")) {
                return Cmd.C_POP;
            }
            return Cmd.C_ARITHMETIC;
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
        }}; // map for converting jack segment names to asm symbols
        private int id = 0; // id for loop symbols
        
        public CodeWriter(OutputStream out) { // init for writing to output stream
            writer = new OutputStreamWriter(out);
            buffWriter = new BufferedWriter(writer);
        }


        public void writeEq(String EqType, String id) throws IOException { // write asm code for equailities CMD's
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
                    writeEq("JEQ",Integer.toString(id));
                    id++;
                    break;
                case "gt":
                    writeEq("JGT",Integer.toString(id));
                    id++;
                    break;
                case "lt":
                    writeEq("JLT",Integer.toString(id));
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
        
        public void WritePushPop (String cmd, String segment, int index) throws IOException { // handle Push/Pop CMDs
            segment = segmentConverter.containsKey(segment) ? segmentConverter.get(segment) : segment; // convert segment name to asm symbol if needed
            if (cmd.equals("push")){
                switch(segment){
                    case "constant":
                        writePushConstant(Integer.toString(index));
                        break;
                    case "static":
                        writePushFromAddr(Integer.toString(index+16));
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
                        writePopToAddr(Integer.toString(index+16));
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
        InputStream in = new FileInputStream(args[0]); // init input stream from user's input (.vm file path)
        OutputStream out = new FileOutputStream(args[0].replace(".vm", ".asm")); // init output stream as file.asm (if doesn't exist creates it in the same location as input)
        CodeWriter code = new CodeWriter(out);
        Parser parser = new Parser(in);
        while(parser.hasMoreLines()){ // reads input file
            parser.advance();
            if(parser.commandType() == Parser.Cmd.C_ARITHMETIC){ // handle arithmetic
                code.writeArithmetic(parser.arg1());
            }
            else { // handle Push/Pop 
                code.WritePushPop(parser.curCmd.split(" ")[0], parser.arg1(), parser.arg2());
            }
        }
        code.close();
    }
} 