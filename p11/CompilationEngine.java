import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CompilationEngine {
    private VMWriter vmWriter;
    private JackTokenizer tokenizer;
    private SymbolTable symbolTable;
    private String curClass;
    private String curSubroutine;

    private int labelIndex;

    public CompilationEngine(InputStream input, OutputStream output) {

        tokenizer = new JackTokenizer(input);
        vmWriter = new VMWriter(output);
        symbolTable = new SymbolTable();
        labelIndex = 0;

    }


    // return current function name, className.subroutineName
    private String curFunction(){

        if (curClass.length() != 0 && curSubroutine.length() !=0){

            return curClass + "." + curSubroutine;

        }

        return "";
    }


    private String compileType() throws IOException{

        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && (tokenizer.keyWord() == JackTokenizer.keyWordConst.INT || tokenizer.keyWord() == JackTokenizer.keyWordConst.CHAR || tokenizer.keyWord() == JackTokenizer.keyWordConst.BOOLEAN)){
            return tokenizer.getCurToken();
        }

        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER){
            return tokenizer.identifier();
        }

        error("in|char|boolean|className");

        return "";
    }


    // class: 'class' className '{' classVarDec* subroutineDec* '}'
    public void compileClass() throws IOException{

        // 'class'
        tokenizer.advance();

        if (tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD || tokenizer.keyWord() != JackTokenizer.keyWordConst.CLASS){
            System.out.println(tokenizer.getCurToken());
            error("class");
        }

        // className
        tokenizer.advance();

        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
            error("className");
        }

        // The classname does not require entry in the symbol table
        curClass = tokenizer.identifier();

        // '{'
        nextIsSymbol('{');

        // classVarDec* subroutineDec*
        compileClassVarDec();
        compileSubroutine();

        // '}'
        nextIsSymbol('}');

        if (tokenizer.hasMoreTokens()){
            throw new IllegalStateException("Unexpected tokens");
        }

    
        vmWriter.close();

    }

    // classVarDec ('static'|'field') type varName (','varNAme)* ';'
    private void compileClassVarDec() throws IOException{

        
        tokenizer.advance();

        // next is a '}' 
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '}'){
            tokenizer.PrevTokenPos();
            return;
        }

        // subroutineDec or classVarDec, both start with keyword
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD){
            error("Keywords");
        }

        // next is subroutineDec
        if (tokenizer.keyWord() == JackTokenizer.keyWordConst.CONSTRUCTOR || tokenizer.keyWord() == JackTokenizer.keyWordConst.FUNCTION || tokenizer.keyWord() == JackTokenizer.keyWordConst.METHOD){
            tokenizer.PrevTokenPos();
            return;
        }

        // classVarDec exists
        if (tokenizer.keyWord() != JackTokenizer.keyWordConst.STATIC && tokenizer.keyWord() != JackTokenizer.keyWordConst.FIELD){
            error("static or field");
        }

        SymbolTable.KIND kind;
        String type = "";
        String name = "";

        switch (tokenizer.keyWord()){
            case STATIC:kind = SymbolTable.KIND.STATIC;break;
            case FIELD:kind = SymbolTable.KIND.FIELD;break;
            default:kind = null;
        }

        // type
        type = compileType();

        

        do {

            //varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
                error("identifier");
            }

            name = tokenizer.identifier();

            symbolTable.define(name,type,kind);

            // ',' or ';'
            tokenizer.advance();

            if (tokenizer.tokenType() != JackTokenizer.tokenConst.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (tokenizer.symbol() == ';'){
                break;
            }


        }while(true);

        compileClassVarDec();
    }


    
    private void compileSubroutine() throws IOException{

       
        tokenizer.advance();

        // next is a '}' - no subroutine
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '}'){
            tokenizer.PrevTokenPos();
            return;
        }

        // subroutine
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD || 
        (tokenizer.keyWord() != JackTokenizer.keyWordConst.CONSTRUCTOR && 
        tokenizer.keyWord() != JackTokenizer.keyWordConst.FUNCTION && 
        tokenizer.keyWord() != JackTokenizer.keyWordConst.METHOD)) {

                error("constructor|function|method");
        }

        JackTokenizer.keyWordConst keyword = tokenizer.keyWord();

        // reset subroutine table with the prev arguments and vars
        symbolTable.resetSubroutine();

        // add this to symbol table
        if (tokenizer.keyWord() == JackTokenizer.keyWordConst.METHOD){
            symbolTable.define("this",curClass, SymbolTable.KIND.ARG);
        }

        String type;

        // 'void' or type
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && tokenizer.keyWord() == JackTokenizer.keyWordConst.VOID){
            type = "void";
        }else {
            tokenizer.PrevTokenPos();
            type = compileType();
        }

        // subroutineName - identifier
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
            error("subroutineName");
        }

        curSubroutine = tokenizer.identifier();

        // '('
        nextIsSymbol('(');

        // parameterList
        compileParameterList();

        // ')'
        nextIsSymbol(')');

        // subroutineBody
        compileSubroutineBody(keyword);

        compileSubroutine();

    }

    // '{'  varDec* statements '}'
    private void compileSubroutineBody(JackTokenizer.keyWordConst keyword) throws IOException{
        // '{'
        nextIsSymbol('{');
        // varDec*
        compileVarDec();
        
        wrtieFunctionDec(keyword);
        // statements
        compileStatement();
        // '}'
        nextIsSymbol('}');
    }


    // Write the function declaration. When the keyword is METHOD or CONSTRUCTOR, load the pointer.
    private void wrtieFunctionDec(JackTokenizer.keyWordConst keyword){

        vmWriter.writeFunction(curFunction(),symbolTable.varCount(SymbolTable.KIND.VAR));

        //METHOD and CONSTRUCTOR - load 'this' 
        if (keyword == JackTokenizer.keyWordConst.METHOD){

            vmWriter.writePush(VMWriter.SEGMENT.ARG, 0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);

        }else if (keyword == JackTokenizer.keyWordConst.CONSTRUCTOR){

            vmWriter.writePush(VMWriter.SEGMENT.CONST,symbolTable.varCount(SymbolTable.KIND.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);
        }
    }


    // Compiles a single statement
    private void compileStatement() throws IOException{

        
        tokenizer.advance();

        // next is a '}'
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '}'){
            tokenizer.PrevTokenPos();
            return;
        }

        // next is 'let'|'if'|'while'|'do'|'return'
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD){
            error("keyword");
        }else {
            switch (tokenizer.keyWord()){
                case LET:
                    compileLet();
                    break;
                case IF:
                    compileIf();
                    break;
                case WHILE:
                    compilesWhile();
                    break;
                case DO:
                    compileDo();
                    break;
                case RETURN:
                    compileReturn();
                    break;
                default:
                    error("'let'|'if'|'while'|'do'|'return'");
            }
        }

        compileStatement();
    }

    // ((type varName)(',' type varName)*)?
    private void compileParameterList() throws IOException{

        // check if there is parameterList, if next token is ')' than go back
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == ')'){
            tokenizer.PrevTokenPos();
            return;
        }

        String type = "";

        // there is parameter, at least one varName
        tokenizer.PrevTokenPos();
        do {
            // type
            type = compileType();

            // varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
                error("identifier");
            }

            symbolTable.define(tokenizer.identifier(),type, SymbolTable.KIND.ARG);

            // ',' or ')'
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ')')){
                error("',' or ')'");
            }

            if (tokenizer.symbol() == ')'){
                tokenizer.PrevTokenPos();
                break;
            }

        }while(true);

    }


    // 'var' type varName (',' varName)*;
    private void compileVarDec() throws IOException{

       
        tokenizer.advance();
        // no 'var' go back
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.KEYWORD || tokenizer.keyWord() != JackTokenizer.keyWordConst.VAR){
            tokenizer.PrevTokenPos();
            return;
        }

        // type
        String type = compileType();


        do {

            // varName
            tokenizer.advance();

            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
                error("identifier");
            }

            symbolTable.define(tokenizer.identifier(),type, SymbolTable.KIND.VAR);

            // ',' or ';'
            tokenizer.advance();

            if (tokenizer.tokenType() != JackTokenizer.tokenConst.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ';')){
                error("',' or ';'");
            }

            if (tokenizer.symbol() == ';'){
                break;
            }


        }while(true);

        compileVarDec();

    }


    // 'do' subroutineCall ';'
    private void compileDo() throws IOException{

        // subroutineCall
        compileSubroutineCall();
        // ';'
        nextIsSymbol(';');
        // pop return value
        vmWriter.writePop(VMWriter.SEGMENT.TEMP,0);
    }


    // Compiles a let statement
    private void compileLet() throws IOException{

        // varName
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
            error("varName");
        }

        String varName = tokenizer.identifier();

        // '[' or '='
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.SYMBOL || (tokenizer.symbol() != '[' && tokenizer.symbol() != '=')){
            error("'['|'='");
        }

        boolean expressionExist = false;

        // '[' expression ']' - array [base address + offset]
        if (tokenizer.symbol() == '['){

            expressionExist = true;

            // push array variable = base address 
            vmWriter.writePush(getSegment(symbolTable.kindOf(varName)),symbolTable.indexOf(varName));

            // offset
            compileExpression();

            // ']'
            nextIsSymbol(']');

            // add the base address and the offset
            vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);
        }

        if (expressionExist) tokenizer.advance();

        // expression
        compileExpression();

        // ';'
        nextIsSymbol(';');

        if (expressionExist){
            // (base address + offset) = expression
            // pop expression value to temp
            vmWriter.writePop(VMWriter.SEGMENT.TEMP,0);
            // pop base address + offset into 'that'
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,1);
            // pop expression value into (base address + offset)
            vmWriter.writePush(VMWriter.SEGMENT.TEMP,0);
            vmWriter.writePop(VMWriter.SEGMENT.THAT,0);
        }else {
            // pop expression value directly
            vmWriter.writePop(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));

        }
    }

    // return the segment 
    private VMWriter.SEGMENT getSegment(SymbolTable.KIND kind){

        switch (kind){
            case FIELD:return VMWriter.SEGMENT.THIS;
            case STATIC:return VMWriter.SEGMENT.STATIC;
            case VAR:return VMWriter.SEGMENT.LOCAL;
            case ARG:return VMWriter.SEGMENT.ARG;
            default:return VMWriter.SEGMENT.NONE;
        }

    }


    // 'while' '(' expression ')' '{' statements '}'
    private void compilesWhile() throws IOException{

        String exitLoopLabel = newLabel();
        String loopLable = newLabel();

        // loop label for while loop
        vmWriter.writeLabel(loopLable);

        // '('
        nextIsSymbol('(');
        // expression while condition: true or false
        compileExpression();
        // ')'
        nextIsSymbol(')');
        // if ~(condition) go to exit loop label
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(exitLoopLabel);
        // '{'
        nextIsSymbol('{');
        // statements
        compileStatement();
        // '}'
        nextIsSymbol('}');
        // if (condition) go to loop label
        vmWriter.writeGoto(loopLable);
        // or continue
        vmWriter.writeLabel(exitLoopLabel);
    }

    // generic lable + index
    private String newLabel(){
        return "LABEL_" + (labelIndex++);
    }


    // ‘return’ expression? ';'
    private void compileReturn() throws IOException{

        // check if there is any expression
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == ';'){
            // no expression push 0 to stack
            vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
        }else {
            // expression exist
            tokenizer.PrevTokenPos();
            // expression
            compileExpression();
            // ';'
            nextIsSymbol(';');
        }

        vmWriter.writeReturn();

    }

   
    // 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
    private void compileIf() throws IOException{

        String elseLabel = newLabel();
        String endLabel = newLabel();

        // '('
        nextIsSymbol('(');
        // expression
        compileExpression();
        // ')'
        nextIsSymbol(')');
        // if ~(condition) go to else label
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(elseLabel);
        // '{'
        nextIsSymbol('{');
        // statements
        compileStatement();
        // '}'
        nextIsSymbol('}');
        // if condition after statement finishing, go to end label
        vmWriter.writeGoto(endLabel);

        vmWriter.writeLabel(elseLabel);
        // check if there is 'else'
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && tokenizer.keyWord() == JackTokenizer.keyWordConst.ELSE){
            // '{'
            nextIsSymbol('{');
            // statements
            compileStatement();
            // '}'
            nextIsSymbol('}');
        }else {
            tokenizer.PrevTokenPos();
        }

        vmWriter.writeLabel(endLabel);

    }

    
    // integerConstant|stringConstant|keywordConstant|varName|varName '[' expression ']'|subroutineCall|
    private void compileTerm() throws IOException{

        tokenizer.advance();
        // check if it is an identifier
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.IDENTIFIER){
            // varName|varName '[' expression ']'|subroutineCall
            String tempId = tokenizer.identifier();

            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '['){
                // this is an array entry

                // push array variable,base address into stack
                vmWriter.writePush(getSegment(symbolTable.kindOf(tempId)),symbolTable.indexOf(tempId));

                // expression
                compileExpression();
                // ']'
                nextIsSymbol(']');

                // base+offset
                vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);

                // pop into 'that' pointer
                vmWriter.writePop(VMWriter.SEGMENT.POINTER,1);
                // push *(base+index) onto stack
                vmWriter.writePush(VMWriter.SEGMENT.THAT,0);

            }else if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && (tokenizer.symbol() == '(' || tokenizer.symbol() == '.')){
                // this is a subroutineCall
                tokenizer.PrevTokenPos();tokenizer.PrevTokenPos();
                compileSubroutineCall();
            }else {
                // this is varName
                tokenizer.PrevTokenPos();
                // push variable directly onto stack
                vmWriter.writePush(getSegment(symbolTable.kindOf(tempId)), symbolTable.indexOf(tempId));
            }

        }else{
            // integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term
            if (tokenizer.tokenType() == JackTokenizer.tokenConst.INT_CONST){
                // push integerConstant value onto stack
                vmWriter.writePush(VMWriter.SEGMENT.CONST,tokenizer.intVal());
            }else if (tokenizer.tokenType() == JackTokenizer.tokenConst.STRING_CONST){
                // stringConstant new a string and append every char to the new stack
                String str = tokenizer.stringVal();

                vmWriter.writePush(VMWriter.SEGMENT.CONST,str.length());
                vmWriter.writeCall("String.new",1);

                for (int i = 0; i < str.length(); i++){
                    vmWriter.writePush(VMWriter.SEGMENT.CONST,(int)str.charAt(i));
                    vmWriter.writeCall("String.appendChar",2);
                }

            }else if(tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && tokenizer.keyWord() == JackTokenizer.keyWordConst.TRUE){
                // ~0 is true
                vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
                vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);

            }else if(tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && tokenizer.keyWord() == JackTokenizer.keyWordConst.THIS){
                // push this pointer onto stack
                vmWriter.writePush(VMWriter.SEGMENT.POINTER,0);

            }else if(tokenizer.tokenType() == JackTokenizer.tokenConst.KEYWORD && (tokenizer.keyWord() == JackTokenizer.keyWordConst.FALSE || tokenizer.keyWord() == JackTokenizer.keyWordConst.NULL)){
                // 0 for false and null
                vmWriter.writePush(VMWriter.SEGMENT.CONST,0);
            }else if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '('){
                // expression
                compileExpression();
                // ')'
                nextIsSymbol(')');
            }else if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && (tokenizer.symbol() == '-' || tokenizer.symbol() == '~')){

                char s = tokenizer.symbol();

                // term
                compileTerm();

                if (s == '-'){
                    vmWriter.writeArithmetic(VMWriter.COMMAND.NEG);
                }else {
                    vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
                }

            }else {
                error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }

    }

  
    // subroutineName '(' expressionList ')' | (className|varName) '.' subroutineName '(' expressionList ')'
    private void compileSubroutineCall() throws IOException{

        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
            error("identifier");
        }

        String name = tokenizer.identifier();
        int argsNum = 0;

        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '('){
            // push this pointer
            vmWriter.writePush(VMWriter.SEGMENT.POINTER,0);
            // '(' expressionList ')'
            // expressionList
            argsNum = compileExpressionList() + 1;
            // ')'
            nextIsSymbol(')');
            // call subroutine
            vmWriter.writeCall(curClass + '.' + name, argsNum);

        } else if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == '.'){
            // (className|varName) '.' subroutineName '(' expressionList ')'

            String objName = name;
            // subroutineName
            tokenizer.advance();
           
            if (tokenizer.tokenType() != JackTokenizer.tokenConst.IDENTIFIER){
                error("identifier");
            }

            name = tokenizer.identifier();

            // check for if it is built-in type
            String type = symbolTable.typeOf(objName);

            if (type.equals("int")||type.equals("boolean") ||
                type.equals("char")||type.equals("void")) {

                    error("no built-in type");

            }else if (type.equals("")) {

                    name = objName + "." + name;

            }else {

                argsNum = 1;
                // push variable directly to stack
                vmWriter.writePush(getSegment(symbolTable.kindOf(objName)), symbolTable.indexOf(objName));
                name = symbolTable.typeOf(objName) + "." + name;
            }

            // '('
            nextIsSymbol('(');
            // expressionList
            argsNum += compileExpressionList();
            // ')'
            nextIsSymbol(')');
            // subroutine
            vmWriter.writeCall(name,argsNum);
        }else {
            error("'('|'.'");
        }

    }

   
    private void compileExpression() throws IOException{
        // term
        compileTerm();
      
        do {
            tokenizer.advance();
            // operator
            if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.isOperator()){

                String opCmd = "";

                switch (tokenizer.symbol()){
                    case '+':
                        opCmd = "add";
                        break;
                    case '-':
                        opCmd = "sub";
                        break;
                    case '*':
                        opCmd = "call Math.multiply 2";
                        break;
                    case '/':
                        opCmd = "call Math.divide 2";
                        break;
                    case '<':
                        opCmd = "lt";
                        break;
                    case '>':   
                        opCmd = "gt";
                        break;
                    case '=':
                        opCmd = "eq";
                        break;
                    case '&':
                        opCmd = "and";
                        break;
                    case '|':
                        opCmd = "or";
                        break;
                    default:   
                        error("Unknown op!");
                }

                //term
                compileTerm();

                vmWriter.writeCommand(opCmd,"","");

            }else {
                tokenizer.PrevTokenPos();
                break;
            }

        }while (true);

    }

    
    private int compileExpressionList() throws IOException{
        int argsNum = 0;

        tokenizer.advance();
        // if next is ')' - there are no expressions
        if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == ')'){
            tokenizer.PrevTokenPos();
        }else {
            argsNum = 1;
            tokenizer.PrevTokenPos();
            // expression
            compileExpression();
            // (','expression)*
            do {
                tokenizer.advance();
                if (tokenizer.tokenType() == JackTokenizer.tokenConst.SYMBOL && tokenizer.symbol() == ','){
                    // expression
                    compileExpression();
                    argsNum++;
                }else {
                    tokenizer.PrevTokenPos();
                    break;
                }

            }while (true);
        }

        return argsNum;
    }

    
    // throw an exception with the given val , curToken
    private void error(String val){
        throw new IllegalStateException("Expected token missing : " + val + " Current token:" + tokenizer.getCurToken());
    }

    // move to the next token and check that it is the symbol
    private void nextIsSymbol(char symbol) throws IOException{
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.tokenConst.SYMBOL || tokenizer.symbol() != symbol){
            error("'" + symbol + "'");
        }
    }
    
}
