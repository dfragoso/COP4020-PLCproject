package plc.project;

import java.io.PrintWriter;
import java.util.Calendar;

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
        print("public class Main {");
        indent++;
        // declare the fields
        for (int i = 0; i < ast.getFields().size(); i++){
            if(i==0){
                newline(0);
            }
            newline(indent);
            print(ast.getFields().get(i));
        }
        newline(0);
        newline(indent);
        //declare "public static void main (String[] args){
        //            System.exit(new Main().main());
        //         {
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        // declare each of our methods (VISIT each of the methods!!!)
        // one of our methods is called main()!
        for(int i = 0; i < ast.getMethods().size(); i++){
            newline(0);
            newline(indent);
            print(ast.getMethods().get(i));
        }

        // print "}" to close the class Main
        newline(0);
        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        /*Generates a field expression.
        The expression should consist of the type name and the variable name stored in the AST
        separated by a single space character.
        If a value is present, then an equal sign character with surrounding single spaces is generated
        followed by the variable value (expression). A semicolon should be generated at the end.
        *
        * */
        print(ast.getVariable().getType().getJvmName(), " ", ast.getName());
        if(ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        /*The method should begin with the method's JVM type name
        followed by the method name,
        * */
        if (ast.getFunction().getJvmName() == "main") {
            print ("int ");
        }
        else {
            print(ast.getFunction().getJvmName(), " ");
        }
        print(ast.getFunction().getName(), "(");
        //Then the method should generate a comma-separated list of the method parameters surrounded by parenthesis.
        for(int i = 0; i < ast.getParameters().size(); i++){
            //Each parameter will consist of a JVM type name and the parameter name.
            print(ast.getParameterTypeNames().get(i), " ", ast.getParameters().get(i));
            if(i != ast.getParameters().size()-1){
                print(", ");
            }
        }
        print(") {");
        if(!ast.getStatements().isEmpty()){
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");
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

        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        //only if with statements
        print("if (", ast.getCondition(), ") {");
        newline(++indent);
        //generation of all the statements each ending with a newline
        if(!ast.getThenStatements().isEmpty()) {
            for(int i = 0; i < ast.getThenStatements().size(); i++){
                //print the next statement
                print(ast.getThenStatements().get(i));
                if(i != 0){
                    //setup the next line
                    newline(indent);
                }
            }
        }
        newline(--indent);
        print("}");

        //If there is an else block..
        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            newline(++indent);
            for(int i = 0; i < ast.getElseStatements().size(); i++){
                //print the next statement
                print(ast.getElseStatements().get(i));
                if(i != 0){
                    //setup the next line
                    newline(indent);
                }
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (", "int", " ", ast.getName(), " ", ":", " ", ast.getValue());
        print(") {");
        indent++;
        for(int i = 0; i < ast.getStatements().size(); i++){
            //check if newline and indent are needed
            newline(indent);
            //print the next statement
            print(ast.getStatements().get(i));
        }
        newline(--indent);
        print("}");
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
        print("return ", ast.getValue(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        //For characters and strings, need to include the surrounding quotes
        if(ast.getType().getJvmName() == "String"){
            print("\"", ast.getLiteral(), "\"");
        }else{
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        print(ast.getLeft(), " ");

        if(ast.getOperator().equals("AND")){
            print("&&");
        }else if(ast.getOperator().equals("OR")){
            print("||");
        }else {
            print(ast.getOperator());
        }

        print(" ", ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //If the expression has a receiver, evaluate it and return the value of the appropriate field,
        if(ast.getReceiver().isPresent()){
            print(ast.getReceiver().get(), ".");
        }
        // otherwise return the value of the appropriate variable in the current scope.
        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //If the function has a receiver, evaluate it and return the value of the appropriate field,
        if(ast.getReceiver().isPresent()){
            print(ast.getReceiver().get(), ".");
        }
        // otherwise return the jvmName of the function
        print(ast.getFunction().getJvmName());
        if(!ast.getArguments().isEmpty()){
            print("(");
            for (int i = 0; i < ast.getArguments().size(); i++){
                print(ast.getArguments().get(i));
                if(i == ast.getArguments().size()-1){
                    break;
                }
                print(", ");
            }
            print(")");
        }else {
            print("()");
        }

        return null;
    }

}
