package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        //receive series of objs, iterate and print each of the objs
        //Depends on the type of object
        //The visiting is done here for that node
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        //Perform as many indentations as I need in the code
        //Levels of indentation are already handled for me
        //0 gives no indentation, 1 gives one more indentation
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    "); //4 blank spaces
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        //For source, I need to create a class
        // create a "class Main {"
        // declare the fields
        //declare "public static void main (String[] args){
        //            System.exit(new Main().main());
        //         {
        // declare each of our methods (VISIT each of the methods!!!)
        // one of our methods is called main()!
        // print "}" to close the class Main
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        //throw new UnsupportedOperationException(); //TODO
        //LET x: Integer = 3;
        //write: TYPE variable_name
        //Need to use the Java types correctly (look at the specifications), so use BigInteger in this case
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());

        // is there an assigned value?
        // if so, write: = and the value
        if(ast.getValue().isPresent()){
            print(" = ", ast.getValue().get());
        }
        // write: ;
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //throw new UnsupportedOperationException(); //TODO
        //print the while structure, including the condition
        print("while (", ast.getCondition(), ") {");

        //determine if there are statements to process, if yes..
        if(!ast.getStatements().isEmpty()) {
            //set up the next line
            newline(++indent); //handles i == 0 case
            //handle all statements in the while statement body
            for(int i = 0; i < ast.getStatements().size(); i++){
                //check if newline and indent are needed
                if(i != 0){
                    //setup the next line
                    newline(indent);
                }
                //print the next statement
                print(ast.getStatements().get(i));
            }
            //set up the next line
            newline(--indent);
        }
        //close the while
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        //For characters and strings, need to include the surrounding quotes

        print(ast.getLiteral());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        return null;
    }

}
