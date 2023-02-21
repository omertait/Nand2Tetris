import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> classTable;
    private Map<String, Symbol> subroutineTable;
    private HashMap<KIND,Integer> indices; // keep track on the index of each kind
    public enum KIND {STATIC, FIELD, ARG, VAR, NONE};
    

    public SymbolTable() {
        classTable = new HashMap<>();
        subroutineTable = new HashMap<>();
        indices = new HashMap<KIND, Integer>();
        indices.put(KIND.ARG,0);
        indices.put(KIND.FIELD,0);
        indices.put(KIND.STATIC,0);
        indices.put(KIND.VAR,0);
    }

    // resets the subroutint table
    public void resetSubroutine() {
        subroutineTable.clear();
        indices.put(KIND.VAR,0);
        indices.put(KIND.ARG,0);
    }

    public void define(String name, String type, SymbolTable.KIND kind) {
        // class Table
        if (kind == KIND.STATIC || kind == KIND.FIELD){
            int index = indices.get(kind);
            Symbol symbol = new Symbol(type,kind,index);
            indices.put(kind,index+1);
            classTable.put(name,symbol);

        // subroutine Table
        }else if(kind == KIND.ARG || kind == KIND.VAR){
            int index = indices.get(kind);
            Symbol symbol = new Symbol(type,kind,index);
            indices.put(kind,index+1);
            subroutineTable.put(name,symbol);

        }
    }

    public int varCount(SymbolTable.KIND kind) {
        int count = 0;
        if (kind == SymbolTable.KIND.STATIC || kind == SymbolTable.KIND.FIELD) {
            for (Map.Entry<String, Symbol> entry : classTable.entrySet()) {
                if (entry.getValue().kind.equals(kind)) {
                    count++;
                }
            }
        } else {
            for (Map.Entry<String, Symbol> entry : subroutineTable.entrySet()) {
                if (entry.getValue().kind.equals(kind)) {
                    count++;
                }
            }
        }
        return count;
    }

    public KIND kindOf(String name){

        Symbol symbol = find(name);

        if (symbol != null) return symbol.getKind();

        return KIND.NONE;
    }

    public String typeOf(String name) {
        Symbol symbol = find(name);

        if (symbol != null) return symbol.getType();

        return "";
    }

    public int indexOf(String name){

        Symbol symbol = find(name);

        if (symbol != null) return symbol.getIndex();

        return -1;
    }

    private class Symbol {
        private String type;
        private SymbolTable.KIND kind;
        private int index;

        Symbol(String type, SymbolTable.KIND kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;    
        }

        public String getType() {
            return type;
        } 
        
        public KIND getKind() {
            return kind;
        }
    
        public int getIndex() {
            return index;
        }
    }

    // The find() method is a helper method that is used to find a symbol in the symbol table.
    // It takes a single parameter, name, which is the name of the symbol being searched for.
    // The method first checks if the symbol is present in the classTable, if it is, it returns the symbol object.
    // If not, it then checks if the symbol is present in the subroutineTable and returns it if found.
    // If the symbol is not found in either table, the method returns null.
    private Symbol find(String name){

        if (classTable.get(name) != null){
            return classTable.get(name);
        }else if (subroutineTable.get(name) != null){
            return subroutineTable.get(name);
        }else {
            return null;
        }

    }
}