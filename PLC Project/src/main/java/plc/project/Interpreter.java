package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        //parent scope is inherited by subsequent scopes
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
        scope.defineFunction("logarithm", 1, arg_list -> {
            if (!(arg_list.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected type BigDecimal, received" +
                        arg_list.get(0).getValue().getClass().getName() + ".");
            }
            BigDecimal bd1 = (BigDecimal) arg_list.get(0).getValue();
            BigDecimal bd2 = requireType(
                    BigDecimal.class,
                    Environment.create(arg_list.get(0).getValue())
            );
            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        //Source does not create a new scope , because it will use the base scope already created
        // at the instantiation of the interpreter
        for (int i = 0; i < ast.getFields().size(); i++) {
            visit(ast.getFields().get(i));
        }
        for (int i = 0; i < ast.getMethods().size(); i++) {
            visit(ast.getMethods().get(i));
        }
        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            //scope has been already been defined
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope stateCapture = scope;

        //The callback function (lambda) should implement the behavior of calling this method
        scope.defineFunction(ast.getName(), ast.getParameters().size(), parameters -> {
            //Define variables for the incoming arguments, using the parameter names.
            scope = new Scope(scope);
            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i),
                            parameters.get(i));
                }
                //Evaluate the methods statements. Returns the value contained in a Return exception if thrown, otherwise NIL.
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    try {
                        visit(ast.getStatements().get(i));
                    }catch (Return e){
                        return e.value;
                    }
                }
            }finally {
                scope = stateCapture;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        //LET name; scope={}/NIL/scope={name=NIL}
        //LET name=1; scope={}/NIL/scope={name=1}
        if(ast.getValue().isPresent()){
            //scope has been already been defined
            scope.defineVariable( ast.getName(), visit(ast.getValue().get()) );
        }else{
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access temp = (Ast.Expr.Access) ast.getReceiver();
            if (temp.getReceiver().isPresent()) {
                visit(temp.getReceiver().get()).setField(temp.getName(), visit(ast.getValue()));
            } else {
                scope.lookupVariable(temp.getName()).setValue(visit(ast.getValue()));
            }
        }
        else {
            throw new RuntimeException("Invalid type, must be Access");
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        //Creates a new scope since it is a stmt block
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        else {
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getElseStatements()){
                    visit(stmt);
                }
            }finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        //Creates a new scope since it is a stmt block
        for(Object object : requireType(Iterable.class, visit(ast.getValue())) ){
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(),(Environment.PlcObject) object);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit( stmt );
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        //Creates a new scope since it is a stmt block
        while (requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit( stmt );
                }
                //ast.getStatement().forEach(this::visit);
            }finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() != null){
            return Environment.create(ast.getLiteral());
        }else{
            return Environment.NIL;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        //Evaluates the contained expression, returning it's value.
        // (1)->1 , (1 + 10)->11
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        //Evaluates arguments based on the specific binary operator,
        // returning the appropriate result for the operation
        // (hint: use requireType and Environment.create as needed)
        //requireType(Boolean.class, visit(ast.getCondition()))
        Object leftHand = visit(ast.getLeft()).getValue();
        Object rightHand = visit(ast.getRight()).getValue();
        switch (ast.getOperator()){
            case "AND":
                Ast.Expr.Literal falseTemp = new Ast.Expr.Literal(false);
                if(requireType(Boolean.class, visit(ast.getLeft())) || !requireType(Boolean.class, visit(ast.getLeft()))){
                    if(ast.getLeft().equals(falseTemp)){
                        return Environment.create(false);
                    }
                    else {
                        if(requireType(Boolean.class, visit(ast.getRight())) || !requireType(Boolean.class, visit(ast.getRight()))){
                            if(ast.getRight().equals(falseTemp)){
                                return Environment.create(false) ;
                            }
                        }
                        else{
                            throw new RuntimeException("Invalid operands for AND operation");
                        }
                    }
                    return Environment.create(true);
                }else{
                    throw new RuntimeException("Invalid operands for AND operation");
                }
                //break;
            case "OR":
                Ast.Expr.Literal trueTemp = new Ast.Expr.Literal(true);
                if(requireType(Boolean.class, visit(ast.getLeft()))|| !requireType(Boolean.class, visit(ast.getLeft()))){
                    if(ast.getLeft().equals(trueTemp)){
                        return Environment.create(true);
                    }
                    else {
                        if(requireType(Boolean.class, visit(ast.getRight()))|| !requireType(Boolean.class, visit(ast.getRight()))){
                            if(ast.getRight().equals(trueTemp)){
                                return Environment.create(true) ;
                            }
                        }
                        else{
                            throw new RuntimeException("Invalid operands for OR operation");
                        }
                    }
                    return Environment.create(false);
                }
            case "+":
                if(leftHand.getClass() == BigInteger.class && rightHand.getClass() == BigInteger.class){
                    BigInteger result = ((BigInteger) leftHand).add((BigInteger) rightHand);
                    return Environment.create(result);
                }
                if(leftHand.getClass() == BigDecimal.class && rightHand.getClass() == BigDecimal.class){
                    BigDecimal result = (((BigDecimal) leftHand).add((BigDecimal) rightHand));
                    return Environment.create(result);
                }
                if(leftHand.getClass() == String.class || rightHand.getClass() == String.class){
                    //concat them
                    return Environment.create(leftHand.toString() + rightHand.toString());
                }
                throw new RuntimeException("Invalid operands for addition");
            case "-":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        BigInteger result = ((BigInteger) leftHand).subtract((BigInteger) rightHand);
                        return Environment.create(result);
                    } else if (leftHand.getClass() == BigDecimal.class){
                        BigDecimal result = ((BigDecimal) leftHand).subtract((BigDecimal) rightHand);
                        return Environment.create(result);
                    }
                }
                throw new RuntimeException("Invalid operands for subtraction");
            case "*":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        BigInteger result = ((BigInteger) leftHand).multiply((BigInteger) rightHand);
                        return Environment.create(result);
                    } else if (leftHand.getClass() == BigDecimal.class){
                        BigDecimal result = ((BigDecimal) leftHand).multiply((BigDecimal) rightHand);
                        return Environment.create(result);
                    }
                }
                throw new RuntimeException("Invalid operands for multiplication");
            case "/":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        BigInteger result = ((BigInteger) leftHand).divide((BigInteger) rightHand);
                        return Environment.create(result);
                    } else if (leftHand.getClass() == BigDecimal.class){
                        MathContext rounding = new MathContext(1, RoundingMode.HALF_EVEN);
                        BigDecimal result = ((BigDecimal) leftHand).divide((BigDecimal) rightHand, rounding);
                        return Environment.create(result);
                    }
                }
                throw new RuntimeException("Invalid operands for division");
            case "==":
                if(leftHand.equals(rightHand)){
                    return Environment.create(true);
                }
                else{
                    return Environment.create(false);
                }
            case "!=":
                if(!leftHand.equals(rightHand)){
                    return Environment.create(true);
                }
                else{
                    return Environment.create(false);
                }
            case "<":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        int result = ((BigInteger) leftHand).compareTo((BigInteger) rightHand);
                        if (result < 0)
                            return Environment.create(true);
                        else
                            return Environment.create(false);
                    }
                }
                throw new RuntimeException("Invalid operands for comparison");

            case ">":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        int result = ((BigInteger) leftHand).compareTo((BigInteger) rightHand);
                        if (result > 0)
                            return Environment.create(true);
                        else
                            return Environment.create(false);
                    }
                }
                throw new RuntimeException("Invalid operands for comparison");

            case "<=":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        int result = ((BigInteger) leftHand).compareTo((BigInteger) rightHand);
                        if (result < 1)
                            return Environment.create(true);
                        else
                            return Environment.create(false);
                    }
                }
                throw new RuntimeException("Invalid operands for comparison");

            case ">=":
                if(leftHand.getClass() == rightHand.getClass()) {
                    if (leftHand.getClass() == BigInteger.class) {
                        int result = ((BigInteger) leftHand).compareTo((BigInteger) rightHand);
                        if (result > -1)
                            return Environment.create(true);
                        else
                            return Environment.create(false);
                    }
                }
                throw new RuntimeException("Invalid operands for comparison");

            default:
                throw new RuntimeException("Not a valid binary expression");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if(ast.getReceiver().isPresent()){
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        }
        else {
            if(ast.getName().equals("undefined")){
                return Environment.NIL;
            }
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {

        //arraylist (type PLCObject) of ast arguments
        ArrayList<Environment.PlcObject> argumentArray  = new ArrayList<Environment.PlcObject>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            argumentArray.add(visit(ast.getArguments().get(i)));
        }

        if(ast.getReceiver().isPresent()){
            return visit(ast.getReceiver().get()).callMethod(ast.getName(), argumentArray);
        }
        else {
            return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(argumentArray);
        }
    }
    //return Environment.PlcObject;

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }
}
