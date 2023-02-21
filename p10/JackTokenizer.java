import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

public class JackTokenizer {
    
    public enum tokenConst {
        KEYWORD, 
        SYMBOL,
        IDENTIFIER,
        INT_CONST,
        STRING_CONST,
        ERROR
    };

    public enum keyWordConst {
        CLASS, VOID, ELSE,
        METHOD, VAR, WHILE,
        FUNCTION, STATIC, RETURN,
        CONSTRUCTOR, FIELD, TRUE,
        INT, LET, FALSE,
        BOOLEAN, DO, NULL,
        CHAR, IF, THIS
    };

    public String symbols = "{}()[].,;+-*/&|<>=-~";
    private Reader reader;
    private BufferedReader buffReader;
    private String curToken = "";
    private String[] tokens = {}; // tokens in cur line
    private int tokenPos = 0; // current token position in the tokens array

    public JackTokenizer(InputStream input) {
        this.reader = new InputStreamReader(input);
        this.buffReader = new BufferedReader(reader);
    }

    public String getCurToken(){
        return this.curToken;
    }


    public boolean hasMoreTokens() throws IOException {
        if (tokenPos >= tokens.length){
            buffReader.mark(100);
            String line = buffReader.readLine();
            buffReader.reset();
            if (line != null){
                return true;
            }
            return false;

        }else{
            return true;
        }
    }

    public boolean hasMoreLines() throws IOException { // check if there is more lines to read in the input stream
        buffReader.mark(100);
        if (buffReader.readLine() != null) {
            buffReader.reset();
            return true;
        }
        buffReader.reset();
        return false;
    }

    public void advance() throws IOException {
        if (hasMoreTokens()){
            if (tokenPos >= tokens.length){
                tokenPos = 0;
                if(hasMoreLines()){
                    String curLine = parseLine(buffReader.readLine().trim().replaceAll("\\/{2}.*|\\/\\*.*|^\\*.*", "")); // remove comments and leading,trialing spaces
                    while((hasMoreLines()) && (curLine.length() == 0)){ // ignores empty lines or just comments
                        curLine = parseLine(buffReader.readLine().trim().replaceAll("\\/{2}.*|\\/\\*.*|^\\*.*", "")); // remove comments and leading,trialing spaces
                    }
                    tokens = curLine.trim().replaceAll(" +", " ").split(" "); 
                    curToken = tokens[tokenPos];
                    // if multiple words String const
                    if (curToken.contains("\"")){
                        while(!curToken.matches("^\"[^\"]+\"$")){
                            
                            tokenPos++;
                            curToken += " " + tokens[tokenPos];
                            if (tokenPos >= tokens.length){
                                break;
                            }
                        }
                    }
                    tokenPos++;
                }
            }else{
                curToken = tokens[tokenPos];
                // if multiple words String const
                if (curToken.contains("\"")){
                    while(!curToken.matches("^\"[^\"]+\"$")){
                        
                        tokenPos++;
                        curToken += " " + tokens[tokenPos];
                        if (tokenPos >= tokens.length){
                            break;
                        }
                    }
                }
                    
                tokenPos++;
            }
        }
        
    }

    public String parseLine(String line){ // add spaces to symbols
        if (line.length() > 0){
            for(char symbol : symbols.toCharArray()){
                line = line.replaceAll("(\\"+symbol+")", " $0 ");
            }
        }
        return line;
    }
    public tokenConst tokenType() {

        if (keyWord() != null){
            return tokenConst.KEYWORD;
        }
        if (symbols.contains(curToken)){
            return tokenConst.SYMBOL;
        }
        if (curToken.matches("^\"[^\"]+\"$")) {
            return tokenConst.STRING_CONST;
        }
        if (curToken.matches("^[^\\d\\W]\\w*\\Z")) {
            return tokenConst.IDENTIFIER;
        }
        try {
            if ((Integer.parseInt(curToken) >= 0) && (Integer.parseInt(curToken) <= 32767)) {
                return tokenConst.INT_CONST;
            }
        }
        catch (NumberFormatException e) { 
            return tokenConst.ERROR;
        }
        
        return tokenConst.ERROR;
        
    }
    // retrun null if not a KEYWORD
    public keyWordConst keyWord() {
        keyWordConst k;
            switch(curToken){
                case "class":
                    k = keyWordConst.CLASS;
                    break;
                case "constructor":
                    k = keyWordConst.CONSTRUCTOR;
                    break;
                case "method":
                    k = keyWordConst.METHOD;
                    break;
                case "function":
                    k = keyWordConst.FUNCTION;
                    break;
                case "int":
                    k = keyWordConst.INT;
                    break;
                case "boolean":
                    k = keyWordConst.BOOLEAN;
                    break;
                case "char":
                    k = keyWordConst.CHAR;
                    break;
                case "void":
                    k = keyWordConst.VOID;
                    break;
                case "var":
                    k = keyWordConst.VAR;
                    break;
                case "static":
                    k = keyWordConst.STATIC;
                    break;
                case "field":
                    k = keyWordConst.FIELD;
                    break;
                case "let":
                    k = keyWordConst.LET;
                    break;
                case "do":
                    k = keyWordConst.DO;
                    break;
                case "if":
                    k = keyWordConst.IF;
                    break;
                case "else":
                    k = keyWordConst.ELSE;
                    break;
                case "while":
                    k = keyWordConst.WHILE;
                    break;
                case "return":
                    k = keyWordConst.RETURN;
                    break;
                case "true":
                    k = keyWordConst.TRUE;
                    break;
                case "false":
                    k = keyWordConst.FALSE;
                    break;
                case "null":
                    k = keyWordConst.NULL;
                    break;
                case "this":
                    k = keyWordConst.THIS;
                    break;
                default:
                    k = null;
            }
        return k;
    }

    // return 0 if not a symbol
    public char symbol() {
        if(tokenType() == tokenConst.SYMBOL){
            return curToken.charAt(0);
        }
        return 0;
    }

    // return null if not identifier
    public String identifier() {
        if(tokenType() == tokenConst.IDENTIFIER){
            return curToken;
        }
        return null;
    }
  
    public int intVal() {
        return Integer.parseInt(curToken);
    }

    public String stringVal() {
        return curToken.replace("\"", "");
    }


}
