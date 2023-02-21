import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class HackAssembler {
    public enum Cmd {
        A_COMMAND,
        C_COMMAND,
        L_COMMAND,
    }
    static class Parser {

        private Reader reader;
        private BufferedReader buffReader;
        private String curCmd = ""; // the current CMD that read
        private int line = -1;

        private static enum jmp{
            JGT,
            JEQ,
            JGE,
            JLT,
            JNE,
            JLE,
            JMP
        }
        public static Map<String, Integer> symbols = new HashMap<String, Integer>(){{
            put("SP" , 0);
            put("LCL" , 1);
            put("ARG" , 2);
            put("THIS" , 3);
            put("THAT" , 4);
            put("R0" , 0);
            put("R1" , 1);
            put("R2" , 2);
            put("R3" , 3);
            put("R4" , 4);
            put("R5" , 5);
            put("R6" , 6);
            put("R7" , 7);
            put("R8" , 8);
            put("R9" , 9);
            put("R10" ,10);
            put("R11" , 11);
            put("R12" , 12);
            put("R13" , 13);
            put("R14" , 14);
            put("R15" , 15);
            put("SCREEN" , 16384);
            put("KBD" , 24576);
        }};

        public Parser(InputStream in) { // constructor - init readers
            reader = new InputStreamReader(in);
            buffReader = new BufferedReader(reader);
        }

        public Boolean hasMoreCommands() throws IOException { 
            buffReader.mark(100); // max length of line is 100 (asumption)
            if (buffReader.readLine() != null) {
                buffReader.reset();
                return true;
            }
            buffReader.reset();
            return false;
        }
    
        public void advance() throws IOException {
            if (hasMoreCommands()) {
                String commandLine = buffReader.readLine().replaceAll("//.*", ""); // delete comments
                
                while(hasMoreCommands() && commandLine.length() == 0){ // until EOF or line is empty
                    
                    commandLine = buffReader.readLine().replaceAll("//.*", ""); // delete comments
                }
                          
                curCmd = commandLine.replaceAll(" ", "");   
                if(commandType() != Cmd.L_COMMAND){ // ignore L CMD and count the actual code lines
                    line++;
                }
                
                


            }

        }
        
        public int getLine(){
            return line;
        }

        public Cmd commandType()  {
            if (curCmd.charAt(0) == '@') {
                return Cmd.A_COMMAND;
            }
            if (curCmd.matches("([AMD]{1,3}=)?([-!]?[01AMD]([-+&|][01AMD])?([-+][AMD01])?)(;["+jmp.JGT+"|"+
                                                                    jmp.JEQ+"|"+
                                                                    jmp.JGE+"|"+
                                                                    jmp.JLT+"|"+
                                                                    jmp.JNE+"|"+
                                                                    jmp.JLE+"|"+
                                                                    jmp.JMP+"|"+"]{3})?")){
                                                                      
                return Cmd.C_COMMAND;
            }
            if (curCmd.matches("\\(.+\\)")) {
                return Cmd.L_COMMAND;
            } 
            // not valid command
            return null;
        } 
    
        public String symbol() {
            if (commandType() == Cmd.A_COMMAND) {
                return curCmd.substring(1); // erase @
            }
            return curCmd.replaceAll("\\(|\\)", ""); // erase ()
        }
    
        public String dest() {
            return curCmd.contains("=") ? curCmd.split("=")[0] : "null" ;
        }
    
        public String comp() {
            return curCmd.contains("=") ? curCmd.split("=")[1].split(";")[0] : curCmd.split(";")[0];
        }  
        
        public String jump() { 
            return curCmd.contains(";") ? curCmd.split(";")[1] : "null";
        }
    } 

    static class Code{
        
        private static final Map<String, Integer> destSymbols = new HashMap<String, Integer>(){{
            put("null" , 0);
            put("M" , 1);
            put("D" , 2);
            put("MD" , 3);
            put("A" , 4);
            put("AM" , 5);
            put("AD" , 6);
            put("AMD" , 7);
        }};
        
        private static final Map<String, Integer> compSymbols = new HashMap<String, Integer>(){{
            put("0" , 42);
            put("1" , 63);
            put("-1" , 58);
            put("D" , 12);
            put("A" , 48);
            put("M" , 48);
            put("!D" , 13);
            put("!A" , 49);
            put("!M" , 49);
            put("-D" , 15);
            put("-A" , 51);
            put("-M" , 51);
            put("D+1" , 31);
            put("A+1" , 55);
            put("M+1" , 55);
            put("D-1" , 14);
            put("A-1" , 50);
            put("M-1" , 50);
            put("D+A" , 2);
            put("D+M" , 2);
            put("D-A" , 19);
            put("D-M" , 19);
            put("A-D" , 7);
            put("M-D" , 7);
            put("D&A" , 0);
            put("D&M" , 0);
            put("D|A" , 21);
            put("D|M" , 21);
        }};

        private static final Map<String, Integer> jumpSymbols = new HashMap<String, Integer>(){{
            put("null" , 0);
            put("JGT" , 1);
            put("JEQ" , 2);
            put("JGE" , 3);
            put("JLT" , 4);
            put("JNE" , 5);
            put("JLE" , 6);
            put("JMP" , 7);
        }};

        public static int dest (String dest) {
            return destSymbols.get(dest);
        }

        public static int comp (String comp) {
            return compSymbols.get(comp);  
        }

        public static int compA (String comp){
            return comp.contains("M") ? 1 : 0;
        }

        public static int jump (String jump) {
            return jumpSymbols.get(jump);
        }
    }

    
    public static String cToBinary (int compA, int comp, int dest, int jump) { // convert C CMD to binary
        int binarycmd = 0;
        binarycmd = ((14 + compA) << 12) + (comp << 6) + (dest << 3) + jump;
        
        return String.format("%16s", Integer.toBinaryString(binarycmd)).replace(" ", "0"); // padding string with leading zeros
    }

    public static String aToBinary(int cmd) {
        return String.format("%16s",Integer.toBinaryString(cmd)).replace(" ", "0"); // padding string with leading zeros
    }
    public static boolean isNewSymbol(String symbol){ // checks if need to add symbol to symbol Map
        return ! (Parser.symbols.containsKey(symbol) || symbol.matches("\\d+"));
    }
    public static void handleNewSymbol(String symbol, int address){ // adds new symbol to symbols Map
        
        Parser.symbols.put(symbol, address);
    }
    public static void updateLCmd(Stack<String> prevLCmd, int line){ // update addresses of all lables before cur CMD
        while(!prevLCmd.empty()){
            handleNewSymbol(prevLCmd.pop(), line);
        }
    }
    public static void main(String[] args) {
        String fileName = args[0]; // user's input - .asm file path
        try{
            // first run init
            InputStream in = new FileInputStream(fileName);
            Parser parser = new Parser(in);
            Stack<String> prevLCmd = new Stack<String>();
            while(parser.hasMoreCommands()) {
                parser.advance();
                if (parser.commandType() == Cmd.L_COMMAND) { // add to Stack
                    prevLCmd.push(parser.symbol());
                }
                else { // for all prev L CMDs - put in the symmbol Map the line number as address
                    updateLCmd(prevLCmd, parser.getLine());

                }
                
            }
            //we reached EOF
            Writer writer = new FileWriter(fileName.split("\\.")[0] + ".hack");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            in = new FileInputStream(fileName); // init in and parser
            parser = new Parser(in);
            String binaryCmd = "";
            // second run
            int freeAddress = 16;
            while (parser.hasMoreCommands()) {
               parser.advance();
               if (parser.commandType() == Cmd.L_COMMAND){ // dont write L CMD
                    continue;
               }
               if (parser.commandType() == Cmd.C_COMMAND){
                   
                   binaryCmd = cToBinary(Code.compA(parser.comp()), Code.comp(parser.comp()), Code.dest(parser.dest()), Code.jump(parser.jump()));
               }
               //A command
               if (parser.commandType() == Cmd.A_COMMAND) {
                // if a new symbol, put the next free address in the symbol Map
                    if(isNewSymbol(parser.symbol())) {
                        handleNewSymbol(parser.symbol(), freeAddress);
                        freeAddress++;
                    }
                    // conveting String symbols to binary CMDs
                    if (Parser.symbols.containsKey(parser.symbol())){ //@symbol
                        binaryCmd = aToBinary(Parser.symbols.get(parser.symbol()));
                        
                    }
                    else{ // @num
                        binaryCmd = aToBinary(Integer.parseInt(parser.symbol()));
                    }
                
                   
                   
               }
                // writes the CMD and new line
                buffWriter.write(binaryCmd);
                buffWriter.newLine();
           }
        
           buffWriter.close();
           
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}