package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        for (int i = 0; i < ast.getFields().size(); i++) {
            visit(ast.getFields().get(i));
        }
        boolean mainFound = false;
        for (int i = 0; i < ast.getMethods().size(); i++) {
            visit(ast.getMethods().get(i));
            if(ast.getMethods().get(i).getName() == "main" && ast.getMethods().get(i).getParameters().size() == 0){
                mainFound = true;
                if(ast.getMethods().get(i).getReturnTypeName().get() != "Integer"){
                    throw new RuntimeException("Invalid return type for main");
                }
            }
        }
        if(!mainFound){
            throw new RuntimeException("Missing main");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if(ast.getValue().isPresent()){
            visit(ast.getValue().get());
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));
            requireAssignable(ast.getVariable().getType(), ast.getValue().get().getType());
        }else {
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //The function's name and jvmName are both the name of the method
        String name = ast.getName();

        //The function's parameter types and return type are retrieved from the environment
        // using the corresponding names in the method.
        List<Environment.Type> parameterTypeNames = new ArrayList<>();
        for (int i = 0; i < ast.getParameterTypeNames().size(); i++) {
            parameterTypeNames.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }
        Environment.Type returnTypeName;
        //If the return type is not provided and thus, not present in the AST, the return type will be Nil
        if(!ast.getReturnTypeName().isPresent()){
            returnTypeName = Environment.Type.NIL;
        }else{
            returnTypeName = Environment.getType(ast.getReturnTypeName().get());
        }
        ast.setFunction(scope.defineFunction(name, name, parameterTypeNames, returnTypeName, args->Environment.NIL));

        scope = new Scope(scope);
        for (int i = 0; i < ast.getStatements().size(); i++) {
            this.visit(ast.getStatements().get(i));
            scope = scope.getParent();
        }

        return null;
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
        scope = new Scope(scope);
        for (int i = 0; i < ast.getElseStatements().size(); i++) {
            this.visit(ast.getElseStatements().get(i));
            scope.getParent();
        }
        scope = new Scope(scope);
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            Ast.Stmt temp = ast.getThenStatements().get(i);
            this.visit(ast.getThenStatements().get(i));
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        if(ast.getValue().getType() != Environment.Type.INTEGER_ITERABLE){
            throw new RuntimeException("Value must be of type IntegerIterable");
        }
        if(ast.getStatements().isEmpty()){
            throw new RuntimeException("Missing statements");
        }
        scope = new Scope(scope);
        for (int i = 0; i < ast.getStatements().size(); i++) {
            this.visit(ast.getStatements().get(i));
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        if(ast.getCondition().getType() != Environment.Type.BOOLEAN){
            throw new RuntimeException("Condition must be a boolean");
        }
        scope = new Scope(scope);
        for (int i = 0; i < ast.getStatements().size(); i++) {
            this.visit(ast.getStatements().get(i));
            scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());

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
                visit(ast.getArguments().get(i));
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