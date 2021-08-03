package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 *
 *
 * Implement and use requireAssignable(Environment.Type target, Environment.Type type) in Analyzer.java
 * to identify when a RuntimeException should be thrown when the target type does not match the type being used or assigned.
 * Note, the method requireAssignable returns void because either the exception is generated or the requirement is met.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        throw new UnsupportedOperationException();  // TODO
        //LET num: Integer = 1;
        // DEF main(): Integer DO
        //     print(num + 1.0);
        // END
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if(!(ast.getExpression() instanceof Ast.Expr.Function)){
            throw new RuntimeException("Expression must be a function");
        }else{
            visit(ast.getExpression());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        //'LET' identifier (':' indentifier)? ('=' expression)? ';'
        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()){
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }
        Environment.Type type = null;

        if(ast.getTypeName().isPresent()){
            type = Environment.getType(ast.getTypeName().get());
        }
        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            //if(!ast.getTypeName().isPresent()){
            if(type == null){
                type = ast.getValue().get().getType();
            }
            requireAssignable(type, ast.getValue().get().getType());
        }
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        if(!(ast.getReceiver() instanceof Ast.Expr.Access)){
            throw new RuntimeException("Invalid assignment, receiver is not an access expression");
        }
        visit(ast.getValue());
        visit(ast.getReceiver());
        //Checking if the value is assignable
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }


    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if(ast.getCondition().getType() != Environment.Type.BOOLEAN){
            throw new RuntimeException("Condition must be a boolean");
        }
        if(ast.getThenStatements().isEmpty()){
            throw new RuntimeException("Missing then statement");
        }
        for (int i = 0; i < ast.getElseStatements().size(); i++) {
            visit(ast.getElseStatements().get(i));
        }
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            visit(ast.getThenStatements().get(i));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
//        visit(ast.getValue());
//        if(ast.getValue() != scope.getParent().)
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        Object literal = ast.getLiteral();
        if(literal instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(literal instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
        }
        else if(literal instanceof String){
            ast.setType(Environment.Type.STRING);
        }
        else if(ast.getLiteral() instanceof BigInteger){
            //if out of 32 bit range, throw runtime exception
            if(((BigInteger) ast.getLiteral()).bitCount() > 32){
                throw new RuntimeException("Invalid integer value");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if(ast.getLiteral() instanceof BigDecimal){
            if(((BigDecimal) ast.getLiteral()).doubleValue() < Double.MAX_VALUE){
                throw new RuntimeException("Invalid decimal value");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else{
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        if(!(ast.getExpression() instanceof Ast.Expr.Binary)){
            throw new RuntimeException("Expression must be binary inside a group");
        }else{
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String operator = ast.getOperator();

        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type lType = ast.getLeft().getType();
        Environment.Type rType = ast.getRight().getType();

        switch (operator){
            case "AND": case "OR":
                if(lType != Environment.Type.BOOLEAN || rType != Environment.Type.BOOLEAN){
                    throw new RuntimeException("Invalid operands");
                }else{
                    ast.setType(Environment.Type.BOOLEAN);
                }
                break;
            case "<": case "<=": case ">": case ">=": case "==": case "!=":
                if(lType != Environment.Type.COMPARABLE
                || rType != Environment.Type.COMPARABLE
                || !lType.equals(rType)){
                    throw new RuntimeException("Invalid operands");
                }else{
                    ast.setType(Environment.Type.BOOLEAN);
                }
                break;
            case "+":
                if(lType == Environment.Type.STRING
                        || rType == Environment.Type.STRING){
                    ast.setType(Environment.Type.STRING);
                }
                else{
                    if(lType == Environment.Type.INTEGER){
                        if (rType != Environment.Type.INTEGER) {
                            throw new RuntimeException("Invalid operands");
                        }else {
                            ast.setType(Environment.Type.INTEGER);
                        }
                    }
                    if(lType == Environment.Type.DECIMAL){
                        if (rType != Environment.Type.DECIMAL) {
                            throw new RuntimeException("Invalid operands");
                        }else {
                            ast.setType(Environment.Type.DECIMAL);
                        }
                    }
                }
                break;
            case "-": case "*": case "/":
                if(lType == Environment.Type.INTEGER){
                    if (rType != Environment.Type.INTEGER) {
                        throw new RuntimeException("Invalid operands");
                    }else {
                        ast.setType(Environment.Type.INTEGER);
                    }
                }
                if(lType == Environment.Type.DECIMAL){
                    if (rType != Environment.Type.DECIMAL) {
                        throw new RuntimeException("Invalid operands");
                    }else {
                        ast.setType(Environment.Type.DECIMAL);
                    }
                }
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()){
            visit(ast.getReceiver().get());
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), ast.getReceiver().get().getType().getField(ast.getName()).getType(), Environment.NIL));
        }else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if(ast.getReceiver().isPresent()){
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size()));
            for (int i = 1; i < ast.getArguments().size(); i++) {
                requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }

        }else {
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
            for (int i = 0; i < ast.getArguments().size(); i++) {
                requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target != Environment.Type.ANY){
            if(target == Environment.Type.COMPARABLE){
                if(type != Environment.Type.INTEGER
                  && type != Environment.Type.DECIMAL
                  && type != Environment.Type.CHARACTER
                  && type != Environment.Type.STRING){

                    throw new RuntimeException("Invalid assignment");
                }
            }
            else if(!target.equals(type)){
                throw new RuntimeException("Types don't match");
            }
        }
    }
}
