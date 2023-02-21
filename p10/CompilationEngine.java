import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;


public class CompilationEngine {
    private Writer writer;
    private BufferedWriter buffWriter;
    private JackTokenizer tokenizer;
    private String indention = "";

    public CompilationEngine(InputStream input, OutputStream output){
        this.writer = new OutputStreamWriter(output);
        this.buffWriter = new BufferedWriter(writer);
        this.tokenizer = new JackTokenizer(input);
    }

    // make more indention
    private void indentionInc() {
        indention += "  ";
    }

    // make less indention
    private void indentionDec() {
        indention = indention.substring(2);
    }

    public void compileClass() throws IOException{

        tokenizer.advance();
        if (tokenizer.keyWord() == JackTokenizer.keyWordConst.CLASS) {
            // ========= class =========
            buffWriter.write(indention + "<class>\n");
            indentionInc();
            buffWriter.write(indention + "<keyword> class </keyword>\n");

            // ========= class name =========
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
                System.out.println("ERROR: illegal class name identifier");
                return;
            }
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

            // // ========= { =========
            tokenizer.advance();
            if (!tokenizer.getCurToken().equals("{")) {
                System.out.println("ERROR: no openning { for class");
                return;
            }
            buffWriter.write(indention + "<symbol> { </symbol>\n");

            // potential classVarDec
            tokenizer.advance();
            while ( tokenizer.keyWord() == JackTokenizer.keyWordConst.STATIC ||
                    tokenizer.keyWord() == JackTokenizer.keyWordConst.FIELD) {
                
                // ************ compileClassVarDec ************
                compileClassVarDec();
                tokenizer.advance();
            }

            // potential subroutineDec
            while ( tokenizer.keyWord() == JackTokenizer.keyWordConst.CONSTRUCTOR ||
                    tokenizer.keyWord() == JackTokenizer.keyWordConst.FUNCTION ||
                    tokenizer.keyWord() == JackTokenizer.keyWordConst.METHOD) {
                
                 // ************ compileSubroutine ************
                compileSubroutine();
                tokenizer.advance();
            }

            // // ========= } =========
            if (!tokenizer.getCurToken().equals("}")) {
                
                System.out.println("ERROR: " + tokenizer.getCurToken() + " " + tokenizer.tokenType() + " " + tokenizer.keyWord() + " has not closing } for class");
                return;
            }
            buffWriter.write(indention + "<symbol> } </symbol>\n");
            
            indentionDec();
            // // ========= /class =========
            buffWriter.write(indention + "</class>\n");
            buffWriter.close();
        } else {
            System.out.println("ERROR: does not start with class");
            return;
        }
    }

    public void compileClassVarDec() throws IOException{
        // ========= classVarDec =========
        buffWriter.write(indention + "<classVarDec>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> " + tokenizer.getCurToken() + " </keyword>\n");

        // ========= type =========
        tokenizer.advance();
        if (tokenizer.keyWord() != JackTokenizer.keyWordConst.INT &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.CHAR &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.BOOLEAN &&
            tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
            System.out.println("ERROR: illegal type for class var dec");
            return;
        }
        // ========= identifier =========
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER){
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
        }else{ // ========= int | char | boolean =========
            buffWriter.write(indention + "<keyword> " + tokenizer.getCurToken() + " </keyword>\n");

        }

        // ========= varName =========
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
            System.out.println("ERROR: illegal classVar identifier");
            return; 
        }
        buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

        // potential ", varName" 
        tokenizer.advance();
        while (tokenizer.symbol() == ',') {
            buffWriter.write("<symbol> , </symbol>\n");
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER) {
                buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
            } else {
                System.out.println("ERROR: illegal classVar identifier");
                return;
            }
            tokenizer.advance();
        }

        // ========= ; =========
        if (tokenizer.symbol() == ';') {
            buffWriter.write(indention + "<symbol> ; </symbol>\n");
        } else {
            System.out.println("ERROR: no ending ;");
            return;
        }
        indentionDec();
        // ========= /classVarDec =========
        buffWriter.write(indention + "</classVarDec>\n");
    }



    public void compileSubroutine() throws IOException{
        // ========= subroutineDec =========
        buffWriter.write(indention + "<subroutineDec>\n");
        indentionInc();
        // ========= constructor | function | method =========
        buffWriter.write(indention + "<keyword> " + tokenizer.getCurToken() + " </keyword>\n");  

        // return type (void|type)
        tokenizer.advance();
        if ((tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD ||
            (!tokenizer.getCurToken().equals("void"))) &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.INT &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.CHAR &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.BOOLEAN &&
            tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
                System.out.println("ERROR: Illegal type name for subroutine");
                return;
        } 
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER){
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
        }else{
            buffWriter.write(indention + "<keyword> " + tokenizer.getCurToken() + " </keyword>\n");

        } 

        // subroutine identifier
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
            System.out.println("ERROR: illegal subroutine name");
            return;
        }
        buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

        // parameter list
        tokenizer.advance();
        if (tokenizer.symbol() != '(') {
            System.out.println("ERROR: no () after function name");
            return;
            
        } 
        buffWriter.write(indention + "<symbol> ( </symbol>\n");
        // ************ compileParameterList ************
        compileParameterList();

        // ========= ) for parameter list =========
        if (tokenizer.symbol() != ')') {
            System.out.println("ERROR: no () after function name");
            return;
        } 
        buffWriter.write(indention + "<symbol> ) </symbol>\n");

        // ========= { =========
        tokenizer.advance();
        if (tokenizer.symbol() != '{') {
            System.out.println("ERROR: no { after function parameters");
            return;
        } 
        // ************ compileSubroutineBody ************
        compileSubroutineBody();

        // the closing } is in compileSubroutineBody()

        indentionDec();
        // ========= /subroutineDec =========
        buffWriter.write(indention + "</subroutineDec>\n");
    }

    public void compileParameterList() throws IOException{
        // ========= parameterList =========
        buffWriter.write(indention + "<parameterList>\n");
        indentionInc();
        tokenizer.advance();
        // until reach the end  )
        while (!(tokenizer.tokenType().equals(JackTokenizer.tokenConst.SYMBOL) && (tokenizer.symbol() == ')'))) {
            if (tokenizer.tokenType().equals(JackTokenizer.tokenConst.IDENTIFIER)) {
                buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
                tokenizer.advance();
            } else if (tokenizer.tokenType().equals(JackTokenizer.tokenConst.KEYWORD)) {
                buffWriter.write(indention + "<keyword> " + tokenizer.keyWord() + " </keyword>\n");
                tokenizer.advance();
            }
            // , separate the list - multiple
            else if ((tokenizer.tokenType().equals(JackTokenizer.tokenConst.SYMBOL)) && (tokenizer.symbol() == ',')) {
                buffWriter.write(indention + "<symbol> , </symbol>\n");
                tokenizer.advance();
            }
        }
        indentionDec();
        // ========= /parameterList =========
        buffWriter.write(indention + "</parameterList>\n");
    }

    public void compileSubroutineBody() throws IOException{
        // ========= subroutineBody =========
        buffWriter.write(indention + "<subroutineBody>\n");
        indentionInc();
        buffWriter.write(indention + "<symbol> { </symbol>\n");

        tokenizer.advance();
        // all declarations of vars
        while ( tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD &&
                tokenizer.getCurToken().equals("var")) {
            compileVarDec();
            tokenizer.advance();
        }

        // ************ compileStatements ************
        compileStatements();

        // ========= } =========
        if (tokenizer.symbol() != '}') {
            System.out.println("ERROR: no } found to close subroutine call");
            return;
        }
        buffWriter.write(indention + "<symbol> } </symbol>\n");
        indentionDec();
        buffWriter.write(indention + "</subroutineBody>\n");
    }

    public void compileVarDec() throws IOException{
        // ========= varDec =========
        buffWriter.write(indention + "<varDec>\n");
        indentionInc();
        // ========= var =========
        buffWriter.write(indention + "<keyword> var </keyword>\n"); 

        // ========= type =========
        tokenizer.advance();
        if (tokenizer.keyWord() != JackTokenizer.keyWordConst.INT &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.CHAR &&
            tokenizer.keyWord() != JackTokenizer.keyWordConst.BOOLEAN &&
            tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
            System.out.println("ERROR: illegal type for var");
            return;
        }
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER){
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
        }else{
            buffWriter.write(indention + "<keyword> " + tokenizer.getCurToken() + " </keyword>\n");

        }

        // ========= var name =========
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
            System.out.println("ERROR: illegal identifier for var");
            return;
        } 
        buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

        tokenizer.advance();
        while (tokenizer.symbol() == ',') {
            buffWriter.write(indention + "<symbol> , </symbol>\n");
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
                System.out.println("ERROR: illegal identifier for var");
                return;
            }
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

            tokenizer.advance();
        }

        if (tokenizer.symbol() != ';') {
            System.out.println("ERROR: varDec doesn't end with ;");
            return;
        } 
        buffWriter.write(indention + "<symbol> ; </symbol>\n");

        indentionDec();
        // ========= /varDec =========
        buffWriter.write(indention + "</varDec>\n");

    }

    public void compileStatements() throws IOException{
        buffWriter.write(indention + "<statements>\n");
        indentionInc();
        
        while (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD) {
            JackTokenizer.keyWordConst keyword_type = tokenizer.keyWord();
            // compileIf look ahead to check "else",
            // so no advance
            switch(keyword_type) {
                case LET:    compileLet(); tokenizer.advance(); break;
                case IF:     compileIf(); break;
                case WHILE:  compileWhile(); tokenizer.advance(); break;
                case DO:     compileDo(); tokenizer.advance(); break;
                case RETURN: compileReturn(); tokenizer.advance(); break;
                default: System.out.println("ERROR: illegal statement"); return;
            }
        }
        indentionDec();
        buffWriter.write(indention + "</statements>\n");
    }

    public void compileLet() throws IOException{
        // ========= letStatement =========
        buffWriter.write(indention + "<letStatement>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> let </keyword>\n");

        tokenizer.advance();
        // ========= name =========
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER) {
            System.out.println("ERROR: Illegal identifier");
            return;
        }
        buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

        tokenizer.advance();
        // if let x[]
        if (tokenizer.symbol() == '[') {
            buffWriter.write(indention + "<symbol> [ </symbol>\n");
            
            // ************ compileExpression ************
            tokenizer.advance();
            compileExpression();

            if(tokenizer.symbol() != ']') {
                System.out.println("ERROR: No closing ], current: " + tokenizer.getCurToken());
                return;
            }
            buffWriter.write(indention + "<symbol> ] </symbol>\n");

            tokenizer.advance();
        }

        if (tokenizer.symbol() != '=') {
            System.out.println("ERROR: No = found");
            return;
        }

        // ========= = =========
        buffWriter.write(indention + "<symbol> = </symbol>\n");

         // ************ compileExpression ************
        tokenizer.advance();
        compileExpression();

        // compileExpression does one token look ahead, so no advance 
        if (tokenizer.symbol() != ';') {
            System.out.println("ERROR: No ; found at the end of statement");
            return;
        }
        // ========= ; =========
        buffWriter.write(indention + "<symbol> ; </symbol>\n");
        indentionDec();
        // ========= /letStatement =========
        buffWriter.write(indention + "</letStatement>\n");
    }
    
    public void compileIf() throws IOException{
        // ========= ifStatement =========
        buffWriter.write(indention + "<ifStatement>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> if </keyword>\n");

        tokenizer.advance();
        if (tokenizer.symbol() != '(') {
            System.out.println("ERROR: No openning ( for if statement");
            return;
        }
        buffWriter.write(indention + "<symbol> ( </symbol>\n");

        // ************ compileExpression ************
        tokenizer.advance();
        compileExpression();

        // compileExpression does one token look ahead, so no advance
        if (tokenizer.symbol() != ')') {
            System.out.println("ERROR: No closing ) for if statement");
            return;
        }
        buffWriter.write(indention + "<symbol> ) </symbol>\n");

        tokenizer.advance();
        if (tokenizer.symbol() != '{') {
            System.out.println("ERROR: No { for if statement");
            return;
        }
        buffWriter.write(indention + "<symbol> { </symbol>\n");

        // ************ compileStatements ************
        tokenizer.advance();
        compileStatements();

        if (tokenizer.symbol() != '}') {
            System.out.println("ERROR: No } for if statement");
            System.out.println("the current symbol is " + tokenizer.getCurToken());
            return;
        }
        buffWriter.write(indention + "<symbol> } </symbol>\n");

        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD &&
            tokenizer.getCurToken().equals("else")) {

            buffWriter.write(indention + "<keyword> else </keyword>\n");
            tokenizer.advance();
            if (tokenizer.symbol() != '{') {
                System.out.println("ERROR: No { for if statement");
                return;
            }
            buffWriter.write(indention + "<symbol> { </symbol>\n");

            // ************ compileStatements ************
            tokenizer.advance();
            compileStatements();

            // compileExpression does one token look ahead, so no advance
            if (tokenizer.symbol() != '}') {
                System.out.println("ERROR: No } for if statement");
                return;
            }
            buffWriter.write(indention + "<symbol> } </symbol>\n");
            tokenizer.advance();
        }

        indentionDec();
        // ========= /ifStatement =========
        buffWriter.write(indention + "</ifStatement>\n");
    }

    public void compileWhile() throws IOException{
        // ========= whileStatement =========
        buffWriter.write(indention + "<whileStatement>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> while </keyword>\n");

        tokenizer.advance();
        if (tokenizer.symbol() != '(') {
            System.out.println("ERROR: No ( in while statement");
            return;
        }
        buffWriter.write(indention + "<symbol> ( </symbol>\n");

        tokenizer.advance();
        compileExpression();

        // compileExpression does one token look ahead, so no advance
        if (tokenizer.symbol() != ')') {
            System.out.println("ERROR: No ) in while statement");
            return;
        }
        buffWriter.write(indention + "<symbol> ) </symbol>\n");

        tokenizer.advance();
        if (tokenizer.symbol() != '{') {
            System.out.println("ERROR: No { in while statement");
            return;
        }
        buffWriter.write(indention + "<symbol> { </symbol>\n");

        // ************ compileStatements ************
        tokenizer.advance();
        compileStatements();

        
        if (tokenizer.symbol() != '}') {
            System.out.println("ERROR: No } in while statement");
            return;
        }
        buffWriter.write(indention + "<symbol> } </symbol>\n");

        indentionDec();
        // ========= /whileStatement =========
        buffWriter.write(indention + "</whileStatement>\n");
    }


    public void compileDo() throws IOException{
        buffWriter.write(indention + "<doStatement>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> do </keyword>\n");

        tokenizer.advance();

        // if the current token is valid identifier.and the next is . or (
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER) {
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");

            tokenizer.advance();
            if (tokenizer.symbol() == '.' || tokenizer.symbol() == '(') {
                buffWriter.write(indention + "<symbol> " + tokenizer.symbol() +" </symbol>\n");
                // ************ compileSubRoutineCall ************
                compileSubRoutineCall();
            } else {
                System.out.println("ERROR: Not valid subroutine call");
                return;
            }
        } else {
            System.out.println("ERROR: Not a valid identifier for do statement");
            return;
        }

        tokenizer.advance();
        if (tokenizer.symbol() != ';') {
            System.out.println("ERROR: No closing ;");
            return;
        }
        buffWriter.write(indention + "<symbol> " + tokenizer.symbol() +" </symbol>\n");
        indentionDec();
        buffWriter.write(indention + "</doStatement>\n");
    }

    public void compileReturn() throws IOException{
        buffWriter.write(indention + "<returnStatement>\n");
        indentionInc();
        buffWriter.write(indention + "<keyword> return </keyword>\n");

        tokenizer.advance();

        // if the following is not ; try to parse argument
        if (tokenizer.symbol() != ';') {
            // ************ compileExpression ************
            compileExpression();

            
            if (tokenizer.symbol() != ';') {
                System.out.println("ERROR: return statement not ending with ;");
                return;
            }
            buffWriter.write(indention + "<symbol> ; </symbol>\n");

        }else{
            buffWriter.write(indention + "<symbol> ; </symbol>\n");
        }
        
        

        indentionDec();
        buffWriter.write(indention + "</returnStatement>\n");
    }
    public void compileExpression() throws IOException{
        buffWriter.write(indention + "<expression>\n");
        indentionInc();
        // ************ compileTerm ************
        compileTerm();

        // compileTerm do one token look ahead, so no advance 
        while (tokenizer.symbol() == '+' || tokenizer.symbol() == '-' || tokenizer.symbol() == '*' || tokenizer.symbol() == '/' ||
               tokenizer.symbol() == '&' || tokenizer.symbol() == '|' || tokenizer.symbol() == '<' || tokenizer.symbol() == '>' ||
               tokenizer.symbol() == '=') {

                String symbol = tokenizer.symbol() + "";
                if (tokenizer.symbol() == '&'){symbol = "&amp;"; }
                if (tokenizer.symbol() == '<'){symbol = "&lt;"; }
                if (tokenizer.symbol() == '>'){symbol = "&gt;"; }
                buffWriter.write(indention + "<symbol> " + symbol +" </symbol>\n");

                tokenizer.advance();
                // ************ compileTerm ************
                compileTerm();
                // compileTerm do one token look ahead, so no advance 
        }

        indentionDec();
        buffWriter.write(indention + "</expression>\n");
    }

    public void compileTerm() throws IOException{
        buffWriter.write(indention + "<term>\n");
        indentionInc();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.INT_CONST) {
            buffWriter.write(indention + "<integerConstant> " + tokenizer.intVal() +" </integerConstant>\n");
            tokenizer.advance();
        } else if (tokenizer.tokenType() == JackTokenizer.tokenConst.STRING_CONST) {
            buffWriter.write(indention + "<stringConstant> " + tokenizer.stringVal() + " </stringConstant>\n");
            tokenizer.advance();
        } else if (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && 
                   (tokenizer.getCurToken().equals("true") || 
                   tokenizer.getCurToken().equals("false") || 
                   tokenizer.getCurToken().equals("null") ||
                   tokenizer.getCurToken().equals("this"))) {

                    buffWriter.write(indention + "<keyword> "+ tokenizer.getCurToken() + " </keyword>\n");
                    tokenizer.advance();
        } else if (tokenizer.symbol() == '-' || tokenizer.symbol() == '~') {
            buffWriter.write(indention + "<symbol> " + tokenizer.symbol() +" </symbol>\n");
            tokenizer.advance();
            // ************ compileTerm ************
            compileTerm();
        } else if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER) {
            buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
            tokenizer.advance();
            if (tokenizer.symbol() == '[') {
                buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
                // ************ compileArrayTerm ************
                compileArrayTerm();
                tokenizer.advance();
            } else if (tokenizer.symbol() == '(' || tokenizer.symbol() == '.') {
                buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
                // ************ compileSubRoutineCall ************
                compileSubRoutineCall();
                tokenizer.advance();
            }
            // if not [, (, or . => identifier
        } else if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL) {
            if (tokenizer.symbol() == '(') {
                buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
                tokenizer.advance();
                // ************ compileExpression ************
                compileExpression();
                if (tokenizer.symbol() == ')') {
                    buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
                    tokenizer.advance();
                } else {
                    System.out.println("ERROR: no closing bracket for term");
                }
            }

        } else {
            System.out.println("ERRPR: illegal varName: " + tokenizer.getCurToken());
            return;
        }
        indentionDec();
        buffWriter.write(indention + "</term>\n");
    }

    public void compileArrayTerm() throws IOException {
        tokenizer.advance();
        // ************ compileExpression ************
        compileExpression();

        if (tokenizer.symbol() != ']') {
            System.out.println("ERROR: No closing ] for the array expression");
        }
        buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
    }

    public void compileSubRoutineCall() throws IOException {
        if (tokenizer.symbol() == '(') {
            tokenizer.advance();
            // ************ compileExpressionList ************
            compileExpressionList();

            if (tokenizer.symbol() != ')') {
                System.out.println("ERROR: No closing ) for the expressionlist");
                return;
            }
            buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
        } else {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER) {
                buffWriter.write(indention + "<identifier> " + tokenizer.identifier() + " </identifier>\n");
            } else {
                System.out.println("ERROR: illegal identifier for subroutine call");
                return;
            }

            tokenizer.advance();
            if (tokenizer.symbol() != '(') {
                System.out.println("ERROR: Expecting a open bracket in subroutine call");
                return;
            }
            buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");

            tokenizer.advance();
            // ************ compileExpressionList ************
            compileExpressionList();

            if (tokenizer.symbol() != ')') {
                System.out.println("ERROR: No closing ) for the expressionlist");
                return;
            }
            buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
        }
    }


    public void compileExpressionList() throws IOException{
        buffWriter.write(indention + "<expressionList>\n");
        indentionInc();

        if (tokenizer.symbol() != ')') {
            // ************ compileExpression ************
            compileExpression();

            // compileExpression did one look ahead, no advance
            while (tokenizer.symbol() == ',') {
                buffWriter.write(indention + "<symbol> " + tokenizer.symbol() + " </symbol>\n");
                tokenizer.advance();
                // ************ compileExpression ************
                compileExpression();
            }
        }

        indentionDec();
        buffWriter.write(indention + "</expressionList>\n");


    }
}
